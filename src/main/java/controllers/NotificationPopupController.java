package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.NotificationService;

import java.util.List;

public class NotificationPopupController {

    @FXML
    private VBox notifContainer;

    private NotificationService service = new NotificationService();
    private String userId;
    private DashboardController dashboard;

    public void loadNotifications(String userId, DashboardController dashboard) {
        this.userId = userId;
        this.dashboard = dashboard;

        notifContainer.getChildren().clear();

        List<String> notifications = service.getUserNotifications(userId);

        if (notifications.isEmpty()) {
            Label empty = new Label("No notifications");
            empty.setStyle("-fx-text-fill: white;");
            notifContainer.getChildren().add(empty);
            return;
        }

        for (String msg : notifications) {
            Label label = new Label("• " + msg);

            // 🔥 STYLE IMPORTANT
            label.getStyleClass().add("notification-item");
            label.setWrapText(true);
            label.setMaxWidth(360);

            notifContainer.getChildren().add(label);
        }
    }    @FXML
    private void closePopup() {
        Stage stage = (Stage) notifContainer.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void markAllAsRead() {
        service.markAllAsRead(userId);

        notifContainer.getChildren().clear();
        notifContainer.getChildren().add(new Label("No notifications"));

        // 🔥 refresh badge
        if (dashboard != null) {
            dashboard.refreshNotificationBadge();
        }
    }
}