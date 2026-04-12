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
 * JavaFX entry point for the FitSense desktop client.
 * Run: {@code mvn javafx:run} (see {@code pom.xml}).
 */
public class FitSenseApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        seedCoach();

        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/HomeView.fxml")));
        Parent root = loader.load();

        Scene scene = new Scene(root, 980, 620);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/home.css")).toExternalForm());

        primaryStage.setTitle("FitSense");
        primaryStage.setMinWidth(840);
        primaryStage.setMinHeight(540);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void seedCoach() {
        try {
            UserService userService = new UserService();
            if (userService.findByEmail("coachamine@gmail.com") == null) {
                User coach = new User();
                coach.setFirstname("Amine");
                coach.setLastname("Coach");
                coach.setEmail("coachamine@gmail.com");
                coach.setPassword("123456"); // will be BCrypt-hashed by createPrepared
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
