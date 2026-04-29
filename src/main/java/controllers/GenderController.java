package controllers;

import app.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.Objects;

public class GenderController {
    @FXML
    private StackPane root;
    @FXML
    private Button maleButton;
    @FXML
    private Button femaleButton;

    private String selectedGender;

    @FXML
    private void initialize() {
        selectedGender = AppSession.getOnboardingData().getGender();
        applySelectionStyles();
    }

    @FXML
    private void onSelectMale() {
        selectedGender = "male";
        applySelectionStyles();
    }

    @FXML
    private void onSelectFemale() {
        selectedGender = "female";
        applySelectionStyles();
    }

    @FXML
    private void onBack() {
        switchScene("/fxml/WeightView.fxml", "/css/onboarding.css");
    }

    @FXML
    private void onContinue() {
        if (selectedGender == null) {
            return;
        }
        AppSession.getOnboardingData().setGender(selectedGender);
        switchScene("/fxml/ObjectiveView.fxml", "/css/onboarding.css");
    }

    private void applySelectionStyles() {
        maleButton.getStyleClass().remove("choice-selected");
        femaleButton.getStyleClass().remove("choice-selected");
        if ("male".equals(selectedGender)) {
            maleButton.getStyleClass().add("choice-selected");
        } else if ("female".equals(selectedGender)) {
            femaleButton.getStyleClass().add("choice-selected");
        }
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
