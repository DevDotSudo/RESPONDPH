package com.ionres.respondph.common.services;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BiConsumer;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * ══════════════════════════════════════════════════════════
 *  NewsGeneratorService
 *  Pipeline:
 *    1. Fetch RSS feeds  →  raw articles
 *    2. Claude           →  summarises articles into English SMS bodies
 *    3. Google Translate →  translates English SMS bodies to Hiligaynon (hil)
 *
 *  Required env vars:
 *    ANTHROPIC_API_KEY        (required)
 *    GOOGLE_TRANSLATE_API_KEY (required for Hiligaynon translation)
 * ══════════════════════════════════════════════════════════
 */
public class NewsGeneratorService {

    // ─── Env vars ────────────────────────────────────────────────────────────
    private static final String ENV_ANTHROPIC_KEY    = "ANTHROPIC_API_KEY";
    private static final String ENV_GOOGLE_TRANS_KEY = "GOOGLE_TRANSLATE_API_KEY";

    // ─── Model ───────────────────────────────────────────────────────────────
    private static final Model MODEL_ID = Model.CLAUDE_SONNET_4_5;

    // ─── Google Translate ─────────────────────────────────────────────────────
    private static final String GOOGLE_TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2";
    private static final String LANG_SOURCE          = "en";
    private static final String LANG_TARGET          = "hil"; // Hiligaynon

    // ─── SMS limits ───────────────────────────────────────────────────────────
    private static final int TARGET  = 5;   // number of SMS items to produce
    private static final int MIN_LEN = 200; // min chars (Hiligaynon is ~20% longer than English)
    private static final int MAX_LEN = 360; // max chars

    // ─── HTTP / RSS ───────────────────────────────────────────────────────────
    private static final int HTTP_TIMEOUT_SEC        = 12;
    private static final int MAX_ARTICLES_PER_SOURCE = 20;
    private static final int MAX_ARTICLES_IN_PROMPT  = 15;

    // ─── RSS feed lists ───────────────────────────────────────────────────────
    private static final List<String> RSS_LOCAL = List.of(
            "https://panaynews.net/feed/",
            "https://www.dailyguardian.com.ph/feed/"
    );
    private static final List<String> RSS_WEATHER = List.of(
            "https://data.gmanetwork.com/gno/rss/weather/feed.xml",
            "https://www.weather.gov/rss/"
    );
    private static final List<String> RSS_NATIONAL = List.of(
            "https://data.gmanetwork.com/gno/rss/news/feed.xml",
            "https://www.philstar.com/rss/headlines",
            "https://abcnews.com/abcnews/politicsheadlines",
            "https://abcnews.com/abcnews/healthheadlines"
    );

    // ─── Category sets ────────────────────────────────────────────────────────
    private static final Set<String> WEATHER_CATEGORIES = Set.of(
            "weather news", "weather", "weather update"
    );
    private static final Set<String> NATIONAL_CATEGORIES = Set.of(
            "national news", "politics news", "health news", "crime / law / public safety news"
    );

    // ─── Geo scopes (shown to Claude in the prompt) ───────────────────────────
    private static final String GEO_LOCAL =
            "Iloilo City and municipalities of Iloilo Province ONLY: " +
                    "Ajuy, Alimodian, Anilao, Badiangan, Balasan, Banate, Barotac Nuevo, Barotac Viejo, " +
                    "Batad, Bingawan, Cabatuan, Calinog, Carles, Concepcion, Dingle, Duenas, Dumangas, " +
                    "Estancia, Guimbal, Igbaras, Janiuay, Lambunao, Leganes, Lemery, Leon, Maasin, " +
                    "Miagao, Mina, New Lucena, Oton, Pavia, Pototan, San Dionisio, San Enrique, " +
                    "San Joaquin, San Miguel, San Rafael, Santa Barbara, Sara, Tigbauan, Tubungan, Zarraga";

    private static final String GEO_WEATHER_ILOILO =
            "Iloilo City and Iloilo Province (prioritise Banate first): " +
                    "Ajuy, Alimodian, Anilao, Badiangan, Balasan, Banate, Barotac Nuevo, Barotac Viejo, " +
                    "Batad, Bingawan, Cabatuan, Calinog, Carles, Concepcion, Dingle, Duenas, Dumangas, " +
                    "Estancia, Guimbal, Igbaras, Janiuay, Lambunao, Leganes, Lemery, Leon, Maasin, " +
                    "Miagao, Mina, New Lucena, Oton, Pavia, Pototan, San Dionisio, San Enrique, " +
                    "San Joaquin, San Miguel, San Rafael, Santa Barbara, Sara, Tigbauan, Tubungan, Zarraga";

