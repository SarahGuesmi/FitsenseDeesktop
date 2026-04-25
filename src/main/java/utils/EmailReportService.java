package utils;

import models.FeedbackResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;

public class EmailReportService {

    // Replace with your SendGrid API key
    private static final String SENDGRID_API_KEY = loadKey("sendgrid.api.key");

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
    private static final String FROM_EMAIL = "nourammarr9@gmail.com";
    private static final String TO_EMAIL = "nourammarr9@gmail.com";
    private static final String API_URL = "https://api.sendgrid.com/v3/mail/send";

    public static void sendDailyReport(List<FeedbackResponse> responses) throws IOException, InterruptedException {
        String html = buildHtml(responses);

        String body = "{"
                + "\"personalizations\":[{\"to\":[{\"email\":\"" + TO_EMAIL + "\"}]}],"
                + "\"from\":{\"email\":\"" + FROM_EMAIL + "\"},"
                + "\"subject\":\"📊 Daily Feedback Report - FitSense\","
                + "\"content\":[{\"type\":\"text/html\",\"value\":\"" + escapeJson(html) + "\"}]"
                + "}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SENDGRID_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("SendGrid error " + response.statusCode() + ": " + response.body());
        }
    }

    private static String buildHtml(List<FeedbackResponse> responses) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm").withZone(ZoneId.systemDefault());

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:Arial,sans-serif;background:#0f1117;color:#eff4ff;padding:24px'>");
        sb.append("<h1 style='color:#a78bfa'>📊 Daily Feedback Report</h1>");
        sb.append("<p style='color:#8b92b8'>Total responses: <strong style='color:#eff4ff'>").append(responses.size()).append("</strong></p>");
        sb.append("<hr style='border-color:#2e3250'/>");

        // Summary stats
        long positive = responses.stream().filter(f -> "positive".equalsIgnoreCase(f.getSentiment())).count();
        long negative = responses.stream().filter(f -> "negative".equalsIgnoreCase(f.getSentiment())).count();
        long neutral = responses.stream().filter(f -> "neutral".equalsIgnoreCase(f.getSentiment())).count();

        sb.append("<div style='display:flex;gap:16px;margin:16px 0'>");
        sb.append("<div style='background:#1a2e1a;padding:12px 20px;border-radius:8px'>✅ Positive: <strong>").append(positive).append("</strong></div>");
        sb.append("<div style='background:#2e1a1a;padding:12px 20px;border-radius:8px'>❌ Negative: <strong>").append(negative).append("</strong></div>");
        sb.append("<div style='background:#1a1a2e;padding:12px 20px;border-radius:8px'>➖ Neutral: <strong>").append(neutral).append("</strong></div>");
        sb.append("</div><hr style='border-color:#2e3250'/>");

        // Individual feedbacks
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
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "").replace("\r", "");
    }
}
