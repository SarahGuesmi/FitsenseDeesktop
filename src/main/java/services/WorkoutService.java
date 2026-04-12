package services;

import models.Exercise;
import models.ObjectifSportif;
import models.Workout;
import utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkoutService implements CRUD<Workout> {

    private final Connection cnx;
    private final ExerciseService exerciseService;

    public WorkoutService() {
        cnx = DbConnection.getInstance().getCnx();
        exerciseService = new ExerciseService();
    }

    //  Mapping ResultSet → Workout
    private Workout mapRow(ResultSet rs) throws SQLException {
        Workout w = new Workout();
        w.setId(rs.getInt("id"));
        w.setNom(rs.getString("nom"));
        w.setNiveau(rs.getString("niveau"));
        w.setDuree(rs.getInt("duree"));
        w.setDescription(rs.getString("description"));
        w.setStatus(rs.getString("status"));
        return w;
    }

    // Find workouts with their exercises loaded
    public List<Workout> readWithExercises() throws SQLException {
        List<Workout> workouts = read();
        for (Workout w : workouts) {
            w.setExercises(exerciseService.findByWorkoutId(w.getId()));
            w.setObjectifs(findObjectifsByWorkoutId(w.getId()));
        }
        return workouts;
    }

    /** Load objectifs linked to a workout via workout_objectif */
    private List<ObjectifSportif> findObjectifsByWorkoutId(int workoutId) throws SQLException {
        String sql = "SELECT o.* FROM `objectif_sportif` o "
                + "INNER JOIN `workout_objectif` wo ON wo.`objectif_sportif_id` = o.`id` "
                + "WHERE wo.`workout_id` = ?";
        List<ObjectifSportif> list = new java.util.ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, workoutId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int rawId = rs.getInt("id");
                    String name = rs.getString("name");
                    list.add(new ObjectifSportif(new java.util.UUID(0L, rawId), name, null));
                }
            }
        } catch (SQLException e) {
            // table may not exist or column names differ — return empty
        }
        return list;
    }

    /** Sync workout_objectif junction table for a workout */
    public void syncObjectifs(int workoutId, List<String> objectiveNames) throws SQLException {
        // Delete existing links
        try (PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM `workout_objectif` WHERE `workout_id` = ?")) {
            ps.setInt(1, workoutId);
            ps.executeUpdate();
        }
        if (objectiveNames == null || objectiveNames.isEmpty()) return;

        // Find objectif_sportif ids by name (distinct names from the table)
        String findSql = "SELECT `id` FROM `objectif_sportif` WHERE `name` = ? LIMIT 1";
        String insertSql = "INSERT IGNORE INTO `workout_objectif` (`workout_id`, `objectif_sportif_id`) VALUES (?, ?)";
        try (PreparedStatement find = cnx.prepareStatement(findSql);
             PreparedStatement insert = cnx.prepareStatement(insertSql)) {
            for (String name : objectiveNames) {
                find.setString(1, name.trim());
                try (ResultSet rs = find.executeQuery()) {
                    if (rs.next()) {
                        insert.setInt(1, workoutId);
                        insert.setInt(2, rs.getInt("id"));
                        insert.executeUpdate();
                    }
                }
            }
        }
    }
    public List<Workout> findByObjectiveNames(List<String> objectiveNames) throws SQLException {
        if (objectiveNames == null || objectiveNames.isEmpty()) return readWithExercises();
        List<Workout> all = readWithExercises();
        return all.stream().filter(w -> {
            for (ObjectifSportif o : w.getObjectifs()) {
                if (o.getName() != null) {
                    for (String name : objectiveNames) {
                        if (o.getName().equalsIgnoreCase(name.trim())) return true;
                    }
                }
            }
            return false;
        }).toList();
    }

    // ================= CRUD =================

    @Override
    public void create(Workout workout) throws SQLException {
        createPrepared(workout);
    }

    @Override
    public void createPrepared(Workout workout) throws SQLException {
        String sql = "INSERT INTO workout (nom, niveau, duree, description, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, workout.getNom());
            stmt.setString(2, workout.getNiveau());
            stmt.setObject(3, workout.getDuree(), Types.INTEGER);
            stmt.setString(4, workout.getDescription());
            stmt.setString(5, workout.getStatus());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    workout.setId(rs.getInt(1));
                }
            }
        }

        //Gestion ManyToMany avec Exercise
        for (Exercise e : workout.getExercises()) {
            linkExercise(workout.getId(), e.getId());
        }
    }

    private void linkExercise(int workoutId, int exerciseId) throws SQLException {
        String sql = "INSERT IGNORE INTO workout_exercise (workout_id, exercise_id) VALUES (?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, workoutId);
            stmt.setInt(2, exerciseId);
            stmt.executeUpdate();
        }
    }

    @Override
    public List<Workout> read() throws SQLException {
        String sql = "SELECT * FROM workout";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<Workout> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    @Override
    public void update(Workout workout) throws SQLException {
        String sql = "UPDATE workout SET nom = ?, niveau = ?, duree = ?, description = ?, status = ? WHERE id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, workout.getNom());
            stmt.setString(2, workout.getNiveau());
            stmt.setObject(3, workout.getDuree(), Types.INTEGER);
            stmt.setString(4, workout.getDescription());
            stmt.setString(5, workout.getStatus());
            stmt.setInt(6, workout.getId());
            stmt.executeUpdate();
        }

        // Synchronisation ManyToMany
        String sqlDelete = "DELETE FROM workout_exercise WHERE workout_id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sqlDelete)) {
            stmt.setInt(1, workout.getId());
            stmt.executeUpdate();
        }
        for (Exercise e : workout.getExercises()) {
            linkExercise(workout.getId(), e.getId());
        }
    }

    @Override
    public void delete(Workout workout) throws SQLException {
        //  Supprimer les liens ManyToMany
        String sqlDeleteLinks = "DELETE FROM workout_exercise WHERE workout_id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sqlDeleteLinks)) {
            stmt.setInt(1, workout.getId());
            stmt.executeUpdate();
        }

        String sql = "DELETE FROM workout WHERE id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, workout.getId());
            stmt.executeUpdate();
        }
    }
}
