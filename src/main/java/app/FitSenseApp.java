package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.User;
import services.UserService;
import java.io.IOException;
import java.util.Objects;

/**
 * FitSenseApp — point d'entree de l'application JavaFX.
 *
 * C'est ici que tout commence. Maven appelle main() qui appelle launch()
 * qui appelle start(). JavaFX cree la fenetre (Stage) et affiche la premiere vue.
 *
 * Architecture de la fenetre JavaFX :
 *   Stage (fenetre physique, une seule)
 *     └── Scene (contenu de la fenetre)
 *           └── Root Node (StackPane de HomeView.fxml)
 *                 └── tous les composants (boutons, labels...)
 *
 * Pour naviguer entre les vues, on ne cree pas de nouvelle fenetre.
 * On remplace juste le Root Node de la Scene via scene.setRoot(newRoot).
 * Voir HomeController.switchScene() pour le detail.
 *
 * Lancement : mvn javafx:run
 */
public class FitSenseApp extends Application {

    /**
     * start — methode principale de JavaFX, appelee automatiquement apres launch().
     *
     * Etapes :
     *   1. seedCoach() : cree le compte coach par defaut si absent
     *   2. Charge HomeView.fxml avec FXMLLoader
     *   3. Cree la Scene avec ce contenu
     *   4. Applique le CSS home.css
     *   5. Configure et affiche la fenetre (Stage)
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Cree le coach par defaut au premier demarrage si necessaire
        seedCoach();

        // 1. FXMLLoader lit HomeView.fxml, cree tous les composants JavaFX,
        //    instancie HomeController, injecte les @FXML, appelle initialize()
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/HomeView.fxml")));
        Parent root = loader.load();

        // 2. Cree la Scene avec le contenu de HomeView comme noeud racine
        //    Taille initiale : 980 x 620 pixels
        Scene scene = new Scene(root, 980, 620);

        // 3. Applique la feuille de style CSS a toute la Scene
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/home.css")).toExternalForm());

        // 4. Configure la fenetre
        primaryStage.setTitle("FitSense");
        primaryStage.setMinWidth(840);   // taille minimale pour eviter les deformations
        primaryStage.setMinHeight(540);
        
        // 5. Set application icon
        utils.AppIconLoader.setIcon(primaryStage);
        
        primaryStage.setScene(scene);
        primaryStage.show(); // affiche la fenetre
    }

    /**
     * main — point d'entree Java standard.
     * launch() demarre le cycle de vie JavaFX et appelle start().
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * seedCoach — cree un compte coach par defaut au premier demarrage.
     *
     * Verifie d'abord si le coach existe deja (findByEmail) pour ne pas
     * creer de doublon a chaque redemarrage de l'application.
     *
     * Le mot de passe "123456" sera automatiquement hache en BCrypt
     * par UserService.createPrepared() avant insertion en base.
     *
     * Ce coach peut etre utilise pour tester les fonctionnalites coach
     * sans avoir a en creer un manuellement depuis l'interface admin.
     */
    private void seedCoach() {
        try {
            UserService userService = new UserService();
            // Verifie si le coach existe deja pour eviter les doublons
            if (userService.findByEmail("coachamine@gmail.com") == null) {
                User coach = new User();
                coach.setFirstname("Amine");
                coach.setLastname("Coach");
                coach.setEmail("coachamine@gmail.com");
                coach.setPassword("123456"); // sera hache en BCrypt par createPrepared
                coach.setRolesJson("[\"ROLE_COACH\"]"); // role coach
                coach.setAccountStatus("active");
                userService.createPrepared(coach); // INSERT en base
                System.out.println("Coach coachamine@gmail.com created.");
            }
        } catch (Exception e) {
            // Ne pas planter l'app si le seeder echoue (ex: DB pas encore prete)
            System.err.println("Seeder error: " + e.getMessage());
        }
    }
}