    private static final String GEO_WEATHER_NATIONAL =
            "the entire Philippines (national fallback — no Iloilo weather news found)";

    private static final String GEO_NATIONAL =
            "the entire Philippines (national level)";

    // ─── Regex: parse RSS XML ─────────────────────────────────────────────────
    private static final Pattern RSS_ITEM  = Pattern.compile(
            "<item[^>]*>(.*?)</item>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern RSS_TITLE = Pattern.compile(
            "<title[^>]*>\\s*(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?\\s*</title>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern RSS_LINK  = Pattern.compile(
            "(?:<link>|<link[^/]*/?>)\\s*(https?://[^\\s<\"]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RSS_GUID  = Pattern.compile(
            "<guid[^>]*>\\s*(?:<!\\[CDATA\\[)?(https?://[^\\s<\"]+?)(?:]]>)?\\s*</guid>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RSS_DESC  = Pattern.compile(
            "<description[^>]*>\\s*(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?\\s*</description>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_SCAN  = Pattern.compile("(https?://[^\\s<\"']+)");

    // ─── Regex: parse Claude output lines ─────────────────────────────────────
    private static final Pattern LINE_COMPLETE = Pattern.compile(
            "(?m)^\\s*(?:\\d+\\s*[\\.)]\\s*)?.+?\\|\\s*Headline:\\s*.+?\\|\\s*ArticleIndex:\\s*\\d+\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LINE_FULL = Pattern.compile(
            "^\\s*(?:\\d+\\s*[\\.)]\\s*)?(.+?)\\s*\\|\\s*Headline:\\s*(.+?)\\s*\\|\\s*ArticleIndex:\\s*(\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LINE_FALLBACK = Pattern.compile(
            "^\\s*(?:\\d+\\s*[\\.)]\\s*)?(.+?)\\s*\\|\\s*Headline:\\s*(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    // ─── Dependencies ─────────────────────────────────────────────────────────
    private final AnthropicClient         claude;
    private final HttpClient              http;
    private final String                  googleApiKey;
    private final ObjectMapper            json    = new ObjectMapper();
    private final AtomicBoolean           cancelled   = new AtomicBoolean(false);
    private final AtomicReference<Thread> activeThread = new AtomicReference<>(null);

    // =========================================================================
    //  Constructor
    // =========================================================================
    public NewsGeneratorService() {
        String anthropicKey = System.getenv(ENV_ANTHROPIC_KEY);
        if (anthropicKey == null || anthropicKey.isBlank())
            throw new IllegalStateException("Missing env var: " + ENV_ANTHROPIC_KEY);

        this.claude = AnthropicOkHttpClient.builder()
                .apiKey(anthropicKey.trim())
                .build();

        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        String gKey = System.getenv(ENV_GOOGLE_TRANS_KEY);
        this.googleApiKey = (gKey != null && !gKey.isBlank()) ? gKey.trim() : null;

        System.out.println("[News] Pipeline ready:");
        System.out.println("[News]   Step 1 → Fetch RSS articles");
        System.out.println("[News]   Step 2 → Claude writes English SMS summaries");
        System.out.println("[News]   Step 3 → Google Translate converts to Hiligaynon (hil): "
                + (googleApiKey != null ? "ENABLED" : "DISABLED – set GOOGLE_TRANSLATE_API_KEY"));
    }

    public void cancel() {
        cancelled.set(true);
        Thread t = activeThread.get();
        if (t != null) t.interrupt();
    }

    // =========================================================================
    //  Public entry point
    // =========================================================================
    public CompletableFuture<List<NewsItem>> generateNewsHeadlines(
            String topic,
            String category,
            BiConsumer<Double, String> onProgress) {

        cancelled.set(false);

        return CompletableFuture.supplyAsync(() -> {
            activeThread.set(Thread.currentThread());
            long start = System.currentTimeMillis();

            // ── Ticker: progress updates every second ─────────────────────────
            AtomicInteger confirmed = new AtomicInteger(0);
            AtomicBoolean done      = new AtomicBoolean(false);

            ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "news-ticker");
                t.setDaemon(true);
                return t;
            });
            ScheduledFuture<?> tick = ticker.scheduleAtFixedRate(() -> {
                if (done.get()) return;
                int n       = confirmed.get();
                long secs   = elapsed(start);
                double pct  = 0.10 + (double) n / TARGET * 0.70;
                String msg  = n == 0
                        ? "Fetching RSS articles... (" + secs + "s)"
                        : "Writing summaries... " + n + "/" + TARGET + " (" + secs + "s)";
                emit(onProgress, pct, msg);
            }, 1, 1, TimeUnit.SECONDS);

            try {

                // ══════════════════════════════════════════════════════════════
                //  STEP 1 — Fetch RSS articles
                // ══════════════════════════════════════════════════════════════
                emit(onProgress, 0.03, "Step 1/3 — Fetching RSS articles...");

                CategoryGroup    group    = resolveGroup(category);
                List<RawArticle> articles = fetchRssArticles(group, topic, category);

                System.out.printf("[RSS] Fetched %d articles after dedup%n", articles.size());

                if (cancelled.get()) return cancelled();

                boolean iloiloWeather   = (group == CategoryGroup.WEATHER) && hasIloiloContent(articles);
                boolean weatherFallback = (group == CategoryGroup.WEATHER) && !iloiloWeather;

                String geoScope = switch (group) {
                    case LOCAL   -> GEO_LOCAL;
                    case WEATHER -> iloiloWeather ? GEO_WEATHER_ILOILO : GEO_WEATHER_NATIONAL;
                    case NATIONAL -> GEO_NATIONAL;
                };

                // ══════════════════════════════════════════════════════════════
                //  STEP 2 — Claude: read articles → write English SMS bodies
                // ══════════════════════════════════════════════════════════════
                emit(onProgress, 0.10, "Step 2/3 — Claude writing English summaries...");

                String prompt = buildPrompt(
                        topic, category, geoScope,
                        LocalDate.now().toString(),
                        articles, weatherFallback
                );

                System.out.println("\n[Claude] Sending prompt (" + prompt.length() + " chars)...");

                StringBuilder buffer    = new StringBuilder();
                int           lastCount = 0;

                MessageCreateParams params = MessageCreateParams.builder()
                        .model(MODEL_ID)
                        .maxTokens(3000L)
                        .addUserMessage(prompt)
                        .build();

                try (StreamResponse<RawMessageStreamEvent> stream =
                             claude.messages().createStreaming(params)) {

                    for (Iterator<RawMessageStreamEvent> it = stream.stream().iterator(); it.hasNext(); ) {
                        if (cancelled.get()) break;

                        RawMessageStreamEvent event;
                        try { event = it.next(); }
                        catch (Exception ex) {
                            if (cancelled.get() || Thread.interrupted()) break;
                            throw ex;
                        }

                        String delta = extractDelta(event);
                        if (delta == null || delta.isEmpty()) continue;

                        buffer.append(delta);

                        int nowCount = countCompleteLines(buffer.toString());
                        if (nowCount > lastCount) {
                            lastCount = nowCount;
                            confirmed.set(nowCount);
                            double pct = Math.min(0.78, 0.10 + (double) nowCount / TARGET * 0.68);
                            emit(onProgress, pct, "Claude: " + nowCount + "/" + TARGET + " items written...");
                            System.out.printf("[Claude] Item %d/%d confirmed%n", nowCount, TARGET);
                        }
                    }
                }

                if (cancelled.get()) return cancelled();

                String claudeRaw = buffer.toString().replaceAll("[ \\t]+", " ").trim();
                System.out.println("\n══ CLAUDE RAW (ENGLISH) ══════════════════════════════");
                System.out.println(claudeRaw.isEmpty() ? "(empty)" : claudeRaw);
                System.out.println("══════════════════════════════════════════════════════\n");

                // ── Parse Claude's English lines ──────────────────────────────
                emit(onProgress, 0.80, "Parsing Claude output...");
                List<ParsedItem> parsed = parseClaudeOutput(claudeRaw, articles);
                System.out.printf("[Parse] %d items parsed from Claude output%n", parsed.size());

                if (parsed.isEmpty()) {
                    emit(onProgress, 1.0, "No items parsed from Claude output.");
                    return List.of();
                }

                // ══════════════════════════════════════════════════════════════
                //  STEP 3 — Google Translate: English → Hiligaynon
                // ══════════════════════════════════════════════════════════════
                List<NewsItem> candidates;

                if (googleApiKey != null) {
                    emit(onProgress, 0.85, "Step 3/3 — Google Translate: English → Hiligaynon...");
                    candidates = translateToHiligaynon(parsed);
                } else {
                    // No API key — keep English as final output with a warning
                    System.out.println("[Translate] SKIPPED — GOOGLE_TRANSLATE_API_KEY not set. Output stays in English.");
                    candidates = parsed.stream()
                            .map(p -> new NewsItem(cleanText(p.englishBody()), p.url()))
                            .collect(Collectors.toList());
                }

                System.out.printf("[Done] %d candidates ready%n", candidates.size());

                List<NewsItem> best = selectBest(candidates, TARGET);

                emit(onProgress, 1.0,
                        "Done — " + best.size() + "/" + TARGET + " items in " + elapsed(start) + "s");
                return best;

            } catch (Exception e) {
                System.err.println("[News] Fatal error: " + e.getMessage());
                e.printStackTrace();
                emit(onProgress, 1.0, "Error: " + e.getMessage());
                return List.of();
            } finally {
                done.set(true);
                tick.cancel(false);
                ticker.shutdownNow();
                activeThread.set(null);
                Thread.interrupted();
            }
        });
    }

