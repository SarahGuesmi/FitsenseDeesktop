package services;

import utils.DbConnection;
import utils.UuidUtil;

import java.sql.*;
import java.util.UUID;

/**
 * Stores exercise ratings in user_exercise_progression.rating column.
 * No new table needed — reuses existing progression table.
 */
public class ExerciseRatingService {

    private final Connection cnx;

    public ExerciseRatingService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    /** Save or update rating (1-5) — upserts into user_exercise_progression. */
    public void saveRating(UUID userId, UUID exerciseId, int rating) {
        try {
            // Try update first (row may already exist from exercise completion)
            String update = "UPDATE `user_exercise_progression` SET `rating` = ? "
                    + "WHERE `user_id` = ? AND `exercise_id` = ?";
            try (PreparedStatement ps = cnx.prepareStatement(update)) {
                ps.setInt(1, rating);
                ps.setBytes(2, UuidUtil.toBytes16(userId));
                ps.setBytes(3, UuidUtil.toBytes16(exerciseId));
                int rows = ps.executeUpdate();
                if (rows > 0) return; // updated existing row
            }
            // No existing row — insert new one with just the rating
            String insert = "INSERT INTO `user_exercise_progression` "
                    + "(`id`, `user_id`, `exercise_id`, `created_by_id`, `updated_by_id`, "
                    + "`status`, `elapsed_time`, `rating`, `created_at`, `updated_at`) "
                    + "VALUES (UNHEX(REPLACE(UUID(),'-','')), ?, ?, ?, ?, 'rated', 0, ?, NOW(), NOW())";
            try (PreparedStatement ps = cnx.prepareStatement(insert)) {
                ps.setBytes(1, UuidUtil.toBytes16(userId));
                ps.setBytes(2, UuidUtil.toBytes16(exerciseId));
                ps.setBytes(3, UuidUtil.toBytes16(userId));
                ps.setBytes(4, UuidUtil.toBytes16(userId));
                ps.setInt(5, rating);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("ExerciseRatingService.saveRating error: " + e.getMessage());
        }
    }

    /** Get user's rating for an exercise (0 = not rated). */
    public int getRating(UUID userId, UUID exerciseId) {
        try {
            String sql = "SELECT `rating` FROM `user_exercise_progression` "
                    + "WHERE `user_id` = ? AND `exercise_id` = ? LIMIT 1";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setBytes(1, UuidUtil.toBytes16(userId));
                ps.setBytes(2, UuidUtil.toBytes16(exerciseId));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("rating");
                }
            }
        } catch (SQLException e) {
            System.err.println("ExerciseRatingService.getRating error: " + e.getMessage());
        }
        return 0;
    }

    /** Average rating for an exercise across all users. */
    public double getAverageRating(UUID exerciseId) {
        try {
            String sql = "SELECT AVG(`rating`) FROM `user_exercise_progression` "
                    + "WHERE `exercise_id` = ? AND `rating` > 0";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setBytes(1, UuidUtil.toBytes16(exerciseId));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("ExerciseRatingService.getAverageRating error: " + e.getMessage());
        }
        return 0;
    }

    /** Get average rating for all exercises of a workout — for coach view. */
    public double getWorkoutAverageRating(UUID workoutUuid) {
        try {
            String sql = "SELECT AVG(uep.rating) FROM `user_exercise_progression` uep "
                    + "INNER JOIN `workout_exercise` we ON we.exercise_id = uep.exercise_id "
                    + "WHERE we.workout_id = ? AND uep.rating > 0";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setBytes(1, UuidUtil.toBytes16(workoutUuid));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("ExerciseRatingService.getWorkoutAverageRating error: " + e.getMessage());
        }
        return 0;
    }
}
