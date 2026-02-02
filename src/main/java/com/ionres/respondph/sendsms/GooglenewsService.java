package com.ionres.respondph.sendsms;

import okhttp3.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class GooglenewsService {

    private static final String GOOGLE_NEWS_RSS_BASE = "https://news.google.com/rss/search";

    private final OkHttpClient client;

    public GooglenewsService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public CompletableFuture<List<NewsArticle>> fetchRealNews(String category, int count) {
        CompletableFuture<List<NewsArticle>> future = new CompletableFuture<>();

        try {
            String searchQuery = buildSearchQuery(category);
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.toString());

            // Build Google News RSS URL
            String rssUrl = GOOGLE_NEWS_RSS_BASE +
                    "?q=" + encodedQuery +
                    "&hl=en-PH" +  // Philippines English
                    "&gl=PH" +      // Philippines region
                    "&ceid=PH:en";  // Philippines, English

            System.out.println("Fetching from Google News: " + searchQuery);

            Request request = new Request.Builder()
                    .url(rssUrl)
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    future.completeExceptionally(new IOException("Failed to fetch news: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();

                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("Google News error: " + response.code()));
                        return;
                    }

                    try {
                        List<NewsArticle> articles = parseRSSFeed(responseBody, count);

                        if (articles.isEmpty()) {
                            future.completeExceptionally(new IOException("No news articles found"));
                            return;
                        }

                        System.out.println("✓ Successfully fetched " + articles.size() + " articles from Google News");
                        future.complete(articles);

                    } catch (Exception e) {
                        e.printStackTrace();
                        future.completeExceptionally(new IOException("Failed to parse RSS feed: " + e.getMessage()));
                    }
                }
            });

        } catch (Exception e) {
            future.completeExceptionally(new IOException("Failed to build request: " + e.getMessage()));
        }

        return future;
    }

    /**
     * Build search query based on category
     */
    private String buildSearchQuery(String category) {
        if (category == null) return "Philippines news";

        switch (category.toLowerCase()) {
            case "national":
                return "Philippines national news";
            case "local news":
                return "Iloilo Philippines OR Western Visayas news";
            case "weather news":
                return "Philippines weather typhoon PAGASA";
            case "politics news":
                return "Philippines politics government";
            case "health news":
                return "Philippines health DOH medical";
            case "crime / law / public safety":
                return "Philippines crime police security";
            default:
                return "Philippines news";
        }
    }

    /**
     * Parse Google News RSS feed
     */
    private List<NewsArticle> parseRSSFeed(String rssContent, int maxCount) throws Exception {
        List<NewsArticle> articles = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(rssContent.getBytes(StandardCharsets.UTF_8)));

        NodeList itemNodes = doc.getElementsByTagName("item");
        int count = Math.min(itemNodes.getLength(), maxCount);

        for (int i = 0; i < count; i++) {
            try {
                Element item = (Element) itemNodes.item(i);

                String title = getElementText(item, "title");
                String link = getElementText(item, "link");
                String pubDate = getElementText(item, "pubDate");
                String description = getElementText(item, "description");

                // Extract source from title (Google News format: "Title - Source")
                String sourceName = "Google News";
                if (title.contains(" - ")) {
                    int lastDash = title.lastIndexOf(" - ");
                    sourceName = title.substring(lastDash + 3);
                    title = title.substring(0, lastDash);
                }

                String smsHeadline = createSMSHeadline(title, sourceName);

                NewsArticle article = new NewsArticle(
                        title,
                        description,
                        link,
                        sourceName,
                        pubDate,
                        smsHeadline
                );

                articles.add(article);

            } catch (Exception e) {
                System.err.println("Error parsing item " + i + ": " + e.getMessage());
                // Continue with next item
            }
        }

        return articles;
    }

    /**
     * Helper to extract text from XML element
     */
    private String getElementText(Element parent, String tagName) {
        try {
            NodeList nodes = parent.getElementsByTagName(tagName);
            if (nodes.getLength() > 0) {
                return nodes.item(0).getTextContent().trim();
            }
        } catch (Exception e) {
            // Return empty string if element not found
        }
        return "";
    }

    /**
     * Create SMS-friendly headline (160 chars max)
     */
    private String createSMSHeadline(String title, String sourceName) {
        String headline = title;

        if (title.length() > 120) {
            headline = title.substring(0, 117) + "...";
        }

        String fullText = headline + " (Source: " + sourceName + ")";

        if (fullText.length() > 160) {
            int maxTitleLength = 160 - (" (Source: " + sourceName + ")").length() - 3;
            if (maxTitleLength > 0) {
                headline = title.substring(0, Math.min(maxTitleLength, title.length())) + "...";
                fullText = headline + " (Source: " + sourceName + ")";
            }
        }

        return fullText;
    }

    /**
     * News Article model
     */
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