package controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import utils.ActivityTracker;
import utils.ActivityTracker.Event;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ActivityLogController {

    @FXML private Label dateLabel;
    @FXML private HBox summaryBox;
    @FXML private Label minutesLabel;
    @FXML private Label exercisesLabel;
    @FXML private Label workoutsLabel;
    @FXML private Label insightLabel;
    @FXML private VBox emptyBox;
    @FXML private VBox timelineBox;

    @FXML
    private void initialize() {
        dateLabel.setText("Today, " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd yyyy")));
        refresh();
    }

    public void refresh() {
        List<Event> events = ActivityTracker.getEvents();
        boolean hasActivity = ActivityTracker.hasActivity();

        if (!hasActivity) {
            emptyBox.setVisible(true);  emptyBox.setManaged(true);
            summaryBox.setVisible(false); summaryBox.setManaged(false);
            insightLabel.setVisible(false); insightLabel.setManaged(false);
            timelineBox.setVisible(false); timelineBox.setManaged(false);
            return;
        }

        // Show summary
        emptyBox.setVisible(false); emptyBox.setManaged(false);
        summaryBox.setVisible(true); summaryBox.setManaged(true);
        minutesLabel.setText(String.valueOf(ActivityTracker.totalActiveMinutes()));
        exercisesLabel.setText(String.valueOf(ActivityTracker.completedExercisesCount()));
        workoutsLabel.setText(String.valueOf(ActivityTracker.completedWorkoutsCount()));

        // Insight
        String insight = ActivityTracker.behaviorInsight();
        if (insight != null) {
            insightLabel.setText("💡 " + insight);
            insightLabel.setVisible(true); insightLabel.setManaged(true);
        }

        // Timeline
        timelineBox.getChildren().clear();
        timelineBox.setVisible(true); timelineBox.setManaged(true);

        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            boolean isLast = (i == events.size() - 1);
            timelineBox.getChildren().add(buildTimelineRow(e, isLast));
        }
    }

    private HBox buildTimelineRow(Event e, boolean isLast) {
        // Time
        Label time = new Label(e.formattedTime());
        time.setStyle("-fx-text-fill:#6B7280;-fx-font-size:11px;-fx-font-weight:700;-fx-min-width:40;");
        time.setAlignment(Pos.CENTER_RIGHT);

        // Dot + line
        VBox dotLine = new VBox();
        dotLine.setAlignment(Pos.TOP_CENTER);
        dotLine.setMinWidth(24);

        Label dot = new Label(ActivityTracker.icon(e.type));
        dot.setStyle("-fx-font-size:14px;-fx-min-width:24;-fx-min-height:24;-fx-alignment:center;");

        if (!isLast) {
            Region line = new Region();
            line.setStyle("-fx-background-color:#1F2937;-fx-min-width:2;-fx-max-width:2;-fx-pref-height:32;");
            line.setTranslateX(11);
            dotLine.getChildren().addAll(dot, line);
        } else {
            dotLine.getChildren().add(dot);
        }

        // Content
        VBox content = new VBox(2);
        content.setPadding(new Insets(0, 0, isLast ? 0 : 8, 0));

        Label labelLbl = new Label(e.label);
        labelLbl.setStyle("-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:700;");

        content.getChildren().add(labelLbl);

        if (e.detail != null && !e.detail.isBlank()) {
            Label detail = new Label(e.detail);
            detail.setStyle("-fx-text-fill:#6B7280;-fx-font-size:11px;");
            content.getChildren().add(detail);
        }

        HBox row = new HBox(10, time, dotLine, content);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(4, 0, 0, 0));
        return row;
    }
}
