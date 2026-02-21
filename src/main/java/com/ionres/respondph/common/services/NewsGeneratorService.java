package com.ionres.respondph.common.services;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.RawMessageStreamEvent;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NewsGeneratorService {

    private static final String ENV_ANTHROPIC_KEY = "ANTHROPIC_API_KEY";

    private static final Model MODEL_ID = Model.CLAUDE_SONNET_4_5_20250929;

    private static final int REQUEST_COUNT = 10;
    private static final int TARGET = 5;

    private static final Set<String> NATIONAL_CATEGORIES = Set.of(
            "national news", "politics", "health news", "law", "crime"
    );

    private static final Set<String> WEATHER_CATEGORIES = Set.of(
            "weather", "weather news", "weather update"
    );

    private static final int MIN_LEN = 280;
    private static final int MAX_LEN = 320;

    private static final int HTTP_TIMEOUT_SEC = 12;
    private static final int MAX_ARTICLES_PER_SOURCE = 20;
    private static final int MAX_ARTICLES_IN_PROMPT = 15;
    private static final int RSS_SUFFICIENT_THRESHOLD = 3;

    // Progress detection: completed line with ArticleIndex
    private static final Pattern COMPLETE_ITEM = Pattern.compile(
            "(?m)^\\s*(?:\\d+\\s*[\\.)]\\s*)?.+?\\|\\s*Headline:\\s*.+?\\|\\s*ArticleIndex:\\s*\\d+\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    // Parse line: sms | Headline: ... | ArticleIndex: N
    private static final Pattern LINE_FULL = Pattern.compile(
            "^\\s*(?:\\d+\\s*[\\.)]\\s*)?(.+?)\\s*\\|\\s*Headline:\\s*(.+?)\\s*\\|\\s*ArticleIndex:\\s*(\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    // Fallback: sms | Headline: ...
    private static final Pattern LINE_FALLBACK = Pattern.compile(
            "^\\s*(?:\\d+\\s*[\\.)]\\s*)?(.+?)\\s*\\|\\s*Headline:\\s*(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    // RSS regex
    private static final Pattern RSS_ITEM  = Pattern.compile("<item[^>]*>(.*?)</item>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern RSS_TITLE = Pattern.compile(
            "<title[^>]*>\\s*(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?\\s*</title>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern RSS_LINK  = Pattern.compile(
            "(?:<link>|<link[^/]*/?>)\\s*(https?://[^\\s<\"]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RSS_DESC  = Pattern.compile(
            "<description[^>]*>\\s*(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?\\s*</description>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final List<String> LOCAL_RSS = List.of(
            "https://panaynews.net/feed/",
            "https://www.dailyguardian.com.ph/feed/"
    );

    private static final List<String> WEATHER_RSS = List.of(
            "https://data.gmanetwork.com/gno/rss/weather/feed.xml",
            "https://www.weather.gov/rss/"
    );

    private static final List<String> NATIONAL_RSS = List.of(
            "https://data.gmanetwork.com/gno/rss/news/feed.xml",
            "https://abcnews.com/abcnews/politicsheadlines",
            "https://abcnews.com/abcnews/healthheadlines",
            "https://www.philstar.com/rss/headlines"
    );

    private final AnthropicClient client;
    private final HttpClient http;

    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private final AtomicReference<Thread> streamThread = new AtomicReference<>(null);

    public NewsGeneratorService() {
        String apiKey = System.getenv(ENV_ANTHROPIC_KEY);
        if (apiKey == null || apiKey.isBlank())
            throw new IllegalStateException("Missing env var: " + ENV_ANTHROPIC_KEY);

        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey.trim())
                .build();

        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    public void cancelCurrentGeneration() {
        cancelRequested.set(true);
        Thread t = streamThread.get();
        if (t != null) t.interrupt();
    }

    public CompletableFuture<List<NewsItem>> generateNewsHeadlines(
            String topic,
            String category,
            BiConsumer<Double, String> onProgress
    ) {
        cancelRequested.set(false);

        return CompletableFuture.supplyAsync(() -> {

            streamThread.set(Thread.currentThread());

            long startMs = System.currentTimeMillis();
            AtomicInteger confirmedItems = new AtomicInteger(0);
            AtomicBoolean done = new AtomicBoolean(false);

            ScheduledExecutorService ticker =
                    Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "news-elapsed-ticker");
                        t.setDaemon(true);
                        return t;
                    });

            ScheduledFuture<?> tick = ticker.scheduleAtFixedRate(() -> {
                if (done.get()) return;
                int n = confirmedItems.get();
                long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                double bar = (double) n / REQUEST_COUNT;
                String label = (n == 0)
                        ? "Fetching live articles… (" + elapsed + "s)"
                        : n + " of " + REQUEST_COUNT + " drafted (" + elapsed + "s)";
                emit(onProgress, bar, label);
            }, 1, 1, TimeUnit.SECONDS);

            try {
                // ── STEP 1: fetch RSS ─────────────────────────────────────
                emit(onProgress, 0.03, "Fetching live articles (RSS)…");
                CategoryGroup group = resolveGroup(category);
                List<RawArticle> articles = fetchArticlesRssOnly(group, topic);

                System.out.printf("[News] RSS articles after dedup: %d%n", articles.size());

                if (cancelRequested.get()) {
                    emit(onProgress, 1.0, "Cancelled.");
                    return List.of();
                }

                String geoScope = (group == CategoryGroup.NATIONAL) ? "Philippines" : "Iloilo City";
                String today = LocalDate.now().toString();

                boolean hasGrounding = articles.size() >= RSS_SUFFICIENT_THRESHOLD;

                String groundingBlock = buildGroundingBlock(articles);
                String prompt = buildPromptSingleUser(topic, category, geoScope, today, groundingBlock, hasGrounding);

                emit(onProgress, 0.10, hasGrounding ? "Writing from real articles…" : "RSS sparse — writing fallback items…");

                // ── STEP 2: stream Claude ──────────────────────────────────
                MessageCreateParams params = MessageCreateParams.builder()
                        .model(MODEL_ID)
                        .maxTokens(3000L)
                        .addUserMessage(prompt)
                        .build();

                StringBuilder buffer = new StringBuilder();
                int seenInBuffer = 0;
                boolean firstChunkSeen = false;

                try (StreamResponse<RawMessageStreamEvent> stream = client.messages().createStreaming(params)) {
                    Iterator<RawMessageStreamEvent> iter = stream.stream().iterator();

                    while (true) {
                        if (cancelRequested.get()) {
                            System.out.println("[Claude] Cancelled between chunks.");
                            break;
                        }

                        RawMessageStreamEvent event;
                        try {
                            if (!iter.hasNext()) break;
                            event = iter.next();
                        } catch (Exception ex) {
                            if (cancelRequested.get() || Thread.interrupted()) {
                                System.out.println("[Claude] Stream interrupted by cancellation.");
                                break;
                            }
                            throw ex;
                        }

                        String piece = extractTextDelta(event);
                        if (piece == null || piece.isEmpty()) continue;

                        buffer.append(piece);

                        if (!firstChunkSeen) {
                            firstChunkSeen = true;
                            long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                            emit(onProgress, 0.10, "Writing news… (" + elapsed + "s)\n▶ " + chunkPreview(buffer.toString()));
                        }

                        int nowComplete = countCompleteItems(buffer.toString());
                        if (nowComplete > seenInBuffer) {
                            seenInBuffer = nowComplete;
                            confirmedItems.set(seenInBuffer);

                            double bar = Math.min(0.90, (double) seenInBuffer / REQUEST_COUNT);
                            long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                            emit(onProgress, bar,
                                    seenInBuffer + " of " + REQUEST_COUNT + " drafted (" + elapsed + "s)\n▶ "
                                            + chunkPreview(buffer.toString()));

                            System.out.println("[Claude] Stream: item " + seenInBuffer + "/" + REQUEST_COUNT + " confirmed");
                        }
                    }
                }

                if (cancelRequested.get()) {
                    emit(onProgress, 1.0, "Cancelled.");
                    return List.of();
                }

                // ── STEP 3: parse + resolve URLs locally ────────────────────
                emit(onProgress, 0.92, "Validating items…");

                String raw = buffer.toString()
                        .replaceAll("[ \\t]+", " ")
                        .trim();

                System.out.println("\n=== CLAUDE RAW RESPONSE ===");
                System.out.println(raw.isEmpty() ? "(empty)" : raw);
                System.out.println("===========================\n");

                List<NewsItem> candidates = parseAllCandidates(raw, articles);

                System.out.printf("[News] Parsed candidates: %d%n", candidates.size());
                for (int i = 0; i < candidates.size(); i++) {
                    System.out.printf("  [%d] len=%d url=%s%n", i + 1, candidates.get(i).smsText().length(), candidates.get(i).url());
                }

                List<NewsItem> best = selectBest(candidates, TARGET);

                long totalMs = System.currentTimeMillis() - startMs;
                emit(onProgress, 1.0, "Done — " + best.size() + " of " + TARGET + " in " + (totalMs / 1000) + "s");

                return best;

            } catch (Exception e) {
                System.err.println("[News] Error: " + e.getMessage());
                e.printStackTrace();
                emit(onProgress, 1.0, "Error: " + e.getMessage());
                return List.of();
            } finally {
                done.set(true);
                tick.cancel(false);
                ticker.shutdownNow();
                streamThread.set(null);
                Thread.interrupted();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RSS FETCH (LIVE URLS)
    // ─────────────────────────────────────────────────────────────────────────

    private List<RawArticle> fetchArticlesRssOnly(CategoryGroup group, String topic) {
        List<String> sources = switch (group) {
            case LOCAL -> LOCAL_RSS;
            case WEATHER -> WEATHER_RSS;
            case NATIONAL -> NATIONAL_RSS;
        };

        List<CompletableFuture<List<RawArticle>>> futures = sources.stream()
                .map(url -> CompletableFuture.supplyAsync(() -> fetchRss(url)))
                .toList();

        List<RawArticle> all = new ArrayList<>();
        for (var f : futures) {
            try { all.addAll(f.get(HTTP_TIMEOUT_SEC + 3L, TimeUnit.SECONDS)); }
            catch (Exception e) { System.err.println("[News] RSS future error: " + e.getMessage()); }
        }

        return dedup(filterByTopic(all, topic));
    }

    private List<RawArticle> fetchRss(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (compatible; RespondPH-NewsBot/2.0; +https://ionres.com)")
                    .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
                    .timeout(java.time.Duration.ofSeconds(HTTP_TIMEOUT_SEC))
                    .GET().build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                System.err.printf("[News] RSS HTTP %d → %s%n", resp.statusCode(), url);
                return List.of();
            }

            String body = resp.body();
            if (body == null || body.isBlank()) return List.of();

            List<RawArticle> items = parseRssXml(body, url);
            System.out.printf("[News] RSS %s → %d items%n", url, items.size());
            return items;

        } catch (Exception e) {
            System.err.printf("[News] RSS fetch failed [%s]: %s%n", url, e.getMessage());
            return List.of();
        }
    }

    private List<RawArticle> parseRssXml(String xml, String sourceUrl) {
        List<RawArticle> items = new ArrayList<>();
        Matcher itemM = RSS_ITEM.matcher(xml);

        while (itemM.find() && items.size() < MAX_ARTICLES_PER_SOURCE) {
            String block = itemM.group(1);
            String title = cleanHtml(extractFirst(RSS_TITLE, block));
            String link = extractFirst(RSS_LINK, block);
            String desc = cleanHtml(extractFirst(RSS_DESC, block));

            if (title == null || title.isBlank()) continue;
            if (link == null || link.isBlank()) link = sourceUrl;

            link = link.replaceAll("[<\"'\\s].*$", "").trim();

            items.add(new RawArticle(title, link, desc != null ? desc : ""));
        }

        return items;
    }

    private List<RawArticle> filterByTopic(List<RawArticle> articles, String topic) {
        if (topic == null || topic.isBlank() || topic.equalsIgnoreCase("general"))
            return articles;

        String lc = topic.toLowerCase(Locale.ROOT);
        List<RawArticle> filtered = articles.stream()
                .filter(a -> a.title().toLowerCase(Locale.ROOT).contains(lc)
                        || a.description().toLowerCase(Locale.ROOT).contains(lc))
                .toList();

        return filtered.isEmpty() ? articles : filtered;
    }

    private List<RawArticle> dedup(List<RawArticle> articles) {
        Set<String> seen = new LinkedHashSet<>();
        List<RawArticle> unique = new ArrayList<>();
        for (RawArticle a : articles) {
            String key = a.title().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
            if (seen.add(key)) unique.add(a);
        }
        return unique;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROMPT (Claude does NOT browse; uses your RSS list)
    // ─────────────────────────────────────────────────────────────────────────

    private String buildGroundingBlock(List<RawArticle> articles) {
        if (articles.isEmpty()) return "";
        int limit = Math.min(articles.size(), MAX_ARTICLES_IN_PROMPT);

        StringBuilder sb = new StringBuilder();
        sb.append("=== REAL NEWS ARTICLES (GAMITA LANG INI NGA BASEHAN) ===\n\n");
        for (int i = 0; i < limit; i++) {
            RawArticle a = articles.get(i);
            sb.append("ARTICLE ").append(i + 1).append(":\n");
            sb.append("  Title: ").append(a.title()).append("\n");
            sb.append("  URL  : ").append(a.url()).append("\n");
            if (!a.description().isBlank()) {
                String desc = a.description().length() > 300
                        ? a.description().substring(0, 300) + "…"
                        : a.description();
                sb.append("  Desc : ").append(desc).append("\n");
            }
            sb.append("\n");
        }
        sb.append("=== END ARTICLES ===\n\n");
        return sb.toString();
    }

    private String buildPromptSingleUser(
            String topic,
            String category,
            String geoScope,
            String today,
            String groundingBlock,
            boolean hasGrounding
    ) {
        StringBuilder p = new StringBuilder();

        p.append("IKAW ISA KA eksperto nga manunulat sang HILIGAYNON SMS NEWS para sa ")
                .append(geoScope)
                .append(", Philippines.\n\n");

        p.append("IMPORTANTE: WALA IKAW SANG INTERNET ACCESS. ")
                .append("INDI ka mag pangita sang balita. ")
                .append("KUN MAY LISTA sang articles sa idalom, amo lang ina ang imo basehan.\n\n");

        p.append("Tanan nga SMS body dapat PURE HILIGAYNON gid. ")
                .append("INDI mag gamit sang English kag indi mag Tagalog. ")
                .append("Kung may termino nga wala gid Hiligaynon, ilisan sang Hiligaynon nga pagpaathag.\n\n");

        p.append("Subong nga adlaw: ").append(today).append("\n")
                .append("Topic: ").append(topic).append(" | Category: ").append(category).append("\n\n");

        if (hasGrounding && !groundingBlock.isBlank()) {
            p.append(groundingBlock);
            p.append("Baseha ang tagsa ka SMS item STRICTLY sa isa ka lain-lain nga ARTICLE sa lista. ")
                    .append("INDI mag imbento sang facts nga wala sa article.\n\n");
        } else {
            p.append("WALA SANG sapat nga articles. ")
                    .append("Himoa ang items base sa general knowledge, pero indi mag butang sang peke nga detalye.\n\n");
        }

        p.append("HIMOA EXACTLY ").append(REQUEST_COUNT).append(" ka SMS items.\n")
                .append("Tagsa ka item: 2–3 ka kumpleto nga pangungusap.\n")
                .append("Kada SMS body: EXACTLY ").append(MIN_LEN).append("–").append(MAX_LEN).append(" characters.\n")
                .append("BAWAL: ellipsis (…), '...', putol nga pangungusap, kag raw URL sa SMS body.\n\n");

        p.append("STRICT LINE FORMAT (isa lang ka linya kada item, WALAY blank lines):\n")
                .append("{N}. {SMS body} | Headline: {exact article title} | ArticleIndex: {1-based article number}\n\n");

        p.append("RULES:\n")
                .append("1) KADA item dapat lain nga ARTICLE (wala sang repeat).\n")
                .append("2) Ang Headline dapat EXACT pareho sang Title sa article list.\n")
                .append("3) Ang ArticleIndex dapat sakto nga numero sang article nga gin-basehan.\n")
                .append("4) Output ONLY the numbered list. WALA intro. WALA closing. WALA markdown.\n");

        return p.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARSE + URL RESOLUTION (LOCAL)
    // ─────────────────────────────────────────────────────────────────────────

    private List<NewsItem> parseAllCandidates(String raw, List<RawArticle> articles) {
        List<NewsItem> results = new ArrayList<>();
        if (raw == null || raw.isBlank()) return results;

        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;

            Matcher m = LINE_FULL.matcher(t);
            if (m.matches()) {
                String smsText = cleanSmsText(m.group(1));
                String headline = m.group(2).trim();
                int idx = safeInt(m.group(3), -1);

                if (!validateSms(smsText)) continue;

                String url = resolveUrl(idx, headline, articles);
                if (url == null) url = "";
                results.add(new NewsItem(smsText, url));
                continue;
            }

            Matcher fb = LINE_FALLBACK.matcher(t);
            if (fb.matches()) {
                String smsText = cleanSmsText(fb.group(1));
                String headline = fb.group(2).trim();
                if (!validateSms(smsText)) continue;
                String url = resolveByTitle(headline, articles);
                if (url == null) url = "";
                results.add(new NewsItem(smsText, url));
            }
        }

        return results;
    }

    private int safeInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private String resolveUrl(int index, String headline, List<RawArticle> articles) {
        if (index >= 1 && index <= articles.size()) return articles.get(index - 1).url();
        return resolveByTitle(headline, articles);
    }

    private String resolveByTitle(String headline, List<RawArticle> articles) {
        if (articles == null || articles.isEmpty()) return "";
        if (headline == null || headline.isBlank()) return "";

        Set<String> words = Arrays.stream(headline.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(w -> w.length() > 3)
                .collect(Collectors.toSet());

        RawArticle best = null;
        int bestScore = 0;
        for (RawArticle a : articles) {
            int score = 0;
            String lc = a.title().toLowerCase(Locale.ROOT);
            for (String w : words) if (lc.contains(w)) score++;
            if (score > bestScore) { bestScore = score; best = a; }
        }
        return (best != null && bestScore > 0) ? best.url() : "";
    }

    private List<NewsItem> selectBest(List<NewsItem> candidates, int target) {
        if (candidates.isEmpty()) return List.of();

        // Prefer items closest to midpoint
        int mid = (MIN_LEN + MAX_LEN) / 2;
        List<NewsItem> sorted = candidates.stream()
                .sorted(Comparator.comparingInt(it -> Math.abs(it.smsText().length() - mid)))
                .collect(Collectors.toList());

        // Dedup by URL
        LinkedHashMap<String, NewsItem> dedup = new LinkedHashMap<>();
        for (NewsItem it : sorted) {
            String key = (it.url() == null) ? "" : it.url().trim();
            if (!key.isEmpty() && !dedup.containsKey(key)) dedup.put(key, it);
            if (dedup.size() >= target) break;
        }

        // Fill if URLs missing/empty
        if (dedup.size() < target) {
            for (NewsItem it : sorted) {
                if (dedup.size() >= target) break;
                String key = (it.url() == null) ? "" : it.url().trim();
                if (key.isEmpty()) {
                    dedup.put(UUID.randomUUID().toString(), it);
                }
            }
        }

        return new ArrayList<>(dedup.values()).subList(0, Math.min(target, dedup.size()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATION / CLEANUP
    // ─────────────────────────────────────────────────────────────────────────

    private String cleanSmsText(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        if (!s.isEmpty() && !s.matches(".*[.!?]$")) s += ".";
        if (s.length() > MAX_LEN) s = truncateAtSentence(s, MAX_LEN);
        return s;
    }

    private boolean validateSms(String sms) {
        if (sms == null) return false;
        int len = sms.length();
        if (len < MIN_LEN || len > MAX_LEN) return false;

        int sentences = countSentences(sms);
        if (sentences < 2 || sentences > 3) return false;

        if (sms.contains("…") || sms.contains("...")) return false;

        return true;
    }

    private int countSentences(String text) {
        if (text == null || text.isBlank()) return 0;
        int n = 0;
        for (String part : text.split("[.!?]+"))
            if (part.trim().length() > 10) n++;
        return n;
    }

    private String truncateAtSentence(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        String sub = text.substring(0, maxLen);
        int last = Math.max(sub.lastIndexOf('.'),
                Math.max(sub.lastIndexOf('!'), sub.lastIndexOf('?')));
        if (last > maxLen / 2) return sub.substring(0, last + 1).trim();
        return sub.trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROGRESS / STREAM TEXT EXTRACTION
    // ─────────────────────────────────────────────────────────────────────────

    private int countCompleteItems(String buffer) {
        if (buffer == null || buffer.isBlank()) return 0;
        Matcher m = COMPLETE_ITEM.matcher(buffer);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    private String chunkPreview(String bufferStr) {
        if (bufferStr == null || bufferStr.isBlank()) return "";
        String tail = bufferStr.length() > 90
                ? bufferStr.substring(bufferStr.length() - 90)
                : bufferStr;
        tail = tail.replaceAll("\\s+", " ").trim();
        return tail.isEmpty() ? "" : "…" + tail;
    }

    private String extractTextDelta(RawMessageStreamEvent event) {
        try {
            if (event == null) return null;
            return event.contentBlockDelta().stream()
                    .flatMap(cbd -> cbd.delta().text().stream())
                    .map(td -> td.text())
                    .collect(Collectors.joining(""));
        } catch (Exception e) {
            return null;
        }
    }

    private void emit(BiConsumer<Double, String> cb, double p, String msg) {
        if (cb != null) cb.accept(Math.max(0.0, Math.min(1.0, p)), msg);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CATEGORY GROUPING
    // ─────────────────────────────────────────────────────────────────────────

    private enum CategoryGroup { LOCAL, WEATHER, NATIONAL }

    private CategoryGroup resolveGroup(String category) {
        if (category == null || category.isBlank()) return CategoryGroup.LOCAL;
        String lc = category.toLowerCase(Locale.ROOT).trim();
        if (WEATHER_CATEGORIES.contains(lc)) return CategoryGroup.WEATHER;
        if (NATIONAL_CATEGORIES.contains(lc)) return CategoryGroup.NATIONAL;
        return CategoryGroup.LOCAL;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RSS UTIL
    // ─────────────────────────────────────────────────────────────────────────

    private String extractFirst(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String cleanHtml(String html) {
        if (html == null) return null;
        return html
                .replaceAll("<[^>]+>", " ")
                .replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&apos;", "'")
                .replace("&nbsp;", " ").replace("&#8211;", "–")
                .replace("&#8212;", "—")
                .replaceAll("\\s+", " ").trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TYPES
    // ─────────────────────────────────────────────────────────────────────────

    public record RawArticle(String title, String url, String description) {}
}
