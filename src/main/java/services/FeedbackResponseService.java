package services;

import models.FeedbackResponse;
import utils.UuidUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FeedbackResponseService implements CRUD<FeedbackResponse> {

    private final Connection connection;

    public FeedbackResponseService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void create(FeedbackResponse f) throws SQLException {
        createPrepared(f);
    }

    @Override
    public void createPrepared(FeedbackResponse f) throws SQLException {
        UUID newId = UUID.randomUUID();
        String sql = "INSERT INTO `feedback_response` (`id`, `user_id`, `workout_id`, `coach_id`, `rating`, "
                + "`comment`, `sentiment`, `keywords`, `ai_summary`, `created_at`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(newId));
            if (f.getUser() != null && f.getUser().getId() != null)
                ps.setBytes(2, UuidUtil.toBytes16(f.getUser().getId()));
            else
                ps.setNull(2, Types.BINARY);
            if (f.getWorkout() != null && f.getWorkout().getId() != null)
                ps.setBytes(3, UuidUtil.toBytes16(f.getWorkout().getId()));
            else
                ps.setNull(3, Types.BINARY);
            if (f.getCoach() != null && f.getCoach().getId() != null)
                ps.setBytes(4, UuidUtil.toBytes16(f.getCoach().getId()));
            else
                ps.setNull(4, Types.BINARY);
            ps.setString(5, f.getRating());
            ps.setString(6, f.getComment());
            ps.setString(7, f.getSentiment());
            ps.setString(8, f.getKeywords());
            ps.setString(9, f.getAiSummary());
            ps.setTimestamp(10, f.getCreatedAt() != null ? Timestamp.from(f.getCreatedAt()) : Timestamp.from(java.time.Instant.now()));
            ps.executeUpdate();
            f.setId(newId);
        }
    }

    @Override
    public List<FeedbackResponse> read() throws SQLException {
        List<FeedbackResponse> list = new ArrayList<>();
        String sql = "SELECT * FROM `feedback_response`";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                FeedbackResponse f = new FeedbackResponse();
                f.setId(UuidUtil.fromResultSet(rs, "id"));
                f.setRating(rs.getString("rating"));
                f.setComment(rs.getString("comment"));
                f.setSentiment(rs.getString("sentiment"));
                f.setKeywords(rs.getString("keywords"));
                f.setAiSummary(rs.getString("ai_summary"));
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) f.setCreatedAt(ts.toInstant());
                list.add(f);
            }
        }
        return list;
    }

    @Override
    public void update(FeedbackResponse f) throws SQLException {
        String sql = "UPDATE `feedback_response` SET `user_id` = ?, `workout_id` = ?, `coach_id` = ?, "
                + "`rating` = ?, `comment` = ?, `sentiment` = ?, `keywords` = ?, `ai_summary` = ? "
                + "WHERE `id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (f.getUser() != null && f.getUser().getId() != null)
                ps.setBytes(1, UuidUtil.toBytes16(f.getUser().getId()));
            else
                ps.setNull(1, Types.BINARY);
            if (f.getWorkout() != null && f.getWorkout().getId() != null)
                ps.setBytes(2, UuidUtil.toBytes16(f.getWorkout().getId()));
            else
                ps.setNull(2, Types.BINARY);
            if (f.getCoach() != null && f.getCoach().getId() != null)
                ps.setBytes(3, UuidUtil.toBytes16(f.getCoach().getId()));
            else
                ps.setNull(3, Types.BINARY);
            ps.setString(4, f.getRating());
            ps.setString(5, f.getComment());
            ps.setString(6, f.getSentiment());
            ps.setString(7, f.getKeywords());
            ps.setString(8, f.getAiSummary());
            ps.setBytes(9, UuidUtil.toBytes16(f.getId()));
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(FeedbackResponse f) throws SQLException {
        String sql = "DELETE FROM `feedback_response` WHERE `id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(f.getId()));
            ps.executeUpdate();
        }
    }
}
