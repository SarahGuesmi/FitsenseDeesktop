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

        UUID id = UuidUtil.fromResultSet(rs, "id");
        UUID userId = UuidUtil.fromResultSet(rs, "user_id");

        return new ProfilePhysique(id, weight, height, gender, userId);
    }

    public List<ProfilePhysique> findByUserId(UUID userId) throws SQLException {
        String sql = "SELECT * FROM `profile_physique` WHERE `user_id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(userId));
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
                + "INNER JOIN `app_user` u ON u.`id` = p.`user_id` "
                + "WHERE u.`email_email` = ?";
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
            if (p.getWeight() != null) stmt.setFloat(1, p.getWeight());
            else stmt.setNull(1, Types.FLOAT);
            if (p.getHeight() != null) stmt.setFloat(2, p.getHeight());
            else stmt.setNull(2, Types.FLOAT);
            stmt.setString(3, p.getGender());
            stmt.setBytes(4, UuidUtil.toBytes16(p.getId()));
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(ProfilePhysique p) throws SQLException {
        String sql = "DELETE FROM `profile_physique` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(p.getId()));
            stmt.executeUpdate();
        }
    }

    @Override
    public void createPrepared(ProfilePhysique p) throws SQLException {
        if (p.getId() == null) p.setId(UUID.randomUUID());
        String sql = "INSERT INTO `profile_physique` (`id`, `weight`, `height`, `gender`, `user_id`) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(p.getId()));
            if (p.getWeight() != null) stmt.setFloat(2, p.getWeight());
            else stmt.setNull(2, Types.FLOAT);
            if (p.getHeight() != null) stmt.setFloat(3, p.getHeight());
            else stmt.setNull(3, Types.FLOAT);
            stmt.setString(4, p.getGender());
            stmt.setBytes(5, UuidUtil.toBytes16(p.getUserId()));
            stmt.executeUpdate();
        }
    }
}
