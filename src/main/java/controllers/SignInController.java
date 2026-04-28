package controllers;

import app.AppSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import models.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import services.LoginSecurityService;
import services.ProfilePhysiqueService;
import services.UserService;
import utils.RememberMeManager;
import utils.SessionManager;
import utils.WebAssets;

import java.sql.SQLException;
import java.util.Objects;

/**
 * Flux de connexion :
 *   1. L'utilisateur saisit email + mot de passe
 *   2. onSignInWithPassword() valide les champs
 *   3. Cherche l'utilisateur en base par email (UserService.findByEmail)
 *   4. Verifie le mot de passe avec BCrypt (passwordEncoder.matches)
 *   5. Met l'utilisateur en session (AppSession + SessionManager)
 *   6. Redirige selon le role :
 *      - ROLE_ADMIN  → AdminDashboardView.fxml
 *      - ROLE_COACH  → CoachDashboardView.fxml
 *      - ROLE_USER   → DashboardView.fxml (ou HeightView si pas de profil)
 *
 * Navigation possible depuis cette vue :
 *   - Retour Home : onGoHome() → HomeView.fxml
 *   - Pas de compte : onSignUp() → SignUpView.fxml
 */
public class SignInController {

    /**
     * Email de l'administrateur principal.
     * Si cet email se connecte, il est redirige vers AdminDashboardView
     * meme si son roles JSON ne contient pas explicitement ROLE_ADMIN.
     */
    private static final String ADMIN_EMAIL = "sarahguesmi223@gmail.com";

    /**
     * Noeud racine de SignInView.fxml (fx:id="root").
     * Utilise dans switchScene() pour recuperer la Scene via root.getScene().
     */
    @FXML private StackPane root;

    // Boutons de navigation
    @FXML private Button homeButton;              // retour a la page d'accueil
    @FXML private Button signInWithPasswordButton; // soumettre le formulaire
    @FXML private Button signUpButton;            // aller vers l'inscription

    // Champs du formulaire de connexion
    @FXML private TextField emailField;       // champ email
    @FXML private PasswordField passwordField; // champ mot de passe (masque)

    /** Image decorative chargee depuis WebAssets */
    @FXML private ImageView heroImageView;

    // Labels d'erreur affiches sous les champs en cas d'echec
    @FXML private Label emailError;
    @FXML private Label passwordError;

    @FXML private CheckBox rememberMeCheckBox;

    /** Service d'acces a la base de donnees pour les utilisateurs */
    private UserService userService;

    /** Service pour verifier si l'utilisateur a deja un profil physique */
    private ProfilePhysiqueService profilePhysiqueService;

    /**
     * Encodeur BCrypt pour verifier le mot de passe.
     * passwordEncoder.matches("motDePasse", hashStocke) → true/false
     * Ne jamais comparer les mots de passe en clair !
     */
    private BCryptPasswordEncoder passwordEncoder;

    /** Stocke l'erreur d'initialisation des services pour l'afficher a l'utilisateur */
    private Throwable servicesInitError;

    /**
     * initialize — appelee automatiquement par JavaFX apres le chargement du FXML.
     *
     * Initialise les services (connexion DB, encodeur BCrypt) et configure
     * les listeners pour effacer les erreurs quand l'utilisateur tape.
     */
    @FXML
    private void initialize() {
        try {
            passwordEncoder = new BCryptPasswordEncoder();
            userService = new UserService();             // connexion DB pour les users
            profilePhysiqueService = new ProfilePhysiqueService(); // connexion DB pour les profils
        } catch (Throwable t) {
            servicesInitError = t;
            t.printStackTrace();
        }
        try {
            WebAssets.loadPublicAsset(heroImageView, WebAssets.HERO_SPORT_IMAGE);
        } catch (Exception e) {
            System.out.println("Impossible de charger l'image hero : " + e.getMessage());
        }
        // Efface l'erreur d'un champ des que l'utilisateur commence a taper
        if (emailField != null) emailField.textProperty().addListener((o, a, b) -> clearError(emailField, emailError));
        if (passwordField != null) passwordField.textProperty().addListener((o, a, b) -> clearError(passwordField, passwordError));

        // Pre-fill email if "remember me" was checked on last login
        String savedEmail = RememberMeManager.load();
        if (!savedEmail.isEmpty() && emailField != null) {
            emailField.setText(savedEmail);
            if (rememberMeCheckBox != null) rememberMeCheckBox.setSelected(true);
        }
    }

