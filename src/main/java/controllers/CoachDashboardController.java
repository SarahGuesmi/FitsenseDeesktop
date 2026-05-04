package controllers;

import app.AppSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.Questionnaire;
import models.User;
import models.Workout;
import services.UserService;
import services.WorkoutService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Coach workspace: same shell as the admin dashboard (sidebar + navbar), with read-only athlete list.
 */
public class CoachDashboardController {

    private static final String ADMIN_EMAIL = "sarahguesmi223@gmail.com";

    @FXML
    private VBox coachDashboardPane;
    @FXML
    private VBox athletesPane;
    @FXML
    private VBox chatroomPane;
    @FXML
    private VBox workoutCatalogPane;
    @FXML
    private VBox feedbackPane;
    @FXML
    private VBox mentalWellnessPane;
    @FXML
    private VBox mentalTestsPane;
    @FXML
    private VBox mentalAssessmentsPane;
    @FXML
    private javafx.scene.control.ScrollPane securityPane;
    @FXML
    private VBox nutritionPane;
    @FXML
    private VBox profilePane;
    @FXML
    private javafx.scene.layout.StackPane logoutOverlay;

    @FXML
    private Button coachHomeBtn;
    @FXML
    private Button athletesBtn;
    @FXML
    private Button chatroomBtn;
    @FXML
    private Button workoutCatalogBtn;
    @FXML
    private Button feedbackBtn;
    @FXML
    private Button mentalWellnessBtn;
    @FXML
    private Button mentalTestsBtn;
    @FXML
    private Button mentalAssessmentsBtn;
    @FXML
    private Button securityBtn;
    @FXML
    private Button nutritionBtn;
    @FXML
    private Button profileBtn;

    @FXML
    private Label navbarPageTitle;
    @FXML
    private Label navbarPageSubtitle;
    @FXML
    private Button navbarBellBtn;
    @FXML
    private Label navbarUserName;
    @FXML
    private Label navbarUserRole;
    @FXML
    private Label navbarAvatar;

    @FXML private Label totalAthletesLabel;
    @FXML private Label activeAthletesLabel;
    @FXML private Label sessionsLabel;
    @FXML private Label positiveSentimentLabel;
    @FXML private VBox ratingChartContainer;
    @FXML private VBox sentimentChartContainer;
    @FXML private TableView<models.FeedbackResponse> dashFeedbackTable;
    @FXML private TableColumn<models.FeedbackResponse, String> dashUserCol;
    @FXML private TableColumn<models.FeedbackResponse, String> dashWorkoutCol;
    @FXML private TableColumn<models.FeedbackResponse, String> dashRatingCol;
    @FXML private TableColumn<models.FeedbackResponse, String> dashSentimentCol;
    @FXML private TableColumn<models.FeedbackResponse, String> dashCommentCol;
    @FXML private TableColumn<models.FeedbackResponse, String> dashDateCol;
    @FXML private VBox dashWorkoutsContainer;

    @FXML private TableView<User> athletesTable;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, String> statusCol;
    @FXML private TableColumn<User, String> dateCol;

    // Feedback management fields
    @FXML
    private javafx.scene.control.TextField feedbackSearchField;
    @FXML
    private TableView<models.Questionnaire> feedbackTable;
    @FXML
    private TableColumn<models.Questionnaire, String> feedbackTitleCol;
    @FXML
    private TableColumn<models.Questionnaire, String> feedbackWorkoutsCol;
    @FXML
    private TableColumn<models.Questionnaire, String> feedbackOptionsCol;
    @FXML
    private TableColumn<models.Questionnaire, String> feedbackActionsCol;

    // Modal overlay fields
    @FXML private StackPane feedbackModalOverlay;
    @FXML private javafx.scene.control.ScrollPane feedbackModalScroll;
    @FXML private StackPane deleteConfirmOverlay;
    @FXML private TextField modalTitreField;
    @FXML private VBox workoutsCheckboxList;
    @FXML private HBox optionsList;  // now a star preview HBox, not editable
    @FXML private Label modalTitreErrorLabel;
    @FXML private Label workoutsErrorLabel;

    // Recent responses
    @FXML private VBox responsesContainer;
    @FXML private Label responsesCountLabel;

    // Sidebar avatar
    @FXML private javafx.scene.image.ImageView sidebarAvatarView;
    @FXML private Label sidebarAvatarInitials;

    // Sub-controllers (fx:include) — JavaFX injects as fx:id + "Controller"
    @FXML private controllers.CoachMentalTestsFragmentController coachMentalTestsController;
    @FXML private controllers.CoachMentalAssessmentsFragmentController coachMentalAssessmentsController;
    @FXML private controllers.ProfileFragmentController coachProfileController;
    @FXML private controllers.TwoFactorSetupController coachSecurityController;
    @FXML private controllers.ChatroomController chatroomController;

    private Questionnaire pendingDeleteQuestionnaire;

