package com.ionres.respondph.sendsms;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GeminiNewsService {
    private static final String API_KEY = "";

    private static final String[] FALLBACK_MODELS = {
            "gemini-2.5-pro",
            "gemini-2.5-flash"
    };



    private final OkHttpClient client;
    private final Gson gson;
    private int currentModelIndex = 0;

    public GeminiNewsService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public CompletableFuture<List<String>> generateNewsHeadlines(String category, int count) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        tryGenerateWithModel(category, count, 0, future);
        return future;
    }

    private void tryGenerateWithModel(String category, int count, int modelIndex, CompletableFuture<List<String>> future) {
        if (modelIndex >= FALLBACK_MODELS.length) {
            future.completeExceptionally(new IOException("All models exhausted quota. Please try again later or upgrade your plan."));
            return;
        }

        String currentModel = FALLBACK_MODELS[modelIndex];
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + currentModel + ":generateContent?key=" + API_KEY;

        System.out.println("Trying model: " + currentModel);

        String prompt = buildPrompt(category, count);

        JsonObject requestBody = new JsonObject();
        JsonArray contentsArray = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray partsArray = new JsonArray();
        JsonObject parts = new JsonObject();

        parts.addProperty("text", prompt);
        partsArray.add(parts);
        content.add("parts", partsArray);
        contentsArray.add(content);
        requestBody.add("contents", contentsArray);

        RequestBody body = RequestBody.create(
                gson.toJson(requestBody),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.err.println("Model " + currentModel + " failed: " + e.getMessage());
                tryGenerateWithModel(category, count, modelIndex + 1, future);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                // Check for quota exceeded (429 error)
                if (response.code() == 429) {
                    System.err.println("Model " + currentModel + " quota exceeded. Trying next model...");
                    tryGenerateWithModel(category, count, modelIndex + 1, future);
                    return;
                }

                if (!response.isSuccessful()) {
                    System.err.println("Model " + currentModel + " error: " + responseBody);
                    tryGenerateWithModel(category, count, modelIndex + 1, future);
                    return;
                }

                try {
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                    String text = jsonResponse
                            .getAsJsonArray("candidates")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("content")
                            .getAsJsonArray("parts")
                            .get(0).getAsJsonObject()
                            .get("text").getAsString();

                    System.out.println("✓ Successfully generated news with model: " + currentModel);
                    List<String> newsList = parseNewsResponse(text);
                    future.complete(newsList);

                } catch (Exception e) {
                    System.err.println("Parse error with model " + currentModel + ": " + e.getMessage());
                    tryGenerateWithModel(category, count, modelIndex + 1, future);
                }
            }
        });
    }

    private String buildPrompt(String category, int count) {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        String today = currentDate.format(formatter);

        String location;
        String sourceInstruction;

        switch (category.toLowerCase()) {

            case "weather news":
                location = "Western Visayas, Philippines";
                sourceInstruction =
                        "Base headlines on PAGASA updates and include the source link https://www.pagasa.dost.gov.ph/. ";
                break;

            case "national news":
                location = "Philippines";
                sourceInstruction =
                        "Base headlines on CNN reports and include the source link https://edition.cnn.com/. ";
                break;

            case "local news":
            case "panay news":
                location = "Iloilo City and selected Panay Island areas";
                sourceInstruction =
                        "Base headlines on Panay News Iloilo and include the source link " +
                                "https://www.panaynews.net/category/news/iloilo/. ";
                break;

            case "politics news":
            case "health news":
            case "crime / law / public safety news":
                location = "Iloilo City, Western Visayas";
                sourceInstruction =
                        "Base headlines on ABS-CBN and GMA News and include one source link: " +
                                "https://www.abs-cbn.com/news/ or https://www.gmanetwork.com/news/. ";
                break;

            default:
                location = "Iloilo City, Western Visayas";
                sourceInstruction =
                        "Base headlines on credible Philippine news sources and include a source link. ";
                break;
        }

//        return String.format(
//                "Generate %d urgent Hiligaynon news headlines about '%s'. " +
//                        "Date: %s. Location: %s. " +
//                        "%s" +
//                        "Events must be current (today or past few days). " +
//                        "Max 160 characters per headline, including the link. " +
//                        "For emergency SMS alerts. " +
//                        "Use time words: 'subong nga adlaw', 'karon', 'bag-o lang'. " +
//                        "Format: numbered list only. No explanations.",
//                count, category, today, location, sourceInstruction
//        );

        return  "Generate 5 urgent Hiligaynon news headlines about 'local news'. Date: February 2, 2026. Location: Iloilo City and selected Panay Island areas. Base headlines on Panay News Iloilo and include the source link https://www.panaynews.net/category/news/iloilo/. Events must be current (today or past few days). Max 160 characters per headline, including the link. For emergency SMS alerts. Use time words: 'subong nga adlaw', 'karon', 'bag-o lang'. Format: numbered list only. No explanations.";
    }



    private List<String> parseNewsResponse(String response) {
        List<String> newsList = new ArrayList<>();
        String[] lines = response.split("\n");

        for (String line : lines) {
            line = line.trim();
            line = line.replaceFirst("^\\d+\\.\\s*", "");
            line = line.replaceFirst("^\\*\\*\\d+\\.\\*\\*\\s*", "");
            line = line.replaceAll("\\*\\*", "");
            line = line.trim();

            if (!line.isEmpty() && line.length() <= 500) {
                if (line.length() > 300) {
                    line = line.substring(0, 300) + "...";
                }
                newsList.add(line);
            }
        }

        return newsList;
    }
}