    // =========================================================================
    //  STEP 1 — RSS fetching
    // =========================================================================

    private List<RawArticle> fetchRssArticles(CategoryGroup group, String topic, String category) {
        List<String> feeds = switch (group) {
            case LOCAL    -> RSS_LOCAL;
            case WEATHER  -> RSS_WEATHER;
            case NATIONAL -> RSS_NATIONAL;
        };

        // Fetch all feeds in parallel
        List<CompletableFuture<List<RawArticle>>> futures = feeds.stream()
                .map(url -> CompletableFuture.supplyAsync(() -> fetchOneFeed(url)))
                .toList();

        List<RawArticle> all = new ArrayList<>();
        for (var f : futures) {
            try { all.addAll(f.get(HTTP_TIMEOUT_SEC + 3L, TimeUnit.SECONDS)); }
            catch (Exception e) { System.err.println("[RSS] Feed error: " + e.getMessage()); }
        }

        // Filter and dedup
        List<RawArticle> filtered = group == CategoryGroup.WEATHER
                ? filterWeather(all)
                : filterByKeywords(all, topic, category, group);

        return dedup(filtered);
    }

    private List<RawArticle> fetchOneFeed(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (compatible; RespondPH-NewsBot/2.0)")
                    .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                    .timeout(java.time.Duration.ofSeconds(HTTP_TIMEOUT_SEC))
                    .GET().build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                System.err.printf("[RSS] HTTP %d → %s%n", resp.statusCode(), url);
                return List.of();
            }

