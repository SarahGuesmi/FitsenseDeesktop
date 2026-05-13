package controllers;

import app.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.User;
import models.Workout;
import utils.FeedbackLauncher;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.control.Alert;
import services.NotificationService;
import java.util.List;
import java.io.IOException;
import java.util.Objects;
public class DashboardController {

    @FXML
    private StackPane root;
    @FXML
    private Label welcomeLabel;
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
    private Stage notificationStage;
    @FXML
    private Label navbarAvatar;
    @FXML 
    private ImageView navbarLogoImage;
    @FXML private VBox dashHomePane;
    @FXML private VBox dashProfilePane;
    @FXML private VBox dashWorkoutsPane;
    @FXML private VBox dashActivityPane;
    @FXML private VBox dashMentalHealthPane;
    @FXML private VBox dashChatroomPane;
    @FXML private VBox workoutsFragmentPane;
    @FXML private VBox libraryPane;
    @FXML private VBox dashNutritionPane;
    @FXML private Button dashHomeBtn;
    @FXML private Button dashProfileBtn;
    @FXML private Button dashWorkoutsBtn;
    @FXML private Button dashMentalHealthBtn;
    @FXML private Button dashChatroomBtn;
    @FXML private Button dashNutritionBtn;
    @FXML private Label notificationBadge;
    @FXML private ProfileFragmentController dashProfileController;
    @FXML private ActivityLogController dashActivityController;
    @FXML private MentalHealthUserFragmentController dashMentalHealthController;
    @FXML private ChatroomController dashChatroomController;
    @FXML private NutritionFragmentController dashNutritionStatsController;

    // Singleton reference so child controllers can call back
    private static DashboardController instance;
    public static DashboardController getInstance() { return instance; }
    @FXML
    private void initialize() {
        instance = this;
        
        // Load navbar logo
        loadNavbarLogo();
        
        if (navbarBellBtn != null) {
            navbarBellBtn.setText("\uD83D\uDD14");
        }
        if (dashProfileController != null) {
            dashProfileController.setAfterSaveCallback(this::refreshNavbar);
        }
        refreshNavbar();
        setDashboardNavbarTitles();
        showDashboardHome();
        refreshNotificationBadge();
    }
    
    private void loadNavbarLogo() {
        if (navbarLogoImage != null) {
            try {
                java.io.File logoFile = new java.io.File("C:/xampp2/htdocs/pidevassets/sport-hero.png");
                if (logoFile.exists()) {
                    javafx.scene.image.Image logo = new javafx.scene.image.Image(logoFile.toURI().toString(), true);
                    navbarLogoImage.setImage(logo);
                }
            } catch (Exception e) {
                System.err.println("Could not load navbar logo: " + e.getMessage());
            }
        }
    }
    @FXML
    private void onShowNutrition() {
        showOnly(dashNutritionPane); // ou le bon pane
        setDashNavActive(dashNutritionBtn);
    }

    @FXML
    private void onShowMentalHealth() {
        if (navbarPageTitle != null) navbarPageTitle.setText("Mental Health");
        if (navbarPageSubtitle != null) navbarPageSubtitle.setText("Mental wellness and assessments");
        showOnly(dashMentalHealthPane);
        setDashNavActive(dashMentalHealthBtn);
    }

