package controllers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import models.RecetteNutritionnelle;
import models.User;
import services.RecetteFavoriService;
import services.RecetteNutritionnelleService;
import utils.SessionManager;
import javafx.scene.layout.Region;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecipeCatalogController {

    private static final String DEFAULT_IMAGE_URL =
            "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1200&q=60";

    @FXML
    private VBox formPane;
    @FXML
    private TilePane recipesGrid;
    @FXML
    private TextField titleField;
    @FXML
    private ComboBox<String> typeMealCombo;
    @FXML
    private TextField kcalField;
    @FXML
    private TextField proteinsField;
    @FXML
    private Label imageFileLabel;
    @FXML
    private ImageView formImagePreview;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextArea ingredientsArea;
    @FXML
    private TextArea preparationArea;
    @FXML
    private VBox weightLossCard;
    @FXML
    private VBox muscleGainCard;
    @FXML
    private VBox enduranceCard;
    @FXML
    private VBox wellBeingCard;
    @FXML private CheckBox peanutsCB;
    @FXML private CheckBox treeNutsCB;
    @FXML private CheckBox milkCB;
    @FXML private CheckBox eggsCB;
    @FXML private CheckBox glutenCB;
    @FXML private CheckBox soyCB;
    @FXML private CheckBox fishCB;
    @FXML private CheckBox shellfishCB;
    @FXML private CheckBox sesameCB;
    @FXML private CheckBox mustardCB;
    @FXML private CheckBox celeryCB;
    @FXML private CheckBox sulfitesCB;
    private final RecetteNutritionnelleService recetteService = new RecetteNutritionnelleService();

    private File selectedImageFile;
    private String selectedImageUrl = null;
    private boolean weightLossSelected = false;
    private boolean muscleGainSelected = false;
    private boolean enduranceSelected = false;
    private boolean wellBeingSelected = false;

    @FXML
    private void initialize() {
        if (typeMealCombo != null) {
            typeMealCombo.getItems().addAll("Breakfast", "Lunch", "Dinner", "Snack");
        }
        refreshObjectiveCards();
        refreshRecipesGrid();
    }

    @FXML
    private void onToggleForm() {
        if (formPane == null) return;

        boolean show = !formPane.isVisible();
        formPane.setVisible(show);
        formPane.setManaged(show);
    }

    private static final String IMAGES_BASE_URL = "http://localhost/images/";
    private static final String IMAGES_LOCAL_DIR = "C:/wamp64/www/images/";

    /**
     * Convertit une URL stockée (http://localhost/images/x ou file:///... )
     * en URL utilisable par JavaFX ImageView (file:/// local).
     * Si le fichier local n'existe pas, retourne DEFAULT_IMAGE_URL.
     */
    private String toDisplayUrl(String stored) {
        if (stored == null || stored.isBlank()) return DEFAULT_IMAGE_URL;

        // Déjà un file:// URI
        if (stored.startsWith("file:")) return stored;

        // http://localhost/images/fichier  →  file:///C:/wamp64/www/images/fichier
        if (stored.startsWith(IMAGES_BASE_URL)) {
            String fileName = stored.substring(IMAGES_BASE_URL.length())
                    .replace("%20", " ");
            File f = new File(IMAGES_LOCAL_DIR + fileName);
            if (f.exists()) return f.toURI().toString();
            return DEFAULT_IMAGE_URL;
        }

        // Chemin absolu Windows (ex: C:/wamp64/...)
        File f = new File(stored);
        if (f.exists()) return f.toURI().toString();

        return DEFAULT_IMAGE_URL;
    }

    @FXML
    private void onChooseImage() {
        String chosen = openImagePickerDialog(
                formPane != null && formPane.getScene() != null ? formPane.getScene().getWindow() : null,
                null
        );
        if (chosen != null) {
            selectedImageUrl = chosen;
            selectedImageFile = null;
            String fileName = chosen.substring(chosen.lastIndexOf('/') + 1);
            if (imageFileLabel != null) imageFileLabel.setText(fileName);
            if (formImagePreview != null) {
                formImagePreview.setImage(new Image(toDisplayUrl(chosen), true));
                formImagePreview.setVisible(true);
                formImagePreview.setManaged(true);
            }
        }
    }

    /**
     * Ouvre une fenêtre modale qui liste les images de localhost/images/
     * et retourne l'URL http://localhost/images/<fichier> de l'image choisie, ou null.
     */
    private String openImagePickerDialog(Window owner, Stage parentStage) {
        Stage dialog = new Stage();
        if (owner != null) dialog.initOwner(owner);
        else if (parentStage != null) dialog.initOwner(parentStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Choisir une image");
        dialog.setResizable(true);

        final String[] result = {null};

        // ── Header ──────────────────────────────────────────────────────────
        Label title = new Label("🖼 Choose an Image");
        title.setStyle("-fx-text-fill:white;-fx-font-size:18;-fx-font-weight:bold;");

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-font-size:16;-fx-cursor:hand;");
        closeBtn.setOnAction(e -> dialog.close());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox header = new HBox(12, title, headerSpacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 12, 0));

        // ── Toolbar ─────────────────────────────────────────────────────────
        TextField filterField = new TextField();
        filterField.setPromptText("Filter images...");
        filterField.setStyle("-fx-background-color:#1e293b;-fx-text-fill:white;-fx-prompt-text-fill:#64748b;-fx-border-color:#334155;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8 12;");
        HBox.setHgrow(filterField, Priority.ALWAYS);

        Button uploadBtn = new Button("+ Upload New");
        uploadBtn.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:8 16;-fx-cursor:hand;");

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.setStyle("-fx-background-color:#1e293b;-fx-text-fill:#94a3b8;-fx-border-color:#334155;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8 14;-fx-cursor:hand;");

        HBox toolbar = new HBox(10, filterField, uploadBtn, refreshBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // ── Images grid ─────────────────────────────────────────────────────
        TilePane grid = new TilePane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPrefColumns(4);
        grid.setPadding(new Insets(10, 0, 10, 0));

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color:transparent;-fx-background:#0f172a;");
        scrollPane.setPrefHeight(420);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Charger les images depuis le dossier local
        Runnable loadImages = () -> {
            grid.getChildren().clear();
            File dir = new File(IMAGES_LOCAL_DIR);
            String filter = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();

            if (!dir.exists() || !dir.isDirectory()) {
                Label noDir = new Label("Dossier introuvable : " + IMAGES_LOCAL_DIR);
                noDir.setStyle("-fx-text-fill:#ef4444;");
                grid.getChildren().add(noDir);
                return;
            }

            File[] files = dir.listFiles(f -> {
                String n = f.getName().toLowerCase();
                boolean isImage = n.endsWith(".jpg") || n.endsWith(".jpeg")
                        || n.endsWith(".png") || n.endsWith(".webp") || n.endsWith(".gif");
                boolean matchFilter = filter.isEmpty() || n.contains(filter);
                return f.isFile() && isImage && matchFilter;
            });

            if (files == null || files.length == 0) {
                Label empty = new Label("Aucune image trouvée.");
                empty.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:14;");
                grid.getChildren().add(empty);
                return;
            }

            for (File imgFile : files) {
                String url = IMAGES_BASE_URL + imgFile.getName().replace(" ", "%20");
                String localUrl = imgFile.toURI().toString(); // file:/// pour affichage JavaFX

                ImageView iv = new ImageView();
                try {
                    iv.setImage(new Image(localUrl, 140, 100, false, true, true));
                } catch (Exception ex) {
                    iv.setImage(new Image(DEFAULT_IMAGE_URL, 140, 100, false, true, true));
                }
                iv.setFitWidth(140);
                iv.setFitHeight(100);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);
                iv.setStyle("-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),6,0,0,2);");

                Label nameLabel = new Label(imgFile.getName());
                nameLabel.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:10;-fx-max-width:140;");
                nameLabel.setMaxWidth(140);
                nameLabel.setWrapText(false);
                nameLabel.setEllipsisString("…");

                VBox card = new VBox(6, iv, nameLabel);
                card.setAlignment(Pos.CENTER);
                card.setPadding(new Insets(8));
                card.setStyle("-fx-background-color:#1e293b;-fx-background-radius:8;-fx-cursor:hand;");

                // Hover effect
                card.setOnMouseEntered(e ->
                        card.setStyle("-fx-background-color:#334155;-fx-background-radius:8;-fx-cursor:hand;"));
                card.setOnMouseExited(e ->
                        card.setStyle("-fx-background-color:#1e293b;-fx-background-radius:8;-fx-cursor:hand;"));

                card.setOnMouseClicked(e -> {
                    result[0] = url;
                    dialog.close();
                });

                grid.getChildren().add(card);
            }
        };

        loadImages.run();

        filterField.textProperty().addListener((obs, o, n) -> loadImages.run());
        refreshBtn.setOnAction(e -> loadImages.run());

        // Upload : ouvre le FileChooser système pour copier une image dans le dossier
        uploadBtn.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Uploader une image");
            fc.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
            File chosen = fc.showOpenDialog(dialog);
            if (chosen != null) {
                try {
                    Files.createDirectories(Path.of(IMAGES_LOCAL_DIR));
                    String cleanName = chosen.getName().replaceAll("\\s+", "_");
                    Path dest = Path.of(IMAGES_LOCAL_DIR, cleanName);
                    Files.copy(chosen.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                    loadImages.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // ── Layout ──────────────────────────────────────────────────────────
        VBox root = new VBox(16, header, toolbar, scrollPane);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color:#0f172a;");

        Scene scene = new Scene(root, 720, 560);
        scene.setFill(Color.web("#0f172a"));
        dialog.setScene(scene);
        dialog.showAndWait();

        return result[0];
    }

    @FXML
    private void onToggleWeightLoss() {
        weightLossSelected = !weightLossSelected;
        refreshObjectiveCards();
    }

    @FXML
    private void onToggleMuscleGain() {
        muscleGainSelected = !muscleGainSelected;
        refreshObjectiveCards();
    }

    @FXML
    private void onToggleEndurance() {
        enduranceSelected = !enduranceSelected;
        refreshObjectiveCards();
    }

    @FXML
    private void onToggleWellBeing() {
        wellBeingSelected = !wellBeingSelected;
        refreshObjectiveCards();
    }

    @FXML
    private void onSaveRecipe() {
        try {
            String title = getText(titleField);
            String description = getText(descriptionArea);
            String ingredients = getText(ingredientsArea);
            String preparation = getText(preparationArea);
            String typeMeal = typeMealCombo != null ? typeMealCombo.getValue() : null;

            String kcalText = getText(kcalField);
            String proteinsText = getText(proteinsField);

            List<String> objectifs = getSelectedObjectifs();

            // 1) Vérifier les champs vides d'abord
            if (title.isBlank() || description.isBlank() || ingredients.isBlank()
                    || preparation.isBlank() || typeMeal == null
                    || kcalText.isBlank() || proteinsText.isBlank()
                    || objectifs.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation",
                        "Veuillez remplir tous les champs obligatoires.");
                return;
            }

            // 2) Vérifier que kcal et proteins sont des entiers
            Integer kcal = parseInteger(kcalText);
            Integer proteins = parseInteger(proteinsText);

            if (kcal == null) {
                showAlert(Alert.AlertType.ERROR, "Valeur invalide",
                        "Le champ Calories doit contenir un nombre entier.");
                return;
            }

            if (proteins == null) {
                showAlert(Alert.AlertType.ERROR, "Valeur invalide",
                        "Le champ Protéines doit contenir un nombre entier.");
                return;
            }

            List<String> selectedAllergens = getSelectedAllergens();
            if (!selectedAllergens.isEmpty()) {
                ingredients = ingredients + "\n\nALLERGENS: " + String.join(", ", selectedAllergens);
            }
            User currentUser = SessionManager.getCurrentUser();
            if (currentUser == null || currentUser.getId() == null) {
                showAlert(Alert.AlertType.ERROR, "Session", "Aucun coach connecté.");
                return;
            }

            RecetteNutritionnelle recette = new RecetteNutritionnelle();
            recette.setTitle(title);
            recette.setDescription(description);
            recette.setIngredients(ingredients);
            recette.setPreparation(preparation);
            recette.setTypeMeal(typeMeal);
            recette.setKcal(kcal);
            recette.setProteins(proteins);
            recette.setObjectifs(objectifs);
            recette.setCoachId(currentUser.getId().toString());

            if (selectedImageFile != null) {
                String imageUrl = saveImageToWamp(selectedImageFile);
                recette.setImage(imageUrl);
            } else if (selectedImageUrl != null && !selectedImageUrl.isBlank()) {
                recette.setImage(selectedImageUrl);
            } else {
                recette.setImage(null);
            }

            recetteService.createPrepared(recette);

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Recette enregistrée avec succès.");

            clearForm();
            refreshRecipesGrid();
            clearAllergenCheckboxes();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'enregistrer la recette.");
        }
    }
    private void clearAllergenCheckboxes() {
        CheckBox[] boxes = {
                peanutsCB, treeNutsCB, milkCB, eggsCB,
                glutenCB, soyCB, fishCB, shellfishCB,
                sesameCB, mustardCB, celeryCB, sulfitesCB
        };

        for (CheckBox box : boxes) {
            if (box != null) box.setSelected(false);
        }
    }
    private void refreshObjectiveCards() {
        setCardSelected(weightLossCard, weightLossSelected);
        setCardSelected(muscleGainCard, muscleGainSelected);
        setCardSelected(enduranceCard, enduranceSelected);
        setCardSelected(wellBeingCard, wellBeingSelected);
    }

    private void setCardSelected(VBox card, boolean selected) {
        if (card == null) return;
        card.getStyleClass().remove("objective-card-selected");
        if (selected) {
            card.getStyleClass().add("objective-card-selected");
        }
    }

    private List<String> getSelectedObjectifs() {
        List<String> objectifs = new ArrayList<>();
        if (weightLossSelected) objectifs.add("WEIGHT_LOSS");
        if (muscleGainSelected) objectifs.add("MUSCLE_GAIN");
        if (enduranceSelected) objectifs.add("ENDURANCE");
        if (wellBeingSelected) objectifs.add("WELL_BEING");
        return objectifs;
    }

    private void refreshRecipesGrid() {
        try {
            if (recipesGrid == null) return;

            recipesGrid.getChildren().clear();
            List<RecetteNutritionnelle> recipes = recetteService.read();

            for (RecetteNutritionnelle recipe : recipes) {
                recipesGrid.getChildren().add(createRecipeCard(recipe));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private VBox createRecipeCard(RecetteNutritionnelle recipe) {

        VBox card = new VBox(12);
        card.getStyleClass().add("recipe-card");
        card.setPrefWidth(300);

        ImageView imageView = new ImageView();

        String imageUrl = (recipe.getImage() == null || recipe.getImage().isBlank())
                ? DEFAULT_IMAGE_URL
                : toDisplayUrl(recipe.getImage());

        Image img = new Image(imageUrl, false);

        if (img.isError()) {
            img = new Image(DEFAULT_IMAGE_URL, false);
        }

        imageView.setImage(img);
        imageView.setFitWidth(300);
        imageView.setFitHeight(170);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);

        StackPane imageContainer = new StackPane(imageView);

        User currentUser = SessionManager.getCurrentUser();

        if (currentUser != null
                && currentUser.getRolesJson() != null
                && currentUser.getRolesJson().contains("ROLE_USER")) {

            RecetteFavoriService favoriService = new RecetteFavoriService();

            // Vérifier l'état initial du favori
            boolean isFav = false;
            try {
                isFav = favoriService.isFavorite(
                        currentUser.getId().toString(),
                        recipe.getId().toString()
                );
            } catch (Exception ignored) {}

            Button heartBtn = new Button(isFav ? "♥" : "♡");
            heartBtn.getStyleClass().add("heart-btn");
            if (isFav) heartBtn.getStyleClass().add("heart-btn-liked");

            StackPane.setAlignment(heartBtn, Pos.TOP_RIGHT);
            StackPane.setMargin(heartBtn, new Insets(10));

            heartBtn.setOnAction(e -> {
                e.consume();
                try {
                    favoriService.toggleFavorite(
                            currentUser.getId().toString(),
                            recipe.getId().toString()
                    );
                    if ("♡".equals(heartBtn.getText())) {
                        heartBtn.setText("♥");
                        heartBtn.getStyleClass().add("heart-btn-liked");
                    } else {
                        heartBtn.setText("♡");
                        heartBtn.getStyleClass().remove("heart-btn-liked");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            // Empêcher le clic sur le cœur d'ouvrir les détails
            heartBtn.setOnMouseClicked(e -> e.consume());

            imageContainer.getChildren().add(heartBtn);
        }

        Label typeBadge = new Label(
                recipe.getTypeMeal() == null || recipe.getTypeMeal().isBlank()
                        ? "MEAL"
                        : recipe.getTypeMeal().toUpperCase()
        );
        typeBadge.getStyleClass().add("recipe-type-badge");

        Label titleLabel = new Label(safe(recipe.getTitle()));
        titleLabel.getStyleClass().add("recipe-card-title");
        titleLabel.setWrapText(true);

        Label descLabel = new Label(
                recipe.getDescription() == null || recipe.getDescription().isBlank()
                        ? "A delicious and healthy meal choice for your fitness journey."
                        : recipe.getDescription()
        );
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("recipe-card-desc");

        HBox statsBox = new HBox(10);

        Label kcalLabel = new Label("🔥 " + recipe.getKcal() + " KCAL");
        kcalLabel.getStyleClass().add("recipe-kcal-chip");

        Label protLabel = new Label("💪 " + recipe.getProteins() + " g");
        protLabel.getStyleClass().add("recipe-prot-chip");

        statsBox.getChildren().addAll(kcalLabel, protLabel);

        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(0, 14, 14, 14));
        contentBox.getChildren().addAll(typeBadge, titleLabel, descLabel, statsBox);

        card.getChildren().addAll(imageContainer, contentBox);
        card.setOnMouseClicked(event -> {
            // Ne pas ouvrir les détails si le clic vient du bouton cœur
            if (event.getTarget() instanceof Button) return;
            openRecipeDetails(recipe);
        });

        return card;
    }    @FXML
    private TextField searchField;
    @FXML
    private void onSearchRecipes() {
        filterRecipesByName();
    }

    @FXML
    private void onResetSearch() {
        if (searchField != null) {
            searchField.clear();
        }
        refreshRecipesGrid();
    }
    private String extractAllergens(String ingredients) {
        if (ingredients == null) return "";

        for (String line : ingredients.split("\\n")) {
            if (line.startsWith("ALLERGENS:")) {
                return line.replace("ALLERGENS:", "").trim();
            }
        }

        return "";
    }

    private void filterRecipesByName() {
        try {
            if (recipesGrid == null) return;

            String keyword = searchField == null ? "" : searchField.getText().trim().toLowerCase();

            recipesGrid.getChildren().clear();

            List<RecetteNutritionnelle> recipes = recetteService.read();

            for (RecetteNutritionnelle recipe : recipes) {
                String title = recipe.getTitle() == null ? "" : recipe.getTitle().toLowerCase();

                if (keyword.isEmpty() || title.contains(keyword)) {
                    recipesGrid.getChildren().add(createRecipeCard(recipe));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Search Error",
                    "Impossible de filtrer les recettes.");
        }
    }

    private void openRecipeDetails(RecetteNutritionnelle recipe) {
        Stage modal = buildBaseModal();

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("recipe-modal-card");

        rebuildDetailsView(root, modal, recipe);
        showModal(modal, root);
    }

    private void rebuildDetailsView(VBox root, Stage modal, RecetteNutritionnelle recipe) {
        root.getChildren().clear();

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(safe(recipe.getTitle()));
        title.getStyleClass().add("recipe-modal-title");
        title.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("recipe-modal-close-btn");
        closeBtn.setOnAction(e -> modal.close());

        header.getChildren().addAll(title, spacer, closeBtn);

        HBox badges = new HBox(12);
        badges.getChildren().addAll(
                createBadge("🔥 " + safe(recipe.getKcal()) + " kcal", "recipe-badge-blue"),
                createBadge("💪 " + safe(recipe.getProteins()) + " g protéines", "recipe-badge-purple"),
                createBadge("🍽 " + safe(recipe.getTypeMeal()).toUpperCase(), "recipe-badge-gray")
        );

        HBox objectifsBox = new HBox(10);
        if (recipe.getObjectifs() != null) {
            for (String obj : recipe.getObjectifs()) {
                Label chip = new Label(obj.replace("_", " "));
                chip.getStyleClass().add("recipe-goal-chip");
                objectifsBox.getChildren().add(chip);
            }
        }

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
            allergyText.setMaxWidth(860);
            allergyText.getStyleClass().add("allergy-text");

            allergyBox.getChildren().addAll(allergyTitle, allergyText);
        }

        HBox content = new HBox(35);
        content.setAlignment(Pos.TOP_LEFT);

        ImageView imageView = new ImageView();
        try {
            imageView.setImage(new Image(resolveImage(recipe), true));
        } catch (Exception e) {
            imageView.setImage(new Image(DEFAULT_IMAGE_URL, true));
        }

        imageView.setFitWidth(360);
        imageView.setFitHeight(260);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("recipe-modal-image");

        VBox rightContent = new VBox(18);
        HBox.setHgrow(rightContent, Priority.ALWAYS);

        Label descTitle = sectionTitle("DESCRIPTION");
        Label descValue = sectionText(safe(recipe.getDescription()));
        descValue.setMaxWidth(520);
        descValue.setWrapText(true);

        Label ingTitle = sectionTitle("📋 Ingredients");
        Label ingValue = sectionText(cleanIngredientsForDetails(recipe.getIngredients()));
        ingValue.setMaxWidth(520);
        ingValue.setWrapText(true);

        Label prepTitle = sectionTitle("👨‍🍳 Preparation");
        Label prepValue = sectionText(safe(recipe.getPreparation()));
        prepValue.setMaxWidth(520);
        prepValue.setWrapText(true);

        rightContent.getChildren().addAll(
                descTitle, descValue,
                ingTitle, ingValue,
                prepTitle, prepValue
        );

        content.getChildren().addAll(imageView, rightContent);

        Region pushFooter = new Region();
        VBox.setVgrow(pushFooter, Priority.ALWAYS);

        HBox footer = new HBox(14);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("recipe-modal-footer");

        Button editBtn = new Button("✎ Edit");
        editBtn.getStyleClass().add("recipe-edit-btn");

        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.getStyleClass().add("recipe-delete-btn");

        editBtn.setOnAction(e -> switchToEditMode(root, modal, recipe));

        deleteBtn.setOnAction(e -> {
            try {
                recetteService.delete(recipe);
                refreshRecipesGrid();
                modal.close();
                showAlert(Alert.AlertType.INFORMATION, "Deleted", "Recipe deleted successfully.");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Impossible de supprimer la recette.");
            }
        });

        footer.getChildren().addAll(editBtn, deleteBtn);

        root.getChildren().addAll(
                header,
                badges,
                objectifsBox,
                allergyBox,
                content,
                pushFooter,
                footer
        );
    }
    private String cleanIngredientsForDetails(String ingredients) {
        if (ingredients == null || ingredients.isBlank()) return "";

        StringBuilder clean = new StringBuilder();

        for (String line : ingredients.split("\\n")) {
            if (!line.trim().startsWith("ALLERGENS:")) {
                clean.append(line).append("\n");
            }
        }

        return clean.toString().trim();
    }
    private void switchToEditMode(VBox root, Stage modal, RecetteNutritionnelle recipe) {
        root.getChildren().clear();

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label formTitle = new Label("Edit Recipe");
        formTitle.getStyleClass().add("recipe-modal-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("recipe-modal-close-btn");
        closeBtn.setOnAction(e -> modal.close());

        header.getChildren().addAll(formTitle, spacer, closeBtn);

        TextField titleEdit = new TextField(safe(recipe.getTitle()));
        titleEdit.getStyleClass().add("recipe-modal-input");

        TextField kcalEdit = new TextField(recipe.getKcal() == null ? "" : String.valueOf(recipe.getKcal()));
        kcalEdit.getStyleClass().add("recipe-modal-input");

        TextField proteinsEdit = new TextField(recipe.getProteins() == null ? "" : String.valueOf(recipe.getProteins()));
        proteinsEdit.getStyleClass().add("recipe-modal-input");

        ComboBox<String> typeMealEdit = new ComboBox<>();
        typeMealEdit.getItems().addAll("Breakfast", "Lunch", "Dinner", "Snack");
        typeMealEdit.setValue(safe(recipe.getTypeMeal()));
        typeMealEdit.getStyleClass().add("recipe-modal-combo");

        HBox badges = new HBox(12);
        Label kcalBadge = createBadge("🔥 " + safe(recipe.getKcal()) + " kcal", "recipe-badge-blue");
        Label protBadge = createBadge("💪 " + safe(recipe.getProteins()) + " g protéines", "recipe-badge-purple");
        Label mealBadge = createBadge("🍽 " + safe(recipe.getTypeMeal()).toUpperCase(), "recipe-badge-gray");
        badges.getChildren().addAll(kcalBadge, protBadge, mealBadge);

        kcalEdit.textProperty().addListener((obs, oldV, newV) ->
                kcalBadge.setText("🔥 " + (newV == null || newV.isBlank() ? "0" : newV) + " kcal"));

        proteinsEdit.textProperty().addListener((obs, oldV, newV) ->
                protBadge.setText("💪 " + (newV == null || newV.isBlank() ? "0" : newV) + " g protéines"));

        typeMealEdit.valueProperty().addListener((obs, oldV, newV) ->
                mealBadge.setText("🍽 " + (newV == null ? "" : newV.toUpperCase())));

        GridPane topForm = new GridPane();
        topForm.setHgap(16);
        topForm.setVgap(14);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        topForm.getColumnConstraints().addAll(c1, c2);

        topForm.add(formField("Title", titleEdit), 0, 0, 2, 1);
        topForm.add(formField("Kcal", kcalEdit), 0, 1);
        topForm.add(formField("Proteins (g)", proteinsEdit), 1, 1);
        topForm.add(formField("Meal Type", typeMealEdit), 0, 2, 2, 1);

        VBox leftBox = new VBox(12);

        ImageView imageView = new ImageView();
        try {
            imageView.setImage(new Image(resolveImage(recipe), true));
        } catch (Exception e) {
            imageView.setImage(new Image(DEFAULT_IMAGE_URL, true));
        }

        imageView.setFitWidth(300);
        imageView.setFitHeight(240);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("recipe-modal-image");

        Button chooseImageBtn = new Button("Choose Image");
        chooseImageBtn.getStyleClass().add("recipe-file-btn");

        Label imageLabel = new Label(
                recipe.getImage() == null || recipe.getImage().isBlank()
                        ? "No file selected"
                        : "Current image"
        );
        imageLabel.getStyleClass().add("recipe-file-label");

        final String[] editImageUrl = {
                recipe.getImage() == null || recipe.getImage().isBlank() ? null : recipe.getImage()
        };

        chooseImageBtn.setOnAction(e -> {
            String chosen = openImagePickerDialog(null, modal);
            if (chosen != null) {
                editImageUrl[0] = chosen;
                String fileName = chosen.substring(chosen.lastIndexOf('/') + 1);
                imageLabel.setText(fileName);
                try {
                    imageView.setImage(new Image(toDisplayUrl(chosen), true));
                } catch (Exception ignored) {
                }
            }
        });

        leftBox.getChildren().addAll(imageView, chooseImageBtn, imageLabel);

        TextArea descEdit = new TextArea(safe(recipe.getDescription()));
        descEdit.setWrapText(true);
        descEdit.setPrefHeight(110);
        descEdit.getStyleClass().add("recipe-modal-textarea");

        TextArea ingEdit = new TextArea(cleanIngredientsForDetails(recipe.getIngredients()));
        ingEdit.setWrapText(true);
        ingEdit.setPrefHeight(120);
        ingEdit.getStyleClass().add("recipe-modal-textarea");

        TextArea prepEdit = new TextArea(safe(recipe.getPreparation()));
        prepEdit.setWrapText(true);
        prepEdit.setPrefHeight(120);
        prepEdit.getStyleClass().add("recipe-modal-textarea");

        String currentAllergens = extractAllergens(recipe.getIngredients());

        CheckBox editPeanutsCB = new CheckBox("🥜 Peanuts");
        CheckBox editTreeNutsCB = new CheckBox("🌰 Tree nuts");
        CheckBox editMilkCB = new CheckBox("🥛 Milk");
        CheckBox editEggsCB = new CheckBox("🥚 Eggs");
        CheckBox editGlutenCB = new CheckBox("🌾 Wheat / Gluten");
        CheckBox editSoyCB = new CheckBox("🟢 Soy");
        CheckBox editFishCB = new CheckBox("🐟 Fish");
        CheckBox editShellfishCB = new CheckBox("🦐 Shellfish");
        CheckBox editSesameCB = new CheckBox("🌱 Sesame");
        CheckBox editMustardCB = new CheckBox("🧂 Mustard");
        CheckBox editCeleryCB = new CheckBox("🥬 Celery");
        CheckBox editSulfitesCB = new CheckBox("⚗ Sulfites");

        CheckBox[] editAllergenBoxes = {
                editPeanutsCB, editTreeNutsCB, editMilkCB, editEggsCB,
                editGlutenCB, editSoyCB, editFishCB, editShellfishCB,
                editSesameCB, editMustardCB, editCeleryCB, editSulfitesCB
        };

        for (CheckBox cb : editAllergenBoxes) {
            cb.getStyleClass().add("allergen-checkbox");
            String label = cb.getText().replaceAll("^[^A-Za-z]+", "").trim();
            cb.setSelected(currentAllergens.contains(label));
        }

        TilePane editAllergensPane = new TilePane();
        editAllergensPane.setHgap(12);
        editAllergensPane.setVgap(12);
        editAllergensPane.setPrefColumns(3);
        editAllergensPane.getChildren().addAll(editAllergenBoxes);

        VBox rightBox = new VBox(14,
                sectionTitle("DESCRIPTION"), descEdit,
                sectionTitle("📋 Ingredients"), ingEdit,
                sectionTitle("👨‍🍳 Preparation"), prepEdit,
                sectionTitle("⚠ Allergens"), editAllergensPane
        );
        HBox.setHgrow(rightBox, Priority.ALWAYS);

        HBox content = new HBox(30, leftBox, rightBox);
        content.setAlignment(Pos.TOP_LEFT);

        Region pushFooter = new Region();
        VBox.setVgrow(pushFooter, Priority.ALWAYS);

        HBox footer = new HBox(16);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("recipe-modal-footer");

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("recipe-edit-btn");

        Button saveBtn = new Button("✔ Save Changes");
        saveBtn.getStyleClass().add("recipe-save-btn");

        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.getStyleClass().add("recipe-delete-btn");

        backBtn.setOnAction(e -> rebuildDetailsView(root, modal, recipe));

        saveBtn.setOnAction(e -> {
            try {
                String titleValue = titleEdit.getText() == null ? "" : titleEdit.getText().trim();
                String kcalValue = kcalEdit.getText() == null ? "" : kcalEdit.getText().trim();
                String proteinsValue = proteinsEdit.getText() == null ? "" : proteinsEdit.getText().trim();
                String mealTypeValue = typeMealEdit.getValue();

                if (titleValue.isEmpty() || kcalValue.isEmpty() || mealTypeValue == null) {
                    showAlert(Alert.AlertType.WARNING, "Missing Information",
                            "Please fill in Title, Meal Type and Kcal.");
                    return;
                }

                int kcalInt = Integer.parseInt(kcalValue);
                int proteinsInt = proteinsValue.isEmpty() ? 0 : Integer.parseInt(proteinsValue);

                String cleanIngredients = cleanIngredientsForDetails(ingEdit.getText());

                List<String> selectedAllergens = new ArrayList<>();
                for (CheckBox cb : editAllergenBoxes) {
                    if (cb.isSelected()) {
                        selectedAllergens.add(cb.getText().replaceAll("^[^A-Za-z]+", "").trim());
                    }
                }

                if (!selectedAllergens.isEmpty()) {
                    cleanIngredients += "\n\nALLERGENS: " + String.join(", ", selectedAllergens);
                }

                recipe.setTitle(titleValue);
                recipe.setKcal(kcalInt);
                recipe.setProteins(proteinsInt);
                recipe.setTypeMeal(mealTypeValue);
                recipe.setDescription(descEdit.getText());
                recipe.setIngredients(cleanIngredients);
                recipe.setPreparation(prepEdit.getText());

                if (editImageUrl[0] != null) {
                    recipe.setImage(editImageUrl[0]);
                }

                recetteService.update(recipe);
                refreshRecipesGrid();
                rebuildDetailsView(root, modal, recipe);

                showAlert(Alert.AlertType.INFORMATION, "Updated", "Recipe updated successfully.");

            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Invalid Number",
                        "Calories and Proteins must be numbers.");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Impossible de mettre à jour la recette.");
            }
        });

        deleteBtn.setOnAction(e -> {
            try {
                recetteService.delete(recipe);
                refreshRecipesGrid();
                modal.close();
                showAlert(Alert.AlertType.INFORMATION, "Deleted", "Recipe deleted successfully.");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Impossible de supprimer la recette.");
            }
        });

        footer.getChildren().addAll(backBtn, saveBtn, deleteBtn);

        root.getChildren().addAll(header, topForm, badges, content, pushFooter, footer);
    }
    private VBox formField(String labelText, Control field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("recipe-field-label");

        VBox box = new VBox(8);
        box.getChildren().addAll(label, field);
        return box;
    }

    private Label createBadge(String text, String extraClass) {
        Label badge = new Label(text);
        badge.getStyleClass().add("recipe-badge");
        badge.getStyleClass().add(extraClass);
        return badge;
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

    private Stage buildBaseModal() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Recipe");
        modal.setResizable(false);
        return modal;
    }

    private void showModal(Stage modal, VBox content) {
        content.setMaxWidth(980);
        content.setPrefWidth(980);
        content.setMaxHeight(Region.USE_COMPUTED_SIZE);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("recipe-modal-scroll");
        scroll.setPrefViewportWidth(980);
        scroll.setPrefViewportHeight(720);

        StackPane wrapper = new StackPane(scroll);
        wrapper.getStyleClass().add("recipe-modal-overlay");
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(30));

        Scene scene = new Scene(wrapper, 1100, 820);
        scene.setFill(Color.TRANSPARENT);

        String css = getClass().getResource("/css/recette.css").toExternalForm();
        scene.getStylesheets().add(css);

        modal.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        modal.setScene(scene);
        modal.showAndWait();
    }
    private String resolveImage(RecetteNutritionnelle recipe) {
        return toDisplayUrl(recipe.getImage());
    }

    private String formatMultiline(String text) {
        if (text == null || text.isBlank()) return "";
        return text;
    }

    private void clearForm() {
        if (titleField != null) titleField.clear();
        if (typeMealCombo != null) typeMealCombo.getSelectionModel().clearSelection();
        if (kcalField != null) kcalField.clear();
        if (proteinsField != null) proteinsField.clear();
        if (descriptionArea != null) descriptionArea.clear();
        if (ingredientsArea != null) ingredientsArea.clear();
        if (preparationArea != null) preparationArea.clear();

        selectedImageFile = null;
        selectedImageUrl = null;

        if (formImagePreview != null) {
            formImagePreview.setImage(null);
            formImagePreview.setVisible(false);
            formImagePreview.setManaged(false);
        }

        if (imageFileLabel != null) {
            imageFileLabel.setText("Aucun fichier n’a été sélectionné");
        }

        weightLossSelected = false;
        muscleGainSelected = false;
        enduranceSelected = false;
        wellBeingSelected = false;
        refreshObjectiveCards();

        if (formPane != null) {
            formPane.setVisible(false);
            formPane.setManaged(false);
        }
    }

    private String getText(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private String getText(TextArea area) {
        return area == null || area.getText() == null ? "" : area.getText().trim();
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Integer parseInteger(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private List<String> getSelectedAllergens() {
        List<String> allergens = new ArrayList<>();

        if (peanutsCB != null && peanutsCB.isSelected()) allergens.add("Peanuts");
        if (treeNutsCB != null && treeNutsCB.isSelected()) allergens.add("Tree nuts");
        if (milkCB != null && milkCB.isSelected()) allergens.add("Milk");
        if (eggsCB != null && eggsCB.isSelected()) allergens.add("Eggs");
        if (glutenCB != null && glutenCB.isSelected()) allergens.add("Wheat / Gluten");
        if (soyCB != null && soyCB.isSelected()) allergens.add("Soy");
        if (fishCB != null && fishCB.isSelected()) allergens.add("Fish");
        if (shellfishCB != null && shellfishCB.isSelected()) allergens.add("Shellfish");
        if (sesameCB != null && sesameCB.isSelected()) allergens.add("Sesame");
        if (mustardCB != null && mustardCB.isSelected()) allergens.add("Mustard");
        if (celeryCB != null && celeryCB.isSelected()) allergens.add("Celery");
        if (sulfitesCB != null && sulfitesCB.isSelected()) allergens.add("Sulfites");

        return allergens;
    }


    private String saveImageToWamp(File file) throws Exception {
        String uploadDir = "C:/wamp64/www/images/";
        Files.createDirectories(Path.of(uploadDir));
        String cleanName = file.getName().replaceAll("\\s+", "_");
        String fileName = System.currentTimeMillis() + "_" + cleanName;
        Path destination = Path.of(uploadDir, fileName);
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        return IMAGES_BASE_URL + fileName;
    }
}