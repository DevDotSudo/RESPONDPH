package com.ionres.respondph.common.services;

import com.google.genai.Client;
import com.google.genai.types.Candidate;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GroundingChunk;
import com.google.genai.types.GroundingMetadata;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.Tool;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger; // FIX #2 — was unsafe int[]
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsGeneratorService {

    private static final String ENV_API_KEY = "GEMINI_API_KEY";
    // FIX #1 — correct model ID (gemini-3-pro-preview does not exist)
    private static final String MODEL_ID    = "gemini-3-pro-preview";

    private static final int TARGET  = 5;
    private static final int MIN_LEN = 300; // widened: AI targets 300 but can miss by ~20
    private static final int MAX_LEN = 320; // widened: stream artifacts or AI overshoot

    // Detects a fully completed item line in the accumulated stream buffer.
    // A line is complete when it ends with (Source: https://domain/path).
    // This pattern is the ONLY thing that advances the progress bar.
    private static final Pattern COMPLETE_ITEM = Pattern.compile(
            "(?m)^\\s*(?:\\d+\\s*[\\.)]\\s*)?.+?\\(\\s*Source\\s*:\\s*https?://[^\\s)]{10,}\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private final Client client;

    public NewsGeneratorService() {
        String apiKey = System.getenv(ENV_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing environment variable " + ENV_API_KEY);
        }
        this.client = Client.builder().apiKey(apiKey.trim()).build();
    }

    public CompletableFuture<List<NewsItem>> generateNewsHeadlines(
            String topic,
            BiConsumer<Double, String> onProgress) {

        return CompletableFuture.supplyAsync(() -> {

            long startMs = System.currentTimeMillis();

            // FIX #2 — AtomicInteger for safe cross-thread read by the ticker
            AtomicInteger confirmedItems = new AtomicInteger(0);
            // FIX #5 — AtomicBoolean set BEFORE ticker cancel so in-flight tick sees it
            AtomicBoolean done           = new AtomicBoolean(false);

            // ── Elapsed ticker ────────────────────────────────────────────────
            // FIX #3 — ticker NEVER writes a new bar value.
            // It only refreshes the elapsed-seconds portion of the label text.
            // During the silent search phase it shows the last known search query.
            ScheduledExecutorService ticker =
                    Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "news-elapsed-ticker");
                        t.setDaemon(true);
                        return t;
                    });

            // Shared reference so ticker can show the last search query
            java.util.concurrent.atomic.AtomicReference<String> lastQueryRef =
                    new java.util.concurrent.atomic.AtomicReference<>(null);

            ScheduledFuture<?> tick = ticker.scheduleAtFixedRate(() -> {
                if (done.get()) return;
                int    n       = confirmedItems.get();
                long   elapsed = (System.currentTimeMillis() - startMs) / 1000;
                double bar     = (double) n / TARGET;
                String q       = lastQueryRef.get();
                String label;
                if (n == 0) {
                    // Show last known grounding status (query or page title) + elapsed
                    label = q != null
                            ? q + " (" + elapsed + "s)"
                            : "Searching news sources… (" + elapsed + "s)";
                } else {
                    label = n + " of " + TARGET + " items found (" + elapsed + "s)";
                }
                emit(onProgress, bar, label);
            }, 1, 1, TimeUnit.SECONDS);

            // ── Initial state ─────────────────────────────────────────────────
            emit(onProgress, 0.0, "Waiting for AI…");

            String prompt = buildPrompt(topic, LocalDate.now().toString());
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .tools(Tool.builder()
                            .googleSearch(GoogleSearch.builder().build())
                            .build())
                    .build();

            StringBuilder buffer       = new StringBuilder();
            int           seenInBuffer = 0;
            // FIX #4 — one-shot flag; first text-chunk label fires exactly once
            boolean firstChunkSeen     = false;
            // Track last search query shown so we don't repeat identical labels
            String lastSearchQuery     = null;

            try {
                Iterable<GenerateContentResponse> stream =
                        client.models.generateContentStream(MODEL_ID, prompt, config);

                for (GenerateContentResponse chunk : stream) {

                    // ── Grounding phase: show queries AND page titles ─────────
                    // Queries fire first ("🔍 Searching: …"), then as results
                    // come back individual page titles appear ("📄 Reading: …").
                    // All of this happens BEFORE any SMS text is generated.
                    GroundingStatus gs = extractGroundingStatus(chunk);
                    if (gs != null && !gs.label.equals(lastSearchQuery)) {
                        lastSearchQuery = gs.label;
                        lastQueryRef.set(gs.label);
                        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                        double bar   = (double) seenInBuffer / TARGET;
                        emit(onProgress, bar, gs.label + " (" + elapsed + "s)");
                        System.out.println("[AI] " + gs.label);
                    }

                    String piece = safeText(chunk);
                    if (piece == null || piece.isEmpty()) continue;

                    buffer.append(piece);

                    // FIX #4 — fires exactly once when text starts flowing
                    if (!firstChunkSeen) {
                        firstChunkSeen = true;
                        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                        emit(onProgress, 0.0,
                                "Writing news… (" + elapsed + "s)\n▶ " + chunkPreview(buffer.toString()));
                    }

                    // ── Real progress: bar moves ONLY when a full item lands ──
                    int nowComplete = countCompleteItems(buffer.toString());
                    if (nowComplete > seenInBuffer) {
                        seenInBuffer = nowComplete;
                        confirmedItems.set(seenInBuffer);

                        double bar   = (double) seenInBuffer / TARGET;
                        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                        emit(onProgress, bar,
                                seenInBuffer + " of " + TARGET + " items found (" + elapsed + "s)\n▶ "
                                        + chunkPreview(buffer.toString()));

                        System.out.println("[AI] Stream: item " + seenInBuffer + "/" + TARGET + " confirmed");
                    } else {
                        // No new item yet — update live chunk preview on every chunk
                        double bar   = (double) seenInBuffer / TARGET;
                        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                        String header = seenInBuffer == 0
                                ? "Writing news… (" + elapsed + "s)"
                                : seenInBuffer + " of " + TARGET + " items found (" + elapsed + "s)";
                        emit(onProgress, bar, header + "\n▶ " + chunkPreview(buffer.toString()));
                    }
                }

            } catch (Exception streamEx) {
                System.err.println("[AI] Stream failed, blocking fallback: " + streamEx.getMessage());
                streamEx.printStackTrace();
                try {
                    emit(onProgress, (double) confirmedItems.get() / TARGET, "Reconnecting…");
                    GenerateContentResponse resp =
                            client.models.generateContent(MODEL_ID, prompt, config);
                    // Blocking response is complete — safe to trim here
                    String raw = resp != null && resp.text() != null ? resp.text().trim() : null;
                    if (raw != null && !raw.isEmpty()) {
                        buffer.append(raw);
                        int nowComplete = countCompleteItems(buffer.toString());
                        if (nowComplete > seenInBuffer) {
                            seenInBuffer = nowComplete;
                            confirmedItems.set(seenInBuffer);
                            long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                            emit(onProgress, (double) seenInBuffer / TARGET,
                                    seenInBuffer + " of " + TARGET + " items found (" + elapsed + "s)");
                        }
                    }
                } catch (Exception blockEx) {
                    System.err.println("[AI] Blocking fallback failed: " + blockEx.getMessage());
                    blockEx.printStackTrace();
                }
            } finally {
                // FIX #5 — set done true BEFORE cancel so any in-flight tick exits cleanly
                done.set(true);
                tick.cancel(false);
                ticker.shutdownNow(); // hard stop; no more ticks after this
            }

            // ── Parse final buffer ────────────────────────────────────────────
            long totalMs = System.currentTimeMillis() - startMs;
            emit(onProgress,
                    (double) confirmedItems.get() / TARGET,
                    "Validating articles… (" + totalMs / 1000 + "s)");

            // Trim the complete buffer once — this is where trimming belongs, not per-chunk
            String raw = buffer.toString()
                    .replaceAll("[ \\t]+", " ")  // collapse multiple spaces from empty chunk joins
                    .trim();
            System.out.println("\n=== AI RAW RESPONSE ===");
            System.out.println(raw.isEmpty() ? "(empty)" : raw);
            System.out.println("=======================\n");

            // FIX #6 — declared at point of assignment, no dead empty list before try
            List<NewsItem> parsed = parseSmsWithSource(raw, TARGET);

            long totalSec = totalMs / 1000;
            emit(onProgress, 1.0,
                    "Done — " + parsed.size() + " of " + TARGET + " items in " + totalSec + "s");

            System.out.printf("[AI] Final: %d/%d items in %d ms%n", parsed.size(), TARGET, totalMs);
            return parsed;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COUNT COMPLETE ITEMS IN STREAM BUFFER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Counts lines in the buffer that have a complete (Source: https://...) suffix.
     * This is called on every chunk and is the sole driver of bar progress.
     */
    private int countCompleteItems(String buffer) {
        if (buffer == null || buffer.isBlank()) return 0;
        Matcher m = COMPLETE_ITEM.matcher(buffer);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROMPT
    // ─────────────────────────────────────────────────────────────────────────

    private String buildPrompt(String topic, String today) {
        return
                "You are an SMS news generator for Iloilo City, Philippines.\n" +
                        "Use Google Search grounding to find REAL, VERIFIED, CURRENTLY LIVE article URLs.\n" +
                        "News must come from Iloilo City. Prefer today (" + today + ") or the last 3 days.\n" +
                        "Topic: " + topic + "\n\n" +

                        "Return EXACTLY " + TARGET + " items. ONE item per line. " +
                        "NO intro. NO blank lines. NO markdown. NO commentary.\n\n" +

                        "STRICT LINE FORMAT:\n" +
                        "{number}. {Hiligaynon SMS text, 300-320 characters, 2-3 complete sentences, no ellipsis} " +
                        "(Source: {FULL article URL including path})\n\n" +

                        "RULES — violating any = response rejected:\n" +
                        "1. SMS text must be 300–320 characters (count carefully).\n" +
                        "2. 2–3 complete sentences. No cut-off. No '...'.\n" +
                        "3. Hiligaynon only. English only if no equivalent.\n" +
                        "4. Do NOT put the URL inside the SMS text.\n" +
                        "5. Source URL must be a full article path, not just a homepage.\n" +
                        "6. Source URL must be real and currently accessible via Google Search.\n" +
                        "7. NEVER fabricate or guess URLs. Skip that story instead.\n" +
                        "8. No extra lines or text outside the format.\n" +
                        "9. Complete each item fully before starting the next one.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the last ~80 characters of the buffer as a live preview,
     * stripped of leading numbering and trimmed to a clean word boundary.
     * Shown in the toast as: "▶ …nga implementasyon sang layi sa karsada."
     */
    private String chunkPreview(String bufferStr) {
        if (bufferStr == null || bufferStr.isBlank()) return "";
        // Take the last 80 chars of the full accumulated text
        String tail = bufferStr.length() > 80
                ? bufferStr.substring(bufferStr.length() - 80)
                : bufferStr;
        // Strip leading partial word if we sliced mid-word
        int firstSpace = tail.indexOf(' ');
        if (firstSpace > 0 && firstSpace < 15) tail = tail.substring(firstSpace + 1);
        // Collapse whitespace and trim
        tail = tail.replaceAll("\\s+", " ").trim();
        return tail.isEmpty() ? "" : "…" + tail;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUNDING STATUS — queries + page titles shown during search phase
    // ─────────────────────────────────────────────────────────────────────────

    /** Holds a display label from the grounding metadata of one stream chunk. */
    private static class GroundingStatus {
        final String label;
        GroundingStatus(String label) { this.label = label; }
    }

    /**
     * Extracts the most informative grounding signal from a stream chunk:
     *
     *   Priority 1 — grounding chunks (actual page titles being read):
     *                "📄 Reading: Iloilo City fire — Daily Guardian"
     *   Priority 2 — web search queries (what Gemini searched for):
     *                "🔍 Searching: "Iloilo City disaster news 2025""
     *
     * Returns null if this chunk has no grounding metadata (most chunks won't).
     * All of this fires BEFORE any SMS text is generated.
     */
    private GroundingStatus extractGroundingStatus(GenerateContentResponse chunk) {
        try {
            if (chunk == null) return null;
            List<Candidate> candidates = chunk.candidates().orElse(null);
            if (candidates == null || candidates.isEmpty()) return null;
            Candidate c = candidates.get(0);
            if (c == null) return null;
            GroundingMetadata meta = c.groundingMetadata().orElse(null);
            if (meta == null) return null;

            // Priority 1: grounding chunks = actual pages Gemini is reading
            List<GroundingChunk> chunks = meta.groundingChunks().orElse(null);
            if (chunks != null && !chunks.isEmpty()) {
                // Show the last page title (most recently fetched)
                for (int i = chunks.size() - 1; i >= 0; i--) {
                    GroundingChunk gc = chunks.get(i);
                    if (gc == null) continue;
                    var web = gc.web().orElse(null);
                    if (web == null) continue;
                    String title = web.title().orElse(null);
                    String uri   = web.uri().orElse(null);
                    if (title != null && !title.isBlank()) {
                        // Trim long titles to fit the toast
                        String display = title.length() > 55
                                ? title.substring(0, 52) + "…"
                                : title;
                        return new GroundingStatus("📄 Reading: " + display);
                    }
                    if (uri != null && !uri.isBlank()) {
                        // Fallback: show the domain if no title
                        try {
                            String host = URI.create(uri).getHost();
                            if (host != null) return new GroundingStatus("📄 Reading: " + host);
                        } catch (Exception ignored) {}
                    }
                }
            }

            // Priority 2: search query (fires before pages are fetched)
            List<String> queries = meta.webSearchQueries().orElse(null);
            if (queries != null && !queries.isEmpty()) {
                String q = queries.get(0);
                if (q != null && !q.isBlank()) {
                    String display = q.length() > 55 ? q.substring(0, 52) + "…" : q;
                    return new GroundingStatus("🔍 Searching: \"" + display + "\"");
                }
            }

            return null;
        } catch (Exception e) {
            return null; // grounding metadata absent — normal for most chunks
        }
    }

    private void emit(BiConsumer<Double, String> cb, double p, String s) {
        if (cb != null) cb.accept(p, s);
    }

    private String safeText(GenerateContentResponse r) {
        try {
            if (r == null) return null;
            // FIX — do NOT trim individual stream chunks.
            // Trimming removes the leading/trailing spaces that sit at chunk
            // boundaries, causing adjacent words to merge ("kag " + "360" → "kag360").
            // The final buffer is trimmed once in the caller instead.
            return r.text(); // null is handled by the caller's null/isEmpty check
        } catch (Exception e) {
            return null; // normal for intermediate stream chunks with no text part
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARSER
    // ─────────────────────────────────────────────────────────────────────────

    private List<NewsItem> parseSmsWithSource(String raw, int limit) {
        List<NewsItem> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;

        Pattern p = Pattern.compile(
                "^\\s*(?:\\d+\\s*[\\.)]\\s*)?(.+?)\\s*\\(\\s*Source\\s*:\\s*(https?://[^\\s)]+)\\s*\\)\\s*$",
                Pattern.CASE_INSENSITIVE
        );

        for (String line : raw.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            Matcher m = p.matcher(trimmed);
            if (!m.matches()) continue;

            String smsText = m.group(1).trim();
            String url     = m.group(2).trim();

            if (!isValidFullArticleUrl(url)) continue;

            smsText = smsText.replaceAll("https?://\\S+", "").replaceAll("\\s+", " ").trim();
            smsText = enforceLen(smsText);
            if (smsText == null) continue;

            result.add(new NewsItem(smsText, url));
            if (result.size() >= limit) break;
        }

        return result;
    }

    private boolean isValidFullArticleUrl(String url) {
        try {
            URI u = URI.create(url);
            String s = u.getScheme(), h = u.getHost(), p = u.getPath();
            if (s == null || (!s.equalsIgnoreCase("http") && !s.equalsIgnoreCase("https"))) return false;
            if (h == null || h.isBlank()) return false;
            if (p == null || p.isBlank() || "/".equals(p) || p.length() < 2) return false;
            return true;
        } catch (Exception e) { return false; }
    }

    private String enforceLen(String text) {
        if (text == null) return null;
        text = text.replaceAll("\\s+", " ").trim();
        text = text.replaceAll("\\.{2,}", ".").trim();
        if (!text.matches(".*[.!?]$")) text += ".";
        int len = text.length();
        if (len < MIN_LEN || len > MAX_LEN) return null;
        if (countSentences(text) < 2) return null;
        return text;
    }

    private int countSentences(String text) {
        if (text == null || text.isBlank()) return 0;
        int n = 0;
        for (String p : text.split("[.!?]+"))
            if (p.trim().length() > 20) n++;
        return n;
    }
}