package controllers;

import app.AppSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.LoginAttempt;
import models.User;
import services.LoginSecurityService;
import utils.TotpUtil;
import utils.DbConnection;
import utils.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * TwoFactorSetupController — manages the 2FA settings panel.
 *
 * States:
 *   - 2FA disabled → shows status card + "Enable 2FA" button
 *   - Enabling     → shows QR code + secret + verify step
 *   - 2FA enabled  → shows status card + "Disable 2FA" button
 *   - Disabling    → shows code confirmation panel
 */
public class TwoFactorSetupController {

    @FXML private Label  statusIcon;
    @FXML private Label  statusTitle;
    @FXML private Label  statusDesc;
    @FXML private Button toggleBtn;

    @FXML private VBox   setupPanel;
    @FXML private ImageView qrImageView;
    @FXML private Label  qrLoadingLabel;
    @FXML private Label  secretLabel;
    @FXML private TextField verifyCodeField;
    @FXML private Label  verifyError;

    @FXML private VBox   disablePanel;
    @FXML private TextField disableCodeField;
    @FXML private Label  disableError;

    @FXML private VBox   loginHistoryBox;

    /** Pending secret — generated when setup starts, saved only after verification */
    private String pendingSecret;

    @FXML
    private void initialize() {
        refreshStatus();
        loadLoginHistory();
    }

    // ── Toggle button ─────────────────────────────────────────────────────────

    @FXML
    private void onToggle() {
        User user = AppSession.getCurrentUser();
        if (user == null) return;

        boolean isEnabled = user.getGoogleAuthenticatorSecret() != null
                && !user.getGoogleAuthenticatorSecret().isBlank();

        if (isEnabled) {
            // Show disable confirmation
            showPanel(disablePanel);
            hidePanel(setupPanel);
        } else {
            // Start setup flow
            startSetup(user);
            showPanel(setupPanel);
            hidePanel(disablePanel);
        }
    }

    // ── Setup flow ────────────────────────────────────────────────────────────

