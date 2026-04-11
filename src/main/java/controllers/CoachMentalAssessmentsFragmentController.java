package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import models.MentalHealthAssessmentSubmission;
import services.MentalHealthSubmissionService;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Coach view of member mental check-ins with search, status filter, and recommendations.
 */
public class CoachMentalAssessmentsFragmentController {

    private static final DateTimeFormatter DATE_LINE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_LINE = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private TextField searchField;
    @FXML
    private Button manageRecommendationsBtn;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private TableView<MentalHealthAssessmentSubmission> assessmentsTable;
    @FXML
    private TableColumn<MentalHealthAssessmentSubmission, String> colUser;
    @FXML
    private TableColumn<MentalHealthAssessmentSubmission, String> colDate;
    @FXML
    private TableColumn<MentalHealthAssessmentSubmission, String> colStress;
    @FXML
    private TableColumn<MentalHealthAssessmentSubmission, String> colSleep;
    @FXML
    private TableColumn<MentalHealthAssessmentSubmission, String> colMood;
    @FXML
    private TableColumn<MentalHealthAssessmentSubmission, String> colMotivation;
    @FXML
    private TableColumn<MentalHealthAssessmentSubmission, String> colScore;
    @FXML
    private TableColumn<MentalHealthAssessmentSubmission, String> colStatus;
    @FXML
    private TableColumn<MentalHealthAssessmentSubmission, Void> colAction;

    private final MentalHealthSubmissionService submissionService = MentalHealthSubmissionService.getInstance();
    private FilteredList<MentalHealthAssessmentSubmission> filtered;

