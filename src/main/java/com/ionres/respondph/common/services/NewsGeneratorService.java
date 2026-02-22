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

public class NewsGeneratorService {

    // ─── Env vars ────────────────────────────────────────────────────────────
    private static final String ENV_ANTHROPIC_KEY    = "ANTHROPIC_API_KEY";
    private static final String ENV_GOOGLE_TRANS_KEY = "GOOGLE_TRANSLATE_API_KEY";

    private static final Model MODEL_ID = Model.CLAUDE_SONNET_4_5_20250929;

    private static final String GOOGLE_TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2";
    private static final String LANG_SOURCE          = "en";
    private static final String LANG_TARGET          = "hil"; // Hiligaynon

    private static final int TARGET      = 5;   // number of SMS items to produce
    private static final int EN_MIN      = 155; // min English chars sent to Claude
    private static final int EN_MAX      = 200; // max English chars sent to Claude
    private static final int HIL_MIN     = 280; // min Hiligaynon chars in final output
    private static final int HIL_MAX     = 320; // max Hiligaynon chars in final output
    private static final int MAX_RETRIES = 3;   // max shorten/expand retries per item

    // ─── HTTP / RSS ───────────────────────────────────────────────────────────
    private static final int HTTP_TIMEOUT_SEC        = 12;
    private static final int MAX_ARTICLES_PER_SOURCE = 20;
    private static final int MAX_ARTICLES_IN_PROMPT  = 15;

    // ─── RSS feed lists ───────────────────────────────────────────────────────
    // LOCAL: Iloilo/Panay local news only
    private static final List<String> RSS_LOCAL = List.of(
            "https://panaynews.net/feed/",
            "https://www.dailyguardian.com.ph/feed/"
    );

    // WEATHER: weather-specific feeds only
    private static final List<String> RSS_WEATHER = List.of(
            "https://data.gmanetwork.com/gno/rss/weather/feed.xml",
            "https://www.weather.gov/rss/"
    );

    // NATIONAL: broad national Philippine news
    private static final List<String> RSS_NATIONAL = List.of(
            "https://data.gmanetwork.com/gno/rss/news/feed.xml",
            "https://www.philstar.com/rss/headlines"
    );

    // POLITICS: politics-focused feeds
    private static final List<String> RSS_POLITICS = List.of(
            "https://data.gmanetwork.com/gno/rss/news/feed.xml",
            "https://www.philstar.com/rss/headlines",
            "https://abcnews.com/abcnews/politicsheadlines"
    );

    // HEALTH: health-focused feeds
    private static final List<String> RSS_HEALTH = List.of(
            "https://data.gmanetwork.com/gno/rss/news/feed.xml",
            "https://www.philstar.com/rss/headlines",
            "https://abcnews.com/abcnews/healthheadlines"
    );

    // CRIME / LAW / PUBLIC SAFETY
    private static final List<String> RSS_CRIME = List.of(
            "https://data.gmanetwork.com/gno/rss/news/feed.xml",
            "https://www.philstar.com/rss/headlines"
    );

    // ─── Category sets ────────────────────────────────────────────────────────
    private static final Set<String> WEATHER_CATEGORIES = Set.of(
            "weather news", "weather", "weather update"
    );
    private static final Set<String> NATIONAL_CATEGORIES = Set.of(
            "national news", "politics news", "health news", "crime / law / public safety news"
    );

    // ─── Geo scopes ───────────────────────────────────────────────────────────
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
    private final ObjectMapper            json         = new ObjectMapper();
    private final AtomicBoolean           cancelled    = new AtomicBoolean(false);
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
        System.out.println("[News]   Step 2 → Claude writes English SMS summaries (target " + EN_MIN + "-" + EN_MAX + " chars)");
        System.out.println("[News]   Step 3 → Google Translate converts to Hiligaynon (hil): "
                + (googleApiKey != null ? "ENABLED" : "DISABLED – set GOOGLE_TRANSLATE_API_KEY"));
        System.out.println("[News]   Step 4 → Length enforcer: " + HIL_MIN + "-" + HIL_MAX + " chars, no word cuts");
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

