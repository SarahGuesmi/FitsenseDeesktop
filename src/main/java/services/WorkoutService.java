package services;

import models.Exercise;
import models.ObjectifSportif;
import models.Workout;
import utils.DbConnection;
import utils.UuidUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        UUID uuid = UuidUtil.fromResultSet(rs, "id");
        w.setUuid(uuid);
        w.setId(uuid != null ? (int)(uuid.getLeastSignificantBits() & 0x7fffffffL) : 0);
        try { w.setCoachId(UuidUtil.fromResultSet(rs, "coach_id")); } catch (SQLException ignored) {}
        w.setNom(rs.getString("nom"));
        w.setNiveau(rs.getString("niveau"));
        w.setDuree(rs.getInt("duree"));
        w.setDescription(rs.getString("description"));
        w.setStatus(rs.getString("status"));
        return w;
    }

    public List<Workout> readWithExercises() throws SQLException {
        List<Workout> workouts = read();
        for (Workout w : workouts) {
            w.setExercises(exerciseService.findByWorkoutUuid(w.getUuid()));
            w.setObjectifs(findObjectifsByWorkoutId(w.getUuid()));
        }
        return workouts;
    }

    private List<ObjectifSportif> findObjectifsByWorkoutId(UUID workoutUuid) throws SQLException {
        String sql = "SELECT o.* FROM `objectif_sportif` o "
                + "INNER JOIN `workout_objectif` wo ON wo.`objectif_sportif_id` = o.`id` "
                + "WHERE wo.`workout_id` = ?";
        List<ObjectifSportif> list = new java.util.ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(workoutUuid));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID id = UuidUtil.fromResultSet(rs, "id");
                    String name = rs.getString("name");
                    list.add(new ObjectifSportif(id, name, null));
                }
            }
        } catch (SQLException e) {
            // table may not exist — return empty
        }
        return list;
    }

    public void syncObjectifs(UUID workoutUuid, List<String> objectiveNames) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM `workout_objectif` WHERE `workout_id` = ?")) {
            ps.setBytes(1, UuidUtil.toBytes16(workoutUuid));
            ps.executeUpdate();
        }
        if (objectiveNames == null || objectiveNames.isEmpty()) return;

        String findSql = "SELECT `id` FROM `objectif_sportif` WHERE `name` = ? LIMIT 1";
        String insertSql = "INSERT IGNORE INTO `workout_objectif` (`workout_id`, `objectif_sportif_id`) VALUES (?, ?)";
        try (PreparedStatement find = cnx.prepareStatement(findSql);
             PreparedStatement insert = cnx.prepareStatement(insertSql)) {
            for (String name : objectiveNames) {
                find.setString(1, name.trim());
                try (ResultSet rs = find.executeQuery()) {
                    if (rs.next()) {
                        UUID objId = UuidUtil.fromResultSet(rs, "id");
                        insert.setBytes(1, UuidUtil.toBytes16(workoutUuid));
                        insert.setBytes(2, UuidUtil.toBytes16(objId));
                        insert.executeUpdate();
                    }
                }
            }
        }
    }

    /** @deprecated use syncObjectifs(UUID, List) */
    public void syncObjectifs(int workoutId, List<String> objectiveNames) throws SQLException {
        // no-op — UUID version is used now
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
        if (workout.getUuid() == null) workout.setUuid(UUID.randomUUID());
        String sql = "INSERT INTO workout (id, nom, niveau, duree, description, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(workout.getUuid()));
            stmt.setString(2, workout.getNom());
            stmt.setString(3, workout.getNiveau());
            stmt.setObject(4, workout.getDuree(), Types.INTEGER);
            stmt.setString(5, workout.getDescription());
            stmt.setString(6, workout.getStatus());
            stmt.executeUpdate();
        }
        for (Exercise e : workout.getExercises()) {
            linkExercise(workout.getUuid(), e.getUuid());
        }
    }

    private void linkExercise(UUID workoutUuid, UUID exerciseUuid) throws SQLException {
        if (workoutUuid == null || exerciseUuid == null) return;
        String sql = "INSERT IGNORE INTO workout_exercise (workout_id, exercise_id) VALUES (?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(workoutUuid));
            stmt.setBytes(2, UuidUtil.toBytes16(exerciseUuid));
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
            stmt.setBytes(6, UuidUtil.toBytes16(workout.getUuid()));
            stmt.executeUpdate();
        }
        String sqlDelete = "DELETE FROM workout_exercise WHERE workout_id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sqlDelete)) {
            stmt.setBytes(1, UuidUtil.toBytes16(workout.getUuid()));
            stmt.executeUpdate();
        }
        for (Exercise e : workout.getExercises()) {
            linkExercise(workout.getUuid(), e.getUuid());
        }
    }

    @Override
    public void delete(Workout workout) throws SQLException {
        String sqlDeleteLinks = "DELETE FROM workout_exercise WHERE workout_id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sqlDeleteLinks)) {
            stmt.setBytes(1, UuidUtil.toBytes16(workout.getUuid()));
            stmt.executeUpdate();
        }
        String sql = "DELETE FROM workout WHERE id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(workout.getUuid()));
            stmt.executeUpdate();
        }
    }
}
