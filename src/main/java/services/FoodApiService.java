package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FoodApiService {
    private static final String YOUTUBE_API_KEY = "AIzaSyCjVl09loNuZgefWH2kRcJI-R4YZ7oLRUw";

    private static final String USDA_API_KEY = "FXhAbJjmXRyDqOO66Vos6k3ZrJ7fwGCrlKHPQbAU";
    public List<YoutubeVideo> searchYoutubeVideos(String query) {
        List<YoutubeVideo> videos = new ArrayList<>();

        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

            String url = "https://www.googleapis.com/youtube/v3/search"
                    + "?part=snippet"
                    + "&type=video"
                    + "&maxResults=4"
                    + "&videoEmbeddable=true"
                    + "&safeSearch=moderate"
                    + "&q=" + encoded
                    + "&key=" + YOUTUBE_API_KEY;

            JSONObject json = getJson(url);
            JSONArray items = json.optJSONArray("items");

            if (items == null) return videos;

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);

                String videoId = item.getJSONObject("id").optString("videoId");
                JSONObject snippet = item.getJSONObject("snippet");

                String title = snippet.optString("title");
                String channel = snippet.optString("channelTitle");
                String image = snippet
                        .getJSONObject("thumbnails")
                        .getJSONObject("medium")
                        .optString("url");

                videos.add(new YoutubeVideo(videoId, title, channel, image));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return videos;
    }
    /* ================= USDA: CALORIES ================= */
    public int getCaloriesFromFood(String foodQuery) {
        try {
            String encoded = URLEncoder.encode(foodQuery, StandardCharsets.UTF_8);

            String url = "https://api.nal.usda.gov/fdc/v1/foods/search"
                    + "?query=" + encoded
                    + "&pageSize=1"
                    + "&api_key=" + USDA_API_KEY;

            JSONObject json = getJson(url);
            JSONArray foods = json.optJSONArray("foods");

            if (foods == null || foods.isEmpty()) return 0;

            JSONObject firstFood = foods.getJSONObject(0);
            JSONArray nutrients = firstFood.optJSONArray("foodNutrients");

            if (nutrients == null) return 0;

            for (int i = 0; i < nutrients.length(); i++) {
                JSONObject nutrient = nutrients.getJSONObject(i);
                String name = nutrient.optString("nutrientName", "");

                if (name.equalsIgnoreCase("Energy")) {
                    return (int) Math.round(nutrient.optDouble("value", 0));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /* ================= TheMealDB: RECIPES ================= */
    public List<ApiRecipe> searchRecipesByIngredients(String query) {
        List<ApiRecipe> recipes = new ArrayList<>();

        try {
            List<String> ingredients = prepareIngredients(query);
            if (ingredients.isEmpty()) return recipes;

            List<List<String>> idSets = new ArrayList<>();

            for (String ing : ingredients) {
                String url = "https://www.themealdb.com/api/json/v1/1/filter.php?i="
                        + URLEncoder.encode(ing, StandardCharsets.UTF_8);

                JSONObject json = getJson(url);
                JSONArray meals = json.optJSONArray("meals");

                List<String> ids = new ArrayList<>();

                if (meals != null) {
                    for (int i = 0; i < meals.length(); i++) {
                        ids.add(meals.getJSONObject(i).optString("idMeal"));
                    }
                }

                idSets.add(ids);
            }

            if (idSets.isEmpty()) return recipes;

            List<String> commonIds = new ArrayList<>(idSets.get(0));

            for (int i = 1; i < idSets.size(); i++) {
                commonIds.retainAll(idSets.get(i));
            }

            if (commonIds.isEmpty()) {
                commonIds = idSets.get(0);
            }

            commonIds = commonIds.subList(0, Math.min(commonIds.size(), 4));

            for (String id : commonIds) {
                String url = "https://www.themealdb.com/api/json/v1/1/lookup.php?i=" + id;

                JSONObject detail = getJson(url);
                JSONArray meals = detail.optJSONArray("meals");

                if (meals == null || meals.isEmpty()) continue;

                JSONObject meal = meals.getJSONObject(0);

                List<String> ings = new ArrayList<>();

                for (int k = 1; k <= 20; k++) {
                    String name = meal.optString("strIngredient" + k, "").trim();
                    String measure = meal.optString("strMeasure" + k, "").trim();

                    if (!name.isEmpty()) {
                        ings.add((measure + " " + name).trim());
                    }
                }

                recipes.add(new ApiRecipe(
                        meal.optString("idMeal"),
                        meal.optString("strMeal"),
                        meal.optString("strMealThumb"),
                        meal.optString("strCategory"),
                        meal.optString("strArea"),
                        meal.optString("strInstructions"),
                        ings
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return recipes;
    }

    private JSONObject getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return new JSONObject(response.body());
    }

    private List<String> prepareIngredients(String q) {
        Map<String, String> map = new HashMap<>();
        map.put("oeuf", "egg");
        map.put("oeufs", "egg");
        map.put("tomate", "tomato");
        map.put("tomates", "tomato");
        map.put("epinard", "spinach");
        map.put("épinard", "spinach");
        map.put("epinards", "spinach");
        map.put("épinards", "spinach");
        map.put("poulet", "chicken");
        map.put("viande", "beef");
        map.put("fromage", "cheese");
        map.put("lait", "milk");
        map.put("pomme", "apple");

        if (q == null || q.isBlank()) return new ArrayList<>();

        String[] parts = q.toLowerCase().trim().split("[\\s,;]+");

        List<String> result = new ArrayList<>();

        for (int i = 0; i < parts.length && result.size() < 3; i++) {
            String word = parts[i].trim();
            if (!word.isEmpty()) {
                result.add(map.getOrDefault(word, word));
            }
        }

        return result.stream().distinct().toList();
    }

    public static class ApiRecipe {
        public String id;
        public String title;
        public String image;
        public String category;
        public String area;
        public String instructions;
        public List<String> ingredients;

        public ApiRecipe(String id, String title, String image, String category,
                         String area, String instructions, List<String> ingredients) {
            this.id = id;
            this.title = title;
            this.image = image;
            this.category = category;
            this.area = area;
            this.instructions = instructions;
            this.ingredients = ingredients;
        }
    }

    private final HttpClient client = HttpClient.newHttpClient();

    public static class YoutubeVideo {
        public String videoId;
        public String title;
        public String channel;
        public String image;

        public YoutubeVideo(String videoId, String title, String channel, String image) {
            this.videoId = videoId;
            this.title = title;
            this.channel = channel;
            this.image = image;
        }
    }
}