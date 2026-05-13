package controllers;

import app.AppSession;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import models.Questionnaire;
import models.Workout;
import services.FeedbackService;
import services.GroqAiService;
import services.WorkoutService;
import services.YouTubeService;
import utils.DbConnection;

import java.awt.Desktop;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class WorkoutFeedbackController {

    @FXML private Label workoutTitleLabel;
    @FXML private VBox optionsGrid;
    @FXML private TextArea commentArea;
    @FXML private Button submitBtn;

    private Workout currentWorkout;
    private Questionnaire template;
    private Runnable onDoneCallback;
    private int selectedStars = 0;
    private final Label[] starLabels = new Label[5];

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
        if (currentWorkout == null) {
            showNoFeedback("No workout provided.");
            return;
        }
        try {
            FeedbackService service = new FeedbackService(DbConnection.getInstance().getCnx());
            // Try by UUID first
            template = currentWorkout.getId() != null
                    ? service.findTemplateByWorkoutUuid(currentWorkout.getId())
                    : null;
            // Fallback: any available template
            if (template == null) {
                template = service.findTemplateByWorkoutId(0);
            }
            if (template == null) {
                showNoFeedback("No feedback questionnaire assigned to this workout.");
                return;
            }
            buildOptions(parseOptions(template.getOptions()));
        } catch (SQLException e) {
            showNoFeedback("Could not load feedback: " + e.getMessage());
        }
    }

    private void showNoFeedback(String msg) {
        submitBtn.setDisable(true);
        optionsGrid.getChildren().clear();
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px;");
        optionsGrid.getChildren().add(lbl);
    }

    private void buildOptions(List<String> options) {
        optionsGrid.getChildren().clear();
        selectedStars = 0;

        // Star rating row
        HBox starsRow = new HBox(8);
        starsRow.setAlignment(Pos.CENTER_LEFT);

        String[] labels = {"Very poor", "Poor", "Average", "Good", "Excellent"};

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            Label star = new Label("☆");
            star.getStyleClass().add("fb-star");
            star.setUserData(labels[i]);

            // Hover: fill up to hovered star
            star.setOnMouseEntered(e -> highlightStars(starIndex));
            star.setOnMouseExited(e -> highlightStars(selectedStars));
            star.setOnMouseClicked(e -> {
                selectedStars = starIndex;
                highlightStars(selectedStars);
            });

            starLabels[i] = star;
            starsRow.getChildren().add(star);
        }

        // Label showing current selection text
        Label selectionLabel = new Label("Select a rating");
        selectionLabel.getStyleClass().add("fb-star-label");
        selectionLabel.setId("starSelectionLabel");

        // Update label on click
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            starLabels[i].setOnMouseClicked(e -> {
                selectedStars = idx + 1;
                highlightStars(selectedStars);
                selectionLabel.setText(selectedStars + " – " + (String) starLabels[idx].getUserData());
            });
        }

        optionsGrid.getChildren().addAll(starsRow, selectionLabel);
    }

    private void highlightStars(int count) {
        for (int i = 0; i < 5; i++) {
            if (starLabels[i] == null) continue;
            if (i < count) {
                starLabels[i].setText("★");
                starLabels[i].getStyleClass().remove("fb-star");
                if (!starLabels[i].getStyleClass().contains("fb-star-filled"))
                    starLabels[i].getStyleClass().add("fb-star-filled");
            } else {
                starLabels[i].setText("☆");
                starLabels[i].getStyleClass().remove("fb-star-filled");
                if (!starLabels[i].getStyleClass().contains("fb-star"))
                    starLabels[i].getStyleClass().add("fb-star");
            }
        }
    }

    @FXML
    private void onSubmit() {
        if (template == null) return;

        if (selectedStars == 0) {
            showAlert(Alert.AlertType.WARNING, "Selection required", "Please select a star rating.");
            return;
        }

        String[] ratingLabels = {"Very poor", "Poor", "Average", "Good", "Excellent"};
        String rating = selectedStars + " " + ratingLabels[selectedStars - 1];

        models.FeedbackResponse response = new models.FeedbackResponse();
        response.setUser(AppSession.getCurrentUser());
        response.setWorkout(currentWorkout);
        response.setCoach(template.getCoach());
        response.setRating(rating);
        response.setComment(commentArea.getText());
        response.setCreatedAt(java.time.Instant.now());

        // Analyze sentiment + keywords via Groq
        String comment = commentArea.getText();
        if (comment != null && !comment.isBlank()) {
            utils.SentimentAnalyzer.AnalysisResult result = utils.SentimentAnalyzer.analyze(comment);
            response.setSentiment(result.sentiment());
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
        } catch (java.sql.SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not save: " + e.getMessage());
            return;
        }

        // Disable submit to prevent double-submit, show loading state
        submitBtn.setDisable(true);
        submitBtn.setText("⏳  Getting video suggestions...");

        final int stars = selectedStars;
        final String ratingLabel = ratingLabels[selectedStars - 1];
        final String workoutName = currentWorkout != null ? currentWorkout.getNom() : "your workout";

        // Use only YouTube service since AI service is currently unavailable
        System.out.println("[DEBUG] Starting YouTube search for workout: " + workoutName + ", negative rating: " + (stars <= 3));
        YouTubeService.getInstance()
                .searchWorkoutVideos(workoutName, stars <= 3, 3)
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(videos -> Platform.runLater(() -> {
                    System.out.println("[DEBUG] YouTube search completed, found " + videos.size() + " videos");
                    System.out.println("[YouTube] Videos found: " + videos.size());
                    // Create a simple JSON object for the dialog (no AI needed)
                    JsonObject simpleJson = new com.google.gson.JsonObject();
                    if (stars <= 3) {
                        simpleJson.addProperty("message", "Thanks for your feedback! Let's find some alternatives that might work better for you.");
                        simpleJson.addProperty("followup_question", "What specific aspect was challenging? This will help us recommend better workouts.");
                    } else {
                        simpleJson.addProperty("message", "Great job completing this workout! Keep up the excellent work.");
                        simpleJson.addProperty("followup_question", "");
                    }
                    System.out.println("[DEBUG] About to show advice dialog");
                    showAiAdviceDialog(simpleJson, stars, videos);
                    done();
                }))
                .exceptionally(ex -> {
                    System.err.println("[YouTube] Error: " + ex.getMessage());
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Thank you!", "Your feedback has been submitted.");
                        done();
                    });
                    return null;
                });
    }

    /**
     * Shows the AI advice dialog after feedback submission.
     * - Negative (1-3 stars): empathetic message + follow-up question + YouTube alternatives
     * - Positive (4-5 stars): congratulatory message + YouTube similar videos
     */
    private void showAiAdviceDialog(JsonObject json, int stars,
                                    List<YouTubeService.VideoResult> videos) {
        System.out.println("[DEBUG] showAiAdviceDialog called with " + videos.size() + " videos, stars: " + stars);
        
        boolean negative = stars <= 3;

        String message = getJsonString(json, "message");
        String followupQuestion = getJsonString(json, "followup_question");

        System.out.println("[DEBUG] Creating dialog stage");
        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(16);
        root.getStyleClass().add("ai-dialog-root");
        root.setPadding(new Insets(28));
        root.setPrefWidth(480);
        root.setMaxWidth(480);

        // Header
        Label icon = new Label(negative ? "🤖  AI Coach Feedback" : "🤖  AI Coach");
        icon.getStyleClass().add("ai-dialog-title");

        // Main message
        Label msgLabel = new Label(message.isBlank() ? "Thank you for your feedback!" : message);
        msgLabel.getStyleClass().add("ai-dialog-message");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(420);

        root.getChildren().addAll(icon, msgLabel);

        // Follow-up question (only for negative ratings)
        if (negative && followupQuestion != null && !followupQuestion.isBlank()) {
            Label qLabel = new Label("💬  " + followupQuestion);
            qLabel.getStyleClass().add("ai-dialog-question");
            qLabel.setWrapText(true);
            qLabel.setMaxWidth(420);
            root.getChildren().add(qLabel);
        }

        // YouTube video suggestions
        if (!videos.isEmpty()) {
            Label ytTitle = new Label(negative
                    ? "▶  Recommended videos for you:"
                    : "▶  Keep going with these:");
            ytTitle.getStyleClass().add("ai-dialog-section");
            root.getChildren().add(ytTitle);

            for (YouTubeService.VideoResult video : videos) {
                VBox videoCard = new VBox(4);
                videoCard.getStyleClass().add("yt-video-card");
                videoCard.setCursor(Cursor.HAND);

                Label titleLbl = new Label("🎬  " + video.title());
                titleLbl.getStyleClass().add("yt-video-title");
                titleLbl.setWrapText(true);
                titleLbl.setMaxWidth(400);

                Label urlLbl = new Label(video.watchUrl());
                urlLbl.getStyleClass().add("yt-video-url");

                videoCard.getChildren().addAll(titleLbl, urlLbl);

                // Click → open in system browser
                videoCard.setOnMouseClicked(e -> {
                    try {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(new URI(video.watchUrl()));
                        }
                    } catch (Exception ex) {
                        System.err.println("Could not open browser: " + ex.getMessage());
                    }
                });

                root.getChildren().add(videoCard);
            }
        }

        // Close button
        Button closeBtn = new Button("Got it  ✓");
        closeBtn.getStyleClass().add("ai-dialog-close-btn");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());
        root.getChildren().add(closeBtn);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(
                getClass().getResource("/css/feedback.css").toExternalForm());
        dialog.setScene(scene);
        
        System.out.println("[DEBUG] About to show dialog with showAndWait()");
        dialog.showAndWait();
        System.out.println("[DEBUG] Dialog closed");
    }

    private String getJsonString(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) return "";
        return json.get(key).getAsString();
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
