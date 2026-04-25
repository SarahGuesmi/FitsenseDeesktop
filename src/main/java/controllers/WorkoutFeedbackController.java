package controllers;

import app.AppSession;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.Questionnaire;
import models.Workout;
import services.FeedbackService;
import utils.DbConnection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WorkoutFeedbackController {

    @FXML private Label workoutTitleLabel;
    @FXML private VBox optionsGrid;
    @FXML private TextArea commentArea;
    @FXML private Button submitBtn;

    private Workout currentWorkout;
    private Questionnaire template;
    private Runnable onDoneCallback;
    private ToggleGroup ratingGroup;

    public void setWorkout(Workout workout) {
        this.currentWorkout = workout;
        if (workoutTitleLabel != null && workout != null) {
            workoutTitleLabel.setText("📋  Feedback for: " + workout.getNom());
        }
        loadTemplate();
    }

    public void setOnDoneCallback(Runnable callback) {
        this.onDoneCallback = callback;
    }

    private void loadTemplate() {
        if (currentWorkout == null) return;
        try {
            FeedbackService service = new FeedbackService(DbConnection.getInstance().getCnx());
            template = service.findTemplateByWorkoutId(currentWorkout.getId());
            if (template == null) {
                submitBtn.setDisable(true);
                optionsGrid.getChildren().add(new Label("No feedback questionnaire assigned to this workout."));
                return;
            }
            buildOptions(parseOptions(template.getOptions()));
        } catch (SQLException e) {
            submitBtn.setDisable(true);
            optionsGrid.getChildren().add(new Label("Could not load feedback: " + e.getMessage()));
        }
    }

    private void buildOptions(List<String> options) {
        optionsGrid.getChildren().clear();
        ratingGroup = new ToggleGroup();

        // 2-column grid
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        int col = 0, row = 0;
        for (String opt : options) {
            HBox cell = buildOptionCell(opt, ratingGroup);
            grid.add(cell, col, row);
            col++;
            if (col == 2) { col = 0; row++; }
        }
        optionsGrid.getChildren().add(grid);
    }

    private HBox buildOptionCell(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setMaxWidth(Double.MAX_VALUE);
        rb.getStyleClass().add("fb-radio");

        HBox cell = new HBox(rb);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.getStyleClass().add("fb-option-cell");
        cell.setPrefWidth(240);
        cell.setMaxWidth(Double.MAX_VALUE);

        // Highlight on select
        rb.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                if (!cell.getStyleClass().contains("fb-option-selected"))
                    cell.getStyleClass().add("fb-option-selected");
            } else {
                cell.getStyleClass().remove("fb-option-selected");
            }
        });

        // Click anywhere on cell selects the radio
        cell.setOnMouseClicked(e -> rb.setSelected(true));
        return cell;
    }

    @FXML
    private void onSubmit() {
        if (template == null) return;

        Toggle selected = ratingGroup != null ? ratingGroup.getSelectedToggle() : null;
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection required", "Please select a rating.");
            return;
        }

        String rating = ((RadioButton) selected).getText();

        models.FeedbackResponse response = new models.FeedbackResponse();
        response.setUser(AppSession.getCurrentUser());
        response.setWorkout(currentWorkout);
        response.setCoach(template.getCoach());
        response.setRating(rating);
        response.setComment(commentArea.getText());
        response.setCreatedAt(java.time.Instant.now());

        // Analyze sentiment + keywords via OpenAI
        String comment = commentArea.getText();
        if (comment != null && !comment.isBlank()) {
            utils.SentimentAnalyzer.AnalysisResult result = utils.SentimentAnalyzer.analyze(comment);
            response.setSentiment(result.sentiment());
            // Convert keywords to valid JSON array: "like, good" -> ["like","good"]
            String kw = result.keywords();
            if (kw != null && !kw.isBlank()) {
                StringBuilder json = new StringBuilder("[");
                String[] parts = kw.split(",");
                for (int i = 0; i < parts.length; i++) {
                    json.append("\"").append(parts[i].trim().replace("\"", "")).append("\"");
                    if (i < parts.length - 1) json.append(",");
                }
                json.append("]");
                response.setKeywords(json.toString());
            }
        }

        try {
            new services.FeedbackResponseService().createPrepared(response);
            showAlert(Alert.AlertType.INFORMATION, "Thank you!", "Your feedback has been submitted.");
            done();
        } catch (java.sql.SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not save: " + e.getMessage());
        }
    }

    private void done() {
        if (onDoneCallback != null) onDoneCallback.run();
        // Close the stage
        if (submitBtn.getScene() != null)
            ((javafx.stage.Stage) submitBtn.getScene().getWindow()).close();
    }

    private List<String> parseOptions(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        for (String part : trimmed.split(",")) {
            String opt = part.trim().replaceAll("^\"|\"$", "");
            if (!opt.isEmpty()) result.add(opt);
        }
        return result;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
