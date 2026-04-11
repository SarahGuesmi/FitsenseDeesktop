package controllers;

import app.AppSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import models.User;
import services.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private VBox mentalTestsPane;
    @FXML
    private VBox mentalAssessmentsPane;
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
    private Button mentalTestsBtn;
    @FXML
    private Button mentalAssessmentsBtn;
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

    @FXML
    private Label totalAthletesLabel;
    @FXML
    private Label activeAthletesLabel;
    @FXML
    private Label sessionsLabel;

    @FXML
    private TableView<User> athletesTable;
    @FXML
    private TableColumn<User, String> nameCol;
    @FXML
    private TableColumn<User, String> roleCol;
    @FXML
    private TableColumn<User, String> statusCol;
    @FXML
    private TableColumn<User, String> dateCol;

    @FXML
    private ProfileFragmentController coachProfileController;
    @FXML
    private CoachMentalTestsFragmentController coachMentalTestsController;
    @FXML
    private CoachMentalAssessmentsFragmentController coachMentalAssessmentsController;

    private final UserService userService = new UserService();
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
        setNavbarText("Coach Panel", "Manage your athletes");
        setActiveSidebar(coachHomeBtn, null);
        refreshStats();
    }

    @FXML
    private void onShowAthletes() {
        hideAllContent();
        athletesPane.setManaged(true);
        athletesPane.setVisible(true);
        setNavbarText("Athletes", "Directory of users (read-only)");
        setActiveSidebar(athletesBtn, null);
        refreshAthletesTable();
    }

    @FXML
    private void onShowChatroom() {
        hideAllContent();
        chatroomPane.setManaged(true);
        chatroomPane.setVisible(true);
        setNavbarText("Chatroom", "Team messaging");
        setActiveSidebar(chatroomBtn, null);
    }

    @FXML
    private void onShowWorkoutCatalog() {
        hideAllContent();
        workoutCatalogPane.setManaged(true);
        workoutCatalogPane.setVisible(true);
        setNavbarText("Workout catalog", "Programs and exercises");
        setActiveSidebar(workoutCatalogBtn, null);
    }

    @FXML
    private void onShowFeedback() {
        hideAllContent();
        feedbackPane.setManaged(true);
        feedbackPane.setVisible(true);
        setNavbarText("Feedback management", "Athlete feedback");
        setActiveSidebar(feedbackBtn, null);
    }

    @FXML
    private void onShowMentalTests() {
        hideAllContent();
        mentalTestsPane.setManaged(true);
        mentalTestsPane.setVisible(true);
        setNavbarText("Mental wellness", "Create tests: title + questions (1–5 scoring, total = sum)");
        setActiveSidebar(null, mentalTestsBtn);
        if (coachMentalTestsController != null) {
            coachMentalTestsController.showListView();
        }
    }

    @FXML
    private void onShowMentalAssessments() {
        hideAllContent();
        mentalAssessmentsPane.setManaged(true);
        mentalAssessmentsPane.setVisible(true);
        setNavbarText("Latest assessments", "Member mental check-ins (stress, sleep, mood, motivation)");
        setActiveSidebar(null, mentalAssessmentsBtn);
        if (coachMentalAssessmentsController != null) {
            coachMentalAssessmentsController.refreshOnShow();
        }
    }

    @FXML
    private void onShowNutrition() {
        hideAllContent();
        nutritionPane.setManaged(true);
        nutritionPane.setVisible(true);
        setNavbarText("Nutrition plan", "Meals and macros");
        setActiveSidebar(nutritionBtn, null);
    }

    @FXML
    private void onShowProfile() {
        hideAllContent();
        profilePane.setManaged(true);
        profilePane.setVisible(true);
        setNavbarText("Profile", "Your account and physique");
        setActiveSidebar(profileBtn, null);
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
                feedbackPane, mentalTestsPane, mentalAssessmentsPane, nutritionPane, profilePane)) {
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

    /**
     * Highlights a main nav item, or a mental-wellness sub-item (mutually exclusive).
     */
    private void setActiveSidebar(Button mainNav, Button mentalSubNav) {
        for (Button b : List.of(coachHomeBtn, athletesBtn, chatroomBtn, workoutCatalogBtn,
                feedbackBtn, nutritionBtn, profileBtn)) {
            if (b != null) {
                b.getStyleClass().remove("side-link-active");
            }
        }
        if (mentalTestsBtn != null) {
            mentalTestsBtn.getStyleClass().remove("side-sub-link-active");
        }
        if (mentalAssessmentsBtn != null) {
            mentalAssessmentsBtn.getStyleClass().remove("side-sub-link-active");
        }
        if (mentalSubNav != null) {
            if (!mentalSubNav.getStyleClass().contains("side-sub-link-active")) {
                mentalSubNav.getStyleClass().add("side-sub-link-active");
            }
        } else if (mainNav != null && !mainNav.getStyleClass().contains("side-link-active")) {
            mainNav.getStyleClass().add("side-link-active");
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
        if (totalAthletesLabel != null) {
            totalAthletesLabel.setText(String.valueOf(roster.size()));
        }
        if (activeAthletesLabel != null) {
            activeAthletesLabel.setText(String.valueOf(active));
        }
        if (sessionsLabel != null) {
            sessionsLabel.setText("—");
        }
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
