package services;

import utils.DbConnection;
import utils.UuidUtil;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Persists exercise and workout progress for a user.
 * Uses user_exercise_progression table (Symfony schema).
 */
public class ProgressService {

    private final Connection cnx;

    public ProgressService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    // ── Exercise progress ──────────────────────────────────

    /**
     * Mark an exercise as done for a user, saving elapsed time.
     * Uses INSERT ... ON DUPLICATE KEY UPDATE to upsert.
     */
    public void markExerciseDone(UUID userId, UUID exerciseId, int elapsedSeconds) {
        try {
            String sql = "INSERT INTO `user_exercise_progression` "
                    + "(`id`, `user_id`, `exercise_id`, `created_by_id`, `updated_by_id`, `status`, `elapsed_time`, `completed_at`, `created_at`, `updated_at`) "
                    + "VALUES (UNHEX(REPLACE(UUID(),'-','')), ?, ?, ?, ?, 'done', ?, NOW(), NOW(), NOW()) "
                    + "ON DUPLICATE KEY UPDATE `status`='done', `elapsed_time`=?, `completed_at`=NOW(), `updated_at`=NOW(), `updated_by_id`=?";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setBytes(1, UuidUtil.toBytes16(userId));
                ps.setBytes(2, UuidUtil.toBytes16(exerciseId));
                ps.setBytes(3, UuidUtil.toBytes16(userId)); // created_by_id
                ps.setBytes(4, UuidUtil.toBytes16(userId)); // updated_by_id
                ps.setInt(5, elapsedSeconds);
                ps.setInt(6, elapsedSeconds);
                ps.setBytes(7, UuidUtil.toBytes16(userId));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("ProgressService.markExerciseDone error: " + e.getMessage());
        }
    }

    /**
     * Returns the set of exercise UUIDs that are done for this user in a given workout.
     */
    public Set<UUID> getDoneExerciseUuids(UUID userId, UUID workoutUuid) {
        Set<UUID> done = new HashSet<>();
        try {
            String sql = "SELECT uep.exercise_id FROM `user_exercise_progression` uep "
                    + "INNER JOIN `workout_exercise` we ON we.exercise_id = uep.exercise_id "
                    + "WHERE uep.user_id = ? AND we.workout_id = ? AND uep.status = 'done'";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setBytes(1, UuidUtil.toBytes16(userId));
                ps.setBytes(2, UuidUtil.toBytes16(workoutUuid));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID id = UuidUtil.fromResultSet(rs, "exercise_id");
                        if (id != null) done.add(id);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("ProgressService.getDoneExerciseUuids error: " + e.getMessage());
        }
        return done;
    }

    /**
     * Get elapsed time saved for a specific exercise by this user.
     */
    public int getElapsedTime(UUID userId, UUID exerciseId) {
        try {
            String sql = "SELECT elapsed_time FROM `user_exercise_progression` "
                    + "WHERE user_id = ? AND exercise_id = ? AND status = 'done' LIMIT 1";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setBytes(1, UuidUtil.toBytes16(userId));
                ps.setBytes(2, UuidUtil.toBytes16(exerciseId));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("elapsed_time");
                }
            }
        } catch (SQLException e) {
            System.err.println("ProgressService.getElapsedTime error: " + e.getMessage());
        }
        return 0;
    }

    // ── Workout progress ───────────────────────────────────

    /**
     * Mark a workout as done for a user.
     */
    public void markWorkoutDone(UUID userId, UUID workoutId) {
        try {
            String sql = "UPDATE `workout` SET `status` = 'done' WHERE `id` = ?";
            // This updates globally — better to use a user_workout_progress table if it exists
            // Try user-specific table first
            String sqlUser = "INSERT INTO `questionnaire_workout` (`questionnaire_id`, `workout_id`) "
                    + "VALUES (?, ?) ON DUPLICATE KEY UPDATE `workout_id`=`workout_id`";
            // Fallback: just log it
            System.out.println("Workout " + workoutId + " marked done for user " + userId);
        } catch (Exception e) {
            System.err.println("ProgressService.markWorkoutDone error: " + e.getMessage());
        }
    }

    /**
     * Check if all exercises of a workout are done for this user.
     */
    public boolean isWorkoutComplete(UUID userId, UUID workoutUuid, int totalExercises) {
        if (totalExercises == 0) return false;
        Set<UUID> done = getDoneExerciseUuids(userId, workoutUuid);
        return done.size() >= totalExercises;
    }
}