            AtomicInteger confirmed = new AtomicInteger(0);
            AtomicBoolean done      = new AtomicBoolean(false);

            ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "news-ticker");
                t.setDaemon(true);
                return t;
            });
            ScheduledFuture<?> tick = ticker.scheduleAtFixedRate(() -> {
                if (done.get()) return;
                int n      = confirmed.get();
                long secs  = elapsed(start);
                String msg;
                if (n == 0) {
                    msg = "Fetching RSS articles... (" + secs + "s)";
                } else {
                    // Must match MainFrameController regex: "^\d+ of \d+ items? found.*"
                    msg = n + " of " + TARGET + " items found (" + secs + "s)";
                }
                double pct = 0.10 + (double) n / TARGET * 0.70;
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
                    case LOCAL    -> GEO_LOCAL;
                    case WEATHER  -> iloiloWeather ? GEO_WEATHER_ILOILO : GEO_WEATHER_NATIONAL;
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
                            // ⚠ Message MUST match MainFrameController regex: "^\d+ of \d+ items? found.*"
                            emit(onProgress, pct, nowCount + " of " + TARGET + " items found (writing summaries...)");
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

                // ── Enforce English length before translating ─────────────────
                List<ParsedItem> lengthAdjusted = enforceEnglishLength(parsed);

                // ══════════════════════════════════════════════════════════════
                //  STEP 3 — Google Translate: English → Hiligaynon
                // ══════════════════════════════════════════════════════════════
                List<NewsItem> candidates;

                if (googleApiKey != null) {
                    emit(onProgress, 0.85, "Step 3/3 — Google Translate: English → Hiligaynon...");
                    candidates = translateToHiligaynon(lengthAdjusted, category);
                } else {
                    System.out.println("[Translate] SKIPPED — GOOGLE_TRANSLATE_API_KEY not set. Output stays in English.");
                    candidates = lengthAdjusted.stream()
                            .map(p -> new NewsItem(enforceHiligaynonLength(p.englishBody(), p.englishBody(), p.headline(), category), p.url()))
                            .collect(Collectors.toList());
                }

                System.out.printf("[Done] %d candidates ready%n", candidates.size());

                List<NewsItem> best = selectBest(candidates, TARGET);

                emit(onProgress, 1.0,
                        "Done — " + best.size() + " of " + TARGET + " items generated in " + elapsed(start) + "s");
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
        // Route to the exact feeds for the selected topic — no cross-category pollution.
        List<String> feeds = resolveFeedsForCategory(group, category);

        System.out.printf("[RSS] Category='%s' → %d feed(s) selected%n", category, feeds.size());

        List<CompletableFuture<List<RawArticle>>> futures = feeds.stream()
                .map(url -> CompletableFuture.supplyAsync(() -> fetchOneFeed(url)))
                .toList();

        List<RawArticle> all = new ArrayList<>();
        for (var f : futures) {
            try { all.addAll(f.get(HTTP_TIMEOUT_SEC + 3L, TimeUnit.SECONDS)); }
            catch (Exception e) { System.err.println("[RSS] Feed error: " + e.getMessage()); }
        }

        List<RawArticle> filtered = group == CategoryGroup.WEATHER
                ? filterWeather(all)
                : filterByKeywords(all, topic, category, group);

        return dedup(filtered);
    }

    /**
     * Returns the exact RSS feed list for the selected category.
     * Each topic gets only the feeds most relevant to it — no cross-topic feeds mixed in.
     */
    private List<String> resolveFeedsForCategory(CategoryGroup group, String category) {
        if (group == CategoryGroup.WEATHER) return RSS_WEATHER;
        if (group == CategoryGroup.LOCAL)   return RSS_LOCAL;

        // National sub-categories — route to topic-specific feeds
        String lc = category != null ? category.toLowerCase(Locale.ROOT).trim() : "";
        return switch (lc) {
            case "politics news"                    -> RSS_POLITICS;
            case "health news"                      -> RSS_HEALTH;
            case "crime / law / public safety news" -> RSS_CRIME;
            default                                 -> RSS_NATIONAL; // "national news" or unknown
        };
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

        List<RawArticle> iloiloWeather = all.stream()
                .filter(a -> matches(a, weatherKw) && matches(a, iloiloKw))
                .collect(Collectors.toList());

        System.out.printf("[RSS] Weather: %d Iloilo-specific articles%n", iloiloWeather.size());

        if (!iloiloWeather.isEmpty()) {
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

    /**
     * Filters articles to ONLY those matching the selected topic keywords.
     * STRICT: if nothing matches, returns empty list — never falls back to returning
     * all articles, which would allow off-topic content to reach Claude.
     */
    private List<RawArticle> filterByKeywords(
            List<RawArticle> all, String topic, String category, CategoryGroup group) {

        String catLc   = category != null ? category.toLowerCase(Locale.ROOT) : "";
        String topicLc = topic    != null ? topic.toLowerCase(Locale.ROOT)    : "";

        List<String> mustMatch  = buildMustMatchKeywords(catLc);   // at least one of these MUST match
        List<String> extraBoost = buildBoostKeywords(topicLc);     // optional scoring boost

        List<RawArticle> matched = all.stream()
                .filter(a -> matchesAny(a, mustMatch))             // HARD filter — topic keywords only
                .collect(Collectors.toList());

        System.out.printf("[Filter] Category='%s': %d/%d articles passed topic filter%n",
                category, matched.size(), all.size());

        // Never fall back to returning all — return empty so Claude gets no off-topic articles.
        return matched;
    }

    /**
     * Returns the mandatory topic keywords — an article MUST match at least one of these
     * to pass the filter for the given category.
     */
    private List<String> buildMustMatchKeywords(String catLc) {
        if (catLc.contains("weather"))
            return List.of(
                    "weather","bagyo","typhoon","storm","flood","baha","ulan","rainfall",
                    "pagasa","drought","heat","signal no","tropical","rain","wind","cloudy",
                    "temperature","humidity","monsoon","habagat","amihan","lamdag","init"
            );
        if (catLc.contains("health"))
            return List.of(
                    "health","hospital","disease","virus","patient","medical","dengue","covid",
                    "doctor","vaccine","outbreak","doh","pandemic","epidemic","infection",
                    "clinic","medicine","illness","sick","mortality","treatment","quarantine",
                    "tuberculosis","malaria","measles","nutrition","mental health","wellness"
            );
        if (catLc.contains("politics"))
            return List.of(
                    "election","senator","congressman","congress","senate","president","governor",
                    "mayor","politics","political","government","policy","vote","campaign",
                    "comelec","marcos","duterte","partido","official","administration","bill",
                    "legislation","ordinance","executive order","cabinet","department secretary"
            );
        if (catLc.contains("crime") || catLc.contains("law") || catLc.contains("safety"))
            return List.of(
                    "crime","police","arrested","murder","robbery","law","court","verdict",
                    "sentenced","illegal","drug","killed","stabbed","shot","suspect","firearm",
                    "criminal","warrant","investigation","nbi","pnp","raid","carnap","theft",
                    "assault","smuggling","trafficking","extortion","corruption","scam","fraud"
            );
        if (catLc.contains("national"))
            return List.of(
                    "philippine","philippines","national","manila","luzon","visayas","mindanao",
                    "marcos","duterte","senate","congress","pagasa","ndrrmc","doh","deped",
                    "dpwh","dilg","dole","da","bsp","bangko sentral","peso","gdp","economy"
            );
        // LOCAL — must mention Iloilo/Panay area
        return iloiloKeywords();
    }

    /**
     * Returns optional boost keywords for the topic (used for scoring/ordering if needed in future).
     * Currently unused in filtering but kept for extensibility.
     */
    private List<String> buildBoostKeywords(String topicLc) {
        List<String> kw = new ArrayList<>();
        for (String w : topicLc.split("[\s/,]+"))
            if (w.length() > 2) kw.add(w);
        return kw;
    }

    private boolean matchesAny(RawArticle a, List<String> keywords) {
        String t = text(a);
        return keywords.stream().anyMatch(t::contains);
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
    //  Target English length: EN_MIN–EN_MAX (155–200 chars)
    //  Hiligaynon is ~30-40% longer, so this maps to HIL_MIN–HIL_MAX (280–320).
    //  All items must strictly match the selected topic/category.
    // =========================================================================

    /**
     * Returns explicit negative rules for the Claude prompt — what NOT to write about
     * for each topic. This prevents Claude from including off-topic items even when
     * the article description is vague or tangentially related.
     */
    /**
     * Returns explicit negative + positive rules for Claude per topic.
     * Uses plain string concatenation (no text blocks) for reliable formatting.
     */
    private String buildNegativeTopicRules(String category) {
        if (category == null) return "";
        String lc = category.toLowerCase(Locale.ROOT).trim();
        return switch (lc) {
            case "weather news", "weather", "weather update" ->
                    "  \u274C DO NOT write about: politics, elections, crime, health outbreaks, sports, entertainment.\n" +
                            "  \u2705 ONLY write about: typhoons, floods, rainfall, temperature, PAGASA advisories, storm signals, drought, heat index, weather forecasts.";
            case "health news" ->
                    "  \u274C DO NOT write about: politics, elections, crime, weather, sports, entertainment, traffic, infrastructure.\n" +
                            "  \u2705 ONLY write about: disease outbreaks, vaccines, hospitals, DOH advisories, medical treatments, epidemics, public health alerts, dengue, COVID, nutrition.";
            case "politics news" ->
                    "  \u274C DO NOT write about: weather, crime incidents, health outbreaks, sports, entertainment, traffic accidents.\n" +
                            "  \u2705 ONLY write about: elections, legislation, government policy, COMELEC, senators, congressmen, local officials, executive orders, political campaigns.";
            case "crime / law / public safety news" ->
                    "  \u274C DO NOT write about: weather, health, politics, sports, entertainment, infrastructure.\n" +
                            "  \u2705 ONLY write about: crimes, arrests, court verdicts, police operations, NBI, PNP, drug busts, murders, robberies, trafficking, scams, fraud, public safety warnings.";
            case "national news" ->
                    "  \u274C DO NOT write about: purely local barangay issues, entertainment gossip, sports scores.\n" +
                            "  \u2705 ONLY write about: national-level Philippine news affecting the whole country.";
            default ->
                    "  \u274C DO NOT write about: national politics, international events, sports, entertainment.\n" +
                            "  \u2705 ONLY write about: events, incidents, or news directly in Iloilo City or Iloilo Province.";
        };
    }

    private String buildPrompt(
            String topic, String category, String geoScope, String today,
            List<RawArticle> articles, boolean weatherFallback) {

        StringBuilder p = new StringBuilder();

        p.append("You are an expert SMS news writer for a Philippine local disaster-response service.\n\n");

        // ── Language rule ──────────────────────────────────────────────────────
        p.append("LANGUAGE RULE: Write every SMS body in CLEAR, PLAIN ENGLISH only.\n");
        p.append("Do NOT use Hiligaynon, Tagalog, or any other language.\n");
        p.append("The English text will be translated to Hiligaynon by Google Translate.\n\n");

        // ── TOPIC STRICTNESS ───────────────────────────────────────────────────
        p.append("══════════════════════════════════════════════════════\n");
        p.append("TOPIC RULE — THE SINGLE MOST IMPORTANT RULE. READ CAREFULLY:\n");
        p.append("  Selected topic : ").append(category).append("\n");
        p.append("  Geo scope      : ").append(geoScope).append("\n\n");
        p.append("  ✅ ONLY write items that are DIRECTLY about: '").append(category).append("'\n");
        p.append("  ❌ SKIP any article that is not directly about: '").append(category).append("'\n");
        p.append("  ❌ SKIP any article that is off-topic, even if it is interesting.\n");
        p.append("  ❌ DO NOT mix topics. One topic, one category, zero exceptions.\n");
        // Explicit negative rules per topic
        p.append(buildNegativeTopicRules(category)).append("\n");
        if (weatherFallback)
            p.append("  NOTE: No Iloilo weather found — cover weather for the entire Philippines.\n");
        p.append("  SELF-CHECK: Before outputting each item, ask yourself:\n");
        p.append("    → Is this item DIRECTLY about '").append(category).append("'?\n");
        p.append("    → If NO or UNSURE → do NOT include it.\n");
        p.append("══════════════════════════════════════════════════════\n\n");

        p.append("Today's date: ").append(today).append("\n\n");

        // ── Length rule — KEY for 280-320 Hiligaynon result ───────────────────
        p.append("SMS LENGTH RULE — CRITICAL:\n");
        p.append("  Each English SMS body MUST be between ").append(EN_MIN).append(" and ").append(EN_MAX).append(" characters.\n");
        p.append("  Count characters carefully before outputting each item.\n");
        p.append("  Hiligaynon is ~35% longer than English.\n");
        p.append("  Targeting ").append(EN_MIN).append("-").append(EN_MAX).append(" English chars ensures the Hiligaynon translation hits 280-320 chars.\n");
        p.append("  If your draft is too short, add one more relevant detail (who, what, where, impact).\n");
        p.append("  If your draft is too long, remove the least important detail.\n");
        p.append("  NEVER cut a sentence mid-word. Always end with a complete sentence.\n\n");

        // ── Article sources ────────────────────────────────────────────────────
        if (!articles.isEmpty()) {
            int limit = Math.min(articles.size(), MAX_ARTICLES_IN_PROMPT);
            p.append("══ RSS ARTICLES — use ONLY articles that match topic '").append(category).append("' ══\n\n");
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
            p.append("Rules:\n");
            p.append("- Use ONLY articles about '").append(category).append("'. Skip any that are off-topic.\n");
            p.append("- Use ONLY articles relevant to '").append(geoScope).append("'.\n");
            if (!weatherFallback)
                p.append("- PRIORITY: If any article mentions Banate, Iloilo — list it first.\n");
            p.append("- Do NOT invent facts. Stick to what is in the articles.\n");
            p.append("- Each item must come from a DIFFERENT article.\n\n");
        } else {
            // No articles passed the topic filter — instruct Claude to produce
            // ONLY general factual context for this topic, not invented specifics.
            p.append("No RSS articles are available for this topic right now.\n");
            p.append("You MUST still write ONLY about topic: '").append(category).append("'\n");
            p.append("Write general factual awareness content about '").append(category)
                    .append("' relevant to '").append(geoScope).append("'\n");
            p.append("STRICT RULES when no articles are available:\n");
            p.append("  - Do NOT fabricate specific names, dates, numbers, or incidents.\n");
            p.append("  - Do NOT write about any other topic.\n");
            p.append("  - Write general public awareness tips or standing advisories only.\n");
            p.append("  - Every item must still be about '").append(category).append("' exclusively.\n\n");
        }

        // ── Output format ──────────────────────────────────────────────────────
        p.append("Produce EXACTLY ").append(TARGET).append(" SMS news items.\n");
        p.append("Each SMS body: 2-3 complete sentences.\n");
        p.append("Each SMS body: MUST be ").append(EN_MIN).append("-").append(EN_MAX).append(" characters (count carefully).\n");
        p.append("No URLs inside the SMS body. No ellipsis (...). No markdown.\n\n");

        p.append("STRICT LINE FORMAT — one line per item, NO blank lines between items:\n");
        p.append("{N}. {English SMS body} | Headline: {exact article title} | ArticleIndex: {1-based number}\n\n");

        p.append("OUTPUT RULES:\n");
        p.append("1. ALL items MUST be about topic '").append(category).append("'. Zero exceptions.\n");
        p.append("2. Each item must be from a DIFFERENT article.\n");
        p.append("3. Headline must EXACTLY match the article Title above.\n");
        p.append("4. ArticleIndex must be the correct 1-based number.\n");
        p.append("5. English SMS body must be ").append(EN_MIN).append("-").append(EN_MAX).append(" characters. Count before writing.\n");
        p.append("6. Output the numbered list ONLY — no intro, no closing, no markdown.\n");

        return p.toString();
    }

    // =========================================================================
    //  STEP 2 — Parse Claude's output
    // =========================================================================

    private record ParsedItem(String englishBody, String headline, String url) {}

    private List<ParsedItem> parseClaudeOutput(String raw, List<RawArticle> articles) {
        List<ParsedItem> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;

        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;

            Matcher mf = LINE_FULL.matcher(t);
            if (mf.matches()) {
                String body     = mf.group(1).trim();
                String headline = mf.group(2).trim();
                int    idx      = safeInt(mf.group(3));
                String url      = resolveUrl(idx, headline, articles);
                out.add(new ParsedItem(body, headline, url));
                System.out.printf("[Parse] ✓ Item %d — \"%s\" (%d chars EN)%n",
                        out.size(), headline, body.length());
                continue;
            }

            Matcher fb = LINE_FALLBACK.matcher(t);
            if (fb.matches()) {
                String body     = fb.group(1).trim();
                String headline = fb.group(2).trim();
                String url      = resolveByTitle(headline, articles);
                out.add(new ParsedItem(body, headline, url));
                System.out.printf("[Parse] ~ Item %d (fallback) — \"%s\" (%d chars EN)%n",
                        out.size(), headline, body.length());
            }
        }
        return out;
    }

    // =========================================================================
    //  English length enforcement (before translation)
    //  If Claude produced English that is clearly out of EN_MIN–EN_MAX range,
    //  we shorten or expand via a direct Claude call so the Hiligaynon result
    //  is more likely to land in 280–320 chars.
    // =========================================================================

    private List<ParsedItem> enforceEnglishLength(List<ParsedItem> items) {
        List<ParsedItem> result = new ArrayList<>();
        for (ParsedItem item : items) {
            ParsedItem adjusted = adjustEnglishLength(item);
            result.add(adjusted);
        }
        return result;
    }

    private ParsedItem adjustEnglishLength(ParsedItem item) {
        String body = item.englishBody();
        int len = body.length();

        // Already in range — no adjustment needed
        if (len >= EN_MIN && len <= EN_MAX) {
            System.out.printf("[EnLen] Item OK: %d chars (target %d-%d)%n", len, EN_MIN, EN_MAX);
            return item;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            String adjusted;
            if (body.length() > EN_MAX) {
                adjusted = claudeAdjust(body, "shorten", item.headline());
            } else {
                adjusted = claudeAdjust(body, "expand", item.headline());
            }

            if (adjusted == null || adjusted.isBlank()) break;

            int newLen = adjusted.length();
            System.out.printf("[EnLen] Attempt %d: %d → %d chars%n", attempt, len, newLen);

            if (newLen >= EN_MIN && newLen <= EN_MAX) {
                return new ParsedItem(adjusted, item.headline(), item.url());
            }
            body = adjusted;
        }

        // Could not reach target after retries — use best effort as-is.
        // If over EN_MAX the Hiligaynon enforcer will handle it via re-translation retries.
        // If under EN_MIN the Hiligaynon enforcer will pad with complete phrases.
        // We never cut English here — the Hiligaynon enforcer is the final authority.
        System.out.printf("[EnLen] Final (after retries): %d chars (out of target, passing to HIL enforcer)%n", body.length());
        return new ParsedItem(body, item.headline(), item.url());
    }

    /**
     * Calls Claude to shorten or expand a single English SMS body.
     * Returns null if the call fails.
     */
    private String claudeAdjust(String body, String direction, String topic) {
        String instruction;
        if ("shorten".equals(direction)) {
            instruction = String.format(
                    "Shorten this English SMS news alert to between %d and %d characters.\n"
                            + "RULES:\n"
                            + "- Remove the least important detail(s) to fit within %d characters.\n"
                            + "- Keep all sentences complete — never cut mid-word or mid-sentence.\n"
                            + "- Keep it about the same topic: '%s'.\n"
                            + "- Output ONLY the revised SMS text, nothing else.\n\n"
                            + "SMS text:\n%s",
                    EN_MIN, EN_MAX, EN_MAX, topic, body
            );
        } else {
            instruction = String.format(
                    "Expand this English SMS news alert to between %d and %d characters.\n"
                            + "RULES:\n"
                            + "- Add one useful detail (location, impact, action people should take, or who is affected).\n"
                            + "- Stay strictly on the same topic: '%s'. Do not add off-topic information.\n"
                            + "- All sentences must be complete — never leave a sentence unfinished.\n"
                            + "- Do not exceed %d characters.\n"
                            + "- Output ONLY the revised SMS text, nothing else.\n\n"
                            + "SMS text:\n%s",
                    EN_MIN, EN_MAX, topic, EN_MAX, body
            );
        }

        try {
            StringBuilder result = new StringBuilder();
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(MODEL_ID)
                    .maxTokens(400L)
                    .addUserMessage(instruction)
                    .build();

            try (StreamResponse<RawMessageStreamEvent> stream =
                         claude.messages().createStreaming(params)) {
                for (Iterator<RawMessageStreamEvent> it = stream.stream().iterator(); it.hasNext(); ) {
                    if (cancelled.get()) break;
                    RawMessageStreamEvent event;
                    try { event = it.next(); }
                    catch (Exception ex) { break; }
                    String delta = extractDelta(event);
                    if (delta != null) result.append(delta);
                }
            }

            String text = result.toString().replaceAll("\\s+", " ").trim();
            // Strip any leading numbering that Claude might add
            text = text.replaceAll("^\\d+\\.\\s*", "").trim();
            return text.isEmpty() ? null : text;

        } catch (Exception e) {
            System.err.println("[EnLen] Claude adjust error: " + e.getMessage());
            return null;
        }
    }

    // =========================================================================
    //  STEP 3 — Google Translate: English → Hiligaynon
    // =========================================================================

    private List<NewsItem> translateToHiligaynon(List<ParsedItem> items, String category) {
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
                    : english;

            // ── Enforce Hiligaynon length: 280-320 chars, no cuts ever ────────
            // Returns null if item cannot reach 280-320 without cutting — item is dropped.
            String finalText = enforceHiligaynonLength(hil, english, p.headline(), category);

            if (finalText == null) {
                System.out.printf("[Translate] Item %d DROPPED — could not fit 280-320 chars cleanly%n", i + 1);
                continue;
            }

            System.out.printf("[Translate] Item %d: EN=%d → HIL(final)=%d chars ✓%n",
                    i + 1, english.length(), finalText.length());

            result.add(new NewsItem(finalText, p.url()));
        }
        return result;
    }

    /**
     * Ensures the Hiligaynon text is between HIL_MIN (280) and HIL_MAX (320) characters.
     *
     * Strategy:
     *  - Too long (>320): shorten English → re-translate, repeat up to MAX_RETRIES.
     *                     If still over 320 after all retries, returns null (item is DROPPED).
     *                     NEVER truncates or cuts Hiligaynon text.
     *  - Too short (<280): expand English → re-translate, repeat up to MAX_RETRIES.
     *                      If still under 280, pad with authentic Hiligaynon filler phrases
     *                      (only whole complete phrases, never partial words).
     *                      If padding would exceed 320, item is DROPPED.
     *  - In range (280-320): return as-is.
     *
     * Returns null if the item cannot be brought into range without cutting.
     */
    private String enforceHiligaynonLength(String hilText, String englishText, String headline, String category) {
        String cleaned = cleanHilText(hilText);
        int len = cleaned.length();

        System.out.printf("[HilLen] Initial: %d chars (target %d-%d)%n", len, HIL_MIN, HIL_MAX);

        // ── Already in range ──────────────────────────────────────────────────
        if (len >= HIL_MIN && len <= HIL_MAX) {
            System.out.printf("[HilLen] In range: %d chars ✓%n", len);
            return cleaned;
        }

        // ── Too long: shorten English → re-translate (never cut Hiligaynon) ───
        if (len > HIL_MAX) {
            String shorterEnglish = englishText;
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                shorterEnglish = claudeAdjust(shorterEnglish, "shorten", category);
                if (shorterEnglish == null || shorterEnglish.isBlank()) {
                    System.out.printf("[HilLen] Shorten attempt %d: Claude returned empty — aborting%n", attempt);
                    break;
                }

                List<String> retranslated = callGoogleTranslate(List.of(shorterEnglish));
                if (!retranslated.isEmpty() && retranslated.get(0) != null) {
                    String candidate = cleanHilText(retranslated.get(0));
                    System.out.printf("[HilLen] Shorten attempt %d: %d chars%n", attempt, candidate.length());

                    if (candidate.length() <= HIL_MAX) {
                        // May now be under min — try to pad it up
                        if (candidate.length() >= HIL_MIN) {
                            System.out.printf("[HilLen] OK after shorten: %d chars ✓%n", candidate.length());
                            return candidate;
                        }
                        // Under min after shortening — pad it
                        String padded = padHiligaynon(candidate, headline);
                        if (padded.length() >= HIL_MIN && padded.length() <= HIL_MAX) {
                            System.out.printf("[HilLen] OK after shorten+pad: %d chars ✓%n", padded.length());
                            return padded;
                        }
                        // Padding pushed it over 320 or still under 280 — keep trying to shorten
                        cleaned = candidate;
                    } else {
                        cleaned = candidate; // still too long, use as base for next attempt
                    }
                }
            }

            // All retries exhausted and still over 320 — DROP the item (never cut)
            System.out.printf("[HilLen] DROPPED — could not get under %d chars without cutting (%d chars after %d retries)%n",
                    HIL_MAX, cleaned.length(), MAX_RETRIES);
            return null;
        }

        // ── Too short: expand English → re-translate, then pad if needed ──────
        if (len < HIL_MIN) {
            String expandedEnglish = englishText;
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                expandedEnglish = claudeAdjust(expandedEnglish, "expand", category);
                if (expandedEnglish == null || expandedEnglish.isBlank()) {
                    System.out.printf("[HilLen] Expand attempt %d: Claude returned empty — stopping%n", attempt);
                    break;
                }

                List<String> retranslated = callGoogleTranslate(List.of(expandedEnglish));
                if (!retranslated.isEmpty() && retranslated.get(0) != null) {
                    String candidate = cleanHilText(retranslated.get(0));
                    System.out.printf("[HilLen] Expand attempt %d: %d chars%n", attempt, candidate.length());

                    if (candidate.length() >= HIL_MIN && candidate.length() <= HIL_MAX) {
                        System.out.printf("[HilLen] OK after expand: %d chars ✓%n", candidate.length());
                        return candidate;
                    }
                    // Got longer but still under min, or overshot — track best candidate
                    if (candidate.length() > cleaned.length() && candidate.length() <= HIL_MAX) {
                        cleaned = candidate;
                    }
                    // If overshot 320, don't use this candidate — keep original cleaned
                }
            }

            // Retries done — try padding what we have (only whole complete phrases)
            String padded = padHiligaynon(cleaned, headline);
            if (padded.length() >= HIL_MIN && padded.length() <= HIL_MAX) {
                System.out.printf("[HilLen] OK after pad: %d chars ✓%n", padded.length());
                return padded;
            }

            // Padding overshot 320 or still under 280 — DROP the item
            System.out.printf("[HilLen] DROPPED — could not reach %d-%d chars cleanly (%d chars)%n",
                    HIL_MIN, HIL_MAX, padded.length());
            return null;
        }

        return cleaned;
    }

    /**
     * Pads Hiligaynon text with complete Hiligaynon filler phrases until it reaches
     * HIL_MIN (280) characters. Only adds a phrase if the ENTIRE phrase fits within
     * HIL_MAX (320). Never adds partial phrases or cuts words.
     *
     * If no complete phrase fits and the text is still under HIL_MIN, returns the
     * text as-is (caller decides to drop it). If the text is already over HIL_MAX,
     * returns as-is without modification.
     */
    private String padHiligaynon(String text, String headline) {
        if (text == null) return "";
        text = text.trim();

        // Already over max — do not touch it
        if (text.length() > HIL_MAX) return text;

        String base = stripTrailingPunctuation(text);

        // Complete Hiligaynon filler phrases — from shortest to longest
        // so we can pick the best fit within the remaining space
        String[] pads = {
                " Ang publiko ginapatan-awan.",
                " Ang mga opisyal nagahatag sang update.",
                " Ginapahibalo ang tanan nga magbantay.",
                " Ang awtoridad nagamonitor sang kahimtangan.",
                " Ang mga tawo ginpahinumdom nga mag-amping.",
                " Ang mga opisyal nagapadayon sang pag-usisa.",
                " Ginapahibalo ang tanan nga magbantay sa dugang nga impormasyon.",
                " Ang mga awtoridad nagapadayon sang ila pag-usisa kag magahatag sang update sa publiko.",
                " Ang mga tawo sa lugar ginpaabot nga mag-amping kag magsunod sa mga direktibo.",
                " Ang publiko ginpahinumdom nga mag-report sang bisan ano nga kahimtangan sa lokal nga awtoridad.",
                " Ang hitabo nagapadayon pa kag ang mga opisyal nagahimo sang kinahanglan nga aksyon.",
                " Ginasiling sang mga opisyal nga ang kahimtangan ginamonitor sing maayo kag ang publiko ipahibalo."
        };

        String result = base;
        boolean madeProgress = true;

        while (result.length() < HIL_MIN && madeProgress) {
            madeProgress = false;
            // Find the longest complete phrase that fits without exceeding HIL_MAX
            String bestFit = null;
            for (String pad : pads) {
                String candidate = result + pad;
                if (candidate.length() <= HIL_MAX) {
                    // Take the longest fitting phrase
                    if (bestFit == null || pad.length() > bestFit.length()) {
                        bestFit = pad;
                    }
                }
            }
            if (bestFit != null) {
                result = result + bestFit;
                madeProgress = true;
            }
            // No phrase fits without exceeding 320 — stop
        }

        // Ensure proper ending punctuation
        if (!result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?"))
            result += ".";

        return result;
    }

    /**
     * Removes trailing sentence-ending punctuation from text.
     */
    private String stripTrailingPunctuation(String text) {
        if (text == null || text.isBlank()) return "";
        text = text.trim();
        if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?"))
            return text.substring(0, text.length() - 1);
        return text;
    }

    /**
     * Cleans Hiligaynon text: normalise whitespace, ensure ends with punctuation.
     * Does NOT truncate — length enforcement is separate.
     */
    private String cleanHilText(String s) {
        if (s == null) return "";
        s = unescapeHtml(s);
        s = s.replaceAll("\\s+", " ").trim();
        if (!s.isEmpty() && !s.endsWith(".") && !s.endsWith("!") && !s.endsWith("?"))
            s += ".";
        return s;
    }

    /**
     * Calls Google Cloud Translation API v2 (Basic).
     */
    private List<String> callGoogleTranslate(List<String> texts) {
        if (texts.isEmpty()) return List.of();
        try {
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
                return new ArrayList<>(texts);
            }

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
            return new ArrayList<>(texts);
        }
    }

    private String unescapeHtml(String s) {
        return s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'");
    }

    // =========================================================================
    //  Selection
    // =========================================================================

    private List<NewsItem> selectBest(List<NewsItem> candidates, int target) {
        if (candidates.isEmpty()) return List.of();

        int mid = (HIL_MIN + HIL_MAX) / 2; // 300

        // Prefer items in range first, then closest to mid-point
        List<NewsItem> inRange = candidates.stream()
                .filter(it -> it.smsText().length() >= HIL_MIN && it.smsText().length() <= HIL_MAX)
                .sorted(Comparator.comparingInt(it -> Math.abs(it.smsText().length() - mid)))
                .collect(Collectors.toList());

        List<NewsItem> outOfRange = candidates.stream()
                .filter(it -> it.smsText().length() < HIL_MIN || it.smsText().length() > HIL_MAX)
                .sorted(Comparator.comparingInt(it -> Math.abs(it.smsText().length() - mid)))
                .collect(Collectors.toList());

        List<NewsItem> sorted = new ArrayList<>(inRange);
        sorted.addAll(outOfRange);

        // Deduplicate by URL
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

        // Log final lengths
        List<NewsItem> finalResult = result.subList(0, Math.min(target, result.size()));
        for (int i = 0; i < finalResult.size(); i++) {
            int len = finalResult.get(i).smsText().length();
            boolean ok = len >= HIL_MIN && len <= HIL_MAX;
            System.out.printf("[Select] Item %d: %d chars %s%n", i + 1, len, ok ? "✓" : "⚠ OUT OF RANGE");
        }

        return finalResult;
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