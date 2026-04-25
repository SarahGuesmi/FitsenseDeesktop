package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import models.Exercise;
import org.json.JSONArray;
import org.json.JSONObject;
import services.ExerciseService;
import utils.WebAssets;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Objects;

public class ExerciseFormController {

    private static final String YOUTUBE_API_KEY = "AIzaSyDA2wyzB2YTHI21DX0hkgLP28HEPzX2h58";

    @FXML private Label formTitle;
    @FXML private Button submitBtn;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeField;
    @FXML private TextField durationField;
    @FXML private TextField setsField;
    @FXML private TextField repsField;
    @FXML private TextArea descriptionField;

    @FXML private StackPane imagePreviewPane;
    @FXML private ImageView imagePreview;
    @FXML private Label currentImageLabel;
    @FXML private Label selectedFileLabel;
    @FXML private CheckBox deleteImageCheck;

    @FXML private Label nameError;
    @FXML private Label durationError;
    @FXML private Label typeError;
    @FXML private Label setsError;
    @FXML private Label repsError;

    // YouTube fields
    @FXML private TextField youtubeSearchField;
    @FXML private Label youtubeError;
    @FXML private VBox youtubeResultsBox;
    @FXML private HBox selectedVideoBox;
    @FXML private ImageView selectedVideoThumb;
    @FXML private Label selectedVideoTitle;
    @FXML private Label selectedVideoChannel;

    private String selectedVideoId = null;

    private final ExerciseService exerciseService = new ExerciseService();
    private Exercise editingExercise;
    private File selectedImageFile;
    private String selectedPidevassetFilename = null; // filename in pidevassets

    @FXML
    private void initialize() {
        typeField.setItems(FXCollections.observableArrayList(
                "Cardio", "Strength", "Flexibility", "HIIT", "Endurance", "Balance", "Other"));
    }

