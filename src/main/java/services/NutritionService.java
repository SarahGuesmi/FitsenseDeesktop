package services;

import utils.DbConnection;

import java.nio.ByteBuffer;
import java.sql.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
public class NutritionService {

    private final Connection cnx;

    public NutritionService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    public record DailyNutritionData(
            int calories,
            int caloriesGoal,
            int waterMl,
            int waterGoal
    ) {}

    public DailyNutritionData getOrCreateToday(String userId) {
        try {
            LocalDate today = LocalDate.now();

            String select = """
                SELECT calories, calories_goal, water_ml, water_goal
                FROM daily_nutrition
                WHERE user_id = ? AND day_date = ?
            """;

            PreparedStatement ps = cnx.prepareStatement(select);
            ps.setBytes(1, uuidToBytes(userId));
            ps.setDate(2, Date.valueOf(today));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new DailyNutritionData(
                        rs.getInt("calories"),
                        rs.getInt("calories_goal"),
                        rs.getInt("water_ml"),
                        rs.getInt("water_goal")
                );
            }

            int caloriesGoal = calculateCaloriesGoal(userId);
            int waterGoal = calculateWaterGoal(userId);

            String insert = """
                INSERT INTO daily_nutrition
                (id, user_id, day_date, calories, water_ml, calories_goal, water_goal,
                 over_goal_alert_shown, water_goal_alert_shown)
                VALUES (?, ?, ?, 0, 0, ?, ?, 0, 0)
            """;

            PreparedStatement psInsert = cnx.prepareStatement(insert);
            psInsert.setBytes(1, uuidToBytes(UUID.randomUUID().toString()));
            psInsert.setBytes(2, uuidToBytes(userId));
            psInsert.setDate(3, Date.valueOf(today));
            psInsert.setInt(4, caloriesGoal);
            psInsert.setInt(5, waterGoal);
            psInsert.executeUpdate();

            return new DailyNutritionData(0, caloriesGoal, 0, waterGoal);

        } catch (SQLException e) {
            throw new RuntimeException("Error loading daily nutrition", e);
        }
    }

    public void addWater(String userId, int amount) {
        getOrCreateToday(userId);

        try {
            String sql = """
                UPDATE daily_nutrition
                SET water_ml = water_ml + ?
                WHERE user_id = ? AND day_date = ?
            """;

            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, amount);
            ps.setBytes(2, uuidToBytes(userId));
            ps.setDate(3, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();

            checkWaterNotification(userId);

        } catch (SQLException e) {
            throw new RuntimeException("Error adding water", e);
        }
    }

    public void addCalories(String userId, int kcal) {
        getOrCreateToday(userId);

        try {
            String sql = """
                UPDATE daily_nutrition
                SET calories = calories + ?
                WHERE user_id = ? AND day_date = ?
            """;

            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, kcal);
            ps.setBytes(2, uuidToBytes(userId));
            ps.setDate(3, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();

            checkCaloriesNotification(userId);

        } catch (SQLException e) {
            throw new RuntimeException("Error adding calories", e);
        }
    }

    private void checkWaterNotification(String userId) throws SQLException {
        String sql = """
            SELECT water_ml, water_goal, water_goal_alert_shown
            FROM daily_nutrition
            WHERE user_id = ? AND day_date = ?
        """;

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setBytes(1, uuidToBytes(userId));
        ps.setDate(2, Date.valueOf(LocalDate.now()));

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            int water = rs.getInt("water_ml");
            int goal = rs.getInt("water_goal");
            boolean alreadyShown = rs.getBoolean("water_goal_alert_shown");

            if (water >= goal && !alreadyShown) {
                new NotificationService().createNotification(
                        userId,
                        "Congratulations! You reached your daily water goal.",
                        "water_goal_reached"
                );

                markWaterAlertShown(userId);
            }
        }
    }

    private void checkCaloriesNotification(String userId) throws SQLException {
        String sql = """
            SELECT calories, calories_goal, over_goal_alert_shown
            FROM daily_nutrition
            WHERE user_id = ? AND day_date = ?
        """;

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setBytes(1, uuidToBytes(userId));
        ps.setDate(2, Date.valueOf(LocalDate.now()));

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            int calories = rs.getInt("calories");
            int goal = rs.getInt("calories_goal");
            boolean alreadyShown = rs.getBoolean("over_goal_alert_shown");

            if (calories > goal && !alreadyShown) {
                new NotificationService().createNotification(
                        userId,
                        "You exceeded your daily calories goal.",
                        "calories_exceeded"
                );

                markCaloriesAlertShown(userId);
            }
        }
    }

    private void markWaterAlertShown(String userId) throws SQLException {
        String sql = """
            UPDATE daily_nutrition
            SET water_goal_alert_shown = 1
            WHERE user_id = ? AND day_date = ?
        """;

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setBytes(1, uuidToBytes(userId));
        ps.setDate(2, Date.valueOf(LocalDate.now()));
        ps.executeUpdate();
    }

    private void markCaloriesAlertShown(String userId) throws SQLException {
        String sql = """
            UPDATE daily_nutrition
            SET over_goal_alert_shown = 1
            WHERE user_id = ? AND day_date = ?
        """;

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setBytes(1, uuidToBytes(userId));
        ps.setDate(2, Date.valueOf(LocalDate.now()));
        ps.executeUpdate();
    }

    public Map<String, Integer> getWeeklyCalories(String userId) {
        Map<String, Integer> result = new LinkedHashMap<>();
        LocalDate start = LocalDate.now().minusDays(6);

        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            result.put(day.getDayOfWeek().toString().substring(0, 3), 0);
        }

        try {
            String sql = """
                SELECT day_date, calories
                FROM daily_nutrition
                WHERE user_id = ?
                AND day_date BETWEEN ? AND ?
                ORDER BY day_date ASC
            """;

            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setBytes(1, uuidToBytes(userId));
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(LocalDate.now()));

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                LocalDate date = rs.getDate("day_date").toLocalDate();
                String label = date.getDayOfWeek().toString().substring(0, 3);
                result.put(label, rs.getInt("calories"));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error loading weekly calories", e);
        }

        return result;
    }

    private int calculateCaloriesGoal(String userId) {
        double weight = getUserWeight(userId);
        String objective = getUserObjective(userId);

        if (weight <= 0) return 2000;

        objective = objective == null ? "" : objective.toUpperCase();

        if (objective.contains("WEIGHT_LOSS")) return (int) Math.round(weight * 24);
        if (objective.contains("MUSCLE_GAIN")) return (int) Math.round(weight * 32);
        if (objective.contains("ENDURANCE")) return (int) Math.round(weight * 30);

        return (int) Math.round(weight * 28);
    }

    private int calculateWaterGoal(String userId) {
        double weight = getUserWeight(userId);
        if (weight <= 0) return 2500;
        return (int) Math.round(weight * 30);
    }

    private double getUserWeight(String userId) {
        try {
            String sql = """
                SELECT weight
                FROM profile_physique
                WHERE user_id = ?
                ORDER BY id DESC
                LIMIT 1
            """;

            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setBytes(1, uuidToBytes(userId));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getDouble("weight");

        } catch (SQLException e) {
            System.out.println("Weight not found, using default.");
        }

        return 0;
    }

    private String getUserObjective(String userId) {
        try {
            String sql = """
                SELECT objectifs
                FROM app_user
                WHERE id = ?
                LIMIT 1
            """;

            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setBytes(1, uuidToBytes(userId));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getString("objectifs");

        } catch (SQLException e) {
            System.out.println("Objective not found, using default.");
        }

        return "";
    }

    private byte[] uuidToBytes(String uuid) {
        UUID u = UUID.fromString(uuid);
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(u.getMostSignificantBits());
        bb.putLong(u.getLeastSignificantBits());
        return bb.array();
    }
}