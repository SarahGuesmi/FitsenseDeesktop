package controllers;
import javafx.scene.layout.GridPane;
import javafx.scene.control.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.layout.*;
import models.RecetteNutritionnelle;
import models.User;
import services.FoodApiService;
import services.RecetteNutritionnelleService;
import utils.SessionManager;
import java.sql.SQLException;
import java.util.List;
import javafx.geometry.Pos;
import services.RecetteFavoriService;
public class NutritionController {
    private static final String DEFAULT_IMAGE_URL =
            "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1200&q=60";


    private final FoodApiService foodApiService = new FoodApiService();
    @FXML private TextField youtubeSearchField;
    @FXML private GridPane youtubeVideosBox;
    private int previewCalories = 0;
    @FXML
    private GridPane recipesGrid;
    @FXML private ScrollPane apiRecipesScroll;
    @FXML
    private ComboBox<String> allergyFilterCombo;
    @FXML private TextField calorieField;
    @FXML private TextField proteinField;
    @FXML private TextField ingredientInput;
    @FXML private VBox apiRecipesBox;
    @FXML
    private TextField searchField;
    @FXML private TextField foodInput;
    @FXML private Label foodCaloriesLabel;
    @FXML private Button addCaloriesBtn;
    private final RecetteNutritionnelleService recetteService = new RecetteNutritionnelleService();
    @FXML private VBox recipeSearchPanel;
    @FXML
    public void initialize() {
        allergyFilterCombo.getItems().addAll(
                "All",
                "Peanuts",
                "Tree nuts",
                "Milk",
                "Eggs",
                "Wheat / Gluten",
                "Soy",
                "Fish",
                "Shellfish",
                "Sesame",
                "Mustard",
                "Celery",
                "Sulfites"
        );
        loadDefaultYoutubeVideos();
        recipeSearchPanel.setVisible(false);
        recipeSearchPanel.setManaged(false);
        addCaloriesBtn.setDisable(true); // 🔥 important
        allergyFilterCombo.setValue("All");
        apiRecipesScroll.setVisible(false);
        apiRecipesScroll.setManaged(false);
        loadRecipes();
    }
    private void loadDefaultYoutubeVideos() {
        youtubeVideosBox.getChildren().clear();
        new Thread(() -> {
            List<FoodApiService.YoutubeVideo> videos =
                    foodApiService.searchYoutubeVideos("high protein breakfast healthy recipes");
            javafx.application.Platform.runLater(() -> showYoutubeVideos(videos));
        }, "youtube-default-load").start();
    }

    @FXML
    private void onSearchYoutubeVideos() {
        String query = youtubeSearchField.getText();
        if (query == null || query.isBlank()) {
            query = "high protein breakfast healthy recipes";
        }
        final String finalQuery = query;
        new Thread(() -> {
            List<FoodApiService.YoutubeVideo> videos =
                    foodApiService.searchYoutubeVideos(finalQuery);
            javafx.application.Platform.runLater(() -> showYoutubeVideos(videos));
        }, "youtube-search").start();
    }

    private void showYoutubeVideos(List<FoodApiService.YoutubeVideo> videos) {
        youtubeVideosBox.getChildren().clear();
        youtubeVideosBox.getColumnConstraints().clear();

        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPrefWidth(220);
            col.setMinWidth(220);
            col.setMaxWidth(220);
            youtubeVideosBox.getColumnConstraints().add(col);
        }

