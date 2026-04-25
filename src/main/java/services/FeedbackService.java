package services;

import models.Questionnaire;
import utils.UuidUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FeedbackService implements CRUD<Questionnaire> {

    private final Connection connection;

    public FeedbackService(Connection connection) {
        this.connection = connection;
    }

    private static Questionnaire mapRow(ResultSet rs) throws SQLException {
        Questionnaire q = new Questionnaire();
        UUID uuid = UuidUtil.fromResultSet(rs, "id");
        q.setId(uuid != null ? Math.abs(uuid.hashCode()) : 0);
        q.setTitre(rs.getString("titre"));
        q.setType(rs.getString("type"));
        q.setOptions(rs.getString("options"));
        q.setExercicesCompris(rs.getString("exercices_compris"));
        q.setCommentaire(rs.getString("commentaire"));
        Timestamp ts = rs.getTimestamp("date_soumission");
        if (ts != null) q.setDateSoumission(ts.toInstant());
        return q;
    }

    @Override
    public void create(Questionnaire q) throws SQLException { createPrepared(q); }

    @Override
    public void createPrepared(Questionnaire q) throws SQLException {
        UUID id = UUID.randomUUID();
        String sql = "INSERT INTO questionnaire (id, user_id, coach_id, titre, type, options, exercices_compris, date_soumission) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(id));
            ps.setBytes(2, q.getUser() != null ? UuidUtil.toBytes16(q.getUser().getId()) : null);
            ps.setBytes(3, q.getCoach() != null ? UuidUtil.toBytes16(q.getCoach().getId()) : null);
            ps.setString(4, q.getTitre());
            ps.setString(5, q.getType() != null ? q.getType() : "template");
            ps.setString(6, q.getOptions());
            ps.setString(7, q.getExercicesCompris());
            ps.setObject(8, q.getDateSoumission() != null ? Timestamp.from(q.getDateSoumission()) : null);
            ps.executeUpdate();
        }
        // Link workouts
        for (models.Workout w : q.getWorkouts()) {
            if (w.getUuid() != null) {
                try (PreparedStatement ps2 = connection.prepareStatement(
                        "INSERT IGNORE INTO questionnaire_workout (questionnaire_id, workout_id) VALUES (?,?)")) {
                    ps2.setBytes(1, UuidUtil.toBytes16(id));
                    ps2.setBytes(2, UuidUtil.toBytes16(w.getUuid()));
                    ps2.executeUpdate();
                }
            }
        }
        q.setId(Math.abs(id.hashCode()));
    }

    @Override
    public List<Questionnaire> read() throws SQLException {
        List<Questionnaire> list = new ArrayList<>();
        String sql = "SELECT * FROM questionnaire WHERE type='template'";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public void update(Questionnaire q) throws SQLException {
        String sql = "UPDATE questionnaire SET options=?, exercices_compris=? WHERE titre=? AND type='template'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, q.getOptions());
            ps.setString(2, q.getExercicesCompris());
            ps.setString(3, q.getTitre());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Questionnaire q) throws SQLException {
        String sql = "DELETE FROM questionnaire WHERE titre=? AND type='template'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, q.getTitre());
            ps.executeUpdate();
        }
    }

    public Questionnaire findTemplateByWorkoutUuid(UUID workoutUuid) throws SQLException {
        String sql = "SELECT q.* FROM questionnaire q " +
                "INNER JOIN questionnaire_workout qw ON qw.questionnaire_id = q.id " +
                "WHERE qw.workout_id = ? AND q.type = 'template' " +
                "ORDER BY q.date_soumission DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(workoutUuid));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public Questionnaire findTemplateByWorkoutId(Integer workoutId) throws SQLException {
        // Fallback: return first available template
        String sql = "SELECT * FROM questionnaire WHERE type='template' ORDER BY date_soumission DESC LIMIT 1";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }
}
