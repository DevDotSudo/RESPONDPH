package com.ionres.respondph.common.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class NewsGeneratorService {

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta";
    private static final String PRIMARY_MODEL = "gemini-3-pro-preview";
    private static final String[] FALLBACK_MODELS = {
            "gemini-pro-latest",
            "gemini-2.5-flash"
    };
    
    private static final String FALLBACK_MESSAGE =
            "Wala ako sang bag-o kag napamatud-an nga impormasyon para sini nga topiko.";

    private final HttpClient http;

    public NewsGeneratorService() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(60))
                .build();
    }

    public CompletableFuture<List<String>> generateNewsHeadlines(String category, int count) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            future.completeExceptionally(new IllegalStateException("Missing GEMINI_API_KEY environment variable."));
            return future;
        }
        tryGenerateWithModel(category, count, apiKey, 0, future);
        return future;
    }

    public NewsResult generateLatestNews(String topic) throws Exception {
        List<String> options = generateNewsHeadlines(topic, 5).get();
        return new NewsResult(options, List.of(), String.join("\n", options));
    }

    private void tryGenerateWithModel(String category,
                                      int count,
                                      String apiKey,
                                      int modelIndex,
                                      CompletableFuture<List<String>> future) {
        String currentModel = getModelForIndex(modelIndex);
        if (currentModel == null) {
            future.completeExceptionally(new IllegalStateException(
                    "All models exhausted quota. Please try again later or upgrade your plan."));
            return;
        }

        String endpoint = BASE + "/models/" + currentModel + ":generateContent";
        String prompt = buildPrompt(category, count);
        String jsonBody = buildJsonBody(prompt);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .whenComplete((resp, err) -> {
                    if (err != null) {
                        tryGenerateWithModel(category, count, apiKey, modelIndex + 1, future);
                        return;
                    }

                    if (resp.statusCode() == 429 || resp.statusCode() / 100 != 2) {
                        tryGenerateWithModel(category, count, apiKey, modelIndex + 1, future);
                        return;
                    }

                    try {
                        String text = extractFirstText(resp.body());
                        if (text == null || text.isBlank()) {
                            throw new IllegalStateException("Empty response text");
                        }
                        List<String> parsed = parseNewsResponse(text, count);
                        future.complete(parsed);
                    } catch (Exception ex) {
                        tryGenerateWithModel(category, count, apiKey, modelIndex + 1, future);
                    }
                });
    }

    private String getModelForIndex(int index) {
        if (index == 0) return PRIMARY_MODEL;
        int fallbackIndex = index - 1;
        if (fallbackIndex < FALLBACK_MODELS.length) {
            return FALLBACK_MODELS[fallbackIndex];
        }
        return null;
    }

    private String buildJsonBody(String prompt) {
        return "{" +
                "\"contents\":[{" +
                "  \"parts\":[{\"text\":" + toJsonString(prompt) + "}]" +
                "}]," +
                "\"generationConfig\":{" +
                "  \"temperature\":0.0," +
                "  \"topP\":1," +
                "  \"maxOutputTokens\":1024" +
                "}," +
                "\"tools\":[{\"googleSearch\":{}}]" +
                "}";
    }

    private String buildPrompt(String category, int count) {
        LocalDate currentDate = LocalDate.now(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
        String today = currentDate.format(formatter);

        String location;
        String normalized = Optional.ofNullable(category).orElse("").toLowerCase(Locale.ENGLISH);
        switch (normalized) {
            case "weather news":
                location = "Western Visayas, Philippines";
                break;
            case "national news":
                location = "Philippines";
                break;
            case "local news":
            case "panay news":
                location = "Iloilo City and selected Panay Island areas";
                break;
            case "politics news":
            case "health news":
            case "crime / law / public safety news":
                location = "Iloilo City, Western Visayas";
                break;
            default:
                location = "Iloilo City, Western Visayas";
                break;
        }

        return String.format(
                "Generate exactly %d urgent news headlines about '%s'. " +
                        "Date: %s. Location: %s. " +
                        "Use Google Search to find reliable, recent sources. Only include an item if a credible source link is found; omit any item without a solid source. " +
                        "Include one FULL source link per item (link placed after the headline). " +
                        "Events must be current (today or past few days). " +
                        "Each headline must be AT LEAST 160 characters EXCLUDING the link. Do not shorten below 160. " +
                        "Output ONLY in Hiligaynon (no English). " +
                        "If the news is from the current day, include the phrase 'subong nga adlaw'. " +
                        "Use time words: 'subong nga adlaw', 'karon', 'bag-o lang'. " +
                        "Format: numbered list only. Example: 1) <headline min 160 chars> <link>",
                count, category, today, location
        );
    }

    private List<String> parseNewsResponse(String response, int count) {
        List<String> newsList = new ArrayList<>();
        String[] lines = response.split("\n");

        for (String line : lines) {
            String cleaned = line.trim();
            cleaned = cleaned.replaceFirst("^\\d+\\.\\s*", "");
            cleaned = cleaned.replaceFirst("^\\d+\\)\\s*", "");
            cleaned = cleaned.replaceFirst("^\\*\\*\\d+\\.\\*\\*\\s*", "");
            cleaned = cleaned.replaceAll("\\*\\*", "");

            // Extract first URL (if any)
            String link = null;
            java.util.regex.Matcher linkMatcher = java.util.regex.Pattern.compile("(https?://\\S+)").matcher(cleaned);
            if (linkMatcher.find()) {
                link = linkMatcher.group(1);
            }

            // Text portion without URL
            String textOnly = cleaned.replaceAll("https?://\\S+", "").trim();
            if (textOnly.isEmpty()) {
                continue;
            }

            // Enforce minimum 160 chars for headline text (excluding link)
            if (textOnly.length() < 160) {
                continue;
            }

            if (link != null) {
                newsList.add(textOnly + " " + link);
            } else {
                newsList.add(textOnly);
            }
        }

        if (newsList.isEmpty()) {
            newsList.add(FALLBACK_MESSAGE);
        }

        if (newsList.size() > count) {
            return newsList.subList(0, count);
        }
        return newsList;
    }

    private static String extractFirstText(String json) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\\"text\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            String raw = m.group(1);
            return raw.replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\t", "\t")
                    .replace("\\r", "\r");
        }
        return null;
    }

    private static String toJsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
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

        public List<String> getOptions() {
            return options;
        }

        public List<String> getSources() {
            return sources;
        }

        public String getRawText() {
            return rawText;
        }
    }
}
