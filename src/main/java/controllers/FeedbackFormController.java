package controllers;

import app.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Questionnaire;
import services.FeedbackService;
import utils.DbConnection;

import java.sql.SQLException;
import java.time.Instant;

public class FeedbackFormController {

    @FXML private TextField titreField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField noteGlobaleField;
    @FXML private TextField satisfactionField;
    @FXML private ComboBox<String> intensiteCombo;
    @FXML private ComboBox<String> dureeCombo;
    @FXML private TextArea commentaireArea;

    private Runnable onSaveCallback;

    @FXML
    private void initialize() {
        typeCombo.getItems().addAll("response", "template");
        typeCombo.setValue("template");

        intensiteCombo.getItems().addAll("Faible", "Modérée", "Élevée", "Très élevée");
        dureeCombo.getItems().addAll("< 30 min", "30-60 min", "60-90 min", "> 90 min");
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void onSave() {
        String titre = titreField.getText() == null ? "" : titreField.getText().trim();
        if (titre.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Field", "Title is required.");
            return;
        }

        Questionnaire q = new Questionnaire();
        q.setTitre(titre);
        q.setType(typeCombo.getValue() != null ? typeCombo.getValue() : "template");
        q.setIntensite(intensiteCombo.getValue());
        q.setDuree(dureeCombo.getValue());
        q.setCommentaire(commentaireArea.getText());
        q.setDateSoumission(Instant.now());

        // coach
        if (AppSession.getCurrentUser() != null) {
            q.setCoach(AppSession.getCurrentUser());
        }

        try {
            Integer noteGlobale = parseIntOrNull(noteGlobaleField.getText());
            Integer satisfaction = parseIntOrNull(satisfactionField.getText());
            q.setNoteGlobale(noteGlobale);
            q.setSatisfaction(satisfaction);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Invalid Input", "Rating and satisfaction must be numbers between 1 and 10.");
            return;
        }

        try {
            FeedbackService qs = new FeedbackService(DbConnection.getInstance().getCnx());
            qs.createPrepared(q);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Feedback created successfully.");
            if (onSaveCallback != null) onSaveCallback.run();
            closeWindow();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not save feedback: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) titreField.getScene().getWindow();
        stage.close();
    }

    private Integer parseIntOrNull(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        return Integer.parseInt(text.trim());
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
