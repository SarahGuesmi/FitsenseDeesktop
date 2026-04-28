package controllers;

import app.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.User;
import utils.SidebarAvatarLoader;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

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
    @FXML
    private Label navbarAvatar;
    @FXML
    private VBox dashHomePane;
    @FXML
    private VBox dashProfilePane;
    @FXML
    private VBox dashMentalHealthPane;
    @FXML
    private VBox dashWorkoutsPane;
    @FXML
    private VBox dashChatroomPane;
    @FXML
    private Button dashHomeBtn;
    @FXML
    private Button dashProfileBtn;
    @FXML
    private Button dashWorkoutsBtn;
    @FXML
    private Button dashMentalHealthBtn;
    @FXML private Button dashChatroomBtn;
    @FXML private Button dashSecurityBtn;
    @FXML private ScrollPane dashSecurityPane;
    @FXML private ImageView sidebarAvatarView;
    @FXML private Label     sidebarAvatarInitials;
    @FXML
    private ProfileFragmentController dashProfileController;

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
        SidebarAvatarLoader.load(AppSession.getCurrentUser(), sidebarAvatarView, sidebarAvatarInitials);
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

    private void setMentalHealthNavbarTitles() {
        if (navbarPageTitle != null) {
            navbarPageTitle.setText("Dashboard");
        }
        if (navbarPageSubtitle != null) {
            navbarPageSubtitle.setText("Welcome back to FitSense.");
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
    private void onShowChatroom() {
        if (navbarPageTitle != null) navbarPageTitle.setText("Chatroom");
        if (navbarPageSubtitle != null) navbarPageSubtitle.setText("Message your coaches and teammates");
        hideAllDashPanes();
        if (dashChatroomPane != null) { dashChatroomPane.setManaged(true); dashChatroomPane.setVisible(true); }
        setDashNavActive(dashChatroomBtn);
    }

    private void hideAllDashPanes() {
        for (VBox p : new VBox[]{dashHomePane, dashProfilePane, dashMentalHealthPane, dashWorkoutsPane, dashChatroomPane}) {
            if (p != null) { p.setManaged(false); p.setVisible(false); }
        }
        if (dashSecurityPane != null) { dashSecurityPane.setManaged(false); dashSecurityPane.setVisible(false); }
    }

    @FXML
    private void onShowSecurity() {
        if (navbarPageTitle != null) navbarPageTitle.setText("Security");
        if (navbarPageSubtitle != null) navbarPageSubtitle.setText("Manage two-factor authentication");
        hideAllDashPanes();
        if (dashSecurityPane != null) { dashSecurityPane.setManaged(true); dashSecurityPane.setVisible(true); }
        setDashNavActive(dashSecurityBtn);
    }

    @FXML
    private void onShowWorkouts() {
        if (navbarPageTitle != null) navbarPageTitle.setText("My Workouts");
        if (navbarPageSubtitle != null) navbarPageSubtitle.setText("Personalized sessions matching your goals");
        hideAllDashPanes();
        if (dashWorkoutsPane != null) { dashWorkoutsPane.setManaged(true); dashWorkoutsPane.setVisible(true); }
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
        hideAllDashPanes();
        if (dashProfilePane != null) { dashProfilePane.setManaged(true); dashProfilePane.setVisible(true); }
        setDashNavActive(dashProfileBtn);
        if (dashProfileController != null) dashProfileController.reloadFromSession();
    }

    @FXML
    private void onShowMentalHealth() {
        setMentalHealthNavbarTitles();
        hideAllDashPanes();
        if (dashMentalHealthPane != null) { dashMentalHealthPane.setManaged(true); dashMentalHealthPane.setVisible(true); }
        setDashNavActive(dashMentalHealthBtn);
    }

    private void showDashboardHome() {
        hideAllDashPanes();
        if (dashHomePane != null) { dashHomePane.setManaged(true); dashHomePane.setVisible(true); }
        setDashNavActive(dashHomeBtn);
    }

    private void setDashNavActive(Button selected) {
        Stream.of(dashHomeBtn, dashProfileBtn, dashWorkoutsBtn, dashMentalHealthBtn, dashChatroomBtn, dashSecurityBtn)
                .filter(Objects::nonNull)
                .forEach(b -> {
                    b.getStyleClass().setAll("dash-side-link");
                    if (b == selected) b.getStyleClass().add("dash-side-link-active");
                });
    }

    @FXML
    private StackPane logoutOverlay;

    @FXML
    private void onLogout() {
        if (logoutOverlay != null) {
            logoutOverlay.setManaged(true);
            logoutOverlay.setVisible(true);
        }
    }

    @FXML
    private void onConfirmLogout() {
        AppSession.setCurrentUser(null);
        AppSession.resetOnboarding();
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    @FXML
    private void onCancelLogout() {
        if (logoutOverlay != null) {
            logoutOverlay.setManaged(false);
            logoutOverlay.setVisible(false);
        }
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
