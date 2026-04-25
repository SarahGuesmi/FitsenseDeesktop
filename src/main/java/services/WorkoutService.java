package services;

import models.Workout;
import utils.DbConnection;
import utils.UuidUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
public class WorkoutService implements CRUD<Workout> {

    private final Connection cnx;

    public WorkoutService() {
        this.cnx = DbConnection.getInstance().getCnx();
    }

    private static Workout mapRow(ResultSet rs) throws SQLException {
        Workout w = new Workout();
        UUID uuid = UuidUtil.fromResultSet(rs, "id");
        w.setUuid(uuid);
        // Use hashCode of UUID as integer id for legacy references
        w.setId(uuid != null ? Math.abs(uuid.hashCode()) : 0);
        w.setNom(rs.getString("nom"));
        w.setNiveau(rs.getString("niveau"));
        w.setDuree(rs.getObject("duree", Integer.class));
        w.setDescription(rs.getString("description"));
        w.setStatus(rs.getString("status"));
        return w;
    }

    @Override
    public List<Workout> read() throws SQLException {
        List<Workout> list = new ArrayList<>();
        String sql = "SELECT * FROM workout";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Workout> readWithExercises() throws SQLException {
        List<Workout> workouts = read();
        ExerciseService exerciseService = new ExerciseService();
        for (Workout w : workouts) {
            if (w.getUuid() != null)
                w.setExercises(exerciseService.findByWorkoutUuid(w.getUuid()));
        }
        return workouts;
    }

    public List<Workout> findByObjectiveNames(List<String> names) throws SQLException {
        return readWithExercises();
    }

    public void syncObjectifs(Integer workoutId, List<String> objectives) throws SQLException {
        // stub
    }

    @Override
    public void create(Workout w) throws SQLException { createPrepared(w); }

    @Override
    public void createPrepared(Workout w) throws SQLException {
        if (w.getUuid() == null) w.setUuid(UUID.randomUUID());
        String sql = "INSERT INTO workout (id, nom, niveau, duree, description, status) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(w.getUuid()));
            ps.setString(2, w.getNom());
            ps.setString(3, w.getNiveau());
            ps.setObject(4, w.getDuree());
            ps.setString(5, w.getDescription());
            ps.setString(6, w.getStatus());
            ps.executeUpdate();
        }
        // Link exercises
        for (models.Exercise e : w.getExercises()) {
            if (e.getUuid() != null) {
                try (PreparedStatement ps2 = cnx.prepareStatement(
                        "INSERT IGNORE INTO workout_exercise (workout_id, exercise_id) VALUES (?,?)")) {
                    ps2.setBytes(1, UuidUtil.toBytes16(w.getUuid()));
                    ps2.setBytes(2, UuidUtil.toBytes16(e.getUuid()));
                    ps2.executeUpdate();
                }
            }
        }
    }

    @Override
    public void update(Workout w) throws SQLException {
        String sql = "UPDATE workout SET nom=?, niveau=?, duree=?, description=?, status=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, w.getNom());
            ps.setString(2, w.getNiveau());
            ps.setObject(3, w.getDuree());
            ps.setString(4, w.getDescription());
            ps.setString(5, w.getStatus());
            ps.setBytes(6, UuidUtil.toBytes16(w.getUuid()));
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Workout w) throws SQLException {
        String sql = "DELETE FROM workout WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(w.getUuid()));
            ps.executeUpdate();
        }
    }
}
