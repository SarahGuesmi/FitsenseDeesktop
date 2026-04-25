package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.Workout;
import services.ExerciseRatingService;
import services.WorkoutService;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class WorkoutCatalogController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> levelFilter;
    @FXML private Label counterLabel;
    @FXML private VBox workoutListBox;

    private final WorkoutService workoutService = new WorkoutService();
    private final ExerciseRatingService ratingService = new ExerciseRatingService();
    private List<Workout> allWorkouts;
    private Workout pendingDelete;

    // Delete modal
    @FXML private StackPane deleteOverlay;
    @FXML private Label deleteConfirmLabel;

    @FXML
    private void initialize() {
        levelFilter.setItems(FXCollections.observableArrayList("All levels", "Beginner", "Intermediate", "Advanced"));
        levelFilter.setValue("All levels");
        loadWorkouts();
    }

    private void loadWorkouts() {
        try {
            allWorkouts = workoutService.readWithExercises();
        } catch (SQLException e) {
            allWorkouts = List.of();
        }
        applyFilter();
    }

    @FXML
    private void onSearch() {
        applyFilter();
    }

    private void applyFilter() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String level = levelFilter.getValue();

        List<Workout> filtered = allWorkouts.stream()
                .filter(w -> query.isEmpty() || safe(w.getNom()).toLowerCase().contains(query)
                        || safe(w.getDescription()).toLowerCase().contains(query))
                .filter(w -> level == null || level.equals("All levels")
                        || safe(w.getNiveau()).equalsIgnoreCase(level))
                .toList();

        renderWorkouts(filtered);
        counterLabel.setText(filtered.size() + " workout(s)");
    }

    private void renderWorkouts(List<Workout> workouts) {
        workoutListBox.getChildren().clear();
        if (workouts.isEmpty()) {
            Label empty = new Label("No workouts found.");
            empty.getStyleClass().add("users-subtitle");
            workoutListBox.getChildren().add(empty);
            return;
        }
        for (Workout w : workouts) {
            workoutListBox.getChildren().add(buildCard(w));
        }
    }

    private VBox buildCard(Workout w) {
        VBox card = new VBox(8);
        card.getStyleClass().add("workout-card");
        card.setPadding(new Insets(16));

        // Top row: name + badges
        HBox top = new HBox(10);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label name = new Label(safe(w.getNom()));
        name.getStyleClass().add("workout-card-name");
        HBox.setHgrow(name, Priority.ALWAYS);

        Label levelBadge = new Label(safe(w.getNiveau()));
        levelBadge.getStyleClass().addAll("role-badge", levelBadgeStyle(w.getNiveau()));

        Label statusBadge = new Label(safe(w.getStatus()).toUpperCase());
        statusBadge.getStyleClass().addAll("status-badge",
                "active".equalsIgnoreCase(w.getStatus()) ? "status-active" : "status-inactive");

        top.getChildren().addAll(name, levelBadge, statusBadge);

        // Info row
        HBox info = new HBox(20);
        info.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label duration = new Label("⏱  " + (w.getDuree() != null ? w.getDuree() + " min" : "—"));
        duration.getStyleClass().add("user-email");
        Label exercises = new Label("🏋  " + w.getExercises().size() + " exercise(s)");
        exercises.getStyleClass().add("user-email");

        // Average rating from users
        double avgRating = w.getUuid() != null ? ratingService.getWorkoutAverageRating(w.getUuid()) : 0;
        Label ratingLbl = new Label(avgRating > 0
                ? buildStars(avgRating) + String.format("  %.1f/5", avgRating)
                : "☆ No ratings yet");
        ratingLbl.setStyle("-fx-text-fill:#F59E0B;-fx-font-size:13px;-fx-font-weight:700;");

        info.getChildren().addAll(duration, exercises, ratingLbl);

        // Description
        Label desc = new Label(safe(w.getDescription()));
        desc.getStyleClass().add("user-email");
        desc.setWrapText(true);
        desc.setMaxWidth(Double.MAX_VALUE);

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Button editBtn = new Button("✎  Edit");
        editBtn.getStyleClass().addAll("table-icon-btn", "icon-edit");
        editBtn.setOnAction(e -> openEditModal(w));
        Button deleteBtn = new Button("🗑  Delete");
        deleteBtn.getStyleClass().addAll("table-icon-btn", "icon-delete");
        deleteBtn.setOnAction(e -> openDeleteConfirm(w));
        actions.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(top, info, desc, actions);
        return card;
    }

    private String levelBadgeStyle(String niveau) {
        if (niveau == null) return "";
        return switch (niveau.toLowerCase()) {
            case "beginner"     -> "level-beginner";
            case "intermediate" -> "level-intermediate";
            case "advanced"     -> "level-advanced";
            default             -> "";
        };
    }

    // ── Create / Edit modal ───────────────────────────────────────────────

    @FXML
    private void onCreateWorkout() {
        navigateToForm(null);
    }

    @FXML
    private void onManageExercises() {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/fxml/ExerciseListFragment.fxml")));
            searchField.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert("Navigation Error", e.getMessage());
        }
    }

    private void openEditModal(Workout w) {
        navigateToForm(w);
    }

    private void navigateToForm(Workout workout) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/fxml/WorkoutFormView.fxml")));
            Parent root = loader.load();
            WorkoutFormController controller = loader.getController();
            if (workout != null) controller.setWorkout(workout);
            Scene scene = searchField.getScene();
            scene.setRoot(root);
        } catch (Exception e) {
            showAlert("Navigation Error", e.getMessage());
        }
    }

    // ── Delete modal ──────────────────────────────────────────────────────

    private void openDeleteConfirm(Workout w) {
        pendingDelete = w;
        deleteConfirmLabel.setText("Delete \"" + safe(w.getNom()) + "\"? This cannot be undone.");
        showModal(deleteOverlay);
    }

    @FXML
    private void onConfirmDelete() {
        if (pendingDelete == null) { closeModal(deleteOverlay); return; }
        try {
            workoutService.delete(pendingDelete);
            closeModal(deleteOverlay);
            loadWorkouts();
        } catch (SQLException e) {
            showAlert("Delete Failed", e.getMessage());
        }
    }

    @FXML
    private void onCancelDelete() { closeModal(deleteOverlay); }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void showModal(StackPane overlay) {
        overlay.setManaged(true);
        overlay.setVisible(true);
    }

    private void closeModal(StackPane overlay) {
        overlay.setManaged(false);
        overlay.setVisible(false);
        pendingDelete = null;
    }

    private static String buildStars(double avg) {
        int full = (int) Math.round(avg);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= full ? "★" : "☆");
        return sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
