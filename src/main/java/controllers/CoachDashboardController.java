package controllers;

import app.AppSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import models.Exercise;
import models.User;
import models.Workout;
import services.ExerciseService;
import services.UserService;
import services.WorkoutService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private VBox nutritionPane;
    @FXML
    private VBox profilePane;

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
    @FXML private BarChart<String, Number> exerciseBarChart;
    @FXML private PieChart goalPieChart;
    @FXML private PieChart levelPieChart;

    @FXML private TableView<User> athletesTable;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, String> statusCol;
    @FXML private TableColumn<User, String> dateCol;

    @FXML private ProfileFragmentController coachProfileController;

    private final UserService userService = new UserService();
    private final WorkoutService workoutService = new WorkoutService();
    private final ExerciseService exerciseService = new ExerciseService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML
    private void initialize() {
        setupAthletesTable();
        if (coachProfileController != null) {
            coachProfileController.setAfterSaveCallback(this::refreshNavbar);
        }
        refreshNavbar();
        onShowDashboard();
    }

    @FXML
    private void onShowDashboard() {
        hideAllContent();
        coachDashboardPane.setManaged(true);
        coachDashboardPane.setVisible(true);
        setNavbarText("Coach dashboard", "Train and support your athletes");
        setActiveSidebar(coachHomeBtn);
        refreshStats(); // always refresh on navigate
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
        setNavbarText("Feedback management", "Athlete feedback");
        setActiveSidebar(feedbackBtn);
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
        AppSession.setCurrentUser(null);
        AppSession.resetOnboarding();
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    private void hideAllContent() {
        for (VBox p : List.of(coachDashboardPane, athletesPane, chatroomPane, workoutCatalogPane,
                feedbackPane, mentalWellnessPane, nutritionPane, profilePane)) {
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
        for (Button b : List.of(coachHomeBtn, athletesBtn, chatroomBtn, workoutCatalogBtn,
                feedbackBtn, mentalWellnessBtn, nutritionBtn, profileBtn)) {
            if (b != null) {
                b.getStyleClass().remove("side-link-active");
            }
        }
        if (selected != null && !selected.getStyleClass().contains("side-link-active")) {
            selected.getStyleClass().add("side-link-active");
        }
    }

    private void setupAthletesTable() {
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
            List<Workout> workouts = workoutService.readWithExercises();

            // AVG duration
            double avg = workouts.stream()
                    .filter(w -> w.getDuree() != null)
                    .mapToInt(Workout::getDuree)
                    .average().orElse(0);
            if (sessionsLabel != null) sessionsLabel.setText(String.format("%.1f min", avg));

            // ── Bar chart: Most Used Exercises ──
            Map<String, Integer> exerciseUsage = new LinkedHashMap<>();
            for (Workout w : workouts) {
                for (Exercise e : w.getExercises()) {
                    String name = e.getNom() != null ? e.getNom() : "Unknown";
                    exerciseUsage.merge(name, 1, Integer::sum);
                }
            }
            // Sort by usage desc, take top 8
            List<Map.Entry<String, Integer>> top = exerciseUsage.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(8).toList();

            if (exerciseBarChart != null) {
                exerciseBarChart.getData().clear();
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Uses");
                for (Map.Entry<String, Integer> entry : top) {
                    series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                }
                exerciseBarChart.getData().add(series);
                // Apply color per bar after rendering
                javafx.application.Platform.runLater(() -> {
                    String[] colors = {"#22C55E","#3B82F6","#EC4899","#F59E0B","#8B5CF6","#EF4444","#06B6D4","#10B981"};
                    var bars = exerciseBarChart.lookupAll(".chart-bar");
                    int i = 0;
                    for (javafx.scene.Node bar : bars) {
                        bar.setStyle("-fx-bar-fill:" + colors[i % colors.length] + ";");
                        i++;
                    }
                });
            }

            // ── Pie chart: Workouts by Goal ──
            Map<String, Integer> goalDist = new LinkedHashMap<>();
            for (Workout w : workouts) {
                if (w.getObjectifs().isEmpty()) {
                    goalDist.merge("Unknown", 1, Integer::sum);
                } else {
                    for (var obj : w.getObjectifs()) {
                        String name = obj.getName() != null ? obj.getName() : "Unknown";
                        goalDist.merge(name, 1, Integer::sum);
                    }
                }
            }
            if (goalPieChart != null) {
                goalPieChart.getData().clear();
                goalDist.forEach((k, v) ->
                        goalPieChart.getData().add(new PieChart.Data(k + " (" + v + ")", v)));
                goalPieChart.setLabelsVisible(false);
                // Add tooltips
                javafx.application.Platform.runLater(() -> {
                    for (PieChart.Data d : goalPieChart.getData()) {
                        javafx.scene.control.Tooltip.install(d.getNode(),
                                new javafx.scene.control.Tooltip(d.getName() + ": " + (int)d.getPieValue()));
                    }
                });
            }

            // ── Pie chart: Workouts by Level ──
            Map<String, Integer> levelDist = new LinkedHashMap<>();
            for (Workout w : workouts) {
                String level = w.getNiveau() != null ? capitalize(w.getNiveau()) : "Unknown";
                levelDist.merge(level, 1, Integer::sum);
            }
            if (levelPieChart != null) {
                levelPieChart.getData().clear();
                levelDist.forEach((k, v) ->
                        levelPieChart.getData().add(new PieChart.Data(k + " (" + v + ")", v)));
                levelPieChart.setLabelsVisible(false);
                javafx.application.Platform.runLater(() -> {
                    for (PieChart.Data d : levelPieChart.getData()) {
                        javafx.scene.control.Tooltip.install(d.getNode(),
                                new javafx.scene.control.Tooltip(d.getName() + ": " + (int)d.getPieValue()));
                    }
                });
            }

        } catch (SQLException e) {
            System.err.println("CoachDashboard refreshStats error: " + e.getMessage());
        }
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