    /** onGoHome — clic sur le bouton retour → navigue vers la page d'accueil */
    @FXML
    private void onGoHome() {
        switchScene("/fxml/HomeView.fxml", "/css/home.css");
    }

    /**
     * onSignInWithPassword — appele quand l'utilisateur clique sur "SIGN IN".
     *
     * Etapes :
     *   1. Verifie que les services sont disponibles
     *   2. Valide les champs (vide, format email)
     *   3. Cherche l'utilisateur par email en base
     *   4. Verifie le mot de passe avec BCrypt
     *   5. Met en session et redirige selon le role
     */
    @FXML
    private void onSignInWithPassword() {
        clearAllErrors();

        // Verifie que les services sont disponibles (connexion DB OK)
        if (servicesInitError != null) {
            showError(emailField, emailError, "Service error: " + servicesInitError.getMessage());
            return;
        }
        if (userService == null || userService.cnx == null) {
            showError(emailField, emailError, "No database connection. Check MySQL.");
            return;
        }

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        // Validation des champs
        boolean valid = true;
        if (email.isEmpty()) {
            showError(emailField, emailError, "Email is required.");
            valid = false;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showError(emailField, emailError, "Enter a valid email address.");
            valid = false;
        }
        if (password.isEmpty()) {
            showError(passwordField, passwordError, "Password is required.");
            valid = false;
        }
        if (!valid) return;

        try {
            // Cherche l'utilisateur par email : SELECT * FROM app_user WHERE email_email = ?
            User user = userService.findByEmail(email);

            // Verifie le mot de passe avec BCrypt
            // passwordEncoder.matches("motDePasse", "$2a$10$hash...") → true ou false
            // On ne compare JAMAIS les mots de passe en clair
            if (user == null || user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
                showError(emailField, emailError, "Invalid email or password.");
                showError(passwordField, passwordError, "Invalid email or password.");
                return;
            }

            // Connexion reussie : met l'utilisateur en session memoire
            SessionManager.setCurrentUser(user);
            AppSession.setCurrentUser(user);

            // Handle remember me
            if (rememberMeCheckBox != null && rememberMeCheckBox.isSelected()) {
                RememberMeManager.save(user.getEmail());
            } else {
                RememberMeManager.clear();
            }

            // If 2FA is enabled, route to verify screen before dashboard
            if (user.getGoogleAuthenticatorSecret() != null
                    && !user.getGoogleAuthenticatorSecret().isBlank()) {
                // Clear session — will be set again after 2FA verification
                SessionManager.setCurrentUser(null);
                AppSession.setCurrentUser(null);
                openTwoFactorVerify(user);
                return;
            }

            // Record login attempt + check for unusual location (background)
            runLoginSecurityCheck(user);

            // Redirige vers le bon dashboard selon le role
            String rolesJson = user.getRolesJson() == null ? "" : user.getRolesJson();

            // Admin : email specifique OU role ROLE_ADMIN dans le JSON
            if (ADMIN_EMAIL.equalsIgnoreCase(user.getEmail()) || rolesJson.contains("ROLE_ADMIN")) {
                switchScene("/fxml/AdminDashboardView.fxml", "/css/admin.css");
                return;
            }

            // Coach : role ROLE_COACH dans le JSON
            if (rolesJson.contains("ROLE_COACH")) {
                switchScene("/fxml/CoachDashboardView.fxml", "/css/admin.css");
                return;
            }

            // Utilisateur normal : verifie s'il a deja complete l'onboarding
            // Si profil physique existe → dashboard, sinon → onboarding (saisie taille)
            boolean hasProfile = !profilePhysiqueService.findByUserId(user.getId()).isEmpty();
            if (hasProfile) {
                switchScene("/fxml/DashboardView.fxml", "/css/dashboard.css");
            } else {
                AppSession.resetOnboarding(); // reinitialise les donnees d'onboarding
                switchScene("/fxml/HeightView.fxml", "/css/onboarding.css");
            }

        } catch (SQLException e) {
            showError(emailField, emailError, "Database error: " + e.getMessage());
        } catch (Exception e) {
            showError(emailField, emailError, "Unexpected error: " + e.getMessage());
        }
    }

