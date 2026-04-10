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

public class HeightController {
    @FXML
    private StackPane root;
    @FXML
    private Slider heightSlider;
    @FXML
    private Label heightValueLabel;

    @FXML
    private void initialize() {
        float h = AppSession.getOnboardingData().getHeightCm();
        heightSlider.setValue(h);
        updateHeightLabel(h);
        heightSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateHeightLabel(newVal.floatValue()));
    }

    @FXML
    private void onMinus() {
        heightSlider.setValue(Math.max(130, heightSlider.getValue() - 1));
    }

    @FXML
    private void onPlus() {
        heightSlider.setValue(Math.min(230, heightSlider.getValue() + 1));
    }

    @FXML
    private void onContinue() {
        AppSession.getOnboardingData().setHeightCm((float) heightSlider.getValue());
        switchScene("/fxml/WeightView.fxml", "/css/onboarding.css");
    }

    private void updateHeightLabel(float value) {
        heightValueLabel.setText(String.format("%.0f cm", value));
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
