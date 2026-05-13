package services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * YouTubeService — searches YouTube Data API v3 for workout videos.
 *
 * For negative ratings (1-3★): searches "beginner {workout} tutorial"
 * For positive ratings (4-5★): searches "advanced {workout} workout"
 *
 * Returns a list of VideoResult (title + watchUrl).
 */
public class YouTubeService {

    private static final String SEARCH_URL =
            "https://www.googleapis.com/youtube/v3/search";

    private static YouTubeService instance;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final String apiKey;

    private YouTubeService() {
        this.apiKey = loadApiKey();
    }

    private static String loadApiKey() {
        try (java.io.InputStream in =
                     YouTubeService.class.getResourceAsStream("/config.properties")) {
            if (in == null) {
                System.err.println("[YouTube] config.properties not found");
                return "";
            }
            java.util.Properties p = new java.util.Properties();
            p.load(in);
            String key = p.getProperty("youtube.api.key", "");
            System.out.println("[YouTube] API key loaded: " + (key.isEmpty() ? "EMPTY" : "OK (" + key.length() + " chars)"));
            return key;
        } catch (Exception e) { 
            System.err.println("[YouTube] Error loading API key: " + e.getMessage());
            return ""; 
        }
    }

    public static synchronized YouTubeService getInstance() {
        if (instance == null) instance = new YouTubeService();
        return instance;
    }

    /** A single YouTube video result. */
    public record VideoResult(String title, String videoId) {
        public String watchUrl() {
            return "https://www.youtube.com/watch?v=" + videoId;
        }
    }

    /**
     * Searches YouTube for videos related to the workout.
     *
     * @param workoutName  name of the workout (e.g. "cardio workout")
     * @param negative     true for poor ratings → beginner/alternative query
     *                     false for good ratings → similar/advanced query
     * @param maxResults   number of videos to return (1-5)
     */
    public CompletableFuture<List<VideoResult>> searchWorkoutVideos(
            String workoutName, boolean negative, int maxResults) {

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("[YouTube] API key is empty - cannot make requests");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        String query = negative
                ? "beginner " + workoutName + " tutorial workout"
                : workoutName + " full workout routine";

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        String url = SEARCH_URL
                + "?part=snippet"
                + "&type=video"
                + "&videoCategoryId=17"   // Sports category
                + "&q=" + encodedQuery
                + "&maxResults=" + maxResults
                + "&key=" + apiKey;

        System.out.println("[YouTube] Searching for: " + query);
        System.out.println("[YouTube] Request URL: " + url.replace(apiKey, "***API_KEY***"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("[YouTube] Response status: " + response.statusCode());
                    List<VideoResult> results = new ArrayList<>();
                    if (response.statusCode() != 200) {
                        System.err.println("[YouTube] API error " + response.statusCode()
                                + ": " + response.body());
                        return results;
                    }
                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    JsonArray items = json.has("items")
                            ? json.getAsJsonArray("items") : new JsonArray();

                    System.out.println("[YouTube] Found " + items.size() + " items in response");

                    for (int i = 0; i < items.size(); i++) {
                        JsonObject item = items.get(i).getAsJsonObject();
                        JsonObject id = item.getAsJsonObject("id");
                        JsonObject snippet = item.getAsJsonObject("snippet");

                        if (id == null || !id.has("videoId")) continue;
                        String videoId = id.get("videoId").getAsString();
                        String title = snippet != null && snippet.has("title")
                                ? snippet.get("title").getAsString() : "Workout video";

                        results.add(new VideoResult(title, videoId));
                        System.out.println("[YouTube] Added video: " + title);
                    }
                    System.out.println("[YouTube] Returning " + results.size() + " videos");
                    return results;
                });
    }
}
