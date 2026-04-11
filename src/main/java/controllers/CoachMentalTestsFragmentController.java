package controllers;

import app.AppSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import models.CoachMentalTest;
import models.CoachMentalTestQuestion;
import models.User;
import services.CoachMentalTestService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Coach CRUD for mental wellbeing tests (title + variable questions; scoring 1–5 per question, total = sum).
 */
public class CoachMentalTestsFragmentController {

    @FXML
    private VBox listPane;
    @FXML
    private VBox formPane;
    @FXML
    private TableView<CoachMentalTest> testsTable;
    @FXML
    private TableColumn<CoachMentalTest, String> colTitle;
    @FXML
    private TableColumn<CoachMentalTest, String> colQuestionCount;
    @FXML
    private TableColumn<CoachMentalTest, String> colMaxScore;
    @FXML
    private TableColumn<CoachMentalTest, Void> colAction;
    @FXML
    private Label formHeadingLabel;
    @FXML
    private TextField titleField;
    @FXML
    private VBox questionsEditorBox;

    private final CoachMentalTestService service = CoachMentalTestService.getInstance();
    private UUID editingTestId;

    @FXML
    private void initialize() {
        testsTable.setItems(service.getTests());
        testsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        testsTable.setPlaceholder(new Label("No tests yet. Click “+ Create test”."));

        colTitle.setCellValueFactory(cdf -> new SimpleStringProperty(safe(cdf.getValue().getTitle())));
        colQuestionCount.setCellValueFactory(cdf -> new SimpleStringProperty(String.valueOf(cdf.getValue().getQuestionCount())));
        colMaxScore.setCellValueFactory(cdf -> new SimpleStringProperty(String.valueOf(cdf.getValue().getMaxScore())));
        colAction.setCellFactory(actionColumnFactory());

        showListView();
    }

    /**
     * Called when coach opens Mental wellness; ensures list is visible after returning from other tabs.
     */
    public void showListView() {
        editingTestId = null;
        clearFormFields();
        listPane.setManaged(true);
        listPane.setVisible(true);
        formPane.setManaged(false);
        formPane.setVisible(false);
        testsTable.refresh();
    }

    @FXML
    private void onCreateTest() {
        editingTestId = null;
        formHeadingLabel.setText("New test");
        titleField.clear();
        questionsEditorBox.getChildren().clear();
        addQuestionRow("");
        showFormView();
    }

    @FXML
    private void onBackToList() {
        showListView();
    }

    @FXML
    private void onAddQuestionRow() {
        addQuestionRow("");
    }

    @FXML
    private void onSaveTest() {
        User coach = AppSession.getCurrentUser();
        if (coach == null || coach.getId() == null) {
            warn("Sign in required", "Sign in as a coach to save mental health tests.");
            return;
        }
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) {
            warn("Missing title", "Please enter a test title.");
            return;
        }
        List<String> prompts = collectPromptsFromEditor();
        if (prompts.isEmpty()) {
            warn("Missing questions", "Add at least one question with text.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        UUID id = editingTestId != null ? editingTestId : UUID.randomUUID();
        CoachMentalTest test = new CoachMentalTest(id);
        test.setCoachUserId(coach.getId());
        test.setTitle(title);
        int order = 0;
        for (String p : prompts) {
            test.addQuestion(new CoachMentalTestQuestion(UUID.randomUUID(), order++, p));
        }
        if (editingTestId != null) {
            service.findById(editingTestId).ifPresent(old -> test.setCreatedAt(old.getCreatedAt()));
        } else {
            test.setCreatedAt(now);
        }
        test.setUpdatedAt(now);

        service.add(test);
        showListView();
    }

    private List<String> collectPromptsFromEditor() {
        List<String> out = new ArrayList<>();
        for (var node : questionsEditorBox.getChildren()) {
            if (node instanceof HBox row && !row.getChildren().isEmpty()
                    && row.getChildren().get(0) instanceof TextField tf) {
                String t = tf.getText() == null ? "" : tf.getText().trim();
                if (!t.isEmpty()) {
                    out.add(t);
                }
            }
        }
        return out;
    }

    private void addQuestionRow(String initialText) {
        TextField tf = new TextField(initialText);
        tf.setPromptText("Question text…");
        tf.getStyleClass().add("cm-text-field");
        HBox.setHgrow(tf, Priority.ALWAYS);

        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("cm-remove-btn");

        HBox row = new HBox(10, tf, removeBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        removeBtn.setOnAction(e -> {
            if (questionsEditorBox.getChildren().size() <= 1) {
                warn("Keep one question", "A test needs at least one question row.");
                return;
            }
            questionsEditorBox.getChildren().remove(row);
        });
        questionsEditorBox.getChildren().add(row);
    }

    private void beginEdit(CoachMentalTest test) {
        editingTestId = test.getId();
        formHeadingLabel.setText("Edit test");
        titleField.setText(safe(test.getTitle()));
        questionsEditorBox.getChildren().clear();
        for (CoachMentalTestQuestion q : test.getQuestions()) {
            addQuestionRow(safe(q.getPrompt()));
        }
        if (questionsEditorBox.getChildren().isEmpty()) {
            addQuestionRow("");
        }
        showFormView();
    }

    private void confirmDelete(CoachMentalTest test) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete test");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete “" + safe(test.getTitle()) + "”? This cannot be undone.");
        Optional<ButtonType> ans = confirm.showAndWait();
        if (ans.isPresent() && ans.get() == ButtonType.OK) {
            service.remove(test.getId());
            testsTable.refresh();
        }
    }

    private void showFormView() {
        listPane.setManaged(false);
        listPane.setVisible(false);
        formPane.setManaged(true);
        formPane.setVisible(true);
    }

    private void clearFormFields() {
        titleField.clear();
        questionsEditorBox.getChildren().clear();
    }

    private Callback<TableColumn<CoachMentalTest, Void>, TableCell<CoachMentalTest, Void>> actionColumnFactory() {
        return column -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(8, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.getStyleClass().add("cm-table-action");
                deleteBtn.getStyleClass().add("cm-table-action");
                editBtn.setOnAction(e -> {
                    CoachMentalTest row = getTableView().getItems().get(getIndex());
                    if (row != null) {
                        beginEdit(row);
                    }
                });
                deleteBtn.setOnAction(e -> {
                    CoachMentalTest row = getTableView().getItems().get(getIndex());
                    if (row != null) {
                        confirmDelete(row);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        };
    }

    private static void warn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
