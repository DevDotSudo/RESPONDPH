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
import java.util.Objects;
import java.util.concurrent.atomic.*;
import java.util.function.BiConsumer;
import java.util.regex.*;
import java.util.stream.Collectors;

public class NewsGeneratorService {

    // ─── Env vars ────────────────────────────────────────────────────────────
    private static final String ENV_ANTHROPIC_KEY    = "ANTHROPIC_API_KEY";
    private static final String ENV_GOOGLE_TRANS_KEY = "GOOGLE_TRANSLATE_API_KEY";
    // NOTE: WEATHER_API_KEY is no longer used. Google Weather API reuses GOOGLE_TRANSLATE_API_KEY
    // (both are Google Maps Platform APIs activated on the same Google Cloud project/key).

    // ─── Model ───────────────────────────────────────────────────────────────
    private static final Model MODEL_ID = Model.CLAUDE_SONNET_4_5_20250929;

    // ─── Google Translate ─────────────────────────────────────────────────────
    private static final String GOOGLE_TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2";
    private static final String LANG_SOURCE          = "en";
    private static final String LANG_TARGET          = "hil"; // Hiligaynon

    private static final String GOOGLE_WEATHER_URL          = "https://weather.googleapis.com/v1/currentConditions:lookup";
    // Forecast endpoint — returns an array of daily forecasts; we take index [1] = tomorrow.
    // Docs: https://developers.google.com/maps/documentation/weather/forecast
    // Params: key, location.latitude, location.longitude, unitsSystem=METRIC, days=2
    private static final String GOOGLE_WEATHER_FORECAST_URL = "https://weather.googleapis.com/v1/forecast/days:lookup";

    // Banate, Iloilo — the pinned location for weather Slot 1
    private static final double BANATE_LAT           = 11.0069373;
    private static final double BANATE_LNG           = 122.8255065;
    private static final String BANATE_LABEL         = "Banate, Iloilo";
    // AccuWeather source link shown as the URL for the Banate live-weather slot
    private static final String BANATE_ACCUWEATHER_URL =
            "https://www.accuweather.com/en/ph/banate/263448/weather-forecast/263448?city=banate";

    // National weather RSS (Slots 3-5 when category = weather)
    private static final int    WEATHER_NATIONAL_TARGET = 3; // 3 national items (slots 3, 4, 5)

    // ─── SMS limits ───────────────────────────────────────────────────────────
    // Hiligaynon is typically 30-40% longer than English.
    // So target English at ~170-200 chars to land Hiligaynon at 280-320.
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
            "https://data.gmanetwork.com/gno/rss/lifestyle/healthandwellness/feed.xml",
            "https://www.philstar.com/rss/lifestyle",
            "https://abcnews.com/abcnews/healthheadlines",
            "https://www.msf.org/rss/Philippines"
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
    private final String                  googleApiKey;  // used for both Translate and Weather APIs
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
        System.out.println("[News]   WEATHER: Slot 1 = Google Weather API today (" + BANATE_LABEL + ")");
        System.out.println("[News]           Slot 2 = Google Weather API tomorrow (" + BANATE_LABEL + " forecast)");
        System.out.println("[News]           Slots 3-5 = national RSS weather news");
        System.out.println("[News]   Google Weather API: " + (googleApiKey != null ? "ENABLED (shares GOOGLE_TRANSLATE_API_KEY)" : "DISABLED – set GOOGLE_TRANSLATE_API_KEY"));
        System.out.println("[News]   OTHER:   Step 1 → RSS | Step 2 → Claude EN | Step 3 → Google Translate HIL");
        System.out.println("[News]   Google APIs: " + (googleApiKey != null ? "ENABLED" : "DISABLED – set GOOGLE_TRANSLATE_API_KEY"));
        System.out.println("[News]   Length enforcer: " + HIL_MIN + "-" + HIL_MAX + " chars, no word cuts");
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

                CategoryGroup group = resolveGroup(category);

                // ══════════════════════════════════════════════════════════════
                //  WEATHER: special split pipeline
                //    Slot 1  → Google Weather API (Banate, Iloilo) — live data
                //    Slots 2-5 → national RSS weather news
                // ══════════════════════════════════════════════════════════════
                if (group == CategoryGroup.WEATHER) {
                    return generateWeatherItems(onProgress, start, confirmed);
                }

                // ══════════════════════════════════════════════════════════════
                //  NON-WEATHER: standard RSS → Claude → Translate pipeline
                // ══════════════════════════════════════════════════════════════
                emit(onProgress, 0.03, "Step 1/3 — Fetching RSS articles...");

                List<RawArticle> articles = fetchRssArticles(group, topic, category);

                System.out.printf("[RSS] Fetched %d articles after dedup%n", articles.size());

                if (cancelled.get()) return cancelled();

                String geoScope = switch (group) {
                    case LOCAL    -> GEO_LOCAL;
                    case NATIONAL -> GEO_NATIONAL;
                    default       -> GEO_LOCAL;
                };

                // ══════════════════════════════════════════════════════════════
                //  STEP 2 — Claude: read articles → write English SMS bodies
                // ══════════════════════════════════════════════════════════════
                emit(onProgress, 0.10, "Step 2/3 — Claude writing English summaries...");