    private final UserService userService = new UserService();
    private final WorkoutService workoutService = new WorkoutService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML
    private void initialize() {
        try {
            setupAthletesTable();
            if (coachProfileController != null) {
                coachProfileController.setAfterSaveCallback(this::refreshNavbar);
            }
            refreshNavbar();
            onShowDashboard();
        } catch (Exception e) {
            // Write error to a log file so we can read it
            try {
                java.io.FileWriter fw = new java.io.FileWriter("coach_error.log", false);
                fw.write("CoachDashboardController.initialize() FAILED:\n");
                fw.write(e.toString() + "\n");
                for (StackTraceElement el : e.getStackTrace()) {
                    fw.write("  at " + el.toString() + "\n");
                }
                if (e.getCause() != null) {
                    fw.write("Caused by: " + e.getCause().toString() + "\n");
                    for (StackTraceElement el : e.getCause().getStackTrace()) {
                        fw.write("  at " + el.toString() + "\n");
                    }
                }
                fw.close();
            } catch (Exception ignored) {}
            throw e;
        }
    }

    @FXML
    private void onShowDashboard() {
        hideAllContent();
        if (coachDashboardPane == null) return;
        coachDashboardPane.setManaged(true);
        coachDashboardPane.setVisible(true);
        setNavbarText("Coach dashboard", "Train and support your athletes");
        setActiveSidebar(coachHomeBtn);
        refreshStats();
    }

    @FXML
    private void onShowAthletes() {
        hideAllContent();
        athletesPane.setManaged(true);
        athletesPane.setVisible(true);
        setNavbarText("Athletes", "Directory of users (read-only)");
        setActiveSidebar(athletesBtn);
        refreshAthletesTable();
    }

    @FXML
    private void onShowChatroom() {
        hideAllContent();
        chatroomPane.setManaged(true);
        chatroomPane.setVisible(true);
        setNavbarText("Chatroom", "Team messaging");
        setActiveSidebar(chatroomBtn);
        // Reload contacts now that user is in session
        if (chatroomController != null) {
            chatroomController.refresh();
        }
    }

    @FXML
    private void onShowWorkoutCatalog() {
        hideAllContent();
        workoutCatalogPane.setManaged(true);
        workoutCatalogPane.setVisible(true);
        setNavbarText("Workout catalog", "Programs and exercises");
        setActiveSidebar(workoutCatalogBtn);
    }

    @FXML
    private void onShowFeedback() {
        hideAllContent();
        feedbackPane.setManaged(true);
        feedbackPane.setVisible(true);
        setNavbarText("Feedback Management", "Create and manage feedback questionnaires for your athletes.");
        setActiveSidebar(feedbackBtn);
        setupFeedbackTable();
        refreshFeedbackTable();
        refreshResponseCards();
    }

    @FXML
    private void onAddFeedback() {
        clearFeedbackValidation();
        workoutsCheckboxList.getChildren().clear();
        modalTitreField.clear();
        // Load workouts from DB
        try {
            List<Workout> workouts = new services.WorkoutService().read();
            for (Workout w : workouts) {
                CheckBox cb = new CheckBox(w.getNom());
                cb.setUserData(w);
                cb.setStyle("-fx-text-fill: #eff4ff; -fx-font-size: 14px;");
                workoutsCheckboxList.getChildren().add(cb);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Options are now fixed 5-star scale (no manual entry needed)
        feedbackModalOverlay.setManaged(true);
        feedbackModalOverlay.setVisible(true);
    }

    @FXML
    private void onCancelDelete() {
        pendingDeleteQuestionnaire = null;
        deleteConfirmOverlay.setManaged(false);
        deleteConfirmOverlay.setVisible(false);
    }

    @FXML
    private void onConfirmDelete() {
        if (pendingDeleteQuestionnaire == null) return;
        try {
            new services.FeedbackService(utils.DbConnection.getInstance().getCnx()).delete(pendingDeleteQuestionnaire);
            pendingDeleteQuestionnaire = null;
            deleteConfirmOverlay.setManaged(false);
            deleteConfirmOverlay.setVisible(false);
            refreshFeedbackTable();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Could not delete: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onCloseFeedbackModal() {
        feedbackModalOverlay.setManaged(false);
        feedbackModalOverlay.setVisible(false);
        clearFeedbackValidation();
    }

    private void clearFeedbackValidation() {
        if (modalTitreErrorLabel != null) {
            modalTitreErrorLabel.setVisible(false);
        }
        if (workoutsErrorLabel != null) {
            workoutsErrorLabel.setVisible(false);
        }
        if (modalTitreField != null) {
            modalTitreField.getStyleClass().remove("modal-field-error");
        }
        if (workoutsCheckboxList != null) {
            workoutsCheckboxList.getStyleClass().remove("modal-field-error");
        }
    }

    @FXML
    private void onSaveFeedbackModal() {
        clearFeedbackValidation();
        String titre = modalTitreField.getText() == null ? "" : modalTitreField.getText().trim();
        // Collect selected workouts
        List<Workout> selectedWorkouts = new ArrayList<>();
        for (javafx.scene.Node node : workoutsCheckboxList.getChildren()) {
            if (node instanceof CheckBox cb && cb.isSelected()) {
                selectedWorkouts.add((Workout) cb.getUserData());
            }
        }
        boolean hasValidationError = false;
        if (titre.isEmpty()) {
            if (modalTitreErrorLabel != null) modalTitreErrorLabel.setVisible(true);
            if (modalTitreField != null && !modalTitreField.getStyleClass().contains("modal-field-error")) {
                modalTitreField.getStyleClass().add("modal-field-error");
            }
            hasValidationError = true;
        }
        if (selectedWorkouts.isEmpty()) {
            if (workoutsErrorLabel != null) workoutsErrorLabel.setVisible(true);
            if (workoutsCheckboxList != null && !workoutsCheckboxList.getStyleClass().contains("modal-field-error")) {
                workoutsCheckboxList.getStyleClass().add("modal-field-error");
            }
            hasValidationError = true;
        }
        if (hasValidationError) {
            return;
        }
        // Options are always the fixed 5-star scale
        String jsonOptions = "[\"1 Very poor\",\"2 Poor\",\"3 Average\",\"4 Good\",\"5 Excellent\"]";
        Questionnaire q = new Questionnaire();
        // Check if editing existing
        Object userData = modalTitreField.getUserData();
        if (userData instanceof UUID existingId) {
            q.setId(existingId);
        }
        q.setTitre(titre);
        q.setType("template");
        q.setOptions(jsonOptions);
        q.setDateSoumission(Instant.now());
        selectedWorkouts.forEach(q::addWorkout);
        String selectedWorkoutTitles = selectedWorkouts.stream()
                .map(Workout::getNom)
                .filter(title -> title != null && !title.isBlank())
                .collect(Collectors.joining(", "));
        q.setExercicesCompris(selectedWorkoutTitles);
        if (AppSession.getCurrentUser() != null) q.setCoach(AppSession.getCurrentUser());
        try {
            services.FeedbackService fs = new services.FeedbackService(utils.DbConnection.getInstance().getCnx());
            if (q.getId() != null) {
                fs.update(q);
            } else {
                fs.createPrepared(q);
            }
            modalTitreField.setUserData(null);
            onCloseFeedbackModal();
            refreshFeedbackTable();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Could not save: " + e.getMessage()).showAndWait();
        }
    }

    private void setupFeedbackTable() {
        if (feedbackTitleCol == null) return;

        // TITLE col — bold white text
        feedbackTitleCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTitre()));
        feedbackTitleCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label lbl = new Label(item);
                lbl.setStyle("-fx-text-fill: #f4f8ff; -fx-font-weight: 700; -fx-font-size: 14px;");
                setGraphic(lbl);
                setText(null);
            }
        });