    /** onSignUp — clic sur "Don't have an account?" → navigue vers l'inscription */
    @FXML
    private void onSignUp() {
        switchScene("/fxml/SignUpView.fxml", "/css/signup.css");
    }

    /** onForgotPassword — clic sur "Forgot password?" → navigue vers la page de réinitialisation */
    @FXML
    private void onForgotPassword() {
        switchScene("/fxml/ForgotPasswordView.fxml", "/css/signin.css");
    }

    /** onFaceIdLogin — opens the QR code overlay for phone-based login */
    @FXML
    private void onFaceIdLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/fxml/FaceIdQrView.fxml")));
            Parent newRoot = loader.load();
            controllers.FaceIdQrController ctrl = loader.getController();
            Scene scene = root.getScene();
            scene.setRoot(newRoot);
            scene.getStylesheets().setAll(
                    Objects.requireNonNull(getClass().getResource("/css/signin.css")).toExternalForm());
            ctrl.startSession();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs the login security check in a background thread.
     * If an unusual location is detected, clears the session and redirects to sign-in.
     */
    private void runLoginSecurityCheck(User user) {
        new Thread(() -> {
            try {
                LoginSecurityService security = new LoginSecurityService();
                boolean unusual = security.recordAndCheck(user, LoginSecurityService.STATUS_SUCCESS);
                if (unusual) {
                    Platform.runLater(() -> {
                        SessionManager.setCurrentUser(null);
                        AppSession.setCurrentUser(null);
                        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
                    });
                }
            } catch (Exception e) {
                System.err.println("Security check failed: " + e.getMessage());
            }
        }, "login-security-check").start();
    }

    /** Routes to the 2FA verify screen, passing the authenticated user */
    private void openTwoFactorVerify(User user) {        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/fxml/TwoFactorVerifyView.fxml")));
            Parent newRoot = loader.load();
            TwoFactorVerifyController ctrl = loader.getController();
            ctrl.setPendingUser(user);
            Scene scene = root.getScene();
            scene.setRoot(newRoot);
            scene.getStylesheets().setAll(
                    Objects.requireNonNull(getClass().getResource("/css/signin.css")).toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Methodes utilitaires ──────────────────────────────────────────────────

    /**
     * showError — affiche un message d'erreur sous un champ.
     * Ajoute la classe CSS "input-field-error" (bordure rouge).
     */
    private void showError(javafx.scene.Node field, Label label, String msg) {
        if (field != null) field.getStyleClass().add("input-field-error");
        if (label != null) {
            label.setText(msg);
            label.setManaged(true);
            label.setVisible(true);
        }
    }

    /**
     * clearError — efface le message d'erreur d'un champ.
     * Retire la classe CSS "input-field-error" et cache le label.
     */
    private void clearError(javafx.scene.Node field, Label label) {
        if (field != null) field.getStyleClass().remove("input-field-error");
        if (label != null) {
            label.setText("");
            label.setManaged(false);
            label.setVisible(false);
        }
    }

    /** clearAllErrors — efface les erreurs de tous les champs */
    private void clearAllErrors() {
        clearError(emailField, emailError);
        clearError(passwordField, passwordError);
    }

    /**
     * switchScene — remplace le contenu de la Scene pour naviguer vers une autre vue.
     *
     * Principe : la fenetre (Stage) reste la meme, on change juste son contenu.
     *   1. FXMLLoader.load() : charge le nouveau FXML, cree les composants,
     *      instancie le nouveau controleur, injecte ses @FXML, appelle initialize()
     *   2. root.getScene() : recupere la Scene actuelle (root est encore dedans)
     *   3. scene.setRoot(newRoot) : detache l'ancien root, attache le nouveau
     *      → la fenetre affiche maintenant le nouveau contenu
     *   4. scene.getStylesheets().setAll() : remplace le CSS
     */
    private void switchScene(String fxmlPath, String cssPath) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Scene scene = root.getScene();   // recupere la Scene via le root actuel
            scene.setRoot(newRoot);          // remplace le contenu affiche
            scene.getStylesheets().setAll(Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm());
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            cause.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText(null);
            alert.setContentText("Could not load: " + fxmlPath + "\n" + cause.getMessage());
            alert.showAndWait();
        }
    }
}
