package services;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * GroqService — calls the Groq API (llama3-8b-8192) for text generation.
 * API key is read from config.properties: groq.api.key=YOUR_KEY
 */
public class GroqService {

    private static final String API_URL  = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama3-8b-8192";
    private static final String API_KEY  = loadKey();

    /**
     * Generates 5 sport-themed username suggestions based on first + last name.
     * Returns a list of uppercase handles, e.g. ["IRONMARAI", "SWEATQUEENM", ...]
     */
    public List<String> generateUsernames(String firstname, String lastname) {
        String prompt = "Generate exactly 5 creative, sport-themed usernames for a fitness app user "
                + "named " + firstname + " " + lastname + ". "
                + "Rules: uppercase only, no spaces, no special characters, max 16 chars each, "
                + "incorporate parts of the name, add fitness words like IRON, FIT, SWEAT, GAINS, "
                + "PULSE, BEAST, FLEX, RUSH, FIRE, PEAK, GRIND, FUEL, BOLT, APEX. "
                + "Return ONLY the 5 usernames, one per line, nothing else.";

        String response = chat(prompt);
        return parseLines(response, 5);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String chat(String userMessage) {
        try {
            String body = "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":"
                + "\"" + userMessage.replace("\"", "\\\"").replace("\n", " ") + "\"}],"
                + "\"max_tokens\":200,"
                + "\"temperature\":0.9"
                + "}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());

            return extractContent(resp.body());
        } catch (Exception e) {
            System.err.println("GroqService: " + e.getMessage());
            return "";
        }
    }

    /** Extracts the "content" field from the Groq JSON response */
    private String extractContent(String json) {
        String marker = "\"content\":\"";
        int start = json.indexOf(marker);
        if (start < 0) return "";
        start += marker.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return "";
        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
    }

    /** Parses newline-separated lines, strips numbering/bullets, uppercases */
    private List<String> parseLines(String text, int max) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) return result;
        for (String line : text.split("\n")) {
            String clean = line.trim()
                    .replaceAll("^[0-9]+[.)\\-\\s]+", "") // remove "1. " "1) " etc.
                    .replaceAll("[^A-Za-z0-9]", "")        // keep alphanumeric only
                    .toUpperCase();
            if (!clean.isBlank() && clean.length() <= 20) {
                result.add(clean);
                if (result.size() >= max) break;
            }
        }
        return result;
    }

    private static String loadKey() {
        try (InputStream in = GroqService.class.getResourceAsStream("/config.properties")) {
            if (in == null) return "";
            Properties p = new Properties();
            p.load(in);
            return p.getProperty("groq.api.key", "");
        } catch (Exception e) {
            return "";
        }
    }
}
