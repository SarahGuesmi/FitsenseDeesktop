package controllers;

import app.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import models.User;

import java.io.IOException;
import java.util.Objects;

public class DashboardController {
    @FXML
    private StackPane root;
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label userNameLabel;

    @FXML
    private void initialize() {
        User currentUser = AppSession.getCurrentUser();
        if (currentUser != null) {
            String name = (currentUser.getFirstname() == null ? "" : currentUser.getFirstname()).trim();
            if (name.isEmpty()) {
                name = currentUser.getEmail();
            }
            userNameLabel.setText(name);
            welcomeLabel.setText("Welcome back, " + name + "!");
        }
    }

    @FXML
    private void onLogout() {
        AppSession.setCurrentUser(null);
        AppSession.resetOnboarding();
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    private void switchScene(String fxmlPath, String cssPath) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Scene scene = root.getScene();
            scene.setRoot(newRoot);
            scene.getStylesheets().setAll(Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm());
        } catch (IOException e) {
            throw new RuntimeException("Failed to switch scene", e);
        }
    }
}
