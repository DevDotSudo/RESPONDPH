package com.ionres.respondph.common.services;

import com.google.genai.Client;
import com.google.genai.types.*;
import java.net.URI;
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

    private static final List<String> MODEL_CHAIN = List.of(
            "gemini-pro-latest",
            "gemini-3-pro-preview",
            "gemini-3-flash-preview",
            "gemini-2.5-flash"
    );

    private static final int REQUEST_COUNT = 10;
    private static final int TARGET        = 5;
    private static final int MAX_LEN       = 320;

    private static final Pattern LINE_PARSER = Pattern.compile(
            "^\\s*(?:\\d+\\s*[.):\\-]\\s*)?(.+?)\\s*\\(\\s*[Ss]ource\\s*:\\s*(https?://[^\\s)]+)\\s*\\)\\s*[.\\s]*$"
    );

    private static final Pattern COMPLETE_ITEM = Pattern.compile(
            "(?m)^\\s*(?:\\d+\\s*[.):\\-]\\s*)?.{30,}?\\(\\s*[Ss]ource\\s*:\\s*https?://[^\\s)]{10,}\\s*\\)\\s*$"
    );

    // ── URL blocklist ─────────────────────────────────────────────────────────
    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "facebook.com", "fb.com", "m.facebook.com",
            "youtube.com", "youtu.be",
            "tiktok.com", "instagram.com",
            "x.com", "twitter.com",
            "google.com", "news.google.com",
            "bit.ly", "tinyurl.com", "t.co",
            "reddit.com", "wikipedia.org"
    );

    // ── Infrastructure ────────────────────────────────────────────────────────
    private final Client client;

    private final AtomicBoolean           cancelRequested   = new AtomicBoolean(false);
    private final AtomicReference<Thread> streamThread      = new AtomicReference<>(null);
    // Tracks which model in MODEL_CHAIN is active (reset to 0 each generation)
    private final AtomicInteger           currentModelIndex = new AtomicInteger(0);

    public record NewsItem(String smsText, String url) {}

    // ─────────────────────────────────────────────────────────────────────────

    public NewsGeneratorService() {
        String apiKey = System.getenv(ENV_API_KEY);
        if (apiKey == null || apiKey.isBlank())
            throw new IllegalStateException("Missing environment variable: " + ENV_API_KEY);
        this.client = Client.builder().apiKey(apiKey.trim()).build();
        LOG.info("[NewsGen] Initialized. models=" + MODEL_CHAIN);
    }

    public void cancelCurrentGeneration() {
        cancelRequested.set(true);
        Thread t = streamThread.get();
        if (t != null) t.interrupt();
        LOG.info("[NewsGen] Cancel requested.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main entry point
    // ─────────────────────────────────────────────────────────────────────────

    public CompletableFuture<List<NewsItem>> generateNewsHeadlines(
            String topic,
            BiConsumer<Double, String> onProgress) {

        cancelRequested.set(false);

        return CompletableFuture.supplyAsync(() -> {
            streamThread.set(Thread.currentThread());
            currentModelIndex.set(0);
            long startMs = System.currentTimeMillis();
            LOG.info("[NewsGen] Starting: topic=" + topic);

            // Shared state — all parsing uses these so dedup spans the full run
            List<NewsItem> results     = new ArrayList<>();
            Set<String>    usedUrls    = new LinkedHashSet<>();
            Set<String>    usedDomains = new LinkedHashSet<>();
            Set<String>    seenFprints = new HashSet<>();

            // ── Ticker — fires every second, backs off after grounding events ─
            AtomicBoolean           streamDone   = new AtomicBoolean(false);
            AtomicReference<String> lastTickLbl  = new AtomicReference<>("🔍 Searching news sources…");
            AtomicLong              lastGroundMs = new AtomicLong(0);

            ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "news-ticker"); t.setDaemon(true); return t;
            });
            ScheduledFuture<?> tick = ticker.scheduleAtFixedRate(() -> {
                if (streamDone.get()) return;
                if (System.currentTimeMillis() - lastGroundMs.get() < 3000) return;
                long e = elapsed(startMs);
                int  n = results.size();
                String lbl = n == 0
                        ? lastTickLbl.get() + " (" + e + "s)"
                        : n + " of " + TARGET + " items found (" + e + "s)";
                emit(onProgress, Math.min((double) n / TARGET, 0.85), lbl);
            }, 0, 1, TimeUnit.SECONDS);

            // ── Buffer persists across all model attempts ─────────────────────
            StringBuilder buffer = new StringBuilder();

            try {
                // ── Model fallback loop ───────────────────────────────────────
                for (int modelIdx = 0;
                     modelIdx < MODEL_CHAIN.size()
                             && results.size() < TARGET
                             && !cancelRequested.get();
                     modelIdx++) {

                    currentModelIndex.set(modelIdx);
                    String model = MODEL_CHAIN.get(modelIdx);
                    LOG.info("[NewsGen] Trying model [" + (modelIdx + 1) + "/" + MODEL_CHAIN.size() + "]: " + model);
                    emit(onProgress, 0.02, "Using " + model + "…");

                    // Count lines already processed so we don't re-parse them
                    // when the next model appends to the shared buffer.
                    int linesSeen    = countLines(buffer.toString());
                    int seenComplete = countComplete(buffer.toString());
                    String lastGround = null;

                    try {
                        Iterable<GenerateContentResponse> stream =
                                client.models.generateContentStream(
                                        model, buildPrompt(topic), buildConfig());

                        for (GenerateContentResponse chunk : stream) {
                            if (cancelRequested.get()) break;

                            // ── Grounding events ──────────────────────────────
                            GroundingStatus gs = extractGroundingStatus(chunk);
                            if (gs != null) {
                                if (!gs.label.equals(lastGround)) {
                                    lastGround = gs.label;
                                    lastTickLbl.set(gs.label);
                                    LOG.fine("[NewsGen] Ground: " + gs.label);
                                }
                                lastGroundMs.set(System.currentTimeMillis());
                                int  n = results.size();
                                long e = elapsed(startMs);
                                String display = n > 0
                                        ? n + " of " + TARGET + " found\n▶ " + gs.label
                                        : gs.label + " (" + e + "s)";
                                emit(onProgress, Math.min((double) n / TARGET, 0.85), display);
                            }

                            // ── Text chunk ────────────────────────────────────
                            String piece = safeText(chunk);
                            if (piece == null || piece.isEmpty()) continue;
                            buffer.append(piece);

                            int nowComplete = countComplete(buffer.toString());
                            if (nowComplete > seenComplete) {
                                seenComplete = nowComplete;

                                // Parse all lines up to the current end of buffer,
                                // resuming from where we left off (linesSeen).
                                // NOTE: we do NOT stop at TARGET here — we parse all
                                // complete lines so seenFprints stays accurate and
                                // no valid items are left in the buffer un-indexed.
                                int newLinesSeen = parseAll(buffer.toString(), linesSeen,
                                        results, usedUrls, usedDomains, seenFprints);
                                linesSeen = newLinesSeen;

                                int  n = results.size();
                                long e = elapsed(startMs);
                                LOG.info("[NewsGen] Collected " + n + "/" + TARGET);
                                emit(onProgress,
                                        Math.min((double) n / TARGET, 0.90),
                                        n + " of " + TARGET + " items found (" + e + "s)\n▶ " + tail(buffer.toString()));

                                // Stop streaming only after TARGET; let all items be indexed
                                if (n >= TARGET) {
                                    LOG.info("[NewsGen] Reached " + TARGET + ", stopping stream.");
                                    break;
                                }
                            } else {
                                // Still assembling current line — show live preview
                                int    n   = results.size();
                                long   e   = elapsed(startMs);
                                String txt = tail(buffer.toString());
                                String hdr;
                                if (n > 0)                     hdr = n + " of " + TARGET + " items found (" + e + "s)";
                                else if (buffer.length() > 10) hdr = "Writing news… (" + e + "s)";
                                else                           hdr = (lastGround != null ? lastGround : "Searching…") + " (" + e + "s)";
                                emit(onProgress, (double) n / TARGET,
                                        txt.length() > 2 ? hdr + "\n▶ " + txt : hdr);
                            }
                        }

                        // Parse any remaining content after the stream ends normally
                        parseAll(buffer.toString(), linesSeen,
                                results, usedUrls, usedDomains, seenFprints);

                        LOG.info("[NewsGen] Stream done [" + model + "]. buffer=" + buffer.length()
                                + " results=" + results.size());

                        // If we already have TARGET, no need to try next model
                        if (results.size() >= TARGET) break;

                        // Model succeeded but gave fewer than TARGET items.
                        // Try next model with a fresh prompt asking for the remaining gap.
                        if (modelIdx + 1 < MODEL_CHAIN.size() && results.size() < TARGET) {
                            int gap = TARGET - results.size();
                            LOG.info("[NewsGen] Got " + results.size() + "/" + TARGET
                                    + " — trying next model for " + gap + " more.");
                            emit(onProgress, (double) results.size() / TARGET,
                                    "Found " + results.size() + " — searching for " + gap + " more…");
                        }

                    } catch (Exception ex) {
                        LOG.log(Level.WARNING, "[NewsGen] Model [" + model + "] failed: " + ex.getMessage(), ex);
                        if (modelIdx + 1 < MODEL_CHAIN.size()) {
                            String next = MODEL_CHAIN.get(modelIdx + 1);
                            LOG.info("[NewsGen] Falling back to: " + next);
                            emit(onProgress, (double) results.size() / TARGET,
                                    "Switching to " + next + "…");
                        } else {
                            LOG.severe("[NewsGen] All models exhausted.");
                            emit(onProgress, (double) results.size() / TARGET,
                                    "All models unavailable.");
                        }
                    }
                }

            } finally {
                streamThread.set(null);
                streamDone.set(true);
                tick.cancel(false);
                ticker.shutdownNow();
                Thread.interrupted();
            }

            if (cancelRequested.get()) {
                emit(onProgress, 1.0, "Cancelled.");
                return Collections.emptyList();
            }

            // Return in order, capped at TARGET
            List<NewsItem> out = results.size() > TARGET
                    ? new ArrayList<>(results.subList(0, TARGET))
                    : new ArrayList<>(results);

            long total = elapsed(startMs);
            LOG.info("[NewsGen] Done in " + total + "s. count=" + out.size());
            emit(onProgress, 1.0,
                    "Done — " + out.size() + " of " + TARGET + " items in " + total + "s");
            return out;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parser — scans ALL lines from `startLine` onward, collects every valid
    // item found (no early stop at TARGET so the buffer stays fully indexed).
    // Returns the new total line count so the caller can resume correctly.
    // ─────────────────────────────────────────────────────────────────────────

    private int parseAll(
            String raw,
            int startLine,
            List<NewsItem> results,
            Set<String> usedUrls,
            Set<String> usedDomains,
            Set<String> seenFprints) {

        if (raw == null || raw.isBlank()) return startLine;

        String[] lines = raw.split("\\r?\\n", -1);
        int      total = lines.length;

        for (int i = startLine; i < total; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.isEmpty()) continue;

            // Fingerprint the raw trimmed line so partially-received lines
            // don't collide with their eventual complete form.
            // Only fingerprint lines that actually match (below).

            Matcher m = LINE_PARSER.matcher(trimmed);
            if (!m.matches()) {
                LOG.fine("[NewsGen] No match (line " + i + "): "
                        + trimmed.substring(0, Math.min(80, trimmed.length())));
                continue;
            }

            // Fingerprint matched lines only (avoids polluting seenFprints
            // with fragments that would block the completed version later).
            String fp = fingerprint(trimmed);
            if (seenFprints.contains(fp)) {
                LOG.fine("[NewsGen] Dup fingerprint: " + trimmed.substring(0, Math.min(50, trimmed.length())));
                continue;
            }
            seenFprints.add(fp);

            String rawSms = m.group(1).trim();
            String url    = m.group(2).trim();

            LOG.fine("[NewsGen] Parsed url=" + url + " smsLen=" + rawSms.length());

            if (!isUsableUrl(url)) {
                LOG.fine("[NewsGen] Bad URL: " + url); continue;
            }
            if (usedUrls.contains(url)) {
                LOG.fine("[NewsGen] Dup URL: " + url); continue;
            }
            String dom = domain(url);
            if (dom != null && usedDomains.contains(dom)) {
                LOG.fine("[NewsGen] Dup domain: " + dom); continue;
            }

            String sms = cleanSms(rawSms);
            if (!isSmsUsable(sms)) {
                LOG.fine("[NewsGen] SMS unusable len=" + sms.length()); continue;
            }

            usedUrls.add(url);
            if (dom != null) usedDomains.add(dom);
            results.add(new NewsItem(sms, url));
            LOG.info("[NewsGen] ✅ Item #" + results.size() + ": " + url);
        }

        return total;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SMS cleaning
    // ─────────────────────────────────────────────────────────────────────────

    private String cleanSms(String raw) {
        if (raw == null) return "";
        String t = raw
                .replaceAll("https?://\\S+", "")
                .replaceAll("\\*{1,2}([^*]+)\\*{1,2}", "$1")
                .replaceAll("\\.{2,}", ".")
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (!t.isEmpty() && !t.matches(".*[.!?]$")) t += ".";
        if (t.length() > MAX_LEN) t = truncateAtSentence(t, MAX_LEN);
        return t;
    }

    /** Minimal gate — only rejects truly broken output. */
    private boolean isSmsUsable(String sms) {
        if (sms == null || sms.isBlank()) return false;
        if (sms.length() < 50) return false;
        if (!sms.matches("(?s).*[.!?].*")) return false;
        if (sms.toLowerCase(Locale.ROOT).contains("lorem ipsum")) return false;
        return true;
    }

    private String truncateAtSentence(String text, int max) {
        if (text.length() <= max) return text;
        String sub  = text.substring(0, max);
        int    last = Math.max(sub.lastIndexOf('.'), Math.max(sub.lastIndexOf('!'), sub.lastIndexOf('?')));
        return (last > max / 2) ? sub.substring(0, last + 1).trim() : sub.trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // URL structural check — no HTTP request
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isUsableUrl(String url) {
        try {
            URI    u = URI.create(url);
            String s = u.getScheme(), h = u.getHost(), p = u.getPath();
            if (s == null || (!s.equals("http") && !s.equals("https"))) return false;
            if (h == null || h.isBlank()) return false;
            if (p == null || p.length() < 4) return false;
            String host = h.toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) host = host.substring(4);
            for (String blocked : BLOCKED_HOSTS)
                if (host.equals(blocked) || host.endsWith("." + blocked)) return false;
            String path = p.toLowerCase(Locale.ROOT);
            if (path.contains("/tag/") || path.contains("/search")
                    || path.contains("/category/")
                    || path.contains("?s=") || path.contains("?q=")) return false;
            return true;
        } catch (Exception e) { return false; }
    }

    private String domain(String url) {
        try { return URI.create(url).getHost(); } catch (Exception e) { return null; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prompt — always ask for REQUEST_COUNT items so we have buffer for dedup
    // ─────────────────────────────────────────────────────────────────────────

    private String buildPrompt(String topic) {
        String today = LocalDate.now().toString();
        return "You are an AI assistant generating SMS news alerts for RESPONDPH, " +
                "a disaster-response and barangay management app for Iloilo City, Philippines.\n\n" +

                "TASK: Use Google Search grounding to find " + REQUEST_COUNT + " REAL, CURRENTLY LIVE " +
                "news articles about '" + topic + "' from Iloilo City, " +
                "published on " + today + " or within the last 3 days.\n\n" +

                "OUTPUT FORMAT — output ONLY the numbered list, nothing else:\n" +
                "{N}. {Hiligaynon SMS text} (Source: {FULL article URL})\n\n" +

                "STRICT RULES:\n" +
                "1. SMS text must be 280–320 characters (count carefully).\n" +
                "2. Write 2–3 complete sentences in Hiligaynon. Never cut a sentence mid-way.\n" +
                "3. Do NOT embed the URL inside the SMS text.\n" +
                "4. Source URL must be a specific article path — never a homepage, tag, or category page.\n" +
                "5. Source URL must be real and currently accessible — confirmed via Google Search.\n" +
                "6. NEVER fabricate or guess a URL. Skip a story rather than invent a link.\n" +
                "7. Each item must be a distinct story from a distinct news domain.\n" +
                "8. Fully complete each line before starting the next.\n" +
                "9. Output EXACTLY " + REQUEST_COUNT + " items. No preamble, no markdown, no blank lines.\n";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Config — always enable Google Search grounding
    // ─────────────────────────────────────────────────────────────────────────

    private GenerateContentConfig buildConfig() {
        return GenerateContentConfig.builder()
                .tools(List.of(
                        Tool.builder().googleSearch(GoogleSearch.builder().build()).build()))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Buffer / streaming helpers
    // ─────────────────────────────────────────────────────────────────────────

    private int countComplete(String buffer) {
        if (buffer == null || buffer.isBlank()) return 0;
        Matcher m = COMPLETE_ITEM.matcher(buffer);
        int n = 0; while (m.find()) n++; return n;
    }

    private int countLines(String buffer) {
        if (buffer == null || buffer.isEmpty()) return 0;
        return buffer.split("\\r?\\n", -1).length;
    }

    private String tail(String buffer) {
        if (buffer == null || buffer.isBlank()) return "";
        String t = buffer.length() > 120 ? buffer.substring(buffer.length() - 120) : buffer;
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
        try { return r == null ? null : r.text(); }
        catch (Exception e) { LOG.fine("[NewsGen] safeText err: " + e.getMessage()); return null; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Grounding metadata extraction
    // ─────────────────────────────────────────────────────────────────────────

    private static class GroundingStatus { final String label; GroundingStatus(String l) { label = l; } }

    private GroundingStatus extractGroundingStatus(GenerateContentResponse chunk) {
        try {
            if (chunk == null) return null;
            List<Candidate> cands = chunk.candidates().orElse(null);
            if (cands == null || cands.isEmpty()) return null;
            Candidate c = cands.get(0); if (c == null) return null;

            GroundingMetadata meta = c.groundingMetadata().orElse(null);
            if (meta == null) return null;

            // Prefer grounding chunks (pages being read) over search-query labels
            List<GroundingChunk> gchunks = meta.groundingChunks().orElse(null);
            if (gchunks != null && !gchunks.isEmpty()) {
                for (int i = gchunks.size() - 1; i >= 0; i--) {
                    GroundingChunk gc = gchunks.get(i); if (gc == null) continue;
                    var web = gc.web().orElse(null); if (web == null) continue;
                    String title = web.title().orElse(null);
                    String uri   = web.uri().orElse(null);
                    if (title != null && !title.isBlank()) {
                        String d = title.length() > 52 ? title.substring(0, 49) + "…" : title;
                        return new GroundingStatus("📄 Reading: " + d);
                    }
                    if (uri != null && !uri.isBlank()) {
                        try {
                            String host = URI.create(uri).getHost();
                            if (host != null) return new GroundingStatus("📄 Reading: " + host);
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
        } catch (Exception e) { return null; }
    }
}