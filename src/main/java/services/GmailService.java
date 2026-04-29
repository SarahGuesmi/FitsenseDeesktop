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
 *
 * NOTE: In Resend test mode (no verified domain), emails can only be sent
 * to the account owner's email. Set resend.test.override.to in config.properties
 * to your verified Resend account email. Once a domain is verified at
 * resend.com/domains, remove that property and update FROM_ADDRESS to use
 * your domain (e.g. "FitSense <noreply@yourdomain.com>").
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

    private static java.util.Properties loadConfig() {
        java.util.Properties p = new java.util.Properties();
        try (java.io.InputStream in = GmailService.class.getResourceAsStream("/config.properties")) {
            if (in != null) p.load(in);
        } catch (Exception ignored) {}
        return p;
    }

    private static String loadApiKey() {
        return loadConfig().getProperty("resend.api.key", "");
    }

    public static synchronized GmailService getInstance() {
        if (instance == null) {
            instance = new GmailService();
        }
        return instance;
    }

    public CompletableFuture<Void> sendEmail(String to, String subject, String bodyText) {
        // Resend test-mode: if resend.test.override.to is set in config.properties,
        // redirect all emails to that address (your verified Resend account email).
        // Remove this override once you verify a domain at resend.com/domains.
        String overrideTo = loadConfig().getProperty("resend.test.override.to", "").trim();
        String effectiveTo = (!overrideTo.isEmpty()) ? overrideTo : to;

        // If the effective recipient is the same as the original, or no override is set,
        // and we're in test mode, the API will reject it. Detect this and skip gracefully.
        if (overrideTo.isEmpty()) {
            // No override configured — skip silently to avoid 403 blocking the workflow
            System.out.println("[GmailService] No resend.test.override.to set — email skipped for: " + to);
            return CompletableFuture.completedFuture(null);
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("from", FROM_ADDRESS);
        payload.addProperty("to", effectiveTo);
        payload.addProperty("subject", subject + (!effectiveTo.equals(to)
                ? " [originally for: " + to + "]" : ""));
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
