package services;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Email service backed by the Resend API (https://resend.com).
 * Replaces the previous Gmail OAuth implementation.
 */
public class GmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final String FROM_ADDRESS = "FitSense <onboarding@resend.dev>";

    private static GmailService instance;
    private final HttpClient httpClient;
    private final String apiKey;

    private GmailService() {
        this.httpClient = HttpClient.newHttpClient();
        this.apiKey = loadApiKey();
    }

    private static String loadApiKey() {
        try (java.io.InputStream in = GmailService.class.getResourceAsStream("/config.properties")) {
            if (in == null) return "";
            java.util.Properties p = new java.util.Properties();
            p.load(in);
            return p.getProperty("resend.api.key", "");
        } catch (Exception e) { return ""; }
    }

    public static synchronized GmailService getInstance() {
        if (instance == null) {
            instance = new GmailService();
        }
        return instance;
    }

    public CompletableFuture<Void> sendEmail(String to, String subject, String bodyText) {
        JsonObject payload = new JsonObject();
        payload.addProperty("from", FROM_ADDRESS);
        payload.addProperty("to", to);
        payload.addProperty("subject", subject);
        payload.addProperty("text", bodyText);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND_API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new RuntimeException(
                                "Resend API error " + response.statusCode() + ": " + response.body());
                    }
                    System.out.println("Email sent successfully to: " + to);
                });
    }
}