    // =========================
    // 🎬 YOUTUBE SEARCH
    // =========================
    @FXML
    private void onYoutubeSearch() {
        String query = youtubeSearchField.getText() == null ? "" : youtubeSearchField.getText().trim();
        if (query.isEmpty()) {
            showError(youtubeError, "Enter a search term.");
            return;
        }
        youtubeError.setVisible(false);
        youtubeError.setManaged(false);
        youtubeResultsBox.getChildren().clear();
        youtubeResultsBox.setVisible(false);
        youtubeResultsBox.setManaged(false);

        // Run in background thread
        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=4&q="
                        + encoded + "&key=" + YOUTUBE_API_KEY;

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

                JSONObject json = new JSONObject(resp.body());
                JSONArray items = json.optJSONArray("items");

                Platform.runLater(() -> {
                    if (items == null || items.isEmpty()) {
                        showError(youtubeError, "No results found.");
                        return;
                    }
                    youtubeResultsBox.getChildren().clear();
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        String videoId = item.getJSONObject("id").optString("videoId");
                        JSONObject snippet = item.getJSONObject("snippet");
                        String title = snippet.optString("title");
                        String channel = snippet.optString("channelTitle");
                        String thumb = snippet.getJSONObject("thumbnails").getJSONObject("default").optString("url");

                        HBox row = buildResultRow(videoId, title, channel, thumb);
                        youtubeResultsBox.getChildren().add(row);
                    }
                    youtubeResultsBox.setVisible(true);
                    youtubeResultsBox.setManaged(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(youtubeError, "Search failed: " + e.getMessage()));
            }
        }).start();
    }

    private HBox buildResultRow(String videoId, String title, String channel, String thumbUrl) {
        ImageView thumb = new ImageView();
        thumb.setFitWidth(100);
        thumb.setFitHeight(56);
        thumb.setPreserveRatio(true);
        try { thumb.setImage(new Image(thumbUrl, true)); } catch (Exception ignored) {}

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:700;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(340);

        Label channelLabel = new Label(channel);
        channelLabel.setStyle("-fx-text-fill:#6B7280;-fx-font-size:11px;");

        VBox info = new VBox(3, titleLabel, channelLabel);
        info.setMaxWidth(340);

        Button selectBtn = new Button("Select");
        selectBtn.setStyle("-fx-background-color:#22C55E;-fx-text-fill:black;-fx-font-weight:800;"
                + "-fx-background-radius:8;-fx-padding:6 14;-fx-cursor:hand;");
        selectBtn.setOnAction(e -> selectVideo(videoId, title, channel, thumbUrl));

        HBox row = new HBox(10, thumb, info, selectBtn);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:#111827;-fx-border-color:#1F2937;-fx-border-radius:8;"
                + "-fx-background-radius:8;-fx-padding:8;");
        return row;
    }

    private void selectVideo(String videoId, String title, String channel, String thumbUrl) {
        selectedVideoId = videoId;
        selectedVideoTitle.setText(title);
        selectedVideoChannel.setText(channel);
        try { selectedVideoThumb.setImage(new Image(thumbUrl, true)); } catch (Exception ignored) {}
        selectedVideoBox.setVisible(true);
        selectedVideoBox.setManaged(true);
        youtubeResultsBox.setVisible(false);
        youtubeResultsBox.setManaged(false);
    }

    @FXML
    private void onClearVideo() {
        selectedVideoId = null;
        selectedVideoBox.setVisible(false);
        selectedVideoBox.setManaged(false);
        selectedVideoThumb.setImage(null);
        selectedVideoTitle.setText("");
        selectedVideoChannel.setText("");
    }

    // =========================
    // 📸 IMAGE
    // =========================
    @FXML
    private void onChooseImage() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/fxml/ImagePickerView.fxml")));
            javafx.scene.Parent root = loader.load();
            ImagePickerController ctrl = loader.getController();

            javafx.stage.Stage pickerStage = new javafx.stage.Stage();
            pickerStage.setTitle("Choose Image from pidevassets");
            pickerStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            pickerStage.initOwner(nameField.getScene().getWindow());
            pickerStage.setScene(new javafx.scene.Scene(root));
            pickerStage.setResizable(false);

            ctrl.init(pickerStage, filename -> {
                // filename selected from pidevassets
                selectedImageFile = null; // no local file
                selectedFileLabel.setText(filename);
                // Store filename directly — it's already in pidevassets
                String url = "http://localhost/pidevassets/" + filename;
                try {
                    Image img = new Image(url, 160, 160, true, true, true);
                    imagePreview.setImage(img);
                    imagePreviewPane.setVisible(true);
                    imagePreviewPane.setManaged(true);
                } catch (Exception ignored) {}
                // Store the filename for saving
                selectedPidevassetFilename = filename;
            });

            pickerStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // ✅ SUBMIT
    // =========================
    @FXML
    private void onSubmit() {
        System.out.println("CLICK SAVE"); // 🔥 DEBUG

        clearErrors();
        boolean valid = true;

        String name = safe(nameField.getText()).trim();
        String type = typeField.getValue();
        String durationText = safe(durationField.getText()).trim();
        String setsText = safe(setsField.getText()).trim();
        String repsText = safe(repsField.getText()).trim();
        String description = safe(descriptionField.getText()).trim();

        // NAME
        if (name.isEmpty()) {
            showError(nameError, "Name is required.");
            valid = false;
        } else if (name.matches("^\\d+$")) {
            showError(nameError, "Cannot be only numbers.");
            valid = false;
        }

        // TYPE
        if (type == null || type.isBlank()) {
            showError(typeError, "Type required.");
            valid = false;
        }

        // DURATION
        Integer duration = null;
        if (durationText.isEmpty()) {
            showError(durationError, "Duration is required.");
            valid = false;
        } else {
            try {
                duration = Integer.parseInt(durationText);
                if (duration <= 0 || duration > 7200) {
                    showError(durationError, "Must be between 1 and 7200 sec.");
                    valid = false;
                }
            } catch (Exception e) {
                showError(durationError, "Must be a valid number.");
                valid = false;
            }
        }

        // SETS — optional
        Integer sets = null;
        if (!setsText.isEmpty()) {
            try {
                sets = Integer.parseInt(setsText);
                if (sets <= 0) { showError(setsError, "Must be > 0."); valid = false; }
            } catch (Exception e) {
                showError(setsError, "Must be a number."); valid = false;
            }
        }

        // REPS — optional
        Integer reps = null;
        if (!repsText.isEmpty()) {
            try {
                reps = Integer.parseInt(repsText);
                if (reps <= 0) { showError(repsError, "Must be > 0."); valid = false; }
            } catch (Exception e) {
                showError(repsError, "Must be a number."); valid = false;
            }
        }

        if (!valid) {
            System.out.println("VALIDATION FAILED ❌");
            return;
        }

        String imageName = (editingExercise != null) ? editingExercise.getImageName() : null;

        if (deleteImageCheck != null && deleteImageCheck.isSelected()) {
            imageName = null;
        }

        if (selectedPidevassetFilename != null) {
            imageName = selectedPidevassetFilename; // already in pidevassets
        } else if (selectedImageFile != null) {
            imageName = copyImageToUploads(selectedImageFile);
        }

        try {
            if (editingExercise == null) {
                System.out.println("CREATE MODE");

                Exercise e = new Exercise();
                e.setNom(name);
                e.setType(type);
                e.setDuree(duration);
                e.setSets(sets);
                e.setReps(reps);
                e.setDescription(description);
                e.setImageName(imageName);
                e.setYoutubeVideoId(selectedVideoId);

                exerciseService.createPrepared(e);

            } else {
                System.out.println("UPDATE MODE");

                editingExercise.setNom(name);
                editingExercise.setType(type);
                editingExercise.setDuree(duration);
                editingExercise.setSets(sets);
                editingExercise.setReps(reps);
                editingExercise.setDescription(description);
                editingExercise.setImageName(imageName);
                if (selectedVideoId != null) editingExercise.setYoutubeVideoId(selectedVideoId);

                exerciseService.update(editingExercise);
            }

            System.out.println("SUCCESS ✅");
            goBack();

        } catch (SQLException ex) {
            ex.printStackTrace();
            showError(nameError, "Database error: " + ex.getMessage());
        }
    }

    // =========================
    // 🖼 IMAGE LOAD
    // =========================
    private void loadCurrentImage(String imageName) {
        try {
            String url = WebAssets.assetUrl("uploads/exercises/" + imageName);
            Image img = new Image(url, 160, 160, true, true, true);

            imagePreview.setImage(img);
            imagePreviewPane.setVisible(true);
            imagePreviewPane.setManaged(true);

        } catch (Exception e) {
            imagePreviewPane.setStyle("-fx-background-color: gray;");
        }
    }

    // =========================
    // EDIT MODE
    // =========================
    public void setExercise(Exercise e) {
        this.editingExercise = e;

        if (e == null) return;

        formTitle.setText("Edit Exercise");
        submitBtn.setText("Update");

        nameField.setText(safe(e.getNom()));
        typeField.setValue(e.getType());
        durationField.setText(e.getDuree() != null ? String.valueOf(e.getDuree()) : "");
        setsField.setText(e.getSets() != null ? String.valueOf(e.getSets()) : "");
        repsField.setText(e.getReps() != null ? String.valueOf(e.getReps()) : "");
        descriptionField.setText(safe(e.getDescription()));

        if (e.getImageName() != null) {
            loadCurrentImage(e.getImageName());
        }

        if (e.getYoutubeVideoId() != null && !e.getYoutubeVideoId().isBlank()) {
            selectedVideoId = e.getYoutubeVideoId();
            selectedVideoTitle.setText("Current: https://youtu.be/" + selectedVideoId);
            selectedVideoBox.setVisible(true);
            selectedVideoBox.setManaged(true);
        }
    }

    // =========================
    // NAVIGATION
    // =========================
    @FXML private void onBack() { goBack(); }
    @FXML private void onCancel() { goBack(); }

    private void goBack() {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/fxml/ExerciseListFragment.fxml")));
            nameField.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // UTILS
    // =========================
    private void clearErrors() {
        for (Label l : new Label[]{nameError, durationError, typeError, setsError, repsError}) {
            l.setVisible(false);
            l.setManaged(false);
            l.setText("");
        }
    }

    private static void showError(Label l, String msg) {
        l.setText("⚠ " + msg);
        l.setVisible(true);
        l.setManaged(true);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private String copyImageToUploads(File file) {
        try {
            // Copy to XAMPP htdocs/pidevassets/ — accessible via http://localhost/pidevassets/
            Path dir = Path.of("C:/xampp/htdocs/pidevassets");

            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String name = System.currentTimeMillis() + "_" + file.getName();
            Files.copy(file.toPath(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            return name;

        } catch (IOException e) {
            System.err.println("copyImageToUploads error: " + e.getMessage());
            return file.getName();
        }
    }
}