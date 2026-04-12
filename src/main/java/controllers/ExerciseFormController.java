package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import models.Exercise;
import services.ExerciseService;
import utils.WebAssets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Objects;

public class ExerciseFormController {

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

    private final ExerciseService exerciseService = new ExerciseService();
    private Exercise editingExercise;
    private File selectedImageFile;

    @FXML
    private void initialize() {
        typeField.setItems(FXCollections.observableArrayList(
                "Cardio", "Strength", "Flexibility", "HIIT", "Endurance", "Balance", "Other"));
    }

    // =========================
    // 📸 IMAGE
    // =========================
    @FXML
    private void onChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

        File file = chooser.showOpenDialog(nameField.getScene().getWindow());

        if (file != null) {
            selectedImageFile = file;
            selectedFileLabel.setText(file.getName());

            Image img = new Image(file.toURI().toString(), 160, 160, true, true);
            imagePreview.setImage(img);

            imagePreviewPane.setVisible(true);
            imagePreviewPane.setManaged(true);
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
        } else if (!name.matches("^[a-zA-Z0-9 ]+$")) {
            showError(nameError, "Only letters and numbers.");
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

        if (selectedImageFile != null) {
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

                exerciseService.update(editingExercise);
            }

            System.out.println("SUCCESS ✅");
            goBack();

        } catch (SQLException ex) {
            ex.printStackTrace(); // 🔥 IMPORTANT
            showError(nameError, "Database error!");
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
        durationField.setText(String.valueOf(e.getDuree()));
        setsField.setText(String.valueOf(e.getSets()));
        repsField.setText(String.valueOf(e.getReps()));
        descriptionField.setText(safe(e.getDescription()));

        if (e.getImageName() != null) {
            loadCurrentImage(e.getImageName());
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
            Path dir = Path.of("../FitsenseApp/public/uploads/exercises");

            if (!Files.exists(dir)) return file.getName();

            String name = System.currentTimeMillis() + "_" + file.getName();
            Files.copy(file.toPath(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);

            return name;

        } catch (IOException e) {
            return file.getName();
        }
    }
}