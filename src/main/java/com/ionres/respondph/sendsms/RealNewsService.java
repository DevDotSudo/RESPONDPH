package com.ionres.respondph.sendsms;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Improved Real News Service using NewsAPI.org
 * Uses keyword search instead of country filter for better results
 */
public class RealNewsService {

    private static final String NEWSAPI_KEY = "99225bcdafd943fe9ab6b7c132ac6dcd";
    // Use "everything" endpoint instead of "top-headlines" for better Philippines coverage
    private static final String NEWSAPI_BASE_URL = "https://newsapi.org/v2/everything";

    private final OkHttpClient client;
    private final Gson gson;

    public RealNewsService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Fetch real news headlines from NewsAPI using keyword search
     * This works better than country filter for Philippines
     */
    public CompletableFuture<List<NewsArticle>> fetchRealNews(String category, int count) {
        CompletableFuture<List<NewsArticle>> future = new CompletableFuture<>();

        // Build search query for Philippines news
        String searchQuery = buildSearchQuery(category);

        HttpUrl url = HttpUrl.parse(NEWSAPI_BASE_URL).newBuilder()
                .addQueryParameter("q", searchQuery)
                .addQueryParameter("language", "en")
                .addQueryParameter("sortBy", "publishedAt") // Most recent first
                .addQueryParameter("pageSize", String.valueOf(Math.min(count, 20)))
                .addQueryParameter("apiKey", NEWSAPI_KEY)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        System.out.println("Fetching news with query: " + searchQuery);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(new IOException("Failed to fetch news: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    System.err.println("NewsAPI Error Response: " + responseBody);
                    future.completeExceptionally(new IOException("NewsAPI error (Code " + response.code() + "): " + responseBody));
                    return;
                }

                try {
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                    String status = jsonResponse.get("status").getAsString();
                    if (!status.equals("ok")) {
                        String errorMsg = jsonResponse.has("message")
                                ? jsonResponse.get("message").getAsString()
                                : "Unknown error";
                        future.completeExceptionally(new IOException("NewsAPI returned error: " + errorMsg));
                        return;
                    }

                    JsonArray articles = jsonResponse.getAsJsonArray("articles");
                    int totalResults = jsonResponse.get("totalResults").getAsInt();

                    System.out.println("Found " + totalResults + " articles, returning " + articles.size());

                    if (articles.size() == 0) {
                        future.completeExceptionally(new IOException("No news articles found for category: " + category));
                        return;
                    }

                    List<NewsArticle> newsList = parseArticles(articles);
                    System.out.println("✓ Successfully fetched " + newsList.size() + " real news articles");
                    future.complete(newsList);

                } catch (Exception e) {
                    e.printStackTrace();
                    future.completeExceptionally(new IOException("Failed to parse news response: " + e.getMessage()));
                }
            }
        });

        return future;
    }

    /**
     * Build search query based on category
     * Uses Philippines-specific keywords for better results
     */
    private String buildSearchQuery(String category) {
        if (category == null) return "Philippines";

        switch (category.toLowerCase()) {
            case "national":
                return "Philippines government OR Manila OR national";
            case "local news":
                return "Philippines Iloilo OR Visayas OR local";
            case "weather news":
                return "Philippines weather OR typhoon OR PAGASA OR storm";
            case "politics news":
                return "Philippines politics OR government OR election OR senate";
            case "health news":
                return "Philippines health OR DOH OR COVID OR medical";
            case "crime / law / public safety":
                return "Philippines crime OR police OR PNP OR safety OR law";
            default:
                return "Philippines news";
        }
    }

    private List<NewsArticle> parseArticles(JsonArray articles) {
        List<NewsArticle> newsList = new ArrayList<>();

        for (int i = 0; i < articles.size(); i++) {
            try {
                JsonObject article = articles.get(i).getAsJsonObject();

                String title = article.get("title").getAsString();
                String description = article.has("description") && !article.get("description").isJsonNull()
                        ? article.get("description").getAsString()
                        : "";
                String url = article.get("url").getAsString();
                String sourceName = article.getAsJsonObject("source").get("name").getAsString();
                String publishedAt = article.get("publishedAt").getAsString();

                // Skip articles with [Removed] in title (these are deleted articles)
                if (title.contains("[Removed]")) {
                    continue;
                }

                String smsHeadline = createSMSHeadline(title, description, sourceName, url);

                NewsArticle newsArticle = new NewsArticle(
                        title,
                        description,
                        url,
                        sourceName,
                        publishedAt,
                        smsHeadline
                );

                newsList.add(newsArticle);
            } catch (Exception e) {
                System.err.println("Error parsing article " + i + ": " + e.getMessage());
                // Continue with next article
            }
        }

        return newsList;
    }

    private String createSMSHeadline(String title, String description, String sourceName, String url) {
        String headline = title;

        if (title.length() > 120) {
            headline = title.substring(0, 117) + "...";
        }

        String fullText = headline + " (Source: " + sourceName + ")";

        if (fullText.length() > 160) {
            int maxTitleLength = 160 - (" (Source: " + sourceName + ")").length() - 3;
            headline = title.substring(0, Math.max(0, maxTitleLength)) + "...";
            fullText = headline + " (Source: " + sourceName + ")";
        }

        return fullText;
    }

    public static class NewsArticle {
        private final String title;
        private final String description;
        private final String url;
        private final String sourceName;
        private final String publishedAt;
        private final String smsHeadline;

        public NewsArticle(String title, String description, String url,
                           String sourceName, String publishedAt, String smsHeadline) {
            this.title = title;
            this.description = description;
            this.url = url;
            this.sourceName = sourceName;
            this.publishedAt = publishedAt;
            this.smsHeadline = smsHeadline;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getUrl() { return url; }
        public String getSourceName() { return sourceName; }
        public String getPublishedAt() { return publishedAt; }
        public String getSmsHeadline() { return smsHeadline; }

        @Override
        public String toString() {
            return smsHeadline + "\nRead more: " + url;
        }
    }
}