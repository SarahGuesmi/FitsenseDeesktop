package services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import models.MentalHealthEvaluation;
import utils.DbConnection;
import utils.UuidUtil;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC persistence for member mental health evaluations (check-ins).
 */
public final class MentalHealthEvaluationRepository {

    private static final Gson GSON = new Gson();
    private static final Type INT_LIST = new TypeToken<List<Integer>>() { }.getType();
    private static final Type STRING_LIST = new TypeToken<List<String>>() { }.getType();

    private final Connection cnx;

    public MentalHealthEvaluationRepository() {
        this.cnx = DbConnection.getInstance().getCnx();
    }

    public List<MentalHealthEvaluation> findByUserId(UUID userId) throws SQLException {
        if (cnx == null || userId == null) {
            return List.of();
        }
        List<MentalHealthEvaluation> out = new ArrayList<>();
        String sql = """
                SELECT id, user_id, coach_test_id, coach_test_title, tested_at, mood, stress, sleep, motivation,
                       mental_tired, score, status, member_notes, question_scores_json, question_prompts_json
                FROM mental_health_evaluation
                WHERE user_id = ?
                ORDER BY tested_at DESC
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(userId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
        }
        return out;
    }

    public void upsert(MentalHealthEvaluation ev, UUID userId) throws SQLException {
        if (cnx == null || userId == null || ev == null) {
            throw new SQLException("Missing connection, user, or evaluation");
        }
        ev.setUserId(userId);
        String scoresJson = GSON.toJson(ev.getQuestionScores());
        String promptsJson = GSON.toJson(new ArrayList<>(ev.getQuestionPrompts()));
        String sql = """
                INSERT INTO mental_health_evaluation (
                    id, user_id, coach_test_id, coach_test_title, tested_at, mood, stress, sleep, motivation,
                    mental_tired, score, status, member_notes, question_scores_json, question_prompts_json
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
                    user_id = VALUES(user_id),
                    coach_test_id = VALUES(coach_test_id),
                    coach_test_title = VALUES(coach_test_title),
                    tested_at = VALUES(tested_at),
                    mood = VALUES(mood),
                    stress = VALUES(stress),
                    sleep = VALUES(sleep),
                    motivation = VALUES(motivation),
                    mental_tired = VALUES(mental_tired),
                    score = VALUES(score),
                    status = VALUES(status),
                    member_notes = VALUES(member_notes),
                    question_scores_json = VALUES(question_scores_json),
                    question_prompts_json = VALUES(question_prompts_json)
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            int i = 1;
            ps.setBytes(i++, UuidUtil.toBytes16(ev.getId()));
            ps.setBytes(i++, UuidUtil.toBytes16(userId));
            if (ev.getCoachTestId() != null) {
                ps.setBytes(i++, UuidUtil.toBytes16(ev.getCoachTestId()));
            } else {
                ps.setObject(i++, null);
            }
            ps.setString(i++, ev.getCoachTestTitle());
            setTimestamp(ps, i++, ev.getTestedAt());
            ps.setInt(i++, ev.getMood());
            ps.setInt(i++, ev.getStress());
            ps.setInt(i++, ev.getSleep());
            ps.setInt(i++, ev.getMotivation());
            ps.setInt(i++, ev.getMentalTired());
            ps.setInt(i++, ev.getScore());
            ps.setString(i++, ev.getStatus());
            ps.setString(i++, ev.getNotes());
            ps.setString(i++, scoresJson);
            ps.setString(i, promptsJson);
            ps.executeUpdate();
        }
    }

    public void delete(UUID evaluationId) throws SQLException {
        if (cnx == null) {
            throw new SQLException("No connection");
        }
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM mental_health_evaluation WHERE id = ?")) {
            ps.setBytes(1, UuidUtil.toBytes16(evaluationId));
            ps.executeUpdate();
        }
    }

    private MentalHealthEvaluation mapRow(ResultSet rs) throws SQLException {
        UUID id = UuidUtil.fromResultSet(rs, "id");
        MentalHealthEvaluation ev = new MentalHealthEvaluation(id);
        ev.setUserId(UuidUtil.fromResultSet(rs, "user_id"));
        ev.setCoachTestId(UuidUtil.fromResultSet(rs, "coach_test_id"));
        ev.setCoachTestTitle(rs.getString("coach_test_title"));
        Timestamp ts = rs.getTimestamp("tested_at");
        ev.setTestedAt(ts == null ? null : ts.toLocalDateTime());
        ev.setMood(rs.getInt("mood"));
        ev.setStress(rs.getInt("stress"));
        ev.setSleep(rs.getInt("sleep"));
        ev.setMotivation(rs.getInt("motivation"));
        ev.setMentalTired(rs.getInt("mental_tired"));
        ev.setScore(rs.getInt("score"));
        ev.setStatus(rs.getString("status"));
        ev.setNotes(rs.getString("member_notes"));
        String sj = rs.getString("question_scores_json");
        if (sj != null && !sj.isBlank()) {
            List<Integer> scores = GSON.fromJson(sj, INT_LIST);
            if (scores != null) {
                ev.setQuestionScores(scores);
            }
        }
        String pj = rs.getString("question_prompts_json");
        if (pj != null && !pj.isBlank()) {
            List<String> prompts = GSON.fromJson(pj, STRING_LIST);
            if (prompts != null) {
                ev.setQuestionPrompts(prompts);
            }
        }
        return ev;
    }

    private static void setTimestamp(PreparedStatement ps, int idx, LocalDateTime ldt) throws SQLException {
        if (ldt == null) {
            ps.setTimestamp(idx, null);
        } else {
            ps.setTimestamp(idx, Timestamp.valueOf(ldt));
        }
    }
}
