package com.ionres.respondph.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.time.Duration;

public class SMSApi {
    private final String API_KEY;
    private final String API_URL;
    public SMSApi() {
        this.API_KEY = ConfigLoader.get("skysms.api.key");
        this.API_URL = ConfigLoader.get("skysms.api.url");
    }

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_2)
            .build();

    public boolean sendSMS(String phoneNumber, String message) {
        try {
            String json = String.format(
                    "{ \"phone_number\": \"%s\", \"message\": \"%s\", \"use_subscription\": %b }",
                    phoneNumber,
                    message.replace("\"", "\\\""),
                    true
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("X-API-Key", API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200 || response.statusCode() == 201;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void sendBulkSMS(List<String> phoneNumbers, String message) {
        for (String number : phoneNumbers) {
            boolean success = sendSMS(number, message);
            System.out.println("Sent to " + number + " → " + success);

            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
