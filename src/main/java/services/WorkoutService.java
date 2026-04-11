package services;

import models.Workout;
import utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkoutService implements CRUD<Workout> {

    private final Connection cnx;

    public WorkoutService() {
        this.cnx = DbConnection.getInstance().getCnx();
    }

    private static Workout mapRow(ResultSet rs) throws SQLException {
        return new Workout(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("niveau"),
                rs.getObject("duree", Integer.class),
                rs.getString("description"),
                rs.getString("status"),
                null);
    }

    @Override
    public List<Workout> read() throws SQLException {
        List<Workout> list = new ArrayList<>();
        String sql = "SELECT * FROM workout";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public void create(Workout w) throws SQLException { createPrepared(w); }

    @Override
    public void createPrepared(Workout w) throws SQLException {
        String sql = "INSERT INTO workout (nom, niveau, duree, description, status, coach_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, w.getNom());
            ps.setString(2, w.getNiveau());
            ps.setObject(3, w.getDuree());
            ps.setString(4, w.getDescription());
            ps.setString(5, w.getStatus());
            ps.setObject(6, w.getCoach() != null ? (int) w.getCoach().getId().getLeastSignificantBits() : null);
            ps.executeUpdate();
        }
    }

    @Override
    public void update(Workout w) throws SQLException {
        String sql = "UPDATE workout SET nom=?, niveau=?, duree=?, description=?, status=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, w.getNom());
            ps.setString(2, w.getNiveau());
            ps.setObject(3, w.getDuree());
            ps.setString(4, w.getDescription());
            ps.setString(5, w.getStatus());
            ps.setInt(6, w.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Workout w) throws SQLException {
        String sql = "DELETE FROM workout WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, w.getId());
            ps.executeUpdate();
        }
    }
}
