package services;

import models.MentalHealthAssessmentSubmission;
import models.RecommendedExercise;
import models.User;
import utils.DbConnection;
import utils.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JDBC persistence for coach-visible submission rows and recommended exercises.
 */
public final class MentalHealthSubmissionRepository {

    private final Connection cnx;

    public MentalHealthSubmissionRepository() {
        this.cnx = DbConnection.getInstance().getCnx();
    }

    public List<MentalHealthAssessmentSubmission> loadAllWithExercises() throws SQLException {
        if (cnx == null) {
            return List.of();
        }
        List<MentalHealthAssessmentSubmission> list = new ArrayList<>();
        String sql = """
                SELECT id, evaluation_id, user_id, user_full_name, user_email, tested_at,
                       stress, sleep, mood, motivation, mental_tired, score, status, member_notes,
                       coach_test_title, coach_recommendation, recommendation_general_note
                FROM mental_health_submission
                ORDER BY tested_at DESC
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapSubmission(rs));
            }
        }
        if (list.isEmpty()) {
            return list;
        }
        Map<UUID, MentalHealthAssessmentSubmission> byId = new HashMap<>();
        for (MentalHealthAssessmentSubmission s : list) {
            byId.put(s.getId(), s);
        }
        String placeholders = String.join(",", byId.keySet().stream().map(u -> "?").toList());
        String exSql = """
                SELECT submission_id, sort_order, name, duration_minutes, description
                FROM mental_health_recommended_exercise
                WHERE submission_id IN (""" + placeholders + ") ORDER BY submission_id, sort_order ASC";
        try (PreparedStatement ps = cnx.prepareStatement(exSql)) {
            int i = 1;
            for (UUID sid : byId.keySet()) {
                ps.setBytes(i++, UuidUtil.toBytes16(sid));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID subId = UuidUtil.fromResultSet(rs, "submission_id");
                    MentalHealthAssessmentSubmission s = byId.get(subId);
                    if (s == null) {
                        continue;
                    }
                    RecommendedExercise e = new RecommendedExercise(
                            rs.getString("name"),
                            rs.getString("duration_minutes"),
                            rs.getString("description"));
                    s.getRecommendedExercises().add(e);
                }
            }
        }
        return list;
    }

    private static MentalHealthAssessmentSubmission mapSubmission(ResultSet rs) throws SQLException {
        MentalHealthAssessmentSubmission s = new MentalHealthAssessmentSubmission(UuidUtil.fromResultSet(rs, "id"));
        s.setEvaluationId(UuidUtil.fromResultSet(rs, "evaluation_id"));
        s.setUserId(UuidUtil.fromResultSet(rs, "user_id"));
        s.setUserFullName(rs.getString("user_full_name"));
        s.setUserEmail(rs.getString("user_email"));
        Timestamp ts = rs.getTimestamp("tested_at");
        s.setTestedAt(ts == null ? null : ts.toLocalDateTime());
        s.setStress(rs.getInt("stress"));
        s.setSleep(rs.getInt("sleep"));
        s.setMood(rs.getInt("mood"));
        s.setMotivation(rs.getInt("motivation"));
        s.setMentalTired(rs.getInt("mental_tired"));
        s.setScore(rs.getInt("score"));
        s.setStatus(rs.getString("status"));
        s.setMemberNotes(rs.getString("member_notes"));
        s.setCoachTestTitle(rs.getString("coach_test_title"));
        s.setCoachRecommendation(rs.getString("coach_recommendation"));
        s.setRecommendationGeneralNote(rs.getString("recommendation_general_note"));
        return s;
    }

    public void upsertMemberSnapshot(User user, MentalHealthAssessmentSubmission s) throws SQLException {
        if (cnx == null || user == null || s.getEvaluationId() == null) {
            throw new SQLException("Missing connection, user, or evaluation id");
        }
        if (submissionExistsForEvaluation(s.getEvaluationId())) {
            try (PreparedStatement ps = cnx.prepareStatement("""
                    UPDATE mental_health_submission SET
                        user_id = ?, user_full_name = ?, user_email = ?, tested_at = ?,
                        stress = ?, sleep = ?, mood = ?, motivation = ?, mental_tired = ?,
                        score = ?, status = ?, member_notes = ?, coach_test_title = ?
                    WHERE evaluation_id = ?
                    """)) {
                int i = 1;
                ps.setBytes(i++, UuidUtil.toBytes16(user.getId()));
                ps.setString(i++, s.getUserFullName());
                ps.setString(i++, s.getUserEmail());
                setTimestamp(ps, i++, s.getTestedAt());
                ps.setInt(i++, s.getStress());
                ps.setInt(i++, s.getSleep());
                ps.setInt(i++, s.getMood());
                ps.setInt(i++, s.getMotivation());
                ps.setInt(i++, s.getMentalTired());
                ps.setInt(i++, s.getScore());
                ps.setString(i++, s.getStatus());
                ps.setString(i++, s.getMemberNotes());
                ps.setString(i++, s.getCoachTestTitle());
                ps.setBytes(i, UuidUtil.toBytes16(s.getEvaluationId()));
                ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = cnx.prepareStatement("""
                    INSERT INTO mental_health_submission (
                        id, evaluation_id, user_id, user_full_name, user_email, tested_at,
                        stress, sleep, mood, motivation, mental_tired, score, status, member_notes, coach_test_title
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """)) {
                int i = 1;
                ps.setBytes(i++, UuidUtil.toBytes16(s.getId()));
                ps.setBytes(i++, UuidUtil.toBytes16(s.getEvaluationId()));
                ps.setBytes(i++, UuidUtil.toBytes16(user.getId()));
                ps.setString(i++, s.getUserFullName());
                ps.setString(i++, s.getUserEmail());
                setTimestamp(ps, i++, s.getTestedAt());
                ps.setInt(i++, s.getStress());
                ps.setInt(i++, s.getSleep());
                ps.setInt(i++, s.getMood());
                ps.setInt(i++, s.getMotivation());
                ps.setInt(i++, s.getMentalTired());
                ps.setInt(i++, s.getScore());
                ps.setString(i++, s.getStatus());
                ps.setString(i++, s.getMemberNotes());
                ps.setString(i, s.getCoachTestTitle());
                ps.executeUpdate();
            }
        }
    }

    private boolean submissionExistsForEvaluation(UUID evaluationId) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT 1 FROM mental_health_submission WHERE evaluation_id = ? LIMIT 1")) {
            ps.setBytes(1, UuidUtil.toBytes16(evaluationId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public UUID findSubmissionIdByEvaluationId(UUID evaluationId) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT id FROM mental_health_submission WHERE evaluation_id = ? LIMIT 1")) {
            ps.setBytes(1, UuidUtil.toBytes16(evaluationId));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UuidUtil.fromResultSet(rs, "id");
                }
            }
        }
        return null;
    }

    public void persistCoachRecommendation(MentalHealthAssessmentSubmission s) throws SQLException {
        if (cnx == null) {
            throw new SQLException("No connection");
        }
        cnx.setAutoCommit(false);
        try {
            try (PreparedStatement ps = cnx.prepareStatement("""
                    UPDATE mental_health_submission SET
                        coach_recommendation = ?, recommendation_general_note = ?
                    WHERE id = ?
                    """)) {
                ps.setString(1, s.getCoachRecommendation());
                ps.setString(2, s.getRecommendationGeneralNote());
                ps.setBytes(3, UuidUtil.toBytes16(s.getId()));
                ps.executeUpdate();
            }
            try (PreparedStatement del = cnx.prepareStatement(
                    "DELETE FROM mental_health_recommended_exercise WHERE submission_id = ?")) {
                del.setBytes(1, UuidUtil.toBytes16(s.getId()));
                del.executeUpdate();
            }
            try (PreparedStatement ins = cnx.prepareStatement("""
                    INSERT INTO mental_health_recommended_exercise (id, submission_id, sort_order, name, duration_minutes, description)
                    VALUES (?,?,?,?,?,?)
                    """)) {
                int order = 0;
                for (RecommendedExercise e : s.getRecommendedExercises()) {
                    if (e == null || !e.hasContent()) {
                        continue;
                    }
                    ins.setBytes(1, UuidUtil.toBytes16(UUID.randomUUID()));
                    ins.setBytes(2, UuidUtil.toBytes16(s.getId()));
                    ins.setInt(3, order++);
                    ins.setString(4, e.getName());
                    ins.setString(5, e.getDurationMinutes());
                    ins.setString(6, e.getDescription());
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    public void clearCoachRecommendationFields(UUID submissionId) throws SQLException {
        if (cnx == null) {
            throw new SQLException("No connection");
        }
        cnx.setAutoCommit(false);
        try {
            try (PreparedStatement ps = cnx.prepareStatement("""
                    UPDATE mental_health_submission SET coach_recommendation = NULL, recommendation_general_note = NULL
                    WHERE id = ?
                    """)) {
                ps.setBytes(1, UuidUtil.toBytes16(submissionId));
                ps.executeUpdate();
            }
            try (PreparedStatement del = cnx.prepareStatement(
                    "DELETE FROM mental_health_recommended_exercise WHERE submission_id = ?")) {
                del.setBytes(1, UuidUtil.toBytes16(submissionId));
                del.executeUpdate();
            }
            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    private static void setTimestamp(PreparedStatement ps, int idx, LocalDateTime ldt) throws SQLException {
        if (ldt == null) {
            ps.setTimestamp(idx, null);
        } else {
            ps.setTimestamp(idx, Timestamp.valueOf(ldt));
        }
    }
}
