//package com.ionres.respondph.sendsms;
//
//import com.google.gson.Gson;
//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;
//import okhttp3.*;
//
//import java.io.IOException;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.TimeUnit;
//
//public class GeminiNewsService {
//    private static final String API_KEY = "AIzaSyB4Krmu8MnzkLKcKcAlW5i56cHJQigKoOs";
//    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
//
//    private final OkHttpClient client;
//    private final Gson gson;
//
//    public GeminiNewsService() {
//        this.client = new OkHttpClient.Builder()
//                .connectTimeout(60, TimeUnit.SECONDS)
//                .writeTimeout(60, TimeUnit.SECONDS)
//                .readTimeout(60, TimeUnit.SECONDS)
//                .build();
//        this.gson = new Gson();
//    }
//
//    public CompletableFuture<List<String>> generateNewsHeadlines(String category, int count) {
//        CompletableFuture<List<String>> future = new CompletableFuture<>();
//
//        String prompt = buildPrompt(category, count);
//
//        JsonObject requestBody = new JsonObject();
//        JsonArray contentsArray = new JsonArray();
//        JsonObject content = new JsonObject();
//        JsonArray partsArray = new JsonArray();
//        JsonObject parts = new JsonObject();
//
//        parts.addProperty("text", prompt);
//        partsArray.add(parts);
//        content.add("parts", partsArray);
//        contentsArray.add(content);
//        requestBody.add("contents", contentsArray);
//
//        RequestBody body = RequestBody.create(
//                gson.toJson(requestBody),
//                MediaType.parse("application/json; charset=utf-8")
//        );
//
//        Request request = new Request.Builder()
//                .url(API_URL)
//                .post(body)
//                .addHeader("Content-Type", "application/json")
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                future.completeExceptionally(e);
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                String responseBody = response.body().string();
//
//                if (!response.isSuccessful()) {
//                    future.completeExceptionally(
//                            new IOException("API Error " + response.code() + ": " + responseBody)
//                    );
//                    return;
//                }
//
//                try {
//                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
//
//                    String text = jsonResponse
//                            .getAsJsonArray("candidates")
//                            .get(0).getAsJsonObject()
//                            .getAsJsonObject("content")
//                            .getAsJsonArray("parts")
//                            .get(0).getAsJsonObject()
//                            .get("text").getAsString();
//
//                    List<String> newsList = parseNewsResponse(text);
//                    future.complete(newsList);
//
//                } catch (Exception e) {
//                    future.completeExceptionally(new Exception("Error parsing response: " + e.getMessage()));
//                }
//            }
//        });
//
//        return future;
//    }
//
////    private String buildPrompt(String category, int count) {
////        LocalDate currentDate = LocalDate.now();
////        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
////        String today = currentDate.format(formatter);
////
////        String location = "Western Visayas/Iloilo, Philippines";
////
////        return String.format(
////                "Generate %d VERIFIED news headlines about '%s' in Hiligaynon language with credible links. " +
////                        "Date: %s | Location: %s " +
////
////                        "REQUIREMENTS: " +
////                        "✓ Real events from past 7 days only " +
////                        "✓ Use official Philippine sources with actual working URLs " +
////                        "✓ Credible sources: " +
////                        "  - Gov: pagasa.dost.gov.ph, ndrrmc.gov.ph, doh.gov.ph, pna.gov.ph " +
////                        "  - News: gmanetwork.com, abs-cbn.com, inquirer.net, rappler.com, mb.com.ph " +
////                        "✓ NO fake news or fabricated links " +
////                        "✓ Hiligaynon language with urgency (subong/karon/bag-o) " +
////
////                        "FORMAT (Headline in one line, link in next line): " +
////                        "1. [Hiligaynon headline - max 120 characters] " +
////                        "https://[actual-source-url] " +
////                        "2. [Hiligaynon headline - max 120 characters] " +
////                        "https://[actual-source-url] " +
////
////                        "Example: " +
////                        "1. Bagyo Egay padulong sa Panay, Signal #2 gipatakda sa Iloilo " +
////                        "https://www.gmanetwork.com/news/topstories/weather/... " +
////
////                        "Generate %d headlines with working links. Keep headlines under 120 chars to allow room for links. " +
////                        "Provide ONLY numbered headlines with links on separate lines.",
////                count, category, today, location, count
////        );
////    }
//
//    private String buildPrompt(String category, int count) {
//        LocalDate currentDate = LocalDate.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
//        String today = currentDate.format(formatter);
//
//        String location = "Western Visayas/Iloilo, Philippines";
//
//        return String.format(
//                "Generate %d BREAKING and LATEST news headlines about '%s' in Hiligaynon language. " +
//                        "Today's date: %s. Focus on CURRENT events happening NOW in %s. " +
//                        "Make the news URGENT and TIMELY - as if these events are happening TODAY or in the PAST FEW DAYS. " +
//                        "Each headline must be EXACTLY 300 characters or less (including spaces). " +
//                        "Include recent developments, today's updates, or ongoing situations. " +
//                        "Make them realistic and suitable for emergency SMS alerts to Filipino citizens. " +
//                        "Use time indicators: 'subong nga adlaw' (today), 'karon' (now), 'bag-o lang' (just recently). " +
//                        "IMPORTANT: Base the news on REAL, VERIFIED sources only. Include credible reference links from official sources like: " +
//                        "- Philippine government websites (gov.ph, pna.gov.ph, pia.gov.ph) " +
//                        "- Major Philippine news outlets (ABS-CBN, GMA, Inquirer, Rappler, Manila Bulletin) " +
//                        "- International news agencies (Reuters, AP, BBC) " +
//                        "- Official social media of government agencies (NDRRMC, PAGASA, DOH, PNP) " +
//                        "DO NOT create fake or misleading news. Ensure all information is FACTUAL and VERIFIABLE. " +
//                        "Format as numbered list (1. 2. 3. etc.) with each headline on a new line. " +
//                        "After each headline, add the source link in parentheses like: (Source: [URL]) " +
//                        "Do not add explanations, only numbered headlines with source links.",
//                count, category, today, location
//        );
//    }
//
//    private List<String> parseNewsResponse(String response) {
//        List<String> newsList = new ArrayList<>();
//
//        String[] lines = response.split("\n");
//
//        for (String line : lines) {
//            line = line.trim();
//
//            line = line.replaceFirst("^\\d+\\.\\s*", "");
//            line = line.replaceFirst("^\\*\\*\\d+\\.\\*\\*\\s*", ""); // For bold numbers
//            line = line.replaceAll("\\*\\*", ""); // Remove markdown bold
//            line = line.trim();
//
//            if (!line.isEmpty() && line.length() <= 200) {
//                if (line.length() > 160) {
//                    line = line.substring(0, 157) + "...";
//                }
//                newsList.add(line);
//            }
//        }
//
//        return newsList;
//    }
//
////    private List<String> parseNewsResponse(String response) {
////        List<String> combinedNews = new ArrayList<>();
////        String[] lines = response.split("\n");
////
////        StringBuilder currentEntry = new StringBuilder();
////
////        for (String line : lines) {
////            line = line.trim();
////            if (line.isEmpty()) continue;
////
////            // Remove numbering (e.g., "1. ")
////            String cleanLine = line.replaceFirst("^\\d+\\.\\s*", "").replaceAll("\\*", "");
////
////            if (cleanLine.startsWith("http")) {
////                // This is a link, attach it to the previous headline
////                if (currentEntry.length() > 0) {
////                    currentEntry.append("\n").append(cleanLine);
////                    combinedNews.add(currentEntry.toString());
////                    currentEntry.setLength(0); // Reset for next pair
////                }
////            } else {
////                // This is a headline
////                currentEntry.append(cleanLine);
////            }
////        }
////        return combinedNews;
////    }
//}

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
            "gemini-3-pro-preview",
            "gemini-3-flash-preview"
//            "gemini-flash-lite-latest",
//            "gemini-2.0-flash-lite",
//            "gemini-2.0-flash",
//            "gemini-flash-latest",
//            "gemma-3-4b-it",

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

        return String.format(
                "Generate %d urgent Hiligaynon news headlines about '%s'. " +
                        "Date: %s. Location: %s. " +
                        "%s" +
                        "Events must be current (today or past few days). " +
                        "Max 160 characters per headline, including the link. " +
                        "For emergency SMS alerts. " +
                        "Use time words: 'subong nga adlaw', 'karon', 'bag-o lang'. " +
                        "Format: numbered list only. No explanations.",
                count, category, today, location, sourceInstruction
        );
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

