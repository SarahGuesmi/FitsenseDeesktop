package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.Exercise;
import services.ExerciseService;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class ExerciseListController {

    @FXML private TextField searchField;
    @FXML private Label counterLabel;
    @FXML private VBox exerciseListBox;
    @FXML private StackPane deleteOverlay;
    @FXML private Label deleteConfirmLabel;

    private final ExerciseService exerciseService = new ExerciseService();
    private List<Exercise> allExercises;
    private Exercise pendingDelete;

    @FXML
    private void initialize() {
        loadExercises();
    }

    private void loadExercises() {
        try {
            allExercises = exerciseService.read();
        } catch (SQLException e) {
            allExercises = List.of();
        }
        applyFilter();
    }

    @FXML
    private void onSearch() {
        applyFilter();
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<Exercise> filtered = allExercises.stream()
                .filter(e -> q.isEmpty()
                        || safe(e.getNom()).toLowerCase().contains(q)
                        || safe(e.getType()).toLowerCase().contains(q))
                .toList();
        renderExercises(filtered);
        counterLabel.setText(filtered.size() + " exercise(s)");
    }

    private void renderExercises(List<Exercise> exercises) {
        exerciseListBox.getChildren().clear();
        if (exercises.isEmpty()) {
            Label empty = new Label("No exercises found.");
            empty.getStyleClass().add("exercise-card-desc");
            empty.setPadding(new Insets(20));
            exerciseListBox.getChildren().add(empty);
            return;
        }
        for (Exercise e : exercises) {
            exerciseListBox.getChildren().add(buildCard(e));
        }
    }

    private HBox buildCard(Exercise e) {
        HBox card = new HBox(14);
        card.getStyleClass().add("exercise-card");
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER_LEFT);

        // Green icon box
        Label icon = new Label("🏋");
        icon.getStyleClass().add("exercise-icon-box");
        icon.setMinWidth(48); icon.setMinHeight(48);
        icon.setMaxWidth(48); icon.setMaxHeight(48);
        icon.setAlignment(Pos.CENTER);
        icon.setStyle("-fx-font-size: 20px; -fx-background-color: linear-gradient(to bottom right,#22C55E,#16A34A); -fx-background-radius: 10;");

        // Info section
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Row 1: name + type badge + duration
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(safe(e.getNom()));
        name.getStyleClass().add("exercise-card-name");

        if (e.getType() != null && !e.getType().isBlank()) {
            Label typeBadge = new Label(e.getType().toUpperCase());
            typeBadge.getStyleClass().add("exercise-type-badge");
            typeBadge.setStyle(typeBadgeCss(e.getType()));
            topRow.getChildren().addAll(name, typeBadge);
        } else {
            topRow.getChildren().add(name);
        }

        if (e.getDuree() > 0) {
            Label dur = new Label("⏱  " + e.getDuree() + " sec");
            dur.getStyleClass().add("exercise-duration-label");
            topRow.getChildren().add(dur);
        }

        // Row 2: description (HTML stripped, truncated)
        String rawDesc = safe(e.getDescription());
        String cleanDesc = stripHtml(rawDesc);
        Label desc = new Label(cleanDesc.isEmpty() ? "No description" :
                (cleanDesc.length() > 120 ? cleanDesc.substring(0, 120) + "…" : cleanDesc));
        desc.getStyleClass().add("exercise-card-desc");
        desc.setWrapText(true);

        info.getChildren().addAll(topRow, desc);

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("✎");
        editBtn.getStyleClass().add("btn-exercise-edit");
        editBtn.setOnAction(ev -> navigateToExerciseForm(e));

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("btn-exercise-delete");
        deleteBtn.setOnAction(ev -> openDeleteConfirm(e));

        actions.getChildren().addAll(editBtn, deleteBtn);
        card.getChildren().addAll(icon, info, actions);
        return card;
    }

    /** Color per type matching the web app */
    private String typeBadgeCss(String type) {
        if (type == null) return "";
        return switch (type.toLowerCase()) {
            case "cardio"     -> "-fx-background-color: rgba(59,130,246,0.2); -fx-text-fill: #60A5FA;";
            case "strength"   -> "-fx-background-color: rgba(168,85,247,0.2); -fx-text-fill: #C084FC;";
            case "hiit"       -> "-fx-background-color: rgba(239,68,68,0.2);  -fx-text-fill: #F87171;";
            case "endurance"  -> "-fx-background-color: rgba(34,197,94,0.2);  -fx-text-fill: #4ADE80;";
            case "flexibility"-> "-fx-background-color: rgba(234,179,8,0.2);  -fx-text-fill: #FACC15;";
            default           -> "-fx-background-color: rgba(107,114,128,0.2);-fx-text-fill: #9CA3AF;";
        };
    }

    /** Strip basic HTML tags */
    private static String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        return html.replaceAll("<[^>]+>", "").replaceAll("&nbsp;", " ").trim();
    }

    // ── Navigation ────────────────────────────────────────────────────────

    @FXML
    private void onCreateExercise() {
        navigateToExerciseForm(null);
    }

    private void navigateToExerciseForm(Exercise exercise) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/fxml/ExerciseFormView.fxml")));
            Parent root = loader.load();
            ExerciseFormController ctrl = loader.getController();
            if (exercise != null) ctrl.setExercise(exercise);
            searchField.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onBackToCatalog() {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/fxml/CoachDashboardView.fxml")));
            searchField.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ── Delete modal ──────────────────────────────────────────────────────

    private void openDeleteConfirm(Exercise e) {
        pendingDelete = e;
        deleteConfirmLabel.setText("Delete \"" + safe(e.getNom()) + "\"? This cannot be undone.");
        deleteOverlay.setManaged(true);
        deleteOverlay.setVisible(true);
    }

    @FXML
    private void onConfirmDelete() {
        if (pendingDelete == null) { closeDelete(); return; }
        try {
            exerciseService.delete(pendingDelete);
            closeDelete();
            loadExercises();
        } catch (SQLException ex) {
            deleteConfirmLabel.setText("Error: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancelDelete() { closeDelete(); }

    private void closeDelete() {
        deleteOverlay.setManaged(false);
        deleteOverlay.setVisible(false);
        pendingDelete = null;
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
