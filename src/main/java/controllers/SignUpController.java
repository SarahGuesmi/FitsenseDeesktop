package controllers;

import app.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import models.User;
import services.UserService;
import utils.WebAssets;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

public class SignUpController {

    @FXML private StackPane root;
    @FXML private Button homeButton;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button signUpButton;
    @FXML private Button signInButton;
    @FXML private ImageView heroImageView;

    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label emailError;
    @FXML private Label passwordError;

    private UserService userService;
    private Throwable userServiceInitError;

    @FXML
    private void initialize() {
        try {
            userService = new UserService();
        } catch (Throwable t) {
            userServiceInitError = t;
            t.printStackTrace();
        }
        try {
            WebAssets.loadPublicAsset(heroImageView, WebAssets.HERO_SPORT_IMAGE);
        } catch (Exception e) {
            System.out.println("Impossible de charger l'image hero : " + e.getMessage());
        }
        // Clear errors on typing
        if (firstNameField != null) firstNameField.textProperty().addListener((o, a, b) -> clearError(firstNameField, firstNameError));
        if (lastNameField != null)  lastNameField.textProperty().addListener((o, a, b) -> clearError(lastNameField, lastNameError));
        if (emailField != null)     emailField.textProperty().addListener((o, a, b) -> clearError(emailField, emailError));
        if (passwordField != null)  passwordField.textProperty().addListener((o, a, b) -> clearError(passwordField, passwordError));
    }

    @FXML
    private void onGoHome() {
        switchScene("/fxml/HomeView.fxml", "/css/home.css");
    }

    @FXML
    private void onSignUp() {
        clearAllErrors();

        String firstName = safeTrim(firstNameField.getText());
        String lastName  = safeTrim(lastNameField.getText());
        String email     = safeTrim(emailField.getText());
        String password  = passwordField.getText() != null ? passwordField.getText() : "";

        boolean valid = true;

        if (firstName.isEmpty()) {
            showError(firstNameField, firstNameError, "First name is required.");
            valid = false;
        } else if (!firstName.matches("^[\\p{L} '-]+$")) {
            showError(firstNameField, firstNameError, "Only letters, spaces and hyphens.");
            valid = false;
        }

        if (lastName.isEmpty()) {
            showError(lastNameField, lastNameError, "Last name is required.");
            valid = false;
        } else if (!lastName.matches("^[\\p{L} '-]+$")) {
            showError(lastNameField, lastNameError, "Only letters, spaces and hyphens.");
            valid = false;
        }

        if (email.isEmpty()) {
            showError(emailField, emailError, "Email is required.");
            valid = false;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showError(emailField, emailError, "Enter a valid email address.");
            valid = false;
        }

        if (password.isEmpty()) {
            showError(passwordField, passwordError, "Password is required.");
            valid = false;
        } else if (password.length() < 6) {
            showError(passwordField, passwordError, "Password must be at least 6 characters.");
            valid = false;
        }

        if (!valid) return;

        if (userServiceInitError != null) {
            showError(emailField, emailError, "Service error: " + userServiceInitError.getMessage());
            return;
        }
        if (userService == null || userService.cnx == null) {
            showError(emailField, emailError, "No database connection. Check MySQL.");
            return;
        }

        try {
            if (userService.findByEmail(email) != null) {
                showError(emailField, emailError, "An account with this email already exists.");
                return;
            }

            User user = new User();
            user.setFirstname(firstName);
            user.setLastname(lastName);
            user.setEmail(email);
            user.setPassword(password);
            user.setRolesJson("[\"ROLE_USER\"]");
            user.setAccountStatus("active");
            user.setPhoneNumber(null);
            user.setPhoto(null);
            user.setUsername(uniqueUsernameFromEmail(email));
            user.setGoogleAuthenticatorSecret(null);

            userService.createPrepared(user);
            AppSession.setCurrentUser(user);
            AppSession.resetOnboarding();

            // Notify admin
            try {
                services.NotificationService ns = new services.NotificationService();
                models.Notification notif = new models.Notification(
                        "NEW_USER",
                        "New athlete registered: " + firstName + " " + lastName,
                        email,
                        java.time.LocalDateTime.now());
                ns.create(notif);
            } catch (Exception ignored) {}

            switchScene("/fxml/HeightView.fxml", "/css/onboarding.css");

        } catch (SQLException e) {
            showError(emailField, emailError, "Could not create account: " + e.getMessage());
        }
    }

    @FXML
    private void onSignIn() {
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void showError(javafx.scene.Node field, Label label, String msg) {
        if (field != null) field.getStyleClass().add("input-field-error");
        if (label != null) {
            label.setText(msg);
            label.setManaged(true);
            label.setVisible(true);
        }
    }

    private void clearError(javafx.scene.Node field, Label label) {
        if (field != null) field.getStyleClass().remove("input-field-error");
        if (label != null) {
            label.setText("");
            label.setManaged(false);
            label.setVisible(false);
        }
    }

    private void clearAllErrors() {
        clearError(firstNameField, firstNameError);
        clearError(lastNameField, lastNameError);
        clearError(emailField, emailError);
        clearError(passwordField, passwordError);
    }

    private String uniqueUsernameFromEmail(String email) {
        int at = email.indexOf('@');
        String base = (at > 0 ? email.substring(0, at) : email).replaceAll("[^a-zA-Z0-9._-]", "");
        if (base.isBlank()) base = "user";
        if (base.length() > 48) base = base.substring(0, 48);
        String candidate = base;
        int suffix = 0;
        try {
            while (userService.findByUsername(candidate) != null) {
                suffix++;
                String tail = "_" + suffix;
                candidate = base.substring(0, Math.max(1, Math.min(base.length(), 48 - tail.length()))) + tail;
            }
        } catch (SQLException ignored) {
            return base + "_" + System.currentTimeMillis();
        }
        return candidate;
    }

    private void switchScene(String fxmlPath, String cssPath) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Scene scene = root.getScene();
            scene.setRoot(newRoot);
            scene.getStylesheets().setAll(Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm());
        } catch (IOException e) {
            throw new RuntimeException("Failed to switch scene to " + fxmlPath, e);
        }
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
