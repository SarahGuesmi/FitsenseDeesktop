package controllers;

import app.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import utils.WebAssets;
import models.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import services.ProfilePhysiqueService;
import services.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

public class SignInController {
    private static final String ADMIN_EMAIL = "admin@fitsense.com";

    @FXML
    private StackPane root;

    @FXML
    private Button homeButton;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button signInWithPasswordButton;

    @FXML
    private Button signUpButton;

    @FXML
    private ImageView heroImageView;

    private final UserService userService = new UserService();
    private final ProfilePhysiqueService profilePhysiqueService = new ProfilePhysiqueService();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @FXML
    private void initialize() {
        WebAssets.loadPublicAsset(heroImageView, WebAssets.HERO_SPORT_IMAGE);
    }

    @FXML
    private void onGoHome() {
        switchScene("/fxml/HomeView.fxml", "/css/home.css");
    }

    @FXML
    private void onSignInWithPassword() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", "Please enter email and password.");
            return;
        }

        try {
            User user = userService.findByEmail(email);
            if (user == null || user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
                showAlert(Alert.AlertType.ERROR, "Authentication Failed", "Invalid email or password.");
                return;
            }

            AppSession.setCurrentUser(user);

            String rolesJson = user.getRolesJson() == null ? "" : user.getRolesJson();
            if (ADMIN_EMAIL.equalsIgnoreCase(user.getEmail()) || rolesJson.contains("ROLE_ADMIN")) {
                switchScene("/fxml/AdminDashboardView.fxml", "/css/admin.css");
                return;
            }
            if (rolesJson.contains("ROLE_COACH")) {
                switchScene("/fxml/CoachDashboardView.fxml", "/css/admin.css");
                return;
            }

            boolean hasProfile = !profilePhysiqueService.findByUserId(user.getId()).isEmpty();
            if (hasProfile) {
                switchScene("/fxml/DashboardView.fxml", "/css/dashboard.css");
            } else {
                AppSession.resetOnboarding();
                switchScene("/fxml/HeightView.fxml", "/css/onboarding.css");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Sign In Failed", e.getMessage());
        }
    }

    @FXML
    private void onSignUp() {
        switchScene("/fxml/SignUpView.fxml", "/css/signup.css");
    }

    private void switchScene(String fxmlPath, String cssPath) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Scene scene = root.getScene();
            scene.setRoot(newRoot);
            scene.getStylesheets().setAll(Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm());
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            cause.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load view: " + fxmlPath + "\n\n" + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