    @FXML
    private void initialize() {
        statusFilterCombo.getItems().setAll("All statuses", "Low", "Moderate", "Good");
        statusFilterCombo.getSelectionModel().selectFirst();

        filtered = new FilteredList<>(submissionService.getSubmissions(), s -> true);
        assessmentsTable.setItems(filtered);
        assessmentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        assessmentsTable.setPlaceholder(new Label("No assessments yet. Members appear here after they save a mental check-in."));

        colUser.setCellValueFactory(cdf -> new SimpleStringProperty(""));
        colUser.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                MentalHealthAssessmentSubmission s = getTableRow().getItem();
                Label av = new Label(initials(s.getUserFullName(), s.getUserEmail()));
                av.getStyleClass().add("ca-user-avatar");
                Label name = new Label(safe(s.getUserFullName()));
                name.getStyleClass().add("ca-user-name");
                Label role = new Label("Athlete");
                role.getStyleClass().add("ca-user-role");
                VBox text = new VBox(2, name, role);
                HBox row = new HBox(12, av, text);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
            }
        });

        colDate.setCellValueFactory(cdf -> new SimpleStringProperty(""));
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                MentalHealthAssessmentSubmission s = getTableRow().getItem();
                if (s.getTestedAt() == null) {
                    setGraphic(new Label("—"));
                    return;
                }
                Label l1 = new Label(DATE_LINE.format(s.getTestedAt()));
                l1.getStyleClass().add("ca-date-line1");
                Label l2 = new Label(TIME_LINE.format(s.getTestedAt()));
                l2.getStyleClass().add("ca-date-line2");
                VBox vb = new VBox(2, l1, l2);
                setGraphic(vb);
            }
        });

        colStress.setCellValueFactory(cdf -> new SimpleStringProperty(slashFive(cdf.getValue().getStress())));
        colSleep.setCellValueFactory(cdf -> new SimpleStringProperty(slashFive(cdf.getValue().getSleep())));
        colMood.setCellValueFactory(cdf -> new SimpleStringProperty(slashFive(cdf.getValue().getMood())));
        colMotivation.setCellValueFactory(cdf -> new SimpleStringProperty(slashFive(cdf.getValue().getMotivation())));
        styleMetricColumn(colStress);
        styleMetricColumn(colSleep);
        styleMetricColumn(colMood);
        styleMetricColumn(colMotivation);

        colScore.setCellValueFactory(cdf -> new SimpleStringProperty(String.valueOf(cdf.getValue().getScore())));
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(String.valueOf(getTableRow().getItem().getScore()));
                badge.getStyleClass().add("ca-score-badge");
                setGraphic(badge);
            }
        });

        colStatus.setCellValueFactory(cdf -> new SimpleStringProperty(""));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                MentalHealthAssessmentSubmission s = getTableRow().getItem();
                Label badge = new Label(statusLabel(s.getStatus()));
                badge.getStyleClass().setAll("ca-status-" + statusKey(s.getStatus()));
                setGraphic(badge);
            }
        });

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button rec = new Button("+ Recommendation");

            {
                rec.getStyleClass().add("ca-rec-btn");
                rec.setOnAction(e -> {
                    TableRow<MentalHealthAssessmentSubmission> tr = getTableRow();
                    if (tr != null && tr.getItem() != null) {
                        openRecommendationEditor(tr.getItem());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : rec);
            }
        });

        searchField.textProperty().addListener((o, a, b) -> applyFilter());
        statusFilterCombo.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> applyFilter());
        applyFilter();
    }

    @FXML
    private void onManageRecommendations() {
        Window w = assessmentsTable.getScene() != null ? assessmentsTable.getScene().getWindow() : null;
        if (w == null) {
            return;
        }
        CoachRecommendationsManageDialog.show(w, this::refreshOnShow);
    }

    private void styleMetricColumn(TableColumn<MentalHealthAssessmentSubmission, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label l = new Label(item);
                l.getStyleClass().add("ca-metric");
                setGraphic(l);
            }
        });
    }

    public void refreshOnShow() {
        applyFilter();
        assessmentsTable.refresh();
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String statusSel = statusFilterCombo.getSelectionModel().getSelectedItem();
        filtered.setPredicate(s -> {
            if (!q.isEmpty()) {
                String name = safe(s.getUserFullName()).toLowerCase(Locale.ROOT);
                String email = safe(s.getUserEmail()).toLowerCase(Locale.ROOT);
                if (!name.contains(q) && !email.contains(q)) {
                    return false;
                }
            }
            if (statusSel != null && !"All statuses".equals(statusSel)) {
                if (!statusSel.equals(statusLabel(s.getStatus()))) {
                    return false;
                }
            }
            return true;
        });
    }

    private void openRecommendationEditor(MentalHealthAssessmentSubmission row) {
        Window w = assessmentsTable.getScene() != null ? assessmentsTable.getScene().getWindow() : null;
        if (w == null) {
            return;
        }
        if (RecommendationEditorDialog.showEditor(w, row)) {
            assessmentsTable.refresh();
        }
    }

    private static String initials(String name, String email) {
        String n = safe(name).trim();
        if (!n.isEmpty()) {
            String[] parts = n.split("\\s+");
            if (parts.length >= 2) {
                return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
            }
            return n.substring(0, Math.min(2, n.length())).toUpperCase(Locale.ROOT);
        }
        String em = safe(email);
        if (em.length() >= 2) {
            return em.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        return "?";
    }

    private static String slashFive(int v) {
        if (v < 1 || v > 5) {
            return "—";
        }
        return v + "/5";
    }

    /** Map internal band to UI label (matches design: Low / Moderate). */
    private static String statusLabel(String internal) {
        if ("Needs attention".equals(internal)) {
            return "Low";
        }
        if ("Moderate".equals(internal)) {
            return "Moderate";
        }
        if ("Good".equals(internal)) {
            return "Good";
        }
        return safe(internal);
    }

    private static String statusKey(String internal) {
        if ("Needs attention".equals(internal)) {
            return "low";
        }
        if ("Moderate".equals(internal)) {
            return "moderate";
        }
        if ("Good".equals(internal)) {
            return "good";
        }
        return "moderate";
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
