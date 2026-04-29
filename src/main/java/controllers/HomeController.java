package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import utils.WebAssets;

import java.util.Objects;

/**
 * HomeController — controleur de la page d'accueil (HomeView.fxml).
 *
 * C'est la premiere vue affichee au demarrage de l'application.
 * Elle propose trois actions de navigation :
 *   - SIGN UP   → SignUpView.fxml
 *   - SIGN IN   → SignInView.fxml
 *   - START YOUR JOURNEY → SignInView.fxml (meme destination)
 *
 * Lien avec le FXML :
 *   HomeView.fxml declare fx:controller="controllers.HomeController"
 *   Chaque fx:id="xxx" est injecte dans @FXML xxx
 *   Chaque onAction="#methode" appelle la methode correspondante ici
 *
 * Navigation :
 *   La methode switchScene() remplace le noeud racine de la Scene.
 *   La fenetre (Stage) ne change pas, seul son contenu change.
 *   Avant de naviguer, la video de fond est liberee (disposeBackgroundVideo)
 *   pour eviter les fuites memoire.
 */
public class HomeController {

    /**
     * Noeud racine de HomeView.fxml (fx:id="root").
     * Utilise dans switchScene() pour recuperer la Scene via root.getScene().
     * C'est le point d'acces a la Scene depuis le controleur.
     */
    @FXML
    private StackPane root;

    // Boutons de navigation — lies aux fx:id correspondants dans HomeView.fxml
    @FXML
    private Button signUpButton;

    @FXML
    private Button signInButton;

    @FXML
    private Button startJourneyButton;

    /** Logo de l'application — fx:id="logoImageView" dans le FXML */
    @FXML
    private ImageView logoImageView;

    /**
     * Composant d'affichage video — fx:id="backgroundMediaView" dans le FXML.
     * Affiche la video de fond en plein ecran.
     */
    @FXML
    private MediaView backgroundMediaView;

    /** Lecteur media qui controle la video (lecture, arret, boucle) */
    private MediaPlayer backgroundMediaPlayer;

    /**
     * initialize — appelee automatiquement par JavaFX apres le chargement du FXML.
     *
     * C'est ici qu'on initialise les composants qui ont besoin de configuration
     * supplementaire apres leur creation par FXMLLoader.
     * Equivalent du constructeur pour les controleurs JavaFX.
     */
    @FXML
    private void initialize() {
        setupBackgroundVideo(); // demarre la video de fond
    }

    /**
     * setupBackgroundVideo — configure et lance la video de fond en boucle.
     *
     * La video est redimensionnee pour couvrir toute la fenetre (bind sur width/height).
     * setCycleCount(INDEFINITE) : boucle infinie.
     * setMute(true) : video muette.
     * setAutoPlay(true) : demarre automatiquement.
     */
    private void setupBackgroundVideo() {
        if (backgroundMediaView == null || root == null) {
            return;
        }
        // La video s'adapte a la taille de la fenetre en temps reel
        backgroundMediaView.fitWidthProperty().bind(root.widthProperty());
        backgroundMediaView.fitHeightProperty().bind(root.heightProperty());
        backgroundMediaView.setPreserveRatio(false);

        String videoUrl = WebAssets.assetUrl(WebAssets.HOME_GYM_VIDEO); // file:// path to pidevassets/gym-video.mp4
        try {
            Media media = new Media(videoUrl);
            backgroundMediaPlayer = new MediaPlayer(media);
            backgroundMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // boucle infinie
            backgroundMediaPlayer.setMute(true);                          // sans son
            backgroundMediaPlayer.setAutoPlay(true);                      // demarre auto
            backgroundMediaPlayer.setOnError(() ->
                    System.err.println("Home background video error: " + backgroundMediaPlayer.getError()));
            backgroundMediaView.setMediaPlayer(backgroundMediaPlayer);
        } catch (RuntimeException e) {
            // Si la video ne charge pas, l'app continue sans video (pas bloquant)
            System.err.println("Could not start background video: " + videoUrl + " — " + e.getMessage());
        }
    }

    // ── Methodes de navigation ────────────────────────────────────────────────
    // Chaque methode est liee a un bouton via onAction="#methode" dans le FXML.

    /**
     * onSignUp — appele quand l'utilisateur clique sur le bouton "SIGN UP".
     * Navigue vers la page d'inscription.
     */
    @FXML
    private void onSignUp() {
        switchScene("/fxml/SignUpView.fxml", "/css/signup.css");
    }

    /**
     * onSignIn — appele quand l'utilisateur clique sur le bouton "SIGN IN".
     * Navigue vers la page de connexion.
     */
    @FXML
    private void onSignIn() {
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    /**
     * onStartJourney — appele quand l'utilisateur clique sur "START YOUR JOURNEY".
     * Redirige vers la connexion (meme destination que SIGN IN).
     */
    @FXML
    private void onStartJourney() {
        switchScene("/fxml/SignInView.fxml", "/css/signin.css");
    }

    /**
     * switchScene — remplace le contenu de la Scene pour naviguer vers une autre vue.
     *
     * Principe :
     *   La fenetre (Stage) reste la meme. On change juste ce qu'elle affiche.
     *   La Scene a toujours un seul noeud racine (root). On le remplace.
     *
     * Etapes :
     *   1. disposeBackgroundVideo() : libere la video pour eviter les fuites memoire
     *   2. FXMLLoader.load(fxmlPath) : lit le FXML, cree les composants, instancie
     *      le nouveau controleur, injecte ses @FXML, appelle son initialize()
     *   3. root.getScene() : recupere la Scene actuelle (root est encore dedans)
     *   4. scene.setRoot(newRoot) : detache l'ancien root, attache le nouveau
     *      → la fenetre affiche maintenant le nouveau contenu
     *   5. scene.getStylesheets().setAll(...) : remplace le CSS
     *      setAll() vide l'ancien CSS et met le nouveau (evite les conflits de styles)
     */
    private void switchScene(String fxmlPath, String cssPath) {
        disposeBackgroundVideo(); // libere la video avant de quitter cette vue

        try {
            // Charge le nouveau FXML et cree tous ses composants + controleur
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));

            // Recupere la Scene via le noeud racine actuel (root est encore dans la Scene ici)
            Scene scene = root.getScene();

            // Remplace le contenu de la Scene → la fenetre affiche maintenant newRoot
            scene.setRoot(newRoot);

            // Remplace le CSS : setAll() vide l'ancien et met le nouveau
            scene.getStylesheets().setAll(Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm());

        } catch (Exception e) {
            // Affiche une alerte si le FXML ne peut pas etre charge
            e.printStackTrace();
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Could not open screen");
            alert.setHeaderText("Failed to load " + fxmlPath);
            alert.setContentText(root.getClass().getSimpleName() + ": " + root.getMessage());
            alert.getDialogPane().setMinWidth(600);
            alert.showAndWait();
        }
    }

    /**
     * disposeBackgroundVideo — arrete et libere le lecteur video.
     *
     * Appele avant chaque navigation pour eviter que le MediaPlayer
     * continue de tourner en arriere-plan apres avoir quitte la vue Home.
     * Sans ca, on aurait une fuite memoire a chaque navigation.
     */
    private void disposeBackgroundVideo() {
        if (backgroundMediaView != null) {
            backgroundMediaView.setMediaPlayer(null); // deconnecte la vue du lecteur
        }
        if (backgroundMediaPlayer != null) {
            backgroundMediaPlayer.stop();    // arrete la lecture
            backgroundMediaPlayer.dispose(); // libere les ressources memoire
            backgroundMediaPlayer = null;
        }
    }
}
