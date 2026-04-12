package controllers;

import app.AppSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.ObjectifSportif;
import models.User;
import models.Workout;
import services.ObjectifSportifService;
import services.WorkoutService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserWorkoutsController {

    @FXML private FlowPane objectivePillsPane;
    @FXML private ComboBox<String> levelFilter;
    @FXML private ComboBox<String> durationFilter;
    @FXML private VBox workoutsContainer;

    private final WorkoutService workoutService = new WorkoutService();
    private final ObjectifSportifService objectifService = new ObjectifSportifService();

    private List<Workout> matchingWorkouts = new ArrayList<>();
    private List<String> userObjectiveNames = new ArrayList<>();

    @FXML
    private void initialize() {
        levelFilter.setItems(FXCollections.observableArrayList(
                "All Levels", "Beginner", "Intermediate", "Advanced"));
        levelFilter.setValue("All Levels");

        durationFilter.setItems(FXCollections.observableArrayList(
                "All Durations", "≤ 20 min", "≤ 40 min", "≤ 60 min"));
        durationFilter.setValue("All Durations");

        loadUserObjectives();
        loadWorkouts();
    }

    private void loadUserObjectives() {
        User user = AppSession.getCurrentUser();
        if (user == null) return;
        try {
            List<ObjectifSportif> objs = objectifService.findAllByAppUserId(user.getId());
            for (ObjectifSportif o : objs) {
                if (o.getName() != null && !o.getName().isBlank()) {
                    String name = o.getName().trim();
                    if (!userObjectiveNames.contains(name)) {
                        userObjectiveNames.add(name);
                    }
                }
            }
        } catch (SQLException ignored) {}

        // Show only unique pills for current user
        objectivePillsPane.getChildren().clear();
        for (String name : userObjectiveNames) {
            Label pill = new Label("🏷  " + name);
            pill.getStyleClass().add("objective-pill");
            objectivePillsPane.getChildren().add(pill);
        }
    }

    private void loadWorkouts() {
        try {
            if (userObjectiveNames.isEmpty()) {
                matchingWorkouts = workoutService.readWithExercises();
            } else {
                matchingWorkouts = workoutService.findByObjectiveNames(userObjectiveNames);
            }
        } catch (SQLException e) {
            matchingWorkouts = new ArrayList<>();
        }
        applyFilter();
    }

    @FXML
    private void onFilter() {
        applyFilter();
    }

    private void applyFilter() {
        String level = levelFilter.getValue();
        String dur = durationFilter.getValue();

        List<Workout> filtered = matchingWorkouts.stream()
                .filter(w -> level == null || level.equals("All Levels")
                        || safe(w.getNiveau()).equalsIgnoreCase(level))
                .filter(w -> {
                    if (dur == null || dur.equals("All Durations")) return true;
                    int d = w.getDuree() != null ? w.getDuree() : 0;
                    return switch (dur) {
                        case "≤ 20 min" -> d <= 20;
                        case "≤ 40 min" -> d <= 40;
                        case "≤ 60 min" -> d <= 60;
                        default -> true;
                    };
                })
                .toList();

        renderWorkouts(filtered);
    }

    private void renderWorkouts(List<Workout> workouts) {
        workoutsContainer.getChildren().clear();
        if (workouts.isEmpty()) {
            renderEmpty();
            return;
        }
        // 2-column grid using HBox rows
        List<Workout> list = new ArrayList<>(workouts);
        for (int i = 0; i < list.size(); i += 2) {
            HBox row = new HBox(16);
            row.setPadding(new Insets(0, 0, 16, 0));
            VBox card1 = buildCard(list.get(i));
            HBox.setHgrow(card1, Priority.ALWAYS);
            row.getChildren().add(card1);
            if (i + 1 < list.size()) {
                VBox card2 = buildCard(list.get(i + 1));
                HBox.setHgrow(card2, Priority.ALWAYS);
                row.getChildren().add(card2);
            } else {
                // empty spacer for odd count
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.getChildren().add(spacer);
            }
            workoutsContainer.getChildren().add(row);
        }
    }

    private VBox buildCard(Workout w) {
        VBox card = new VBox(0);
        card.getStyleClass().add("workout-user-card");

        // Image area with level badge
        StackPane imageArea = new StackPane();
        imageArea.getStyleClass().add("card-image-area");
        imageArea.setMinHeight(120);
        imageArea.setMaxHeight(120);

        // Level badge bottom-left
        Label levelBadge = new Label(safe(w.getNiveau()).toUpperCase());
        levelBadge.getStyleClass().addAll("card-level-badge", levelBadgeStyle(w.getNiveau()));
        StackPane.setAlignment(levelBadge, Pos.BOTTOM_LEFT);
        StackPane.setMargin(levelBadge, new Insets(0, 0, 10, 12));

        // Dumbbell icon center
        Label icon = new Label("🏋");
        icon.setStyle("-fx-font-size: 36px; -fx-opacity: 0.15;");

        imageArea.getChildren().addAll(icon, levelBadge);

        // Content
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));

        Label name = new Label(safe(w.getNom()));
        name.getStyleClass().add("card-workout-name");
        name.setWrapText(true);

        HBox meta = new HBox(16);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label duration = new Label("⏱  " + (w.getDuree() != null ? w.getDuree() : 0) + " mins");
        duration.getStyleClass().add("card-meta-label");
        Label exercises = new Label("📋  " + w.getExercises().size() + " exercises");
        exercises.getStyleClass().add("card-meta-label");
        meta.getChildren().addAll(duration, exercises);

        String rawDesc = safe(w.getDescription());
        String cleanDesc = rawDesc.replaceAll("<[^>]+>", "").trim();
        Label desc = new Label(cleanDesc.isEmpty() ? "No description provided for this session."
                : (cleanDesc.length() > 100 ? cleanDesc.substring(0, 100) + "…" : cleanDesc));
        desc.getStyleClass().add("card-desc");
        desc.setWrapText(true);

        Button startBtn = new Button("Start Workout");
        startBtn.getStyleClass().add("btn-start-workout");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setOnAction(ev -> navigateToDetail(w));

        content.getChildren().addAll(name, meta, desc, startBtn);
        card.getChildren().addAll(imageArea, content);
        return card;
    }

    private void renderEmpty() {
        VBox empty = new VBox(10);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(60));
        Label icon = new Label("🏋");
        icon.setStyle("-fx-font-size: 48px; -fx-opacity: 0.3;");
        Label title = new Label("No matching workouts");
        title.getStyleClass().add("empty-state-label");
        Label sub = new Label("Wait for your coach to create workouts matching your objectives.");
        sub.getStyleClass().add("empty-state-sub");
        sub.setWrapText(true);
        sub.setAlignment(Pos.CENTER);
        empty.getChildren().addAll(icon, title, sub);
        workoutsContainer.getChildren().add(empty);
    }

    private String levelBadgeStyle(String niveau) {
        if (niveau == null) return "badge-beginner";
        return switch (niveau.toLowerCase()) {
            case "intermediate" -> "badge-intermediate";
            case "advanced"     -> "badge-advanced";
            default             -> "badge-beginner";
        };
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private void navigateToDetail(Workout w) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/fxml/WorkoutDetailView.fxml")));
            Parent root = loader.load();
            WorkoutDetailController ctrl = loader.getController();
            ctrl.setWorkout(w);
            levelFilter.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
