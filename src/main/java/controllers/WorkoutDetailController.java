package controllers;

import app.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import models.Exercise;
import models.ObjectifSportif;
import models.Workout;
import services.ProgressService;
import utils.ActivityTracker;
import utils.UuidUtil;
import utils.WebAssets;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
    private final Set<UUID> doneExerciseUuids = new HashSet<>();
    private final ProgressService progressService = new ProgressService();

    public void setWorkout(Workout w) {
        this.workout = w;
        ActivityTracker.track(ActivityTracker.EventType.VIEW_WORKOUT,
                "Viewed: " + (w.getNom() != null ? w.getNom() : "Workout"),
                w.getExercises().size() + " exercises");
        loadProgressFromDb();
        populate();
    }

    private void loadProgressFromDb() {
        if (workout == null || AppSession.getCurrentUser() == null) return;
        UUID userId = AppSession.getCurrentUser().getId();
        UUID workoutUuid = workout.getUuid();
        if (userId == null || workoutUuid == null) return;

        Set<UUID> done = progressService.getDoneExerciseUuids(userId, workoutUuid);
        System.out.println("DEBUG loadProgress: done UUIDs count = " + done.size());
        doneExerciseUuids.clear();
        doneExerciseIds.clear();
        doneExerciseUuids.addAll(done);
        // Sync integer ids only for exercises actually in done set
        for (Exercise e : workout.getExercises()) {
            if (e.getId() != null && done.contains(e.getId())) {
                doneExerciseIds.add(e.getId());
                System.out.println("DEBUG: exercise done = " + e.getNom());
            }
        }
    }

    public void markExerciseDone(int exerciseId) {
        // legacy — no-op, use markExerciseDoneUuid instead
    }

    public void markExerciseDoneUuid(UUID exerciseUuid, int exerciseId) {
        if (exerciseUuid != null) doneExerciseUuids.add(exerciseUuid);
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
                badge.setStyle("-fx-background-color:rgba(59,130,246,0.1);-fx-border-color:rgba(59,130,246,0.2);"
                        + "-fx-border-radius:8;-fx-background-radius:8;-fx-text-fill:#60A5FA;"
                        + "-fx-font-size:11px;-fx-font-weight:800;-fx-padding:3 10;");
                objectivesPane.getChildren().add(badge);
            }
        }

        // Exercises
        int total = workout.getExercises().size();
        exercisesTitle.setText("Exercises (" + total + ")");
        exercisesListBox.getChildren().clear();

        if (total == 0) {
            Label empty = new Label("No exercises added to this workout yet.");
            empty.setStyle("-fx-text-fill:#6B7280;-fx-font-style:italic;-fx-padding:20;");
            exercisesListBox.getChildren().add(empty);
        } else {
            for (Exercise e : workout.getExercises()) {
                exercisesListBox.getChildren().add(buildExerciseCard(e));
            }
        }

        updateProgress();
    }

    private HBox buildExerciseCard(Exercise e) {
        // Use UUID-based check only — int id can have hash collisions
        boolean isDone = e.getId() != null && doneExerciseUuids.contains(e.getId());

        HBox card = new HBox(16);
        card.setPadding(new Insets(18));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(javafx.scene.Cursor.HAND);

        if (isDone) {
            card.setStyle("-fx-background-color:rgba(34,197,94,0.05);-fx-border-color:rgba(34,197,94,0.3);"
                    + "-fx-border-radius:16;-fx-background-radius:16;");
        } else {
            card.setStyle("-fx-background-color:#111827;-fx-border-color:#1F2937;"
                    + "-fx-border-radius:16;-fx-background-radius:16;");
            card.setOnMouseEntered(ev -> card.setStyle(
                    "-fx-background-color:rgba(255,255,255,0.03);-fx-border-color:rgba(59,130,246,0.3);"
                    + "-fx-border-radius:16;-fx-background-radius:16;"));
            card.setOnMouseExited(ev -> card.setStyle(
                    "-fx-background-color:#111827;-fx-border-color:#1F2937;"
                    + "-fx-border-radius:16;-fx-background-radius:16;"));
        }

        card.setOnMouseClicked(ev -> openExerciseDetail(e));

        // ── Image thumbnail with tick overlay ──
        StackPane thumbPane = new StackPane();
        thumbPane.setMinWidth(64); thumbPane.setMinHeight(64);
        thumbPane.setMaxWidth(64); thumbPane.setMaxHeight(64);
        thumbPane.setStyle("-fx-background-color:#1F2937;-fx-background-radius:12;");

        if (exercise_hasImage(e)) {
            try {
                ImageView img = new ImageView(
                        new Image(WebAssets.assetUrl("uploads/exercises/" + e.getImageName()), 64, 64, false, true, true));
                img.setFitWidth(64); img.setFitHeight(64);
                img.setStyle("-fx-opacity:" + (isDone ? "0.5" : "1.0") + ";");
                img.setStyle(isDone ? "-fx-opacity:0.5;" : "");
                thumbPane.getChildren().add(img);
            } catch (Exception ignored) {
                thumbPane.getChildren().add(defaultIcon());
            }
        } else {
            thumbPane.getChildren().add(defaultIcon());
        }

        if (isDone) {
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color:rgba(34,197,94,0.3);-fx-background-radius:12;");
            overlay.setMinWidth(64); overlay.setMinHeight(64);
            overlay.setMaxWidth(64); overlay.setMaxHeight(64);
            Label tick = new Label("✔");
            tick.setStyle("-fx-text-fill:#4ADE80;-fx-font-size:20px;-fx-font-weight:900;");
            overlay.getChildren().add(tick);
            thumbPane.getChildren().add(overlay);
        }

        // ── Info ──
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(safe(e.getNom()));
        name.setStyle("-fx-text-fill:" + (isDone ? "#4ADE80" : "white")
                + ";-fx-font-size:16px;-fx-font-weight:700;");

        nameRow.getChildren().add(name);

        if (isDone) {
            Label doneBadge = new Label("✔ Done");
            doneBadge.setStyle("-fx-background-color:rgba(34,197,94,0.2);-fx-border-color:rgba(34,197,94,0.3);"
                    + "-fx-border-radius:999;-fx-background-radius:999;-fx-text-fill:#4ADE80;"
                    + "-fx-font-size:10px;-fx-font-weight:800;-fx-padding:2 8;");
            nameRow.getChildren().add(doneBadge);
        }

        String typeStr = safe(e.getType()).toUpperCase();
        Label typeLbl = new Label(typeStr + (typeStr.isEmpty() ? "FULL BODY" : "  •  FULL BODY"));
        typeLbl.setStyle("-fx-text-fill:#6B7280;-fx-font-size:11px;-fx-font-weight:700;");

        info.getChildren().addAll(nameRow, typeLbl);

        // ── Right: duration + chevron ──
        VBox right = new VBox(4);
        right.setAlignment(Pos.CENTER_RIGHT);

        String durStr = e.getDuree() != null && e.getDuree() > 0
                ? e.getDuree() + " secs"
                : (e.getSets() != null ? e.getSets() + " x " + (e.getReps() != null ? e.getReps() : 12) : "—");

        Label dur = new Label(durStr);
        dur.setStyle("-fx-text-fill:" + (isDone ? "#4ADE80" : "#60A5FA")
                + ";-fx-font-size:14px;-fx-font-weight:800;");

        Label perf = new Label("PERFORMANCE");
        perf.setStyle("-fx-text-fill:#4B5563;-fx-font-size:10px;-fx-font-weight:800;");

        Label chevron = new Label(isDone ? "✔" : "›");
        chevron.setStyle("-fx-text-fill:" + (isDone ? "#22C55E" : "#6B7280")
                + ";-fx-font-size:" + (isDone ? "14" : "18") + "px;");

        right.getChildren().addAll(dur, perf, chevron);

        card.getChildren().addAll(thumbPane, info, right);
        return card;
    }

    private Label defaultIcon() {
        Label icon = new Label("🏃");
        icon.setStyle("-fx-font-size:22px;");
        icon.setAlignment(Pos.CENTER);
        return icon;
    }

    private boolean exercise_hasImage(Exercise e) {
        return e.getImageName() != null && !e.getImageName().isBlank();
    }

    private void updateProgress() {
        int total = workout.getExercises().size();
        int done = doneExerciseUuids.size();
        boolean allDone = total > 0 && done == total;

        progressLabel.setText(done + " / " + total + " exercises completed");
        progressLabel.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:"
                + (allDone ? "#4ADE80" : "#60A5FA") + ";");

        if (total > 0) {
            double pct = (double) done / total;
            progressFill.prefWidthProperty().unbind();
            progressFill.prefWidthProperty().bind(
                    ((StackPane) progressFill.getParent()).widthProperty().multiply(pct));
            progressFill.setStyle("-fx-background-color:" + (allDone ? "#22C55E" : "#3B82F6")
                    + ";-fx-background-radius:6;-fx-pref-height:10;-fx-max-height:10;");
        }

        if (allDone) {
            startBtn.setText("🏆  Mark Workout Done");
            startBtn.setStyle("-fx-background-color:linear-gradient(to right,#22C55E,#16A34A);"
                    + "-fx-text-fill:black;-fx-font-weight:900;-fx-font-size:14px;"
                    + "-fx-background-radius:12;-fx-padding:14 0;-fx-max-width:Infinity;-fx-cursor:hand;");
            startBtn.setDisable(false);
        } else {
            startBtn.setText("🔒  Complete all exercises to unlock (" + done + "/" + total + ")");
            startBtn.setStyle("-fx-background-color:#1F2937;-fx-text-fill:#4B5563;"
                    + "-fx-font-weight:900;-fx-font-size:14px;"
                    + "-fx-background-radius:12;-fx-padding:14 0;-fx-max-width:Infinity;");
            startBtn.setDisable(true);
        }
    }

    @FXML
    private void onStartWorkout() {
        startBtn.setText("✅  Workout Completed! 🎉");
        startBtn.setDisable(true);
        startBtn.setStyle("-fx-background-color:rgba(34,197,94,0.2);-fx-border-color:rgba(34,197,94,0.3);"
                + "-fx-text-fill:#4ADE80;-fx-font-weight:800;-fx-font-size:14px;"
                + "-fx-background-radius:12;-fx-padding:14 0;-fx-max-width:Infinity;");
    }

    @FXML
    private void onBack() {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/fxml/DashboardView.fxml")));
            workoutNameLabel.getScene().setRoot(root);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void openExerciseDetail(Exercise e) {
        ActivityTracker.track(ActivityTracker.EventType.START_EXERCISE,
                "Started: " + safe(e.getNom()), safe(e.getType()) + " • " + (e.getDuree() != null ? e.getDuree() + "s" : ""));
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/fxml/ExerciseDetailView.fxml")));
            Parent root = loader.load();
            ExerciseDetailController ctrl = loader.getController();
            ctrl.setExercise(e, workout, done -> {
                doneExerciseIds.add(done.getId());
                if (done.getUuid() != null) doneExerciseUuids.add(done.getUuid());
            });
            workoutNameLabel.getScene().setRoot(root);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
