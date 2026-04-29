package services;

import models.FeedbackResponse;
import models.User;
import models.Workout;
import utils.DbConnection;
import utils.UuidUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        UUID uuid = UuidUtil.fromResultSet(rs, "id");
        f.setId(uuid);
        f.setRating(rs.getString("rating"));
        f.setComment(rs.getString("comment"));
        f.setSentiment(rs.getString("sentiment"));
        f.setKeywords(rs.getString("keywords"));
        f.setAiSummary(rs.getString("ai_summary"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) f.setCreatedAt(ts.toInstant());

        try {
            String firstname = rs.getString("firstname");
            String lastname = rs.getString("lastname");
            if (firstname != null || lastname != null) {
                User u = new User();
                u.setFirstname(firstname);
                u.setLastname(lastname);
                f.setUser(u);
            }
        } catch (SQLException ignored) {}

        try {
            String workoutNom = rs.getString("nom");
            if (workoutNom != null) {
                Workout w = new Workout();
                w.setNom(workoutNom);
                f.setWorkout(w);
            }
        } catch (SQLException ignored) {}

        return f;
    }

    public List<FeedbackResponse> readWithDetails() throws SQLException {
        String sql = "SELECT fr.*, u.name_firstname as firstname, u.name_lastname as lastname, w.nom " +
                "FROM feedback_response fr " +
                "LEFT JOIN fitsense.app_user u ON u.id = fr.user_id " +
                "LEFT JOIN workout w ON w.id = fr.workout_id " +
                "ORDER BY fr.created_at DESC";
        List<FeedbackResponse> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public void create(FeedbackResponse f) throws SQLException { createPrepared(f); }

    @Override
    public void createPrepared(FeedbackResponse f) throws SQLException {
        UUID id = UUID.randomUUID();
        String sql = "INSERT INTO feedback_response (id, user_id, workout_id, coach_id, rating, comment, sentiment, keywords, ai_summary, created_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(id));
            ps.setBytes(2, f.getUser() != null ? UuidUtil.toBytes16(f.getUser().getId()) : null);
            ps.setBytes(3, f.getWorkout() != null && f.getWorkout().getId() != null
                    ? UuidUtil.toBytes16(f.getWorkout().getId()) : null);
            ps.setBytes(4, f.getCoach() != null ? UuidUtil.toBytes16(f.getCoach().getId()) : null);
            ps.setString(5, f.getRating());
            ps.setString(6, f.getComment());
            ps.setString(7, f.getSentiment());
            ps.setString(8, f.getKeywords());
            ps.setString(9, f.getAiSummary());
            ps.setTimestamp(10, Timestamp.from(f.getCreatedAt()));
            ps.executeUpdate();
        }
    }

    @Override
    public List<FeedbackResponse> read() throws SQLException {
        return readWithDetails();
    }

    @Override
    public void update(FeedbackResponse f) throws SQLException {
        // find by matching created_at + rating since we don't store UUID in model
        String sql = "UPDATE feedback_response SET sentiment=?, keywords=?, ai_summary=? WHERE rating=? AND created_at=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, f.getSentiment());
            ps.setString(2, f.getKeywords());
            ps.setString(3, f.getAiSummary());
            ps.setString(4, f.getRating());
            ps.setTimestamp(5, f.getCreatedAt() != null ? Timestamp.from(f.getCreatedAt()) : null);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(FeedbackResponse f) throws SQLException {
        // delete by rating + created_at since we don't store UUID in model
        String sql = "DELETE FROM feedback_response WHERE rating=? AND created_at=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, f.getRating());
            ps.setTimestamp(2, f.getCreatedAt() != null ? Timestamp.from(f.getCreatedAt()) : null);
            ps.executeUpdate();
        }
    }
}
