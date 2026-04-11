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
import services.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

public class SignUpController {

    @FXML
    private StackPane root;

    @FXML
    private Button homeButton;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button signUpButton;

    @FXML
    private Button signInButton;

    @FXML
    private ImageView heroImageView;

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
        WebAssets.loadPublicAsset(heroImageView, WebAssets.HERO_SPORT_IMAGE);
    }

    @FXML
    private void onGoHome() {
        switchScene("/fxml/HomeView.fxml", "/css/home.css");
    }

    @FXML
    private void onSignUp() {
        String firstName = safeTrim(firstNameField.getText());
        String lastName = safeTrim(lastNameField.getText());
        String email = safeTrim(emailField.getText());
        String password = passwordField.getText() != null ? passwordField.getText().trim() : "";

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showWarning("Missing Information", "Please fill in all required fields.");
            return;
        }
        if (!isValidEmail(email)) {
            showWarning("Invalid Email", "Please enter a valid email address.");
            return;
        }
        if (password.length() < 6) {
            showWarning("Weak Password", "Password must contain at least 6 characters.");
            return;
        }

        if (userServiceInitError != null) {
            showError("Sign-up unavailable", "Services did not start correctly:\n" + userServiceInitError.getMessage());
            return;
        }
        if (userService == null || userService.cnx == null) {
            showError("Database", "No database connection. Check MySQL (default port 3308) or -Dfitsense.db.url=...");
            return;
        }

        try {
            if (userService.findByEmail(email) != null) {
                showWarning("Email Already Used", "An account with this email already exists.");
                return;
            }

            User user = new User();
            user.setFirstname(firstName);
            user.setLastname(lastName);
            user.setEmail(email);
            user.setPassword(password); // UserService hashes it with BCrypt
            user.setRolesJson("[\"ROLE_USER\"]");
            user.setAccountStatus("active");
            user.setPhoneNumber(null);
            user.setPhoto(null);
            user.setUsername(uniqueUsernameFromEmail(email));
            user.setGoogleAuthenticatorSecret(null);

            userService.createPrepared(user);
            AppSession.setCurrentUser(user);
            AppSession.resetOnboarding();
            showInfo("Account Created", "Your account was created successfully. Let's set up your profile.");
            switchScene("/fxml/HeightView.fxml", "/css/onboarding.css");
        } catch (SQLException e) {
            showError("Sign Up Failed", "Could not create the account: " + e.getMessage());
        }
    }

    @FXML
    private void onSignIn() {
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    private String uniqueUsernameFromEmail(String email) {
        int at = email.indexOf('@');
        String base = (at > 0 ? email.substring(0, at) : email).replaceAll("[^a-zA-Z0-9._-]", "");
        if (base.isBlank()) {
            base = "user";
        }
        if (base.length() > 48) {
            base = base.substring(0, 48);
        }
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

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