        for (int i = 0; i < videos.size() && i < 4; i++) {
            VBox card = createYoutubeCard(videos.get(i));
            youtubeVideosBox.add(card, i, 0);
        }
    }    @FXML
    private void toggleRecipeSearch() {

        boolean visible = recipeSearchPanel.isVisible();

        recipeSearchPanel.setVisible(!visible);
        recipeSearchPanel.setManaged(!visible);
    }
    @FXML
    private void onCloseApiRecipes() {
        apiRecipesBox.getChildren().clear();
        apiRecipesScroll.setVisible(false);
        apiRecipesScroll.setManaged(false);
    }
    @FXML
    private void onSearchApiRecipes() {
        String ingredient = ingredientInput.getText();
        if (ingredient == null || ingredient.isBlank()) return;

        // Show scroll and clear previous results
        apiRecipesScroll.setVisible(true);
        apiRecipesScroll.setManaged(true);
        apiRecipesBox.getChildren().clear();

        // Loading indicator
        javafx.scene.control.Label loading = new javafx.scene.control.Label("⏳ Searching recipes...");
        loading.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:13px;-fx-padding:12;");
        apiRecipesBox.getChildren().add(loading);

        // Run API call in background — never block the UI thread
        new Thread(() -> {
            List<FoodApiService.ApiRecipe> recipes =
                    foodApiService.searchRecipesByIngredients(ingredient);

            javafx.application.Platform.runLater(() -> {
                apiRecipesBox.getChildren().clear();
                if (recipes.isEmpty()) {
                    javafx.scene.control.Label empty = new javafx.scene.control.Label(
                            "No recipes found for \"" + ingredient + "\". Try: chicken, egg, tomato");
                    empty.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:13px;-fx-padding:12;");
                    apiRecipesBox.getChildren().add(empty);
                } else {
                    for (FoodApiService.ApiRecipe r : recipes) {
                        apiRecipesBox.getChildren().add(createApiRecipeCard(r));
                    }
                }
            });
        }, "recipe-search").start();
    }
    private HBox createApiRecipeCard(FoodApiService.ApiRecipe recipe) {
        HBox card = new HBox(14);
        card.getStyleClass().add("api-recipe-row");
        card.setAlignment(Pos.CENTER_LEFT);

        ImageView imageView = new ImageView();
        Image img = new Image(recipe.image, true);
        imageView.setImage(img);
        imageView.setFitWidth(120);
        imageView.setFitHeight(85);
        imageView.setPreserveRatio(false);

        Label title = new Label(safe(recipe.title));
        title.getStyleClass().add("api-recipe-title");
        title.setWrapText(true);

        Label info = new Label(safe(recipe.category) + " • " + safe(recipe.area));
        info.getStyleClass().add("api-recipe-info");

        VBox textBox = new VBox(6, title, info);
        textBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button detailsBtn = new Button("View");
        detailsBtn.getStyleClass().add("btn-view-recipe");
        detailsBtn.setOnAction(e -> showApiRecipeDetails(recipe));

        card.getChildren().addAll(imageView, textBox, spacer, detailsBtn);
        return card;
    }    private void showApiRecipeDetails(FoodApiService.ApiRecipe recipe) {

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(recipe.title);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color:#111827;");

        Label title = new Label(recipe.title);
        title.setStyle("-fx-text-fill:white; -fx-font-size:20px; -fx-font-weight:bold;");

        ImageView image = new ImageView(new Image(recipe.image, true));
        image.setFitWidth(400);
        image.setFitHeight(250);

        Label instructions = new Label(recipe.instructions);
        instructions.setWrapText(true);
        instructions.setStyle("-fx-text-fill:#d1d5db;");

        root.getChildren().addAll(title, image, instructions);

        Scene scene = new Scene(new ScrollPane(root), 450, 500);
        modal.setScene(scene);
        modal.showAndWait();
    }
    @FXML
    private void onFilterRecipes() {
        loadRecipes();
    }
    @FXML
    private void onResetFilters() {
        searchField.clear();
        allergyFilterCombo.setValue(null);
        calorieField.setText("1500");
        proteinField.setText("0");
        loadRecipes();
    }

    private void loadRecipes() {
        try {
            recipesGrid.getChildren().clear();

            List<RecetteNutritionnelle> recipes = recetteService.read();

            String search = searchField == null || searchField.getText() == null
                    ? ""
                    : searchField.getText().trim().toLowerCase();

            String allergy = allergyFilterCombo == null || allergyFilterCombo.getValue() == null
                    ? "All"
                    : allergyFilterCombo.getValue().toString();

            int maxCalories = 1500;
            int minProteins = 0;

            try {
                if (calorieField != null
                        && calorieField.getText() != null
                        && !calorieField.getText().isBlank()) {
                    maxCalories = Integer.parseInt(calorieField.getText().trim());
                }

                if (proteinField != null
                        && proteinField.getText() != null
                        && !proteinField.getText().isBlank()) {
                    minProteins = Integer.parseInt(proteinField.getText().trim());
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter valid numbers for calories and proteins.");
            }

            int finalMaxCalories = maxCalories;
            int finalMinProteins = minProteins;

            recipes = recipes.stream()

                    // Search by name
                    .filter(r -> {
                        String title = r.getTitle() == null ? "" : r.getTitle().toLowerCase();
                        return search.isEmpty() || title.contains(search);
                    })

                    // Allergy filter: hide recipes containing selected allergen
                    .filter(r -> {
                        if (allergy.equalsIgnoreCase("All")) return true;

                        String allergens = extractAllergens(r.getIngredients()).toLowerCase();
                        return !allergens.contains(allergy.toLowerCase());
                    })

                    // Max calories
                    .filter(r -> r.getKcal() != null && r.getKcal() <= finalMaxCalories)

                    // Min proteins
                    .filter(r -> r.getProteins() != null && r.getProteins() >= finalMinProteins)

                    .toList();

            int col = 0;
            int row = 0;

            for (RecetteNutritionnelle recipe : recipes) {
                VBox card = createRecipeCard(recipe);
                recipesGrid.add(card, col, row);

                col++;
                if (col == 3) {
                    col = 0;
                    row++;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private VBox createRecipeCard(RecetteNutritionnelle recipe) {

        VBox card = new VBox(10);
        card.getStyleClass().add("recipe-card");
        card.setPrefWidth(300);

        ImageView imageView = new ImageView();

        String imageUrl = (recipe.getImage() == null || recipe.getImage().isBlank())
                ? DEFAULT_IMAGE_URL
                : recipe.getImage();

        Image img = new Image(imageUrl, false);
        if (img.isError()) {
            img = new Image(DEFAULT_IMAGE_URL, false);
        }

        imageView.setImage(img);
        imageView.setFitWidth(300);
        imageView.setFitHeight(170);
        imageView.setPreserveRatio(false);

        // IMAGE CONTAINER
        StackPane imageContainer = new StackPane(imageView);

        // BADGE TYPE
        Label typeBadge = new Label(
                recipe.getTypeMeal() == null ? "MEAL" : recipe.getTypeMeal().toUpperCase()
        );
        typeBadge.getStyleClass().add("recipe-type-badge");

        StackPane.setAlignment(typeBadge, Pos.TOP_LEFT);
        StackPane.setMargin(typeBadge, new Insets(12));

        imageContainer.getChildren().add(typeBadge);

        // ❤️ FAVORI (FIXED)
        User currentUser = SessionManager.getCurrentUser();

        if (currentUser != null && currentUser.getRolesJson().contains("ROLE_USER")) {

            RecetteFavoriService favoriService = new RecetteFavoriService();

            String userId = currentUser.getId().toString();
            String recipeId = recipe.getId().toString();

            Button heartBtn = new Button("♡");
            heartBtn.getStyleClass().add("heart-btn");

            // état initial
            if (favoriService.isFavorite(userId, recipeId)) {
                heartBtn.setText("♥");
                heartBtn.getStyleClass().add("heart-btn-liked");
            }

            // CLICK ❤️
            heartBtn.setOnAction(e -> {
                e.consume();

                System.out.println("CLICK HEART 🔥");

                favoriService.toggleFavorite(userId, recipeId);

                boolean nowFav = favoriService.isFavorite(userId, recipeId);

                if (nowFav) {
                    heartBtn.setText("♥");
                    heartBtn.getStyleClass().add("heart-btn-liked");
                } else {
                    heartBtn.setText("♡");
                    heartBtn.getStyleClass().remove("heart-btn-liked");
                }
            });

            StackPane.setAlignment(heartBtn, Pos.TOP_RIGHT);
            StackPane.setMargin(heartBtn, new Insets(12));

            imageContainer.getChildren().add(heartBtn);
        }

        // TEXT
        Label title = new Label(safe(recipe.getTitle()));
        title.getStyleClass().add("recipe-card-title");

        Label desc = new Label(safe(recipe.getDescription()));
        desc.getStyleClass().add("recipe-card-desc");

        // STATS
        HBox stats = new HBox(10);

        Label kcal = new Label("💧 " + safe(recipe.getKcal()) + " KCAL");
        kcal.getStyleClass().add("recipe-kcal-chip");

        Label proteins = new Label(safe(recipe.getProteins()) + "g PROT");
        proteins.getStyleClass().add("recipe-prot-chip");

        stats.getChildren().addAll(kcal, proteins);

        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(14));
        contentBox.getChildren().addAll(title, desc, stats);

        // ALLERGY
        String allergens = extractAllergens(recipe.getIngredients());
        if (!allergens.isEmpty()) {
            Label warn = new Label("⚠ Allergy warning");
            warn.getStyleClass().add("allergy-warning");
            contentBox.getChildren().add(warn);
        }

        card.getChildren().addAll(imageContainer, contentBox);

        // CLICK CARD → DETAILS
        card.setOnMouseClicked(e -> showDetails(recipe));

        return card;
    }
    private void showDetails(RecetteNutritionnelle recipe) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Recipe Details");

        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("recipe-modal-card");

        Label title = new Label(safe(recipe.getTitle()));
        title.getStyleClass().add("recipe-modal-title");
        title.setWrapText(true);

        HBox badges = new HBox(10);
        badges.getChildren().addAll(
                badge("🔥 " + safe(recipe.getKcal()) + " kcal"),
                badge("💪 " + safe(recipe.getProteins()) + " g proteins"),
                badge("🍽 " + safe(recipe.getTypeMeal()))
        );

        ImageView imageView = new ImageView();

        String imageUrl = recipe.getImage() == null || recipe.getImage().isBlank()
                ? DEFAULT_IMAGE_URL
                : recipe.getImage();

        Image img = new Image(imageUrl, true);

        if (img.isError()) {
            img = new Image(DEFAULT_IMAGE_URL, true);
        }

        imageView.setImage(img);
        imageView.setFitWidth(680);
        imageView.setFitHeight(300);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("recipe-detail-image");

        String allergens = extractAllergens(recipe.getIngredients());

        VBox allergyBox = new VBox(6);
        if (!allergens.isEmpty()) {
            allergyBox.getStyleClass().add("allergy-box");

            Label allergyTitle = new Label("⚠ Allergy Warning");
            allergyTitle.getStyleClass().add("allergy-title");

            Label allergyText = new Label(
                    "This recipe is not recommended if you are allergic to: " + allergens
            );
            allergyText.setWrapText(true);
            allergyText.getStyleClass().add("allergy-text");

            allergyBox.getChildren().addAll(allergyTitle, allergyText);
        }

        Label descTitle = sectionTitle("DESCRIPTION");
        Label desc = sectionText(safe(recipe.getDescription()));
        desc.setMaxWidth(680);

        Label ingTitle = sectionTitle("INGREDIENTS");
        Label ingredients = sectionText(cleanIngredients(recipe.getIngredients()));
        ingredients.setMaxWidth(680);

        Label prepTitle = sectionTitle("PREPARATION");
        Label preparation = sectionText(safe(recipe.getPreparation()));
        preparation.setMaxWidth(680);

        User currentUser = SessionManager.getCurrentUser();

        Button doneBtn = new Button("✔ Mark as done");
        doneBtn.getStyleClass().add("recipe-save-btn");

        doneBtn.setOnAction(e -> {
            try {
                if (currentUser == null || currentUser.getId() == null) {
                    return;
                }

                recetteService.markAsDone(
                        currentUser.getId().toString(),
                        recipe
                );

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Done");
                alert.setHeaderText(null);
                alert.setContentText("Recipe added to your daily calories!");
                alert.showAndWait();

                modal.close();

            } catch (Exception ex) {
                ex.printStackTrace();

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Could not save this recipe as consumed.");
                alert.showAndWait();
            }
        });

        root.getChildren().addAll(
                title,
                badges,
                imageView,
                allergyBox,
                descTitle,
                desc,
                ingTitle,
                ingredients,
                prepTitle,
                preparation,
                doneBtn
        );

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("nutrition-detail-scroll");

        Scene scene = new Scene(scroll, 760, 780);
        scene.getStylesheets().add(
                getClass().getResource("/css/nutrition.css").toExternalForm()
        );

        modal.setScene(scene);
        modal.showAndWait();
    }    private Label badge(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("recipe-badge");
        return label;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("recipe-section-title");
        return label;
    }

    private Label sectionText(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("recipe-section-text");
        return label;
    }
    @FXML
    private void onCheckCalories() {
        String food = foodInput.getText();
        if (food == null || food.isBlank()) {
            foodCaloriesLabel.setText("Please type a food first.");
            previewCalories = 0;
            return;
        }
        foodCaloriesLabel.setText("⏳ Checking...");
        addCaloriesBtn.setDisable(true);

        new Thread(() -> {
            int cal = foodApiService.getCaloriesFromFood(food);
            javafx.application.Platform.runLater(() -> {
                previewCalories = cal;
                if (cal <= 0) {
                    foodCaloriesLabel.setText("No calories found.");
                    addCaloriesBtn.setDisable(true);
                } else {
                    foodCaloriesLabel.setText(cal + " kcal");
                    addCaloriesBtn.setDisable(false);
                }
            });
        }, "calorie-check").start();
    }
    @FXML
    private void onAddCalories() {
        try {
            User currentUser = SessionManager.getCurrentUser();

            if (currentUser == null || previewCalories <= 0) {
                return;
            }

            recetteService.addCustomCalories(
                    currentUser.getId().toString(),
                    foodInput.getText(),
                    previewCalories
            );

            foodInput.clear();
            foodCaloriesLabel.setText("Calories added ✅");
            addCaloriesBtn.setDisable(true);
            previewCalories = 0;

        } catch (Exception e) {
            e.printStackTrace();
            foodCaloriesLabel.setText("Error while adding calories.");
        }
    }
    private String extractAllergens(String ingredients) {
        if (ingredients == null) return "";

        for (String line : ingredients.split("\\n")) {
            if (line.trim().startsWith("ALLERGENS:")) {
                return line.replace("ALLERGENS:", "").trim();
            }
        }

        return "";
    }
    private VBox createYoutubeCard(FoodApiService.YoutubeVideo video) {
        VBox card = new VBox(8);
        card.getStyleClass().add("youtube-video-card");

        card.setPrefWidth(220);
        card.setMinWidth(220);
        card.setMaxWidth(220);

        ImageView image = new ImageView(new Image(video.image, true));
        image.setFitWidth(220);
        image.setFitHeight(120);
        image.setPreserveRatio(true);
        image.setSmooth(true);

        Label title = new Label(video.title);
        title.setWrapText(true);
        title.getStyleClass().add("youtube-video-title");

        Label channel = new Label(video.channel);
        channel.getStyleClass().add("youtube-video-channel");

        Button openBtn = new Button("Watch");
        openBtn.getStyleClass().add("youtube-btn");
        openBtn.setOnAction(e -> showYoutubePopup(video.videoId));

        card.getChildren().addAll(image, title, channel, openBtn);

        return card;
    }
    private void showYoutubePopup(String videoId) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("YouTube Video");

        VBox root = new VBox(18);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #111827;");

        Label title = new Label("Watch this video on YouTube");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        Button openBtn = new Button("▶ Open on YouTube");
        openBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #3b82f6, #a855f7);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 12 26;"
        );

        openBtn.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(
                        new java.net.URI("https://www.youtube.com/watch?v=" + videoId)
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        root.getChildren().addAll(title, openBtn);

        Scene scene = new Scene(root, 420, 220);
        popup.setScene(scene);
        popup.showAndWait();
    }    private String cleanIngredients(String ingredients) {
        if (ingredients == null) return "";

        StringBuilder clean = new StringBuilder();

        for (String line : ingredients.split("\\n")) {
            if (!line.trim().startsWith("ALLERGENS:")) {
                clean.append(line).append("\n");
            }
        }

        return clean.toString().trim();
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}