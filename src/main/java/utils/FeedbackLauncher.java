package utils;

import controllers.WorkoutFeedbackController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Workout;

import java.io.IOException;
import java.util.Objects;

/**
 * Utility to open the post-workout feedback popup.
 * Usage: FeedbackLauncher.show(workout, () -> { ... after done ... });
 */
public final class FeedbackLauncher {

    private FeedbackLauncher() {}

    public static void show(Workout workout, Runnable onDone) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(FeedbackLauncher.class.getResource("/fxml/WorkoutFeedbackView.fxml")));
            Parent root = loader.load();

            WorkoutFeedbackController controller = loader.getController();
            controller.setWorkout(workout);
            controller.setOnDoneCallback(() -> {
                if (onDone != null) onDone.run();
            });

            Stage stage = new Stage();
            utils.AppIconLoader.setIcon(stage);
            stage.setTitle("Post-Workout Feedback");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 660, 600));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("Could not open feedback view", e);
        }
    }
}
