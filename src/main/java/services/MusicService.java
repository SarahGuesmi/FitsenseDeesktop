package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches free music tracks from Jamendo API.
 * Free for non-commercial use — https://developer.jamendo.com
 */
public class MusicService {

    // Official Jamendo test client_id — for production get yours at devportal.jamendo.com
    private static final String CLIENT_ID = "a9e7a0dc";
    private static final String BASE = "https://api.jamendo.com/v3.0/tracks/";

    public static class Track {
        public final String title;
        public final String artist;
        public final String audioUrl;

        public Track(String title, String artist, String audioUrl) {
            this.title    = title;
            this.artist   = artist;
            this.audioUrl = audioUrl;
        }
    }

    public enum Mood {
        WORKOUT("workout",    "💪 Workout"),
        HIIT("energetic",     "🔥 HIIT / Cardio"),
        RELAX("ambient",      "🧘 Relax / Yoga"),
        ENERGY("electronic",  "⚡ Energy Boost");

        public final String tag;
        public final String label;
        Mood(String tag, String label) { this.tag = tag; this.label = label; }
    }

    public List<Track> fetchTracks(Mood mood) throws Exception {
        String url = BASE
                + "?client_id=" + CLIENT_ID
                + "&format=json"
                + "&limit=5"
                + "&tags=" + mood.tag
                + "&audioformat=mp32"
                + "&order=popularity_total"
                + "&include=musicinfo";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        System.out.println("Jamendo response: " + resp.body().substring(0, Math.min(300, resp.body().length())));

        JSONObject json = new JSONObject(resp.body());

        // Check API status
        JSONObject headers = json.optJSONObject("headers");
        if (headers != null && !"success".equals(headers.optString("status"))) {
            System.err.println("Jamendo API error: " + headers.optString("error_message"));
            return List.of();
        }

        JSONArray results = json.optJSONArray("results");
        List<Track> tracks = new ArrayList<>();
        if (results != null) {
            for (int i = 0; i < results.length(); i++) {
                JSONObject t = results.getJSONObject(i);
                String audio = t.optString("audio", "");
                if (!audio.isBlank()) {
                    tracks.add(new Track(
                            t.optString("name", "Unknown"),
                            t.optString("artist_name", "Unknown"),
                            audio
                    ));
                }
            }
        }
        return tracks;
    }
}
