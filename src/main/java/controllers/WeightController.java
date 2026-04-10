package controllers;

import app.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.Objects;

public class WeightController {
    @FXML
    private StackPane root;
    @FXML
    private Slider weightSlider;
    @FXML
    private Label weightValueLabel;

    @FXML
    private void initialize() {
        float w = AppSession.getOnboardingData().getWeightKg();
        weightSlider.setValue(w);
        updateWeightLabel(w);
        weightSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateWeightLabel(newVal.floatValue()));
    }

    @FXML
    private void onMinus() {
        weightSlider.setValue(Math.max(30, weightSlider.getValue() - 1));
    }

    @FXML
    private void onPlus() {
        weightSlider.setValue(Math.min(200, weightSlider.getValue() + 1));
    }

    @FXML
    private void onContinue() {
        AppSession.getOnboardingData().setWeightKg((float) weightSlider.getValue());
        switchScene("/fxml/GenderView.fxml", "/css/onboarding.css");
    }

    private void updateWeightLabel(float value) {
        weightValueLabel.setText(String.format("%.0f kg", value));
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
