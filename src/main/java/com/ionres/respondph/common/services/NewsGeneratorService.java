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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsGeneratorService {

    private static final Logger LOG = Logger.getLogger(NewsGeneratorService.class.getName());

    private static final String ENV_API_KEY = "GEMINI_API_KEY";

    private static final String MODEL_ID = "gemini-3-pro-preview";

    private static final int REQUEST_COUNT = 10;
    private static final int TARGET        = 5;

    private static final int MIN_LEN = 280;
    private static final int MAX_LEN = 320;

    private static final boolean VERIFY_URL_LIVE = true;

    private static final Duration URL_TIMEOUT = Duration.ofSeconds(7);

    private static final int URL_VERIFY_THREADS = 6;

    // FIX #4: Relaxed LINE_PARSER — allow trailing whitespace/punctuation after closing ')'
    // Also make the number prefix fully optional, and allow '.' at end.
    private static final Pattern LINE_PARSER = Pattern.compile(
            "^\\s*(?:\\d+\\s*[.):\\-]\\s*)?(.+?)\\s*\\(\\s*[Ss]ource\\s*:\\s*(https?://[^\\s)]+)\\s*\\)\\s*[.\\s]*$"
    );

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

        LOG.info("[NewsGen] Service initialized. Model: " + MODEL_ID + ", URL verify: " + VERIFY_URL_LIVE);
    }

    public void cancelCurrentGeneration() {
        cancelRequested.set(true);
        Thread t = streamThread.get();
        if (t != null) t.interrupt();
        LOG.info("[NewsGen] Cancel requested.");
    }

    public CompletableFuture<List<NewsItem>> generateNewsHeadlines(
            String topic,
            BiConsumer<Double, String> onProgress) {

        cancelRequested.set(false);

        return CompletableFuture.supplyAsync(() -> {
            streamThread.set(Thread.currentThread());
            long startMs = System.currentTimeMillis();

            LOG.info("[NewsGen] Starting generation for topic: " + topic);

            List<NewsItem>         verified         = new CopyOnWriteArrayList<>();
            Set<String>            usedUrls         = ConcurrentHashMap.newKeySet();
            Set<String>            seenFingerprints = ConcurrentHashMap.newKeySet();
            List<Future<NewsItem>> pendingVerify    = new ArrayList<>();
            AtomicBoolean          streamDone       = new AtomicBoolean(false);

            ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "news-ticker"); t.setDaemon(true); return t;
            });
            AtomicReference<String> lastTickLabel   = new AtomicReference<>("🔍 Searching news sources…");
            AtomicLong              lastGroundingMs = new AtomicLong(0);

            ScheduledFuture<?> tick = ticker.scheduleAtFixedRate(() -> {
                if (streamDone.get()) return;
                // Don't overwrite a fresh grounding event with the stale ticker label
                if (System.currentTimeMillis() - lastGroundingMs.get() < 3000) return;
                long elapsed = elapsed(startMs);
                int  n       = verified.size();
                double bar   = Math.min((double) n / TARGET, 0.90);
                String label = (n == 0)
                        ? lastTickLabel.get() + " (" + elapsed + "s)"
                        : n + " of " + TARGET + " items verified (" + elapsed + "s)";
                emit(onProgress, bar, label);
            }, 0, 1, TimeUnit.SECONDS);

            // ── Phase 1: stream Gemini response ──────────────────────────────
            StringBuilder buffer         = new StringBuilder();
            int           linesSeenSoFar = 0;
            int           seenComplete   = 0;
            String        lastGrounding  = null; // most recent grounding label (search/page)

            try {
                LOG.info("[NewsGen] Calling Gemini stream with model: " + MODEL_ID);

                Iterable<GenerateContentResponse> stream =
                        client.models.generateContentStream(MODEL_ID, buildPrompt(topic), buildConfig());

                for (GenerateContentResponse chunk : stream) {
                    if (cancelRequested.get()) break;

                    // ── Grounding events (search queries / pages being read) ──
                    GroundingStatus gs = extractGroundingStatus(chunk);
                    if (gs != null) {
                        if (!gs.label.equals(lastGrounding)) {
                            lastGrounding = gs.label;
                            lastTickLabel.set(gs.label);
                            LOG.fine("[NewsGen] Grounding event: " + gs.label);
                        }
                        // Record when we last emitted a grounding event so the ticker backs off
                        lastGroundingMs.set(System.currentTimeMillis());
                        long e = elapsed(startMs);
                        int  n = verified.size();
                        String display = n > 0
                                ? n + " of " + TARGET + " found\n▶ " + gs.label
                                : gs.label + " (" + e + "s)";
                        emit(onProgress, Math.min((double) n / TARGET, 0.85), display);
                    }

                    // ── Text chunks (actual generated content) ───────────────
                    String piece = safeText(chunk);
                    if (piece != null && !piece.isEmpty()) {
                        buffer.append(piece);
                        LOG.finest("[NewsGen] Chunk received, buffer size: " + buffer.length());

                        int nowComplete = countComplete(buffer.toString());
                        if (nowComplete > seenComplete) {
                            seenComplete = nowComplete;

                            int[] result = dispatchNewLines(
                                    buffer.toString(), linesSeenSoFar,
                                    usedUrls, seenFingerprints,
                                    pendingVerify, verified
                            );
                            linesSeenSoFar = result[0];

                            drainVerified(pendingVerify, verified, usedUrls);

                            int n = verified.size();
                            long e = elapsed(startMs);
                            LOG.info("[NewsGen] Verified so far: " + n + "/" + TARGET);
                            emit(onProgress,
                                    Math.min((double) n / TARGET, 0.85),
                                    n + " of " + TARGET + " items found (" + e + "s)\n▶ " + tail(buffer.toString()));

                            if (n >= TARGET) {
                                LOG.info("[NewsGen] Reached target, stopping stream early.");
                                break;
                            }
                        } else {
                            // Mid-stream: show live writing preview
                            int n = verified.size();
                            long e = elapsed(startMs);
                            String tailText = tail(buffer.toString());
                            String header;
                            if (n > 0) {
                                header = n + " of " + TARGET + " items found (" + e + "s)";
                            } else if (buffer.length() > 10) {
                                // Gemini started writing — show "Writing" not grounding label
                                header = "Writing news… (" + e + "s)";
                            } else {
                                // Buffer nearly empty — still searching
                                header = (lastGrounding != null ? lastGrounding : "Searching…") + " (" + e + "s)";
                            }
                            // Only append tail if it has real content beyond the ellipsis
                            if (tailText.length() > 2) {
                                emit(onProgress, (double) n / TARGET, header + "\n▶ " + tailText);
                            } else {
                                emit(onProgress, (double) n / TARGET, header);
                            }
                        }
                    }
                }

                LOG.info("[NewsGen] Stream complete. Buffer length: " + buffer.length());

            } catch (Exception streamEx) {
                LOG.log(Level.WARNING, "[NewsGen] Stream error: " + streamEx.getMessage(), streamEx);
                if (!cancelRequested.get()) {
                    try {
                        emit(onProgress, (double) verified.size() / TARGET, "Reconnecting…");
                        LOG.info("[NewsGen] Falling back to blocking generateContent call.");
                        GenerateContentResponse resp =
                                client.models.generateContent(MODEL_ID, buildPrompt(topic), buildConfig());
                        String raw = safeText(resp);
                        if (raw != null) {
                            buffer.append("\n").append(raw);
                            LOG.info("[NewsGen] Fallback response received, length: " + raw.length());
                        }
                    } catch (Exception fallbackEx) {
                        LOG.log(Level.SEVERE, "[NewsGen] Fallback also failed: " + fallbackEx.getMessage(), fallbackEx);
                    }
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

            // ── Phase 2: dispatch any remaining unverified lines ──────────────
            LOG.info("[NewsGen] Phase 2: dispatching remaining lines.");
            dispatchNewLines(buffer.toString(), linesSeenSoFar,
                    usedUrls, seenFingerprints,
                    pendingVerify, verified);

            emit(onProgress, 0.88, "Validating articles… (" + elapsed(startMs) + "s)");

            waitForPending(pendingVerify, verified, usedUrls, 30_000);
            LOG.info("[NewsGen] After phase 2 wait, verified: " + verified.size());

            // ── Phase 3: retry gap if < TARGET ───────────────────────────────
            if (!cancelRequested.get() && verified.size() < TARGET) {
                int gap = TARGET - verified.size();
                LOG.info("[NewsGen] Phase 3: fetching " + gap + " more items.");
                emit(onProgress, 0.90,
                        "Only " + verified.size() + " found — fetching " + gap + " more…");

                List<NewsItem> extra = retryGap(topic, gap, usedUrls, seenFingerprints, startMs);
                verified.addAll(extra);
                LOG.info("[NewsGen] After retry, total verified: " + verified.size());
            }

            // ── Phase 4: rank, dedup, trim to TARGET ─────────────────────────
            List<NewsItem> best = selectBest(new ArrayList<>(verified), TARGET);

            long total = elapsed(startMs);
            LOG.info("[NewsGen] Done in " + total + "s. Final count: " + best.size());
            emit(onProgress, 1.0,
                    "Done — " + best.size() + " of " + TARGET + " items in " + total + "s");

            return best;
        });
    }

    // FIX #3: Now returns int[]{newLinesSeenCount} so the caller knows
    // how many raw lines have been processed (not how many were dispatched).
    private int[] dispatchNewLines(
            String raw, int alreadyProcessedLines,
            Set<String> usedUrls, Set<String> seenFingerprints,
            List<Future<NewsItem>> pending,
            List<NewsItem> verified) {

        if (raw == null || raw.isBlank()) return new int[]{alreadyProcessedLines};

        String[] lines = raw.split("\\r?\\n", -1);
        int dispatched   = 0;
        int totalLines   = lines.length;

        for (int lineIndex = alreadyProcessedLines; lineIndex < totalLines; lineIndex++) {
            String line    = lines[lineIndex];
            String trimmed = line.trim();

            if (trimmed.isEmpty()) continue;

            String fp = fingerprint(trimmed);
            if (seenFingerprints.contains(fp)) continue;

            Matcher m = LINE_PARSER.matcher(trimmed);
            if (!m.matches()) {
                LOG.fine("[NewsGen] Line did not match parser: " + trimmed.substring(0, Math.min(80, trimmed.length())));
                continue;
            }

            seenFingerprints.add(fp);

            String rawSms = m.group(1).trim();
            String url    = m.group(2).trim();

            LOG.fine("[NewsGen] Parsed line — url: " + url + ", sms length: " + rawSms.length());

            if (!isLikelyReliableUrl(url)) {
                LOG.fine("[NewsGen] URL rejected (unreliable): " + url);
                continue;
            }
            if (usedUrls.contains(url)) {
                LOG.fine("[NewsGen] URL already used: " + url);
                continue;
            }

            String sms = cleanSms(rawSms);
            if (!isSmsAcceptable(sms)) {
                LOG.fine("[NewsGen] SMS rejected (unacceptable, len=" + sms.length() + "): "
                        + sms.substring(0, Math.min(60, sms.length())));
                continue;
            }

            usedUrls.add(url);
            dispatched++;

            final String finalSms = sms;
            final String finalUrl = url;

            Future<NewsItem> future = urlVerifyPool.submit(() -> {
                if (cancelRequested.get()) return null;
                if (!VERIFY_URL_LIVE) {
                    LOG.fine("[NewsGen] URL verification skipped (disabled): " + finalUrl);
                    return new NewsItem(finalSms, finalUrl);
                }
                boolean live = isUrlLive(finalUrl);
                LOG.fine("[NewsGen] URL " + finalUrl + " live=" + live);
                if (live) {
                    return new NewsItem(finalSms, finalUrl);
                }
                usedUrls.remove(finalUrl);
                return null;
            });
            pending.add(future);
        }

        if (dispatched > 0)
            LOG.info("[NewsGen] dispatchNewLines: dispatched " + dispatched + " new items from " + totalLines + " lines.");

        return new int[]{totalLines};
    }

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
                    if (item != null && !containsUrl(verified, item.url())) {
                        verified.add(item);
                        LOG.info("[NewsGen] ✅ Verified item added: " + item.url());
                    }
                } catch (Exception ignored) {}
            }
        }
    }

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
        if (!pending.isEmpty()) {
            LOG.warning("[NewsGen] waitForPending timeout, " + pending.size() + " tasks still running.");
        }
        cancelPending(pending);
    }

    private void cancelPending(List<Future<NewsItem>> pending) {
        pending.forEach(f -> f.cancel(true));
        pending.clear();
    }

    private boolean containsUrl(List<NewsItem> list, String url) {
        return list.stream().anyMatch(i -> i.url().equalsIgnoreCase(url));
    }

    private List<NewsItem> retryGap(
            String topic, int needed,
            Set<String> usedUrls, Set<String> seenFingerprints,
            long startMs) {

        List<NewsItem> out = new ArrayList<>();
        try {
            String retryPrompt = buildRetryPrompt(topic, needed);
            LOG.info("[NewsGen] Retry gap: requesting " + (needed + 2) + " items.");
            GenerateContentResponse resp =
                    client.models.generateContent(MODEL_ID, retryPrompt, buildConfig());
            String raw = safeText(resp);
            if (raw == null || raw.isBlank()) {
                LOG.warning("[NewsGen] Retry returned empty response.");
                return out;
            }

            LOG.fine("[NewsGen] Retry raw response:\n" + raw);

            List<Future<NewsItem>> pending  = new ArrayList<>();
            int                    targeted = 0;

            for (String line : raw.split("\\r?\\n", -1)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                String fp = fingerprint(trimmed);
                if (seenFingerprints.contains(fp)) continue;

                Matcher m = LINE_PARSER.matcher(trimmed);
                if (!m.matches()) continue;

                seenFingerprints.add(fp);

                String sms = cleanSms(m.group(1).trim());
                String url = m.group(2).trim();

                if (!isSmsAcceptable(sms))     continue;
                if (!isLikelyReliableUrl(url)) continue;
                if (usedUrls.contains(url))    continue;

                usedUrls.add(url);
                targeted++;

                pending.add(urlVerifyPool.submit(() -> {
                    if (!VERIFY_URL_LIVE || isUrlLive(url)) return new NewsItem(sms, url);
                    usedUrls.remove(url);
                    return null;
                }));

                // FIX #5: Don't break early based on pending+out — let all lines be dispatched
                // so we have enough candidates after verification completes.
                if (targeted >= needed + 4) break; // slight buffer above needed
            }

            waitForPending(pending, out, usedUrls, 20_000);
            LOG.info("[NewsGen] Retry got " + out.size() + " verified items.");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[NewsGen] Retry gap failed: " + e.getMessage(), e);
        }

        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ranking / selection
    // ─────────────────────────────────────────────────────────────────────────

    private List<NewsItem> selectBest(List<NewsItem> candidates, int target) {
        if (candidates == null || candidates.isEmpty()) return new ArrayList<>();

        candidates.sort(Comparator.comparingInt(item -> {
            int len = item.smsText().length();
            if (len >= MIN_LEN && len <= MAX_LEN) return 0;
            return Math.min(Math.abs(len - MIN_LEN), Math.abs(len - MAX_LEN));
        }));

        List<NewsItem> out         = new ArrayList<>();
        Set<String>    seenDomains = new HashSet<>();

        for (NewsItem item : candidates) {
            if (out.size() >= target) break;
            String domain = domain(item.url());
            if (domain != null && seenDomains.contains(domain)) continue;
            if (domain != null) seenDomains.add(domain);
            out.add(item);
        }

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
            if (path  == null || path.length() < 6 || "/".equals(path)) return false;

            String h = host.toLowerCase(Locale.ROOT);
            if (h.startsWith("www.")) h = h.substring(4);

            for (String blocked : BLOCKED_HOSTS)
                if (h.equals(blocked) || h.endsWith("." + blocked)) return false;

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

            // FIX #6: removed duplicate 405 in condition
            if (code == 403 || code == 405 || code == 400) {
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
            LOG.fine("[NewsGen] URL liveness check failed for " + url + ": " + e.getMessage());
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
                .replaceAll("https?://\\S+", "")
                .replaceAll("\\*{1,2}([^*]+)\\*{1,2}", "$1")
                .replaceAll("\\.{2,}", ".")
                .replaceAll("\\s+", " ")
                .trim();

        if (!t.isEmpty() && !t.matches(".*[.!?]$")) t += ".";
        if (t.length() > MAX_LEN) t = truncateAtSentence(t, MAX_LEN);
        return t;
    }

    // FIX #2: Relaxed isSmsAcceptable — the old sentence-count check was rejecting
    // valid Hiligaynon text. Use a simpler heuristic: at least one sentence terminator
    // and a reasonable character count. Also allow up to MAX_LEN+40 before clean/truncate.
    private boolean isSmsAcceptable(String sms) {
        if (sms == null || sms.isBlank()) return false;
        int len = sms.length();

        // After cleanSms() the max is MAX_LEN (truncateAtSentence ensures this).
        // Only reject if genuinely too short or astronomically long (shouldn't happen).
        if (len < MIN_LEN - 20) {
            LOG.fine("[NewsGen] SMS too short (" + len + "): " + sms.substring(0, Math.min(40, len)));
            return false;
        }
        if (len > MAX_LEN + 50) {
            LOG.fine("[NewsGen] SMS too long (" + len + ") after clean.");
            return false;
        }
        if (sms.toLowerCase(Locale.ROOT).contains("lorem ipsum")) return false;

        // Must have at least one sentence-ending punctuation
        if (!sms.matches("(?s).*[.!?].*")) return false;

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

    // FIX #8: buildConfig now wraps the Tool in a List as required by the SDK.
    private GenerateContentConfig buildConfig() {
        Tool searchTool = Tool.builder()
                .googleSearch(GoogleSearch.builder().build())
                .build();

        return GenerateContentConfig.builder()
                .tools(List.of(searchTool))
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
        try { return r == null ? null : r.text(); } catch (Exception e) {
            LOG.fine("[NewsGen] safeText failed: " + e.getMessage());
            return null;
        }
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