package services;

import models.FeedbackResponse;
import models.User;
import models.Workout;
import utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeedbackResponseService implements CRUD<FeedbackResponse> {

    private final Connection connection;

    public FeedbackResponseService() {
        this.connection = DbConnection.getInstance().getCnx();
    }

    public FeedbackResponseService(Connection connection) {
        this.connection = connection;
    }

    private static FeedbackResponse mapRow(ResultSet rs) throws SQLException {
        FeedbackResponse f = new FeedbackResponse();
        f.setId(rs.getInt("id"));
        f.setRating(rs.getString("rating"));
        f.setComment(rs.getString("comment"));
        f.setSentiment(rs.getString("sentiment"));
        f.setKeywords(rs.getString("keywords"));
        f.setAiSummary(rs.getString("ai_summary"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) f.setCreatedAt(ts.toInstant());

        // User
        try {
            int userId = rs.getInt("user_id");
            String firstname = rs.getString("firstname");
            String lastname = rs.getString("lastname");
            if (firstname != null || lastname != null) {
                User u = new User();
                u.setId(new java.util.UUID(0, userId));
                u.setFirstname(firstname);
                u.setLastname(lastname);
                f.setUser(u);
            }
        } catch (SQLException ignored) {}

        // Workout
        try {
            int workoutId = rs.getInt("workout_id");
            String workoutNom = rs.getString("nom");
            if (workoutNom != null) {
                Workout w = new Workout();
                w.setId(workoutId);
                w.setNom(workoutNom);
                f.setWorkout(w);
            }
        } catch (SQLException ignored) {}

        return f;
    }

    /** Read all responses with user and workout info joined */
    public List<FeedbackResponse> readWithDetails() throws SQLException {
        String sql = "SELECT fr.*, u.firstname, u.lastname, w.nom " +
                "FROM feedback_response fr " +
                "LEFT JOIN user u ON u.id = fr.user_id " +
                "LEFT JOIN workout w ON w.id = fr.workout_id " +
                "ORDER BY fr.created_at DESC";
        List<FeedbackResponse> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public void create(FeedbackResponse f) throws SQLException {
        createPrepared(f);
    }

    @Override
    public void createPrepared(FeedbackResponse f) throws SQLException {
        String sql = "INSERT INTO feedback_response (user_id, workout_id, coach_id, rating, comment, sentiment, keywords, ai_summary, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, f.getUser() != null ? (int) f.getUser().getId().getLeastSignificantBits() : null);
            ps.setObject(2, f.getWorkout() != null ? f.getWorkout().getId() : null);
            ps.setObject(3, f.getCoach() != null ? (int) f.getCoach().getId().getLeastSignificantBits() : null);
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
        return readWithDetails();
    }

    @Override
    public void update(FeedbackResponse f) throws SQLException {
        String sql = "UPDATE feedback_response SET rating=?, comment=?, sentiment=?, keywords=?, ai_summary=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, f.getRating());
            ps.setString(2, f.getComment());
            ps.setString(3, f.getSentiment());
            ps.setString(4, f.getKeywords());
            ps.setString(5, f.getAiSummary());
            ps.setInt(6, f.getId());
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
