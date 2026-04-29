package controllers;

import app.AppSession;
import models.MentalHealthAssessmentSubmission;
import models.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import models.CoachMentalTest;
import models.CoachMentalTestQuestion;
import models.MentalHealthEvaluation;
import services.CoachMentalTestService;
import services.GroqAiService;
import services.MentalHealthEvaluationRepository;
import services.MentalHealthSubmissionService;
import services.WeatherService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Mental health list + check-in tied to coach-authored tests (in-memory until DB is added).
 */
public class MentalHealthUserFragmentController {

    private static final List<String> SCALE = List.of(
            "1 — Very low",
            "2 — Low",
            "3 — Moderate",
            "4 — Good",
            "5 — Very good"
    );

    private static final List<String> LEGACY_PROMPTS = List.of(
            "How have you been feeling emotionally lately? (mood)",
            "How often do you feel stressed or anxious?",
            "How is your sleep quality recently?",
            "Do you feel motivated to do your daily activities?",
            "How mentally tired do you feel?"
    );

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CoachMentalTestService coachTestService = CoachMentalTestService.getInstance();
    private final MentalHealthEvaluationRepository evaluationRepository = new MentalHealthEvaluationRepository();
    private final ObservableList<MentalHealthEvaluation> evaluations = FXCollections.observableArrayList();
    private final List<ComboBox<String>> answerCombos = new ArrayList<>();
    private final List<String> currentPromptStrings = new ArrayList<>();

    private UUID editingId;
    /** Non-null while the form is built from a live coach test (also when editing that row). */
    private CoachMentalTest currentTest;

    @FXML
    private VBox listPane;
    @FXML
    private VBox formPane;
    @FXML
    private TableView<MentalHealthEvaluation> evaluationsTable;
    @FXML
    private TableColumn<MentalHealthEvaluation, String> colTestTitle;
    @FXML
    private TableColumn<MentalHealthEvaluation, String> colDate;
    @FXML
    private TableColumn<MentalHealthEvaluation, String> colScore;
    @FXML
    private TableColumn<MentalHealthEvaluation, String> colStatus;
    @FXML
    private TableColumn<MentalHealthEvaluation, Void> colAction;
    @FXML
    private Button submitCheckInBtn;
    @FXML
    private ComboBox<CoachMentalTest> testPicker;
    @FXML
    private VBox dynamicQuestionsBox;
    @FXML
    private TextArea notesArea;
    @FXML
    private Label formHeaderTitle;
    @FXML
    private Label formHeaderSubtitle;
    @FXML
    private TableView<MentalHealthAssessmentSubmission> recommendationsTable;
    @FXML
    private TableColumn<MentalHealthAssessmentSubmission, String> colRecDate;
    @FXML
    private TableColumn<MentalHealthAssessmentSubmission, String> colRecTest;
    @FXML
    private TableColumn<MentalHealthAssessmentSubmission, String> colRecSummary;
    @FXML
    private TableColumn<MentalHealthAssessmentSubmission, Void> colRecAction;
    @FXML
    private VBox weatherAdviceCard;
    @FXML
    private Label weatherIconLabel;
    @FXML
    private Label weatherInfoLabel;
    @FXML
    private Label aiAdviceLabel;

    private final ObservableList<MentalHealthAssessmentSubmission> recommendationRows =
            FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        testPicker.setItems(coachTestService.getTests());
        testPicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(CoachMentalTest t) {
                if (t == null) {
                    return "";
                }
                String title = t.getTitle();
                return title == null || title.isBlank() ? "(Untitled test)" : title;
            }

