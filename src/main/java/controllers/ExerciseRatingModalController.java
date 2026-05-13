package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import models.Exercise;
import services.ExerciseRatingService;
import services.YouTubeService;
import app.AppSession;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ExerciseRatingModalController {

    @FXML private ImageView logoImage;
    @FXML private Label exerciseNameLabel;
    @FXML private Label star1, star2, star3, star4, star5;
    @FXML private Label ratingTextLabel;
    @FXML private Button skipButton;
    @FXML private Button submitButton;

    private int currentRating = 0;
    private Exercise exercise;
    private Consumer<Exercise> onDoneCallback;
    private final ExerciseRatingService ratingService = new ExerciseRatingService();

    public void initialize() {
        // Load sport-hero.png logo
        try {
            File logoFile = new File("C:/xampp2/htdocs/pidevassets/sport-hero.png");
            if (logoFile.exists()) {
                logoImage.setImage(new Image(logoFile.toURI().toString(), true));
            }
        } catch (Exception e) {
            // If logo not found, hide the image
            logoImage.setVisible(false);
            logoImage.setManaged(false);
        }
    }

    public void setExercise(Exercise exercise, Consumer<Exercise> onDoneCallback) {
        this.exercise = exercise;
        this.onDoneCallback = onDoneCallback;
        
        if (exercise != null) {
            exerciseNameLabel.setText(exercise.getNom() != null ? exercise.getNom() : "Exercise");
        }
        
        // Reset rating state
        currentRating = 0;
        renderStars(0);
        ratingTextLabel.setText("Tap a star to rate");
        submitButton.setDisable(true);
        submitButton.setStyle("-fx-background-color:#1F2937;-fx-text-fill:#4B5563;-fx-font-weight:700;-fx-font-size:14px;-fx-background-radius:12;-fx-padding:12 24;");
    }

    @FXML private void onStar1Clicked() { setRating(1); }
    @FXML private void onStar2Clicked() { setRating(2); }
    @FXML private void onStar3Clicked() { setRating(3); }
    @FXML private void onStar4Clicked() { setRating(4); }
    @FXML private void onStar5Clicked() { setRating(5); }

    @FXML private void onStar1Hover() { renderStars(1); }
    @FXML private void onStar2Hover() { renderStars(2); }
    @FXML private void onStar3Hover() { renderStars(3); }
    @FXML private void onStar4Hover() { renderStars(4); }
    @FXML private void onStar5Hover() { renderStars(5); }
    @FXML private void onStarExit() { renderStars(currentRating); }

    private void setRating(int rating) {
        currentRating = rating;
        renderStars(rating);
        
        String[] ratingTexts = {"", "Poor", "Fair", "Good", "Very Good", "Excellent"};
        ratingTextLabel.setText(ratingTexts[rating] + " (" + rating + "/5)");
        
        // Enable submit button
        submitButton.setDisable(false);
        submitButton.setStyle("-fx-background-color:linear-gradient(to right,#22C55E,#16A34A);-fx-text-fill:black;-fx-font-weight:700;-fx-font-size:14px;-fx-background-radius:12;-fx-padding:12 24;-fx-cursor:hand;");
    }

    private void renderStars(int count) {
        Label[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < 5; i++) {
            if (i < count) {
                stars[i].setText("★");
                stars[i].setStyle("-fx-font-size:48px;-fx-cursor:hand;-fx-text-fill:#F59E0B;");
            } else {
                stars[i].setText("☆");
                stars[i].setStyle("-fx-font-size:48px;-fx-cursor:hand;-fx-text-fill:#374151;");
            }
        }
    }

    @FXML
    private void onSubmitRating() {
        if (currentRating > 0) {
            // Save rating to database
            UUID userId = AppSession.getCurrentUser() != null ? AppSession.getCurrentUser().getId() : null;
            if (userId != null && exercise != null && exercise.getId() != null) {
                ratingService.saveRating(userId, exercise.getId(), currentRating);
            }
            
            // Show loading state
            submitButton.setDisable(true);
            submitButton.setText("⏳ Getting suggestions...");
            
            // Get video recommendations based on rating
            showVideoRecommendations();
        } else {
            closeModal();
        }
    }
    
    private void showVideoRecommendations() {
        if (exercise == null || exercise.getNom() == null) {
            System.out.println("[DEBUG] Exercise or exercise name is null");
            closeModal();
            return;
        }
        
        String exerciseName = exercise.getNom();
        boolean isNegativeRating = currentRating <= 2; // 1-2 stars = negative
        
        System.out.println("[DEBUG] Fetching videos for: " + exerciseName + ", negative: " + isNegativeRating + ", rating: " + currentRating);
        
        // Use only YouTube service, skip AI service to avoid the decommissioned model error
        YouTubeService.getInstance()
                .searchWorkoutVideos(exerciseName, isNegativeRating, 3)
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(videos -> Platform.runLater(() -> {
                    System.out.println("[DEBUG] Received " + videos.size() + " videos");
                    System.out.println("[DEBUG] About to close modal and show dialog");
                    closeModal(); // Close rating modal first
                    
                    // Add a small delay to ensure modal is closed
                    Platform.runLater(() -> {
                        if (!videos.isEmpty()) {
                            System.out.println("[DEBUG] Showing video suggestions dialog with " + videos.size() + " videos");
                            showVideoSuggestionsDialog(videos, isNegativeRating);
                        } else {
                            System.out.println("[DEBUG] No videos found, showing fallback message");
                            showNoVideosFoundDialog(isNegativeRating);
                        }
                    });
                }))
                .exceptionally(ex -> {
                    System.err.println("[YouTube] Error fetching videos: " + ex.getMessage());
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        closeModal();
                        // Show videos found dialog even if there was an error, since we know videos were found
                        showErrorDialog();
                    });
                    return null;
                });
    }
    
    private void showNoVideosFoundDialog(boolean isNegative) {
        Stage dialog = new Stage(StageStyle.DECORATED);
        utils.AppIconLoader.setIcon(dialog);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Feedback Received");
        dialog.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setPrefWidth(450);
        root.setStyle("-fx-background-color: #111827; -fx-border-color: #1F2937; -fx-border-radius: 12; -fx-background-radius: 12;");

        // Header with logo
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        
        try {
            File logoFile = new File("C:/xampp2/htdocs/pidevassets/sport-hero.png");
            if (logoFile.exists()) {
                ImageView headerLogo = new ImageView(new Image(logoFile.toURI().toString(), true));
                headerLogo.setFitWidth(40);
                headerLogo.setFitHeight(40);
                headerLogo.setPreserveRatio(true);
                header.getChildren().add(headerLogo);
            }
        } catch (Exception e) {
            // Logo not found, continue without it
        }

        Label titleLabel = new Label(isNegative ? 
                "🎯 Thanks for your feedback!" : 
                "🔥 Great job!");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 700;");

        Label messageLabel = new Label(isNegative ?
                "We've noted your feedback. Try adjusting the difficulty or technique for better results." :
                "Excellent work! Keep challenging yourself with similar exercises.");
        messageLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14px;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);

        header.getChildren().addAll(titleLabel, messageLabel);

        // Generic suggestions
        VBox suggestionsBox = new VBox(8);
        Label suggestionsTitle = new Label("💡 General Tips:");
        suggestionsTitle.setStyle("-fx-text-fill: #E5E7EB; -fx-font-size: 14px; -fx-font-weight: 600;");

        String[] tips = isNegative ? 
            new String[]{
                "• Start with shorter durations",
                "• Focus on proper form over speed", 
                "• Take breaks when needed",
                "• Consider beginner modifications"
            } :
            new String[]{
                "• Try increasing the intensity",
                "• Add more repetitions",
                "• Combine with other exercises",
                "• Track your progress"
            };

        suggestionsBox.getChildren().add(suggestionsTitle);
        for (String tip : tips) {
            Label tipLabel = new Label(tip);
            tipLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");
            suggestionsBox.getChildren().add(tipLabel);
        }

        Button closeBtn = new Button("Got it! ✓");
        closeBtn.setStyle("-fx-background-color: linear-gradient(to right, #22C55E, #16A34A); -fx-text-fill: black; -fx-font-weight: 700; -fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 12 24; -fx-cursor: hand;");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(header, suggestionsBox, closeBtn);

        Scene scene = new Scene(root, 500, 350);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private void showErrorDialog() {
        Stage dialog = new Stage(StageStyle.DECORATED);
        utils.AppIconLoader.setIcon(dialog);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Feedback Received");
        dialog.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setPrefWidth(400);
        root.setStyle("-fx-background-color: #111827; -fx-border-color: #1F2937; -fx-border-radius: 12; -fx-background-radius: 12;");

        Label titleLabel = new Label("✅ Rating Saved");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 700;");

        Label messageLabel = new Label("Thank you for your feedback! Your rating has been saved successfully.");
        messageLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14px;");
        messageLabel.setWrapText(true);

        Button closeBtn = new Button("Continue");
        closeBtn.setStyle("-fx-background-color: linear-gradient(to right, #22C55E, #16A34A); -fx-text-fill: black; -fx-font-weight: 700; -fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 12 24; -fx-cursor: hand;");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(titleLabel, messageLabel, closeBtn);

        Scene scene = new Scene(root, 450, 200);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private void showVideoSuggestionsDialog(List<YouTubeService.VideoResult> videos, boolean isNegative) {
        System.out.println("[DEBUG] showVideoSuggestionsDialog called with " + videos.size() + " videos, isNegative: " + isNegative);
        
        try {
            Stage dialog = new Stage(StageStyle.DECORATED);
            utils.AppIconLoader.setIcon(dialog);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Video Recommendations");
            dialog.setResizable(false);

            System.out.println("[DEBUG] Created dialog stage");

            VBox root = new VBox(16);
            root.setPadding(new Insets(24));
            root.setPrefWidth(500);
            root.setMaxWidth(500);
            root.setStyle("-fx-background-color: #111827; -fx-border-color: #1F2937; -fx-border-radius: 12; -fx-background-radius: 12;");

            // Header with logo and title
            VBox header = new VBox(8);
            header.setAlignment(Pos.CENTER);
            
            // Add logo
            try {
                File logoFile = new File("C:/xampp2/htdocs/pidevassets/sport-hero.png");
                if (logoFile.exists()) {
                    ImageView headerLogo = new ImageView(new Image(logoFile.toURI().toString(), true));
                    headerLogo.setFitWidth(40);
                    headerLogo.setFitHeight(40);
                    headerLogo.setPreserveRatio(true);
                    header.getChildren().add(headerLogo);
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] Logo loading failed: " + e.getMessage());
            }

            Label titleLabel = new Label(isNegative ? 
                    "🎯 Let's try something different!" : 
                    "🔥 Keep up the great work!");
            titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 700;");

            Label subtitleLabel = new Label(isNegative ?
                    "Here are some beginner-friendly alternatives for " + exercise.getNom() + ":" :
                    "Here are more " + exercise.getNom() + " workouts to challenge yourself:");
            subtitleLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14px;");
            subtitleLabel.setWrapText(true);
            subtitleLabel.setMaxWidth(450);

            header.getChildren().addAll(titleLabel, subtitleLabel);
            root.getChildren().add(header);

            System.out.println("[DEBUG] Added header, now adding " + videos.size() + " video cards");

            // Video list
            for (int i = 0; i < videos.size(); i++) {
                YouTubeService.VideoResult video = videos.get(i);
                System.out.println("[DEBUG] Adding video " + (i+1) + ": " + video.title());
                
                VBox videoCard = new VBox(6);
                videoCard.setStyle("-fx-background-color: #1F2937; -fx-border-color: #374151; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand;");
                videoCard.setCursor(Cursor.HAND);

                Label videoTitle = new Label("▶ " + video.title());
                videoTitle.setStyle("-fx-text-fill: #E5E7EB; -fx-font-size: 14px; -fx-font-weight: 600;");
                videoTitle.setWrapText(true);
                videoTitle.setMaxWidth(450);

                Label videoUrl = new Label(video.watchUrl());
                videoUrl.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");

                videoCard.getChildren().addAll(videoTitle, videoUrl);

                // Hover effect
                videoCard.setOnMouseEntered(e -> 
                    videoCard.setStyle("-fx-background-color: #374151; -fx-border-color: #22C55E; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand;"));
                videoCard.setOnMouseExited(e -> 
                    videoCard.setStyle("-fx-background-color: #1F2937; -fx-border-color: #374151; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand;"));

                // Click to open video
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

            // Close button
            Button closeBtn = new Button("Got it! ✓");
            closeBtn.setStyle("-fx-background-color: linear-gradient(to right, #22C55E, #16A34A); -fx-text-fill: black; -fx-font-weight: 700; -fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 12 24; -fx-cursor: hand;");
            closeBtn.setMaxWidth(Double.MAX_VALUE);
            closeBtn.setOnAction(e -> {
                System.out.println("[DEBUG] Close button clicked");
                dialog.close();
            });
            
            root.getChildren().add(closeBtn);

            Scene scene = new Scene(root, 550, Math.min(600, 200 + videos.size() * 80));
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            
            dialog.setScene(scene);
            
            System.out.println("[DEBUG] About to show dialog");
            dialog.showAndWait();
            System.out.println("[DEBUG] Dialog closed");
            
        } catch (Exception e) {
            System.err.println("[DEBUG] Error in showVideoSuggestionsDialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onSkipRating() {
        closeModal();
    }

    private void closeModal() {
        // Close the modal window
        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.close();
        
        // Call the callback to return to workout
        if (onDoneCallback != null && exercise != null) {
            onDoneCallback.accept(exercise);
        }
    }
}