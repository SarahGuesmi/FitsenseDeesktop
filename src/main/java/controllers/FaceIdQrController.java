package controllers;

import app.AppSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import models.User;
import services.ProfilePhysiqueService;
import services.UserService;
import utils.FaceIdServer;
import utils.SessionManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * FaceIdQrController — shows the QR code overlay.
 *
 * Flow:
 *   1. Generates a random session token
 *   2. Builds URL: http://{localIp}:8766/faceid?token=XXX
 *   3. Loads QR image from api.qrserver.com
 *   4. Starts FaceIdServer to wait for phone submission
 *   5. When email received → verifies in DB → sets session → navigates to dashboard
 */
public class FaceIdQrController {

    private static final String ADMIN_EMAIL = "sarahguesmi223@gmail.com";

    @FXML private StackPane root;
    @FXML private ImageView qrImageView;
    @FXML private Label     statusLabel;
    @FXML private Label     urlLabel;

    /** Called by SignInController before showing the overlay */
    public void startSession() {
        String token   = generateToken();

        // Use ngrok URL if configured, otherwise fall back to local IP
        String baseUrl = loadNgrokUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            String localIp = FaceIdServer.getLocalIp();
            baseUrl = "http://" + localIp + ":" + FaceIdServer.PORT;
        }
        baseUrl = baseUrl.replaceAll("/$", ""); // strip trailing slash

        String url = baseUrl + "/faceid?token=" + token;

        // Load QR code image
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data="
                + URLEncoder.encode(url, StandardCharsets.UTF_8);

        new Thread(() -> {
            try {
                Image img = new Image(qrUrl, 200, 200, true, true, false);
                Platform.runLater(() -> {
                    qrImageView.setImage(img);
                    statusLabel.setText("Waiting for scan...");
                    if (urlLabel != null) urlLabel.setText(url);
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Could not load QR code."));
            }
        }, "faceid-qr-loader").start();

        // Start server — callback fires when phone submits email
        FaceIdServer.start(token, email -> Platform.runLater(() -> handleEmail(email)));
    }

    @FXML
    private void onCancel() {
        FaceIdServer.stop();
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void handleEmail(String email) {
        statusLabel.setText("Verifying...");
        try {
            UserService us = new UserService();
            User user = us.findByEmail(email);
            if (user == null) {
                statusLabel.setText("No account found for: " + email);
                FaceIdServer.start(generateToken(), e -> Platform.runLater(() -> handleEmail(e)));
                return;
            }

            SessionManager.setCurrentUser(user);
            AppSession.setCurrentUser(user);
            navigateToDashboard(user);

        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void navigateToDashboard(User user) {
        String roles = user.getRolesJson() == null ? "" : user.getRolesJson();
        if (ADMIN_EMAIL.equalsIgnoreCase(user.getEmail()) || roles.contains("ROLE_ADMIN")) {
            switchScene("/fxml/AdminDashboardView.fxml", "/css/admin.css");
            return;
        }
        if (roles.contains("ROLE_COACH")) {
            switchScene("/fxml/CoachDashboardView.fxml", "/css/admin.css");
            return;
        }
        try {
            ProfilePhysiqueService pps = new ProfilePhysiqueService();
            boolean hasProfile = !pps.findByUserId(user.getId()).isEmpty();
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

    private String generateToken() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String loadNgrokUrl() {
        try (java.io.InputStream in =
                getClass().getResourceAsStream("/config.properties")) {
            if (in == null) return null;
            java.util.Properties p = new java.util.Properties();
            p.load(in);
            return p.getProperty("faceid.ngrok.url", "");
        } catch (Exception e) { return null; }
    }

    private void switchScene(String fxml, String css) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxml)));
            Scene scene = root.getScene();
            scene.setRoot(newRoot);
            scene.getStylesheets().setAll(Objects.requireNonNull(getClass().getResource(css)).toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
