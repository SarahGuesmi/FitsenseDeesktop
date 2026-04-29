package utils;

import models.FeedbackResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Sends the daily feedback report via the Resend API.
 *
 * Resend free/test accounts can only deliver to the account owner's email.
 * The report is delivered to RESEND_ACCOUNT_EMAIL and the subject notes
 * the intended recipient (nourammarr23@icloud.com).
 * Once you verify a domain at resend.com/domains, change TO_EMAIL to
 * "nourammarr23@icloud.com" and update FROM_ADDRESS to use your domain.
 */
public class EmailReportService {

    private static final String RESEND_API_URL     = "https://api.resend.com/emails";
    private static final String FROM_ADDRESS       = "FitSense <onboarding@resend.dev>";

    // Resend account owner — the only address Resend allows in test mode.
    // ⚠ Make sure this matches EXACTLY the email you used to sign up at resend.com
    private static final String TO_EMAIL           = "zarroukmouhamedaziz904@gmail.com";

    // Intended recipient — shown in subject for reference
    private static final String INTENDED_RECIPIENT = "nourammarr23@icloud.com";

    private static String loadKey(String property) {
        try (java.io.InputStream is = EmailReportService.class.getResourceAsStream("/config.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                return props.getProperty(property, "");
            }
        } catch (Exception ignored) {}
        return "";
    }

    public static void sendDailyReport(List<FeedbackResponse> responses) throws IOException, InterruptedException {
        String apiKey = loadKey("resend.api.key");
        String html   = buildHtml(responses);

        String jsonBody = "{"
                + "\"from\":\"" + FROM_ADDRESS + "\","
                + "\"to\":[\"" + TO_EMAIL + "\"],"
                + "\"subject\":\"📊 Daily Feedback Report - FitSense (for " + INTENDED_RECIPIENT + ")\","
                + "\"html\":\"" + escapeJson(html) + "\""
                + "}";

        HttpClient  client  = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Resend error " + response.statusCode() + ": " + response.body());
        }
    }

    // ── HTML builder ──────────────────────────────────────────────────────────

    private static String buildHtml(List<FeedbackResponse> responses) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                .withZone(ZoneId.systemDefault());

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:Arial,sans-serif;background:#0f1117;color:#eff4ff;padding:24px'>");
        sb.append("<h1 style='color:#a78bfa'>📊 Daily Feedback Report</h1>");
        sb.append("<p style='color:#8b92b8'>Intended for: <strong style='color:#eff4ff'>")
          .append(INTENDED_RECIPIENT).append("</strong></p>");
        sb.append("<p style='color:#8b92b8'>Total responses: <strong style='color:#eff4ff'>")
          .append(responses.size()).append("</strong></p>");
        sb.append("<hr style='border-color:#2e3250'/>");

        long positive = responses.stream().filter(f -> "positive".equalsIgnoreCase(f.getSentiment())).count();
        long negative = responses.stream().filter(f -> "negative".equalsIgnoreCase(f.getSentiment())).count();
        long neutral  = responses.stream().filter(f -> "neutral".equalsIgnoreCase(f.getSentiment())).count();

        sb.append("<div style='display:flex;gap:16px;margin:16px 0'>");
        sb.append("<div style='background:#1a2e1a;padding:12px 20px;border-radius:8px'>✅ Positive: <strong>").append(positive).append("</strong></div>");
        sb.append("<div style='background:#2e1a1a;padding:12px 20px;border-radius:8px'>❌ Negative: <strong>").append(negative).append("</strong></div>");
        sb.append("<div style='background:#1a1a2e;padding:12px 20px;border-radius:8px'>➖ Neutral: <strong>").append(neutral).append("</strong></div>");
        sb.append("</div><hr style='border-color:#2e3250'/>");

        for (FeedbackResponse f : responses) {
            String sentimentColor = "positive".equalsIgnoreCase(f.getSentiment()) ? "#22c55e"
                    : "negative".equalsIgnoreCase(f.getSentiment()) ? "#ef4444" : "#eab308";

            sb.append("<div style='background:#141928;border-radius:10px;padding:16px;margin:12px 0;border:1px solid #252d4a'>");
            sb.append("<div style='display:flex;justify-content:space-between'>");
            sb.append("<strong>").append(f.getUserName()).append("</strong>");
            sb.append("<span style='background:").append(sentimentColor).append("22;color:").append(sentimentColor)
              .append(";padding:2px 10px;border-radius:6px;font-size:12px'>")
              .append(f.getSentiment() != null ? f.getSentiment() : "N/A").append("</span>");
            sb.append("</div>");

            if (f.getWorkout() != null)
                sb.append("<p style='color:#5ab4ff;margin:4px 0'>↔ ").append(f.getWorkout().getNom()).append("</p>");
            if (f.getRating() != null)
                sb.append("<p style='margin:4px 0'>⭐ Rating: ").append(f.getRating()).append("</p>");
            if (f.getComment() != null && !f.getComment().isBlank())
                sb.append("<p style='color:#c4c9e2;font-style:italic;margin:4px 0'>❝ ").append(f.getComment()).append("</p>");
            if (f.getKeywords() != null && !f.getKeywords().isBlank())
                sb.append("<p style='color:#a78bfa;font-size:12px;margin:4px 0'>🏷 ").append(f.getKeywords()).append("</p>");
            if (f.getCreatedAt() != null)
                sb.append("<p style='color:#6b7280;font-size:11px;margin:4px 0'>🕐 ").append(fmt.format(f.getCreatedAt())).append("</p>");

            sb.append("</div>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "")
                   .replace("\r", "");
    }
}