        // LINKED WORKOUTS col — badge per workout
        feedbackWorkoutsCol.setCellValueFactory(data -> {
            String workoutNames = data.getValue().getWorkouts().stream()
                    .map(Workout::getNom)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.joining(", "));
            if (workoutNames.isBlank()) {
                workoutNames = data.getValue().getExercicesCompris() != null ? data.getValue().getExercicesCompris() : "";
            }
            return new SimpleStringProperty(workoutNames);
        });
        feedbackWorkoutsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) { setGraphic(null); setText(null); return; }
                HBox box = new HBox(6);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                for (String w : item.split(",")) {
                    if (w.isBlank()) continue;
                    Label badge = new Label("↔ " + w.trim());
                    badge.setStyle("-fx-background-color: rgba(58,141,255,0.15); -fx-text-fill: #5ab4ff;" +
                            "-fx-background-radius: 20px; -fx-border-color: rgba(90,180,255,0.5);" +
                            "-fx-border-radius: 20px; -fx-padding: 4 10; -fx-font-size: 12px; -fx-font-weight: 700;");
                    box.getChildren().add(badge);
                }
                setGraphic(box);
                setText(null);
            }
        });

        // OPTIONS col — "X options" badge
        feedbackOptionsCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOptions()));
        feedbackOptionsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); setText(null); return; }
                int count = 0;
                if (item != null && !item.isBlank()) {
                    count = item.split(",").length;
                }
                Label badge = new Label(count + " options");
                badge.setStyle("-fx-background-color: rgba(30,40,65,0.9); -fx-text-fill: #c8d8ff;" +
                        "-fx-background-radius: 20px; -fx-border-color: rgba(140,170,255,0.35);" +
                        "-fx-border-radius: 20px; -fx-padding: 4 12; -fx-font-size: 12px; -fx-font-weight: 700;");
                setGraphic(badge);
                setText(null);
            }
        });

        // ACTIONS col — edit + delete buttons
        feedbackActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✎");
            private final Button deleteBtn = new Button("🗑");
            {
                editBtn.getStyleClass().addAll("table-icon-btn", "icon-edit");
                deleteBtn.getStyleClass().addAll("table-icon-btn", "icon-delete");
                editBtn.setOnAction(e -> {
                    Questionnaire q = getTableView().getItems().get(getIndex());
                    openEditModal(q);
                });
                deleteBtn.setOnAction(e -> {
                    Questionnaire q = getTableView().getItems().get(getIndex());
                    pendingDeleteQuestionnaire = q;
                    deleteConfirmOverlay.setManaged(true);
                    deleteConfirmOverlay.setVisible(true);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(8, editBtn, deleteBtn);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        if (feedbackSearchField != null) {
            feedbackSearchField.textProperty().addListener((obs, oldVal, newVal) -> refreshFeedbackTable());
        }
    }

    private void openEditModal(Questionnaire q) {
        workoutsCheckboxList.getChildren().clear();
        optionsList.getChildren().clear();
        modalTitreField.setText(q.getTitre());
        // Load workouts
        try {
            List<Workout> workouts = new services.WorkoutService().read();
            java.util.Set<String> linkedWorkoutNames = q.getWorkouts().stream()
                    .map(Workout::getNom)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.toSet());
            if (linkedWorkoutNames.isEmpty() && q.getExercicesCompris() != null) {
                for (String title : q.getExercicesCompris().split(",")) {
                    if (!title.isBlank()) linkedWorkoutNames.add(title.trim());
                }
            }
            for (Workout w : workouts) {
                CheckBox cb = new CheckBox(w.getNom());
                cb.setUserData(w);
                cb.setStyle("-fx-text-fill: #eff4ff; -fx-font-size: 14px;");
                boolean linked = w.getNom() != null && linkedWorkoutNames.contains(w.getNom());
                cb.setSelected(linked);
                workoutsCheckboxList.getChildren().add(cb);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        // Options are now fixed 5-star scale — no need to load them into the modal
        // Store id for update
        modalTitreField.setUserData(q.getId());
        clearFeedbackValidation();
        feedbackModalOverlay.setManaged(true);
        feedbackModalOverlay.setVisible(true);
    }

    private void refreshFeedbackTable() {
        if (feedbackTable == null) return;
        try {
            services.FeedbackService qs = new services.FeedbackService(utils.DbConnection.getInstance().getCnx());
            java.util.List<models.Questionnaire> all = qs.read();
            String search = feedbackSearchField != null ? feedbackSearchField.getText().toLowerCase() : "";
            java.util.List<models.Questionnaire> filtered = all.stream()
                    .filter(q -> search.isEmpty() || (q.getTitre() != null && q.getTitre().toLowerCase().contains(search)))
                    .collect(java.util.stream.Collectors.toList());
            feedbackTable.setItems(javafx.collections.FXCollections.observableArrayList(filtered));
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onSendDailyReport() {
        try {
            List<models.FeedbackResponse> responses = new services.FeedbackResponseService().readWithDetails();
            utils.EmailReportService.sendDailyReport(responses);
            new Alert(Alert.AlertType.INFORMATION, "Daily report sent to nourammarr23@icloud.com").showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Failed to send report: " + e.getMessage()).showAndWait();
        }
    }

    private void refreshResponseCards() {
        if (responsesContainer == null) return;
        responsesContainer.getChildren().clear();

        List<models.FeedbackResponse> responses;
        try {
            responses = new services.FeedbackResponseService().readWithDetails();
        } catch (SQLException e) {
            return;
        }

        if (responsesCountLabel != null)
            responsesCountLabel.setText(responses.size() + " responses");

        HBox row = null;
        for (int i = 0; i < responses.size(); i++) {
            if (i % 3 == 0) {
                row = new HBox(14);
                row.setAlignment(Pos.TOP_LEFT);
                responsesContainer.getChildren().add(row);
            }
            row.getChildren().add(buildResponseCard(responses.get(i)));
        }
    }

    private VBox buildResponseCard(models.FeedbackResponse f) {
        VBox card = new VBox(8);
        card.getStyleClass().add("response-card");
        card.setPrefWidth(300);
        card.setMaxWidth(340);
        HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);

        // Analyze sentiment from comment if not already set
        String sentiment = f.getSentiment();
        if ((sentiment == null || sentiment.isBlank()) && f.getComment() != null && !f.getComment().isBlank()) {
            utils.SentimentAnalyzer.AnalysisResult result = utils.SentimentAnalyzer.analyze(f.getComment());
            sentiment = result.sentiment();
            try {
                f.setSentiment(sentiment);
                f.setKeywords(result.keywords());
                new services.FeedbackResponseService().update(f);
            } catch (SQLException ignored) {}
        }

        String rating = f.getRating() != null ? f.getRating() : "N/A";
        String workoutName = f.getWorkout() != null ? f.getWorkout().getNom() : null;

        Label titleLbl = new Label("Workout Feedback");
        titleLbl.getStyleClass().add("rc-title");

        // Rating badge (user's choice)
        Label ratingBadge = new Label(rating);
        ratingBadge.getStyleClass().addAll("rc-badge", sentimentBadgeStyle(rating));

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("rc-delete-btn");
        deleteBtn.setOnAction(e -> {
            try {
                new services.FeedbackResponseService().delete(f);
                refreshResponseCards();
            } catch (SQLException ex) {
                new Alert(Alert.AlertType.ERROR, "Could not delete: " + ex.getMessage()).showAndWait();
            }
        });

        HBox header = new HBox(8, titleLbl, ratingBadge, deleteBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleLbl, javafx.scene.layout.Priority.ALWAYS);

        Label userLbl = new Label("👤 " + f.getUserName());
        userLbl.getStyleClass().add("rc-user");
        card.getChildren().addAll(header, userLbl);

        if (workoutName != null && !workoutName.isBlank()) {
            Label workoutBadge = new Label("↔ " + workoutName);
            workoutBadge.getStyleClass().add("rc-workout-badge");
            card.getChildren().add(workoutBadge);
        }

        if (f.getComment() != null && !f.getComment().isBlank()) {
            Label commentLbl = new Label("❝ " + f.getComment());
            commentLbl.getStyleClass().add("rc-comment");
            commentLbl.setWrapText(true);
            card.getChildren().add(commentLbl);

            // Sentiment badge
            if (sentiment != null && !sentiment.isBlank()) {
                Label sentimentLbl = new Label("🧠 " + sentiment);
                sentimentLbl.getStyleClass().addAll("rc-badge", sentimentBadgeStyle(sentiment));
                card.getChildren().add(sentimentLbl);
            }

            // Keywords badges
            String keywords = f.getKeywords();
            if (keywords != null && !keywords.isBlank()) {
                HBox kwBox = new HBox(6);
                kwBox.setAlignment(Pos.CENTER_LEFT);
                // Strip JSON brackets and quotes
                String cleaned = keywords.replaceAll("[\\[\\]\"]", "");
                for (String kw : cleaned.split(",")) {
                    String k = kw.trim();
                    if (!k.isEmpty()) {
                        Label kwLbl = new Label("# " + k);
                        kwLbl.getStyleClass().add("rc-keyword");
                        kwBox.getChildren().add(kwLbl);
                    }
                }
                card.getChildren().add(kwBox);
            }
        }

        if (f.getCreatedAt() != null) {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter
                    .ofPattern("MMM dd, yyyy HH:mm")
                    .withZone(java.time.ZoneId.systemDefault());
            Label dateLbl = new Label("🕐 " + fmt.format(f.getCreatedAt()));
            dateLbl.getStyleClass().add("rc-date");
            card.getChildren().add(dateLbl);
        }

        card.setPadding(new Insets(14));
        return card;
    }

    private String sentimentBadgeStyle(String sentiment) {
        if (sentiment == null) return "rc-badge-default";
        return switch (sentiment.toLowerCase()) {
            case "positive" -> "rc-badge-positive";
            case "negative" -> "rc-badge-negative";
            default -> "rc-badge-neutral";
        };
    }

    @FXML
    private void onShowMentalWellness() {
        hideAllContent();
        mentalWellnessPane.setManaged(true);
        mentalWellnessPane.setVisible(true);
        setNavbarText("Mental wellness", "Well-being resources");
        setActiveSidebar(mentalWellnessBtn);
    }

    @FXML
    private void onShowMentalTests() {
        hideAllContent();
        mentalTestsPane.setManaged(true);
        mentalTestsPane.setVisible(true);
        setNavbarText("Tests & questionnaires", "Mental health tests");
        setActiveSidebar(mentalTestsBtn);
    }

    @FXML
    private void onShowMentalAssessments() {
        hideAllContent();
        mentalAssessmentsPane.setManaged(true);
        mentalAssessmentsPane.setVisible(true);
        setNavbarText("Latest assessments", "Member mental assessments");
        setActiveSidebar(mentalAssessmentsBtn);
    }

    @FXML
    private void onShowSecurity() {
        hideAllContent();
        securityPane.setManaged(true);
        securityPane.setVisible(true);
        setNavbarText("Security", "Account security settings");
        setActiveSidebar(securityBtn);
    }

    @FXML
    private void onShowNutrition() {
        hideAllContent();
        nutritionPane.setManaged(true);
        nutritionPane.setVisible(true);
        setNavbarText("Nutrition plan", "Meals and macros");
        setActiveSidebar(nutritionBtn);
    }

    @FXML
    private void onShowProfile() {
        hideAllContent();
        profilePane.setManaged(true);
        profilePane.setVisible(true);
        setNavbarText("Profile", "Your account and physique");
        setActiveSidebar(profileBtn);
        if (coachProfileController != null) {
            coachProfileController.reloadFromSession();
        }
    }

    @FXML
    private void onLogout() {
        if (logoutOverlay != null) {
            logoutOverlay.setManaged(true);
            logoutOverlay.setVisible(true);
        } else {
            doLogout();
        }
    }

    @FXML
    private void onCancelLogout() {
        if (logoutOverlay != null) {
            logoutOverlay.setManaged(false);
            logoutOverlay.setVisible(false);
        }
    }

    @FXML
    private void onConfirmLogout() {
        doLogout();
    }

    private void doLogout() {
        AppSession.setCurrentUser(null);
        AppSession.resetOnboarding();
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    private void hideAllContent() {
        javafx.scene.layout.Region[] panes = {
            coachDashboardPane, athletesPane, chatroomPane, workoutCatalogPane,
            feedbackPane, mentalWellnessPane, mentalTestsPane, mentalAssessmentsPane,
            nutritionPane, profilePane, securityPane
        };
        for (javafx.scene.layout.Region p : panes) {
            if (p != null) {
                p.setManaged(false);
                p.setVisible(false);
            }
        }
    }

    private void setNavbarText(String title, String subtitle) {
        if (navbarPageTitle != null) {
            navbarPageTitle.setText(title);
        }
        if (navbarPageSubtitle != null) {
            navbarPageSubtitle.setText(subtitle);
        }
    }

    private void refreshNavbar() {
        if (navbarBellBtn != null) {
            navbarBellBtn.setText("\uD83D\uDD14");
        }
        User session = AppSession.getCurrentUser();
        if (session == null) {
            return;
        }
        String initials = userInitials(session);
        if (navbarUserName != null) {
            navbarUserName.setText(initials);
        }
        if (navbarAvatar != null) {
            navbarAvatar.setText(initials);
        }
        if (navbarUserRole != null) {
            navbarUserRole.setText("Coach");
        }
    }

    private static String userInitials(User u) {
        String f = safe(u.getFirstname()).trim();
        String l = safe(u.getLastname()).trim();
        StringBuilder sb = new StringBuilder();
        if (!f.isEmpty()) {
            sb.append(Character.toUpperCase(f.charAt(0)));
        }
        if (!l.isEmpty()) {
            sb.append(Character.toUpperCase(l.charAt(0)));
        }
        if (sb.length() > 0) {
            return sb.toString();
        }
        String email = safe(u.getEmail());
        if (!email.isEmpty()) {
            return email.substring(0, Math.min(2, email.length())).toUpperCase();
        }
        return "CH";
    }

    private void setActiveSidebar(Button selected) {
        Button[] buttons = {coachHomeBtn, athletesBtn, chatroomBtn, workoutCatalogBtn,
                feedbackBtn, mentalWellnessBtn, mentalTestsBtn, mentalAssessmentsBtn,
                nutritionBtn, profileBtn, securityBtn};
        for (Button b : buttons) {
            if (b != null) {
                b.getStyleClass().remove("side-link-active");
            }
        }
        if (selected != null && !selected.getStyleClass().contains("side-link-active")) {
            selected.getStyleClass().add("side-link-active");
        }
    }

    private void setupAthletesTable() {
        if (athletesTable == null || nameCol == null) return;
        athletesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        nameCol.setCellValueFactory(data -> new SimpleStringProperty(
                (safe(data.getValue().getFirstname()) + " " + safe(data.getValue().getLastname())).trim()));
        roleCol.setCellValueFactory(data -> new SimpleStringProperty(extractRole(data.getValue().getRolesJson())));
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getAccountStatus())));
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDateCreation() == null ? "" : dateFormatter.format(data.getValue().getDateCreation())));

        nameCol.setCellFactory(col -> new TableCell<>() {
            private final Label fullNameLabel = new Label();
            private final Label emailLabel = new Label();
            private final VBox wrapper = new VBox(2, fullNameLabel, emailLabel);

            {
                fullNameLabel.getStyleClass().add("user-name");
                emailLabel.getStyleClass().add("user-email");
                wrapper.getStyleClass().add("user-cell");
            }

            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                User user = getTableRow().getItem();
                fullNameLabel.setText(name == null || name.isBlank() ? "Unknown user" : name);
                emailLabel.setText(safe(user.getEmail()));
                setGraphic(wrapper);
            }
        });

        roleCol.setCellFactory(col -> new TableCell<>() {
            private final Label roleBadge = new Label();

            {
                roleBadge.getStyleClass().add("role-badge");
            }

            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setGraphic(null);
                    return;
                }
                roleBadge.setText(role);
                setGraphic(roleBadge);
            }
        });

        statusCol.setCellFactory(col -> new TableCell<>() {
            private final Label statusBadge = new Label();

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                statusBadge.getStyleClass().setAll("status-badge",
                        "active".equalsIgnoreCase(status) ? "status-active" : "status-inactive");
                statusBadge.setText(status.toUpperCase());
                setGraphic(statusBadge);
            }
        });
    }

    private void refreshStats() {
        List<User> roster = loadRosterUsers();
        long active = roster.stream()
                .filter(u -> "active".equalsIgnoreCase(safe(u.getAccountStatus())))
                .count();
        if (totalAthletesLabel != null) totalAthletesLabel.setText(String.valueOf(roster.size()));
        if (activeAthletesLabel != null) activeAthletesLabel.setText(String.valueOf(active));

        try {
            List<models.FeedbackResponse> responses = new services.FeedbackResponseService().readWithDetails();
            if (sessionsLabel != null) sessionsLabel.setText(String.valueOf(responses.size()));

            long positive = responses.stream().filter(f -> "positive".equalsIgnoreCase(f.getSentiment())).count();
            int pct = responses.isEmpty() ? 0 : (int) (positive * 100 / responses.size());
            if (positiveSentimentLabel != null) positiveSentimentLabel.setText(pct + "%");

            buildRatingChart(responses);
            buildSentimentChart(responses);
            buildDashFeedbackTable(responses);
        } catch (SQLException e) {
            if (sessionsLabel != null) sessionsLabel.setText("—");
        }
        buildDashWorkouts();
    }

    private void buildDashWorkouts() {
        if (dashWorkoutsContainer == null) return;
        dashWorkoutsContainer.getChildren().clear();
        try {
            List<Workout> workouts = workoutService.read();

            // ── Stats row ──────────────────────────────────────────────────
            long total = workouts.size();
            long beginner = workouts.stream().filter(w -> "beginner".equalsIgnoreCase(safe(w.getNiveau()))).count();
            long intermediate = workouts.stream().filter(w -> "intermediate".equalsIgnoreCase(safe(w.getNiveau()))).count();
            long advanced = workouts.stream().filter(w -> "advanced".equalsIgnoreCase(safe(w.getNiveau()))).count();

            HBox statsRow = new HBox(12);
            statsRow.getChildren().addAll(
                buildMiniStat("TOTAL WORKOUTS", String.valueOf(total), "#9f7cff"),
                buildMiniStat("BEGINNER", String.valueOf(beginner), "#22C55E"),
                buildMiniStat("INTERMEDIATE", String.valueOf(intermediate), "#F59E0B"),
                buildMiniStat("ADVANCED", String.valueOf(advanced), "#EF4444")
            );
            dashWorkoutsContainer.getChildren().add(statsRow);

            // ── Bar chart by level ─────────────────────────────────────────
            if (total > 0) {
                VBox chartBox = new VBox(8);
                chartBox.setStyle("-fx-background-color:#111827;-fx-border-color:#1F2937;" +
                        "-fx-border-radius:12;-fx-background-radius:12;-fx-padding:14;");
                Label chartTitle = new Label("WORKOUTS BY LEVEL");
                chartTitle.setStyle("-fx-text-fill:#6B7280;-fx-font-size:11px;-fx-font-weight:800;");
                chartBox.getChildren().add(chartTitle);

                String[][] levels = {
                    {"Beginner", String.valueOf(beginner), "#22C55E"},
                    {"Intermediate", String.valueOf(intermediate), "#F59E0B"},
                    {"Advanced", String.valueOf(advanced), "#EF4444"}
                };
                for (String[] lv : levels) {
                    long count = Long.parseLong(lv[1]);
                    double pct = total > 0 ? (double) count / total : 0;
                    HBox barRow = new HBox(10);
                    barRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    Label lbl = new Label(lv[0]);
                    lbl.setStyle("-fx-text-fill:#D1D5DB;-fx-font-size:12px;-fx-min-width:90;");
                    javafx.scene.layout.StackPane barBg = new javafx.scene.layout.StackPane();
                    barBg.setStyle("-fx-background-color:#1F2937;-fx-background-radius:4;");
                    barBg.setPrefHeight(10);
                    javafx.scene.layout.HBox.setHgrow(barBg, javafx.scene.layout.Priority.ALWAYS);
                    javafx.scene.layout.Region fill = new javafx.scene.layout.Region();
                    fill.setStyle("-fx-background-color:" + lv[2] + ";-fx-background-radius:4;");
                    fill.setPrefHeight(10);
                    fill.setPrefWidth(pct * 300);
                    barBg.getChildren().add(fill);
                    javafx.scene.layout.StackPane.setAlignment(fill, javafx.geometry.Pos.CENTER_LEFT);
                    Label countLbl = new Label(lv[1]);
                    countLbl.setStyle("-fx-text-fill:" + lv[2] + ";-fx-font-size:12px;-fx-font-weight:700;-fx-min-width:20;");
                    barRow.getChildren().addAll(lbl, barBg, countLbl);
                    chartBox.getChildren().add(barRow);
                }
                dashWorkoutsContainer.getChildren().add(chartBox);
            }

            // ── Workout list ───────────────────────────────────────────────
            if (workouts.isEmpty()) {
                Label empty = new Label("No workouts yet. Go to Workout Catalog to add some.");
                empty.setStyle("-fx-text-fill:#6B7280;-fx-font-size:13px;");
                dashWorkoutsContainer.getChildren().add(empty);
                return;
            }
            for (Workout w : workouts) {
                HBox card = new HBox(14);
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color:#111827;-fx-border-color:#1F2937;" +
                        "-fx-border-radius:12;-fx-background-radius:12;-fx-padding:14;-fx-cursor:hand;");

                Label icon = new Label("🏋");
                icon.setStyle("-fx-font-size:22px;");

                VBox info = new VBox(3);
                javafx.scene.layout.HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
                Label name = new Label(safe(w.getNom()));
                name.setStyle("-fx-text-fill:white;-fx-font-size:14px;-fx-font-weight:700;");
                Label meta = new Label(
                        (w.getNiveau() != null ? w.getNiveau() : "—") +
                        (w.getDuree() != null ? "  •  " + w.getDuree() + " min" : ""));
                meta.setStyle("-fx-text-fill:#6B7280;-fx-font-size:12px;");
                info.getChildren().addAll(name, meta);

                String lvl = safe(w.getNiveau()).toLowerCase();
                String badgeColor = lvl.contains("adv") ? "#EF4444" : lvl.contains("inter") ? "#F59E0B" : "#22C55E";
                Label statusBadge = new Label(w.getNiveau() != null ? w.getNiveau() : "Beginner");
                statusBadge.setStyle("-fx-background-color:rgba(34,197,94,0.15);" +
                        "-fx-border-color:" + badgeColor + ";-fx-border-radius:999;" +
                        "-fx-background-radius:999;-fx-text-fill:" + badgeColor + ";" +
                        "-fx-font-size:11px;-fx-font-weight:700;-fx-padding:3 10;");

                card.getChildren().addAll(icon, info, statusBadge);
                dashWorkoutsContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            Label err = new Label("Could not load workouts.");
            err.setStyle("-fx-text-fill:#F87171;-fx-font-size:12px;");
            dashWorkoutsContainer.getChildren().add(err);
        }
    }

    private VBox buildMiniStat(String title, String value, String color) {
        VBox box = new VBox(4);
        box.setStyle("-fx-background-color:#111827;-fx-border-color:#1F2937;" +
                "-fx-border-radius:12;-fx-background-radius:12;-fx-padding:12;");
        javafx.scene.layout.HBox.setHgrow(box, javafx.scene.layout.Priority.ALWAYS);
        Label t = new Label(title);
        t.setStyle("-fx-text-fill:#6B7280;-fx-font-size:10px;-fx-font-weight:800;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill:" + color + ";-fx-font-size:28px;-fx-font-weight:900;");
        box.getChildren().addAll(t, v);
        return box;
    }

    private void buildRatingChart(List<models.FeedbackResponse> responses) {
        if (ratingChartContainer == null) return;
        ratingChartContainer.getChildren().clear();

        java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
        counts.put("Excellent", 0L); counts.put("Good", 0L);
        counts.put("Average", 0L); counts.put("Poor", 0L);
        for (models.FeedbackResponse f : responses) {
            String r = f.getRating();
            if (r == null) continue;
            for (String key : counts.keySet()) {
                if (r.toLowerCase().contains(key.toLowerCase())) {
                    counts.put(key, counts.get(key) + 1);
                }
            }
        }

        javafx.scene.chart.PieChart chart = new javafx.scene.chart.PieChart();
        chart.setLegendVisible(true);
        chart.setPrefHeight(180);
        String[] colors = {"#22c55e", "#60a5fa", "#eab308", "#ef4444"};
        int i = 0;
        for (var entry : counts.entrySet()) {
            javafx.scene.chart.PieChart.Data slice = new javafx.scene.chart.PieChart.Data(entry.getKey(), entry.getValue());
            chart.getData().add(slice);
        }
        chart.setStyle("-fx-background-color: transparent;");
        ratingChartContainer.getChildren().add(chart);
    }

    private void buildSentimentChart(List<models.FeedbackResponse> responses) {
        if (sentimentChartContainer == null) return;
        sentimentChartContainer.getChildren().clear();

        long pos = responses.stream().filter(f -> "positive".equalsIgnoreCase(f.getSentiment())).count();
        long neg = responses.stream().filter(f -> "negative".equalsIgnoreCase(f.getSentiment())).count();
        long neu = responses.stream().filter(f -> "neutral".equalsIgnoreCase(f.getSentiment())).count();

        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        javafx.scene.chart.BarChart<String, Number> chart = new javafx.scene.chart.BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setPrefHeight(180);
        chart.setStyle("-fx-background-color: transparent;");

        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Nombre de réponses");
        series.getData().add(new javafx.scene.chart.XYChart.Data<>("Positive", pos));
        series.getData().add(new javafx.scene.chart.XYChart.Data<>("Neutral", neu));
        series.getData().add(new javafx.scene.chart.XYChart.Data<>("Negative", neg));
        chart.getData().add(series);
        sentimentChartContainer.getChildren().add(chart);
    }

    private void buildDashFeedbackTable(List<models.FeedbackResponse> responses) {
        if (dashFeedbackTable == null) return;
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter
                .ofPattern("MMM dd, yyyy HH:mm").withZone(java.time.ZoneId.systemDefault());

        dashUserCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserName()));
        dashWorkoutCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getWorkout() != null ? d.getValue().getWorkout().getNom() : ""));
        dashRatingCol.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().getRating())));
        dashSentimentCol.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().getSentiment())));
        dashCommentCol.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().getComment())));
        dashDateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? fmt.format(d.getValue().getCreatedAt()) : ""));

        dashFeedbackTable.setItems(javafx.collections.FXCollections.observableArrayList(responses));
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private void refreshAthletesTable() {
        try {
            athletesTable.setItems(FXCollections.observableArrayList(loadRosterUsers()));
        } catch (Exception e) {
            athletesTable.setItems(FXCollections.observableArrayList());
        }
    }

    /**
     * Same roster as admin “User management” (no admin account, no ROLE_ADMIN), without edit/delete actions.
     */
    private List<User> loadRosterUsers() {
        try {
            return userService.read().stream()
                    .filter(u -> safe(u.getRolesJson()).contains("ROLE_USER"))
                    .filter(u -> !safe(u.getRolesJson()).contains("ROLE_ADMIN"))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            return List.of();
        }
    }

    private static String extractRole(String rolesJson) {
        String s = safe(rolesJson);
        if (s.contains("ROLE_COACH")) {
            return "ROLE_COACH";
        }
        if (s.contains("ROLE_USER")) {
            return "ROLE_USER";
        }
        if (s.contains("ROLE_ADMIN")) {
            return "ROLE_ADMIN";
        }
        return s.isBlank() ? "ROLE_USER" : s;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private void switchScene(String fxmlPath, String cssPath) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Scene scene = coachDashboardPane.getScene();
            scene.setRoot(newRoot);
            scene.getStylesheets().setAll(Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm());
        } catch (IOException e) {
            throw new RuntimeException("Failed to switch scene", e);
        }
    }
}
