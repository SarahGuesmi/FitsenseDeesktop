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
import models.Workout;
import services.ExerciseService;
import services.WorkoutService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorkoutController {

    private static final Logger LOG = Logger.getLogger(WorkoutController.class.getName());

    private static final int NOM_MIN   = 3;
    private static final int NOM_MAX   = 100;
    private static final int DESC_MAX  = 500;
    private static final int DUREE_MAX = 600;

    // ── Table
    @FXML private TableView<Workout>           workoutTable;
    @FXML private TableColumn<Workout, String> colNom;
    @FXML private TableColumn<Workout, String> colNiveau;
    @FXML private TableColumn<Workout, String> colDuree;
    @FXML private TableColumn<Workout, String> colStatus;
    @FXML private TableColumn<Workout, String> colExercises;
    @FXML private TableColumn<Workout, Void>   colActions;

    // ── Modals
    @FXML private StackPane modalOverlay;
    @FXML private VBox      formModal;
    @FXML private VBox      detailsModal;
    @FXML private VBox      deleteConfirmModal;

    // ── Form fields
    @FXML private Label              modalTitle;
    @FXML private TextField          nomField;
    @FXML private ComboBox<String>   niveauCombo;
    @FXML private TextField          dureeField;
    @FXML private ComboBox<String>   statusCombo;
    @FXML private TextArea           descriptionArea;
    @FXML private ListView<Exercise> exercisesListView;

    // ── Error labels
    @FXML private Label errNom;
    @FXML private Label errNiveau;
    @FXML private Label errStatus;
    @FXML private Label errDuree;
    @FXML private Label errDescription;

    // ── Details modal labels
    @FXML private Label detailsNom;
    @FXML private Label detailsNiveau;
    @FXML private Label detailsDuree;
    @FXML private Label detailsStatus;
    @FXML private Label detailsDescription;
    @FXML private VBox  detailsExercisesBox;

    // ── Delete confirm
    @FXML private Label deleteConfirmLabel;

    // ── State
    private final WorkoutService  workoutService  = new WorkoutService();
    private final ExerciseService exerciseService = new ExerciseService();
    private Workout editingWorkout;
    private Workout pendingDeleteWorkout;

    @FXML
    private void initialize() {
        setupTable();
        setupInputRestrictions();
        niveauCombo.setItems(FXCollections.observableArrayList("beginner", "intermediate", "advanced"));
        statusCombo.setItems(FXCollections.observableArrayList("active", "inactive"));
        exercisesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        exercisesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Exercise item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : safe(item.getNom()) + " (" + safe(item.getType()) + ")");
            }
        });
        loadExercisesIntoList();
        refreshData();
    }

    // ── Contrôle de saisie en temps réel ─────────────────────────────────────
    private void setupInputRestrictions() {

        // Nom : lettres (accents inclus), chiffres, espaces, tirets, apostrophes — max NOM_MAX
        nomField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String filtered = newVal.replaceAll("[^\\p{L}\\p{N} '\\-]", "");
            if (filtered.length() > NOM_MAX) filtered = filtered.substring(0, NOM_MAX);
            if (!filtered.equals(newVal)) nomField.setText(filtered);
            // Feedback visuel immédiat
            if (filtered.isBlank() || filtered.length() < NOM_MIN) {
                markInvalid(nomField);
            } else {
                nomField.getStyleClass().remove("modal-field-error");
            }
        });

        // Durée : chiffres uniquement, pas de zéro en tête
        dureeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String digits = newVal.replaceAll("[^\\d]", "").replaceAll("^0+(\\d)", "$1");
            if (!digits.equals(newVal)) dureeField.setText(digits);
        });

        // Description : limite de caractères
        descriptionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > DESC_MAX)
                descriptionArea.setText(oldVal);
        });
    }

    private void setupTable() {
        workoutTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colNom.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().getNom())));
        colNiveau.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().getNiveau())));
        colDuree.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDuree() != null ? d.getValue().getDuree() + " min" : "-"));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().getStatus())));

        // 🔥 Fix NPE : getExercises() peut être null
        colExercises.setCellValueFactory(d -> {
            List<Exercise> exs = d.getValue().getExercises();
            return new SimpleStringProperty((exs != null ? exs.size() : 0) + " exercice(s)");
        });

        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                badge.getStyleClass().setAll("status-badge",
                        "active".equalsIgnoreCase(status) ? "status-active" : "status-inactive");
                badge.setText(status.toUpperCase());
                setGraphic(badge);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button detailsBtn = iconBtn("👁", "icon-activate");
            private final Button editBtn    = iconBtn("✎", "icon-edit");
            private final Button deleteBtn  = iconBtn("🗑", "icon-delete");
            private final HBox   box        = new HBox(8, detailsBtn, editBtn, deleteBtn);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                // 🔥 Fix NPE : vérifier row et item
                detailsBtn.setOnAction(e -> { TableRow<Workout> r = getTableRow(); if (r != null && r.getItem() != null) openDetailsModal(r.getItem()); });
                editBtn.setOnAction(e -> { TableRow<Workout> r = getTableRow(); if (r != null && r.getItem() != null) openEditModal(r.getItem()); });
                deleteBtn.setOnAction(e -> { TableRow<Workout> r = getTableRow(); if (r != null && r.getItem() != null) openDeleteConfirm(r.getItem()); });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                TableRow<Workout> r = getTableRow();
                setGraphic(empty || r == null || r.getItem() == null ? null : box);
            }
        });
    }

    @FXML
    private void onAddWorkout() {
        // 🔥 Ne plus ignorer l'exception SQL
        try {
            List<Exercise> all = exerciseService.read();
            if (all == null || all.isEmpty()) {
                showWarning("Attention", "Créez au moins un exercice avant de créer un workout.");
                return;
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Erreur vérification exercices", ex);
            showError("Erreur", "Impossible de vérifier les exercices disponibles.\n" + ex.getMessage());
            return;
        }
        editingWorkout = null;
        clearForm();
        modalTitle.setText("Nouveau workout");
        showModal(formModal);
    }

    @FXML
    private void onSaveWorkout() {
        if (!validateForm()) return;
        try {
            if (editingWorkout == null) {
                Workout w = buildFromForm(new Workout());
                if (w.getNiveau() == null || w.getNiveau().isBlank()) w.setNiveau("beginner");
                workoutService.createPrepared(w);
            } else {
                buildFromForm(editingWorkout);
                workoutService.update(editingWorkout);
            }
            closeModal();
            refreshData();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Erreur sauvegarde workout", ex);
            showError("Erreur base de données", "Impossible de sauvegarder le workout.\n" + ex.getMessage());
        }
    }

    @FXML
    private void onConfirmDelete() {
        if (pendingDeleteWorkout == null) { closeModal(); return; }
        try {
            workoutService.delete(pendingDeleteWorkout);
            closeModal();
            refreshData();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Erreur suppression workout", ex);
            showError("Erreur suppression", "Impossible de supprimer le workout.\n" + ex.getMessage());
        }
    }

    @FXML private void onCloseModal() { closeModal(); }

    @FXML
    private void onGoExercises() { switchScene("/fxml/ExerciseView.fxml", "/css/admin.css"); }

    @FXML
    private void onLogout() {
        app.AppSession.setCurrentUser(null);
        app.AppSession.resetOnboarding();
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    private void openEditModal(Workout w) {
        if (w == null) return;
        editingWorkout = w;
        modalTitle.setText("Modifier le workout");
        nomField.setText(safe(w.getNom()));
        niveauCombo.setValue(w.getNiveau());
        dureeField.setText(w.getDuree() != null ? String.valueOf(w.getDuree()) : "");
        statusCombo.setValue(w.getStatus());
        descriptionArea.setText(safe(w.getDescription()));

        exercisesListView.getSelectionModel().clearSelection();
        // 🔥 Sécuriser : getExercises() peut être null, getId() peut être null
        List<Exercise> linked = w.getExercises();
        if (linked != null) {
            for (Exercise e : linked) {
                if (e == null || e.getId() == null) continue;
                for (int i = 0; i < exercisesListView.getItems().size(); i++) {
                    Exercise item = exercisesListView.getItems().get(i);
                    if (item != null && e.getId().equals(item.getId()))
                        exercisesListView.getSelectionModel().select(i);
                }
            }
        }
        clearValidationStyles();
        showModal(formModal);
    }

    private void openDetailsModal(Workout w) {
        if (w == null) return;
        detailsNom.setText(safe(w.getNom()));
        detailsNiveau.setText(safe(w.getNiveau()));
        detailsDuree.setText(w.getDuree() != null ? w.getDuree() + " min" : "-");
        detailsStatus.setText(safe(w.getStatus()));
        detailsDescription.setText(safe(w.getDescription()));
        detailsExercisesBox.getChildren().clear();

        // 🔥 Ne plus ignorer l'exception SQL
        try {
            List<Exercise> exercises = exerciseService.findByWorkoutId(w.getId());
            if (exercises == null || exercises.isEmpty()) {
                detailsExercisesBox.getChildren().add(new Label("Aucun exercice lié."));
            } else {
                for (Exercise e : exercises) {
                    if (e == null) continue;
                    Label lbl = new Label("• " + safe(e.getNom()) + " — " + safe(e.getType())
                            + (e.getDuree() != null ? " (" + e.getDuree() + " min)" : ""));
                    detailsExercisesBox.getChildren().add(lbl);
                }
            }
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "Erreur chargement exercices du workout " + w.getId(), ex);
            detailsExercisesBox.getChildren().add(new Label("Erreur lors du chargement des exercices."));
        }
        showModal(detailsModal);
    }

    private void openDeleteConfirm(Workout w) {
        if (w == null) return;
        pendingDeleteWorkout = w;
        deleteConfirmLabel.setText("Supprimer le workout \"" + safe(w.getNom()) + "\" ?");
        showModal(deleteConfirmModal);
    }

    private Workout buildFromForm(Workout w) {
        w.setNom(nomField.getText().trim());
        w.setNiveau(niveauCombo.getValue());
        w.setDuree(parseIntOrNull(dureeField.getText()));
        w.setStatus(statusCombo.getValue());
        w.setDescription(descriptionArea.getText().trim());
        // 🔥 Fix : getSelectedItems() retourne une liste non-modifiable → copier dans ArrayList
        List<Exercise> selected = exercisesListView.getSelectionModel().getSelectedItems();
        w.setExercises(selected != null ? new ArrayList<>(selected) : new ArrayList<>());
        return w;
    }

    private boolean validateForm() {
        clearValidationStyles();
        boolean valid = true;

        String nom = nomField.getText() == null ? "" : nomField.getText().trim();
        if (nom.isBlank()) {
            setFieldError(nomField, errNom, "Le nom est obligatoire."); valid = false;
        } else if (nom.length() < NOM_MIN) {
            setFieldError(nomField, errNom, "Minimum " + NOM_MIN + " caractères."); valid = false;
        } else if (nom.length() > NOM_MAX) {
            setFieldError(nomField, errNom, "Maximum " + NOM_MAX + " caractères."); valid = false;
        }

        if (niveauCombo.getValue() == null) {
            markInvalid(niveauCombo); showErrLabel(errNiveau, "Le niveau est obligatoire."); valid = false;
        }

        if (statusCombo.getValue() == null) {
            markInvalid(statusCombo); showErrLabel(errStatus, "Le statut est obligatoire."); valid = false;
        }

        String dureeStr = safe(dureeField.getText()).trim();
        if (!dureeStr.isBlank()) {
            Integer duree = parseIntOrNull(dureeStr);
            if (duree == null || duree <= 0) {
                setFieldError(dureeField, errDuree, "Durée invalide (entier > 0)."); valid = false;
            } else if (duree > DUREE_MAX) {
                setFieldError(dureeField, errDuree, "Durée max : " + DUREE_MAX + " min."); valid = false;
            }
        }

        String desc = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        if (desc.length() > DESC_MAX) {
            setFieldError(descriptionArea, errDescription, "Maximum " + DESC_MAX + " caractères."); valid = false;
        }

        return valid;
    }

    private void clearForm() {
        nomField.clear(); niveauCombo.setValue(null); dureeField.clear();
        statusCombo.setValue(null); descriptionArea.clear();
        exercisesListView.getSelectionModel().clearSelection();
        clearValidationStyles();
    }

    private void loadExercisesIntoList() {
        try {
            List<Exercise> list = exerciseService.read();
            exercisesListView.setItems(FXCollections.observableArrayList(list != null ? list : new ArrayList<>()));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Erreur chargement liste exercices", ex);
            exercisesListView.setItems(FXCollections.observableArrayList());
            showError("Erreur", "Impossible de charger les exercices.\n" + ex.getMessage());
        }
    }

    private void refreshData() {
        try {
            List<Workout> list = workoutService.readWithExercises();
            // 🔥 Sécuriser : garantir exercises non-null sur chaque workout
            if (list != null) {
                for (Workout w : list) {
                    if (w.getExercises() == null) w.setExercises(new ArrayList<>());
                }
            }
            workoutTable.setItems(FXCollections.observableArrayList(list != null ? list : new ArrayList<>()));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Erreur chargement workouts", ex);
            workoutTable.setItems(FXCollections.observableArrayList());
            showError("Erreur chargement", "Impossible de charger les workouts.\n" + ex.getMessage());
        }
    }

    private void showModal(VBox target) {
        formModal.setManaged(false); formModal.setVisible(false);
        detailsModal.setManaged(false); detailsModal.setVisible(false);
        deleteConfirmModal.setManaged(false); deleteConfirmModal.setVisible(false);
        modalOverlay.setManaged(true); modalOverlay.setVisible(true);
        target.setManaged(true); target.setVisible(true);
    }

    private void closeModal() {
        modalOverlay.setManaged(false); modalOverlay.setVisible(false);
        formModal.setManaged(false); formModal.setVisible(false);
        detailsModal.setManaged(false); detailsModal.setVisible(false);
        deleteConfirmModal.setManaged(false); deleteConfirmModal.setVisible(false);
        editingWorkout = null; pendingDeleteWorkout = null;
    }

    private void setFieldError(javafx.scene.Node field, Label errLabel, String msg) {
        markInvalid(field); showErrLabel(errLabel, msg);
    }

    private void showErrLabel(Label lbl, String msg) {
        if (lbl == null) return;
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }

    private void markInvalid(javafx.scene.Node node) {
        if (node != null && !node.getStyleClass().contains("modal-field-error"))
            node.getStyleClass().add("modal-field-error");
    }

    private void clearValidationStyles() {
        List.of(nomField, niveauCombo, statusCombo, dureeField, descriptionArea)
                .forEach(n -> n.getStyleClass().remove("modal-field-error"));
        for (Label lbl : new Label[]{errNom, errNiveau, errStatus, errDuree, errDescription}) {
            if (lbl != null) { lbl.setText(""); lbl.setVisible(false); lbl.setManaged(false); }
        }
    }

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
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showWarning(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // 🔥 Fix NPE : vérifier que la scène est disponible avant de switcher
    private void switchScene(String fxmlPath, String cssPath) {
        try {
            Scene scene = workoutTable.getScene();
            if (scene == null) { LOG.warning("switchScene : scène non attachée."); return; }
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource(fxmlPath), "FXML introuvable : " + fxmlPath));
            scene.setRoot(root);
            scene.getStylesheets().setAll(Objects.requireNonNull(
                    getClass().getResource(cssPath), "CSS introuvable : " + cssPath).toExternalForm());
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Erreur navigation vers " + fxmlPath, ex);
            showError("Erreur navigation", "Impossible d'ouvrir la vue.\n" + ex.getMessage());
        }
    }
}