            @Override
            public CoachMentalTest fromString(String s) {
                return null;
            }
        });

        testPicker.valueProperty().addListener((o, ov, nv) -> {
            if (editingId != null) {
                return;
            }
            if (nv != null) {
                buildRowsForTest(nv, null);
            } else {
                clearDynamicQuestions();
                resetFormHeaderCopy();
            }
        });

        evaluationsTable.setItems(evaluations);
        evaluationsTable.setPlaceholder(new Label("No evaluations yet. Use “+ Add Mental Health” after your coach publishes a test."));

        colTestTitle.setCellValueFactory(cdf -> new SimpleStringProperty(cdf.getValue().getTestTitleForDisplay()));
        colDate.setCellValueFactory(cdf -> new SimpleStringProperty(
                cdf.getValue().getTestedAt() == null ? "—" : DATE_FMT.format(cdf.getValue().getTestedAt())));
        colScore.setCellValueFactory(cdf -> new SimpleStringProperty(
                cdf.getValue().getScore() + " / " + cdf.getValue().getMaxScoreForDisplay()));
        colStatus.setCellValueFactory(cdf -> new SimpleStringProperty(safe(cdf.getValue().getStatus())));

        colAction.setCellFactory(actionColumnFactory());

        setupRecommendationsTable();

        loadEvaluationsFromDatabase();

        showListView();
    }

    private void loadEvaluationsFromDatabase() {
        User u = AppSession.getCurrentUser();
        if (u == null || u.getId() == null) {
            return;
        }
        try {
            evaluations.setAll(evaluationRepository.findByUserId(u.getId()));
        } catch (SQLException e) {
            System.err.println("loadEvaluationsFromDatabase failed: " + e.getMessage());
        }
    }

    private void setupRecommendationsTable() {
        recommendationsTable.setItems(recommendationRows);
        recommendationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recommendationsTable.setPlaceholder(new Label("No coach recommendations yet."));
        recommendationsTable.setMinHeight(120);

        colRecDate.setCellValueFactory(cdf -> {
            var t = cdf.getValue().getTestedAt();
            return new SimpleStringProperty(t == null ? "—" : DATE_FMT.format(t));
        });
        colRecTest.setCellValueFactory(cdf -> {
            String tt = cdf.getValue().getCoachTestTitle();
            return new SimpleStringProperty(tt == null || tt.isBlank() ? "—" : tt);
        });
        colRecSummary.setCellValueFactory(cdf -> new SimpleStringProperty(cdf.getValue().getRecommendationPreview()));
        colRecSummary.setCellFactory(col -> new TableCell<MentalHealthAssessmentSubmission, String>() {
            private final Label wrap = new Label();

            {
                wrap.setWrapText(true);
                wrap.setMaxWidth(360);
                wrap.getStyleClass().add("mh-rec-cell-text");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    wrap.setText(item);
                    setGraphic(wrap);
                }
            }
        });

        colRecAction.setCellFactory(col -> new TableCell<>() {
            private final Button del = new Button("Delete");

            {
                del.getStyleClass().add("mh-rec-delete-btn");
                del.setOnAction(e -> {
                    TableRow<MentalHealthAssessmentSubmission> tr = getTableRow();
                    MentalHealthAssessmentSubmission row = tr != null ? tr.getItem() : null;
                    if (row == null) {
                        return;
                    }
                    User u = AppSession.getCurrentUser();
                    if (u == null || u.getId() == null) {
                        return;
                    }
                    Alert c = new Alert(Alert.AlertType.CONFIRMATION);
                    c.setTitle("Remove recommendation");
                    c.setHeaderText(null);
                    c.setContentText("Remove this recommendation from your list?");
                    Optional<ButtonType> ans = c.showAndWait();
                    if (ans.isPresent() && ans.get() == ButtonType.OK) {
                        MentalHealthSubmissionService.getInstance().clearRecommendationForUser(row.getId(), u.getId());
                        refreshMyRecommendations();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : del);
            }
        });
    }

    private Callback<TableColumn<MentalHealthEvaluation, Void>, TableCell<MentalHealthEvaluation, Void>> actionColumnFactory() {
        return column -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(8, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.getStyleClass().add("mh-table-action");
                deleteBtn.getStyleClass().add("mh-table-action");
                editBtn.setOnAction(e -> {
                    MentalHealthEvaluation row = getTableView().getItems().get(getIndex());
                    if (row != null) {
                        beginEdit(row);
                    }
                });
                deleteBtn.setOnAction(e -> {
                    MentalHealthEvaluation row = getTableView().getItems().get(getIndex());
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

    @FXML
    private void onAddMentalHealth() {
        if (coachTestService.getTests().isEmpty()) {
            warn("No tests yet", "Your coach has not created any mental health tests yet. Please try again later.");
            return;
        }
        testPicker.setItems(coachTestService.getTests());
        editingId = null;
        currentTest = null;
        testPicker.setDisable(false);
        testPicker.setValue(null);
        clearDynamicQuestions();
        resetFormHeaderCopy();
        notesArea.clear();
        if (submitCheckInBtn != null) {
            submitCheckInBtn.setText("Calculate My Wellbeing Score →");
        }
        loadWeatherAndAdvice();
        showFormView();
    }

    @FXML
    private void onBackToList() {
        editingId = null;
        currentTest = null;
        testPicker.setDisable(false);
        clearForm();
        showListView();
    }

    @FXML
    private void onCalculateWellbeing() {
        if (editingId == null && testPicker.getValue() == null) {
            warn("Choose a test", "Select one of your coach’s tests from the list.");
            return;
        }
        if (answerCombos.isEmpty()) {
            warn("No questions", "Pick a test to load its questions, then answer each one.");
            return;
        }
        for (ComboBox<String> c : answerCombos) {
            if (c.getValue() == null || c.getValue().isBlank()) {
                warn("Incomplete check-in", "Please choose an answer for every question.");
                return;
            }
        }

        List<Integer> scores = new ArrayList<>();
        for (ComboBox<String> c : answerCombos) {
            scores.add(scoreFromChoice(c.getValue()));
        }

        int maxScore = scores.size() * 5;
        int sum = scores.stream().mapToInt(Integer::intValue).sum();
        String status = wellbeingBand(sum, maxScore);
        String note = notesArea.getText() == null ? "" : notesArea.getText().trim();
        List<String> prompts = new ArrayList<>(currentPromptStrings);

        CoachMentalTest metaTest = currentTest != null ? currentTest : testPicker.getValue();

        MentalHealthEvaluation saved = null;
        if (editingId != null) {
            for (MentalHealthEvaluation ev : evaluations) {
                if (ev.getId().equals(editingId)) {
                    applyToEvaluation(ev, metaTest, prompts, scores, sum, status, note);
                    saved = ev;
                    break;
                }
            }
        } else {
            MentalHealthEvaluation ev = new MentalHealthEvaluation(UUID.randomUUID());
            ev.setTestedAt(LocalDateTime.now());
            applyToEvaluation(ev, metaTest, prompts, scores, sum, status, note);
            evaluations.add(0, ev);
            saved = ev;
        }
        if (saved != null) {
            User member = AppSession.getCurrentUser();
            if (member != null && member.getId() != null) {
                saved.setUserId(member.getId());
            }
            MentalHealthSubmissionService.getInstance().recordMemberSubmission(AppSession.getCurrentUser(), saved);
        }

        editingId = null;
        currentTest = null;
        testPicker.setDisable(false);
        clearForm();
        evaluationsTable.refresh();
        showListView();

        // Show result dialog — then enrich it with live weather asynchronously
        final int finalSum = sum;
        final int finalMax = maxScore;
        final String finalStatus = status;

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Check-in Saved");
        a.setHeaderText("Score: " + finalSum + " / " + finalMax + " — " + finalStatus);
        a.setContentText("Fetching current weather conditions…");
        a.show();

        double scoreRatio = finalMax > 0 ? (double) finalSum / finalMax : 0;
        WeatherService.getInstance().fetchWeather(null)
                .thenAccept(weather -> Platform.runLater(() -> {
                    String tip = weather.mentalTip(scoreRatio);
                    a.setContentText(
                            "🌍 Current weather: " + weather + "\n\n"
                            + "💡 " + tip
                    );
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            a.setContentText("Score: " + finalSum + " / " + finalMax + " — " + finalStatus
                                    + "\n\n(Weather unavailable)"));
                    return null;
                });
    }

    private void beginEdit(MentalHealthEvaluation ev) {
        editingId = ev.getId();
        testPicker.setItems(coachTestService.getTests());
        testPicker.setDisable(true);

        if (ev.getCoachTestId() != null) {
            Optional<CoachMentalTest> live = coachTestService.findById(ev.getCoachTestId());
            if (live.isPresent()) {
                CoachMentalTest t = live.get();
                testPicker.setValue(t);
                currentTest = t;
                buildRowsForTest(t, ev);
            } else {
                testPicker.setValue(null);
                currentTest = null;
                buildRowsFromSnapshot(ev);
            }
        } else {
            testPicker.setValue(null);
            currentTest = null;
            buildLegacyRows(ev);
        }

        notesArea.setText(ev.getNotes() == null ? "" : ev.getNotes());
        String title = ev.getCoachTestTitle();
        if (title != null && !title.isBlank()) {
            formHeaderTitle.setText(title);
        } else {
            formHeaderTitle.setText("Your check-in");
        }
        formHeaderSubtitle.setText(answerCombos.size() + " questions · 1–5 scale");

        if (submitCheckInBtn != null) {
            submitCheckInBtn.setText("Save changes →");
        }
        showFormView();
    }

    private void confirmDelete(MentalHealthEvaluation row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete evaluation");
        confirm.setHeaderText(null);
        confirm.setContentText("Remove this evaluation from " + DATE_FMT.format(row.getTestedAt()) + "?");
        Optional<ButtonType> ans = confirm.showAndWait();
        if (ans.isPresent() && ans.get() == ButtonType.OK) {
            try {
                evaluationRepository.delete(row.getId());
            } catch (SQLException e) {
                System.err.println("delete evaluation failed: " + e.getMessage());
            }
            evaluations.remove(row);
            MentalHealthSubmissionService.getInstance().reloadFromDatabase();
        }
    }

    private void applyToEvaluation(MentalHealthEvaluation ev, CoachMentalTest selectedTest,
                                   List<String> prompts, List<Integer> scores, int sum, String status, String notes) {
        if (ev.getTestedAt() == null) {
            ev.setTestedAt(LocalDateTime.now());
        }
        ev.setQuestionScores(scores);
        ev.setQuestionPrompts(prompts);
        if (selectedTest != null) {
            ev.setCoachTestId(selectedTest.getId());
            ev.setCoachTestTitle(safe(selectedTest.getTitle()));
        }
        ev.setScore(sum);
        ev.setStatus(status);
        syncFirstFiveLegacyFields(ev, scores);
        ev.setNotes(notes.isEmpty() ? null : notes);
    }

    private static void syncFirstFiveLegacyFields(MentalHealthEvaluation ev, List<Integer> scores) {
        for (int i = 0; i < 5; i++) {
            int v = i < scores.size() ? scores.get(i) : 0;
            switch (i) {
                case 0 -> ev.setMood(v);
                case 1 -> ev.setStress(v);
                case 2 -> ev.setSleep(v);
                case 3 -> ev.setMotivation(v);
                case 4 -> ev.setMentalTired(v);
                default -> { }
            }
        }
    }

    private void clearForm() {
        testPicker.setValue(null);
        clearDynamicQuestions();
        currentTest = null;
        notesArea.clear();
    }

    private void clearDynamicQuestions() {
        dynamicQuestionsBox.getChildren().clear();
        answerCombos.clear();
        currentPromptStrings.clear();
    }

    private void resetFormHeaderCopy() {
        formHeaderTitle.setText("Your Daily Check-in");
        formHeaderSubtitle.setText("Choose a coach test, then answer each question");
    }

    private void buildRowsForTest(CoachMentalTest test, MentalHealthEvaluation pref) {
        clearDynamicQuestions();
        currentTest = test;
        List<CoachMentalTestQuestion> qs = new ArrayList<>(test.getQuestions());
        qs.sort(Comparator.comparingInt(CoachMentalTestQuestion::getOrderIndex));
        List<Integer> prefScores = pref != null ? pref.getQuestionScores() : List.of();
        for (int i = 0; i < qs.size(); i++) {
            Integer preset = i < prefScores.size() ? prefScores.get(i) : null;
            appendQuestionRow(qs.get(i).getPrompt(), preset);
        }
        formHeaderTitle.setText(test.getTitle() == null || test.getTitle().isBlank() ? "Assessment" : test.getTitle());
        formHeaderSubtitle.setText(qs.size() + " questions · max score " + test.getMaxScore());
    }

    private void buildRowsFromSnapshot(MentalHealthEvaluation ev) {
        clearDynamicQuestions();
        currentTest = null;
        List<String> prompts = new ArrayList<>(ev.getQuestionPrompts());
        List<Integer> scores = new ArrayList<>(ev.getQuestionScores());
        for (int i = 0; i < prompts.size(); i++) {
            Integer preset = i < scores.size() ? scores.get(i) : null;
            appendQuestionRow(prompts.get(i), preset);
        }
    }

    private void buildLegacyRows(MentalHealthEvaluation ev) {
        clearDynamicQuestions();
        currentTest = null;
        List<Integer> preset = List.of(ev.getMood(), ev.getStress(), ev.getSleep(), ev.getMotivation(), ev.getMentalTired());
        for (int i = 0; i < LEGACY_PROMPTS.size(); i++) {
            int v = i < preset.size() ? preset.get(i) : 0;
            Integer p = (v >= 1 && v <= 5) ? v : null;
            appendQuestionRow(LEGACY_PROMPTS.get(i), p);
        }
    }

    private void appendQuestionRow(String prompt, Integer presetScore) {
        String text = prompt == null ? "" : prompt.trim();
        currentPromptStrings.add(text.isEmpty() ? "Question" : text);

        Label lbl = new Label(text.isEmpty() ? "Question" : text);
        lbl.getStyleClass().add("mh-question-label");
        lbl.setWrapText(true);

        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(SCALE));
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setMinHeight(52);
        combo.setPrefHeight(52);
        combo.setPromptText("Choose an answer...");
        combo.getStyleClass().add("mh-combo");
        if (presetScore != null && presetScore >= 1 && presetScore <= 5) {
            combo.setValue(SCALE.get(presetScore - 1));
        }

        VBox block = new VBox(8, lbl, combo);
        block.getStyleClass().add("mh-question-block");
        dynamicQuestionsBox.getChildren().add(block);
        answerCombos.add(combo);
    }

    private void showListView() {
        listPane.setManaged(true);
        listPane.setVisible(true);
        formPane.setManaged(false);
        formPane.setVisible(false);
        refreshMyRecommendations();
    }

    private void refreshMyRecommendations() {
        if (recommendationsTable == null) {
            return;
        }
        recommendationRows.clear();
        User u = AppSession.getCurrentUser();
        if (u == null || u.getId() == null) {
            return;
        }
        List<MentalHealthAssessmentSubmission> rows = MentalHealthSubmissionService.getInstance().getSubmissions().stream()
                .filter(s -> u.getId().equals(s.getUserId()) && s.hasRecommendationContent())
                .sorted(Comparator.comparing(MentalHealthAssessmentSubmission::getTestedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        recommendationRows.setAll(rows);
        recommendationsTable.refresh();
    }

    private void showFormView() {
        listPane.setManaged(false);
        listPane.setVisible(false);
        formPane.setManaged(true);
        formPane.setVisible(true);
    }

    private static void warn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static int scoreFromChoice(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        char first = value.charAt(0);
        if (first >= '1' && first <= '5') {
            return first - '0';
        }
        return 0;
    }

    /** Same relative bands as the old 25-point scale: ~48% / ~72% thresholds. */
    private static String wellbeingBand(int sum, int maxScore) {
        if (maxScore <= 0) {
            maxScore = 25;
        }
        double ratio = sum / (double) maxScore;
        if (ratio <= 0.48) {
            return "Needs attention";
        }
        if (ratio <= 0.72) {
            return "Moderate";
        }
        return "Good";
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    private void loadWeatherAndAdvice() {
        if (weatherIconLabel == null || weatherInfoLabel == null || aiAdviceLabel == null) {
            System.err.println("loadWeatherAndAdvice: one or more labels are null, skipping");
            return;
        }
        
        // Reset to loading state
        weatherIconLabel.setText("⏳");
        weatherInfoLabel.setText("Fetching weather…");
        aiAdviceLabel.setText("Getting personalised advice…");

        // Fetch weather first
        WeatherService.getInstance().fetchWeather(null)
                .thenCompose(weather -> {
                    // Update weather UI
                    Platform.runLater(() -> {
                        weatherIconLabel.setText(weather.emoji());
                        weatherInfoLabel.setText(String.format("%s — %.1f°C, %s",
                                weather.city, weather.tempCelsius, weather.description));
                    });
                    // Then fetch AI advice based on weather
                    return GroqAiService.getInstance().generateWeatherAdvice(weather);
                })
                .thenAccept(advice -> Platform.runLater(() -> {
                    aiAdviceLabel.setText("💡 " + advice);
                }))
                .exceptionally(ex -> {
                    System.err.println("Weather/AI error: " + ex.getMessage());
                    ex.printStackTrace();
                    // Try weather only without AI
                    WeatherService.getInstance().fetchWeather(null)
                        .thenAccept(weather -> Platform.runLater(() -> {
                            weatherIconLabel.setText(weather.emoji());
                            weatherInfoLabel.setText(String.format("%s — %.1f°C, %s",
                                    weather.city, weather.tempCelsius, weather.description));
                            aiAdviceLabel.setText("💡 " + weather.mentalTip(0.5));
                        }))
                        .exceptionally(ex2 -> {
                            Platform.runLater(() -> {
                                weatherIconLabel.setText("⚠️");
                                weatherInfoLabel.setText("Weather unavailable");
                                aiAdviceLabel.setText("Take a moment to check in with yourself before starting.");
                            });
                            return null;
                        });
                    return null;
                });
    }
}
