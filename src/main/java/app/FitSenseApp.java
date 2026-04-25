package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.User;
import services.UserService;
import utils.GifProxyServer;
import java.io.IOException;
import java.util.Objects;

public class FitSenseApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        GifProxyServer.start(); // ✅ une seule fois ici
        seedCoach();

        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/fxml/HomeView.fxml")));
        Parent root = loader.load();

        Scene scene = new Scene(root, 980, 620);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/home.css")).toExternalForm());

        primaryStage.setTitle("FitSense");
        primaryStage.setMinWidth(840);
        primaryStage.setMinHeight(540);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ❌ SUPPRIMÉ : GifProxyServer.start(); — ligne orpheline retirée

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        GifProxyServer.stop(); // ✅ arrêt propre à la fermeture
    }

    private void seedCoach() {
        try {
            UserService userService = new UserService();
            if (userService.findByEmail("coachamine@gmail.com") == null) {
                User coach = new User();
                coach.setFirstname("Amine");
                coach.setLastname("Coach");
                coach.setEmail("coachamine@gmail.com");
                coach.setPassword("123456");
                coach.setRolesJson("[\"ROLE_COACH\"]");
                coach.setAccountStatus("active");
                userService.createPrepared(coach);
                System.out.println("Coach coachamine@gmail.com created.");
            }
        } catch (Exception e) {
            System.err.println("Seeder error: " + e.getMessage());
        }
    }
}