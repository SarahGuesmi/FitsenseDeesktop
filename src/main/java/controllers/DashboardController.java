package controllers;

import app.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.User;
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
    @FXML private VBox dashNutritionPane;
    @FXML private Button dashNutritionBtn;
    @FXML
    private Label navbarPageSubtitle;
    @FXML private Label notificationBadge;
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
    private VBox dashHomePane;
    @FXML
    private VBox dashProfilePane;
    @FXML
    private Button dashHomeBtn;
    @FXML
    private Button dashProfileBtn;
    @FXML
    private ProfileFragmentController dashProfileController;
    @FXML
    private NutritionFragmentController dashNutritionStatsController;
    @FXML
    private void initialize() {
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
    @FXML
    private void onShowNutrition() {
        showOnly(dashNutritionPane); // ou le bon pane
        setDashNavActive(dashNutritionBtn);
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

    public void refreshNotificationBadge(){
        int unreadCount = 1; // après on le lit depuis la DB

        notificationBadge.setText(String.valueOf(unreadCount));
        notificationBadge.setVisible(unreadCount > 0);
        notificationBadge.setManaged(unreadCount > 0);
    }

    private void showOnly(VBox paneToShow) {
        dashHomePane.setVisible(false);
        dashHomePane.setManaged(false);
        dashNutritionPane.setVisible(false);
        dashNutritionPane.setManaged(false);
        dashProfilePane.setVisible(false);
        dashProfilePane.setManaged(false);



        paneToShow.setVisible(true);
        paneToShow.setManaged(true);
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
        showOnly(dashHomePane);
        setDashNavActive(dashHomeBtn);

        if (dashNutritionStatsController != null) {
            dashNutritionStatsController.reloadNutrition();
        }
    }

    private void setDashNavActive(Button selected) {
        if (dashHomeBtn != null) {
            dashHomeBtn.getStyleClass().setAll("dash-side-link");
            if (selected == dashHomeBtn) {
                dashHomeBtn.getStyleClass().add("dash-side-link-active");
            }
        }

        if (dashProfileBtn != null) {
            dashProfileBtn.getStyleClass().setAll("dash-side-link");
            if (selected == dashProfileBtn) {
                dashProfileBtn.getStyleClass().add("dash-side-link-active");
            }
        }

        if (dashNutritionBtn != null) {
            dashNutritionBtn.getStyleClass().setAll("dash-side-link");
            if (selected == dashNutritionBtn) {
                dashNutritionBtn.getStyleClass().add("dash-side-link-active");
            }
        }
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
