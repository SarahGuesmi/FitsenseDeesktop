package services;

import models.Exercise;
import models.Workout;
import utils.DbConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExerciseService implements CRUD<Exercise> {

    private final Connection cnx;

    public ExerciseService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    // Mapping ResultSet → Exercise
    private Exercise mapRow(ResultSet rs) throws SQLException {
        Exercise e = new Exercise();
        e.setId(rs.getInt("id"));
        e.setNom(rs.getString("nom"));
        e.setType(rs.getString("type"));
        e.setDuree(rs.getInt("duree"));
        e.setDescription(rs.getString("description"));
        e.setSets(rs.getObject("sets") != null ? rs.getInt("sets") : null);
        e.setReps(rs.getObject("reps") != null ? rs.getInt("reps") : null);
        e.setImageName(rs.getString("image_name"));
        Timestamp ts = rs.getTimestamp("updated_at");
        if (ts != null) e.setUpdatedAt(ts.toLocalDateTime());
        e.setYoutubeVideoId(rs.getString("youtube_video_id"));
        return e;
    }

    // Find exercises by workout id (ManyToMany relation)
    public List<Exercise> findByWorkoutId(int workoutId) throws SQLException {
        String sql = "SELECT e.* FROM exercise e " +
                "JOIN workout_exercise ew ON e.id = ew.exercise_id " +
                "WHERE ew.workout_id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, workoutId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Exercise> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    // ================= CRUD =================

    @Override
    public void create(Exercise e) throws SQLException {
        createPrepared(e);
    }

    @Override
    public void createPrepared(Exercise e) throws SQLException {
        String sql = "INSERT INTO exercise (nom, type, duree, description, sets, reps, image_name, updated_at, youtube_video_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, e.getNom());
            stmt.setString(2, e.getType());
            stmt.setObject(3, e.getDuree(), Types.INTEGER);
            stmt.setString(4, e.getDescription());
            stmt.setObject(5, e.getSets(), Types.INTEGER);
            stmt.setObject(6, e.getReps(), Types.INTEGER);
            stmt.setString(7, e.getImageName());
            stmt.setTimestamp(8, e.getUpdatedAt() != null ? Timestamp.valueOf(e.getUpdatedAt()) : null);
            stmt.setString(9, e.getYoutubeVideoId());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    e.setId(rs.getInt(1));
                }
            }
        }

        // 🔗 Gestion ManyToMany avec Workout
        for (Workout w : e.getWorkouts()) {
            linkWorkout(e.getId(), w.getId());
        }
    }

    private void linkWorkout(int exerciseId, int workoutId) throws SQLException {
        String sql = "INSERT INTO workout_exercise (exercise_id, workout_id) VALUES (?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, exerciseId);
            stmt.setInt(2, workoutId);
            stmt.executeUpdate();
        }
    }

    private void unlinkWorkout(int exerciseId, int workoutId) throws SQLException {
        String sql = "DELETE FROM workout_exercise WHERE exercise_id = ? AND workout_id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, exerciseId);
            stmt.setInt(2, workoutId);
            stmt.executeUpdate();
        }
    }

    @Override
    public List<Exercise> read() throws SQLException {
        String sql = "SELECT * FROM exercise";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            List<Exercise> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    @Override
    public void update(Exercise e) throws SQLException {
        String sql = "UPDATE exercise SET nom = ?, type = ?, duree = ?, description = ?, sets = ?, reps = ?, image_name = ?, updated_at = ?, youtube_video_id = ? " +
                "WHERE id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, e.getNom());
            stmt.setString(2, e.getType());
            stmt.setObject(3, e.getDuree(), Types.INTEGER);
            stmt.setString(4, e.getDescription());
            stmt.setObject(5, e.getSets(), Types.INTEGER);
            stmt.setObject(6, e.getReps(), Types.INTEGER);
            stmt.setString(7, e.getImageName());
            stmt.setTimestamp(8, e.getUpdatedAt() != null ? Timestamp.valueOf(e.getUpdatedAt()) : null);
            stmt.setString(9, e.getYoutubeVideoId());
            stmt.setInt(10, e.getId());
            stmt.executeUpdate();
        }

        // 🔗 Synchronisation ManyToMany
        // Supprimer les liens existants
        String sqlDelete = "DELETE FROM workout_exercise WHERE exercise_id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sqlDelete)) {
            stmt.setInt(1, e.getId());
            stmt.executeUpdate();
        }
        // Ajouter les liens actuels
        for (Workout w : e.getWorkouts()) {
            linkWorkout(e.getId(), w.getId());
        }
    }

    @Override
    public void delete(Exercise e) throws SQLException {
        // 🔗 Supprimer les liens ManyToMany
        String sqlDeleteLinks = "DELETE FROM workout_exercise WHERE exercise_id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sqlDeleteLinks)) {
            stmt.setInt(1, e.getId());
            stmt.executeUpdate();
        }

        //  Supprimer l'exercice
        String sql = "DELETE FROM exercise WHERE id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, e.getId());
            stmt.executeUpdate();
        }
    }
}
