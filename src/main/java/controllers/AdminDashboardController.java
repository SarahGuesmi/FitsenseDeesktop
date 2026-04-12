package controllers;

import app.AppSession;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.User;
import services.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class AdminDashboardController {
    private static final String ADMIN_EMAIL = "sarahguesmi223@gmail.com";

    @FXML
    private VBox adminDashboardPane;
    @FXML
    private VBox userManagementPane;
    @FXML
    private Button adminDashboardBtn;
    @FXML
    private Button userManagementBtn;
    @FXML
    private Button profileBtn;
    @FXML
    private VBox profilePane;
    @FXML
    private ProfileFragmentController adminProfileController;
    @FXML
    private Button navbarBellBtn;
    @FXML
    private Label navbarUserName;
    @FXML
    private Label navbarUserRole;
    @FXML
    private Label navbarAvatar;
    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label activeUsersLabel;
    @FXML
    private Label twoFaLabel;
    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, String> nameCol;
    @FXML
    private TableColumn<User, String> roleCol;
    @FXML
    private TableColumn<User, String> statusCol;
    @FXML
    private TableColumn<User, String> dateCol;
    @FXML
    private TableColumn<User, Void> actionCol;
    @FXML
    private StackPane modalOverlay;
    @FXML
    private VBox addCoachModal;
    @FXML
    private VBox editUserModal;
    @FXML
    private VBox deleteConfirmModal;
    @FXML
    private Label deleteConfirmLabel;
    @FXML
    private TextField coachFirstNameField;
    @FXML
    private TextField coachLastNameField;
    @FXML
    private TextField coachEmailField;
    @FXML
    private PasswordField coachPasswordField;
    @FXML
    private ComboBox<String> coachStatusCombo;
    @FXML
    private TextField editFirstNameField;
    @FXML
    private TextField editLastNameField;
    @FXML
    private TextField editEmailField;
    @FXML
    private PasswordField editPasswordField;
    @FXML
    private ComboBox<String> editRoleCombo;
    @FXML
    private ComboBox<String> editStatusCombo;
    @FXML
    private TextField userSearchField;
    @FXML
    private ComboBox<String> userStatusFilterCombo;
    @FXML
    private ComboBox<String> userRoleFilterCombo;

    private final UserService userService = new UserService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private List<User> allManagedUsers = List.of();
    private User editingUser;
    private User pendingDeleteUser;

    @FXML
    private void initialize() {
        setupTable();
        setupUserFilters();
        initializeModalControls();
        if (adminProfileController != null) {
            adminProfileController.setAfterSaveCallback(this::refreshNavbar);
        }
        refreshNavbar();
        showAdminDashboard();
    }

    private void refreshNavbar() {
        if (navbarBellBtn != null) {
            navbarBellBtn.setText("\uD83D\uDD14");
        }
        User session = AppSession.getCurrentUser();
        if (session == null) {
            if (navbarUserName != null) {
                navbarUserName.setText("SG");
            }
            if (navbarUserRole != null) {
                navbarUserRole.setText("Administrator");
            }
            if (navbarAvatar != null) {
                navbarAvatar.setText("SG");
            }
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
        return "AD";
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

    @FXML
    private void onShowAdminDashboard() {
        showAdminDashboard();
    }

    @FXML
    private void onShowUserManagement() {
        showUserManagement();
    }

    @FXML
    private void onShowProfile() {
        showProfile();
    }

    @FXML
    private void onClearUserFilters() {
        if (userSearchField != null) {
            userSearchField.clear();
        }
        if (userStatusFilterCombo != null) {
            userStatusFilterCombo.setValue("All");
        }
        if (userRoleFilterCombo != null) {
            userRoleFilterCombo.setValue("All");
        }
        applyUserFilters();
    }

    @FXML
    private void onLogout() {
        AppSession.setCurrentUser(null);
        AppSession.resetOnboarding();
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    @FXML
    private void onAddCoach() {
        clearAddCoachForm();
        showModal(addCoachModal);
    }

    @FXML
    private void onActivateCoach() {
        clearValidationStyles();
        String firstName = safe(coachFirstNameField.getText()).trim();
        String lastName = safe(coachLastNameField.getText()).trim();
        String email = safe(coachEmailField.getText()).trim();
        String password = safe(coachPasswordField.getText()).trim();
        String status = coachStatusCombo.getValue();

        boolean valid = true;
        if (firstName.isBlank()) {
            markInvalid(coachFirstNameField);
            valid = false;
        }
        if (lastName.isBlank()) {
            markInvalid(coachLastNameField);
            valid = false;
        }
        if (!isValidEmail(email)) {
            markInvalid(coachEmailField);
            valid = false;
        }
        if (password.length() < 6) {
            markInvalid(coachPasswordField);
            valid = false;
        }
        if (status == null || status.isBlank()) {
            markInvalid(coachStatusCombo);
            valid = false;
        }
        if (!valid) {
            return;
        }

        try {
            if (userService.findByEmail(email) != null) {
                markInvalid(coachEmailField);
                return;
            }
            User user = new User();
            user.setFirstname(firstName);
            user.setLastname(lastName);
            user.setEmail(email);
            user.setPassword(password);
            user.setRolesJson("[\"ROLE_COACH\"]");
            user.setAccountStatus(status.toLowerCase());
            user.setUsername(uniqueUsernameFromEmail(email));
            userService.createPrepared(user);
            closeModal();
            showUserManagement();
        } catch (SQLException ignored) {
        }
    }

    @FXML
    private void onConfirmEdit() {
        if (editingUser == null) {
            closeModal();
            return;
        }
        clearValidationStyles();
        String firstName = safe(editFirstNameField.getText()).trim();
        String lastName = safe(editLastNameField.getText()).trim();
        String email = safe(editEmailField.getText()).trim();
        String password = safe(editPasswordField.getText()).trim();
        String role = editRoleCombo.getValue();
        String status = editStatusCombo.getValue();

        boolean valid = true;
        if (firstName.isBlank()) {
            markInvalid(editFirstNameField);
            valid = false;
        }
        if (lastName.isBlank()) {
            markInvalid(editLastNameField);
            valid = false;
        }
        if (!isValidEmail(email)) {
            markInvalid(editEmailField);
            valid = false;
        }
        if (!password.isBlank() && password.length() < 6) {
            markInvalid(editPasswordField);
            valid = false;
        }
        if (role == null || role.isBlank()) {
            markInvalid(editRoleCombo);
            valid = false;
        }
        if (status == null || status.isBlank()) {
            markInvalid(editStatusCombo);
            valid = false;
        }
        if (!valid) {
            return;
        }

        try {
            User existing = userService.findByEmail(email);
            if (existing != null && !existing.getId().equals(editingUser.getId())) {
                markInvalid(editEmailField);
                return;
            }
            editingUser.setFirstname(firstName);
            editingUser.setLastname(lastName);
            editingUser.setEmail(email);
            if (!password.isBlank()) {
                editingUser.setPassword(password);
            }
            editingUser.setRolesJson("[\"" + role + "\"]");
            editingUser.setAccountStatus(status.toLowerCase());
            userService.update(editingUser);
            closeModal();
            showUserManagement();
        } catch (SQLException ignored) {
        }
    }

    @FXML
    private void onConfirmDelete() {
        if (pendingDeleteUser == null) {
            closeModal();
            return;
        }
        try {
            userService.delete(pendingDeleteUser);
            closeModal();
            showUserManagement();
        } catch (SQLException ignored) {
        }
    }

    @FXML
    private void onCloseModal() {
        closeModal();
    }

    private void setupTable() {
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

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
                fullNameLabel.setText(name == null || name.isBlank() ? "Unknown User" : name);
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
                roleBadge.setText(role.replace("ROLE_", "ROLE_"));
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

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button activateBtn = createIconButton("✓", "icon-activate");
            private final Button deactivateBtn = createIconButton("○", "icon-deactivate");
            private final Button editBtn = createIconButton("✎", "icon-edit");
            private final Button deleteBtn = createIconButton("🗑", "icon-delete");
            private final HBox actions = new HBox(8, activateBtn, deactivateBtn, editBtn, deleteBtn);

            {
                actions.setAlignment(Pos.CENTER_LEFT);
                actions.getStyleClass().add("actions-box");
                activateBtn.visibleProperty().bind(Bindings.selectBoolean(tableRowProperty(), "hover"));
                activateBtn.managedProperty().bind(activateBtn.visibleProperty());
                deactivateBtn.visibleProperty().bind(Bindings.selectBoolean(tableRowProperty(), "hover"));
                deactivateBtn.managedProperty().bind(deactivateBtn.visibleProperty());

                activateBtn.setOnAction(event -> updateStatusForRow(getTableRow().getItem(), "active"));
                deactivateBtn.setOnAction(event -> updateStatusForRow(getTableRow().getItem(), "inactive"));
                editBtn.setOnAction(event -> openEditModal(getTableRow().getItem()));
                deleteBtn.setOnAction(event -> openDeleteConfirm(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(actions);
            }
        });
    }

    private void setupUserFilters() {
        if (userStatusFilterCombo != null) {
            userStatusFilterCombo.setItems(FXCollections.observableArrayList("All", "Active", "Inactive"));
            userStatusFilterCombo.setValue("All");
            userStatusFilterCombo.valueProperty().addListener((o, a, b) -> applyUserFilters());
        }
        if (userRoleFilterCombo != null) {
            userRoleFilterCombo.setItems(FXCollections.observableArrayList("All", "User", "Coach"));
            userRoleFilterCombo.setValue("All");
            userRoleFilterCombo.valueProperty().addListener((o, a, b) -> applyUserFilters());
        }
        if (userSearchField != null) {
            userSearchField.textProperty().addListener((o, a, b) -> applyUserFilters());
        }
    }

    private void applyUserFilters() {
        if (usersTable == null || allManagedUsers == null) {
            return;
        }
        String q = safe(userSearchField != null ? userSearchField.getText() : "").trim().toLowerCase(Locale.ROOT);
        String st = userStatusFilterCombo != null ? userStatusFilterCombo.getValue() : "All";
        String roleUi = userRoleFilterCombo != null ? userRoleFilterCombo.getValue() : "All";

        List<User> filtered = allManagedUsers.stream()
                .filter(u -> q.isEmpty() || matchesUserSearch(u, q))
                .filter(u -> {
                    if (st == null || "All".equals(st)) {
                        return true;
                    }
                    return st.equalsIgnoreCase(safe(u.getAccountStatus()));
                })
                .filter(u -> {
                    if (roleUi == null || "All".equals(roleUi)) {
                        return true;
                    }
                    String rj = safe(u.getRolesJson());
                    if ("Coach".equals(roleUi)) {
                        return rj.contains("ROLE_COACH");
                    }
                    return rj.contains("ROLE_USER") && !rj.contains("ROLE_COACH");
                })
                .collect(Collectors.toList());
        usersTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private static boolean matchesUserSearch(User u, String qLower) {
        String name = (safe(u.getFirstname()) + " " + safe(u.getLastname())).trim().toLowerCase(Locale.ROOT);
        String email = safe(u.getEmail()).toLowerCase(Locale.ROOT);
        return name.contains(qLower) || email.contains(qLower);
    }

    private static Button createIconButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("table-icon-btn", styleClass);
        btn.setFocusTraversable(false);
        return btn;
    }

    private void updateStatusForRow(User user, String nextStatus) {
        if (user == null) {
            return;
        }
        user.setAccountStatus(nextStatus);
        try {
            userService.update(user);
            refreshData();
        } catch (SQLException ignored) {
        }
    }

    private void openDeleteConfirm(User user) {
        if (user == null) return;
        pendingDeleteUser = user;
        deleteConfirmLabel.setText("Delete " + safe(user.getFirstname()) + " " + safe(user.getLastname()) + " ?");
        showModal(deleteConfirmModal);
    }

    private void openEditModal(User user) {
        if (user == null) return;
        editingUser = user;
        editFirstNameField.setText(safe(user.getFirstname()));
        editLastNameField.setText(safe(user.getLastname()));
        editEmailField.setText(safe(user.getEmail()));
        editPasswordField.clear();
        editRoleCombo.setValue(extractRole(user.getRolesJson()));
        editStatusCombo.setValue(normalizeStatusForUi(user.getAccountStatus()));
        clearValidationStyles();
        showModal(editUserModal);
    }

    private void showAdminDashboard() {
        adminDashboardPane.setManaged(true);
        adminDashboardPane.setVisible(true);
        userManagementPane.setManaged(false);
        userManagementPane.setVisible(false);
        if (profilePane != null) {
            profilePane.setManaged(false);
            profilePane.setVisible(false);
        }
        setActiveSidebar(adminDashboardBtn);
        refreshData();
    }

    private void showUserManagement() {
        adminDashboardPane.setManaged(false);
        adminDashboardPane.setVisible(false);
        userManagementPane.setManaged(true);
        userManagementPane.setVisible(true);
        if (profilePane != null) {
            profilePane.setManaged(false);
            profilePane.setVisible(false);
        }
        setActiveSidebar(userManagementBtn);
        refreshData();
    }

    private void showProfile() {
        adminDashboardPane.setManaged(false);
        adminDashboardPane.setVisible(false);
        userManagementPane.setManaged(false);
        userManagementPane.setVisible(false);
        if (profilePane != null) {
            profilePane.setManaged(true);
            profilePane.setVisible(true);
        }
        setActiveSidebar(profileBtn);
        if (adminProfileController != null) {
            adminProfileController.reloadFromSession();
        }
    }

    private void setActiveSidebar(Button selectedButton) {
        if (adminDashboardBtn != null) {
            adminDashboardBtn.getStyleClass().remove("side-link-active");
        }
        if (userManagementBtn != null) {
            userManagementBtn.getStyleClass().remove("side-link-active");
        }
        if (profileBtn != null) {
            profileBtn.getStyleClass().remove("side-link-active");
        }
        if (selectedButton != null && !selectedButton.getStyleClass().contains("side-link-active")) {
            selectedButton.getStyleClass().add("side-link-active");
        }
    }

    private void initializeModalControls() {
        coachStatusCombo.setItems(FXCollections.observableArrayList("active", "inactive"));
        editStatusCombo.setItems(FXCollections.observableArrayList("active", "inactive"));
        editRoleCombo.setItems(FXCollections.observableArrayList("ROLE_USER", "ROLE_COACH"));
        coachStatusCombo.setValue("active");
    }

    private void showModal(VBox modalToShow) {
        addCoachModal.setManaged(false);
        addCoachModal.setVisible(false);
        editUserModal.setManaged(false);
        editUserModal.setVisible(false);
        deleteConfirmModal.setManaged(false);
        deleteConfirmModal.setVisible(false);
        modalOverlay.setManaged(true);
        modalOverlay.setVisible(true);
        modalToShow.setManaged(true);
        modalToShow.setVisible(true);
    }

    private void closeModal() {
        modalOverlay.setManaged(false);
        modalOverlay.setVisible(false);
        addCoachModal.setManaged(false);
        addCoachModal.setVisible(false);
        editUserModal.setManaged(false);
        editUserModal.setVisible(false);
        deleteConfirmModal.setManaged(false);
        deleteConfirmModal.setVisible(false);
        editingUser = null;
        pendingDeleteUser = null;
    }

    private void clearAddCoachForm() {
        coachFirstNameField.clear();
        coachLastNameField.clear();
        coachEmailField.clear();
        coachPasswordField.clear();
        coachStatusCombo.setValue("active");
        clearValidationStyles();
    }

    private void clearValidationStyles() {
        List<String> classNames = List.of("modal-field-error");
        Arrays.asList(coachFirstNameField, coachLastNameField, coachEmailField, coachPasswordField,
                        editFirstNameField, editLastNameField, editEmailField, editPasswordField,
                        coachStatusCombo, editRoleCombo, editStatusCombo)
                .forEach(node -> node.getStyleClass().removeAll(classNames));
    }

    private void markInvalid(javafx.scene.Node node) {
        if (!node.getStyleClass().contains("modal-field-error")) {
            node.getStyleClass().add("modal-field-error");
        }
    }

    private void refreshData() {
        try {
            List<User> users = userService.read().stream()
                    .filter(u -> !ADMIN_EMAIL.equalsIgnoreCase(safe(u.getEmail())))
                    .filter(u -> !safe(u.getRolesJson()).contains("ROLE_ADMIN"))
                    .collect(Collectors.toList());
            allManagedUsers = new ArrayList<>(users);
            applyUserFilters();

            long activeCount = users.stream()
                    .filter(u -> "active".equalsIgnoreCase(safe(u.getAccountStatus())))
                    .count();
            long twoFaCount = users.stream()
                    .filter(u -> !safe(u.getGoogleAuthenticatorSecret()).isBlank())
                    .count();
            if (totalUsersLabel != null) {
                totalUsersLabel.setText(String.valueOf(users.size()));
            }
            if (activeUsersLabel != null) {
                activeUsersLabel.setText(String.valueOf(activeCount));
            }
            if (twoFaLabel != null) {
                twoFaLabel.setText(String.valueOf(twoFaCount));
            }
        } catch (SQLException e) {
            allManagedUsers = List.of();
            usersTable.setItems(FXCollections.observableArrayList());
        }
    }

    private static String extractRole(String rolesJson) {
        String s = safe(rolesJson);
        if (s.contains("ROLE_COACH")) return "ROLE_COACH";
        if (s.contains("ROLE_USER")) return "ROLE_USER";
        if (s.contains("ROLE_ADMIN")) return "ROLE_ADMIN";
        return s.isBlank() ? "ROLE_USER" : s;
    }

    private static String normalizeStatusForUi(String status) {
        return "inactive".equalsIgnoreCase(status) ? "inactive" : "active";
    }

    private String uniqueUsernameFromEmail(String email) {
        int at = email.indexOf('@');
        String base = (at > 0 ? email.substring(0, at) : safe(email)).replaceAll("[^a-zA-Z0-9._-]", "");
        if (base.isBlank()) {
            base = "coach";
        }
        if (base.length() > 48) {
            base = base.substring(0, 48);
        }
        String candidate = base;
        int suffix = 0;
        try {
            while (userService.findByUsername(candidate) != null) {
                suffix++;
                String tail = "_" + suffix;
                candidate = base.substring(0, Math.max(1, Math.min(base.length(), 48 - tail.length()))) + tail;
            }
        } catch (SQLException ignored) {
            return base + "_" + System.currentTimeMillis();
        }
        return candidate;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private void switchScene(String fxmlPath, String cssPath) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Scene scene = usersTable.getScene();
            scene.setRoot(newRoot);
            scene.getStylesheets().setAll(Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm());
        } catch (IOException e) {
            throw new RuntimeException("Failed to switch scene", e);
        }
    }
}
