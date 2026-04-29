package controllers;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import services.PasswordResetService;

import java.util.Objects;

/**
 * ResetPasswordController — handles the "enter new password" step.
 *
 * Receives the token from ForgotPasswordController after the user
 * clicks the email link. Validates the new password, updates it in DB,
 * then auto-redirects to SignIn after 2 seconds.
 */
public class ResetPasswordController {

    @FXML private StackPane root;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label newPasswordError;
    @FXML private Label confirmPasswordError;
    @FXML private Button resetButton;
    @FXML private VBox formBox;
    @FXML private VBox successBox;

    private String selector;
    private String rawToken;
    private PasswordResetService resetService;

    @FXML
    private void initialize() {
        try {
            resetService = new PasswordResetService();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (newPasswordField != null)
            newPasswordField.textProperty().addListener((o, a, b) -> clearError(newPasswordField, newPasswordError));
        if (confirmPasswordField != null)
            confirmPasswordField.textProperty().addListener((o, a, b) -> clearError(confirmPasswordField, confirmPasswordError));
    }

    /** Called by ForgotPasswordController before the scene is shown */
    public void setCredentials(String selector, String rawToken) {
        this.selector = selector;
        this.rawToken = rawToken;
    }

    @FXML
    private void onReset() {
        clearAllErrors();

        String newPass     = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        boolean valid = true;
        if (newPass.isEmpty()) {
            showError(newPasswordField, newPasswordError, "Password is required.");
            valid = false;
        } else if (newPass.length() < 6) {
            showError(newPasswordField, newPasswordError, "Password must be at least 6 characters.");
            valid = false;
        }
        if (confirmPass.isEmpty()) {
            showError(confirmPasswordField, confirmPasswordError, "Please confirm your password.");
            valid = false;
        } else if (!newPass.equals(confirmPass)) {
            showError(confirmPasswordField, confirmPasswordError, "Passwords do not match.");
            valid = false;
        }
        if (!valid) return;

        resetButton.setDisable(true);
        resetButton.setText("Saving...");

        new Thread(() -> {
            try {
                resetService.resetPassword(selector, rawToken, newPass);
                javafx.application.Platform.runLater(() -> {
                    formBox.setVisible(false);
                    formBox.setManaged(false);
                    successBox.setVisible(true);
                    successBox.setManaged(true);

                    // Auto-redirect to sign in after 2 seconds
                    PauseTransition pause = new PauseTransition(Duration.seconds(2));
                    pause.setOnFinished(e -> switchScene("/fxml/SignInView.fxml", "/css/signin.css"));
                    pause.play();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    resetButton.setDisable(false);
                    resetButton.setText("RESET PASSWORD");
                    showError(newPasswordField, newPasswordError,
                            e.getMessage() != null ? e.getMessage() : "An error occurred.");
                });
            }
        }, "reset-password").start();
    }

    @FXML
    private void onBackToSignIn() {
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    private void showError(javafx.scene.Node field, Label label, String msg) {
        if (field != null) field.getStyleClass().add("input-field-error");
        if (label != null) { label.setText(msg); label.setVisible(true); label.setManaged(true); }
    }

    private void clearError(javafx.scene.Node field, Label label) {
        if (field != null) field.getStyleClass().remove("input-field-error");
        if (label != null) { label.setText(""); label.setVisible(false); label.setManaged(false); }
    }

    private void clearAllErrors() {
        clearError(newPasswordField, newPasswordError);
        clearError(confirmPasswordField, confirmPasswordError);
    }

    private void switchScene(String fxmlPath, String cssPath) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Scene scene = root.getScene();
            scene.setRoot(newRoot);
            scene.getStylesheets().setAll(
                    Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
