package controllers;

import app.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import models.ObjectifSportif;
import models.ProfilePhysique;
import models.User;
import services.ObjectifSportifService;
import services.ProfilePhysiqueService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ObjectiveController {
    @FXML
    private StackPane root;
    @FXML
    private Button objectiveBtn1;
    @FXML
    private Button objectiveBtn2;
    @FXML
    private Button objectiveBtn3;
    @FXML
    private Button objectiveBtn4;

    private final ProfilePhysiqueService profileService = new ProfilePhysiqueService();
    private final ObjectifSportifService objectifService = new ObjectifSportifService();
    private final List<Button> objectiveButtons = new ArrayList<>();
    private final Set<String> selectedObjectives = new LinkedHashSet<>();

    @FXML
    private void initialize() {
        objectiveButtons.add(objectiveBtn1);
        objectiveButtons.add(objectiveBtn2);
        objectiveButtons.add(objectiveBtn3);
        objectiveButtons.add(objectiveBtn4);

        List<String> names = loadObjectiveNames();
        for (int i = 0; i < objectiveButtons.size(); i++) {
            Button b = objectiveButtons.get(i);
            String name = i < names.size() ? names.get(i) : "Objective " + (i + 1);
            b.setText(name);
            b.setUserData(name);
        }
    }

    @FXML
    private void onObjectiveClick(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String name = String.valueOf(clicked.getUserData());
        if (selectedObjectives.contains(name)) {
            selectedObjectives.remove(name);
            clicked.getStyleClass().remove("choice-selected");
        } else {
            selectedObjectives.add(name);
            clicked.getStyleClass().add("choice-selected");
        }
    }

    @FXML
    private void onContinue() {
        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Session Expired", "Please sign in again.");
            switchScene("/fxml/SignInView.fxml", "/css/signin.css");
            return;
        }
        if (selectedObjectives.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Select Objective", "Please choose at least one objective.");
            return;
        }

        AppSession.getOnboardingData().setObjectiveName(String.join(", ", selectedObjectives));

        ProfilePhysique profile = new ProfilePhysique();
        profile.setUserId(currentUser.getId());
        profile.setHeight(AppSession.getOnboardingData().getHeightCm());
        profile.setWeight(AppSession.getOnboardingData().getWeightKg());
        profile.setGender(AppSession.getOnboardingData().getGender());

        try {
            profileService.createPrepared(profile);
            for (String name : selectedObjectives) {
                ObjectifSportif objectif = new ObjectifSportif();
                objectif.setName(name);
                objectif.setProfilePhysiqueId(profile.getId());
                objectifService.createPrepared(objectif);
            }
            switchScene("/fxml/DashboardView.fxml", "/css/dashboard.css");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Save Failed", e.getMessage());
        }
    }

    private List<String> loadObjectiveNames() {
        Set<String> unique = new LinkedHashSet<>();
        try {
            for (ObjectifSportif o : objectifService.read()) {
                if (o.getName() != null && !o.getName().isBlank()) {
                    unique.add(o.getName().trim());
                }
            }
        } catch (SQLException ignored) {
        }
        if (unique.isEmpty()) {
            unique.add("Weight Loss");
            unique.add("Muscle Gain");
            unique.add("Endurance");
            unique.add("Well-being");
        }
        return new ArrayList<>(unique);
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

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
