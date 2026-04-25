package services;

import models.Exercise;
import models.Workout;
import utils.DbConnection;
import utils.UuidUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExerciseService implements CRUD<Exercise> {

    private final Connection cnx;

    public ExerciseService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    private Exercise mapRow(ResultSet rs) throws SQLException {
        Exercise e = new Exercise();
        UUID uuid = UuidUtil.fromResultSet(rs, "id");
        e.setUuid(uuid);
        e.setId(uuid != null ? Math.abs(uuid.hashCode()) : 0);
        e.setNom(rs.getString("nom"));
        e.setType(rs.getString("type"));
        e.setDuree(rs.getObject("duree") != null ? rs.getInt("duree") : null);
        e.setDescription(rs.getString("description"));
        e.setSets(rs.getObject("sets") != null ? rs.getInt("sets") : null);
        e.setReps(rs.getObject("reps") != null ? rs.getInt("reps") : null);
        e.setImageName(rs.getString("image_name"));
        Timestamp ts = rs.getTimestamp("updated_at");
        if (ts != null) e.setUpdatedAt(ts.toLocalDateTime());
        e.setYoutubeVideoId(rs.getString("youtube_video_id"));
        return e;
    }

    public List<Exercise> findByWorkoutUuid(UUID workoutUuid) throws SQLException {
        String sql = "SELECT e.* FROM exercise e JOIN workout_exercise we ON e.id = we.exercise_id WHERE we.workout_id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(workoutUuid));
            try (ResultSet rs = stmt.executeQuery()) {
                List<Exercise> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    public List<Exercise> findByWorkoutId(int workoutId) throws SQLException {
        return new ArrayList<>(); // use findByWorkoutUuid instead
    }

    @Override
    public List<Exercise> read() throws SQLException {
        String sql = "SELECT * FROM exercise ORDER BY nom";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            List<Exercise> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        }
    }

    @Override
    public void create(Exercise e) throws SQLException { createPrepared(e); }

    @Override
    public void createPrepared(Exercise e) throws SQLException {
        if (e.getUuid() == null) e.setUuid(UUID.randomUUID());
        String sql = "INSERT INTO exercise (id, nom, type, duree, description, sets, reps, image_name, updated_at, youtube_video_id) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(e.getUuid()));
            stmt.setString(2, e.getNom());
            stmt.setString(3, e.getType());
            stmt.setObject(4, e.getDuree(), Types.INTEGER);
            stmt.setString(5, e.getDescription());
            stmt.setObject(6, e.getSets(), Types.INTEGER);
            stmt.setObject(7, e.getReps(), Types.INTEGER);
            stmt.setString(8, e.getImageName());
            stmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(10, e.getYoutubeVideoId());
            stmt.executeUpdate();
        }
        for (Workout w : e.getWorkouts()) {
            if (w.getUuid() != null) linkWorkout(e.getUuid(), w.getUuid());
        }
    }

    private void linkWorkout(UUID exerciseId, UUID workoutId) throws SQLException {
        String sql = "INSERT IGNORE INTO workout_exercise (exercise_id, workout_id) VALUES (?,?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(exerciseId));
            stmt.setBytes(2, UuidUtil.toBytes16(workoutId));
            stmt.executeUpdate();
        }
    }

    @Override
    public void update(Exercise e) throws SQLException {
        String sql = "UPDATE exercise SET nom=?, type=?, duree=?, description=?, sets=?, reps=?, image_name=?, updated_at=?, youtube_video_id=? WHERE id=?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, e.getNom());
            stmt.setString(2, e.getType());
            stmt.setObject(3, e.getDuree(), Types.INTEGER);
            stmt.setString(4, e.getDescription());
            stmt.setObject(5, e.getSets(), Types.INTEGER);
            stmt.setObject(6, e.getReps(), Types.INTEGER);
            stmt.setString(7, e.getImageName());
            stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(9, e.getYoutubeVideoId());
            stmt.setBytes(10, UuidUtil.toBytes16(e.getUuid()));
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(Exercise e) throws SQLException {
        try (PreparedStatement stmt = cnx.prepareStatement("DELETE FROM workout_exercise WHERE exercise_id=?")) {
            stmt.setBytes(1, UuidUtil.toBytes16(e.getUuid()));
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = cnx.prepareStatement("DELETE FROM exercise WHERE id=?")) {
            stmt.setBytes(1, UuidUtil.toBytes16(e.getUuid()));
            stmt.executeUpdate();
        }
    }
}
