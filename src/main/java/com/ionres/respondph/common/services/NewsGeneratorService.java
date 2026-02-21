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
    private static final Model MODEL_ID = Model.CLAUDE_SONNET_4_5;

    private static final int TARGET = 5;
    private static final int MIN_LEN = 240;
    private static final int MAX_LEN = 320;
    private static final int HTTP_TIMEOUT_SEC = 12;
    private static final int MAX_ARTICLES_PER_SOURCE = 20;
    private static final int MAX_ARTICLES_IN_PROMPT = 15;

    private static final Pattern COMPLETE_ITEM = Pattern.compile(
            "(?m)^\\s*(?:\\d+\\s*[\\.)]\\s*)?.+?\\|\\s*Headline:\\s*.+?\\|\\s*ArticleIndex:\\s*\\d+\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LINE_FULL = Pattern.compile(
            "^\\s*(?:\\d+\\s*[\\.)]\\s*)?(.+?)\\s*\\|\\s*Headline:\\s*(.+?)\\s*\\|\\s*ArticleIndex:\\s*(\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LINE_FALLBACK = Pattern.compile(
            "^\\s*(?:\\d+\\s*[\\.)]\\s*)?(.+?)\\s*\\|\\s*Headline:\\s*(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RSS_ITEM  = Pattern.compile("<item[^>]*>(.*?)</item>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern RSS_TITLE = Pattern.compile(
            "<title[^>]*>\\s*(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?\\s*</title>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern RSS_LINK  = Pattern.compile(
            "(?:<link>|<link[^/]*/?>)\\s*(https?://[^\\s<\"]+)",
            Pattern.CASE_INSENSITIVE);
    // FIX 1: corrected smart-quote corruption in character class — was [<"\s] with curly quotes
    private static final Pattern RSS_GUID  = Pattern.compile(
            "<guid[^>]*>\\s*(?:<!\\[CDATA\\[)?(https?://[^\\s<\"]+?)(?:]]>)?\\s*</guid>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RSS_DESC  = Pattern.compile(
            "<description[^>]*>\\s*(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?\\s*</description>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    // FIX 2: pre-compiled pattern to replace the broken inline Pattern.compile with smart quotes
    private static final Pattern URL_IN_BLOCK = Pattern.compile("(https?://[^\\s<\"']+)");

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
            "https://www.philstar.com/rss/headlines",
            "https://abcnews.com/abcnews/politicsheadlines",
            "https://abcnews.com/abcnews/healthheadlines"
    );

    private static final Set<String> NATIONAL_CATEGORIES = Set.of(
            "national news", "politics news", "health news", "crime / law / public safety news"
    );

    private static final Set<String> WEATHER_CATEGORIES = Set.of(
            "weather news", "weather", "weather update"
    );

    private static final String GEO_LOCAL   = "Iloilo City kag mga munisipalidad sang Probinsya sang Iloilo LAMANG: " +
            "Ajuy, Alimodian, Anilao, Badiangan, Balasan, Banate, Barotac Nuevo, Barotac Viejo, Batad, Bingawan, " +
            "Cabatuan, Calinog, Carles, Concepcion, Dingle, Dueñas, Dumangas, Estancia, Guimbal, Igbaras, " +
            "Janiuay, Lambunao, Leganes, Lemery, Leon, Maasin, Miagao, Mina, New Lucena, Oton, Pavia, Pototan, " +
            "San Dionisio, San Enrique, San Joaquin, San Miguel, San Rafael, Santa Barbara, Sara, " +
            "Tigbauan, Tubungan, Zarraga";
    private static final String GEO_WEATHER_ILOILO   = "Iloilo City kag mga munisipalidad sang Probinsya sang Iloilo " +
            "(una sa Banate): Ajuy, Alimodian, Anilao, Badiangan, Balasan, Banate, Barotac Nuevo, Barotac Viejo, " +
            "Batad, Bingawan, Cabatuan, Calinog, Carles, Concepcion, Dingle, Dueñas, Dumangas, Estancia, " +
            "Guimbal, Igbaras, Janiuay, Lambunao, Leganes, Lemery, Leon, Maasin, Miagao, Mina, New Lucena, " +
            "Oton, Pavia, Pototan, San Dionisio, San Enrique, San Joaquin, San Miguel, San Rafael, " +
            "Santa Barbara, Sara, Tigbauan, Tubungan, Zarraga";
    private static final String GEO_WEATHER_NATIONAL = "tibuok Pilipinas (national level — fallback tungod wala weather news sa Iloilo)";
    private static final String GEO_NATIONAL = "tibuok Pilipinas (national level)";

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

            ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "news-elapsed-ticker");
                t.setDaemon(true);
                return t;
            });

            ScheduledFuture<?> tick = ticker.scheduleAtFixedRate(() -> {
                if (done.get()) return;
                int n = confirmedItems.get();
                long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                double bar = 0.10 + (double) n / TARGET * 0.80;
                String label = n == 0
                        ? "Fetching live articles… (" + elapsed + "s)"
                        : "Writing news… " + n + " of " + TARGET + " done (" + elapsed + "s)";
                emit(onProgress, bar, label);
            }, 1, 1, TimeUnit.SECONDS);

            try {
                emit(onProgress, 0.03, "Fetching live articles (RSS)…");

                CategoryGroup group = resolveGroup(category);
                List<RawArticle> articles = fetchArticlesRssOnly(group, topic, category);

                System.out.printf("[News] RSS articles after dedup: %d%n", articles.size());

                if (cancelRequested.get()) {
                    emit(onProgress, 1.0, "Cancelled.");
                    return List.of();
                }

                boolean isWeatherIloilo = (group == CategoryGroup.WEATHER) && isIloiloWeatherArticles(articles);
                boolean isWeatherFallback = (group == CategoryGroup.WEATHER) && !isWeatherIloilo;

                String geoScope = switch (group) {
                    case LOCAL    -> GEO_LOCAL;
                    case WEATHER  -> isWeatherIloilo ? GEO_WEATHER_ILOILO : GEO_WEATHER_NATIONAL;
                    case NATIONAL -> GEO_NATIONAL;
                };

                String today = LocalDate.now().toString();
                boolean hasGrounding = !articles.isEmpty();
                String groundingBlock = buildGroundingBlock(articles);
                String prompt = buildPrompt(topic, category, geoScope, today, groundingBlock, hasGrounding, isWeatherFallback);

                emit(onProgress, 0.10, hasGrounding
                        ? "Writing from real articles…"
                        : "RSS sparse — writing fallback items…");

                MessageCreateParams params = MessageCreateParams.builder()
                        .model(MODEL_ID)
                        .maxTokens(3000L)
                        .addUserMessage(prompt)
                        .build();

                StringBuilder buffer = new StringBuilder();
                int lastEmittedCount = 0;

                try (StreamResponse<RawMessageStreamEvent> stream = client.messages().createStreaming(params)) {
                    Iterator<RawMessageStreamEvent> iter = stream.stream().iterator();

                    while (true) {
                        if (cancelRequested.get()) break;

                        RawMessageStreamEvent event;
                        try {
                            if (!iter.hasNext()) break;
                            event = iter.next();
                        } catch (Exception ex) {
                            if (cancelRequested.get() || Thread.interrupted()) break;
                            throw ex;
                        }

                        String piece = extractTextDelta(event);
                        if (piece == null || piece.isEmpty()) continue;

                        buffer.append(piece);

                        int nowComplete = countCompleteItems(buffer.toString());
                        if (nowComplete > lastEmittedCount) {
                            lastEmittedCount = nowComplete;
                            confirmedItems.set(nowComplete);

                            double bar = Math.min(0.92, 0.10 + (double) nowComplete / TARGET * 0.80);
                            long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                            String statusMsg = nowComplete + " of " + TARGET + " items found (" + elapsed + "s)";
                            emit(onProgress, bar, statusMsg);

                            System.out.println("[Claude] Item " + nowComplete + "/" + TARGET + " confirmed");
                        }
                    }
                }

                if (cancelRequested.get()) {
                    emit(onProgress, 1.0, "Cancelled.");
                    return List.of();
                }

                emit(onProgress, 0.93, "Validating items…");

                String raw = buffer.toString()
                        .replaceAll("[ \\t]+", " ")
                        .trim();

                System.out.println("\n=== CLAUDE RAW RESPONSE ===");
                System.out.println(raw.isEmpty() ? "(empty)" : raw);
                System.out.println("===========================\n");

                List<NewsItem> candidates = parseAllCandidates(raw, articles);
                System.out.printf("[News] Parsed candidates: %d%n", candidates.size());

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

    private List<RawArticle> fetchArticlesRssOnly(CategoryGroup group, String topic, String category) {
        if (group == CategoryGroup.WEATHER) {
            return fetchWeatherArticles(topic, category);
        }
        List<String> sources = switch (group) {
            case LOCAL    -> LOCAL_RSS;
            case NATIONAL -> NATIONAL_RSS;
            default       -> LOCAL_RSS;
        };
        List<RawArticle> all = fetchFromSources(sources);
        List<RawArticle> filtered = filterByTopicAndCategory(all, topic, category, group);
        return dedup(filtered);
    }

    private List<RawArticle> fetchWeatherArticles(String topic, String category) {
        List<RawArticle> all = fetchFromSources(WEATHER_RSS);

        List<String> weatherKw = List.of(
                "bagyo", "weather", "ulan", "baha", "flood", "storm", "typhoon",
                "rainfall", "pagasa", "lamdag", "drought", "init", "heat", "signal"
        );
        List<String> iloiloKw = List.of(
                "iloilo", "ajuy", "alimodian", "anilao", "badiangan", "balasan", "banate",
                "barotac nuevo", "barotac viejo", "batad", "bingawan", "cabatuan", "calinog",
                "carles", "concepcion", "dingle", "dueñas", "dumangas", "estancia",
                "guimbal", "igbaras", "janiuay", "lambunao", "leganes", "lemery", "leon",
                "maasin", "miagao", "mina", "new lucena", "oton", "pavia", "pototan",
                "san dionisio", "san enrique", "san joaquin", "san miguel", "san rafael",
                "santa barbara", "sara", "tigbauan", "tubungan", "zarraga",
                "jaro", "la paz", "molo", "mandurriao", "arevalo"
        );

        List<RawArticle> iloiloWeather = all.stream()
                .filter(a -> {
                    String text = (a.title() + " " + a.description()).toLowerCase(Locale.ROOT);
                    boolean hasWeather = weatherKw.stream().anyMatch(text::contains);
                    boolean hasIloilo  = iloiloKw.stream().anyMatch(text::contains);
                    return hasWeather && hasIloilo;
                })
                .collect(Collectors.toList());

        System.out.printf("[News] Weather: Iloilo-specific articles found: %d%n", iloiloWeather.size());

        if (!iloiloWeather.isEmpty()) {
            List<RawArticle> banate = iloiloWeather.stream()
                    .filter(a -> (a.title() + " " + a.description()).toLowerCase(Locale.ROOT).contains("banate"))
                    .collect(Collectors.toList());
            List<RawArticle> others = iloiloWeather.stream()
                    .filter(a -> !(a.title() + " " + a.description()).toLowerCase(Locale.ROOT).contains("banate"))
                    .collect(Collectors.toList());
            List<RawArticle> prioritized = new ArrayList<>();
            prioritized.addAll(banate);
            prioritized.addAll(others);
            System.out.printf("[News] Weather: Banate=%d, Other Iloilo=%d%n", banate.size(), others.size());
            return dedup(prioritized);
        }

        System.out.println("[News] Weather: No Iloilo articles found, falling back to Philippines-wide.");
        List<RawArticle> phWeather = all.stream()
                .filter(a -> {
                    String text = (a.title() + " " + a.description()).toLowerCase(Locale.ROOT);
                    return weatherKw.stream().anyMatch(text::contains);
                })
                .collect(Collectors.toList());

        return dedup(phWeather.isEmpty() ? all : phWeather);
    }

    private boolean isIloiloWeatherArticles(List<RawArticle> articles) {
        if (articles.isEmpty()) return false;
        List<String> iloiloKw = List.of(
                "iloilo", "banate", "ajuy", "alimodian", "anilao", "badiangan", "balasan",
                "barotac nuevo", "barotac viejo", "batad", "bingawan", "cabatuan", "calinog",
                "carles", "concepcion", "dingle", "dueñas", "dumangas", "estancia",
                "guimbal", "igbaras", "janiuay", "lambunao", "leganes", "lemery", "leon",
                "maasin", "miagao", "mina", "new lucena", "oton", "pavia", "pototan",
                "san dionisio", "san enrique", "san joaquin", "san miguel", "san rafael",
                "santa barbara", "sara", "tigbauan", "tubungan", "zarraga",
                "jaro", "la paz", "molo", "mandurriao", "arevalo"
        );
        return articles.stream().anyMatch(a -> {
            String text = (a.title() + " " + a.description()).toLowerCase(Locale.ROOT);
            return iloiloKw.stream().anyMatch(text::contains);
        });
    }

    private List<RawArticle> fetchFromSources(List<String> sources) {
        List<CompletableFuture<List<RawArticle>>> futures = sources.stream()
                .map(url -> CompletableFuture.supplyAsync(() -> fetchRss(url)))
                .toList();
        List<RawArticle> all = new ArrayList<>();
        for (var f : futures) {
            try { all.addAll(f.get(HTTP_TIMEOUT_SEC + 3L, TimeUnit.SECONDS)); }
            catch (Exception e) { System.err.println("[News] RSS future error: " + e.getMessage()); }
        }
        return all;
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
            String desc  = cleanHtml(extractFirst(RSS_DESC, block));

            if (title == null || title.isBlank()) continue;

            // Priority: <guid> (real article URL) → <link> → scan block for any URL
            String link = extractFirst(RSS_GUID, block);
            if (link == null || link.isBlank() || !link.startsWith("http")) {
                link = extractFirst(RSS_LINK, block);
            }
            if (link == null || link.isBlank() || !link.startsWith("http")) {
                // FIX 2: use pre-compiled URL_IN_BLOCK pattern (was broken inline compile with smart quotes)
                Matcher urlM = URL_IN_BLOCK.matcher(block);
                while (urlM.find()) {
                    String candidate = urlM.group(1).replaceAll("[<\"'\\s].*$", "").trim();
                    if (!candidate.contains("/feed") && !candidate.contains("rss") && candidate.length() > 20) {
                        link = candidate;
                        break;
                    }
                }
            }

            if (link == null || link.isBlank() || !link.startsWith("http")) {
                System.err.printf("[News] Skipping article with no real URL: %s%n", title);
                continue;
            }

            link = link.replaceAll("[<\"'\\s].*$", "").trim();

            if (link.equals(sourceUrl) || link.endsWith("/feed") || link.endsWith("/feed/")) {
                System.err.printf("[News] Skipping feed-URL article: %s%n", title);
                continue;
            }

            items.add(new RawArticle(title, link, desc != null ? desc : ""));
        }

        return items;
    }

    private List<RawArticle> filterByTopicAndCategory(
            List<RawArticle> articles, String topic, String category, CategoryGroup group) {

        String catLc   = category != null ? category.toLowerCase(Locale.ROOT).trim() : "";
        String topicLc = topic    != null ? topic.toLowerCase(Locale.ROOT).trim()    : "";

        List<String> keywords = buildKeywords(catLc, topicLc);

        List<RawArticle> matched = articles.stream()
                .filter(a -> {
                    String text = (a.title() + " " + a.description()).toLowerCase(Locale.ROOT);
                    return keywords.stream().anyMatch(text::contains);
                })
                .collect(Collectors.toList());

        return matched.isEmpty() ? articles : matched;
    }

    private List<String> buildKeywords(String catLc, String topicLc) {
        List<String> kw = new ArrayList<>();

        if (!topicLc.isBlank()) {
            for (String word : topicLc.split("[\\s/,]+")) {
                if (word.length() > 2) kw.add(word);
            }
        }

        List<String> ILOILO_GEO = List.of(
                "iloilo", "ajuy", "alimodian", "anilao", "badiangan", "balasan", "banate",
                "barotac nuevo", "barotac viejo", "batad", "bingawan", "cabatuan", "calinog",
                "carles", "concepcion", "dingle", "dueñas", "duenas", "dumangas", "estancia",
                "guimbal", "igbaras", "janiuay", "lambunao", "leganes", "lemery", "leon",
                "maasin", "miagao", "mina", "new lucena", "oton", "pavia", "pototan",
                "san dionisio", "san enrique", "san joaquin", "san miguel", "san rafael",
                "santa barbara", "sara", "tigbauan", "tubungan", "zarraga",
                "jaro", "la paz", "molo", "mandurriao", "arevalo"
        );

        if (catLc.contains("weather")) {
            kw.addAll(List.of(
                    "bagyo", "weather", "ulan", "baha", "flood", "storm", "typhoon",
                    "rainfall", "pagasa", "lamdag", "drought", "init", "heat"
            ));
            kw.addAll(ILOILO_GEO);
        } else if (catLc.contains("crime") || catLc.contains("law") || catLc.contains("safety")) {
            kw.addAll(List.of(
                    "crime", "police", "pulis", "arrested", "murder", "robbery",
                    "law", "court", "verdict", "sentenced", "illegal", "drug", "droga"
            ));
            kw.addAll(ILOILO_GEO);
        } else if (catLc.contains("health")) {
            kw.addAll(List.of(
                    "health", "hospital", "disease", "virus", "patient", "medical",
                    "dengue", "covid", "sakit", "doctor", "vaccine", "bakuna", "outbreak"
            ));
        } else if (catLc.contains("politics")) {
            kw.addAll(List.of(
                    "politics", "election", "mayor", "governor", "congress", "senate",
                    "president", "government", "policy", "vote", "candidate", "partido"
            ));
        } else if (catLc.contains("national")) {
            kw.addAll(List.of(
                    "philippine", "philippines", "national", "manila", "duterte",
                    "marcos", "senate", "house", "pagasa", "ndrrmc", "doh", "dilg"
            ));
        } else {
            kw.addAll(ILOILO_GEO);
        }

        return kw;
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

    private String buildGroundingBlock(List<RawArticle> articles) {
        if (articles.isEmpty()) return "";
        int limit = Math.min(articles.size(), MAX_ARTICLES_IN_PROMPT);

        StringBuilder sb = new StringBuilder();
        sb.append("=== REAL NEWS ARTICLES ===\n\n");
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

    private String buildPrompt(
            String topic,
            String category,
            String geoScope,
            String today,
            String groundingBlock,
            boolean hasGrounding,
            boolean isWeatherFallback
    ) {
        StringBuilder p = new StringBuilder();

        p.append("IKAW ISA KA eksperto nga manunulat sang HILIGAYNON SMS NEWS.\n\n");

        p.append("=== IMPORTANTE NGA LIMITASYON ===\n");
        p.append("GEO SCOPE: ").append(geoScope).append(".\n");
        p.append("TOPIC: ").append(category).append(".\n");
        if (isWeatherFallback) {
            p.append("NAPILIAN: Wala weather news para sa Iloilo kag munisipalidad niya, \n");
            p.append("  kaya himoa ang weather news para sa TIBUOK PILIPINAS.\n");
        }
        p.append("BAWAL GID ang balita nga:\n");
        p.append("  - WALA nahanungud sa geo scope nga: ").append(geoScope).append("\n");
        p.append("  - WALA nahanungud sa topic nga: ").append(category).append("\n");
        p.append("  - Nahanungud sa lugar nga WALA sa ").append(geoScope).append("\n");
        p.append("=== END LIMITASYON ===\n\n");

        p.append("LANGUAGE: Tanan nga SMS body dapat PURE HILIGAYNON gid.\n");
        p.append("INDI mag gamit sang English kag indi mag Tagalog.\n");
        p.append("Kung may termino nga wala Hiligaynon, ilisan sang Hiligaynon nga pagpaathag.\n\n");

        p.append("Subong nga adlaw: ").append(today).append("\n");
        p.append("Topic: ").append(topic).append(" | Category: ").append(category).append("\n\n");

        if (hasGrounding && !groundingBlock.isBlank()) {
            p.append(groundingBlock);
            p.append("IMPORTANTE: Pilion LAMANG ang mga article nga may koneksyon sa ").append(geoScope).append(" kag sa topic nga ").append(category).append(".\n");
            if (!isWeatherFallback) {
                p.append("PRIORITY: Kung may article parte sa Banate, Iloilo, ibutang ini una sa lista.\n");
            }
            p.append("Kung ang article wala koneksyon sa ").append(geoScope).append(" o sa topic na ").append(category).append(", INDI mo ito gamiton.\n");
            p.append("INDI mag imbento sang facts nga wala sa article.\n\n");
        } else {
            p.append("WALA SANG sapat nga articles.\n");
            if (isWeatherFallback) {
                p.append("Himoa ang weather news base sa general knowledge para sa TIBUOK PILIPINAS.\n");
                p.append("Pwede mag-gamit sang PAGASA forecasts, typhoon updates, kag seasonal weather patterns.\n");
            } else {
                p.append("Himoa ang items base sa general knowledge PARTE SA ").append(geoScope).append(" kag topic nga ").append(category).append(" LAMANG.\n");
            }
            p.append("INDI mag butang sang peke nga detalye.\n\n");
        }

        p.append("HIMOA EXACTLY ").append(TARGET).append(" ka SMS items.\n");
        p.append("Tagsa ka item: 2-3 ka kumpleto nga pangungusap.\n");
        p.append("Kada SMS body: EXACTLY ").append(MIN_LEN).append("-").append(MAX_LEN).append(" characters.\n");
        p.append("BAWAL: ellipsis (…), '...', putol nga pangungusap, kag raw URL sa SMS body.\n\n");

        p.append("STRICT LINE FORMAT (isa lang ka linya kada item, WALAY blank lines):\n");
        p.append("{N}. {SMS body} | Headline: {exact article title} | ArticleIndex: {1-based article number}\n\n");

        p.append("RULES:\n");
        p.append("1) KADA item dapat lain nga ARTICLE (wala sang repeat).\n");
        p.append("2) Ang Headline dapat EXACT pareho sang Title sa article list.\n");
        p.append("3) Ang ArticleIndex dapat sakto nga numero sang article nga gin-basehan.\n");
        p.append("4) Output ONLY the numbered list. WALA intro. WALA closing. WALA markdown.\n");
        p.append("5) WALA balita nga wala koneksyon sa ").append(geoScope).append(".\n");
        p.append("6) WALA balita nga wala koneksyon sa topic nga ").append(category).append(".\n");

        return p.toString();
    }

    private List<NewsItem> parseAllCandidates(String raw, List<RawArticle> articles) {
        List<NewsItem> results = new ArrayList<>();
        if (raw == null || raw.isBlank()) return results;

        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;

            Matcher m = LINE_FULL.matcher(t);
            if (m.matches()) {
                String headline = m.group(2).trim();
                String smsText  = cleanSmsText(m.group(1));
                smsText         = padSmsText(smsText, headline);
                int idx         = safeInt(m.group(3), -1);

                if (!validateSms(smsText)) {
                    System.out.printf("[News] Skipped (len=%d): %s%n", smsText.length(), smsText.substring(0, Math.min(60, smsText.length())));
                    continue;
                }

                String url = resolveUrl(idx, headline, articles);
                if (url == null) url = "";
                results.add(new NewsItem(smsText, url));
                continue;
            }

            Matcher fb = LINE_FALLBACK.matcher(t);
            if (fb.matches()) {
                String headline = fb.group(2).trim();
                String smsText  = cleanSmsText(fb.group(1));
                smsText         = padSmsText(smsText, headline);
                if (!validateSms(smsText)) {
                    System.out.printf("[News] Skipped fallback (len=%d): %s%n", smsText.length(), smsText.substring(0, Math.min(60, smsText.length())));
                    continue;
                }
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
        if (articles == null || articles.isEmpty() || headline == null || headline.isBlank()) return "";

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

        int mid = (MIN_LEN + MAX_LEN) / 2;
        List<NewsItem> sorted = candidates.stream()
                .sorted(Comparator.comparingInt(it -> Math.abs(it.smsText().length() - mid)))
                .collect(Collectors.toList());

        LinkedHashMap<String, NewsItem> byUrl = new LinkedHashMap<>();
        List<NewsItem> noUrl = new ArrayList<>();
        for (NewsItem it : sorted) {
            String key = (it.url() == null) ? "" : it.url().trim();
            if (key.isEmpty()) {
                noUrl.add(it);
            } else if (!byUrl.containsKey(key)) {
                byUrl.put(key, it);
            }
        }

        List<NewsItem> result = new ArrayList<>(byUrl.values());

        for (NewsItem it : noUrl) {
            if (result.size() >= target) break;
            result.add(it);
        }

        if (result.size() < target) {
            Set<NewsItem> already = new HashSet<>(result);
            for (NewsItem it : sorted) {
                if (result.size() >= target) break;
                if (!already.contains(it)) {
                    result.add(it);
                    already.add(it);
                }
            }
        }

        System.out.printf("[News] selectBest: %d candidates → %d selected%n", candidates.size(), Math.min(target, result.size()));
        return result.subList(0, Math.min(target, result.size()));
    }

    private String cleanSmsText(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        if (s.length() > MAX_LEN) {
            s = truncateAtSentence(s, MAX_LEN);
        }
        s = s.trim();
        if (!s.isEmpty() && !s.endsWith(".") && !s.endsWith("!") && !s.endsWith("?")) {
            s += ".";
        }
        return s;
    }

    private String padSmsText(String sms, String headline) {
        if (sms == null) return "";
        if (sms.length() >= MIN_LEN) return sms;
        if (headline == null || headline.isBlank()) return sms;
        String[] pads = {
                " Ini nga balita gikan sa: " + headline.trim() + ".",
                " Suno sa bag-o nga balita, ini ang isa sa mga importante nga hitabo subong nga adlaw.",
                " Ginapaabot ang dugang nga impormasyon parte sa maong hitabo sa susunod nga mga adlaw.",
                " Ang publiko ginahangyo nga magbantay sa mga update parte sini nga hitabo.",
        };
        String result = sms;
        for (String pad : pads) {
            if (result.length() >= MIN_LEN) break;
            String candidate = result;
            if (candidate.endsWith(".") || candidate.endsWith("!") || candidate.endsWith("?"))
                candidate = candidate.substring(0, candidate.length() - 1);
            candidate = candidate + pad;
            if (candidate.length() <= MAX_LEN) {
                result = candidate;
            } else {
                int space = MAX_LEN - result.length();
                if (space > 10) {
                    String trimmedPad = pad.substring(0, space).trim();
                    if (!trimmedPad.endsWith(".")) trimmedPad += ".";
                    result = result + trimmedPad;
                }
                break;
            }
        }
        if (!result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?")) result += ".";
        return result;
    }

    private boolean validateSms(String sms) {
        if (sms == null) return false;
        int len = sms.length();
        if (len < MIN_LEN || len > MAX_LEN) return false;
        int sentences = countSentences(sms);
        if (sentences < 1) return false;
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
        int lastPeriod   = sub.lastIndexOf('.');
        int lastExclaim  = sub.lastIndexOf('!');
        int lastQuestion = sub.lastIndexOf('?');
        int lastSentEnd  = Math.max(lastPeriod, Math.max(lastExclaim, lastQuestion));
        if (lastSentEnd > maxLen / 2) {
            return sub.substring(0, lastSentEnd + 1).trim();
        }
        int lastSpace = sub.lastIndexOf(' ');
        if (lastSpace > maxLen / 2) {
            String cut = sub.substring(0, lastSpace).trim();
            if (!cut.endsWith(".") && !cut.endsWith("!") && !cut.endsWith("?")) cut += ".";
            return cut;
        }
        return sub.trim();
    }

    private int countCompleteItems(String buffer) {
        if (buffer == null || buffer.isBlank()) return 0;
        Matcher m = COMPLETE_ITEM.matcher(buffer);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    // FIX 3: simplified extractTextDelta — previous version was overly chained and fragile
    private String extractTextDelta(RawMessageStreamEvent event) {
        if (event == null) return null;
        try {
            return event.contentBlockDelta()
                    .flatMap(cbd -> cbd.delta().text())
                    .map(td -> td.text())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void emit(BiConsumer<Double, String> cb, double p, String msg) {
        if (cb != null) cb.accept(Math.max(0.0, Math.min(1.0, p)), msg);
    }

    private enum CategoryGroup { LOCAL, WEATHER, NATIONAL }

    private CategoryGroup resolveGroup(String category) {
        if (category == null || category.isBlank()) return CategoryGroup.LOCAL;
        String lc = category.toLowerCase(Locale.ROOT).trim();
        if (WEATHER_CATEGORIES.contains(lc))  return CategoryGroup.WEATHER;
        if (NATIONAL_CATEGORIES.contains(lc)) return CategoryGroup.NATIONAL;
        return CategoryGroup.LOCAL;
    }

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

    public record RawArticle(String title, String url, String description) {}
}