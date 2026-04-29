package controllers;

import app.AppSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import models.MentalHealthEvaluation;
import models.User;
import services.MentalHealthTrendService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the mental health chart fragment displayed on the dashboard.
 */
public class MentalHealthChartController {

    @FXML
    private LineChart<String, Number> mentalHealthChart;
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    @FXML
    private Label chartStatusLabel;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd");

    @FXML
    private void initialize() {
        setupChart();
        loadChartData();
    }

    private void setupChart() {
        if (mentalHealthChart == null) return;

        // Configure axes
        xAxis.setLabel("Date");
        yAxis.setLabel("Mental Health Score");
        
        // Chart styling
        mentalHealthChart.setTitle("Your Mental Health Journey");
        mentalHealthChart.setAnimated(true);
        mentalHealthChart.setCreateSymbols(true);
        mentalHealthChart.setLegendVisible(false);
    }

    private void loadChartData() {
        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            updateStatus("Please log in to view your mental health progress.");
            return;
        }

        // Load data in background thread
        new Thread(() -> {
            try {
                MentalHealthTrendService trendService = new MentalHealthTrendService();
                List<MentalHealthEvaluation> evaluations = trendService.getRecentEvaluations(currentUser.getId(), 10);

                Platform.runLater(() -> {
                    if (evaluations.isEmpty()) {
                        updateStatus("No mental health data yet. Take your first assessment to see your progress!");
                        return;
                    }

                    // Create data series
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Mental Health Score");

                    // Add data points (reverse order to show chronologically)
                    for (int i = evaluations.size() - 1; i >= 0; i--) {
                        MentalHealthEvaluation eval = evaluations.get(i);
                        String dateLabel = eval.getTestedAt() != null 
                            ? eval.getTestedAt().format(DATE_FORMAT)
                            : "Unknown";
                        
                        XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(dateLabel, eval.getScore());
                        series.getData().add(dataPoint);
                    }

                    // Clear existing data and add new series
                    mentalHealthChart.getData().clear();
                    mentalHealthChart.getData().add(series);

                    // Update status with insights
                    updateStatusWithInsights(evaluations);
                });

            } catch (SQLException e) {
                Platform.runLater(() -> 
                    updateStatus("Unable to load mental health data. Please try again later."));
            }
        }).start();
    }

    private void updateStatusWithInsights(List<MentalHealthEvaluation> evaluations) {
        if (evaluations.isEmpty()) {
            updateStatus("No data available.");
            return;
        }

        MentalHealthEvaluation latest = evaluations.get(0);
        int latestScore = latest.getScore();
        String status = latest.getStatus() != null ? latest.getStatus() : "Unknown";

        String insight;
        if (evaluations.size() >= 2) {
            MentalHealthEvaluation previous = evaluations.get(1);
            int change = latestScore - previous.getScore();
            
            if (change > 2) {
                insight = String.format("📈 Great progress! Your score improved by %d points to %d (%s)", 
                    change, latestScore, status);
            } else if (change < -2) {
                insight = String.format("📉 Your score decreased by %d points to %d (%s). Consider reaching out for support.", 
                    Math.abs(change), latestScore, status);
            } else {
                insight = String.format("📊 Your score is stable at %d (%s). Keep up the consistent tracking!", 
                    latestScore, status);
            }
        } else {
            insight = String.format("📋 Latest score: %d (%s). Keep tracking to see your progress over time!", 
                latestScore, status);
        }

        updateStatus(insight);
    }

    private void updateStatus(String message) {
        if (chartStatusLabel != null) {
            chartStatusLabel.setText(message);
        }
    }

    /**
     * Public method to refresh chart data (called when user returns to dashboard)
     */
    public void refreshChart() {
        loadChartData();
    }
}