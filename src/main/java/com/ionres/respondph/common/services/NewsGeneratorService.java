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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NewsGeneratorService {

    private static final String ENV_API_KEY = "GEMINI_API_KEY";
    private static final String MODEL_ID    = "gemini-3-pro-preview";

    /** How many items we ask the AI to generate (cast a wide net). */
    private static final int REQUEST_COUNT = 10;

    /** How many items we want to deliver to the caller. */
    private static final int TARGET = 5;

    /**
     * Categories that search Philippines-wide instead of Iloilo City.
     * Must match NATIONAL_CATEGORIES in SendSMSController.
     */
    private static final Set<String> NATIONAL_CATEGORIES = Set.of(
            "national news", "politics", "health news", "law"
    );

    /**
     * Minimum SMS text length. Deliberately loose so items the AI writes
     * slightly shorter than ideal still pass. Ranking by length ensures
     * quality floats to the top.
     */
    private static final int MIN_LEN = 280;

    /** Hard cap — anything above this is truncated at a sentence boundary. */
    private static final int MAX_LEN = 320;

    // Detects a fully-completed item line in the stream buffer.
    // Used only for live progress bar — does NOT gate final parse.
    private static final Pattern COMPLETE_ITEM = Pattern.compile(
            "(?m)^\\s*(?:\\d+\\s*[\\.)]\\s*)?.+?\\(\\s*Source\\s*:\\s*https?://[^\\s)]{10,}\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private final Client client;

    // ── Cancellation ─────────────────────────────────────────────────────────
    private final AtomicBoolean           cancelRequested = new AtomicBoolean(false);
    private final AtomicReference<Thread> streamThread    = new AtomicReference<>(null);

    // ─────────────────────────────────────────────────────────────────────────
    // NESTED RECORD
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Represents a single news item: SMS text (Hiligaynon/English) + source URL.
     */
    public record NewsItem(String smsText, String url) {}

    // ─────────────────────────────────────────────────────────────────────────

    public NewsGeneratorService() {
        String apiKey = System.getenv(ENV_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing environment variable " + ENV_API_KEY);
        }
        this.client = Client.builder().apiKey(apiKey.trim()).build();
    }

    /**
     * Cancels the in-progress generateNewsHeadlines() call immediately.
     *
     * Two-pronged:
     *   1. Sets cancelRequested so the loop exits on the next iteration.
     *   2. Interrupts the stream thread so a blocking iter.next() unblocks
     *      instantly (without this, cancel can wait 5–30 s for next chunk).
     */
    public void cancelCurrentGeneration() {
        cancelRequested.set(true);
        Thread t = streamThread.get();
        if (t != null) t.interrupt();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Streams news from Gemini with Google Search grounding.
     *
     * Strategy:
     *   • Ask the AI for REQUEST_COUNT (10) items so we have plenty of
     *     candidates even when some fail validation.
     *   • Accumulate every streamed chunk into a buffer.
     *   • Parse ALL item lines from the final buffer — no limit during parse.
     *   • Filter: valid full-article URL + at least 2 sentences + MIN_LEN chars.
     *   • Rank survivors by SMS text length (longer = more complete) and take
     *     the best TARGET (5).
     *
     * @param topic      Search keywords (e.g., "local", "disaster", "weather").
     * @param category   Category for determining geographic scope. If in
     *                   NATIONAL_CATEGORIES, searches Philippines-wide;
     *                   otherwise searches Iloilo City + surrounding areas.
     * @param onProgress (progress 0–1, status label) callback for the UI.
     * @return CompletableFuture that completes with up to TARGET NewsItems.
     */
    public CompletableFuture<List<NewsItem>> generateNewsHeadlines(
            String topic,
            String category,
            BiConsumer<Double, String> onProgress) {

        cancelRequested.set(false);

        return CompletableFuture.supplyAsync(() -> {

            streamThread.set(Thread.currentThread());

            long startMs = System.currentTimeMillis();

            AtomicInteger confirmedItems = new AtomicInteger(0);
            AtomicBoolean done           = new AtomicBoolean(false);

            // ── Elapsed ticker ────────────────────────────────────────────────
            ScheduledExecutorService ticker =
                    Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "news-elapsed-ticker");
                        t.setDaemon(true);
                        return t;
                    });

            AtomicReference<String> lastQueryRef = new AtomicReference<>(null);

            ScheduledFuture<?> tick = ticker.scheduleAtFixedRate(() -> {
                if (done.get()) return;
                int    n       = confirmedItems.get();
                long   elapsed = (System.currentTimeMillis() - startMs) / 1000;
                double bar     = (double) n / REQUEST_COUNT;
                String q       = lastQueryRef.get();
                String label;
                if (n == 0) {
                    label = q != null
                            ? q + " (" + elapsed + "s)"
                            : "Searching news sources… (" + elapsed + "s)";
                } else {
                    label = n + " of " + REQUEST_COUNT + " fetched (" + elapsed + "s)";
                }
                emit(onProgress, bar, label);
            }, 1, 1, TimeUnit.SECONDS);

            emit(onProgress, 0.0, "Waiting for AI…");

            // Determine geographic scope from category
            boolean isNational = isNationalCategory(category);
            String geoScope = isNational ? "Philippines" : "Iloilo City";

            String prompt = buildPrompt(topic, category, geoScope, LocalDate.now().toString());
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .tools(Tool.builder()
                            .googleSearch(GoogleSearch.builder().build())
                            .build())
                    .build();

            StringBuilder buffer         = new StringBuilder();
            int           seenInBuffer   = 0;
            boolean       firstChunkSeen = false;
            String        lastSearchQuery = null;

            try {
                Iterable<GenerateContentResponse> stream =
                        client.models.generateContentStream(MODEL_ID, prompt, config);

                // Explicit Iterator:
                //   a) Skip individual malformed JSON chunks without aborting.
                //   b) Detect thread interruption from cancelCurrentGeneration().
                Iterator<GenerateContentResponse> iter = stream.iterator();

                while (iter.hasNext()) {

                    // Fast-path cancel check (no blocking)
                    if (cancelRequested.get()) {
                        System.out.println("[AI] Cancelled between chunks.");
                        break;
                    }

                    // Blocks until next chunk OR thread is interrupted.
                    GenerateContentResponse chunk;
                    try {
                        chunk = iter.next();
                    } catch (Exception chunkEx) {
                        // Interrupted by cancelCurrentGeneration()
                        if (cancelRequested.get() || Thread.interrupted()) {
                            System.out.println("[AI] Stream interrupted by cancellation.");
                            break;
                        }
                        // Malformed / truncated JSON frame — skip this chunk only
                        String msg = chunkEx.getMessage() != null ? chunkEx.getMessage() : "";
                        boolean isJsonError = msg.contains("JSON")
                                || msg.contains("parse")
                                || msg.contains("EOF")
                                || msg.contains("end-of-input");
                        if (isJsonError) {
                            System.err.println("[AI] Skipping malformed chunk: " + msg);
                            continue;
                        }
                        throw chunkEx;
                    }

                    // ── Grounding phase ───────────────────────────────────────
                    GroundingStatus gs = extractGroundingStatus(chunk);
                    if (gs != null && !gs.label.equals(lastSearchQuery)) {
                        lastSearchQuery = gs.label;
                        lastQueryRef.set(gs.label);
                        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                        emit(onProgress, (double) seenInBuffer / REQUEST_COUNT,
                                gs.label + " (" + elapsed + "s)");
                        System.out.println("[AI] " + gs.label);
                    }

                    String piece = safeText(chunk);
                    if (piece == null || piece.isEmpty()) continue;

                    buffer.append(piece);

                    if (!firstChunkSeen) {
                        firstChunkSeen = true;
                        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                        emit(onProgress, 0.0,
                                "Writing news… (" + elapsed + "s)\n▶ " + chunkPreview(buffer.toString()));
                    }

                    // Progress bar: advance on each fully-completed item line
                    int nowComplete = countCompleteItems(buffer.toString());
                    if (nowComplete > seenInBuffer) {
                        seenInBuffer = nowComplete;
                        confirmedItems.set(seenInBuffer);

                        double bar   = (double) seenInBuffer / REQUEST_COUNT;
                        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                        emit(onProgress, bar,
                                seenInBuffer + " of " + REQUEST_COUNT + " fetched (" + elapsed + "s)\n▶ "
                                        + chunkPreview(buffer.toString()));

                        System.out.println("[AI] Stream: item " + seenInBuffer + "/" + REQUEST_COUNT + " confirmed");
                    } else {
                        double bar   = (double) seenInBuffer / REQUEST_COUNT;
                        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                        String header = seenInBuffer == 0
                                ? "Writing news… (" + elapsed + "s)"
                                : seenInBuffer + " of " + REQUEST_COUNT + " fetched (" + elapsed + "s)";
                        emit(onProgress, bar, header + "\n▶ " + chunkPreview(buffer.toString()));
                    }
                }

            } catch (Exception streamEx) {
                if (cancelRequested.get()) {
                    System.out.println("[AI] Cancelled; skipping fallback.");
                } else {
                    System.err.println("[AI] Stream failed, blocking fallback: " + streamEx.getMessage());
                    streamEx.printStackTrace();
                    try {
                        emit(onProgress, (double) confirmedItems.get() / REQUEST_COUNT, "Reconnecting…");
                        GenerateContentResponse resp =
                                client.models.generateContent(MODEL_ID, prompt, config);
                        String raw = resp != null && resp.text() != null ? resp.text().trim() : null;
                        if (raw != null && !raw.isEmpty()) {
                            buffer.append(raw);
                            int nowComplete = countCompleteItems(buffer.toString());
                            if (nowComplete > seenInBuffer) {
                                seenInBuffer = nowComplete;
                                confirmedItems.set(seenInBuffer);
                                long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                                emit(onProgress, (double) seenInBuffer / REQUEST_COUNT,
                                        seenInBuffer + " of " + REQUEST_COUNT + " fetched (" + elapsed + "s)");
                            }
                        }
                    } catch (Exception blockEx) {
                        System.err.println("[AI] Fallback also failed: " + blockEx.getMessage());
                        blockEx.printStackTrace();
                    }
                }
            } finally {
                streamThread.set(null);
                done.set(true);
                tick.cancel(false);
                ticker.shutdownNow();
                Thread.interrupted(); // clear interrupt flag
            }

            // ── Return empty on cancellation ──────────────────────────────────
            if (cancelRequested.get()) {
                emit(onProgress, 1.0, "Cancelled.");
                return new ArrayList<>();
            }

            // ── Parse + select best 5 ─────────────────────────────────────────
            long totalMs = System.currentTimeMillis() - startMs;
            emit(onProgress,
                    (double) confirmedItems.get() / REQUEST_COUNT,
                    "Validating articles… (" + totalMs / 1000 + "s)");

            String raw = buffer.toString()
                    .replaceAll("[ \\t]+", " ")
                    .trim();

            System.out.println("\n=== AI RAW RESPONSE ===");
            System.out.println(raw.isEmpty() ? "(empty)" : raw);
            System.out.println("=======================\n");

            // Parse ALL candidate items — no limit
            List<NewsItem> allCandidates = parseAllCandidates(raw);

            System.out.printf("[AI] Raw candidates parsed: %d%n", allCandidates.size());
            for (int i = 0; i < allCandidates.size(); i++) {
                System.out.printf("  [%d] len=%d url=%s%n",
                        i + 1,
                        allCandidates.get(i).smsText().length(),
                        allCandidates.get(i).url());
            }

            // Pick the best TARGET items
            List<NewsItem> best = selectBest(allCandidates, TARGET);

            long totalSec = totalMs / 1000;
            emit(onProgress, 1.0,
                    "Done — " + best.size() + " of " + TARGET + " items in " + totalSec + "s");

            System.out.printf("[AI] Final delivered: %d/%d in %d ms%n", best.size(), TARGET, totalMs);
            return best;
        });
    }

    private boolean isNationalCategory(String category) {
        if (category == null || category.isBlank()) return false;
        return NATIONAL_CATEGORIES.contains(category.toLowerCase(java.util.Locale.ROOT).trim());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARSE ALL CANDIDATES (no hard limit, lenient length)
    // ─────────────────────────────────────────────────────────────────────────

    private List<NewsItem> parseAllCandidates(String raw) {
        List<NewsItem> candidates = new ArrayList<>();
        if (raw == null || raw.isBlank()) return candidates;

        Pattern linePattern = Pattern.compile(
                "^\\s*(?:\\d+\\s*[\\.)]\\s*)?(.+?)\\s*\\(\\s*Source\\s*:\\s*(https?://[^\\s)]+)\\s*\\)\\s*$",
                Pattern.CASE_INSENSITIVE
        );

        for (String line : raw.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            Matcher m = linePattern.matcher(trimmed);
            if (!m.matches()) continue;

            String smsText = m.group(1).trim();
            String url     = m.group(2).trim();

            // 1. URL must be a real article path
            if (!isValidFullArticleUrl(url)) {
                System.out.println("[AI] Rejected (bad URL): " + url);
                continue;
            }

            // 2. Clean SMS text: strip accidentally embedded URLs
            smsText = smsText.replaceAll("https?://\\S+", "")
                    .replaceAll("\\.{2,}", ".")
                    .replaceAll("\\s+", " ")
                    .trim();

            // 3. Ensure ends with sentence-ending punctuation
            if (!smsText.isEmpty() && !smsText.matches(".*[.!?]$")) {
                smsText += ".";
            }

            // 4. Minimum length
            if (smsText.length() < MIN_LEN) {
                System.out.println("[AI] Rejected (too short, " + smsText.length() + " chars): "
                        + smsText.substring(0, Math.min(60, smsText.length())));
                continue;
            }

            // 5. Must have at least 2 sentences
            if (countSentences(smsText) < 2) {
                System.out.println("[AI] Rejected (< 2 sentences): "
                        + smsText.substring(0, Math.min(60, smsText.length())));
                continue;
            }

            // 6. Hard cap — truncate at sentence boundary if over MAX_LEN
            if (smsText.length() > MAX_LEN) {
                smsText = truncateAtSentence(smsText, MAX_LEN);
            }

            candidates.add(new NewsItem(smsText, url));
        }

        return candidates;
    }

    private List<NewsItem> selectBest(List<NewsItem> candidates, int target) {
        if (candidates.isEmpty()) return new ArrayList<>();

        // Score: distance from ideal 320-char length
        // Bonus: items inside [300, 320] get score 0 (perfect)
        Comparator<NewsItem> byScore = Comparator.comparingInt(item -> {
            int len = item.smsText().length();
            if (len >= 300 && len <= 320) return 0;
            return Math.abs(len - 320);
        });

        List<NewsItem> sorted = candidates.stream()
                .sorted(byScore)
                .collect(Collectors.toList());

        // De-duplicate by domain — keep only first item per domain
        List<NewsItem> deduped      = new ArrayList<>();
        List<String>   seenDomains  = new ArrayList<>();

        for (NewsItem item : sorted) {
            String domain = extractDomain(item.url());
            if (domain != null && seenDomains.contains(domain)) {
                System.out.println("[AI] Skipping duplicate domain: " + domain);
                continue;
            }
            if (domain != null) seenDomains.add(domain);
            deduped.add(item);
            if (deduped.size() >= target) break;
        }

        // If de-duplication removed too many, fill back from sorted list
        if (deduped.size() < target) {
            for (NewsItem item : sorted) {
                if (!deduped.contains(item)) {
                    deduped.add(item);
                    if (deduped.size() >= target) break;
                }
            }
        }

        System.out.println("[AI] Selected " + deduped.size() + " items after ranking + dedup:");
        for (int i = 0; i < deduped.size(); i++) {
            System.out.printf("  #%d len=%d  %s%n",
                    i + 1,
                    deduped.get(i).smsText().length(),
                    deduped.get(i).url());
        }

        return deduped;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STREAM PROGRESS HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private int countCompleteItems(String buffer) {
        if (buffer == null || buffer.isBlank()) return 0;
        Matcher m = COMPLETE_ITEM.matcher(buffer);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    private String buildPrompt(String topic, String category, String geoScope, String today) {
        return
                "You are an SMS news generator for " + geoScope + ", Philippines.\n" +
                        "Use Google Search grounding to find REAL, VERIFIED, CURRENTLY LIVE article URLs.\n" +
                        "News must come from " + geoScope + ". Prefer today (" + today + ") or the last 3 days.\n" +
                        "Topic: " + topic + "\n" +
                        "Category: " + category + "\n\n" +

                        "Return EXACTLY " + REQUEST_COUNT + " items. ONE item per line. " +
                        "NO intro. NO blank lines. NO markdown. NO commentary.\n\n" +

                        "STRICT LINE FORMAT:\n" +
                        "{number}. {Hiligaynon SMS text, 300-320 characters, 2-3 complete sentences, no ellipsis} " +
                        "(Source: {FULL article URL including path})\n\n" +

                        "RULES — violating any = item will be discarded:\n" +
                        "1. SMS text must be 300–320 characters (count carefully).\n" +
                        "2. 2–3 complete sentences. No cut-off. No '...'.\n" +
                        "3. Hiligaynon only. English only if no Hiligaynon equivalent exists.\n" +
                        "4. Do NOT put the URL inside the SMS text.\n" +
                        "5. Source URL must be a full article path (not just homepage).\n" +
                        "6. Source URL must be real and currently accessible via Google Search.\n" +
                        "7. NEVER fabricate or guess URLs. Skip that story if unsure.\n" +
                        "8. No extra lines or text outside the format.\n" +
                        "9. Complete each item fully before starting the next.\n" +
                        "10. All " + REQUEST_COUNT + " items must be distinct stories with distinct URLs.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String chunkPreview(String bufferStr) {
        if (bufferStr == null || bufferStr.isBlank()) return "";
        String tail = bufferStr.length() > 80
                ? bufferStr.substring(bufferStr.length() - 80)
                : bufferStr;
        int firstSpace = tail.indexOf(' ');
        if (firstSpace > 0 && firstSpace < 15) tail = tail.substring(firstSpace + 1);
        tail = tail.replaceAll("\\s+", " ").trim();
        return tail.isEmpty() ? "" : "…" + tail;
    }

    private boolean isValidFullArticleUrl(String url) {
        try {
            URI u = URI.create(url);
            String s = u.getScheme(), h = u.getHost(), p = u.getPath();
            if (s == null || (!s.equalsIgnoreCase("http") && !s.equalsIgnoreCase("https"))) return false;
            if (h == null || h.isBlank()) return false;
            if (p == null || p.isBlank() || "/".equals(p) || p.length() < 2) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractDomain(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Truncates text at the last sentence boundary at or before maxLen.
     * Falls back to hard cut if no boundary found.
     */
    private String truncateAtSentence(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        String sub = text.substring(0, maxLen);
        int lastDot = Math.max(sub.lastIndexOf('.'), Math.max(sub.lastIndexOf('!'), sub.lastIndexOf('?')));
        if (lastDot > maxLen / 2) {
            return sub.substring(0, lastDot + 1).trim();
        }
        return sub.trim();
    }

    private int countSentences(String text) {
        if (text == null || text.isBlank()) return 0;
        int n = 0;
        for (String part : text.split("[.!?]+"))
            if (part.trim().length() > 10) n++;
        return n;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUNDING STATUS
    // ─────────────────────────────────────────────────────────────────────────

    private static class GroundingStatus {
        final String label;
        GroundingStatus(String label) { this.label = label; }
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
                        String display = title.length() > 55 ? title.substring(0, 52) + "…" : title;
                        return new GroundingStatus("📄 Reading: " + display);
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
                    String display = q.length() > 55 ? q.substring(0, 52) + "…" : q;
                    return new GroundingStatus("🔍 Searching: \"" + display + "\"");
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void emit(BiConsumer<Double, String> cb, double p, String s) {
        if (cb != null) cb.accept(Math.max(0.0, Math.min(1.0, p)), s);
    }

    private String safeText(GenerateContentResponse r) {
        try {
            if (r == null) return null;
            // Do NOT trim individual chunks — removes boundary whitespace and
            // merges words across chunks. Final buffer is trimmed once instead.
            return r.text();
        } catch (Exception e) {
            return null;
        }
    }
}