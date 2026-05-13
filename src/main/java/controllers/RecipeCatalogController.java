package controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import models.RecetteNutritionnelle;
import models.User;
import services.RecetteNutritionnelleService;
import utils.SessionManager;

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

    private final RecetteNutritionnelleService recetteService = new RecetteNutritionnelleService();

    private File selectedImageFile;
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

    @FXML
    private void onChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        // Open directly in the pidevassets folder
        File assetsDir = new File("C:/xampp2/htdocs/pidevassets");
        if (assetsDir.exists() && assetsDir.isDirectory()) {
            fileChooser.setInitialDirectory(assetsDir);
        }

        Window window = formPane != null && formPane.getScene() != null
                ? formPane.getScene().getWindow()
                : null;

        File file = fileChooser.showOpenDialog(window);

        if (file != null) {
            selectedImageFile = file;
            if (imageFileLabel != null) {
                imageFileLabel.setText(file.getName());
            }
        }
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
                recette.setImage(selectedImageFile.toURI().toString());
            } else {
                recette.setImage(null);
            }

            recetteService.createPrepared(recette);

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Recette enregistrée avec succès.");

            clearForm();
            refreshRecipesGrid();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'enregistrer la recette.");
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

        String imageUrl = (recipe.getImage() == null || recipe.getImage().isBlank())
                ? DEFAULT_IMAGE_URL
                : recipe.getImage();

        ImageView imageView = new ImageView();
        try {
            imageView.setImage(new Image(imageUrl, true));
        } catch (Exception e) {
            imageView.setImage(new Image(DEFAULT_IMAGE_URL, true));
        }

        imageView.setFitWidth(300);
        imageView.setFitHeight(170);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);

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

        card.getChildren().addAll(imageView, contentBox);
        card.setOnMouseClicked(event -> openRecipeDetails(recipe));

        return card;
    }
    @FXML
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

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        // Add sport-hero.png image next to title
        ImageView titleImage = new ImageView();
        try {
            File logoFile = new File("C:/xampp2/htdocs/pidevassets/sport-hero.png");
            if (logoFile.exists()) {
                titleImage.setImage(new Image(logoFile.toURI().toString(), true));
            } else {
                // Fallback to recipe image if logo not found
                titleImage.setImage(new Image(resolveImage(recipe), true));
            }
        } catch (Exception e) {
            titleImage.setImage(new Image(DEFAULT_IMAGE_URL, true));
        }
        titleImage.setFitWidth(50);
        titleImage.setFitHeight(50);
        titleImage.setPreserveRatio(true);
        titleImage.setSmooth(true);
        titleImage.getStyleClass().add("recipe-title-image");

        Label title = new Label(safe(recipe.getTitle()));
        title.getStyleClass().add("recipe-modal-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("recipe-modal-close-btn");
        closeBtn.setOnAction(e -> modal.close());

        header.getChildren().addAll(titleImage, title, spacer, closeBtn);

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

        HBox content = new HBox(35);
        content.setAlignment(Pos.TOP_LEFT);

        ImageView imageView = new ImageView();
        try {
            imageView.setImage(new Image(resolveImage(recipe), true));
        } catch (Exception e) {
            imageView.setImage(new Image(DEFAULT_IMAGE_URL, true));
        }
        imageView.setFitWidth(650);
        imageView.setFitHeight(480);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("recipe-modal-image");

        VBox rightContent = new VBox(18);
        HBox.setHgrow(rightContent, Priority.ALWAYS);

        Label descTitle = sectionTitle("DESCRIPTION");
        Label descValue = sectionText(safe(recipe.getDescription()));

        Label ingTitle = sectionTitle("📋 Ingredients");
        Label ingValue = sectionText(formatMultiline(recipe.getIngredients()));

        Label prepTitle = sectionTitle("👨‍🍳 Preparation");
        Label prepValue = sectionText(formatMultiline(recipe.getPreparation()));

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

        root.getChildren().addAll(header, badges, objectifsBox, content, pushFooter, footer);
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

        final File[] editImageFile = new File[1];

        chooseImageBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choisir une image");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
            );
            // Open directly in the pidevassets folder
            File assetsDir = new File("C:/xampp2/htdocs/pidevassets");
            if (assetsDir.exists() && assetsDir.isDirectory()) {
                fileChooser.setInitialDirectory(assetsDir);
            }
            File file = fileChooser.showOpenDialog(modal);
            if (file != null) {
                editImageFile[0] = file;
                imageLabel.setText(file.getName());
                try {
                    imageView.setImage(new Image(file.toURI().toString(), true));
                } catch (Exception ignored) {
                }
            }
        });

        leftBox.getChildren().addAll(imageView, chooseImageBtn, imageLabel);

        TextArea descEdit = new TextArea(safe(recipe.getDescription()));
        descEdit.setWrapText(true);
        descEdit.setPrefHeight(110);
        descEdit.getStyleClass().add("recipe-modal-textarea");

        TextArea ingEdit = new TextArea(safe(recipe.getIngredients()));
        ingEdit.setWrapText(true);
        ingEdit.setPrefHeight(120);
        ingEdit.getStyleClass().add("recipe-modal-textarea");

        TextArea prepEdit = new TextArea(safe(recipe.getPreparation()));
        prepEdit.setWrapText(true);
        prepEdit.setPrefHeight(120);
        prepEdit.getStyleClass().add("recipe-modal-textarea");

        VBox rightBox = new VBox(14,
                sectionTitle("DESCRIPTION"), descEdit,
                sectionTitle("📋 Ingredients"), ingEdit,
                sectionTitle("👨‍🍳 Preparation"), prepEdit
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

                recipe.setTitle(titleValue);
                recipe.setKcal(kcalInt);
                recipe.setProteins(proteinsInt);
                recipe.setTypeMeal(mealTypeValue);
                recipe.setDescription(descEdit.getText());
                recipe.setIngredients(ingEdit.getText());
                recipe.setPreparation(prepEdit.getText());

                if (editImageFile[0] != null) {
                    recipe.setImage(editImageFile[0].toURI().toString());
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
        utils.AppIconLoader.setIcon(modal);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Recipe");
        modal.setResizable(false);
        return modal;
    }

    private void showModal(Stage modal, VBox content) {
        content.setMaxWidth(1600);
        content.setPrefWidth(1600);
        content.setMaxHeight(900);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("recipe-modal-scroll");
        scroll.setMaxWidth(1700);
        scroll.setPrefViewportWidth(1700);
        scroll.setPrefViewportHeight(950);

        StackPane wrapper = new StackPane(scroll);
        wrapper.getStyleClass().add("recipe-modal-overlay");
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(30));

        Scene scene = new Scene(wrapper, 1800, 1000);
        content.setMaxWidth(1700);
        scene.setFill(Color.TRANSPARENT);

        String css = getClass().getResource("/css/recette.css").toExternalForm();
        scene.getStylesheets().add(css);

        modal.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        modal.setScene(scene);
        modal.showAndWait();
    }

    private String resolveImage(RecetteNutritionnelle recipe) {
        return (recipe.getImage() == null || recipe.getImage().isBlank())
                ? DEFAULT_IMAGE_URL
                : recipe.getImage();
    }

    private String formatMultiline(String text) {
        if (text == null || text.isBlank()) return "";
        return text.replace(",", "\n");
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
}