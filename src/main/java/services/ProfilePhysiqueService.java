package services;

import models.ProfilePhysique;
import utils.DbConnection;
import utils.ResultSetColumns;
import utils.UuidUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProfilePhysiqueService implements CRUD<ProfilePhysique> {

    public Connection cnx;

    public ProfilePhysiqueService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    private static ProfilePhysique mapRow(ResultSet rs) throws SQLException {
        float w = rs.getFloat("weight");
        Float weight = rs.wasNull() ? null : w;
        float h = rs.getFloat("height");
        Float height = rs.wasNull() ? null : h;
        Object genderObj = ResultSetColumns.getFirstObject(rs, "gender");
        String gender = ResultSetColumns.normalizeGenderDbValue(genderObj);
        // id and user_id are int in DB — wrap as UUID for model compatibility
        UUID id = new UUID(0, rs.getInt("id"));
        UUID userId = new UUID(0, rs.getInt("user_id"));
        return new ProfilePhysique(id, weight, height, gender, userId);
    }

    public List<ProfilePhysique> findByUserId(UUID userId) throws SQLException {
        String sql = "SELECT * FROM `profile_physique` WHERE `user_id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, (int) userId.getLeastSignificantBits());
            try (ResultSet rs = stmt.executeQuery()) {
                List<ProfilePhysique> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    public String findGenderByUserEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) return null;
        String sql = "SELECT p.* FROM `profile_physique` p "
                + "INNER JOIN `user` u ON u.`id` = p.`user_id` "
                + "WHERE u.`email` = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object g = ResultSetColumns.getFirstObject(rs, "gender");
                    String n = ResultSetColumns.normalizeGenderDbValue(g);
                    if (n != null && !n.isBlank()) return n;
                }
                return null;
            }
        }
    }

    @Override
    public void create(ProfilePhysique p) throws SQLException { createPrepared(p); }

    @Override
    public List<ProfilePhysique> read() throws SQLException {
        String sql = "SELECT * FROM `profile_physique`";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            List<ProfilePhysique> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        }
    }

    @Override
    public void update(ProfilePhysique p) throws SQLException {
        String sql = "UPDATE `profile_physique` SET `weight` = ?, `height` = ?, `gender` = ? WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            int i = 1;
            if (profilePhysique.getWeight() != null) {
                stmt.setFloat(i++, profilePhysique.getWeight());
            } else {
                stmt.setNull(i++, Types.FLOAT);
            }
            if (profilePhysique.getHeight() != null) {
                stmt.setFloat(i++, profilePhysique.getHeight());
            } else {
                stmt.setNull(i++, Types.FLOAT);
            }
            stmt.setString(i++, profilePhysique.getGender());
            stmt.setInt(i++, (int) profilePhysique.getUserId().getLeastSignificantBits());
            stmt.setInt(i, (int) profilePhysique.getId().getLeastSignificantBits());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(ProfilePhysique p) throws SQLException {
        String sql = "DELETE FROM `profile_physique` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, (int) profilePhysique.getId().getLeastSignificantBits());
            stmt.executeUpdate();
        }
    }

    @Override
    public void createPrepared(ProfilePhysique profilePhysique) throws SQLException {
        // id is auto_increment, don't insert it
        String sql = "INSERT INTO `profile_physique` (`weight`, `height`, `gender`, `user_id`) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            if (profilePhysique.getWeight() != null) {
                stmt.setFloat(i++, profilePhysique.getWeight());
            } else {
                stmt.setNull(i++, Types.FLOAT);
            }
            if (profilePhysique.getHeight() != null) {
                stmt.setFloat(i++, profilePhysique.getHeight());
            } else {
                stmt.setNull(i++, Types.FLOAT);
            }
            stmt.setString(i++, profilePhysique.getGender());
            stmt.setInt(i, (int) profilePhysique.getUserId().getLeastSignificantBits());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    profilePhysique.setId(new java.util.UUID(0, keys.getInt(1)));
                }
            }
        }
    }
}
