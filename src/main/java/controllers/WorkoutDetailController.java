package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.Exercise;
import models.ObjectifSportif;
import models.Workout;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class WorkoutDetailController {

    @FXML private Label workoutNameLabel;
    @FXML private Label workoutMetaLabel;
    @FXML private Label workoutDescLabel;
    @FXML private FlowPane objectivesPane;
    @FXML private Label exercisesTitle;
    @FXML private VBox exercisesListBox;
    @FXML private Label progressLabel;
    @FXML private Region progressFill;
    @FXML private Button startBtn;

    private Workout workout;
    private final Set<Integer> doneExerciseIds = new HashSet<>();

    public void setWorkout(Workout w) {
        this.workout = w;
        populate();
    }

    private void populate() {
        if (workout == null) return;

        workoutNameLabel.setText(safe(workout.getNom()));
        workoutMetaLabel.setText(
                (workout.getDuree() != null ? workout.getDuree() + " minute session" : "") +
                (workout.getNiveau() != null ? "  •  " + capitalize(workout.getNiveau()) : ""));

        String raw = safe(workout.getDescription()).replaceAll("<[^>]+>", "").trim();
        workoutDescLabel.setText(raw.isEmpty()
                ? "Prepare yourself for an intensive session designed to help you reach your goals."
                : raw);

        // Objectives
        objectivesPane.getChildren().clear();
        for (ObjectifSportif o : workout.getObjectifs()) {
            if (o.getName() != null && !o.getName().isBlank()) {
                Label badge = new Label(o.getName());
                badge.getStyleClass().add("obj-badge");
                objectivesPane.getChildren().add(badge);
            }
        }

        // Exercises
        int total = workout.getExercises().size();
        exercisesTitle.setText("Exercises (" + total + ")");
        exercisesListBox.getChildren().clear();

        if (total == 0) {
            Label empty = new Label("No exercises added to this workout yet.");
            empty.getStyleClass().add("detail-desc");
            empty.setPadding(new Insets(20));
            exercisesListBox.getChildren().add(empty);
        } else {
            for (Exercise e : workout.getExercises()) {
                exercisesListBox.getChildren().add(buildExerciseRow(e));
            }
        }

        // Initial state: locked
        startBtn.setText("🔒  Complete all exercises to unlock (0/" + total + ")");
        startBtn.getStyleClass().setAll("btn-start-disabled");
        startBtn.setDisable(true);
        updateProgress();
    }

    private HBox buildExerciseRow(Exercise e) {
        HBox row = new HBox(14);
        row.getStyleClass().add("exercise-row");
        row.setPadding(new Insets(14));
        row.setAlignment(Pos.CENTER_LEFT);

        // Icon
        Label icon = new Label("🏋");
        icon.getStyleClass().add("exercise-icon-green");
        icon.setAlignment(Pos.CENTER);
        icon.setMinWidth(52); icon.setMinHeight(52);
        icon.setMaxWidth(52); icon.setMaxHeight(52);

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(safe(e.getNom()));
        name.getStyleClass().add("exercise-row-name");
        nameRow.getChildren().add(name);

        // Done badge (hidden initially)
        Label doneBadge = new Label("✔ Done");
        doneBadge.setStyle("-fx-background-color: rgba(34,197,94,0.2); -fx-border-color: rgba(34,197,94,0.3); " +
                "-fx-border-radius: 20; -fx-background-radius: 20; -fx-text-fill: #4ADE80; " +
                "-fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 8;");
        doneBadge.setManaged(false);
        doneBadge.setVisible(false);
        nameRow.getChildren().add(doneBadge);

        String typeStr = safe(e.getType()).toUpperCase();
        Label typeLbl = new Label(typeStr + (typeStr.isEmpty() ? "" : "  •  ") + "FULL BODY");
        typeLbl.getStyleClass().add("exercise-row-type");

        info.getChildren().addAll(nameRow, typeLbl);

        // Right: duration + mark done button
        VBox right = new VBox(4);
        right.setAlignment(Pos.CENTER_RIGHT);

        String durStr = e.getDuree() != null && e.getDuree() > 0
                ? e.getDuree() + " secs"
                : (e.getSets() != null ? e.getSets() + " x " + (e.getReps() != null ? e.getReps() : 12) : "—");
        Label dur = new Label(durStr);
        dur.getStyleClass().add("exercise-row-duration");

        Label perf = new Label("PERFORMANCE");
        perf.setStyle("-fx-text-fill: #4B5563; -fx-font-size: 10px; -fx-font-weight: 800;");

        Button markBtn = new Button("Mark Done");
        markBtn.setStyle("-fx-background-color: #1F2937; -fx-border-color: #374151; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: #9CA3AF; " +
                "-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-cursor: hand;");

        markBtn.setOnAction(ev -> {
            if (doneExerciseIds.contains(e.getId())) {
                // Undo
                doneExerciseIds.remove(e.getId());
                markBtn.setText("Mark Done");
                markBtn.setStyle("-fx-background-color: #1F2937; -fx-border-color: #374151; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: #9CA3AF; " +
                        "-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-cursor: hand;");
                doneBadge.setManaged(false);
                doneBadge.setVisible(false);
                name.setStyle("-fx-text-fill: white;");
                row.setStyle("-fx-background-color: #111827; -fx-border-color: #1F2937; " +
                        "-fx-border-radius: 12; -fx-background-radius: 12;");
                icon.setStyle("-fx-background-color: linear-gradient(to bottom right,#22C55E,#16A34A); " +
                        "-fx-background-radius: 10; -fx-font-size: 22px;");
            } else {
                // Mark done
                doneExerciseIds.add(e.getId());
                markBtn.setText("✔ Done");
                markBtn.setStyle("-fx-background-color: rgba(34,197,94,0.15); -fx-border-color: rgba(34,197,94,0.3); " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: #4ADE80; " +
                        "-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-cursor: hand;");
                doneBadge.setManaged(true);
                doneBadge.setVisible(true);
                name.setStyle("-fx-text-fill: #4ADE80;");
                row.setStyle("-fx-background-color: rgba(34,197,94,0.05); " +
                        "-fx-border-color: rgba(34,197,94,0.3); " +
                        "-fx-border-radius: 12; -fx-background-radius: 12;");
            }
            updateProgress();
        });

        right.getChildren().addAll(dur, perf, markBtn);
        row.getChildren().addAll(icon, info, right);
        return row;
    }

    private void updateProgress() {
        int total = workout.getExercises().size();
        int done = doneExerciseIds.size();
        progressLabel.setText(done + " / " + total + " exercises completed");

        // Update progress bar width via binding
        progressFill.sceneProperty().addListener((obs, o, scene) -> {});
        if (total > 0) {
            double pct = (double) done / total;
            progressFill.prefWidthProperty().bind(
                    ((StackPane) progressFill.getParent()).widthProperty().multiply(pct));
        }

        boolean allDone = total > 0 && done == total;
        if (allDone) {
            startBtn.setText("🏆  Mark Workout Done");
            startBtn.getStyleClass().setAll("btn-start-session");
            startBtn.setDisable(false);
        } else {
            startBtn.setText("🔒  Complete all exercises to unlock (" + done + "/" + total + ")");
            startBtn.getStyleClass().setAll("btn-start-disabled");
            startBtn.setDisable(true);
        }
    }

    @FXML
    private void onStartWorkout() {
        startBtn.setText("✅  Workout Completed! 🎉");
        startBtn.setDisable(true);
        startBtn.setStyle("-fx-background-color: rgba(34,197,94,0.2); -fx-border-color: rgba(34,197,94,0.3); " +
                "-fx-text-fill: #4ADE80; -fx-font-weight: 800; -fx-font-size: 14px; " +
                "-fx-background-radius: 12; -fx-padding: 14 0; -fx-max-width: Infinity;");
    }

    @FXML
    private void onBack() {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/fxml/DashboardView.fxml")));
            workoutNameLabel.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