    @FXML
    private void onShowChatroom() {
        if (navbarPageTitle != null) navbarPageTitle.setText("Chatroom");
        if (navbarPageSubtitle != null) navbarPageSubtitle.setText("Connect with your coach and community");
        showOnly(dashChatroomPane);
        setDashNavActive(dashChatroomBtn);
        // Refresh chatroom when showing
        if (dashChatroomController != null) {
            dashChatroomController.refresh();
        }
    }
    @FXML
    private void onShowNotifications() {
        try {
            if (notificationStage != null && notificationStage.isShowing()) {
                notificationStage.close();
                notificationStage = null;
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NotificationPopup.fxml"));
            Parent root = loader.load();

            NotificationPopupController controller = loader.getController();
            String userId = AppSession.getCurrentUser().getId().toString();
            controller.loadNotifications(userId, this);

            notificationStage = new Stage();
            notificationStage.setScene(new Scene(root));
            notificationStage.initStyle(StageStyle.TRANSPARENT);
            notificationStage.show();

            notificationStage.setOnHidden(e -> notificationStage = null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshNotificationBadge() {
        if (notificationBadge == null) return;
        int unreadCount = 1;
        notificationBadge.setText(String.valueOf(unreadCount));
        notificationBadge.setVisible(unreadCount > 0);
        notificationBadge.setManaged(unreadCount > 0);
    }

    private void showOnly(VBox paneToShow) {
        VBox[] allPanes = {dashHomePane, dashProfilePane, dashWorkoutsPane,
                dashActivityPane, dashMentalHealthPane, dashChatroomPane, dashNutritionPane};
        for (VBox p : allPanes) {
            if (p != null) {
                p.setVisible(false);
                p.setManaged(false);
            }
        }
        if (paneToShow != null) {
            paneToShow.setVisible(true);
            paneToShow.setManaged(true);
        }
    }

    private void setDashboardNavbarTitles() {
        if (navbarPageTitle != null) {
            navbarPageTitle.setText("Dashboard");
        }
        if (navbarPageSubtitle != null) {
            navbarPageSubtitle.setText("Your fitness overview");
        }
    }

    private void setProfileNavbarTitles() {
        if (navbarPageTitle != null) {
            navbarPageTitle.setText("Profile");
        }
        if (navbarPageSubtitle != null) {
            navbarPageSubtitle.setText("Manage your personal information");
        }
    }

    private void refreshNavbar() {
        User session = AppSession.getCurrentUser();
        if (session == null) {
            return;
        }
        String initials = userInitials(session);
        String roleLabel = roleDisplayName(session.getRolesJson());
        if (navbarUserName != null) {
            navbarUserName.setText(initials);
        }
        if (navbarAvatar != null) {
            navbarAvatar.setText(initials);
        }
        if (navbarUserRole != null) {
            navbarUserRole.setText(roleLabel);
        }
        String first = session.getFirstname() == null ? "" : session.getFirstname().trim();
        if (welcomeLabel != null) {
            String greet = first.isEmpty() ? safe(session.getEmail()) : first;
            welcomeLabel.setText("Welcome back, " + greet + "!");
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
        return "U";
    }

    private static String roleDisplayName(String rolesJson) {
        String s = safe(rolesJson);
        if (s.contains("ROLE_ADMIN")) {
            return "Administrator";
        }
        if (s.contains("ROLE_COACH")) {
            return "Coach";
        }
        return "User";
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    @FXML
    public void onShowActivityLog() {
        if (navbarPageTitle != null) navbarPageTitle.setText("Activity Log");
        if (navbarPageSubtitle != null) navbarPageSubtitle.setText("Your session activity today");
        showOnly(dashActivityPane);
        if (dashActivityController != null) dashActivityController.refresh();
        setDashNavActive(null);
    }

    @FXML
    private void onShowWorkouts() {
        if (navbarPageTitle != null) navbarPageTitle.setText("My Workouts");
        if (navbarPageSubtitle != null) navbarPageSubtitle.setText("Personalized sessions matching your goals");
        showOnly(dashWorkoutsPane);
        showWorkoutsList();
        setDashNavActive(dashWorkoutsBtn);
    }

    public void showWorkoutsList() {
        if (workoutsFragmentPane != null) { workoutsFragmentPane.setVisible(true); workoutsFragmentPane.setManaged(true); }
        if (libraryPane != null) { libraryPane.setVisible(false); libraryPane.setManaged(false); libraryPane.getChildren().clear(); }
    }

    public void showLibraryView(javafx.scene.Node view) {
        if (navbarPageTitle != null) navbarPageTitle.setText("Exercise Library");
        if (navbarPageSubtitle != null) navbarPageSubtitle.setText("Browse and discover exercises");
        showOnly(dashWorkoutsPane);
        if (workoutsFragmentPane != null) { workoutsFragmentPane.setVisible(false); workoutsFragmentPane.setManaged(false); }
        if (libraryPane != null) {
            libraryPane.getChildren().setAll(view);
            javafx.scene.layout.VBox.setVgrow(view, javafx.scene.layout.Priority.ALWAYS);
            libraryPane.setVisible(true);
            libraryPane.setManaged(true);
        }
        setDashNavActive(dashWorkoutsBtn);
    }

    @FXML
    private void onShowDashboard() {
        setDashboardNavbarTitles();
        showDashboardHome();
    }
    @FXML
    private void onShowProfile() {
        setProfileNavbarTitles();
        showOnly(dashProfilePane);
        setDashNavActive(dashProfileBtn);

        if (dashProfileController != null) {
            dashProfileController.reloadFromSession();
        }
    }

    private void showDashboardHome() {
        if (dashHomePane != null) { dashHomePane.setManaged(true); dashHomePane.setVisible(true); }
        if (dashProfilePane != null) { dashProfilePane.setManaged(false); dashProfilePane.setVisible(false); }
        if (dashWorkoutsPane != null) { dashWorkoutsPane.setManaged(false); dashWorkoutsPane.setVisible(false); }
        if (dashActivityPane != null) { dashActivityPane.setManaged(false); dashActivityPane.setVisible(false); }
        if (dashMentalHealthPane != null) { dashMentalHealthPane.setManaged(false); dashMentalHealthPane.setVisible(false); }
        if (dashChatroomPane != null) { dashChatroomPane.setManaged(false); dashChatroomPane.setVisible(false); }
        if (dashNutritionPane != null) { dashNutritionPane.setManaged(false); dashNutritionPane.setVisible(false); }
        setDashNavActive(dashHomeBtn);

        if (dashNutritionStatsController != null) {
            dashNutritionStatsController.reloadNutrition();
        }
    }

    private void setDashNavActive(Button selected) {
        for (Button b : new Button[]{dashHomeBtn, dashProfileBtn, dashWorkoutsBtn, dashMentalHealthBtn, dashChatroomBtn, dashNutritionBtn}) {
            if (b != null) {
                b.getStyleClass().setAll("dash-side-link");
                if (b == selected) b.getStyleClass().add("dash-side-link-active");
            }
        }
    }
    @FXML
    private void onTestFeedback() {
        Workout testWorkout = new Workout();
        testWorkout.setNom("Test Workout");
        testWorkout.setNiveau("Medium");
        testWorkout.setDuree(45);
        testWorkout.setDescription("Test");
        testWorkout.setStatus("active");
        FeedbackLauncher.show(testWorkout, () -> System.out.println("Feedback done"));
    }

    @FXML
    private void onLogout() {
        AppSession.setCurrentUser(null);
        AppSession.resetOnboarding();
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
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
}