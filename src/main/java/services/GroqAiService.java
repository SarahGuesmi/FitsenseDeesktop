package services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import models.MentalHealthAssessmentSubmission;
import services.WeatherService.WeatherInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class GroqAiService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private static GroqAiService instance;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final String apiKey;

    private GroqAiService() {
        this.apiKey = loadApiKey();
    }

    private static String loadApiKey() {
        try (java.io.InputStream in = GroqAiService.class.getResourceAsStream("/config.properties")) {
            if (in == null) return "";
            java.util.Properties p = new java.util.Properties();
            p.load(in);
            return p.getProperty("groq.api.key", "");
        } catch (Exception e) { return ""; }
    }

    public static synchronized GroqAiService getInstance() {
        if (instance == null) instance = new GroqAiService();
        return instance;
    }

    public CompletableFuture<JsonObject> generateRecommendation(MentalHealthAssessmentSubmission submission) {
        String prompt = buildPrompt(submission);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        
        JsonArray messages = new JsonArray();
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are a professional mental health coach. You must return your response in JSON format. The JSON should have two keys: 'general_note' (a string with empathetic advice) and 'exercises' (an array of objects, each with 'name', 'duration' (string), and 'description' properties).");
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);

        body.add("messages", messages);
        body.addProperty("temperature", 0.7);
        body.add("response_format", gson.fromJson("{\"type\": \"json_object\"}", JsonObject.class));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Groq API error: " + response.body());
                    }
                    JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                    String content = jsonResponse.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                    return gson.fromJson(content, JsonObject.class);
                });
    }

    private String buildPrompt(MentalHealthAssessmentSubmission s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Provide a mental health recommendation for an athlete with these metrics:\n");
        sb.append("- Stress: ").append(s.getStress()).append("/5, Sleep: ").append(s.getSleep())
          .append("/5, Mood: ").append(s.getMood()).append("/5, Motivation: ").append(s.getMotivation())
          .append("/5, Tiredness: ").append(s.getMentalTired()).append("/5.\n");
        
        if (s.getMemberNotes() != null && !s.getMemberNotes().isBlank()) {
            sb.append("Athlete's comment: \"").append(s.getMemberNotes()).append("\"\n");
        }
        
        sb.append("\nReturn JSON with 'general_note' and an array of 2-3 specific 'exercises' (name, duration, description). Focus on stress relief and recovery.");
        return sb.toString();
    }

    /**
     * Generates a short wellness tip based purely on current weather conditions.
     * Returns a plain string (the advice text) via CompletableFuture.
     */
    public CompletableFuture<String> generateWeatherAdvice(WeatherInfo weather) {
        String prompt = "The current weather is: " + weather.condition
                + ", " + weather.description
                + ", temperature " + String.format("%.1f", weather.tempCelsius) + "°C"
                + ", humidity " + weather.humidity + "%."
                + " Give a single short (2-3 sentences) mental wellness tip for someone about to do a mental health check-in today."
                + " Consider how this weather might affect mood, energy, and motivation."
                + " Be warm, empathetic, and practical. Return only the advice text, no JSON.";

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);

        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", "You are a compassionate mental wellness coach. Give brief, practical advice.");
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prompt);
        messages.add(user);

        body.add("messages", messages);
        body.addProperty("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Groq API error: " + response.body());
                    }
                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    return json.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString().trim();
                });
    }
}
