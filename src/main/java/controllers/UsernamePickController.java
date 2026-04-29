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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import models.User;
import services.GroqService;
import services.UserService;
import utils.DbConnection;
import utils.UuidUtil;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;

/**
 * UsernamePickController — shows 5 AI-generated sport usernames.
 * The user picks one (or types a custom one) then proceeds to avatar selection.
 */
public class UsernamePickController {

    @FXML private StackPane root;
    @FXML private FlowPane  chipsPane;
    @FXML private Button    regenerateBtn;
    @FXML private Button    confirmBtn;
    @FXML private TextField customField;
    @FXML private Label     loadingLabel;
    @FXML private Label     errorLabel;

    private String selectedUsername = null;

    @FXML
    private void initialize() {
        generateUsernames();
        // Selecting custom field clears chip selection
        customField.textProperty().addListener((o, a, b) -> {
            if (!b.isBlank()) clearChipSelection();
        });
    }

    @FXML
    private void onRegenerate() {
        selectedUsername = null;
        customField.clear();
        generateUsernames();
    }

    @FXML
    private void onBack() {
        switchScene("/fxml/ObjectiveView.fxml", "/css/onboarding.css");
    }

    @FXML
    private void onConfirm() {
        hideError();

        // Custom field takes priority if filled
        String custom = customField.getText() == null ? "" : customField.getText().trim();
        String chosen = custom.isBlank() ? selectedUsername : custom.toUpperCase()
                .replaceAll("[^A-Z0-9]", "");

        if (chosen == null || chosen.isBlank()) {
            showError("Please select a username or enter a custom one.");
            return;
        }
        if (chosen.length() < 3) {
            showError("Username must be at least 3 characters.");
            return;
        }

        // Check uniqueness
        try {
            UserService us = new UserService();
            if (us.findByUsername(chosen) != null) {
                showError("That username is already taken. Try another.");
                return;
            }
        } catch (Exception e) {
            showError("Could not verify username: " + e.getMessage());
            return;
        }

        // Save to DB
        try {
            User user = AppSession.getCurrentUser();
            String sql = "UPDATE `app_user` SET `username` = ? WHERE `id` = ?";
            try (PreparedStatement stmt = DbConnection.getInstance().getCnx().prepareStatement(sql)) {
                stmt.setString(1, chosen);
                stmt.setBytes(2, UuidUtil.toBytes16(user.getId()));
                stmt.executeUpdate();
            }
            user.setUsername(chosen);
        } catch (Exception e) {
            showError("Failed to save username: " + e.getMessage());
            return;
        }

        // Navigate to avatar pick
        switchScene("/fxml/AvatarPickView.fxml", "/css/onboarding.css");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void generateUsernames() {
        setLoading(true);
        chipsPane.getChildren().clear();

        User user = AppSession.getCurrentUser();
        String first = user != null && user.getFirstname() != null ? user.getFirstname() : "Fit";
        String last  = user != null && user.getLastname()  != null ? user.getLastname()  : "User";

        new Thread(() -> {
            List<String> names = new GroqService().generateUsernames(first, last);

            // Fallback if Groq fails
            if (names.isEmpty()) {
                String f = first.toUpperCase();
                String l = last.toUpperCase();
                names = List.of(
                        f + "GAINS", "IRON" + f, f + l.charAt(0) + "FIT",
                        "SWEAT" + f, f + "BEAST");
            }

            List<String> finalNames = names;
            Platform.runLater(() -> {
                setLoading(false);
                for (String name : finalNames) {
                    Button chip = new Button(name);
                    chip.getStyleClass().add("username-chip");
                    chip.setOnAction(e -> selectChip(chip, name));
                    chipsPane.getChildren().add(chip);
                }
            });
        }, "groq-usernames").start();
    }

    private void selectChip(Button chip, String name) {
        clearChipSelection();
        chip.getStyleClass().add("username-chip-selected");
        selectedUsername = name;
        customField.clear();
        hideError();
    }

    private void clearChipSelection() {
        chipsPane.getChildren().forEach(n -> n.getStyleClass().remove("username-chip-selected"));
        selectedUsername = null;
    }

    private void setLoading(boolean loading) {
        loadingLabel.setManaged(loading);
        loadingLabel.setVisible(loading);
        regenerateBtn.setDisable(loading);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
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
