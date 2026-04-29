package services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Fetches current weather from OpenWeatherMap API.
 * Free tier: 1,000,000 calls/month.
 * Docs: https://openweathermap.org/current
 */
public class WeatherService {

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String DEFAULT_CITY = "Tunis";

    private static WeatherService instance;
    private final HttpClient httpClient;
    private final String apiKey;

    private WeatherService() {
        this.httpClient = HttpClient.newHttpClient();
        this.apiKey = loadApiKey();
    }

    private static String loadApiKey() {
        try (java.io.InputStream in = WeatherService.class.getResourceAsStream("/config.properties")) {
            if (in == null) return "";
            java.util.Properties p = new java.util.Properties();
            p.load(in);
            return p.getProperty("weather.api.key", "d3f77cf0258317fbf250a358c95705d1");
        } catch (Exception e) { return ""; }
    }

    public static synchronized WeatherService getInstance() {
        if (instance == null) {
            instance = new WeatherService();
        }
        return instance;
    }

    /**
     * Fetches weather for the given city (falls back to DEFAULT_CITY if null/blank).
     * Returns a {@link WeatherInfo} on success.
     */
    public CompletableFuture<WeatherInfo> fetchWeather(String city) {
        String target = (city == null || city.isBlank()) ? DEFAULT_CITY : city.trim();
        String url = BASE_URL
                + "?q=" + URLEncoder.encode(target, StandardCharsets.UTF_8)
                + "&appid=" + apiKey
                + "&units=metric";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("OpenWeatherMap error " + response.statusCode());
                    }
                    return parse(response.body());
                });
    }

    private WeatherInfo parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        String city = root.has("name") ? root.get("name").getAsString() : DEFAULT_CITY;

        JsonObject main = root.getAsJsonObject("main");
        double temp = main.get("temp").getAsDouble();
        int humidity = main.get("humidity").getAsInt();

        JsonObject weatherObj = root.getAsJsonArray("weather").get(0).getAsJsonObject();
        String condition = weatherObj.get("main").getAsString();      // e.g. "Rain", "Clear"
        String description = weatherObj.get("description").getAsString(); // e.g. "light rain"

        return new WeatherInfo(city, temp, humidity, condition, description);
    }

    // ── Inner record ──────────────────────────────────────────────────────────

    public static class WeatherInfo {
        public final String city;
        public final double tempCelsius;
        public final int humidity;
        public final String condition;   // "Clear", "Rain", "Clouds", "Snow", etc.
        public final String description;

        public WeatherInfo(String city, double tempCelsius, int humidity,
                           String condition, String description) {
            this.city = city;
            this.tempCelsius = tempCelsius;
            this.humidity = humidity;
            this.condition = condition;
            this.description = description;
        }

        /** Returns a weather emoji matching the condition. */
        public String emoji() {
            return switch (condition) {
                case "Clear"       -> "☀️";
                case "Clouds"      -> "☁️";
                case "Rain",
                     "Drizzle"     -> "🌧️";
                case "Thunderstorm"-> "⛈️";
                case "Snow"        -> "❄️";
                case "Mist",
                     "Fog",
                     "Haze"        -> "🌫️";
                default            -> "🌡️";
            };
        }

        /**
         * Returns a short mental-health-aware tip based on weather condition
         * and the user's wellbeing score ratio.
         */
        public String mentalTip(double scoreRatio) {
            if (condition.equals("Rain") || condition.equals("Drizzle") || condition.equals("Thunderstorm")) {
                return scoreRatio <= 0.48
                        ? "Rainy days can affect mood — be extra kind to yourself today."
                        : "Rainy outside, but you're doing well! A warm drink can help.";
            }
            if (condition.equals("Clear")) {
                return scoreRatio <= 0.48
                        ? "It's sunny outside — a short walk might lift your spirits."
                        : "Great weather and a good score — keep it up!";
            }
            if (condition.equals("Snow")) {
                return "Cold and snowy — staying warm and cozy supports mental wellbeing.";
            }
            return scoreRatio <= 0.48
                    ? "Take it easy today and focus on small positive actions."
                    : "You're doing well — keep maintaining those healthy habits!";
        }

        @Override
        public String toString() {
            return String.format("%s %s — %.1f°C, %s, humidity %d%%",
                    emoji(), city, tempCelsius, description, humidity);
        }
    }
}
