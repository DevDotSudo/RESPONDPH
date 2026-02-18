package com.ionres.respondph.common.services;

import com.google.genai.Client;
import com.google.genai.types.*;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * NewsGeneratorService — Generates exactly 5 verified, live-linked SMS-ready
 * news items for Iloilo City. Strategy:
 *
 *  1. Ask Gemini (with Google Search grounding) for REQUEST_COUNT (10) raw items.
 *  2. While streaming, parse completed lines, verify URLs in parallel.
 *  3. After streaming ends, wait for in-flight verifications, then rank/dedup.
 *  4. If fewer than TARGET (5) survive, emit a single retry call for the gap.
 *  5. Return best TARGET items, progress-reported throughout.
 */
public class NewsGeneratorService {

    // ── Configuration ─────────────────────────────────────────────────────────
    private static final String ENV_API_KEY    = "GEMINI_API_KEY";
    private static final String MODEL_ID       = "gemini-3-pro-preview";

    /** How many items we ask Gemini for (buffer so we can afford to discard some). */
    private static final int REQUEST_COUNT = 10;
    /** How many we ultimately return to the caller. */
    private static final int TARGET        = 5;

    /** Minimum/maximum acceptable SMS character length. */
    private static final int MIN_LEN = 280;
    private static final int MAX_LEN = 320;

    /** Whether to do a live HTTP probe for every candidate URL. */
    private static final boolean VERIFY_URL_LIVE = true;

    /** How long to wait per URL probe. */
    private static final Duration URL_TIMEOUT = Duration.ofSeconds(7);

    /** Max parallel URL verification workers. */
    private static final int URL_VERIFY_THREADS = 6;

    // ── Regex ─────────────────────────────────────────────────────────────────

    /**
     * Matches a fully-formed output line:
     *   {@code N. <sms text> (Source: https://…)}
     */
    private static final Pattern LINE_PARSER = Pattern.compile(
            "^\\s*(?:\\d+\\s*[.):\\-]\\s*)?(.+?)\\s*\\(\\s*[Ss]ource\\s*:\\s*(https?://[^\\s)]+)\\s*\\)\\s*$"
    );

    /**
     * Used to count how many complete items are already in the buffer
     * (to drive incremental progress events while streaming).
     */
    private static final Pattern COMPLETE_ITEM = Pattern.compile(
            "(?m)^\\s*(?:\\d+\\s*[.):\\-]\\s*)?.{50,}?\\(\\s*[Ss]ource\\s*:\\s*https?://[^\\s)]{10,}\\s*\\)\\s*$"
    );

    // ── Infrastructure ────────────────────────────────────────────────────────
    private final Client     client;
    private final HttpClient http;
    private final ExecutorService urlVerifyPool;

    private final AtomicBoolean           cancelRequested = new AtomicBoolean(false);
    private final AtomicReference<Thread> streamThread    = new AtomicReference<>(null);

    // ── Record ────────────────────────────────────────────────────────────────
    /** Immutable data holder returned to callers. */
    public record NewsItem(String smsText, String url) {}

    // ─────────────────────────────────────────────────────────────────────────

    public NewsGeneratorService() {
        String apiKey = System.getenv(ENV_API_KEY);
        if (apiKey == null || apiKey.isBlank())
            throw new IllegalStateException("Missing environment variable: " + ENV_API_KEY);

        this.client = Client.builder().apiKey(apiKey.trim()).build();

        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(URL_TIMEOUT)
                .build();

        this.urlVerifyPool = Executors.newFixedThreadPool(
                URL_VERIFY_THREADS,
                r -> { Thread t = new Thread(r, "url-verify"); t.setDaemon(true); return t; }
        );
    }

