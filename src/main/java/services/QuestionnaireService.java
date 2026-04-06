package services;

import models.Questionnaire;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionnaireService implements CRUD<Questionnaire> {

    private final Connection connection;

    public QuestionnaireService(Connection connection) {
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
            ps.setObject(1, q.getUser() != null ? q.getUser().getId() : null);
            ps.setObject(2, q.getCoach() != null ? q.getCoach().getId() : null);
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
        String sql = "UPDATE questionnaire SET titre=?, type=?, note_globale=?, satisfaction=?, intensite=?, " +
                "exercices_compris=?, duree=?, ressenti_physique=?, stress=?, motivation=?, progression=?, " +
                "rapproche_objectifs=?, commentaire=?, options=?, user_name=?, date_soumission=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, q.getTitre());
            ps.setString(2, q.getType());
            ps.setObject(3, q.getNoteGlobale());
            ps.setObject(4, q.getSatisfaction());
            ps.setString(5, q.getIntensite());
            ps.setString(6, q.getExercicesCompris());
            ps.setString(7, q.getDuree());
            ps.setString(8, q.getRessentiPhysique());
            ps.setString(9, q.getStress());
            ps.setString(10, q.getMotivation());
            ps.setString(11, q.getProgression());
            ps.setObject(12, q.getRapprocheObjectifs());
            ps.setString(13, q.getCommentaire());
            ps.setString(14, q.getOptions());
            ps.setString(15, q.getUserName());
            ps.setObject(16, q.getDateSoumission() != null ? Timestamp.from(q.getDateSoumission()) : null);
            ps.setInt(17, q.getId());
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
}
