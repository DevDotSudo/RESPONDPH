package com.ionres.respondph.common.services;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.Tool;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsGeneratorService {

    private static final String ENV_API_KEY = "GEMINI_API_KEY";
    private static final String MODEL_ID = "gemini-3-pro-preview";

    private static final int MIN_LEN = 140;
    private static final int MAX_LEN = 160;

    private final Client client;

    public NewsGeneratorService() {
        String apiKey = System.getenv(ENV_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing environment variable " + ENV_API_KEY);
        }
        this.client = Client.builder().apiKey(apiKey.trim()).build();
    }

    public CompletableFuture<List<String>> generateNewsHeadlines(String topic, int count) {

        final int wanted = Math.max(1, Math.min(count, 5));

        return CompletableFuture.supplyAsync(() -> {

            String today = LocalDate.now().toString();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .tools(Tool.builder().googleSearch(GoogleSearch.builder().build()).build())
                    .build();

            String prompt =
                    "Ikaw isa ka generator sang SMS balita.\n" +
                            "Gamita ang Google Search grounding kag magkuha lang sang TINUOD kag naga-ubra nga FULL article links.\n" +
                            "Dapat ang balita halin sa Iloilo City, Philippines.\n" +
                            "Mas ginapaboran ang subong nga adlaw (" + today + ") ukon sulod sang last 3 days.\n" +
                            "Topiko: " + topic + "\n\n" +

                            "MAGBALIK SANG EXACTLY " + wanted + " ITEMS LANG.\n" +
                            "Isa lang ka linya kada item.\n\n" +

                            "FORMAT EXACTLY:\n" +
                            "1. <Hiligaynon SMS text 140-160 characters lang> (Source: FULL ARTICLE URL)\n\n" +

                            "HARD RULES:\n" +
                            "- Hiligaynon lang ang SMS text, likawan ang English words kon indi kinahanglan.\n" +
                            "- Indi pagbutang ang URL sa sulod sang SMS text.\n" +
                            "- Ang Source link dapat FULL article URL (may path), indi homepage.\n" +
                            "- Indi maghatag sang peke nga links.\n" +
                            "- Wala sang extra commentary ukon blank lines.\n";

            GenerateContentResponse response =
                    client.models.generateContent(MODEL_ID, prompt, config);

            String raw = safeText(response);

            System.out.println("\n================ AI RAW RESPONSE ================");
            System.out.println(raw == null ? "(null)" : raw);
            System.out.println("=================================================\n");

            return parseSmsWithSource(raw, wanted);
        });
    }

    private String safeText(GenerateContentResponse response) {
        try {
            if (response == null) return null;
            String t = response.text();
            return (t == null) ? null : t.trim();
        } catch (Exception e) {
            System.out.println("[AI] response.text() failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private List<String> parseSmsWithSource(String raw, int wanted) {

        List<String> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;

        Pattern p = Pattern.compile(
                "^\\s*(\\d+)\\.\\s*(.+?)\\s*\\(\\s*Source\\s*:\\s*(https?://[^\\s)]+)\\s*\\)\\s*$",
                Pattern.CASE_INSENSITIVE
        );

        String[] lines = raw.split("\\r?\\n");

        for (String line : lines) {

            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            Matcher m = p.matcher(trimmed);
            if (!m.matches()) continue;

            String smsText = m.group(2).trim();
            String url = m.group(3).trim();

            if (!isValidFullArticleUrl(url)) continue;

            smsText = stripAnyUrlInsideText(smsText);

            smsText = enforceLen(smsText);

            result.add(smsText + " (Source: " + url + ")");

            if (result.size() >= wanted) break;
        }

        return result;
    }

    private boolean isValidFullArticleUrl(String url) {
        try {
            URI u = URI.create(url);

            String scheme = u.getScheme();
            String host = u.getHost();
            String path = u.getPath();

            if (scheme == null) return false;
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) return false;

            if (host == null || host.isBlank()) return false;

            if (path == null || path.isBlank() || "/".equals(path)) return false;

            if (path.length() < 2) return false;

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private String stripAnyUrlInsideText(String text) {
        if (text == null) return "";
        return text.replaceAll("https?://\\S+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String enforceLen(String text) {

        if (text == null) text = "";
        text = text.replaceAll("\\s+", " ").trim();

        if (text.length() > MAX_LEN) {
            String cut = text.substring(0, MAX_LEN).trim();
            cut = cut.replaceAll("[,;:\\-–—]+$", "").trim();
            return cut;
        }

        if (text.length() < MIN_LEN) {

            String filler =
                    " Padayon nga bantayan ang opisyal nga pahibalo sang LGU para sa seguridad.";

            String combined = (text + " " + filler)
                    .replaceAll("\\s+", " ")
                    .trim();

            if (combined.length() > MAX_LEN) {
                combined = combined.substring(0, MAX_LEN).trim();
                combined = combined.replaceAll("[,;:\\-–—]+$", "").trim();
            }

            while (combined.length() < MIN_LEN) {
                combined = (combined + ".").trim();
                if (combined.length() > MAX_LEN) break;
            }

            return combined;
        }

        return text;
    }
}