            List<RawArticle> items = parseRssFeed(resp.body(), url);
            System.out.printf("[RSS] %s → %d articles%n", url, items.size());
            return items;

        } catch (Exception e) {
            System.err.printf("[RSS] Failed %s: %s%n", url, e.getMessage());
            return List.of();
        }
    }

    private List<RawArticle> parseRssFeed(String xml, String feedUrl) {
        List<RawArticle> items = new ArrayList<>();
        Matcher m = RSS_ITEM.matcher(xml);

        while (m.find() && items.size() < MAX_ARTICLES_PER_SOURCE) {
            String block = m.group(1);
            String title = cleanHtml(firstMatch(RSS_TITLE, block));
            String desc  = cleanHtml(firstMatch(RSS_DESC, block));
            if (title == null || title.isBlank()) continue;

            // Resolve URL: prefer <guid> (real article URL) → <link> → scan block
            String url = firstMatch(RSS_GUID, block);
            if (url == null || !url.startsWith("http"))
                url = firstMatch(RSS_LINK, block);
            if (url == null || !url.startsWith("http")) {
                Matcher us = URL_SCAN.matcher(block);
                while (us.find()) {
                    String c = us.group(1).replaceAll("[<\"'\\s].*$", "").trim();
                    if (!c.contains("feed") && !c.contains("rss") && c.length() > 20) {
                        url = c;
                        break;
                    }
                }
            }
            if (url == null || !url.startsWith("http")) continue;
            url = url.replaceAll("[<\"'\\s].*$", "").trim();
            if (url.equals(feedUrl) || url.endsWith("/feed") || url.endsWith("/feed/")) continue;

            items.add(new RawArticle(title, url, desc != null ? desc : ""));
        }
        return items;
    }

    private List<RawArticle> filterWeather(List<RawArticle> all) {
        List<String> weatherKw = List.of("bagyo","weather","ulan","baha","flood","storm","typhoon",
                "rainfall","pagasa","drought","init","heat","signal","lamdag");
        List<String> iloiloKw  = iloiloKeywords();

        // Try Iloilo-specific weather first
        List<RawArticle> iloiloWeather = all.stream()
                .filter(a -> matches(a, weatherKw) && matches(a, iloiloKw))
                .collect(Collectors.toList());

        System.out.printf("[RSS] Weather: %d Iloilo-specific articles%n", iloiloWeather.size());

        if (!iloiloWeather.isEmpty()) {
            // Put Banate articles first
            List<RawArticle> banate = iloiloWeather.stream()
                    .filter(a -> text(a).contains("banate")).collect(Collectors.toList());
            List<RawArticle> others = iloiloWeather.stream()
                    .filter(a -> !text(a).contains("banate")).collect(Collectors.toList());
            List<RawArticle> out = new ArrayList<>(banate);
            out.addAll(others);
            return out;
        }

        System.out.println("[RSS] Weather: No Iloilo articles — using Philippines-wide fallback.");
        List<RawArticle> phWeather = all.stream()
                .filter(a -> matches(a, weatherKw)).collect(Collectors.toList());
        return phWeather.isEmpty() ? all : phWeather;
    }

    private List<RawArticle> filterByKeywords(
            List<RawArticle> all, String topic, String category, CategoryGroup group) {
        List<String> kw = buildKeywords(
                category != null ? category.toLowerCase(Locale.ROOT) : "",
                topic    != null ? topic.toLowerCase(Locale.ROOT)    : ""
        );
        List<RawArticle> matched = all.stream()
                .filter(a -> matches(a, kw))
                .collect(Collectors.toList());
        return matched.isEmpty() ? all : matched;
    }

    private List<RawArticle> dedup(List<RawArticle> articles) {
        Set<String> seen = new LinkedHashSet<>();
        List<RawArticle> out = new ArrayList<>();
        for (RawArticle a : articles) {
            String key = a.title().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
            if (seen.add(key)) out.add(a);
        }
        return out;
    }

    // =========================================================================
    //  STEP 2 — Claude prompt
    // =========================================================================

    private String buildPrompt(
            String topic, String category, String geoScope, String today,
            List<RawArticle> articles, boolean weatherFallback) {

        StringBuilder p = new StringBuilder();

        // Role
        p.append("You are an expert SMS news writer for a Philippine local news service.\n\n");

        // Language instruction — ALWAYS English here, Google Translate handles Hiligaynon
        p.append("LANGUAGE RULE: Write every SMS body in CLEAR, PLAIN ENGLISH.\n");
        p.append("Do NOT use Hiligaynon, Tagalog, or any other language.\n");
        p.append("The English output will be translated to Hiligaynon by Google Translate.\n\n");

        // Scope constraints
        p.append("═══ SCOPE ═══\n");
        p.append("GEO   : ").append(geoScope).append("\n");
        p.append("TOPIC : ").append(category).append("\n");
        if (weatherFallback)
            p.append("NOTE  : No Iloilo weather found — cover weather for the entire Philippines.\n");
        p.append("RULE  : Only write about stories relevant to the GEO and TOPIC above.\n");
        p.append("═════════════\n\n");

        p.append("Today's date: ").append(today).append("\n");
        p.append("Topic: ").append(topic).append(" | Category: ").append(category).append("\n\n");

        // Grounding articles
        if (!articles.isEmpty()) {
            int limit = Math.min(articles.size(), MAX_ARTICLES_IN_PROMPT);
            p.append("══ RSS ARTICLES (use these as your source) ══\n\n");
            for (int i = 0; i < limit; i++) {
                RawArticle a = articles.get(i);
                p.append("ARTICLE ").append(i + 1).append(":\n");
                p.append("  Title : ").append(a.title()).append("\n");
                p.append("  URL   : ").append(a.url()).append("\n");
                if (!a.description().isBlank()) {
                    String desc = a.description().length() > 300
                            ? a.description().substring(0, 300) + "..." : a.description();
                    p.append("  Desc  : ").append(desc).append("\n");
                }
                p.append("\n");
            }
            p.append("══ END ARTICLES ══\n\n");
            p.append("Rules for using articles:\n");
            p.append("- Use ONLY articles relevant to '").append(geoScope).append("'.\n");
            p.append("- Use ONLY articles relevant to topic '").append(category).append("'.\n");
            if (!weatherFallback)
                p.append("- PRIORITY: If any article mentions Banate, Iloilo — list it first.\n");
            p.append("- Do NOT invent facts. Stick to what is in the articles.\n\n");
        } else {
            p.append("No RSS articles available.\n");
            if (weatherFallback)
                p.append("Write weather news for the Philippines from general knowledge (PAGASA, typhoon patterns).\n");
            else
                p.append("Write from general knowledge about '").append(geoScope)
                        .append("' and topic '").append(category).append("'.\n");
            p.append("Do NOT fabricate specific names, dates, or figures.\n\n");
        }

        // Output format
        p.append("Produce EXACTLY ").append(TARGET).append(" SMS news items.\n");
        p.append("Each SMS body: 2-3 complete sentences, 40-80 words.\n");
        p.append("No URLs inside the SMS body. No ellipsis (...).\n\n");

        p.append("STRICT LINE FORMAT — one line per item, no blank lines between items:\n");
        p.append("{N}. {English SMS body} | Headline: {exact article title} | ArticleIndex: {1-based number}\n\n");

        p.append("OUTPUT RULES:\n");
        p.append("1. Each item must be from a DIFFERENT article.\n");
        p.append("2. Headline must EXACTLY match the article Title above.\n");
        p.append("3. ArticleIndex must be the correct 1-based number.\n");
        p.append("4. Output the numbered list ONLY — no intro, no closing, no markdown.\n");

        return p.toString();
    }

    // =========================================================================
    //  STEP 2 — Parse Claude's output
    // =========================================================================

    /** Holds a single parsed English SMS item before translation. */
    private record ParsedItem(String englishBody, String headline, String url) {}

    private List<ParsedItem> parseClaudeOutput(String raw, List<RawArticle> articles) {
        List<ParsedItem> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;

        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;

            // Try full pattern first (with ArticleIndex)
            Matcher mf = LINE_FULL.matcher(t);
            if (mf.matches()) {
                String body     = cleanText(mf.group(1));
                String headline = mf.group(2).trim();
                int    idx      = safeInt(mf.group(3));
                String url      = resolveUrl(idx, headline, articles);
                out.add(new ParsedItem(body, headline, url));
                System.out.printf("[Parse] ✓ Item %d — \"%s\" (%d chars)%n",
                        out.size(), headline, body.length());
                continue;
            }

            // Fallback (no ArticleIndex)
            Matcher fb = LINE_FALLBACK.matcher(t);
            if (fb.matches()) {
                String body     = cleanText(fb.group(1));
                String headline = fb.group(2).trim();
                String url      = resolveByTitle(headline, articles);
                out.add(new ParsedItem(body, headline, url));
                System.out.printf("[Parse] ~ Item %d (fallback) — \"%s\" (%d chars)%n",
                        out.size(), headline, body.length());
            }
        }
        return out;
    }

    // =========================================================================
    //  STEP 3 — Google Translate: English → Hiligaynon
    // =========================================================================

    private List<NewsItem> translateToHiligaynon(List<ParsedItem> items) {
        List<String> englishBodies = items.stream()
                .map(ParsedItem::englishBody)
                .collect(Collectors.toList());

        System.out.printf("[Translate] Sending %d items to Google Translate (en → hil)...%n",
                englishBodies.size());

        List<String> translated = callGoogleTranslate(englishBodies);

        List<NewsItem> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ParsedItem p   = items.get(i);
            String english = p.englishBody();
            String hil     = (i < translated.size() && translated.get(i) != null)
                    ? translated.get(i)
                    : english; // fallback to English if translation failed

            String finalText = padText(cleanText(hil), p.headline());

            System.out.printf("[Translate] Item %d: EN=%d chars → HIL=%d chars%n",
                    i + 1, english.length(), finalText.length());

            result.add(new NewsItem(finalText, p.url()));
        }
        return result;
    }

    /**
     * Calls Google Cloud Translation API v2 (Basic).
     * Sends all texts in one batch POST request.
     * Returns translated strings in the same order; null entries mean the translation failed.
     */
    private List<String> callGoogleTranslate(List<String> texts) {
        if (texts.isEmpty()) return List.of();
        try {
            // Build URL-encoded body: key + source + target + multiple &q= params
            StringBuilder body = new StringBuilder();
            body.append("key=")    .append(URLEncoder.encode(googleApiKey, StandardCharsets.UTF_8));
            body.append("&source=").append(URLEncoder.encode(LANG_SOURCE,  StandardCharsets.UTF_8));
            body.append("&target=").append(URLEncoder.encode(LANG_TARGET,  StandardCharsets.UTF_8));
            body.append("&format=text");
            for (String text : texts)
                body.append("&q=").append(URLEncoder.encode(text, StandardCharsets.UTF_8));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_TRANSLATE_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(java.time.Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                System.err.printf("[Translate] HTTP %d: %s%n", resp.statusCode(), resp.body());
                return new ArrayList<>(texts); // graceful fallback
            }

            // Parse: { "data": { "translations": [ { "translatedText": "..." }, ... ] } }
            JsonNode root  = json.readTree(resp.body());
            JsonNode trans = root.path("data").path("translations");

            List<String> results = new ArrayList<>();
            if (trans.isArray())
                for (JsonNode node : trans) {
                    String t = node.path("translatedText").asText(null);
                    if (t != null) t = unescapeHtml(t);
                    results.add(t);
                }

            System.out.printf("[Translate] ✓ %d/%d items translated (en → hil)%n",
                    results.size(), texts.size());
            return results;

        } catch (Exception e) {
            System.err.println("[Translate] Error: " + e.getMessage());
            return new ArrayList<>(texts); // graceful fallback: return English
        }
    }

    private String unescapeHtml(String s) {
        return s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'");
    }

    // =========================================================================
    //  Text utilities
    // =========================================================================

    private String cleanText(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        if (s.length() > MAX_LEN) s = truncate(s, MAX_LEN);
        if (!s.isEmpty() && !s.endsWith(".") && !s.endsWith("!") && !s.endsWith("?")) s += ".";
        return s;
    }

    /** Pads short Hiligaynon text with authentic Iloilo Hiligaynon filler phrases. */
    private String padText(String sms, String headline) {
        if (sms == null) return "";
        if (sms.length() >= MIN_LEN) return sms;

        String[] pads = {
                " Ini nga balita gikan sa: " + headline.trim() + ".",
                " Ginapahibalo ang tanan nga nagainteresar sa maong hitabo nga magbantay sa dugang nga impormasyon.",
                " Ang mga tawo sa lugar ginpaabot ang dugang nga balita parte sini sa masunod nga mga adlaw.",
                " Ang awtoridad nagpadayon sang ila pag-usisa kag magahatag sang update sa publiko.",
                " Ang hitabo nagapadayon pa kag ang publiko ginahangyo nga mag-amping.",
        };

        String result = sms;
        for (String pad : pads) {
            if (result.length() >= MIN_LEN) break;
            String base = result.endsWith(".") || result.endsWith("!") || result.endsWith("?")
                    ? result.substring(0, result.length() - 1) : result;
            String candidate = base + pad;
            if (candidate.length() <= MAX_LEN) {
                result = candidate;
            } else {
                int space = MAX_LEN - result.length();
                if (space > 10) {
                    String tp = pad.substring(0, space).trim();
                    if (!tp.endsWith(".")) tp += ".";
                    result = result + tp;
                }
                break;
            }
        }
        if (!result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?")) result += ".";
        return result;
    }

    private String truncate(String text, int max) {
        if (text.length() <= max) return text;
        String sub  = text.substring(0, max);
        int lastEnd = Math.max(sub.lastIndexOf('.'),
                Math.max(sub.lastIndexOf('!'), sub.lastIndexOf('?')));
        if (lastEnd > max / 2) return sub.substring(0, lastEnd + 1).trim();
        int lastSpc = sub.lastIndexOf(' ');
        if (lastSpc > max / 2) {
            String cut = sub.substring(0, lastSpc).trim();
            if (!cut.endsWith(".") && !cut.endsWith("!") && !cut.endsWith("?")) cut += ".";
            return cut;
        }
        return sub.trim();
    }

    // =========================================================================
    //  Selection
    // =========================================================================

    private List<NewsItem> selectBest(List<NewsItem> candidates, int target) {
        if (candidates.isEmpty()) return List.of();

        int mid = (MIN_LEN + MAX_LEN) / 2;
        List<NewsItem> sorted = candidates.stream()
                .sorted(Comparator.comparingInt(it -> Math.abs(it.smsText().length() - mid)))
                .collect(Collectors.toList());

        // Deduplicate by URL, then fill with no-URL items
        LinkedHashMap<String, NewsItem> byUrl = new LinkedHashMap<>();
        List<NewsItem> noUrl = new ArrayList<>();
        for (NewsItem it : sorted) {
            String key = (it.url() == null) ? "" : it.url().trim();
            if (key.isEmpty()) noUrl.add(it);
            else if (!byUrl.containsKey(key)) byUrl.put(key, it);
        }

        List<NewsItem> result = new ArrayList<>(byUrl.values());
        for (NewsItem it : noUrl)  { if (result.size() >= target) break; result.add(it); }

        Set<NewsItem> seen = new HashSet<>(result);
        for (NewsItem it : sorted) {
            if (result.size() >= target) break;
            if (seen.add(it)) result.add(it);
        }

        System.out.printf("[Select] %d candidates → %d selected%n",
                candidates.size(), Math.min(target, result.size()));
        return result.subList(0, Math.min(target, result.size()));
    }

    // =========================================================================
    //  URL resolution
    // =========================================================================

    private String resolveUrl(int idx, String headline, List<RawArticle> articles) {
        if (idx >= 1 && idx <= articles.size()) return articles.get(idx - 1).url();
        return resolveByTitle(headline, articles);
    }

    private String resolveByTitle(String headline, List<RawArticle> articles) {
        if (articles.isEmpty() || headline == null || headline.isBlank()) return "";
        Set<String> words = Arrays.stream(headline.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(w -> w.length() > 3).collect(Collectors.toSet());
        RawArticle best = null; int bestScore = 0;
        for (RawArticle a : articles) {
            int score = 0;
            String lc = a.title().toLowerCase(Locale.ROOT);
            for (String w : words) if (lc.contains(w)) score++;
            if (score > bestScore) { bestScore = score; best = a; }
        }
        return (best != null && bestScore > 0) ? best.url() : "";
    }

    // =========================================================================
    //  Keyword & filter helpers
    // =========================================================================

    private List<String> buildKeywords(String catLc, String topicLc) {
        List<String> kw = new ArrayList<>();
        if (!topicLc.isBlank())
            for (String w : topicLc.split("[\\s/,]+")) if (w.length() > 2) kw.add(w);

        if (catLc.contains("weather"))
            kw.addAll(List.of("bagyo","weather","ulan","baha","flood","storm","typhoon","rainfall","pagasa","drought","init","heat"));
        else if (catLc.contains("crime") || catLc.contains("law") || catLc.contains("safety"))
            kw.addAll(List.of("crime","police","pulis","arrested","murder","robbery","law","court","verdict","sentenced","illegal","drug"));
        else if (catLc.contains("health"))
            kw.addAll(List.of("health","hospital","disease","virus","patient","medical","dengue","covid","doctor","vaccine","outbreak"));
        else if (catLc.contains("politics"))
            kw.addAll(List.of("politics","election","mayor","governor","congress","senate","president","government","policy","vote"));
        else if (catLc.contains("national"))
            kw.addAll(List.of("philippine","philippines","national","manila","marcos","senate","house","pagasa","ndrrmc","doh"));
        else
            kw.addAll(iloiloKeywords());

        return kw;
    }

    private List<String> iloiloKeywords() {
        return List.of("iloilo","banate","ajuy","alimodian","anilao","badiangan","balasan",
                "barotac nuevo","barotac viejo","batad","bingawan","cabatuan","calinog",
                "carles","concepcion","dingle","duenas","dumangas","estancia","guimbal",
                "igbaras","janiuay","lambunao","leganes","lemery","leon","maasin","miagao",
                "mina","new lucena","oton","pavia","pototan","san dionisio","san enrique",
                "san joaquin","san miguel","san rafael","santa barbara","sara","tigbauan",
                "tubungan","zarraga","jaro","la paz","molo","mandurriao","arevalo");
    }

    private boolean hasIloiloContent(List<RawArticle> articles) {
        List<String> kw = iloiloKeywords();
        return articles.stream().anyMatch(a -> matches(a, kw));
    }

    private boolean matches(RawArticle a, List<String> keywords) {
        String t = text(a);
        return keywords.stream().anyMatch(t::contains);
    }

    private String text(RawArticle a) {
        return (a.title() + " " + a.description()).toLowerCase(Locale.ROOT);
    }

    // =========================================================================
    //  Misc helpers
    // =========================================================================

    private enum CategoryGroup { LOCAL, WEATHER, NATIONAL }

    private CategoryGroup resolveGroup(String category) {
        if (category == null || category.isBlank()) return CategoryGroup.LOCAL;
        String lc = category.toLowerCase(Locale.ROOT).trim();
        if (WEATHER_CATEGORIES.contains(lc))  return CategoryGroup.WEATHER;
        if (NATIONAL_CATEGORIES.contains(lc)) return CategoryGroup.NATIONAL;
        return CategoryGroup.LOCAL;
    }

    private String firstMatch(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String cleanHtml(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]+>", " ")
                .replace("&amp;","&").replace("&lt;","<").replace("&gt;",">")
                .replace("&quot;","\"").replace("&#39;","'").replace("&apos;","'")
                .replace("&nbsp;"," ").replace("&#8211;","-").replace("&#8212;","--")
                .replaceAll("\\s+", " ").trim();
    }

    private int countCompleteLines(String buffer) {
        if (buffer == null || buffer.isBlank()) return 0;
        Matcher m = LINE_COMPLETE.matcher(buffer);
        int n = 0; while (m.find()) n++;
        return n;
    }

    private String extractDelta(RawMessageStreamEvent event) {
        if (event == null) return null;
        try {
            return event.contentBlockDelta()
                    .flatMap(b -> b.delta().text())
                    .map(td -> td.text())
                    .orElse(null);
        } catch (Exception e) { return null; }
    }

    private int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return -1; }
    }

    private long elapsed(long startMs) {
        return (System.currentTimeMillis() - startMs) / 1000;
    }

    private void emit(BiConsumer<Double, String> cb, double pct, String msg) {
        if (cb != null) cb.accept(Math.max(0.0, Math.min(1.0, pct)), msg);
    }

    private List<NewsItem> cancelled() {
        System.out.println("[News] Cancelled.");
        return List.of();
    }

    // =========================================================================
    //  Records
    // =========================================================================
    public record RawArticle(String title, String url, String description) {}
}