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

    @Override
    public void create(Questionnaire q) throws SQLException {
        createPrepared(q);
    }

    @Override
    public void createPrepared(Questionnaire q) throws SQLException {
        UUID newId = UUID.randomUUID();
        String sql = "INSERT INTO `questionnaire` (`id`, `user_id`, `coach_id`, `titre`, `type`, `note_globale`, "
                + "`satisfaction`, `intensite`, `exercices_compris`, `duree`, `ressenti_physique`, `stress`, "
                + "`motivation`, `progression`, `rapproche_objectifs`, `commentaire`, `options`, `user_name`, "
                + "`date_soumission`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(newId));
            if (q.getUser() != null && q.getUser().getId() != null)
                ps.setBytes(2, UuidUtil.toBytes16(q.getUser().getId()));
            else
                ps.setNull(2, Types.BINARY);
            if (q.getCoach() != null && q.getCoach().getId() != null)
                ps.setBytes(3, UuidUtil.toBytes16(q.getCoach().getId()));
            else
                ps.setNull(3, Types.BINARY);
            ps.setString(4, q.getTitre());
            ps.setString(5, q.getType() != null ? q.getType() : "template");
            ps.setObject(6, q.getNoteGlobale());
            ps.setObject(7, q.getSatisfaction());
            ps.setString(8, q.getIntensite());
            ps.setString(9, q.getExercicesCompris());
            ps.setString(10, q.getDuree());
            ps.setString(11, q.getRessentiPhysique());
            ps.setString(12, q.getStress());
            ps.setString(13, q.getMotivation());
            ps.setString(14, q.getProgression());
            ps.setObject(15, q.getRapprocheObjectifs());
            ps.setString(16, q.getCommentaire());
            ps.setString(17, q.getOptions());
            ps.setString(18, q.getUserName());
            ps.setObject(19, q.getDateSoumission() != null ? Timestamp.from(q.getDateSoumission()) : null);
            ps.executeUpdate();
            q.setId(newId);
        }
    }

    @Override
    public List<Questionnaire> read() throws SQLException {
        List<Questionnaire> list = new ArrayList<>();
        String sql = "SELECT * FROM `questionnaire`";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Questionnaire q = new Questionnaire();
                q.setId(UuidUtil.fromResultSet(rs, "id"));
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
        String sql = "UPDATE `questionnaire` SET `titre` = ?, `type` = ?, `options` = ?, "
                + "`exercices_compris` = ?, `date_soumission` = ? WHERE `id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, q.getTitre());
            ps.setString(2, q.getType() != null ? q.getType() : "template");
            ps.setString(3, q.getOptions());
            ps.setString(4, q.getExercicesCompris());
            ps.setObject(5, q.getDateSoumission() != null ? Timestamp.from(q.getDateSoumission()) : null);
            ps.setBytes(6, UuidUtil.toBytes16(q.getId()));
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Questionnaire q) throws SQLException {
        String sql = "DELETE FROM `questionnaire` WHERE `id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(q.getId()));
            ps.executeUpdate();
        }
    }
}
