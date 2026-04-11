package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import utils.WebAssets;

import java.util.Objects;

/**
 * Controller for the FitSense home / landing screen ({@code HomeView.fxml}).
 */
public class HomeController {

    @FXML
    private StackPane root;

    @FXML
    private Button signUpButton;

    @FXML
    private Button signInButton;

    @FXML
    private Button startJourneyButton;

    @FXML
    private ImageView logoImageView;

    @FXML
    private MediaView backgroundMediaView;

    private MediaPlayer backgroundMediaPlayer;

    @FXML
    private void initialize() {
        setupBackgroundVideo();
        // Optional: load logo from classpath when you add resources/images/fitsense-logo.png
        // var url = getClass().getResource("/images/fitsense-logo.png");
        // if (url != null) logoImageView.setImage(new Image(url.toExternalForm()));
    }

    private void setupBackgroundVideo() {
        if (backgroundMediaView == null || root == null) {
            return;
        }
        backgroundMediaView.fitWidthProperty().bind(root.widthProperty());
        backgroundMediaView.fitHeightProperty().bind(root.heightProperty());
        backgroundMediaView.setPreserveRatio(false);

        String videoUrl = WebAssets.assetUrl(WebAssets.HOME_GYM_VIDEO);
        try {
            Media media = new Media(videoUrl);
            backgroundMediaPlayer = new MediaPlayer(media);
            backgroundMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundMediaPlayer.setMute(true);
            backgroundMediaPlayer.setAutoPlay(true);
            backgroundMediaPlayer.setOnError(() ->
                    System.err.println("Home background video error: " + backgroundMediaPlayer.getError()));
            backgroundMediaView.setMediaPlayer(backgroundMediaPlayer);
        } catch (RuntimeException e) {
            System.err.println("Could not start background video: " + videoUrl + " — " + e.getMessage());
        }
    }

    @FXML
    private void onSignUp() {
        switchScene("/fxml/SignUpView.fxml", "/css/signup.css");
    }

    @FXML
    private void onSignIn() {
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    @FXML
    private void onStartJourney() {
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    private void switchScene(String fxmlPath, String cssPath) {
        disposeBackgroundVideo();
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Scene scene = root.getScene();
            scene.setRoot(newRoot);
            scene.getStylesheets().setAll(Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Failed to load " + fxmlPath);
            Throwable c = e;
            while (c.getCause() != null) c = c.getCause();
            alert.setContentText(c.getClass().getSimpleName() + ": " + c.getMessage()
                    + "\n\nCheck the Run console for the full stack trace.");
            alert.showAndWait();
        }
    }

    private void disposeBackgroundVideo() {
        if (backgroundMediaView != null) {
            backgroundMediaView.setMediaPlayer(null);
        }
        if (backgroundMediaPlayer != null) {
            backgroundMediaPlayer.stop();
            backgroundMediaPlayer.dispose();
            backgroundMediaPlayer = null;
        }
    }
}
