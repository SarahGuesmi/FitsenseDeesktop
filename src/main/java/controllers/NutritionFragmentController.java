package controllers;

import app.AppSession;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import models.User;
import services.NutritionService;
import java.util.List;
import java.util.Map;

public class NutritionFragmentController {

    @FXML private Label caloriesGoalLabel;
    @FXML private Label caloriesPercentLabel;
    @FXML private Label caloriesRemainingLabel;
    @FXML private ProgressIndicator caloriesProgress;

    @FXML private Label waterGoalLabel;
    @FXML private Label waterPercentLabel;
    @FXML private ProgressIndicator waterProgress;
    @FXML private Label caloriesValueLabel;
    @FXML private Label waterValueLabel;
    @FXML private BarChart<String, Number> weeklyCaloriesChart;

    private final NutritionService nutritionService = new NutritionService();

    @FXML
    private void initialize() {
        reloadNutrition();
    }
    public void reloadNutrition() {
        User user = AppSession.getCurrentUser();
        if (user == null) return;

        String userId = user.getId().toString();

        NutritionService.DailyNutritionData daily =
                nutritionService.getOrCreateToday(userId);

        int calories = daily.calories();
        int caloriesGoal = daily.caloriesGoal();
        int water = daily.waterMl();
        int waterGoal = daily.waterGoal();

        int calPercent = caloriesGoal <= 0 ? 0 : (int) Math.round(calories * 100.0 / caloriesGoal);
        int waterPercent = waterGoal <= 0 ? 0 : (int) Math.round(water * 100.0 / waterGoal);
        caloriesValueLabel.setText(String.valueOf(calories));
        waterValueLabel.setText(String.valueOf(water));
        caloriesGoalLabel.setText(calories + " / " + caloriesGoal + " kcal");
        caloriesPercentLabel.setText(calPercent + "% of daily goal");
        caloriesRemainingLabel.setText("Remaining: " + Math.max(0, caloriesGoal - calories) + " kcal");
        caloriesProgress.setProgress(caloriesGoal <= 0 ? 0 : Math.min(1.0, calories / (double) caloriesGoal));

        waterGoalLabel.setText(water + " / " + waterGoal + " ml");
        waterPercentLabel.setText(waterPercent + "% of daily goal");
        waterProgress.setProgress(waterGoal <= 0 ? 0 : Math.min(1.0, water / (double) waterGoal));

        loadWeeklyCalories(userId);
    }

    @FXML
    private void addWater250() {
        User user = AppSession.getCurrentUser();
        if (user == null) return;

        nutritionService.addWater(user.getId().toString(), 250);
        reloadNutrition();
    }

    private void loadWeeklyCalories(String userId) {
        weeklyCaloriesChart.getData().clear();

        Map<String, Integer> data = nutritionService.getWeeklyCalories(userId);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Calories (kcal)");

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        weeklyCaloriesChart.getData().add(series);
    }
}