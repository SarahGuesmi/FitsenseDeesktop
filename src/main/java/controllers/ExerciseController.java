package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.Exercise;
import services.ExerciseService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExerciseController {

    private static final Logger LOG = Logger.getLogger(ExerciseController.class.getName());

    // Contraintes de saisie
    private static final int NOM_MIN      = 3;
    private static final int NOM_MAX      = 100;
    private static final int DESC_MAX     = 500;
    private static final int DUREE_MAX    = 300;  // minutes
    private static final int SETS_MAX     = 20;
    private static final int REPS_MAX     = 200;

    // ── Table ──────────────────────────────────────────────────────────────────
    @FXML private TableView<Exercise>           exerciseTable;
    @FXML private TableColumn<Exercise, String> colNom;
    @FXML private TableColumn<Exercise, String> colType;
    @FXML private TableColumn<Exercise, String> colDuree;
    @FXML private TableColumn<Exercise, String> colSets;
    @FXML private TableColumn<Exercise, String> colReps;
    @FXML private TableColumn<Exercise, Void>   colActions;

    // ── Modal overlay ──────────────────────────────────────────────────────────
    @FXML private StackPane modalOverlay;
    @FXML private VBox      formModal;
    @FXML private VBox      deleteConfirmModal;

    // ── Form fields ───────────────────────────────────────────────────────────
    @FXML private Label          modalTitle;
    @FXML private TextField      nomField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField      dureeField;
    @FXML private TextField      setsField;
    @FXML private TextField      repsField;
    @FXML private TextField      imageNameField;
    @FXML private TextField      youtubeField;
    @FXML private TextArea       descriptionArea;

    // ── Error labels (inline sous chaque champ) ───────────────────────────────
    @FXML private Label errNom;
    @FXML private Label errType;
    @FXML private Label errDuree;
    @FXML private Label errSets;
    @FXML private Label errReps;
    @FXML private Label errDescription;

    // ── Delete confirm ────────────────────────────────────────────────────────
    @FXML private Label deleteConfirmLabel;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ExerciseService exerciseService = new ExerciseService();
    private Exercise editingExercise;
    private Exercise pendingDeleteExercise;

    @FXML
    private void initialize() {
        setupTable();
        setupInputRestrictions();
        typeCombo.setItems(FXCollections.observableArrayList(
                "cardio", "strength", "flexibility", "balance", "hiit"));
        refreshData();
    }

    // ── Contrôle de saisie en temps réel ─────────────────────────────────────
    private void setupInputRestrictions() {

        // Nom : lettres (accents inclus), chiffres, espaces, tirets, apostrophes — max NOM_MAX
        nomField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            // Rejeter les caractères non autorisés
            String filtered = newVal.replaceAll("[^\\p{L}\\p{N} '\\-]", "");
            // Tronquer si trop long
            if (filtered.length() > NOM_MAX) filtered = filtered.substring(0, NOM_MAX);
            if (!filtered.equals(newVal)) nomField.setText(filtered);
            // Feedback visuel immédiat
            if (filtered.isBlank() || filtered.length() < NOM_MIN) {
                markInvalid(nomField);
            } else {
                nomField.getStyleClass().remove("modal-field-error");
            }
        });

        // Durée / Sets / Reps : chiffres uniquement, pas de zéro en tête
        for (TextField tf : List.of(dureeField, setsField, repsField)) {
            tf.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) return;
                // Garder uniquement les chiffres
                String digits = newVal.replaceAll("[^\\d]", "");
                // Supprimer les zéros en tête (ex: "007" → "7")
                digits = digits.replaceAll("^0+(\\d)", "$1");
                if (!digits.equals(newVal)) tf.setText(digits);
            });
        }

        // YouTube ID : alphanumérique + tirets/underscores uniquement, max 20 chars
        youtubeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String filtered = newVal.replaceAll("[^A-Za-z0-9_\\-]", "");
            if (filtered.length() > 20) filtered = filtered.substring(0, 20);
            if (!filtered.equals(newVal)) youtubeField.setText(filtered);
        });

        // Description : limite de caractères avec compteur visuel
        descriptionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > DESC_MAX)
                descriptionArea.setText(oldVal);
        });
    }

    // ── Table setup ────────────────────────────────────────────────────────────
    private void setupTable() {
        exerciseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        colNom.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().getNom())));
        colType.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().getType())));
        colDuree.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDuree() != null ? d.getValue().getDuree() + " min" : "-"));
        colSets.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSets() != null ? String.valueOf(d.getValue().getSets()) : "-"));
        colReps.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getReps() != null ? String.valueOf(d.getValue().getReps()) : "-"));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = iconBtn("✎", "icon-edit");
            private final Button deleteBtn = iconBtn("🗑", "icon-delete");
            private final HBox   box       = new HBox(8, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                // 🔥 Fix NPE : vérifier que la row et l'item ne sont pas null
                editBtn.setOnAction(e -> {
                    TableRow<Exercise> row = getTableRow();
                    if (row != null && row.getItem() != null) openEditModal(row.getItem());
                });
                deleteBtn.setOnAction(e -> {
                    TableRow<Exercise> row = getTableRow();
                    if (row != null && row.getItem() != null) openDeleteConfirm(row.getItem());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                TableRow<Exercise> row = getTableRow();
                setGraphic(empty || row == null || row.getItem() == null ? null : box);
            }
        });
    }

    // ── CRUD actions ───────────────────────────────────────────────────────────
    @FXML
    private void onAddExercise() {
        editingExercise = null;
        clearForm();
        modalTitle.setText("Nouvel exercice");
        showModal(formModal);
    }

    @FXML
    private void onSaveExercise() {
        if (!validateForm()) return;

        try {
            if (editingExercise == null) {
                Exercise e = buildFromForm(new Exercise());
                if (e.getDescription() == null || e.getDescription().isBlank()) {
                    e.setDescription("Description par défaut");
                }
                exerciseService.createPrepared(e);
            } else {
                buildFromForm(editingExercise);
                exerciseService.update(editingExercise);
            }
            closeModal();
            refreshData();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Erreur sauvegarde exercice", ex);
            showError("Erreur base de données", "Impossible de sauvegarder l'exercice.\n" + ex.getMessage());
        }
    }

    @FXML
    private void onConfirmDelete() {
        if (pendingDeleteExercise == null) { closeModal(); return; }
        try {
            exerciseService.delete(pendingDeleteExercise);
            closeModal();
            refreshData();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Erreur suppression exercice", ex);
            showError("Erreur suppression", "Impossible de supprimer l'exercice.\n" + ex.getMessage());
        }
    }

    @FXML
    private void onCloseModal() { closeModal(); }

    // ── Navigation ─────────────────────────────────────────────────────────────
    @FXML
    private void onGoWorkouts() {
        switchScene("/fxml/WorkoutView.fxml", "/css/admin.css");
    }

    @FXML
    private void onLogout() {
        app.AppSession.setCurrentUser(null);
        app.AppSession.resetOnboarding();
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void openEditModal(Exercise e) {
        if (e == null) return;
        editingExercise = e;
        modalTitle.setText("Modifier l'exercice");
        nomField.setText(safe(e.getNom()));
        typeCombo.setValue(e.getType());
        dureeField.setText(e.getDuree() != null ? String.valueOf(e.getDuree()) : "");
        setsField.setText(e.getSets() != null ? String.valueOf(e.getSets()) : "");
        repsField.setText(e.getReps() != null ? String.valueOf(e.getReps()) : "");
        imageNameField.setText(safe(e.getImageName()));
        youtubeField.setText(safe(e.getYoutubeVideoId()));
        descriptionArea.setText(safe(e.getDescription()));
        clearValidationStyles();
        showModal(formModal);
    }

    private void openDeleteConfirm(Exercise e) {
        if (e == null) return;
        pendingDeleteExercise = e;
        deleteConfirmLabel.setText("Supprimer l'exercice \"" + safe(e.getNom()) + "\" ?");
        showModal(deleteConfirmModal);
    }

    private Exercise buildFromForm(Exercise e) {
        e.setNom(nomField.getText().trim());
        e.setType(typeCombo.getValue());
        e.setDuree(parseIntOrNull(dureeField.getText()));
        e.setSets(parseIntOrNull(setsField.getText()));
        e.setReps(parseIntOrNull(repsField.getText()));
        e.setImageName(imageNameField.getText().trim());
        e.setYoutubeVideoId(youtubeField.getText().trim());
        e.setDescription(descriptionArea.getText().trim());
        // 🔥 Sécuriser : garantir liste non-null
        if (e.getWorkouts() == null) e.setWorkouts(new ArrayList<>());
        return e;
    }

    // ── Validation complète ────────────────────────────────────────────────────
    private boolean validateForm() {
        clearValidationStyles();
        boolean valid = true;

        // Nom
        String nom = nomField.getText() == null ? "" : nomField.getText().trim();
        if (nom.isBlank()) {
            setFieldError(nomField, errNom, "Le nom est obligatoire.");
            valid = false;
        } else if (nom.length() < NOM_MIN) {
            setFieldError(nomField, errNom, "Minimum " + NOM_MIN + " caractères.");
            valid = false;
        } else if (nom.length() > NOM_MAX) {
            setFieldError(nomField, errNom, "Maximum " + NOM_MAX + " caractères.");
            valid = false;
        }

        // Type
        if (typeCombo.getValue() == null) {
            markInvalid(typeCombo);
            showErrLabel(errType, "Le type est obligatoire.");
            valid = false;
        }

        // Durée (optionnelle mais doit être > 0 et <= max si renseignée)
        String dureeStr = safe(dureeField.getText()).trim();
        if (!dureeStr.isBlank()) {
            Integer duree = parseIntOrNull(dureeStr);
            if (duree == null || duree <= 0) {
                setFieldError(dureeField, errDuree, "Durée invalide (nombre entier > 0).");
                valid = false;
            } else if (duree > DUREE_MAX) {
                setFieldError(dureeField, errDuree, "Durée max : " + DUREE_MAX + " min.");
                valid = false;
            }
        }

        // Sets (optionnel)
        String setsStr = safe(setsField.getText()).trim();
        if (!setsStr.isBlank()) {
            Integer sets = parseIntOrNull(setsStr);
            if (sets == null || sets <= 0) {
                setFieldError(setsField, errSets, "Sets invalide (nombre entier > 0).");
                valid = false;
            } else if (sets > SETS_MAX) {
                setFieldError(setsField, errSets, "Sets max : " + SETS_MAX + ".");
                valid = false;
            }
        }

        // Reps (optionnel)
        String repsStr = safe(repsField.getText()).trim();
        if (!repsStr.isBlank()) {
            Integer reps = parseIntOrNull(repsStr);
            if (reps == null || reps <= 0) {
                setFieldError(repsField, errReps, "Reps invalide (nombre entier > 0).");
                valid = false;
            } else if (reps > REPS_MAX) {
                setFieldError(repsField, errReps, "Reps max : " + REPS_MAX + ".");
                valid = false;
            }
        }

        // Description (optionnelle mais limitée)
        String desc = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        if (desc.length() > DESC_MAX) {
            setFieldError(descriptionArea, errDescription, "Maximum " + DESC_MAX + " caractères.");
            valid = false;
        }

        return valid;
    }

    private void clearForm() {
        nomField.clear(); typeCombo.setValue(null);
        dureeField.clear(); setsField.clear(); repsField.clear();
        imageNameField.clear(); youtubeField.clear(); descriptionArea.clear();
        clearValidationStyles();
    }

    private void refreshData() {
        try {
            List<Exercise> list = exerciseService.read();
            // 🔥 Sécuriser : garantir liste non-null
            exerciseTable.setItems(FXCollections.observableArrayList(
                    list != null ? list : new ArrayList<>()));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Erreur chargement exercices", ex);
            exerciseTable.setItems(FXCollections.observableArrayList());
            showError("Erreur chargement", "Impossible de charger les exercices.\n" + ex.getMessage());
        }
    }

    private void showModal(VBox target) {
        formModal.setManaged(false); formModal.setVisible(false);
        deleteConfirmModal.setManaged(false); deleteConfirmModal.setVisible(false);
        modalOverlay.setManaged(true); modalOverlay.setVisible(true);
        target.setManaged(true); target.setVisible(true);
    }

    private void closeModal() {
        modalOverlay.setManaged(false); modalOverlay.setVisible(false);
        formModal.setManaged(false); formModal.setVisible(false);
        deleteConfirmModal.setManaged(false); deleteConfirmModal.setVisible(false);
        editingExercise = null; pendingDeleteExercise = null;
    }

    // ── Validation UI helpers ──────────────────────────────────────────────────
    private void setFieldError(javafx.scene.Node field, Label errLabel, String msg) {
        markInvalid(field);
        showErrLabel(errLabel, msg);
    }

    private void showErrLabel(Label lbl, String msg) {
        if (lbl == null) return;
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private void markInvalid(javafx.scene.Node node) {
        if (node != null && !node.getStyleClass().contains("modal-field-error"))
            node.getStyleClass().add("modal-field-error");
    }

    private void clearValidationStyles() {
        List.of(nomField, typeCombo, dureeField, setsField, repsField, descriptionArea)
                .forEach(n -> n.getStyleClass().remove("modal-field-error"));
        for (Label lbl : new Label[]{errNom, errType, errDuree, errSets, errReps, errDescription}) {
            if (lbl != null) { lbl.setText(""); lbl.setVisible(false); lbl.setManaged(false); }
        }
    }

    // ── Utils ──────────────────────────────────────────────────────────────────
    private static Button iconBtn(String text, String styleClass) {
        Button b = new Button(text);
        b.getStyleClass().addAll("table-icon-btn", styleClass);
        b.setFocusTraversable(false);
        return b;
    }

    private static Integer parseIntOrNull(String s) {
        try { return s == null || s.isBlank() ? null : Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // 🔥 Fix NPE : vérifier que la scène est disponible avant de switcher
    private void switchScene(String fxmlPath, String cssPath) {
        try {
            Scene scene = exerciseTable.getScene();
            if (scene == null) {
                LOG.warning("switchScene appelé avant que la scène soit attachée.");
                return;
            }
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource(fxmlPath),
                    "FXML introuvable : " + fxmlPath));
            scene.setRoot(root);
            String cssUrl = Objects.requireNonNull(
                    getClass().getResource(cssPath),
                    "CSS introuvable : " + cssPath).toExternalForm();
            scene.getStylesheets().setAll(cssUrl);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Erreur navigation vers " + fxmlPath, ex);
            showError("Erreur navigation", "Impossible d'ouvrir la vue.\n" + ex.getMessage());
        }
    }
}