                String prompt = buildPrompt(
                        topic, category, geoScope,
                        LocalDate.now().toString(),
                        articles, false
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
    //  WEATHER PIPELINE — Banate today (Slot 1) + Banate tomorrow (Slot 2) + national RSS (Slots 3-5)
    // =========================================================================

    /**
     * Full weather pipeline:
     *   1. Fetch today's live conditions from Google Weather API for Banate → Slot 1
     *   2. Fetch tomorrow's forecast from Google Weather Forecast API for Banate → Slot 2
     *   3. Fetch national RSS weather news → Slots 3-5 (3 items)
     *   All three fetches run concurrently, then merge with Banate slots pinned first.
     */
    private List<NewsItem> generateWeatherItems(
            BiConsumer<Double, String> onProgress,
            long start,
            AtomicInteger confirmed) throws Exception {

        emit(onProgress, 0.05, "Weather: fetching Banate today/tomorrow + national news...");

        // ── Run all three fetches concurrently ────────────────────────────────
        CompletableFuture<String> todayFuture =
                CompletableFuture.supplyAsync(this::fetchBanateTodayContext);

        CompletableFuture<String> tomorrowFuture =
                CompletableFuture.supplyAsync(this::fetchBanateTomorrowContext);

        CompletableFuture<List<RawArticle>> nationalRssFuture =
                CompletableFuture.supplyAsync(() -> {
                    List<RawArticle> all = new ArrayList<>();
                    for (String feedUrl : RSS_WEATHER) {
                        all.addAll(fetchOneFeed(feedUrl));
                    }
                    List<String> weatherKw = List.of(
                            "weather","bagyo","typhoon","storm","flood","rainfall",
                            "pagasa","drought","heat","signal","tropical","rain"
                    );
                    List<RawArticle> filtered = all.stream()
                            .filter(a -> matches(a, weatherKw))
                            .collect(Collectors.toList());
                    return dedup(filtered.isEmpty() ? all : filtered);
                });

        String todayContext    = todayFuture.get(15, TimeUnit.SECONDS);
        String tomorrowContext = tomorrowFuture.get(15, TimeUnit.SECONDS);
        List<RawArticle> nationalArticles = nationalRssFuture.get(15, TimeUnit.SECONDS);

        System.out.printf("[Weather] Today context: %d chars | Tomorrow context: %d chars | National articles: %d%n",
                todayContext.length(), tomorrowContext.length(), nationalArticles.size());

        if (cancelled.get()) return cancelled();
        emit(onProgress, 0.18, "Weather: Claude writing Banate today/tomorrow SMS + national summaries...");

        // ── Build combined prompt: today (slot 1), tomorrow (slot 2), national (slots 3-5) ──
        String prompt = buildWeatherPrompt(todayContext, tomorrowContext, nationalArticles);

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
                    double pct = Math.min(0.78, 0.18 + (double) nowCount / TARGET * 0.60);
                    emit(onProgress, pct, nowCount + " of " + TARGET + " items found (writing weather...)");
                }
            }
        }

        if (cancelled.get()) return cancelled();

        String claudeRaw = buffer.toString().replaceAll("[ \t]+", " ").trim();
        System.out.println("\n══ CLAUDE RAW (WEATHER EN) ═══════════════════════════");
        System.out.println(claudeRaw.isEmpty() ? "(empty)" : claudeRaw);
        System.out.println("══════════════════════════════════════════════════════\n");

        emit(onProgress, 0.80, "Parsing weather output...");
        List<ParsedItem> parsed = parseClaudeOutput(claudeRaw, nationalArticles);
        if (parsed.isEmpty()) {
            emit(onProgress, 1.0, "No weather items parsed.");
            return List.of();
        }

        List<ParsedItem> lengthAdjusted = enforceEnglishLength(parsed);

        emit(onProgress, 0.85, "Step 3/3 — Translating weather to Hiligaynon...");
        List<NewsItem> candidates;
        if (googleApiKey != null) {
            candidates = translateToHiligaynon(lengthAdjusted, "weather news");
        } else {
            candidates = lengthAdjusted.stream()
                    .map(p -> {
                        String h = enforceHiligaynonLength(p.englishBody(), p.englishBody(), p.headline(), "weather news");
                        return h != null ? new NewsItem(h, p.url()) : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // ── Pin Banate slots (indices 0 and 1 from Claude) ────────────────────
        // Claude is instructed: item 1 = today, item 2 = tomorrow.
        // selectBestWeather pins both with the AccuWeather URL.
        List<NewsItem> result = selectBestWeather(candidates);

        emit(onProgress, 1.0,
                "Done — " + result.size() + " of " + TARGET + " weather items in " + elapsed(start) + "s");
        return result;
    }

    /**
     * Fetches today's live current conditions from Google Weather API for Banate, Iloilo.
     * Endpoint: GET https://weather.googleapis.com/v1/currentConditions:lookup
     * Falls back to a generic advisory on error or missing key.
     */
    private String fetchBanateTodayContext() {
        if (googleApiKey == null) {
            System.out.println("[Weather] GOOGLE_TRANSLATE_API_KEY not set — using generic Banate today context.");
            return buildGenericBanateContext("TODAY");
        }

        try {
            String url = GOOGLE_WEATHER_URL
                    + "?key="               + URLEncoder.encode(googleApiKey,               StandardCharsets.UTF_8)
                    + "&location.latitude="  + URLEncoder.encode(String.valueOf(BANATE_LAT), StandardCharsets.UTF_8)
                    + "&location.longitude=" + URLEncoder.encode(String.valueOf(BANATE_LNG), StandardCharsets.UTF_8)
                    + "&unitsSystem=METRIC";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET().build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                System.err.printf("[Weather] Today API HTTP %d: %s%n", resp.statusCode(), resp.body());
                return buildGenericBanateContext("TODAY");
            }

            return parseCurrentConditionsResponse(resp.body(), "TODAY");

        } catch (Exception e) {
            System.err.println("[Weather] Today API error: " + e.getMessage());
            return buildGenericBanateContext("TODAY");
        }
    }

    /**
     * Fetches tomorrow's forecast from Google Weather Forecast API for Banate, Iloilo.
     *
     * Endpoint: GET https://weather.googleapis.com/v1/forecast/days:lookup
     * Params:   key, location.latitude, location.longitude, unitsSystem=METRIC, days=2
     * Response: { forecastDays: [ {day 0 = today}, {day 1 = tomorrow}, ... ] }
     * We take forecastDays[1] for tomorrow's data.
     *
     * Key forecast fields (inside forecastDays[1]):
     *   interval.startTime               — ISO-8601 start of the forecast day
     *   daytimeForecast.weatherCondition.description.text — daytime condition text
     *   maxTemperature.degrees           — high temperature °C
     *   minTemperature.degrees           — low temperature °C
     *   daytimeForecast.wind.speed.value — daytime wind speed km/h
     *   daytimeForecast.wind.direction.cardinal — daytime wind direction
     *   daytimeForecast.precipitation.probability.percent — % chance of rain
     *   daytimeForecast.precipitation.qpf.quantity — expected rainfall mm
     *   maxUvIndex                       — max UV index for the day
     *
     * Docs: https://developers.google.com/maps/documentation/weather/forecast
     */
    private String fetchBanateTomorrowContext() {
        if (googleApiKey == null) {
            System.out.println("[Weather] GOOGLE_TRANSLATE_API_KEY not set — using generic Banate tomorrow context.");
            return buildGenericBanateContext("TOMORROW");
        }

        try {
            // Request 2 days so index [0]=today, index [1]=tomorrow
            String url = GOOGLE_WEATHER_FORECAST_URL
                    + "?key="               + URLEncoder.encode(googleApiKey,               StandardCharsets.UTF_8)
                    + "&location.latitude="  + URLEncoder.encode(String.valueOf(BANATE_LAT), StandardCharsets.UTF_8)
                    + "&location.longitude=" + URLEncoder.encode(String.valueOf(BANATE_LNG), StandardCharsets.UTF_8)
                    + "&unitsSystem=METRIC"
                    + "&days=2";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET().build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                System.err.printf("[Weather] Tomorrow forecast API HTTP %d: %s%n", resp.statusCode(), resp.body());
                return buildGenericBanateContext("TOMORROW");
            }

            return parseForecastResponse(resp.body());

        } catch (Exception e) {
            System.err.println("[Weather] Tomorrow forecast API error: " + e.getMessage());
            return buildGenericBanateContext("TOMORROW");
        }
    }

    /**
     * Parses the Google Weather API currentConditions:lookup JSON response for today.
     *
     * Key JSON fields (all at root level — no "current" wrapper):
     *   currentTime                        — ISO-8601 timestamp (UTC)
     *   weatherCondition.description.text  — condition text (e.g. "Partly cloudy")
     *   temperature.degrees                — temperature °C
     *   feelsLikeTemperature.degrees       — apparent temperature °C
     *   relativeHumidity                   — % humidity (integer at root)
     *   wind.speed.value                   — wind speed km/h
     *   wind.direction.cardinal            — direction enum (e.g. "NORTH_NORTHEAST")
     *   precipitation.qpf.quantity         — current precipitation mm
     *   precipitation.probability.percent  — % chance of rain
     *   uvIndex                            — UV index (integer at root)
     */
    private String parseCurrentConditionsResponse(String responseBody, String label) {
        try {
            JsonNode root = json.readTree(responseBody);
            if (root.isMissingNode() || root.isEmpty()) {
                System.err.println("[Weather] Empty response from Google Weather API");
                return buildGenericBanateContext(label);
            }

            String condition   = root.path("weatherCondition").path("description").path("text").asText("Unknown");
            double tempDeg     = root.path("temperature").path("degrees").asDouble(0);
            double feelsLike   = root.path("feelsLikeTemperature").path("degrees").asDouble(0);
            String humidity    = root.path("relativeHumidity").asInt(0) + "%";
            double windVal     = root.path("wind").path("speed").path("value").asDouble(0);
            String windDir     = formatCardinal(root.path("wind").path("direction").path("cardinal").asText("unknown"));
            double precipMm    = root.path("precipitation").path("qpf").path("quantity").asDouble(0);
            int    rainPct     = root.path("precipitation").path("probability").path("percent").asInt(0);
            String rainfall    = precipMm > 0
                    ? String.format("%.1f mm (%d%% chance)", precipMm, rainPct)
                    : String.format("none (%d%% chance)", rainPct);
            String uvIndex     = String.format("%.0f", root.path("uvIndex").asDouble(0));

            String currentTime = root.path("currentTime").asText("today");
            if (currentTime.contains("T") && currentTime.length() > 19)
                currentTime = currentTime.substring(0, 19) + "Z";

            String ctx = String.format(
                    "LIVE WEATHER DATA (%s) — %s (as of %s UTC):\n" +
                            "  Condition : %s\n" +
                            "  Temp      : %.1f°C (feels like %.1f°C)\n" +
                            "  Humidity  : %s\n" +
                            "  Wind      : %.0f km/h from %s\n" +
                            "  Rainfall  : %s\n" +
                            "  UV Index  : %s\n" +
                            "  Source    : Google Weather API (real-time)",
                    label, BANATE_LABEL, currentTime,
                    condition, tempDeg, feelsLike, humidity,
                    windVal, windDir, rainfall, uvIndex
            );

            System.out.printf("[Weather] %s: %s | %.1f°C | Humidity %s | Wind %.0f km/h %s | Rain %s%n",
                    label, condition, tempDeg, humidity, windVal, windDir, rainfall);
            return ctx;

        } catch (Exception e) {
            System.err.println("[Weather] Current conditions parse error: " + e.getMessage());
            return buildGenericBanateContext(label);
        }
    }

    /**
     * Parses Google Weather forecast/days:lookup response and extracts tomorrow (index [1]).
     *
     * Response structure:
     *   { "forecastDays": [
     *       { /* today, index 0 *\/ },
     *       { /* tomorrow, index 1 *\/
     *         "interval": { "startTime": "...", "endTime": "..." },
     *         "maxTemperature": { "degrees": 34.0 },
     *         "minTemperature": { "degrees": 26.0 },
     *         "maxUvIndex": 10,
     *         "daytimeForecast": {
     *           "weatherCondition": { "description": { "text": "Partly sunny" } },
     *           "wind": { "speed": { "value": 18.0 }, "direction": { "cardinal": "SOUTHWEST" } },
     *           "precipitation": {
     *             "probability": { "percent": 60 },
     *             "qpf": { "quantity": 5.2 }
     *           }
     *         }
     *       }
     *   ]}
     */
    private String parseForecastResponse(String responseBody) {
        try {
            JsonNode root      = json.readTree(responseBody);
            JsonNode days      = root.path("forecastDays");

            // Index [1] = tomorrow; [0] = today
            if (!days.isArray() || days.size() < 2) {
                System.err.println("[Weather] Forecast response missing tomorrow data (need forecastDays[1])");
                return buildGenericBanateContext("TOMORROW");
            }

            JsonNode tomorrow  = days.get(1);
            JsonNode daytime   = tomorrow.path("daytimeForecast");

            String condition   = daytime.path("weatherCondition").path("description").path("text").asText("Unknown");
            double maxTemp     = tomorrow.path("maxTemperature").path("degrees").asDouble(0);
            double minTemp     = tomorrow.path("minTemperature").path("degrees").asDouble(0);
            double windVal     = daytime.path("wind").path("speed").path("value").asDouble(0);
            String windDir     = formatCardinal(daytime.path("wind").path("direction").path("cardinal").asText("unknown"));
            int    rainPct     = daytime.path("precipitation").path("probability").path("percent").asInt(0);
            double precipMm    = daytime.path("precipitation").path("qpf").path("quantity").asDouble(0);
            String rainfall    = precipMm > 0
                    ? String.format("%.1f mm (%d%% chance)", precipMm, rainPct)
                    : String.format("none (%d%% chance)", rainPct);
            int    uvIndex     = tomorrow.path("maxUvIndex").asInt(0);

            // Extract the date from the interval startTime (just the date part)
            String startTime   = tomorrow.path("interval").path("startTime").asText("");
            String dateLabel   = startTime.length() >= 10 ? startTime.substring(0, 10) : "tomorrow";

            String ctx = String.format(
                    "FORECAST DATA (TOMORROW %s) — %s:\n" +
                            "  Condition : %s\n" +
                            "  Temp      : High %.1f°C / Low %.1f°C\n" +
                            "  Wind      : %.0f km/h from %s\n" +
                            "  Rainfall  : %s\n" +
                            "  UV Index  : %d (max)\n" +
                            "  Source    : Google Weather API (day-ahead forecast)",
                    dateLabel, BANATE_LABEL,
                    condition, maxTemp, minTemp,
                    windVal, windDir, rainfall, uvIndex
            );

            System.out.printf("[Weather] TOMORROW: %s | High %.1f°C / Low %.1f°C | Wind %.0f km/h %s | Rain %s%n",
                    condition, maxTemp, minTemp, windVal, windDir, rainfall);
            return ctx;

        } catch (Exception e) {
            System.err.println("[Weather] Forecast parse error: " + e.getMessage());
            return buildGenericBanateContext("TOMORROW");
        }
    }

    /**
     * Converts Google Weather API cardinal direction strings to compact abbreviations.
     * Google returns values like "NORTH_NORTHEAST" — this converts them to "NNE".
     * Falls back to the raw string if the format is unrecognised.
     *
     * Google cardinal direction values:
     *   NORTH, NORTH_NORTHEAST, NORTHEAST, EAST_NORTHEAST,
     *   EAST, EAST_SOUTHEAST, SOUTHEAST, SOUTH_SOUTHEAST,
     *   SOUTH, SOUTH_SOUTHWEST, SOUTHWEST, WEST_SOUTHWEST,
     *   WEST, WEST_NORTHWEST, NORTHWEST, NORTH_NORTHWEST
     */
    private String formatCardinal(String cardinal) {
        if (cardinal == null || cardinal.isBlank()) return "unknown";
        return switch (cardinal.toUpperCase(Locale.ROOT)) {
            case "NORTH"           -> "N";
            case "NORTH_NORTHEAST" -> "NNE";
            case "NORTHEAST"       -> "NE";
            case "EAST_NORTHEAST"  -> "ENE";
            case "EAST"            -> "E";
            case "EAST_SOUTHEAST"  -> "ESE";
            case "SOUTHEAST"       -> "SE";
            case "SOUTH_SOUTHEAST" -> "SSE";
            case "SOUTH"           -> "S";
            case "SOUTH_SOUTHWEST" -> "SSW";
            case "SOUTHWEST"       -> "SW";
            case "WEST_SOUTHWEST"  -> "WSW";
            case "WEST"            -> "W";
            case "WEST_NORTHWEST"  -> "WNW";
            case "NORTHWEST"       -> "NW";
            case "NORTH_NORTHWEST" -> "NNW";
            default                -> cardinal; // return raw if unknown
        };
    }

    /**
     * Fallback context when Google Weather API is unavailable.
     * @param label "TODAY" or "TOMORROW"
     */
    private String buildGenericBanateContext(String label) {
        if ("TOMORROW".equals(label))
            return "FORECAST DATA (TOMORROW) — " + BANATE_LABEL + " (general advisory, no forecast data available):\n" +
                    "  Condition : Typical tropical weather expected for Western Visayas\n" +
                    "  Advisory  : Residents should monitor PAGASA for tomorrow's official forecast.";
        return "WEATHER ADVISORY (TODAY) — " + BANATE_LABEL + " (general advisory, no live data available):\n" +
                "  Condition : Typical tropical weather for Western Visayas\n" +
                "  Advisory  : Residents should monitor PAGASA advisories and be prepared for sudden rainfall.";
    }

    /**
     * Builds the weather-specific Claude prompt:
     *   - Item 1 MUST be the Banate live weather item (from live API data)
     *   - Items 2-5 MUST be from national weather RSS articles
     */
    /**
     * Builds a dynamic weather prompt with the new 5-slot layout:
     *   Slot 1 — Banate TODAY  (live current conditions from Google Weather API)
     *   Slot 2 — Banate TOMORROW (day-ahead forecast from Google Weather Forecast API)
     *   Slots 3-5 — National Philippine weather news (from RSS)
     *
     * Tone and urgency adjust dynamically to what the live data actually shows.
     */
    private String buildWeatherPrompt(
            String todayContext, String tomorrowContext, List<RawArticle> nationalArticles) {

        StringBuilder p = new StringBuilder();

        // ── Derive tone cues from both data blocks ────────────────────────────
        String allCtx       = (todayContext + " " + tomorrowContext).toLowerCase(Locale.ROOT);
        boolean hasRain     = allCtx.contains("mm") || allCtx.contains("rain") || allCtx.contains("shower");
        boolean hasTyphoon  = allCtx.contains("typhoon") || allCtx.contains("storm") || allCtx.contains("bagyo");
        boolean hasHeat     = allCtx.contains("uv") || allCtx.contains("heat") || allCtx.contains("sunny");
        boolean hasFlood    = allCtx.contains("flood") || allCtx.contains("baha");

        String urgencyTone;
        if (hasTyphoon || hasFlood) urgencyTone = "urgent typhoon/flood emergency alert";
        else if (hasRain)           urgencyTone = "rain advisory bulletin";
        else if (hasHeat)           urgencyTone = "heat and UV safety advisory";
        else                        urgencyTone = "community weather bulletin";

        // ── Persona ───────────────────────────────────────────────────────────
        p.append("You are an expert SMS weather writer producing a ").append(urgencyTone)
                .append(" for RespondPH, a Philippine disaster-response and public-safety service.\n");
        p.append("Your audience: residents of ").append(BANATE_LABEL)
                .append(" and the wider Philippines. Write clearly so anyone can act on it immediately.\n\n");

        // ── Language rule ─────────────────────────────────────────────────────
        p.append("LANGUAGE RULE: Write every SMS body in CLEAR, SIMPLE ENGLISH only.\n");
        p.append("Do NOT use Hiligaynon, Tagalog, or any Filipino dialect.\n");
        p.append("Google Translate converts the English to Hiligaynon as the final step.\n\n");

        // ── Length rule ───────────────────────────────────────────────────────
        p.append("SMS LENGTH RULE — NON-NEGOTIABLE:\n");
        p.append("  Target: ").append(EN_MIN).append("–").append(EN_MAX)
                .append(" characters per English SMS body (Hiligaynon expands ~35%, landing at 280–320).\n");
        p.append("  Too short (< ").append(EN_MIN).append(" chars): add a concrete detail")
                .append(" — temperature, wind speed, rainfall mm, % rain chance, or a safety action.\n");
        p.append("  Too long  (> ").append(EN_MAX).append(" chars): cut the weakest detail.\n");
        p.append("  Always end with a COMPLETE sentence. Never cut mid-word or mid-phrase.\n\n");

        // ══════════════════════════════════════════════════════
        // SLOT 1 — Banate TODAY (live current conditions)
        // ══════════════════════════════════════════════════════
        p.append("══════════════════════════════════════════════════════\n");
        p.append("SLOT 1 — ").append(BANATE_LABEL.toUpperCase(Locale.ROOT))
                .append(" TODAY — CURRENT CONDITIONS (MANDATORY)\n");
        p.append("  Use ONLY the live data block below. Never invent or assume values.\n");
        p.append("  Always mention 'Banate, Iloilo' by name.\n");
        p.append("  Include at least 2 measured values (temperature, humidity, wind, rainfall, UV).\n");

        // Condition-specific guidance for slot 1
        if (hasTyphoon || hasFlood)
            p.append("  ⚠ URGENT: open with a clear danger warning. End with a specific safety action (evacuate, avoid low-lying areas).\n");
        else if (hasRain)
            p.append("  Rain present: mention rainfall amount or chance. End with a practical tip (carry umbrella, avoid flooded roads).\n");
        else if (hasHeat)
            p.append("  High UV/heat: remind residents to stay hydrated, limit outdoor exposure. End with a sun-safety tip.\n");
        else
            p.append("  End with a brief weather-appropriate tip for residents.\n");

        p.append("\n").append(todayContext).append("\n");
        p.append("══════════════════════════════════════════════════════\n\n");

        // ══════════════════════════════════════════════════════
        // SLOT 2 — Banate TOMORROW (day-ahead forecast)
        // ══════════════════════════════════════════════════════
        p.append("══════════════════════════════════════════════════════\n");
        p.append("SLOT 2 — ").append(BANATE_LABEL.toUpperCase(Locale.ROOT))
                .append(" TOMORROW — DAY-AHEAD FORECAST (MANDATORY)\n");
        p.append("  Use ONLY the forecast data block below. Never invent or assume values.\n");
        p.append("  Always mention 'Banate, Iloilo' and that this is the TOMORROW forecast.\n");
        p.append("  Include the high/low temperature range and at least one of: wind, rainfall, UV.\n");
        p.append("  End with a forward-looking tip — what residents should prepare for tomorrow.\n");
        p.append("\n").append(tomorrowContext).append("\n");
        p.append("══════════════════════════════════════════════════════\n\n");

        // ══════════════════════════════════════════════════════
        // SLOTS 3-5 — National weather news from RSS
        // ══════════════════════════════════════════════════════
        int limit = Math.min(nationalArticles.size(), MAX_ARTICLES_IN_PROMPT);
        p.append("══════════════════════════════════════════════════════\n");
        p.append("SLOTS 3–5 — NATIONAL PHILIPPINE WEATHER NEWS (from RSS articles below)\n");
        p.append("  Draw ONLY from the articles provided. One article per slot, no repeats.\n");
        p.append("  ✅ Allowed: typhoons, tropical storms, floods, PAGASA advisories, storm signals,\n");
        p.append("     heavy rainfall, drought, heat index, monsoon (habagat/amihan).\n");
        p.append("  ❌ Forbidden: politics, elections, crime, health outbreaks, sports, entertainment.\n");
        p.append("     Skip any article on these topics entirely.\n");
        p.append("══════════════════════════════════════════════════════\n\n");

        if (limit > 0) {
            p.append("── NATIONAL WEATHER ARTICLES (").append(limit)
                    .append(" available for slots 3–5) ──\n\n");
            for (int i = 0; i < limit; i++) {
                RawArticle a = nationalArticles.get(i);
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
            p.append("── END ARTICLES ──\n\n");
        } else {
            p.append("No national RSS articles are available right now.\n");
            p.append("For slots 3–5, write general Philippines weather awareness content:\n");
            p.append("  PAGASA monitoring reminders, typhoon preparedness, flood/heat safety.\n");
            p.append("  Do NOT fabricate storm names, dates, signal numbers, or casualty figures.\n\n");
        }

        // ── Output format ─────────────────────────────────────────────────────
        p.append("Produce EXACTLY ").append(TARGET).append(" SMS items — no more, no less.\n");
        p.append("No URLs, hashtags, ellipsis (...), or markdown inside the SMS body.\n\n");

        p.append("LINE FORMAT (one item per line, no blank lines between items):\n");
        p.append("{N}. {English SMS body} | Headline: {short descriptive label} | ArticleIndex: {see rules below}\n\n");

        p.append("FINAL CHECKLIST:\n");
        p.append("  1. Slot 1 (item 1): ").append(BANATE_LABEL)
                .append(" TODAY — uses live data only. Headline: 'Banate Iloilo Weather Today'. ArticleIndex: 0\n");
        p.append("  2. Slot 2 (item 2): ").append(BANATE_LABEL)
                .append(" TOMORROW — uses forecast data only. Headline: 'Banate Iloilo Weather Tomorrow'. ArticleIndex: 0\n");
        p.append("  3. Slots 3–5 (items 3, 4, 5): each from a DIFFERENT national weather article.\n");
        p.append("     ArticleIndex = 1-based article number from the list above.\n");
        p.append("  4. Every SMS body is ").append(EN_MIN).append("–").append(EN_MAX)
                .append(" characters (count precisely before writing).\n");
        p.append("  5. Output the numbered list ONLY — no preamble, no closing remarks.\n");

        return p.toString();
    }

    /**
     * Selects best weather items preserving Banate slots 1 and 2.
     * The first two items (Banate today + tomorrow) are always pinned with the AccuWeather URL.
     * Remaining items (national, slots 3-5) are sorted by length closeness to midpoint.
     */
    private List<NewsItem> selectBestWeather(List<NewsItem> candidates) {
        if (candidates.isEmpty()) return List.of();

        int mid = (HIL_MIN + HIL_MAX) / 2; // 300

        // Guard: need at least 2 candidates (today + tomorrow)
        if (candidates.size() < 2) {
            System.out.println("[WeatherSelect] Too few candidates to pin both Banate slots.");
            return candidates.stream()
                    .map(it -> new NewsItem(it.smsText(), BANATE_ACCUWEATHER_URL))
                    .collect(Collectors.toList());
        }

        // Slots 1 & 2 = Banate today + tomorrow — always pinned, always AccuWeather URL
        NewsItem banateToday    = new NewsItem(candidates.get(0).smsText(), BANATE_ACCUWEATHER_URL);
        NewsItem banateTomorrow = new NewsItem(candidates.get(1).smsText(), BANATE_ACCUWEATHER_URL);
        List<NewsItem> national = candidates.subList(2, candidates.size());

        // Sort national by in-range first, then closest to mid
        List<NewsItem> sortedNational = national.stream()
                .filter(it -> it.smsText().length() >= HIL_MIN && it.smsText().length() <= HIL_MAX)
                .sorted(Comparator.comparingInt(it -> Math.abs(it.smsText().length() - mid)))
                .collect(Collectors.toList());
        national.stream()
                .filter(it -> it.smsText().length() < HIL_MIN || it.smsText().length() > HIL_MAX)
                .sorted(Comparator.comparingInt(it -> Math.abs(it.smsText().length() - mid)))
                .forEach(sortedNational::add);

        // Dedup national by URL
        LinkedHashMap<String, NewsItem> byUrl = new LinkedHashMap<>();
        for (NewsItem it : sortedNational) {
            String key = it.url() != null ? it.url().trim() : "";
            if (!key.isEmpty() && !byUrl.containsKey(key)) byUrl.put(key, it);
        }
        List<NewsItem> nationalSelected = new ArrayList<>(byUrl.values());
        if (nationalSelected.size() > WEATHER_NATIONAL_TARGET)
            nationalSelected = nationalSelected.subList(0, WEATHER_NATIONAL_TARGET);

        // Assemble: Banate today (1), Banate tomorrow (2), national (3-5)
        List<NewsItem> result = new ArrayList<>();
        result.add(banateToday);
        result.add(banateTomorrow);
        result.addAll(nationalSelected);

        System.out.printf("[WeatherSelect] Banate today(1) + tomorrow(2) + %d national = %d total%n",
                nationalSelected.size(), result.size());
        String[] slotLabels = {"[BANATE TODAY]", "[BANATE TOMORROW]"};
        for (int i = 0; i < result.size(); i++) {
            int len = result.get(i).smsText().length();
            String label = i < slotLabels.length ? slotLabels[i] : "[NATIONAL]";
            System.out.printf("[WeatherSelect] Slot %d: %d chars %s %s%n",
                    i + 1, len,
                    (len >= HIL_MIN && len <= HIL_MAX) ? "\u2713" : "\u26A0 OUT OF RANGE",
                    label);
        }
        return result;
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
        for (String w : topicLc.split("[\\s/,]+"))
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
     * Returns per-category topic guardrails injected into the main prompt.
     * Includes both DO / DON'T rules and positive writing guidance tailored to
     * the category's specific audience needs (urgency, actionability, tone).
     */
    private String buildNegativeTopicRules(String category) {
        if (category == null) return "";
        String lc = category.toLowerCase(Locale.ROOT).trim();
        return switch (lc) {
            case "weather news", "weather", "weather update" ->
                    "  \u274C DO NOT write about: politics, elections, crime, health outbreaks, sports, entertainment.\n" +
                            "  \u2705 ONLY write about: typhoons, floods, rainfall, temperature, PAGASA advisories,\n" +
                            "     storm signals, drought, heat index, UV, monsoon conditions, weather forecasts.\n" +
                            "  \uD83D\uDCDD Writing style: include measured values (temp, wind speed, rainfall mm) where available.\n" +
                            "     End each item with a clear practical action (bring umbrella, evacuate, stay hydrated).";
            case "health news" ->
                    "  \u274C DO NOT write about: politics, elections, crime, weather, sports, entertainment,\n" +
                            "     traffic accidents, infrastructure projects.\n" +
                            "  \u2705 ONLY write about: disease outbreaks, vaccines, DOH advisories, hospitals,\n" +
                            "     medical treatment, dengue, COVID, nutrition, mental health, public health alerts.\n" +
                            "  \uD83D\uDCDD Writing style: name the specific disease or condition. State who is affected.\n" +
                            "     Include a clear prevention tip or action the public should take.";
            case "politics news" ->
                    "  \u274C DO NOT write about: weather, crime incidents, health outbreaks, sports,\n" +
                            "     entertainment, traffic accidents, infrastructure.\n" +
                            "  \u2705 ONLY write about: elections, legislation, government policy, COMELEC rulings,\n" +
                            "     senators, congressmen, local officials, executive orders, political campaigns.\n" +
                            "  \uD83D\uDCDD Writing style: neutral, factual, no opinion. Name the official, bill, or policy.\n" +
                            "     State what decision was made and what it means for the public.";
            case "crime / law / public safety news" ->
                    "  \u274C DO NOT write about: weather, health outbreaks, politics, sports, entertainment,\n" +
                            "     infrastructure, natural disasters (unless they cause a public safety incident).\n" +
                            "  \u2705 ONLY write about: crimes, arrests, court verdicts, police/NBI/PNP operations,\n" +
                            "     drug busts, murders, robberies, trafficking, scams, fraud, public safety warnings.\n" +
                            "  \uD83D\uDCDD Writing style: include location (city/province) and suspect or victim details\n" +
                            "     if available. End with a public safety tip or reminder where appropriate.";
            case "national news" ->
                    "  \u274C DO NOT write about: purely local barangay-level news, sports scores,\n" +
                            "     entertainment gossip, overseas celebrity news.\n" +
                            "  \u2705 ONLY write about: national-level Philippine events, government policy,\n" +
                            "     economic developments, disasters, or news that affects the whole country.\n" +
                            "  \uD83D\uDCDD Writing style: state how ordinary Filipinos are affected. Use direct, clear language.\n" +
                            "     Avoid jargon. Prioritise news that requires public awareness or action.";
            default -> // LOCAL — Iloilo/Panay
                    "  \u274C DO NOT write about: national politics, international events, sports, entertainment,\n" +
                            "     or events outside Iloilo City / Iloilo Province.\n" +
                            "  \u2705 ONLY write about: incidents, announcements, or events directly in Iloilo City\n" +
                            "     or the municipalities of Iloilo Province.\n" +
                            "  \uD83D\uDCDD Writing style: name the specific barangay, municipality, or city district.\n" +
                            "     Make it immediately useful to a resident of the Iloilo area.";
        };
    }

    /**
     * Builds the main (non-weather) Claude prompt dynamically.
     * Persona, urgency tone, and writing guidance all adapt to the category and geoScope
     * so the model receives contextually appropriate framing rather than boilerplate.
     */
    private String buildPrompt(
            String topic, String category, String geoScope, String today,
            List<RawArticle> articles, boolean weatherFallback) {

        StringBuilder p = new StringBuilder();

        // ── Dynamic persona + service context ─────────────────────────────────
        String catLc      = category != null ? category.toLowerCase(Locale.ROOT) : "";
        String personaCtx = buildPersonaContext(catLc, geoScope);
        p.append(personaCtx).append("\n\n");

        // ── Language rule ──────────────────────────────────────────────────────
        p.append("LANGUAGE RULE: Write every SMS body in CLEAR, SIMPLE ENGLISH only.\n");
        p.append("Do NOT use Hiligaynon, Tagalog, or any Filipino dialect.\n");
        p.append("Google Translate will handle English → Hiligaynon as the last step.\n\n");

        // ── Topic strictness — fully dynamic ──────────────────────────────────
        p.append("══════════════════════════════════════════════════════\n");
        p.append("TOPIC FILTER — THE MOST CRITICAL RULE IN THIS PROMPT\n");
        p.append("  Category : ").append(category).append("\n");
        p.append("  Coverage : ").append(geoScope).append("\n");
        p.append("  Date     : ").append(today).append("\n\n");
        p.append("  ✅ WRITE: items that are DIRECTLY and CLEARLY about '").append(category).append("'\n");
        p.append("  ❌ SKIP : any article that is off-topic, vague, or only tangentially related.\n");
        p.append("  ❌ SKIP : any article outside the coverage area: ").append(geoScope).append("\n");
        p.append("  ❌ NEVER mix categories. If unsure, skip the article.\n\n");
        p.append(buildNegativeTopicRules(category)).append("\n\n");
        if (weatherFallback)
            p.append("  NOTE: No Iloilo-specific weather found — expand coverage to all of the Philippines.\n\n");
        p.append("  SELF-CHECK before each item:\n");
        p.append("    → Is this DIRECTLY about '").append(category).append("'?  → YES: include.  → NO/UNSURE: skip.\n");
        p.append("══════════════════════════════════════════════════════\n\n");

        // ── SMS length rule — dynamic explanation with category-specific tip ──
        p.append("SMS LENGTH RULE — NON-NEGOTIABLE:\n");
        p.append("  English target: ").append(EN_MIN).append("–").append(EN_MAX).append(" characters per SMS body.\n");
        p.append("  (Hiligaynon translation expands ~35%, reaching the required 280–320 chars.)\n");
        p.append("  Too short (< ").append(EN_MIN).append("): add ").append(buildShortTip(catLc)).append("\n");
        p.append("  Too long  (> ").append(EN_MAX).append("): remove the least essential detail.\n");
        p.append("  End every SMS with a COMPLETE sentence. Never cut mid-word or mid-phrase.\n\n");

        // ── Articles ───────────────────────────────────────────────────────────
        if (!articles.isEmpty()) {
            int limit = Math.min(articles.size(), MAX_ARTICLES_IN_PROMPT);
            p.append("── SOURCE ARTICLES (").append(limit).append(" of ").append(articles.size())
                    .append(" available) — ONLY use articles matching '").append(category).append("' ──\n\n");
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
            p.append("── END ARTICLES ──\n\n");
            p.append("Article rules:\n");
            p.append("  • Use ONLY articles about '").append(category).append("'. Skip off-topic ones entirely.\n");
            p.append("  • Coverage area: ").append(geoScope).append(" — skip articles from outside this area.\n");
            if (!weatherFallback)
                p.append("  • PRIORITY: articles mentioning Banate or Iloilo City go first.\n");
            p.append("  • Do NOT invent, exaggerate, or add facts not in the article.\n");
            p.append("  • Each SMS must come from a DIFFERENT article.\n\n");
        } else {
            // No articles — advisory-only mode
            p.append("No current RSS articles are available for this topic.\n");
            p.append("You MUST still produce ").append(TARGET).append(" SMS items about: '").append(category).append("'\n");
            p.append("Write general public-awareness content for: ").append(geoScope).append("\n\n");
            p.append("Advisory-only rules (no articles):\n");
            p.append("  • Do NOT invent specific incident names, dates, casualty numbers, or locations.\n");
            p.append("  • Write only established general facts, standing advisories, or prevention tips.\n");
            p.append("  • Stay strictly on topic: '").append(category).append("'. Zero exceptions.\n\n");
        }

        // ── Output format ──────────────────────────────────────────────────────
        p.append("Produce EXACTLY ").append(TARGET).append(" SMS news items — no more, no less.\n");
        p.append("Each SMS body: 2–3 complete sentences, ").append(EN_MIN).append("–").append(EN_MAX)
                .append(" characters.\n");
        p.append("No URLs, hashtags, ellipsis (...), or markdown inside the SMS body.\n\n");

        p.append("LINE FORMAT — one item per line, no blank lines between items:\n");
        p.append("{N}. {English SMS body} | Headline: {exact article title} | ArticleIndex: {1-based number}\n\n");

        p.append("FINAL CHECKLIST:\n");
        p.append("  1. Every item is about '").append(category).append("' — no exceptions.\n");
        p.append("  2. Each item comes from a DIFFERENT article.\n");
        p.append("  3. Headline matches the article Title exactly.\n");
        p.append("  4. ArticleIndex is the correct 1-based number.\n");
        p.append("  5. SMS body is ").append(EN_MIN).append("–").append(EN_MAX)
                .append(" characters (count before writing).\n");
        p.append("  6. Output the numbered list ONLY — no preamble, no closing text.\n");

        return p.toString();
    }

    /**
     * Returns a dynamic persona/service-context paragraph that varies by category and geo scope.
     * Makes Claude's framing match the nature of the content it's about to write.
     */
    private String buildPersonaContext(String catLc, String geoScope) {
        boolean isLocal = geoScope != null && geoScope.toLowerCase(Locale.ROOT).contains("iloilo");
        String area = isLocal ? "Iloilo, Western Visayas" : "the Philippines";

        if (catLc.contains("health"))
            return "You are a public health communications specialist writing SMS alerts for RespondPH,\n"
                    + "a Philippine disaster-response and community-safety service serving " + area + ".\n"
                    + "Your goal: give residents clear, actionable health information that helps them\n"
                    + "protect themselves and their families — in plain language anyone can understand.";
        if (catLc.contains("politics"))
            return "You are a civic affairs journalist writing concise, neutral SMS news updates\n"
                    + "for RespondPH, a community-safety platform serving " + area + ".\n"
                    + "Your goal: report political developments factually and impartially so residents\n"
                    + "understand decisions that affect their daily lives — no opinion, no bias.";
        if (catLc.contains("crime") || catLc.contains("law") || catLc.contains("safety"))
            return "You are a public safety communications writer producing SMS crime and safety alerts\n"
                    + "for RespondPH, serving " + area + ".\n"
                    + "Your goal: report incidents clearly, name locations precisely, and end each message\n"
                    + "with a safety reminder or action the public can take to protect themselves.";
        if (catLc.contains("national"))
            return "You are a national news editor writing SMS news digests for RespondPH,\n"
                    + "a public-safety information service covering " + area + ".\n"
                    + "Your goal: translate complex national events into plain-language summaries\n"
                    + "that tell ordinary Filipinos what happened and why it matters to them.";
        // Default: local Iloilo news
        return "You are a local community news writer producing SMS news alerts for RespondPH,\n"
                + "a disaster-response and public-safety service for Iloilo Province, Western Visayas.\n"
                + "Your goal: give Iloilo residents clear, specific, locally relevant updates\n"
                + "— name the barangay or municipality, state what happened, and say what residents should do.";
    }

    /**
     * Returns a category-appropriate hint for what to add when an SMS body is too short.
     * Used in the dynamic length rule section of the prompt.
     */
    private String buildShortTip(String catLc) {
        if (catLc.contains("weather"))
            return "a specific value: temperature, wind speed, rainfall amount, UV index, or a safety action.";
        if (catLc.contains("health"))
            return "the affected population, a prevention tip, or the responsible agency (DOH, LGU, hospital).";
        if (catLc.contains("politics"))
            return "the official's name or position, the bill/policy name, or the implementation timeline.";
        if (catLc.contains("crime") || catLc.contains("law") || catLc.contains("safety"))
            return "the specific location (barangay/city), the suspect's status, or a public safety reminder.";
        if (catLc.contains("national"))
            return "what the development means for ordinary Filipinos, or which agency / sector is affected.";
        return "the specific barangay or municipality, who is affected, or a recommended action.";
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

        System.out.printf("[EnLen] Final (after retries): %d chars (out of target, passing to HIL enforcer)%n", body.length());
        return new ParsedItem(body, item.headline(), item.url());
    }

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
     * Never truncates or cuts Hiligaynon text. Returns null if the item cannot be brought
     * into range without cutting.
     */
    private String enforceHiligaynonLength(String hilText, String englishText, String headline, String category) {
        String cleaned = cleanHilText(hilText);
        int len = cleaned.length();

        System.out.printf("[HilLen] Initial: %d chars (target %d-%d)%n", len, HIL_MIN, HIL_MAX);

        if (len >= HIL_MIN && len <= HIL_MAX) {
            System.out.printf("[HilLen] In range: %d chars ✓%n", len);
            return cleaned;
        }

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
                        if (candidate.length() >= HIL_MIN) {
                            System.out.printf("[HilLen] OK after shorten: %d chars ✓%n", candidate.length());
                            return candidate;
                        }
                        String padded = padHiligaynon(candidate, headline);
                        if (padded.length() >= HIL_MIN && padded.length() <= HIL_MAX) {
                            System.out.printf("[HilLen] OK after shorten+pad: %d chars ✓%n", padded.length());
                            return padded;
                        }
                        cleaned = candidate;
                    } else {
                        cleaned = candidate;
                    }
                }
            }

            System.out.printf("[HilLen] DROPPED — could not get under %d chars without cutting (%d chars after %d retries)%n",
                    HIL_MAX, cleaned.length(), MAX_RETRIES);
            return null;
        }

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
                    if (candidate.length() > cleaned.length() && candidate.length() <= HIL_MAX) {
                        cleaned = candidate;
                    }
                }
            }

            String padded = padHiligaynon(cleaned, headline);
            if (padded.length() >= HIL_MIN && padded.length() <= HIL_MAX) {
                System.out.printf("[HilLen] OK after pad: %d chars ✓%n", padded.length());
                return padded;
            }

            System.out.printf("[HilLen] DROPPED — could not reach %d-%d chars cleanly (%d chars)%n",
                    HIL_MIN, HIL_MAX, padded.length());
            return null;
        }

        return cleaned;
    }

    private String padHiligaynon(String text, String headline) {
        if (text == null) return "";
        text = text.trim();

        if (text.length() > HIL_MAX) return text;

        String base = stripTrailingPunctuation(text);

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
            String bestFit = null;
            for (String pad : pads) {
                String candidate = result + pad;
                if (candidate.length() <= HIL_MAX) {
                    if (bestFit == null || pad.length() > bestFit.length()) {
                        bestFit = pad;
                    }
                }
            }
            if (bestFit != null) {
                result = result + bestFit;
                madeProgress = true;
            }
        }

        if (!result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?"))
            result += ".";

        return result;
    }

    private String stripTrailingPunctuation(String text) {
        if (text == null || text.isBlank()) return "";
        text = text.trim();
        if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?"))
            return text.substring(0, text.length() - 1);
        return text;
    }

    private String cleanHilText(String s) {
        if (s == null) return "";
        s = unescapeHtml(s);
        s = s.replaceAll("\\s+", " ").trim();
        if (!s.isEmpty() && !s.endsWith(".") && !s.endsWith("!") && !s.endsWith("?"))
            s += ".";
        return s;
    }

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

        int mid = (HIL_MIN + HIL_MAX) / 2;

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