    private void startSetup(User user) {
        pendingSecret = TotpUtil.generateSecret();
        secretLabel.setText(pendingSecret);
        qrLoadingLabel.setVisible(true);
        qrImageView.setVisible(false);

        String qrUrl = TotpUtil.buildQrCodeUrl(pendingSecret, user.getEmail(), "FitSense");

        // Load QR image in background
        new Thread(() -> {
            try {
                Image img = new Image(qrUrl, 200, 200, true, true, true);
                Platform.runLater(() -> {
                    qrImageView.setImage(img);
                    qrImageView.setVisible(true);
                    qrLoadingLabel.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> qrLoadingLabel.setText("Could not load QR code."));
            }
        }, "qr-loader").start();

        verifyCodeField.clear();
        clearError(verifyError);
    }

    @FXML
    private void onVerifyAndActivate() {
        clearError(verifyError);
        String code = verifyCodeField.getText() == null ? "" : verifyCodeField.getText().trim();

        if (code.length() != 6) {
            showError(verifyError, "Enter the 6-digit code from your app.");
            return;
        }
        if (!TotpUtil.verify(pendingSecret, code)) {
            showError(verifyError, "Incorrect code. Make sure your phone clock is accurate.");
            return;
        }

        // Save secret to DB
        try {
            saveSecret(AppSession.getCurrentUser(), pendingSecret);
            AppSession.getCurrentUser().setGoogleAuthenticatorSecret(pendingSecret);
            pendingSecret = null;
            hidePanel(setupPanel);
            refreshStatus();
        } catch (Exception e) {
            showError(verifyError, "Failed to save: " + e.getMessage());
        }
    }

    // ── Disable flow ──────────────────────────────────────────────────────────

    @FXML
    private void onConfirmDisable() {
        clearError(disableError);
        String code = disableCodeField.getText() == null ? "" : disableCodeField.getText().trim();
        User user = AppSession.getCurrentUser();

        if (code.length() != 6) {
            showError(disableError, "Enter the 6-digit code from your app.");
            return;
        }
        if (!TotpUtil.verify(user.getGoogleAuthenticatorSecret(), code)) {
            showError(disableError, "Incorrect code.");
            return;
        }

        try {
            saveSecret(user, null);
            user.setGoogleAuthenticatorSecret(null);
            hidePanel(disablePanel);
            refreshStatus();
        } catch (Exception e) {
            showError(disableError, "Failed to disable: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelDisable() {
        hidePanel(disablePanel);
        disableCodeField.clear();
        clearError(disableError);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void refreshStatus() {
        User user = AppSession.getCurrentUser();
        boolean enabled = user != null
                && user.getGoogleAuthenticatorSecret() != null
                && !user.getGoogleAuthenticatorSecret().isBlank();

        if (enabled) {
            statusIcon.setText("🔒");
            statusTitle.setText("2FA is Enabled");
            statusDesc.setText("Your account is protected with two-factor authentication.");
            toggleBtn.setText("Disable 2FA");
            toggleBtn.getStyleClass().removeAll("enabled");
            toggleBtn.getStyleClass().add("enabled");
        } else {
            statusIcon.setText("🔓");
            statusTitle.setText("2FA is Disabled");
            statusDesc.setText("Your account is protected by password only.");
            toggleBtn.setText("Enable 2FA");
            toggleBtn.getStyleClass().remove("enabled");
        }
    }

    private void showPanel(VBox panel) {
        panel.setManaged(true);
        panel.setVisible(true);
    }

    private void hidePanel(VBox panel) {
        panel.setManaged(false);
        panel.setVisible(false);
    }

    private void showError(Label label, String msg) {
        label.setText(msg);
        label.setManaged(true);
        label.setVisible(true);
    }

    private void clearError(Label label) {
        label.setText("");
        label.setManaged(false);
        label.setVisible(false);
    }

    // ── Login history ─────────────────────────────────────────────────────────

    private void loadLoginHistory() {
        User user = AppSession.getCurrentUser();
        if (user == null || loginHistoryBox == null) return;

        new Thread(() -> {
            try {
                LoginSecurityService svc = new LoginSecurityService();
                List<LoginAttempt> history = svc.getRecentHistory(user.getId(), 10);
                Platform.runLater(() -> renderHistory(history));
            } catch (Exception e) {
                System.err.println("loadLoginHistory: " + e.getMessage());
            }
        }, "load-login-history").start();
    }

    private void renderHistory(List<LoginAttempt> history) {
        loginHistoryBox.getChildren().clear();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

        if (history.isEmpty()) {
            Label empty = new Label("No login history yet.");
            empty.getStyleClass().add("tfa-step-desc");
            loginHistoryBox.getChildren().add(empty);
            return;
        }

        for (LoginAttempt a : history) {
            boolean success = "SUCCESS".equals(a.getStatus());

            HBox row = new HBox(14);
            row.getStyleClass().add("tfa-history-row");
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Status icon
            Label icon = new Label(success ? "✅" : "❌");
            icon.getStyleClass().add("tfa-history-icon");

            // Location + time
            VBox info = new VBox(3);
            javafx.scene.layout.HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

            Label location = new Label(a.locationString());
            location.getStyleClass().add("tfa-history-location");

            String ispText = a.getIsp() != null && !a.getIsp().isBlank() ? "  ·  " + a.getIsp() : "";
            Label detail = new Label(a.getIpAddress() + ispText);
            detail.getStyleClass().add("tfa-history-detail");

            info.getChildren().addAll(location, detail);

            // Time
            Label time = new Label(a.getTimestamp() != null ? a.getTimestamp().format(fmt) : "");
            time.getStyleClass().add("tfa-history-time");

            // Status badge
            Label badge = new Label(success ? "Success" : "Failed");
            badge.getStyleClass().add(success ? "tfa-badge-success" : "tfa-badge-fail");

            row.getChildren().addAll(icon, info, time, badge);
            loginHistoryBox.getChildren().add(row);
        }
    }

    // ── DB ────────────────────────────────────────────────────────────────────

    private void saveSecret(User user, String secret) throws Exception {
        Connection cnx = DbConnection.getInstance().getCnx();
        String sql = "UPDATE `fitsense`.`app_user` SET `google_authenticator_secret` = ? WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, secret);
            stmt.setBytes(2, UuidUtil.toBytes16(user.getId()));
            stmt.executeUpdate();
        }
    }
}
