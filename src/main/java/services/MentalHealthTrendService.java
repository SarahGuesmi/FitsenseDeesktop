package services;

import models.MentalHealthEvaluation;
import models.User;
import utils.DbConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Advanced mental health trend analysis service.
 * Calculates trends, insights, and statistics from user's mental health history.
 */
public class MentalHealthTrendService {

    private final Connection cnx;

    public MentalHealthTrendService() {
        this.cnx = DbConnection.getInstance().getCnx();
    }

    /**
     * Returns the user's last N mental health evaluations sorted by date.
     */
    public List<MentalHealthEvaluation> getRecentEvaluations(UUID userId, int limit) throws SQLException {
        String sql = "SELECT * FROM `mental_health_evaluation` WHERE `user_id` = ? "
                   + "ORDER BY `tested_at` DESC LIMIT ?";
        List<MentalHealthEvaluation> evaluations = new ArrayList<>();

        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, utils.UuidUtil.toBytes16(userId));
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MentalHealthEvaluation ev = mapRow(rs);
                    evaluations.add(ev);
                }
            }
        }
        return evaluations;
    }

    /**
     * Calculates trend direction based on last 3 scores.
     * Returns: "improving", "declining", "stable", or "insufficient_data"
     */
    public String calculateTrend(UUID userId) throws SQLException {
        List<MentalHealthEvaluation> evals = getRecentEvaluations(userId, 3);
        if (evals.size() < 2) {
            return "insufficient_data";
        }

        // Calculate average of first half vs second half
        int mid = evals.size() / 2;
        double earlyAvg = 0, laterAvg = 0;

        for (int i = 0; i < mid; i++) {
            earlyAvg += evals.get(i).getScore();
        }
        earlyAvg /= mid;

        for (int i = mid; i < evals.size(); i++) {
            laterAvg += evals.get(i).getScore();
        }
        laterAvg /= (evals.size() - mid);

        double diff = laterAvg - earlyAvg;
        if (diff > 2.0) return "improving";
        if (diff < -2.0) return "declining";
        return "stable";
    }

    /**
     * Returns a personalized insight based on trend and recent scores.
     */
    public String generateInsight(UUID userId) throws SQLException {
        String trend = calculateTrend(userId);
        if (trend.equals("insufficient_data")) {
            return "Keep tracking your mental health to see patterns over time.";
        }

        List<MentalHealthEvaluation> recent = getRecentEvaluations(userId, 1);
        if (recent.isEmpty()) {
            return "No recent data available.";
        }

        int latestScore = recent.get(0).getScore();
        String status = recent.get(0).getStatus();

        StringBuilder insight = new StringBuilder();

        // Trend-based insight
        switch (trend) {
            case "improving" -> insight.append("Your mental wellbeing is improving! ");
            case "declining" -> insight.append("Your scores have been trending downward. ");
            case "stable" -> insight.append("Your mental health has been stable. ");
        }

        // Score-based insight
        if (latestScore <= 10) {
            insight.append("Your recent low score suggests you might benefit from extra support. ");
        } else if (latestScore <= 15) {
            insight.append("You're in a moderate range — small daily practices can help. ");
        } else {
            insight.append("You're doing well — keep up the positive habits! ");
        }

        // Status-based insight
        if (status != null) {
            if (status.contains("Needs attention")) {
                insight.append("Consider reaching out to your coach for guidance.");
            } else if (status.contains("Moderate")) {
                insight.append("Regular check-ins can help maintain balance.");
            } else {
                insight.append("Your consistent efforts are paying off.");
            }
        }

        return insight.toString();
    }

    /**
     * Returns the user's average mental health score over the last 30 days.
     */
    public double getAverageScoreLast30Days(UUID userId) throws SQLException {
        String sql = "SELECT AVG(score) as avg_score FROM `mental_health_evaluation` "
                   + "WHERE `user_id` = ? AND `tested_at` >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, utils.UuidUtil.toBytes16(userId));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("avg_score");
                }
            }
        }
        return 0.0;
    }

    /**
     * Returns the most frequent mental health status over the last month.
     */
    public String getMostCommonStatus(UUID userId) throws SQLException {
        String sql = "SELECT status, COUNT(*) as count FROM `mental_health_evaluation` "
                   + "WHERE `user_id` = ? AND `tested_at` >= DATE_SUB(NOW(), INTERVAL 30 DAY) "
                   + "GROUP BY status ORDER BY count DESC LIMIT 1";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, utils.UuidUtil.toBytes16(userId));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        }
        return "No data";
    }

    // Helper method to map ResultSet to MentalHealthEvaluation
    private MentalHealthEvaluation mapRow(ResultSet rs) throws SQLException {
        UUID id = utils.UuidUtil.fromResultSet(rs, "id");
        MentalHealthEvaluation ev = new MentalHealthEvaluation(id);

        ev.setUserId(utils.UuidUtil.fromResultSet(rs, "user_id"));
        ev.setCoachTestId(utils.UuidUtil.fromResultSet(rs, "coach_test_id"));
        ev.setCoachTestTitle(rs.getString("coach_test_title"));

        Timestamp ts = rs.getTimestamp("tested_at");
        if (ts != null) {
            ev.setTestedAt(ts.toLocalDateTime());
        }

        ev.setScore(rs.getInt("score"));
        ev.setStatus(rs.getString("status"));
        ev.setNotes(rs.getString("member_notes"));

        // Legacy fields
        ev.setMood(rs.getInt("mood"));
        ev.setStress(rs.getInt("stress"));
        ev.setSleep(rs.getInt("sleep"));
        ev.setMotivation(rs.getInt("motivation"));
        ev.setMentalTired(rs.getInt("mental_tired"));

        return ev;
    }
}
