package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import models.Exercise;
import models.ObjectifSportif;
import models.Workout;
import services.ExerciseService;
import services.ObjectifSportifService;
import services.WorkoutService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WorkoutFormController {

    @FXML private Label formTitle;
    @FXML private Button submitBtn;
    @FXML private TextField nameField;
    @FXML private FlowPane objectivesPane;
    @FXML private ComboBox<String> levelField;
    @FXML private TextField durationField;
    @FXML private TextArea descriptionField;
    @FXML private ListView<String> exercisesList;

    @FXML private Label nameError;
    @FXML private Label levelError;
    @FXML private Label durationError;
    @FXML private Label exercisesError;

    private final WorkoutService workoutService = new WorkoutService();
    private final ExerciseService exerciseService = new ExerciseService();

    private Workout editingWorkout;
    private List<Exercise> allExercises = new ArrayList<>();
    private List<CheckBox> objectiveCheckboxes = new ArrayList<>();

    @FXML
    private void initialize() {
        levelField.setItems(FXCollections.observableArrayList("Beginner", "Intermediate", "Advanced"));
        exercisesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        loadObjectives();
        loadExercises();
    }

    private void loadObjectives() {
        objectivesPane.getChildren().clear();
        objectiveCheckboxes.clear();
        for (String name : List.of("Weight Loss", "Muscle Gain", "Endurance", "Well-being")) {
            CheckBox cb = new CheckBox(name);
            cb.getStyleClass().add("objective-checkbox");
            objectiveCheckboxes.add(cb);
            objectivesPane.getChildren().add(cb);
        }
    }

    private void loadExercises() {
        try {
            allExercises = exerciseService.read();
            List<String> names = allExercises.stream()
                    .map(e -> e.getNom() != null ? e.getNom() : "Exercise #" + e.getId())
                    .toList();
            exercisesList.setItems(FXCollections.observableArrayList(names));
        } catch (SQLException e) {
            exercisesList.setItems(FXCollections.observableArrayList());
        }
    }

    public void setWorkout(Workout workout) {
        this.editingWorkout = workout;
        if (workout == null) return;

        formTitle.setText("Edit Workout");
        submitBtn.setText("Update Workout");

        nameField.setText(safe(workout.getNom()));
        levelField.setValue(workout.getNiveau());
        durationField.setText(workout.getDuree() != null ? String.valueOf(workout.getDuree()) : "");
        descriptionField.setText(safe(workout.getDescription()));

        // Pre-select exercises
        List<Integer> exerciseIds = workout.getExercises().stream()
                .map(Exercise::getId).toList();

        for (int i = 0; i < allExercises.size(); i++) {
            if (exerciseIds.contains(allExercises.get(i).getId())) {
                exercisesList.getSelectionModel().select(i);
            }
        }

        // Pre-check objectives
        List<String> existingObjNames = workout.getObjectifs().stream()
                .map(o -> o.getName() != null ? o.getName().trim() : "")
                .filter(s -> !s.isEmpty())
                .toList();

        for (CheckBox cb : objectiveCheckboxes) {
            cb.setSelected(existingObjNames.stream()
                    .anyMatch(n -> n.equalsIgnoreCase(cb.getText())));
        }
    }

    @FXML
    private void onSubmit() {
        clearErrors();
        boolean valid = true;

        String name = safe(nameField.getText()).trim();
        String level = levelField.getValue();
        String durationText = safe(durationField.getText()).trim();
        String description = safe(descriptionField.getText()).trim();

        // ===== NAME =====
        if (name.isEmpty()) {
            showError(nameError, "Workout name is required.");
            nameField.getStyleClass().add("form-input-error");
            valid = false;

        } else if (!name.matches("^[a-zA-Z0-9 ]+$")) {
            showError(nameError, "Only letters and numbers allowed.");
            valid = false;

        } else if (name.matches("^\\d+$")) {
            showError(nameError, "Name cannot be only numbers.");
            valid = false;
        }

        // ===== LEVEL =====
        if (level == null || level.isBlank()) {
            showError(levelError, "Level is required.");
            valid = false;
        }

        // ===== DURATION =====
        Integer duration = null;

        if (durationText.isEmpty()) {
            showError(durationError, "Duration is required.");
            valid = false;

        } else {
            try {
                duration = Integer.parseInt(durationText);

                if (duration < 1) {
                    showError(durationError, "Min 1 minute.");
                    valid = false;

                } else if (duration > 300) {
                    showError(durationError, "Max 300 minutes.");
                    valid = false;
                }

            } catch (NumberFormatException e) {
                showError(durationError, "Must be a number.");
                valid = false;
            }
        }

        // ===== EXERCISES =====
        if (exercisesList.getSelectionModel().getSelectedItems().isEmpty()) {
            showError(exercisesError, "Select at least one exercise.");
            exercisesList.getStyleClass().add("form-input-error");
            valid = false;
        }

        if (!valid) return;

        // ===== COLLECT DATA =====
        List<Exercise> selectedExercises = new ArrayList<>();
        for (int idx : exercisesList.getSelectionModel().getSelectedIndices()) {
            if (idx < allExercises.size()) {
                selectedExercises.add(allExercises.get(idx));
            }
        }

        List<String> selectedObjectives = new ArrayList<>();
        for (CheckBox cb : objectiveCheckboxes) {
            if (cb.isSelected()) selectedObjectives.add(cb.getText());
        }

        try {
            if (editingWorkout == null) {
                Workout w = new Workout();
                w.setNom(name);
                w.setNiveau(level);
                w.setDuree(duration);
                w.setDescription(description);
                w.setStatus("active");
                w.setExercises(selectedExercises);

                workoutService.createPrepared(w);
                workoutService.syncObjectifs(w.getUuid(), selectedObjectives);

            } else {
                editingWorkout.setNom(name);
                editingWorkout.setNiveau(level);
                editingWorkout.setDuree(duration);
                editingWorkout.setDescription(description);
                editingWorkout.setExercises(selectedExercises);

                workoutService.update(editingWorkout);
                workoutService.syncObjectifs(editingWorkout.getUuid(), selectedObjectives);
            }

            goBackToCatalog();

        } catch (SQLException e) {
            showError(nameError, "Save failed: " + e.getMessage());
        }
    }

    @FXML private void onBack() { goBackToCatalog(); }
    @FXML private void onCancel() { goBackToCatalog(); }

    private void goBackToCatalog() {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/fxml/CoachDashboardView.fxml")));
            nameField.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearErrors() {
        for (Label l : new Label[]{nameError, levelError, durationError, exercisesError}) {
            l.setVisible(false);
            l.setManaged(false);
            l.setText("");
        }

        nameField.getStyleClass().remove("form-input-error");
        durationField.getStyleClass().remove("form-input-error");
        exercisesList.getStyleClass().remove("form-input-error");
    }

    private static void showError(Label label, String msg) {
        label.setText("⚠ " + msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
