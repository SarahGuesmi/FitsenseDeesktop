package services;

import models.Questionnaire;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeedbackService implements CRUD<Questionnaire> {

    private final Connection connection;

    public FeedbackService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void create(Questionnaire q) throws SQLException {
        String sql = "INSERT INTO questionnaire (user_id, coach_id, titre, type, note_globale, satisfaction, " +
                "intensite, exercices_compris, duree, ressenti_physique, stress, motivation, progression, " +
                "rapproche_objectifs, commentaire, options, user_name, date_soumission) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    @Override
    public void createPrepared(Questionnaire q) throws SQLException {
        String sql = "INSERT INTO questionnaire (user_id, coach_id, titre, type, note_globale, satisfaction, " +
                "intensite, exercices_compris, duree, ressenti_physique, stress, motivation, progression, " +
                "rapproche_objectifs, commentaire, options, user_name, date_soumission) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            // user_id and coach_id are int in DB
            if (q.getUser() != null)
                ps.setInt(1, (int) q.getUser().getId().getLeastSignificantBits());
            else
                ps.setNull(1, Types.INTEGER);
            if (q.getCoach() != null)
                ps.setInt(2, (int) q.getCoach().getId().getLeastSignificantBits());
            else
                ps.setNull(2, Types.INTEGER);
            ps.setString(3, q.getTitre());
            ps.setString(4, q.getType());
            ps.setObject(5, q.getNoteGlobale());
            ps.setObject(6, q.getSatisfaction());
            ps.setString(7, q.getIntensite());
            ps.setString(8, q.getExercicesCompris());
            ps.setString(9, q.getDuree());
            ps.setString(10, q.getRessentiPhysique());
            ps.setString(11, q.getStress());
            ps.setString(12, q.getMotivation());
            ps.setString(13, q.getProgression());
            ps.setObject(14, q.getRapprocheObjectifs());
            ps.setString(15, q.getCommentaire());
            ps.setString(16, q.getOptions());
            ps.setString(17, q.getUserName());
            ps.setObject(18, q.getDateSoumission() != null ? Timestamp.from(q.getDateSoumission()) : null);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Questionnaire> read() throws SQLException {
        List<Questionnaire> list = new ArrayList<>();
        String sql = "SELECT * FROM questionnaire";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Questionnaire q = new Questionnaire();
                q.setId(rs.getObject("id", Integer.class));
                q.setTitre(rs.getString("titre"));
                q.setType(rs.getString("type"));
                q.setNoteGlobale(rs.getObject("note_globale", Integer.class));
                q.setSatisfaction(rs.getObject("satisfaction", Integer.class));
                q.setIntensite(rs.getString("intensite"));
                q.setExercicesCompris(rs.getString("exercices_compris"));
                q.setDuree(rs.getString("duree"));
                q.setRessentiPhysique(rs.getString("ressenti_physique"));
                q.setStress(rs.getString("stress"));
                q.setMotivation(rs.getString("motivation"));
                q.setProgression(rs.getString("progression"));
                q.setRapprocheObjectifs(rs.getObject("rapproche_objectifs", Integer.class));
                q.setCommentaire(rs.getString("commentaire"));
                q.setOptions(rs.getString("options"));
                q.setUserName(rs.getString("user_name"));
                Timestamp ts = rs.getTimestamp("date_soumission");
                if (ts != null) q.setDateSoumission(ts.toInstant());
                list.add(q);
            }
        }
        return list;
    }

    @Override
    public void update(Questionnaire q) throws SQLException {
        String sql = "UPDATE questionnaire SET titre=?, type=?, options=?, exercices_compris=?, date_soumission=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, q.getTitre());
            ps.setString(2, q.getType() != null ? q.getType() : "template");
            ps.setString(3, q.getOptions());
            ps.setString(4, q.getExercicesCompris());
            ps.setObject(5, q.getDateSoumission() != null ? Timestamp.from(q.getDateSoumission()) : null);
            ps.setInt(6, q.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Questionnaire q) throws SQLException {
        String sql = "DELETE FROM questionnaire WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, q.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Returns all responses (type='response') with user name joined from user table.
     */
    public List<Questionnaire> readResponses() throws SQLException {
        List<Questionnaire> list = new ArrayList<>();
        String sql = "SELECT q.*, CONCAT(COALESCE(u.firstname,''), ' ', COALESCE(u.lastname,'')) AS full_name " +
                "FROM questionnaire q LEFT JOIN user u ON u.id = q.user_id " +
                "WHERE q.type = 'response' ORDER BY q.date_soumission DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Questionnaire q = new Questionnaire();
                q.setId(rs.getObject("id", Integer.class));
                q.setTitre(rs.getString("titre"));
                q.setType(rs.getString("type"));
                q.setExercicesCompris(rs.getString("exercices_compris"));
                q.setCommentaire(rs.getString("commentaire"));
                q.setOptions(rs.getString("options"));
                String fullName = rs.getString("full_name");
                q.setUserName(fullName != null ? fullName.trim() : null);
                Timestamp ts = rs.getTimestamp("date_soumission");
                if (ts != null) q.setDateSoumission(ts.toInstant());
                list.add(q);
            }
        }
        return list;
    }

    /**
     * Finds the coach-created template (type='template') linked to a given workout.
     */
    public Questionnaire findTemplateByWorkoutId(Integer workoutId) throws SQLException {
        String sql = "SELECT q.* FROM questionnaire q " +
                "INNER JOIN questionnaire_workout qw ON qw.questionnaire_id = q.id " +
                "WHERE qw.workout_id = ? AND q.type = 'template' " +
                "ORDER BY q.id DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, workoutId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Questionnaire q = new Questionnaire();
                    q.setId(rs.getObject("id", Integer.class));
                    q.setTitre(rs.getString("titre"));
                    q.setType(rs.getString("type"));
                    q.setOptions(rs.getString("options"));
                    q.setIntensite(rs.getString("intensite"));
                    q.setDuree(rs.getString("duree"));
                    q.setCommentaire(rs.getString("commentaire"));
                    return q;
                }
            }
        }
        return null;
    }
}