    /** Signal the in-flight generation to stop gracefully. */
    public void cancelCurrentGeneration() {
        cancelRequested.set(true);
        Thread t = streamThread.get();
        if (t != null) t.interrupt();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Async entry-point. Returns a CompletableFuture that resolves to a list
     * of up to TARGET verified {@link NewsItem} objects.
     *
     * @param topic      The news topic (e.g., "typhoon", "barangay updates").
     * @param onProgress {@code (progress 0..1, statusString) -> void}
     */
    public CompletableFuture<List<NewsItem>> generateNewsHeadlines(
            String topic,
            BiConsumer<Double, String> onProgress) {

        cancelRequested.set(false);

        return CompletableFuture.supplyAsync(() -> {
            streamThread.set(Thread.currentThread());
            long startMs = System.currentTimeMillis();

            // Shared mutable state (only touched on the stream thread except the tick)
            List<NewsItem>            verified          = new CopyOnWriteArrayList<>();
            Set<String>               usedUrls          = ConcurrentHashMap.newKeySet();
            Set<String>               seenFingerprints  = ConcurrentHashMap.newKeySet();
            List<Future<NewsItem>>    pendingVerify     = new ArrayList<>();
            AtomicBoolean             streamDone        = new AtomicBoolean(false);

            // ── Elapsed-seconds ticker ────────────────────────────────────────
            ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "news-ticker"); t.setDaemon(true); return t;
            });
            AtomicReference<String> lastTickLabel = new AtomicReference<>("🔍 Searching news sources…");

            ScheduledFuture<?> tick = ticker.scheduleAtFixedRate(() -> {
                if (streamDone.get()) return;
                long elapsed = elapsed(startMs);
                int  n       = verified.size();
                double bar   = Math.min((double) n / TARGET, 0.90); // reserve last 10 % for validation
                String label = (n == 0)
                        ? lastTickLabel.get() + " (" + elapsed + "s)"
                        : n + " of " + TARGET + " items verified (" + elapsed + "s)";
                emit(onProgress, bar, label);
            }, 0, 1, TimeUnit.SECONDS);

            // ── Phase 1 : stream Gemini response ─────────────────────────────
            StringBuilder buffer       = new StringBuilder();
            int           parsedLines  = 0;    // how many raw lines we've already dispatched for verification
            int           seenComplete = 0;    // count of complete-looking lines in buffer (for UI events)
            String        lastSearchQ  = null;

            try {
                Iterable<GenerateContentResponse> stream =
                        client.models.generateContentStream(MODEL_ID, buildPrompt(topic), buildConfig());

                for (GenerateContentResponse chunk : stream) {
                    if (cancelRequested.get()) break;

                    // ── Grounding label ───────────────────────────────────────
                    GroundingStatus gs = extractGroundingStatus(chunk);
                    if (gs != null && !gs.label.equals(lastSearchQ)) {
                        lastSearchQ = gs.label;
                        lastTickLabel.set(gs.label);
                        long e = elapsed(startMs);
                        emit(onProgress, (double) verified.size() / TARGET,
                                gs.label + " (" + e + "s)");
                    }

                    // ── Accumulate text ───────────────────────────────────────
                    String piece = safeText(chunk);
                    if (piece != null && !piece.isEmpty()) {
                        buffer.append(piece);

                        // Count complete lines now in buffer
                        int nowComplete = countComplete(buffer.toString());
                        if (nowComplete > seenComplete) {
                            seenComplete = nowComplete;

                            // Dispatch any NEW fully-formed lines for async URL verification
                            int dispatched = dispatchNewLines(
                                    buffer.toString(), parsedLines,
                                    usedUrls, seenFingerprints,
                                    pendingVerify, verified
                            );
                            parsedLines += dispatched;

                            drainVerified(pendingVerify, verified, usedUrls);

                            int n = verified.size();
                            long e = elapsed(startMs);
                            emit(onProgress,
                                    Math.min((double) n / TARGET, 0.85),
                                    n + " of " + TARGET + " items found (" + e + "s)\n▶ " + tail(buffer.toString()));

                            if (n >= TARGET) break; // we have enough, no need to wait for more
                        } else {
                            // Still assembling — show live stream preview
                            int n = verified.size();
                            long e = elapsed(startMs);
                            String header = n == 0
                                    ? "Writing news… (" + e + "s)"
                                    : n + " of " + TARGET + " items found (" + e + "s)";
                            emit(onProgress, (double) n / TARGET, header + "\n▶ " + tail(buffer.toString()));
                        }
                    }
                }

            } catch (Exception streamEx) {
                if (!cancelRequested.get()) {
                    // Fallback: blocking call
                    try {
                        emit(onProgress, (double) verified.size() / TARGET, "Reconnecting…");
                        GenerateContentResponse resp =
                                client.models.generateContent(MODEL_ID, buildPrompt(topic), buildConfig());
                        String raw = safeText(resp);
                        if (raw != null) buffer.append("\n").append(raw);
                    } catch (Exception ignored) {}
                }
            } finally {
                streamThread.set(null);
                streamDone.set(true);
                tick.cancel(false);
                ticker.shutdownNow();
                Thread.interrupted(); // clear interrupt flag
            }

            if (cancelRequested.get()) {
                cancelPending(pendingVerify);
                emit(onProgress, 1.0, "Cancelled.");
                return Collections.emptyList();
            }

            // ── Phase 2 : dispatch any remaining unverified lines ─────────────
            dispatchNewLines(buffer.toString(), parsedLines,
                    usedUrls, seenFingerprints,
                    pendingVerify, verified);

            emit(onProgress, 0.88, "Validating articles… (" + elapsed(startMs) + "s)");

            // Wait for all in-flight verifications (up to 30 s total)
            waitForPending(pendingVerify, verified, usedUrls, 30_000);

            // ── Phase 3 : retry gap if < TARGET ──────────────────────────────
            if (!cancelRequested.get() && verified.size() < TARGET) {
                int gap = TARGET - verified.size();
                emit(onProgress, 0.90,
                        "Only " + verified.size() + " found — fetching " + gap + " more…");

                List<NewsItem> extra = retryGap(topic, gap, usedUrls, seenFingerprints, startMs);
                verified.addAll(extra);
            }

            // ── Phase 4 : rank, dedup, trim to TARGET ────────────────────────
            List<NewsItem> best = selectBest(new ArrayList<>(verified), TARGET);

            long total = elapsed(startMs);
            emit(onProgress, 1.0,
                    "Done — " + best.size() + " of " + TARGET + " items in " + total + "s");

            return best;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dispatch / Drain / Wait helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses lines from the buffer starting at {@code fromLine} (0-indexed line count
     * already processed), submits each valid candidate for async URL verification,
     * and returns how many NEW lines were dispatched.
     */
    private int dispatchNewLines(
            String raw, int alreadyParsed,
            Set<String> usedUrls, Set<String> seenFingerprints,
            List<Future<NewsItem>> pending,
            List<NewsItem> verified) {

        if (raw == null || raw.isBlank()) return 0;

        String[] lines = raw.split("\\r?\\n");
        int dispatched = 0;
        int lineIndex  = 0;

        for (String line : lines) {
            lineIndex++;
            if (lineIndex <= alreadyParsed) continue; // already handled

            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            String fp = fingerprint(trimmed);
            if (seenFingerprints.contains(fp)) continue;

            Matcher m = LINE_PARSER.matcher(trimmed);
            if (!m.matches()) continue;

            seenFingerprints.add(fp);

            String rawSms = m.group(1).trim();
            String url    = m.group(2).trim();

            // Quick pre-checks before spinning up a thread
            if (!isLikelyReliableUrl(url))   continue;
            if (usedUrls.contains(url))       continue;

            String sms = cleanSms(rawSms);
            if (!isSmsAcceptable(sms))         continue;

            // Reserve URL slot immediately to prevent double-submission
            usedUrls.add(url);
            dispatched++;

            final String finalSms = sms;
            final String finalUrl = url;

            Future<NewsItem> future = urlVerifyPool.submit(() -> {
                if (cancelRequested.get()) return null;
                if (!VERIFY_URL_LIVE || isUrlLive(finalUrl)) {
                    return new NewsItem(finalSms, finalUrl);
                }
                // URL dead — release the slot so a retry can use it
                usedUrls.remove(finalUrl);
                return null;
            });
            pending.add(future);
        }

        return dispatched;
    }

    /** Poll all pending futures and move done ones into verified (non-blocking). */
    private void drainVerified(
            List<Future<NewsItem>> pending,
            List<NewsItem> verified,
            Set<String> usedUrls) {

        Iterator<Future<NewsItem>> it = pending.iterator();
        while (it.hasNext()) {
            Future<NewsItem> f = it.next();
            if (f.isDone()) {
                it.remove();
                try {
                    NewsItem item = f.get();
                    if (item != null && !containsUrl(verified, item.url()))
                        verified.add(item);
                } catch (Exception ignored) {}
            }
        }
    }

    /** Block until all pending futures complete or timeout expires. */
    private void waitForPending(
            List<Future<NewsItem>> pending,
            List<NewsItem> verified,
            Set<String> usedUrls,
            long maxWaitMs) {

        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (!pending.isEmpty() && System.currentTimeMillis() < deadline) {
            drainVerified(pending, verified, usedUrls);
            if (!pending.isEmpty()) {
                try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }
        // Cancel anything still in-flight
        cancelPending(pending);
    }

    private void cancelPending(List<Future<NewsItem>> pending) {
        pending.forEach(f -> f.cancel(true));
        pending.clear();
    }

    private boolean containsUrl(List<NewsItem> list, String url) {
        return list.stream().anyMatch(i -> i.url().equalsIgnoreCase(url));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Retry gap (blocking, single non-stream call for missing items)
    // ─────────────────────────────────────────────────────────────────────────

    private List<NewsItem> retryGap(
            String topic, int needed,
            Set<String> usedUrls, Set<String> seenFingerprints,
            long startMs) {

        List<NewsItem> out = new ArrayList<>();
        try {
            String retryPrompt = buildRetryPrompt(topic, needed);
            GenerateContentResponse resp =
                    client.models.generateContent(MODEL_ID, retryPrompt, buildConfig());
            String raw = safeText(resp);
            if (raw == null || raw.isBlank()) return out;

            List<Future<NewsItem>> pending = new ArrayList<>();

            for (String line : raw.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                String fp = fingerprint(trimmed);
                if (seenFingerprints.contains(fp)) continue;

                Matcher m = LINE_PARSER.matcher(trimmed);
                if (!m.matches()) continue;

                seenFingerprints.add(fp);

                String sms = cleanSms(m.group(1).trim());
                String url = m.group(2).trim();

                if (!isSmsAcceptable(sms))       continue;
                if (!isLikelyReliableUrl(url))   continue;
                if (usedUrls.contains(url))       continue;

                usedUrls.add(url);
                pending.add(urlVerifyPool.submit(() -> {
                    if (!VERIFY_URL_LIVE || isUrlLive(url)) return new NewsItem(sms, url);
                    usedUrls.remove(url);
                    return null;
                }));

                if (out.size() + pending.size() >= needed) break;
            }

            waitForPending(pending, out, usedUrls, 20_000);
        } catch (Exception ignored) {}

        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ranking / selection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * From all verified candidates, pick the best {@code target} items by:
     *  1. Prefer SMS lengths closest to the ideal 300-320 range.
     *  2. Deduplicate by domain (one story per outlet).
     *  3. If domain-dedup leaves a gap, fill with next-best regardless of domain.
     */
    private List<NewsItem> selectBest(List<NewsItem> candidates, int target) {
        if (candidates == null || candidates.isEmpty()) return new ArrayList<>();

        // Score: 0 = perfect length, higher = worse
        candidates.sort(Comparator.comparingInt(item -> {
            int len = item.smsText().length();
            if (len >= MIN_LEN && len <= MAX_LEN) return 0;
            return Math.min(Math.abs(len - MIN_LEN), Math.abs(len - MAX_LEN));
        }));

        List<NewsItem> out         = new ArrayList<>();
        Set<String>    seenDomains = new HashSet<>();

        // First pass: one item per domain
        for (NewsItem item : candidates) {
            if (out.size() >= target) break;
            String domain = domain(item.url());
            if (domain != null && seenDomains.contains(domain)) continue;
            if (domain != null) seenDomains.add(domain);
            out.add(item);
        }

        // Second pass: fill remaining slots ignoring domain restriction
        if (out.size() < target) {
            for (NewsItem item : candidates) {
                if (out.size() >= target) break;
                if (!out.contains(item)) out.add(item);
            }
        }

        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // URL helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "facebook.com", "fb.com", "m.facebook.com",
            "youtube.com", "youtu.be",
            "tiktok.com", "instagram.com",
            "x.com", "twitter.com",
            "google.com", "news.google.com",
            "bit.ly", "tinyurl.com", "t.co",
            "reddit.com", "wikipedia.org"
    );

    private boolean isLikelyReliableUrl(String url) {
        try {
            URI u = URI.create(url);
            String scheme = u.getScheme();
            String host   = u.getHost();
            String path   = u.getPath();

            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) return false;
            if (host  == null || host.isBlank())  return false;

            // Must be an article path, not a bare homepage
            if (path == null || path.length() < 6 || "/".equals(path)) return false;

            String h = host.toLowerCase(Locale.ROOT);
            // Strip leading "www."
            if (h.startsWith("www.")) h = h.substring(4);

            for (String blocked : BLOCKED_HOSTS)
                if (h.equals(blocked) || h.endsWith("." + blocked)) return false;

            // Avoid tag/category/search pages — not real articles
            String p = path.toLowerCase(Locale.ROOT);
            if (p.contains("/tag/") || p.contains("/search") ||
                    p.contains("/category/") || p.contains("?s=") || p.contains("?q=")) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isUrlLive(String url) {
        try {
            URI uri = URI.create(url);

            // Try HEAD first (fast)
            HttpRequest head = HttpRequest.newBuilder(uri)
                    .timeout(URL_TIMEOUT)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .header("User-Agent",      "Mozilla/5.0 (compatible; RespondPH/1.0)")
                    .header("Accept",          "text/html,*/*")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build();

            HttpResponse<Void> r = http.send(head, HttpResponse.BodyHandlers.discarding());
            int code = r.statusCode();

            if (code >= 200 && code < 400) return true;

            // Some servers block HEAD — fall back to GET
            if (code == 403 || code == 405 || code == 400 || code == 405) {
                HttpRequest get = HttpRequest.newBuilder(uri)
                        .timeout(URL_TIMEOUT)
                        .GET()
                        .header("User-Agent", "Mozilla/5.0 (compatible; RespondPH/1.0)")
                        .build();
                HttpResponse<Void> g = http.send(get, HttpResponse.BodyHandlers.discarding());
                return g.statusCode() >= 200 && g.statusCode() < 400;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String domain(String url) {
        try { return URI.create(url).getHost(); } catch (Exception e) { return null; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SMS text cleaning / validation
    // ─────────────────────────────────────────────────────────────────────────

    private String cleanSms(String raw) {
        if (raw == null) return "";
        String t = raw
                .replaceAll("https?://\\S+", "")       // strip accidental URLs in text
                .replaceAll("\\*{1,2}([^*]+)\\*{1,2}", "$1") // strip markdown bold/italic
                .replaceAll("\\.{2,}", ".")             // collapse ellipsis
                .replaceAll("\\s+", " ")
                .trim();

        if (!t.isEmpty() && !t.matches(".*[.!?]$")) t += ".";
        if (t.length() > MAX_LEN) t = truncateAtSentence(t, MAX_LEN);
        return t;
    }

    private boolean isSmsAcceptable(String sms) {
        if (sms == null || sms.isBlank()) return false;
        int len = sms.length();
        if (len < MIN_LEN || len > MAX_LEN + 30) return false; // allow slight overshoot (truncation will fix)
        if (countSentences(sms) < 2) return false;
        // Reject placeholder / test lines
        if (sms.toLowerCase(Locale.ROOT).contains("lorem ipsum")) return false;
        return true;
    }

    private int countSentences(String text) {
        if (text == null) return 0;
        int n = 0;
        for (String s : text.split("[.!?]+"))
            if (s.trim().length() > 8) n++;
        return n;
    }

    private String truncateAtSentence(String text, int max) {
        if (text.length() <= max) return text;
        String sub  = text.substring(0, max);
        int    last = Math.max(sub.lastIndexOf('.'), Math.max(sub.lastIndexOf('!'), sub.lastIndexOf('?')));
        return (last > max / 2) ? sub.substring(0, last + 1).trim() : sub.trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prompt builders
    // ─────────────────────────────────────────────────────────────────────────

    private String buildPrompt(String topic) {
        String today = LocalDate.now().toString();
        return
                "You are an AI assistant that generates SMS news alerts for RESPONDPH, " +
                        "a disaster-response and barangay management app for Iloilo City, Philippines.\n\n" +

                        "TASK: Use Google Search grounding to find " + REQUEST_COUNT + " REAL, CURRENTLY LIVE " +
                        "news articles from Iloilo City published on " + today + " or within the last 3 days.\n" +
                        "Topic: " + topic + "\n\n" +

                        "OUTPUT FORMAT — one item per line, no blank lines, no preamble, no markdown:\n" +
                        "{N}. {Hiligaynon SMS text} (Source: {FULL article URL})\n\n" +

                        "STRICT RULES (violating any rule = the item is silently discarded):\n" +
                        "1. SMS text must be exactly 300–320 characters (spaces included). Count carefully.\n" +
                        "2. SMS text must contain 2–3 complete sentences. Never cut a sentence short.\n" +
                        "3. Write in Hiligaynon. Use English only where no equivalent exists.\n" +
                        "4. Do NOT embed the URL inside the SMS text.\n" +
                        "5. The Source URL must point to a specific article path (not a homepage or tag page).\n" +
                        "6. The Source URL must be real and currently reachable — confirmed via Google Search.\n" +
                        "7. NEVER fabricate, guess, or construct URLs. If unsure, skip that story.\n" +
                        "8. Each item must be a distinct story with a distinct URL and domain.\n" +
                        "9. Fully complete each item before starting the next.\n" +
                        "10. Output exactly " + REQUEST_COUNT + " items. No extra commentary.\n";
    }

    private String buildRetryPrompt(String topic, int needed) {
        String today = LocalDate.now().toString();
        return
                "You are an AI assistant for RESPONDPH (Iloilo City disaster-response app).\n\n" +
                        "TASK: Use Google Search grounding to find " + (needed + 2) + " MORE news articles " +
                        "about '" + topic + "' from Iloilo City, published today (" + today + ") or last 3 days.\n" +
                        "These must be articles we have NOT already found — different sources and different stories.\n\n" +
                        "OUTPUT FORMAT (one item per line, no preamble):\n" +
                        "{N}. {Hiligaynon SMS, 300-320 chars, 2-3 complete sentences} (Source: {full article URL})\n\n" +
                        "Same rules as before: real URLs, no fabrication, one item per distinct domain.\n" +
                        "Output exactly " + (needed + 2) + " items.";
    }

    private GenerateContentConfig buildConfig() {
        return GenerateContentConfig.builder()
                .tools(Tool.builder()
                        .googleSearch(GoogleSearch.builder().build())
                        .build())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Streaming helpers
    // ─────────────────────────────────────────────────────────────────────────

    private int countComplete(String buffer) {
        if (buffer == null || buffer.isBlank()) return 0;
        Matcher m = COMPLETE_ITEM.matcher(buffer);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    private String tail(String buffer) {
        if (buffer == null || buffer.isBlank()) return "";
        String t = buffer.length() > 100
                ? buffer.substring(buffer.length() - 100)
                : buffer;
        // Strip partial first word (likely cut at chunk boundary)
        int sp = t.indexOf(' ');
        if (sp > 0 && sp < 20) t = t.substring(sp + 1);
        return "…" + t.replaceAll("\\s+", " ").trim();
    }

    private String fingerprint(String line) {
        return Integer.toHexString(line.toLowerCase(Locale.ROOT).hashCode());
    }

    private void emit(BiConsumer<Double, String> cb, double p, String s) {
        if (cb != null) cb.accept(Math.max(0.0, Math.min(1.0, p)), s);
    }

    private long elapsed(long startMs) {
        return (System.currentTimeMillis() - startMs) / 1000;
    }

    private String safeText(GenerateContentResponse r) {
        try { return r == null ? null : r.text(); } catch (Exception e) { return null; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Grounding metadata extraction
    // ─────────────────────────────────────────────────────────────────────────

    private static class GroundingStatus {
        final String label;
        GroundingStatus(String l) { this.label = l; }
    }

    private GroundingStatus extractGroundingStatus(GenerateContentResponse chunk) {
        try {
            if (chunk == null) return null;
            List<Candidate> candidates = chunk.candidates().orElse(null);
            if (candidates == null || candidates.isEmpty()) return null;
            Candidate c = candidates.get(0);
            if (c == null) return null;

            GroundingMetadata meta = c.groundingMetadata().orElse(null);
            if (meta == null) return null;

            // Prefer grounding chunks (pages being read) over search query labels
            List<GroundingChunk> chunks = meta.groundingChunks().orElse(null);
            if (chunks != null && !chunks.isEmpty()) {
                for (int i = chunks.size() - 1; i >= 0; i--) {
                    GroundingChunk gc = chunks.get(i);
                    if (gc == null) continue;
                    var web = gc.web().orElse(null);
                    if (web == null) continue;

                    String title = web.title().orElse(null);
                    String uri   = web.uri().orElse(null);

                    if (title != null && !title.isBlank()) {
                        String d = title.length() > 52 ? title.substring(0, 49) + "…" : title;
                        return new GroundingStatus("📄 Reading: " + d);
                    }
                    if (uri != null && !uri.isBlank()) {
                        try {
                            String host = URI.create(uri).getHost();
                            if (host != null)
                                return new GroundingStatus("📄 Reading: " + host);
                        } catch (Exception ignored) {}
                    }
                }
            }

            List<String> queries = meta.webSearchQueries().orElse(null);
            if (queries != null && !queries.isEmpty()) {
                String q = queries.get(0);
                if (q != null && !q.isBlank()) {
                    String d = q.length() > 52 ? q.substring(0, 49) + "…" : q;
                    return new GroundingStatus("🔍 Searching: \"" + d + "\"");
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}