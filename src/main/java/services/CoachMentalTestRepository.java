package services;

import models.CoachMentalTest;
import models.CoachMentalTestQuestion;
import utils.DbConnection;
import utils.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JDBC persistence for coach-authored mental tests.
 */
public final class CoachMentalTestRepository {

    private final Connection cnx;

    public CoachMentalTestRepository() {
        this.cnx = DbConnection.getInstance().getCnx();
    }

    public List<CoachMentalTest> findAll() throws SQLException {
        if (cnx == null) {
            return List.of();
        }
        Map<UUID, CoachMentalTest> byId = new LinkedHashMap<>();
        String sql = "SELECT id, coach_user_id, title, created_at, updated_at FROM coach_mental_test ORDER BY updated_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID id = UuidUtil.fromResultSet(rs, "id");
                CoachMentalTest t = new CoachMentalTest(id);
                t.setCoachUserId(UuidUtil.fromResultSet(rs, "coach_user_id"));
                t.setTitle(rs.getString("title"));
                t.setCreatedAt(toLdt(rs, "created_at"));
                t.setUpdatedAt(toLdt(rs, "updated_at"));
                byId.put(id, t);
            }
        }
        if (byId.isEmpty()) {
            return List.of();
        }
        String qsql = "SELECT id, test_id, order_index, prompt FROM coach_mental_test_question WHERE test_id = ? ORDER BY order_index ASC";
        for (CoachMentalTest t : byId.values()) {
            try (PreparedStatement ps = cnx.prepareStatement(qsql)) {
                ps.setBytes(1, UuidUtil.toBytes16(t.getId()));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID qid = UuidUtil.fromResultSet(rs, "id");
                        int ord = rs.getInt("order_index");
                        String prompt = rs.getString("prompt");
                        t.addQuestion(new CoachMentalTestQuestion(qid, ord, prompt));
                    }
                }
            }
        }
        return new ArrayList<>(byId.values());
    }

    public void save(CoachMentalTest t) throws SQLException {
        if (cnx == null || t.getCoachUserId() == null) {
            throw new SQLException("No DB connection or missing coach user id");
        }
        cnx.setAutoCommit(false);
        try {
            boolean exists;
            try (PreparedStatement ck = cnx.prepareStatement("SELECT 1 FROM coach_mental_test WHERE id = ? LIMIT 1")) {
                ck.setBytes(1, UuidUtil.toBytes16(t.getId()));
                try (ResultSet rs = ck.executeQuery()) {
                    exists = rs.next();
                }
            }
            if (exists) {
                try (PreparedStatement u = cnx.prepareStatement(
                        "UPDATE coach_mental_test SET coach_user_id = ?, title = ?, created_at = ?, updated_at = ? WHERE id = ?")) {
                    int i = 1;
                    u.setBytes(i++, UuidUtil.toBytes16(t.getCoachUserId()));
                    u.setString(i++, t.getTitle() == null ? "" : t.getTitle());
                    setTimestamp(u, i++, t.getCreatedAt());
                    setTimestamp(u, i++, t.getUpdatedAt());
                    u.setBytes(i, UuidUtil.toBytes16(t.getId()));
                    u.executeUpdate();
                }
            } else {
                try (PreparedStatement ins = cnx.prepareStatement(
                        "INSERT INTO coach_mental_test (id, coach_user_id, title, created_at, updated_at) VALUES (?,?,?,?,?)")) {
                    int i = 1;
                    ins.setBytes(i++, UuidUtil.toBytes16(t.getId()));
                    ins.setBytes(i++, UuidUtil.toBytes16(t.getCoachUserId()));
                    ins.setString(i++, t.getTitle() == null ? "" : t.getTitle());
                    setTimestamp(ins, i++, t.getCreatedAt());
                    setTimestamp(ins, i, t.getUpdatedAt());
                    ins.executeUpdate();
                }
            }
            try (PreparedStatement del = cnx.prepareStatement("DELETE FROM coach_mental_test_question WHERE test_id = ?")) {
                del.setBytes(1, UuidUtil.toBytes16(t.getId()));
                del.executeUpdate();
            }
            try (PreparedStatement iq = cnx.prepareStatement(
                    "INSERT INTO coach_mental_test_question (id, test_id, order_index, prompt) VALUES (?,?,?,?)")) {
                for (CoachMentalTestQuestion q : t.getQuestions()) {
                    iq.setBytes(1, UuidUtil.toBytes16(q.getId()));
                    iq.setBytes(2, UuidUtil.toBytes16(t.getId()));
                    iq.setInt(3, q.getOrderIndex());
                    iq.setString(4, q.getPrompt() == null ? "" : q.getPrompt());
                    iq.addBatch();
                }
                iq.executeBatch();
            }
            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    public void delete(UUID testId) throws SQLException {
        if (cnx == null) {
            throw new SQLException("No connection");
        }
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM coach_mental_test WHERE id = ?")) {
            ps.setBytes(1, UuidUtil.toBytes16(testId));
            ps.executeUpdate();
        }
    }

    private static LocalDateTime toLdt(ResultSet rs, String col) throws SQLException {
        Timestamp ts = rs.getTimestamp(col);
        return ts == null ? null : ts.toLocalDateTime();
    }

    private static void setTimestamp(PreparedStatement ps, int idx, LocalDateTime ldt) throws SQLException {
        if (ldt == null) {
            ps.setTimestamp(idx, null);
        } else {
            ps.setTimestamp(idx, Timestamp.valueOf(ldt));
        }
    }
}
