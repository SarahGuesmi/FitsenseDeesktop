package utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Calls OpenAI API to analyze sentiment and extract keywords from a feedback comment.
 */
public class SentimentAnalyzer {

    private static final String OPENAI_API_KEY = loadKey("groq.api.key");
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private static String loadKey(String property) {
        try (java.io.InputStream is = SentimentAnalyzer.class.getResourceAsStream("/config.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                return props.getProperty(property, "");
            }
        } catch (Exception ignored) {}
        return "";
    }

    public record AnalysisResult(String sentiment, String keywords) {}

    /**
     * Analyzes a comment and returns sentiment + keywords.
     * sentiment: "positive", "negative", or "neutral"
     * keywords: comma-separated list e.g. "intense, tiring, good pace"
     */
    public static AnalysisResult analyze(String comment) {
        if (comment == null || comment.isBlank()) return new AnalysisResult("neutral", "");

        String prompt = "Analyze this workout feedback comment. Reply in this exact format:\n" +
                "SENTIMENT: <positive|negative|neutral>\n" +
                "KEYWORDS: <comma separated keywords, max 4>\n\n" +
                "Comment: " + comment;

        String body = "{\"model\":\"" + MODEL + "\"," +
                "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}]," +
                "\"max_tokens\":60,\"temperature\":0}";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + OPENAI_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseResult(extractContent(response.body()), comment);
            } else {
                System.err.println("OpenAI error " + response.statusCode() + ": " + response.body());
                return fallback(comment);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("OpenAI request failed: " + e.getMessage());
            return fallback(comment);
        }
    }

    private static AnalysisResult parseResult(String content, String comment) {
        if (content == null || content.isBlank()) return fallback(comment);
        String sentiment = "neutral";
        String keywords = "";
        for (String line : content.split("\n")) {
            if (line.toUpperCase().startsWith("SENTIMENT:")) {
                String val = line.substring(10).trim().toLowerCase();
                if (val.contains("positive")) sentiment = "positive";
                else if (val.contains("negative")) sentiment = "negative";
                else sentiment = "neutral";
            } else if (line.toUpperCase().startsWith("KEYWORDS:")) {
                keywords = line.substring(9).trim();
            }
        }
        return new AnalysisResult(sentiment, keywords);
    }

    private static String extractContent(String json) {
        int idx = json.indexOf("\"content\":");
        if (idx == -1) return null;
        int start = json.indexOf("\"", idx + 10) + 1;
        int end = json.indexOf("\"", start);
        if (start <= 0 || end <= start) return null;
        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
    }

    private static AnalysisResult fallback(String comment) {
        String lower = comment.toLowerCase();
        String sentiment;
        // Check negative first (includes negations like "don't like", "not good")
        if (lower.matches(".*(don.t like|not good|not great|bad|poor|terrible|hate|awful|worst|boring|difficult|no |nul|mauvais|horrible|pas bien|trop dur|i dont|don't|cannot|can't).*"))
            sentiment = "negative";
        else if (lower.matches(".*(great|good|excellent|love|amazing|like|enjoy|best|super|bien|j.aime|parfait|top|cool|fantastic|awesome).*"))
            sentiment = "positive";
        else
            sentiment = "neutral";

        // Simple keyword extraction: take first 3 meaningful words
        String[] words = lower.replaceAll("[^a-zA-ZÀ-ÿ\\s]", "").split("\\s+");
        List<String> stopWords = List.of("i", "the", "a", "an", "is", "it", "this", "was", "my", "me", "and", "or", "but", "not");
        StringBuilder kw = new StringBuilder();
        int count = 0;
        for (String w : words) {
            if (!stopWords.contains(w) && w.length() > 2 && count < 3) {
                if (kw.length() > 0) kw.append(", ");
                kw.append(w);
                count++;
            }
        }
        return new AnalysisResult(sentiment, kw.toString());
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
