package controllers;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
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
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.RecetteNutritionnelle;
import models.User;
import services.RecetteFavoriService;
import services.UserService;
import java.util.Map;
import javafx.scene.image.ImageView;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
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
    private Canvas objectiveCanvas;

    @FXML
    private VBox objectiveLegendBox;

    @FXML
    private Label totalClientsLabel;
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
    private PieChart objectiveChart;
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
        setNavbarText("Coach dashboard", "Train and support your athletes");
        setActiveSidebar(coachHomeBtn);
        refreshStats();
        loadTopRecipes();
        loadObjectiveChart();

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
    private HBox topRecipesBox;
    private void loadObjectiveChart() {
        if (objectiveCanvas == null || objectiveLegendBox == null || totalClientsLabel == null) return;

        Map<String, Integer> data = favoriService.getObjectiveDistribution();

        int total = data.values().stream().mapToInt(Integer::intValue).sum();
        totalClientsLabel.setText(String.valueOf(total));

        objectiveLegendBox.getChildren().clear();

        Color[] colors = {
                Color.web("#35c46b"),
                Color.web("#2f73df"),
                Color.web("#f5a21a"),
                Color.web("#8b5cf6")
        };

        GraphicsContext gc = objectiveCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, 130, 130);

        double startAngle = 90;
        int index = 0;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            int value = entry.getValue();
            double percent = total == 0 ? 0 : (value * 100.0 / total);
            double angle = total == 0 ? 0 : (value * 360.0 / total);

            gc.setFill(colors[index]);
            gc.fillArc(5, 5, 120, 120, startAngle, -angle, javafx.scene.shape.ArcType.ROUND);

            startAngle -= angle;
            index++;
        }

        gc.setFill(Color.web("#0f172a"));
        gc.fillOval(40, 40, 50, 50);

        index = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            int value = entry.getValue();
            int percent = total == 0 ? 0 : (int) Math.round(value * 100.0 / total);

            HBox line = new HBox(8);
            line.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            line.setPrefWidth(160); // 👈 AUGMENTE largeur
            Region dot = new Region();
            dot.setPrefSize(10, 10);
            dot.setStyle("-fx-background-color: " + toRgb(colors[index]) + "; -fx-background-radius: 50;");

            Label name = new Label(entry.getKey());
            name.setStyle("-fx-text-fill: white; -fx-font-size: 13;");
            name.setWrapText(true); // 👈 IMPORTANT
            name.setMaxWidth(Double.MAX_VALUE); // 👈 IMPORTANT
            HBox.setHgrow(name, javafx.scene.layout.Priority.ALWAYS); // 👈 IMPORTANT            name.setStyle("-fx-text-fill: white; -fx-font-size: 13;");

            Label pct = new Label(percent + "%");
            pct.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13;");
            HBox.setHgrow(name, javafx.scene.layout.Priority.ALWAYS);

            line.getChildren().addAll(dot, name, pct);
            objectiveLegendBox.getChildren().add(line);

            index++;
        }
    }

    @FXML
    private NutritionController dashNutritionController;
    private String toRgb(Color color) {
        return String.format(
                "rgb(%d,%d,%d)",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }    private final RecetteFavoriService favoriService = new RecetteFavoriService();
    private void loadTopRecipes() {
        if (topRecipesBox == null) return;

        topRecipesBox.getChildren().clear();

        Map<RecetteNutritionnelle, Integer> topRecipes = favoriService.getTop5FavoriteRecipes();

        for (Map.Entry<RecetteNutritionnelle, Integer> entry : topRecipes.entrySet()) {
            RecetteNutritionnelle r = entry.getKey();
            int likes = entry.getValue();

            VBox card = new VBox(6);
            card.getStyleClass().add("top-recipe-card");
            card.setPrefWidth(105);

            ImageView img = new ImageView();

            String imageUrl = (r.getImage() == null || r.getImage().isBlank())
                    ? "https://via.placeholder.com/150"
                    : r.getImage();

            img.setImage(new Image(imageUrl, true));
            img.setFitWidth(105);
            img.setFitHeight(75);
            img.setPreserveRatio(false);

            Label title = new Label(r.getTitle());
            title.getStyleClass().add("top-recipe-title");
            title.setWrapText(true);

            HBox info = new HBox(8);

            Label likesLabel = new Label("❤ " + likes);
            likesLabel.getStyleClass().add("top-recipe-info");

            Label kcalLabel = new Label(r.getKcal() + " kcal");
            kcalLabel.getStyleClass().add("top-recipe-info");

            info.getChildren().addAll(likesLabel, kcalLabel);

            card.getChildren().addAll(img, title, info);
            topRecipesBox.getChildren().add(card);
        }
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
