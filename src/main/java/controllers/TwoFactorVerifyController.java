package controllers;

import app.AppSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import models.User;
import services.LoginSecurityService;
import services.ProfilePhysiqueService;
import utils.SessionManager;
import utils.TotpUtil;

import java.util.Objects;

/**
 * TwoFactorVerifyController — shown after password login when 2FA is enabled.
 * The user must enter their TOTP code to proceed to their dashboard.
 */
public class TwoFactorVerifyController {

    private static final String ADMIN_EMAIL = "sarahguesmi223@gmail.com";

    @FXML private StackPane root;
    @FXML private TextField codeField;
    @FXML private Label     codeError;
    @FXML private Button    verifyBtn;

    /** The authenticated user (password already verified) — set before scene is shown */
    private User pendingUser;

    @FXML
    private void initialize() {
        if (codeField != null) {
            codeField.textProperty().addListener((o, a, b) -> clearError());
            // Auto-submit when 6 digits entered
            codeField.textProperty().addListener((o, a, b) -> {
                if (b != null && b.length() == 6) onVerify();
            });
        }
    }

    public void setPendingUser(User user) {
        this.pendingUser = user;
    }

    @FXML
    private void onVerify() {
        clearError();
        String code = codeField.getText() == null ? "" : codeField.getText().trim();

        if (code.length() != 6) {
            showError("Enter the 6-digit code from your authenticator app.");
            return;
        }

        if (!TotpUtil.verify(pendingUser.getGoogleAuthenticatorSecret(), code)) {
            showError("Incorrect code. Try again.");
            codeField.clear();
            return;
        }

        // Code correct — set session and navigate
        SessionManager.setCurrentUser(pendingUser);
        AppSession.setCurrentUser(pendingUser);

        // Record login + check unusual location in background
        new Thread(() -> {
            try {
                LoginSecurityService sec = new LoginSecurityService();
                boolean unusual = sec.recordAndCheck(pendingUser, LoginSecurityService.STATUS_SUCCESS);
                if (unusual) {
                    Platform.runLater(() -> {
                        SessionManager.setCurrentUser(null);
                        AppSession.setCurrentUser(null);
                        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
                    });
                    return;
                }
            } catch (Exception e) {
                System.err.println("2FA security check: " + e.getMessage());
            }
            Platform.runLater(this::navigateToDashboard);
        }, "2fa-security-check").start();
    }

    @FXML
    private void onBackToSignIn() {
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    private void navigateToDashboard() {
        String rolesJson = pendingUser.getRolesJson() == null ? "" : pendingUser.getRolesJson();

        if (ADMIN_EMAIL.equalsIgnoreCase(pendingUser.getEmail()) || rolesJson.contains("ROLE_ADMIN")) {
            switchScene("/fxml/AdminDashboardView.fxml", "/css/admin.css");
            return;
        }
        if (rolesJson.contains("ROLE_COACH")) {
            switchScene("/fxml/CoachDashboardView.fxml", "/css/admin.css");
            return;
        }
        try {
            ProfilePhysiqueService pps = new ProfilePhysiqueService();
            boolean hasProfile = !pps.findByUserId(pendingUser.getId()).isEmpty();
            if (hasProfile) {
                switchScene("/fxml/DashboardView.fxml", "/css/dashboard.css");
            } else {
                AppSession.resetOnboarding();
                switchScene("/fxml/HeightView.fxml", "/css/onboarding.css");
            }
        } catch (Exception e) {
            switchScene("/fxml/DashboardView.fxml", "/css/dashboard.css");
        }
    }

    private void showError(String msg) {
        codeError.setText(msg);
        codeError.setManaged(true);
        codeError.setVisible(true);
        codeField.getStyleClass().add("input-field-error");
    }

    private void clearError() {
        codeError.setText("");
        codeError.setManaged(false);
        codeError.setVisible(false);
        codeField.getStyleClass().remove("input-field-error");
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
