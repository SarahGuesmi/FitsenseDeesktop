package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import services.PasswordResetService;
import utils.ResetTokenCallbackServer;

import java.util.Objects;

/**
 * ForgotPasswordController — handles the "enter your email" step.
 *
 * Flow:
 *   1. User enters email and clicks Send
 *   2. PasswordResetService generates a token and emails the reset link
 *   3. A success message is shown: "Check your Gmail inbox"
 *   4. A local HTTP server starts listening for the reset link click
 *   5. When the user clicks the link in Gmail, the browser hits localhost:8765
 *      and the app navigates to the ResetPasswordView with the token
 */
public class ForgotPasswordController {

    @FXML private StackPane root;
    @FXML private TextField emailField;
    @FXML private Label emailError;
    @FXML private Button sendButton;
    @FXML private VBox formBox;
    @FXML private VBox successBox;

    private PasswordResetService resetService;

    @FXML
    private void initialize() {
        try {
            resetService = new PasswordResetService();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (emailField != null) {
            emailField.textProperty().addListener((o, a, b) -> clearError());
        }
    }

    @FXML
    private void onSend() {
        clearError();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Email is required.");
            return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showError("Enter a valid email address.");
            return;
        }

        sendButton.setDisable(true);
        sendButton.setText("Sending...");

        // Run in background thread to avoid blocking the UI
        new Thread(() -> {
            try {
                boolean found = resetService.sendResetEmail(email);
                Platform.runLater(() -> {
                    if (!found) {
                        sendButton.setDisable(false);
                        sendButton.setText("SEND RESET LINK");
                        showError("No account found with this email.");
                        return;
                    }
                    // Show success message
                    formBox.setVisible(false);
                    formBox.setManaged(false);
                    successBox.setVisible(true);
                    successBox.setManaged(true);

                    // Start local callback server to catch the reset link click
                    ResetTokenCallbackServer.start(payload -> {
                        String[] parts = payload.split("\\|", 2);
                        Platform.runLater(() -> openResetPage(parts[0], parts[1]));
                    });
                });
            } catch (Exception e) {                Platform.runLater(() -> {
                    sendButton.setDisable(false);
                    sendButton.setText("SEND RESET LINK");
                    showError("Failed to send email: " + e.getMessage());
                });
            }
        }, "send-reset-email").start();
    }

    @FXML
    private void onBackToSignIn() {
        ResetTokenCallbackServer.stop();
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    private void openResetPage(String selector, String rawToken) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/fxml/ResetPasswordView.fxml")));
            Parent newRoot = loader.load();
            ResetPasswordController ctrl = loader.getController();
            ctrl.setCredentials(selector, rawToken);
            Scene scene = root.getScene();
            scene.setRoot(newRoot);
            scene.getStylesheets().setAll(
                    Objects.requireNonNull(getClass().getResource("/css/signin.css")).toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        emailError.setText(msg);
        emailError.setVisible(true);
        emailError.setManaged(true);
        emailField.getStyleClass().add("input-field-error");
    }

    private void clearError() {
        emailError.setText("");
        emailError.setVisible(false);
        emailError.setManaged(false);
        emailField.getStyleClass().remove("input-field-error");
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
