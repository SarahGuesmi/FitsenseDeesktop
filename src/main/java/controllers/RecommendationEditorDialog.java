package controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import models.MentalHealthAssessmentSubmission;
import models.RecommendedExercise;
import services.GmailService;
import services.GroqAiService;
import services.MentalHealthSubmissionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Modal editor: exercises + general note (coach mental health).
 */
public final class RecommendationEditorDialog {

    private RecommendationEditorDialog() {
    }

    public static boolean showEditor(Window owner, MentalHealthAssessmentSubmission submission) {
        Objects.requireNonNull(submission, "submission");
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Recommendation");

        VBox exercisesBox = new VBox(12);
        exercisesBox.setFillWidth(true);
        List<ExerciseRow> rows = new ArrayList<>();

        TextArea generalNote = new TextArea();
        generalNote.setPromptText("Special overall advice…");
        generalNote.setWrapText(true);
        generalNote.setPrefRowCount(4);
        generalNote.setMaxWidth(Double.MAX_VALUE);
        generalNote.getStyleClass().add("rec-textarea");

        Runnable addRow = () -> {
            ExerciseRow r = new ExerciseRow(new RecommendedExercise(), exercisesBox, rows);
            rows.add(r);
            exercisesBox.getChildren().add(r.root);
        };

        if (!submission.getRecommendedExercises().isEmpty()) {
            for (RecommendedExercise e : submission.getRecommendedExercises()) {
                ExerciseRow r = new ExerciseRow(e.copy(), exercisesBox, rows);
                rows.add(r);
                exercisesBox.getChildren().add(r.root);
            }
            String gn = submission.getRecommendationGeneralNote();
            if (gn != null) {
                generalNote.setText(gn);
            }
        } else {
            String gn = submission.getRecommendationGeneralNote();
            String legacy = submission.getCoachRecommendation();
            if (gn != null && !gn.isBlank()) {
                generalNote.setText(gn);
            } else if (legacy != null && !legacy.isBlank()) {
                generalNote.setText(legacy);
            }
            addRow.run();
        }

        if (submission.getRecommendationGeneralNote() != null && !submission.getRecommendationGeneralNote().isBlank()) {
            generalNote.setText(submission.getRecommendationGeneralNote());
        }

        Button addExerciseBtn = new Button("+ Add Exercise");
        addExerciseBtn.getStyleClass().add("rec-add-ex-btn");
        addExerciseBtn.setOnAction(e -> addRow.run());

        HBox exHeader = new HBox(12);
        exHeader.setAlignment(Pos.CENTER_LEFT);
        Label exLabel = new Label("Exercises to Recommend");
        exLabel.getStyleClass().add("rec-section-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        exHeader.getChildren().addAll(exLabel, spacer, addExerciseBtn);

        ScrollPane exScroll = new ScrollPane(exercisesBox);
        exScroll.setFitToWidth(true);
        exScroll.setMaxHeight(280);
        exScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Label genLabel = new Label("General Note (Optional)");
        genLabel.getStyleClass().add("rec-section-label");

        Button aiBtn = new Button("✨ Recommend with AI");
        aiBtn.getStyleClass().add("rec-ai-btn");
        
        // Inline layout for label + button
        Region genSpacer = new Region();
        HBox.setHgrow(genSpacer, Priority.ALWAYS);
        HBox genHeader = new HBox(10, genLabel, genSpacer, aiBtn);
        genHeader.setAlignment(Pos.CENTER_LEFT);

        aiBtn.setOnAction(e -> {
            aiBtn.setDisable(true);
            aiBtn.setText("Generating...");
            generalNote.setPromptText("AI is thinking...");
            
            GroqAiService.getInstance().generateRecommendation(submission)
                .thenAccept(result -> Platform.runLater(() -> {
                    // 1. Set general note
                    if (result.has("general_note")) {
                        generalNote.setText(result.get("general_note").getAsString());
                    }

                    // 2. Clear existing empty or existing rows if AI provides them
                    if (result.has("exercises")) {
                        JsonArray exercisesArr = result.getAsJsonArray("exercises");
                        if (exercisesArr.size() > 0) {
                            // Clear current UI rows
                            exercisesBox.getChildren().clear();
                            rows.clear();

                            // Add new AI suggested rows
                            for (JsonElement el : exercisesArr) {
                                JsonObject obj = el.getAsJsonObject();
                                String exName = obj.has("name") ? obj.get("name").getAsString() : "";
                                String exDur = obj.has("duration") ? obj.get("duration").getAsString() : "";
                                String exDesc = obj.has("description") ? obj.get("description").getAsString() : "";

                                ExerciseRow newRow = new ExerciseRow(
                                        new RecommendedExercise(exName, exDur, exDesc),
                                        exercisesBox,
                                        rows
                                );
                                rows.add(newRow);
                                exercisesBox.getChildren().add(newRow.root);
                            }
                        }
                    }

                    aiBtn.setDisable(false);
                    aiBtn.setText("✨ Recommend with AI");
                    generalNote.setPromptText("Special overall advice…");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        // Groq failed — retry once more, then show an error.
                        // No hardcoded exercises: everything comes from the AI.
                        aiBtn.setDisable(true);
                        aiBtn.setText("Retrying...");
                        generalNote.setPromptText("Contacting AI...");

                        GroqAiService.getInstance().generateRecommendation(submission)
                            .thenAccept(result -> Platform.runLater(() -> {
                                if (result.has("general_note")) {
                                    generalNote.setText(result.get("general_note").getAsString());
                                }
                                if (result.has("exercises")) {
                                    com.google.gson.JsonArray exercisesArr = result.getAsJsonArray("exercises");
                                    if (exercisesArr.size() > 0) {
                                        exercisesBox.getChildren().clear();
                                        rows.clear();
                                        for (com.google.gson.JsonElement el : exercisesArr) {
                                            com.google.gson.JsonObject obj = el.getAsJsonObject();
                                            String exName = obj.has("name") ? obj.get("name").getAsString() : "";
                                            String exDur  = obj.has("duration") ? obj.get("duration").getAsString() : "";
                                            String exDesc = obj.has("description") ? obj.get("description").getAsString() : "";
                                            ExerciseRow newRow = new ExerciseRow(
                                                    new RecommendedExercise(exName, exDur, exDesc),
                                                    exercisesBox, rows);
                                            rows.add(newRow);
                                            exercisesBox.getChildren().add(newRow.root);
                                        }
                                    }
                                }
                                aiBtn.setDisable(false);
                                aiBtn.setText("✨ Recommend with AI");
                                generalNote.setPromptText("Special overall advice…");
                            }))
                            .exceptionally(ex2 -> {
                                Platform.runLater(() -> {
                                    aiBtn.setDisable(false);
                                    aiBtn.setText("✨ Recommend with AI");
                                    generalNote.setPromptText("Special overall advice…");
                                    warn("AI Unavailable",
                                            "Could not reach Groq AI. Please check your API key in config.properties (groq.api.key) and try again.");
                                });
                                return null;
                            });
                    });
                    return null;
                });
        });

        VBox body = new VBox(14, exHeader, exScroll, genHeader, generalNote);
        body.getStyleClass().add("rec-body");
        body.setFillWidth(true);

        Label titlePart1 = new Label("Recommendation for ");
        titlePart1.getStyleClass().add("rec-dialog-title");
        String name = submission.getUserFullName() != null && !submission.getUserFullName().isBlank()
                ? submission.getUserFullName()
                : submission.getUserEmail();
        Label titlePart2 = new Label(name != null ? name : "Athlete");
        titlePart2.getStyleClass().add("rec-dialog-title-accent");
        HBox titleRow = new HBox(0, titlePart1, titlePart2);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("rec-close-btn");
        closeBtn.setOnAction(e -> stage.close());

        Region headSpacer = new Region();
        HBox.setHgrow(headSpacer, Priority.ALWAYS);
        HBox header = new HBox(12, titleRow, headSpacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("rec-dialog-header");

        boolean[] saved = {false};

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("rec-cancel-btn");
        cancelBtn.setOnAction(e -> stage.close());

        Button saveBtn = new Button("Save Recommendation");
        saveBtn.getStyleClass().add("rec-save-btn");
        saveBtn.setOnAction(e -> {
            if (!validateAndApply(submission, rows, generalNote.getText())) {
                return;
            }
            MentalHealthSubmissionService.getInstance().persistCoachRecommendation(submission);
            
            // Send email notification to the athlete
            String athleteEmail = submission.getUserEmail();
            if (athleteEmail != null && !athleteEmail.isBlank()) {
                String subject = "New Mental Health Recommendation from your Coach";
                String emailBody = "Hello " + (submission.getUserFullName() != null ? submission.getUserFullName() : "Athlete") + ",\n\n"
                        + "Your coach has updated your mental health recommendation:\n\n"
                        + buildLegacySummary(submission.getRecommendationGeneralNote() != null ? submission.getRecommendationGeneralNote() : "", submission.getRecommendedExercises())
                        + "\n\nStay focused and keep up the great work!\nFitSense Team";
                
                GmailService.getInstance().sendEmail(athleteEmail, subject, emailBody)
                    .exceptionally(ex -> {
                        // Email failed (e.g. Resend test-mode restriction) — log only,
                        // do NOT show a popup since the recommendation was already saved.
                        System.err.println("[RecommendationEditorDialog] Email not sent to "
                                + athleteEmail + ": " + ex.getMessage());
                        return null;
                    });
            }

            saved[0] = true;
            stage.close();
        });

        HBox footer = new HBox(12, cancelBtn, saveBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("rec-footer");

        VBox root = new VBox(header, body, footer);
        root.getStyleClass().add("rec-dialog-root");
        VBox.setVgrow(body, Priority.ALWAYS);

        Scene scene = new Scene(root, 540, 560);
        var css = RecommendationEditorDialog.class.getResource("/css/recommendation-ui.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.showAndWait();
        return saved[0];
    }

    private static boolean validateAndApply(MentalHealthAssessmentSubmission submission,
                                            List<ExerciseRow> rows, String noteRaw) {
        String note = noteRaw == null ? "" : noteRaw.trim();
        List<RecommendedExercise> collected = new ArrayList<>();
        for (ExerciseRow r : rows) {
            RecommendedExercise m = r.toModel();
            if (m.hasContent()) {
                if (m.getName().isBlank()) {
                    warn("Exercise name", "Each exercise with a duration or description needs a name.");
                    return false;
                }
                collected.add(m);
            }
        }
        if (collected.isEmpty() && note.isEmpty()) {
            warn("Empty recommendation", "Add at least one exercise or a general note.");
            return false;
        }
        submission.setRecommendedExercises(collected);
        submission.setRecommendationGeneralNote(note.isEmpty() ? null : note);
        submission.setCoachRecommendation(buildLegacySummary(note, collected));
        return true;
    }

    private static String buildLegacySummary(String generalNote, List<RecommendedExercise> exercises) {
        StringBuilder sb = new StringBuilder();
        if (!generalNote.isEmpty()) {
            sb.append(generalNote);
        }
        for (RecommendedExercise e : exercises) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("• ").append(e.getName());
            if (!e.getDurationMinutes().isBlank()) {
                sb.append(" — ").append(e.getDurationMinutes()).append(" min");
            }
            if (!e.getDescription().isBlank()) {
                sb.append(": ").append(e.getDescription());
            }
        }
        return sb.toString();
    }

    private static void warn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static final class ExerciseRow {
        private final TextField nameField = new TextField();
        private final TextField durationField = new TextField();
        private final TextArea descriptionArea = new TextArea();
        private final VBox root;

        ExerciseRow(RecommendedExercise e, VBox exercisesBox, List<ExerciseRow> rows) {
            nameField.setPromptText("Exercise Name (e.g. Yoga)");
            durationField.setPromptText("Duration (min)");
            descriptionArea.setPromptText("Exercise Description…");
            descriptionArea.setWrapText(true);
            descriptionArea.setPrefRowCount(3);
            nameField.getStyleClass().add("rec-field");
            durationField.getStyleClass().add("rec-field");
            descriptionArea.getStyleClass().add("rec-textarea");
            nameField.setText(e.getName());
            durationField.setText(e.getDurationMinutes());
            descriptionArea.setText(e.getDescription());

            Button remove = new Button("Remove");
            remove.getStyleClass().add("rec-remove-ex-btn");

            HBox top = new HBox(10, nameField, durationField);
            top.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(nameField, Priority.ALWAYS);
            durationField.setPrefWidth(120);

            VBox box = new VBox(8, top, descriptionArea, remove);
            box.getStyleClass().add("rec-ex-card");
            box.setPadding(new Insets(0));
            remove.setOnAction(ev -> {
                exercisesBox.getChildren().remove(box);
                rows.remove(this);
            });
            root = box;
        }

        boolean isRowEmpty() {
            return nameField.getText().isBlank()
                    && durationField.getText().isBlank()
                    && descriptionArea.getText().isBlank();
        }

        RecommendedExercise toModel() {
            return new RecommendedExercise(
                    nameField.getText(),
                    durationField.getText(),
                    descriptionArea.getText());
        }
    }
}
