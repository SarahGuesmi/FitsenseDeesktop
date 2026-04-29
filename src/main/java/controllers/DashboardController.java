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
    private Button dashHomeBtn;
    @FXML
    private Button dashProfileBtn;
    @FXML
    private Button dashWorkoutsBtn;
    @FXML
    private Button dashMentalHealthBtn;
    @FXML
    private ProfileFragmentController dashProfileController;
    @FXML
    private MentalHealthChartController mentalHealthChartController;

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
    private void onShowWorkouts() {
        if (navbarPageTitle != null) navbarPageTitle.setText("My Workouts");
        if (navbarPageSubtitle != null) navbarPageSubtitle.setText("Personalized sessions matching your goals");
        if (dashHomePane != null) {
            dashHomePane.setManaged(false);
            dashHomePane.setVisible(false);
        }
        if (dashProfilePane != null) {
            dashProfilePane.setManaged(false);
            dashProfilePane.setVisible(false);
        }
        if (dashMentalHealthPane != null) {
            dashMentalHealthPane.setManaged(false);
            dashMentalHealthPane.setVisible(false);
        }
        if (dashWorkoutsPane != null) {
            dashWorkoutsPane.setManaged(true);
            dashWorkoutsPane.setVisible(true);
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
        if (dashHomePane != null) {
            dashHomePane.setManaged(false);
            dashHomePane.setVisible(false);
        }
        if (dashMentalHealthPane != null) {
            dashMentalHealthPane.setManaged(false);
            dashMentalHealthPane.setVisible(false);
        }
        if (dashWorkoutsPane != null) {
            dashWorkoutsPane.setManaged(false);
            dashWorkoutsPane.setVisible(false);
        }
        if (dashProfilePane != null) {
            dashProfilePane.setManaged(true);
            dashProfilePane.setVisible(true);
        }
        setDashNavActive(dashProfileBtn);
        if (dashProfileController != null) {
            dashProfileController.reloadFromSession();
        }
    }

    @FXML
    private void onShowMentalHealth() {
        setMentalHealthNavbarTitles();
        if (dashHomePane != null) {
            dashHomePane.setManaged(false);
            dashHomePane.setVisible(false);
        }
        if (dashProfilePane != null) {
            dashProfilePane.setManaged(false);
            dashProfilePane.setVisible(false);
        }
        if (dashWorkoutsPane != null) {
            dashWorkoutsPane.setManaged(false);
            dashWorkoutsPane.setVisible(false);
        }
        if (dashMentalHealthPane != null) {
            dashMentalHealthPane.setManaged(true);
            dashMentalHealthPane.setVisible(true);
        }
        setDashNavActive(dashMentalHealthBtn);
    }

    private void showDashboardHome() {
        if (dashHomePane != null) {
            dashHomePane.setManaged(true);
            dashHomePane.setVisible(true);
        }
        if (dashProfilePane != null) {
            dashProfilePane.setManaged(false);
            dashProfilePane.setVisible(false);
        }
        if (dashMentalHealthPane != null) {
            dashMentalHealthPane.setManaged(false);
            dashMentalHealthPane.setVisible(false);
        }
        if (dashWorkoutsPane != null) {
            dashWorkoutsPane.setManaged(false);
            dashWorkoutsPane.setVisible(false);
        }
        setDashNavActive(dashHomeBtn);
        
        // Refresh mental health chart when returning to dashboard
        if (mentalHealthChartController != null) {
            mentalHealthChartController.refreshChart();
        }
    }

    private void setDashNavActive(Button selected) {
        Stream.of(dashHomeBtn, dashProfileBtn, dashWorkoutsBtn, dashMentalHealthBtn)
                .filter(Objects::nonNull)
                .forEach(b -> {
                    b.getStyleClass().setAll("dash-side-link");
                    if (b == selected) {
                        b.getStyleClass().add("dash-side-link-active");
                    }
                });
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
