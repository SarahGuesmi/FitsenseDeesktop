package services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import models.MentalHealthAssessmentSubmission;
import services.WeatherService.WeatherInfo;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Calls the Groq API for AI-powered recommendations and advice.
 * Uses the same API key and model as GroqService (username generation).
 */
public class GroqAiService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.1-8b-instant";
    private static final String API_KEY = loadApiKey();

    private static GroqAiService instance;
    private final Gson gson = new Gson();

    private GroqAiService() {}

    // ── Singleton ─────────────────────────────────────────────────────────────

    public static synchronized GroqAiService getInstance() {
        if (instance == null) instance = new GroqAiService();
        return instance;
    }

    // ── Key loading (identical pattern to GroqService) ─────────────────────────

    private static String loadApiKey() {
        try (InputStream in = GroqAiService.class.getResourceAsStream("/config.properties")) {
            if (in == null) {
                System.err.println("[GroqAiService] config.properties not found in classpath.");
                return "";
            }
            Properties p = new Properties();
            p.load(in);
            String key = p.getProperty("groq.api.key", "").trim();
            if (key.isEmpty()) {
                System.err.println("[GroqAiService] groq.api.key is empty.");
            } else {
                System.out.println("[GroqAiService] Key loaded OK (" + key.substring(0, Math.min(6, key.length())) + "...)");
            }
            return key;
        } catch (Exception e) {
            System.err.println("[GroqAiService] Failed to load key: " + e.getMessage());
            return "";
        }
    }

    // ── Shared HTTP helper (same approach as GroqService) ──────────────────────

    private String chat(String systemPrompt, String userPrompt) {
        try {
            String bodyJson = buildBody(systemPrompt, userPrompt);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            System.out.println("[GroqAiService] Sending request, model=" + MODEL);

            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                System.err.println("[GroqAiService] HTTP " + resp.statusCode() + ": " + resp.body());
                throw new RuntimeException("Groq API returned " + resp.statusCode() + ": " + resp.body());
            }

            return extractContent(resp.body());

        } catch (Exception e) {
            System.err.println("[GroqAiService] chat() failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String buildBody(String systemPrompt, String userPrompt) {
        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.addProperty("temperature", 0.7);

        JsonArray messages = new JsonArray();

        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", systemPrompt);
        messages.add(sys);

        JsonObject usr = new JsonObject();
        usr.addProperty("role", "user");
        usr.addProperty("content", userPrompt);
        messages.add(usr);

        body.add("messages", messages);
        return gson.toJson(body);
    }

    private String extractContent(String json) {
        JsonObject root = gson.fromJson(json, JsonObject.class);
        return root.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString().trim();
    }

    /** Strip optional ```json ... ``` markdown fences from the response. */
    private String stripCodeFence(String raw) {
        if (raw == null) return "{}";
        String s = raw.trim();
        if (s.startsWith("```json")) s = s.substring(7);
        else if (s.startsWith("```")) s = s.substring(3);
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        return s.trim();
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Generate a mental health recommendation for the given assessment.
     */
    public CompletableFuture<JsonObject> generateRecommendation(MentalHealthAssessmentSubmission submission) {
        return CompletableFuture.supplyAsync(() -> {
            String systemPrompt =
                "You are a professional mental health coach. " +
                "Return ONLY a raw JSON object (no markdown) with exactly two keys: " +
                "'general_note' (a string with empathetic advice) and " +
                "'exercises' (an array of 2-3 objects each with 'name', 'duration', 'description').";

            String userPrompt = buildPrompt(submission);

            String raw = chat(systemPrompt, userPrompt);
            return gson.fromJson(stripCodeFence(raw), JsonObject.class);
        });
    }

    /**
     * Generate post-workout feedback advice.
     */
    public CompletableFuture<JsonObject> generateWorkoutFeedbackAdvice(
            String workoutName, int stars, String ratingLabel,
            String userComment, List<String> availableWorkouts) {

        return CompletableFuture.supplyAsync(() -> {
            boolean negative = stars <= 3;
            String workoutList = availableWorkouts.isEmpty() ? "none available" : String.join(", ", availableWorkouts);
            String commentPart = (userComment != null && !userComment.isBlank()) ? "User comment: \"" + userComment + "\". " : "";

            String userPrompt;
            if (negative) {
                userPrompt = "User rated \"" + workoutName + "\" " + stars + " stars. " + commentPart +
                        "Available workouts: " + workoutList + ". " +
                        "Return ONLY raw JSON with 'message', 'followup_question', 'suggestions' (array, never empty).";
            } else {
                userPrompt = "User rated \"" + workoutName + "\" " + stars + " stars. " + commentPart +
                        "Available workouts: " + workoutList + ". " +
                        "Return ONLY raw JSON with 'message', 'followup_question' (empty string), 'suggestions' (array, never empty).";
            }

            String systemPrompt = negative
                    ? "You are a fitness coach. Empathize, ask a follow-up, and suggest workouts. JSON only."
                    : "You are a fitness coach. Congratulate and suggest workouts. JSON only.";

            String raw = chat(systemPrompt, userPrompt);
            return gson.fromJson(stripCodeFence(raw), JsonObject.class);
        });
    }

    /**
     * Generate a short wellness tip based on weather conditions.
     */
    public CompletableFuture<String> generateWeatherAdvice(WeatherInfo weather) {
        return CompletableFuture.supplyAsync(() -> {
            String systemPrompt = "You are a compassionate mental wellness coach. Give brief, practical advice in 2-3 sentences. No JSON.";
            String userPrompt = "Weather: " + weather.condition + ", " + weather.description +
                    ", " + String.format("%.1f", weather.tempCelsius) + "°C, humidity " + weather.humidity + "%. " +
                    "Give a mental wellness tip for someone about to do a mental health check-in.";

            return chat(systemPrompt, userPrompt);
        });
    }

    // ── Prompt builder ─────────────────────────────────────────────────────────

    private String buildPrompt(MentalHealthAssessmentSubmission s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mental health recommendation for athlete. Metrics: ");
        sb.append("Stress=").append(s.getStress()).append("/5, ");
        sb.append("Sleep=").append(s.getSleep()).append("/5, ");
        sb.append("Mood=").append(s.getMood()).append("/5, ");
        sb.append("Motivation=").append(s.getMotivation()).append("/5, ");
        sb.append("Tiredness=").append(s.getMentalTired()).append("/5. ");
        if (s.getMemberNotes() != null && !s.getMemberNotes().isBlank()) {
            sb.append("Notes: \"").append(s.getMemberNotes()).append("\". ");
        }
        sb.append("Return JSON with 'general_note' and 'exercises' array.");
        return sb.toString();
    }
}
