package com.ionres.respondph.common.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsGeneratorService {

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta";

    private static final String[] MODELS = {
            "gemini-3-pro-preview",
            "gemini-pro=latest",
            "gemini-1.5-pro-latest"
    };

    private static final java.time.Duration TIMEOUT = java.time.Duration.ofSeconds(30);
    private static final long MAX_TIME_MS = 90_000; // 90 seconds max
    private static final int MAX_ATTEMPTS = 10;
    private static final int MIN_TEXT_LENGTH = 60;

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler;

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\\)\\]]+");

    public NewsGeneratorService() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(12))
                .build();

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "news-gen");
            t.setDaemon(true);
            return t;
        });
    }

    public CompletableFuture<List<String>> generateNewsHeadlines(String category, int count) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            future.completeExceptionally(new IllegalStateException(
                    "Missing GEMINI_API_KEY environment variable"));
            return future;
        }

        System.out.println(">>> Generating " + count + " " + category + " headlines in Hiligaynon");

        long startTime = System.currentTimeMillis();
        generate(category, count, apiKey, 0, future, startTime);

        return future;
    }

    private void generate(String category,
                          int count,
                          String apiKey,
                          int attempt,
                          CompletableFuture<List<String>> future,
                          long startTime) {

        if (future.isDone()) return;

        // Check timeout
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > MAX_TIME_MS) {
            future.completeExceptionally(new TimeoutException("Timed out after " + (elapsed/1000) + "s"));
            return;
        }

        if (attempt >= MAX_ATTEMPTS) {
            future.completeExceptionally(new RuntimeException(
                    "Failed after " + MAX_ATTEMPTS + " attempts. Check API key and network."));
            return;
        }

        String model = MODELS[attempt % MODELS.length];
        System.out.println("[" + attempt + "] Trying: " + model);

        callGemini(category, count, apiKey, model)
                .whenComplete((items, error) -> {

                    if (future.isDone()) return;

                    if (error != null) {
                        System.err.println("✗ Error: " + error.getMessage());
                        retry(category, count, apiKey, attempt + 1, future, startTime);
                        return;
                    }

                    if (items == null || items.isEmpty()) {
                        System.err.println("✗ No valid items generated");
                        retry(category, count, apiKey, attempt + 1, future, startTime);
                        return;
                    }

                    System.out.println("✓ SUCCESS: Got " + items.size() + " valid headlines");
                    future.complete(items);
                });
    }

    private CompletableFuture<List<String>> callGemini(String category, int count, String apiKey, String model) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        String endpoint = BASE + "/models/" + model + ":generateContent";
        String prompt = buildPrompt(category, count);
        String body = buildBody(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {

                    if (error != null) {
                        future.completeExceptionally(error);
                        return;
                    }

                    try {
                        int code = response.statusCode();

                        if (code == 401 || code == 403) {
                            future.completeExceptionally(new IllegalStateException("Invalid API key"));
                            return;
                        }

                        if (code != 200) {
                            String body_snippet = response.body().substring(0, Math.min(200, response.body().length()));
                            System.err.println("HTTP " + code + ": " + body_snippet);
                            future.completeExceptionally(new RuntimeException("HTTP " + code));
                            return;
                        }

                        String text = parseResponse(response.body());
                        List<String> validated = validateAndFormat(text, count);

                        future.complete(validated);

                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });

        return future;
    }

    private void retry(String category, int count, String apiKey, int nextAttempt,
                       CompletableFuture<List<String>> future, long startTime) {

        if (future.isDone()) return;

        long delay = Math.min(500 + (nextAttempt * 300), 3000);
        System.out.println("Retrying in " + delay + "ms...");

        scheduler.schedule(
                () -> generate(category, count, apiKey, nextAttempt, future, startTime),
                delay,
                TimeUnit.MILLISECONDS
        );
    }

    private List<String> validateAndFormat(String text, int maxCount) {
        List<String> result = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return result;
        }

        String[] lines = text.split("\n");

        for (String line : lines) {
            String cleaned = line.trim();

            cleaned = cleaned.replaceFirst("^\\d+[\\).:\\-]\\s*", "");
            cleaned = cleaned.replaceFirst("^[•\\-*]\\s*", "");

            if (cleaned.isEmpty() || cleaned.length() < 20) continue;

            Matcher matcher = URL_PATTERN.matcher(cleaned);
            if (!matcher.find()) {
                System.out.println("  ⊘ No URL: " + cleaned.substring(0, Math.min(50, cleaned.length())) + "...");
                continue;
            }

            String url = matcher.group();
            String textPart = cleaned.replace(url, "").trim();
            textPart = textPart.replaceAll("\\s+", " ");

            if (textPart.length() < MIN_TEXT_LENGTH) {
                System.out.println("  ⊘ Too short (" + textPart.length() + "): " +
                        textPart.substring(0, Math.min(40, textPart.length())) + "...");
                continue;
            }

            String formatted = textPart + " " + url;
            result.add(formatted);

            System.out.println("  ✓ Valid (" + textPart.length() + " chars)");

            if (result.size() >= maxCount) break;
        }

        return result;
    }

    private String buildBody(String prompt) {
        return "{" +
                "\"contents\":[{\"parts\":[{\"text\":" + escape(prompt) + "}]}]," +
                "\"generationConfig\":{" +
                "\"temperature\":0.8," +
                "\"maxOutputTokens\":1500" +
                "}}";
    }

    private String escape(String s) {
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private String parseResponse(String json) throws Exception {
        JsonNode root = mapper.readTree(json);

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new RuntimeException("No candidates");
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new RuntimeException("No parts");
        }

        String text = parts.get(0).path("text").asText("");
        if (text.isBlank()) {
            throw new RuntimeException("Empty response");
        }

        return text;
    }

    private String buildPrompt(String category, int count) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String date = today.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));

        // SINGLE STEP: Generate directly in Hiligaynon with URLs
        return String.format(
                "Ikaw isa ka news writer para sa Iloilo City, Philippines.\n" +
                        "Petsa: %s\n" +
                        "Kategorya: %s\n\n" +
                        "TRABAHO: Himua %d ka balita nga naka-sulat sa HILIGAYNON (Ilonggo).\n\n" +
                        "MGA KINANGLANON:\n" +
                        "1. Sulat sa HILIGAYNON lang - wala English\n" +
                        "2. Kada balita: 80-120 nga mga pulong\n" +
                        "3. Butangi detalye: petsa, lugar, numero, ngalan\n" +
                        "4. Magamit sini nga mga pulong para sa oras:\n" +
                        "   - Subong/karon (today/now)\n" +
                        "   - Bag-o lang/kamakaagi (recently)\n" +
                        "   - Kahapon (yesterday)\n" +
                        "   - Sini nga semana (this week)\n" +
                        "5. IMPORTANTE: Butangi ISA ka URL sa katapusan sang kada linya\n" +
                        "   Gamita sini: https://rappler.com, https://philstar.com, \n" +
                        "   https://gmanetwork.com, https://pna.gov.ph, https://abs-cbn.com\n" +
                        "6. Para sa Iloilo City o Western Visayas\n" +
                        "7. Tunog urgent kag importante\n\n" +
                        "FORMAT (sundon gid ini):\n" +
                        "1) [Balita sa Hiligaynon] https://example.com/article\n" +
                        "2) [Balita sa Hiligaynon] https://example.com/article2\n" +
                        "3) [Balita sa Hiligaynon] https://example.com/article3\n\n" +
                        "HALIMBAWA:\n" +
                        "1) Subong nga adlaw, ang lokal nga gobyerno sang Iloilo City nagdeklarar sang estado sang emergency tungod sa malakas nga ulan kag baha. Napinsala ang mga dalan kag madamo nga pamilya ang nag-evacuate sa mas luwas nga lugar. Ang mayor nagpahibalo nga nagpadala na sila sang tulong para sa mga naapektuhan. https://rappler.com/iloilo-flood-2026\n\n" +
                        "KARON, himua %d ka balita sa Hiligaynon (gamita ang format sa ibabaw):",
                date, category, count, count
        );
    }

    public static class NewsResult {
        private final List<String> options;
        private final List<String> sources;
        private final String rawText;

        public NewsResult(List<String> options, List<String> sources, String rawText) {
            this.options = options == null ? List.of() : options;
            this.sources = sources == null ? List.of() : sources;
            this.rawText = rawText;
        }

        public List<String> getOptions() { return options; }
        public List<String> getSources() { return sources; }
        public String getRawText() { return rawText; }
    }
}