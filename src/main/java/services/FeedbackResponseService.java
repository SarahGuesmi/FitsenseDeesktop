package services;

import models.FeedbackResponse;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeedbackResponseService implements CRUD<FeedbackResponse> {

    private final Connection connection;

    public FeedbackResponseService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void create(FeedbackResponse f) throws SQLException {
        String sql = "INSERT INTO feedback_response (user_id, workout_id, coach_id, rating, comment, sentiment, keywords, ai_summary, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    @Override
    public void createPrepared(FeedbackResponse f) throws SQLException {
        String sql = "INSERT INTO feedback_response (user_id, workout_id, coach_id, rating, comment, sentiment, keywords, ai_summary, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, f.getUser() != null ? f.getUser().getId() : null);
            ps.setObject(2, f.getWorkout() != null ? f.getWorkout().getId() : null);
            ps.setObject(3, f.getCoach() != null ? f.getCoach().getId() : null);
            ps.setString(4, f.getRating());
            ps.setString(5, f.getComment());
            ps.setString(6, f.getSentiment());
            ps.setString(7, f.getKeywords());
            ps.setString(8, f.getAiSummary());
            ps.setTimestamp(9, Timestamp.from(f.getCreatedAt()));
            ps.executeUpdate();
        }
    }

    @Override
    public List<FeedbackResponse> read() throws SQLException {
        List<FeedbackResponse> list = new ArrayList<>();
        String sql = "SELECT * FROM feedback_response";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                FeedbackResponse f = new FeedbackResponse();
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
        String sql = "UPDATE feedback_response SET user_id=?, workout_id=?, coach_id=?, rating=?, comment=?, " +
                "sentiment=?, keywords=?, ai_summary=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, f.getUser() != null ? f.getUser().getId() : null);
            ps.setObject(2, f.getWorkout() != null ? f.getWorkout().getId() : null);
            ps.setObject(3, f.getCoach() != null ? f.getCoach().getId() : null);
            ps.setString(4, f.getRating());
            ps.setString(5, f.getComment());
            ps.setString(6, f.getSentiment());
            ps.setString(7, f.getKeywords());
            ps.setString(8, f.getAiSummary());
            ps.setInt(9, f.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(FeedbackResponse f) throws SQLException {
        String sql = "DELETE FROM feedback_response WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, f.getId());
            ps.executeUpdate();
        }
    }
}
