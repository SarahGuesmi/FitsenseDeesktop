package services;

import models.ProfilePhysique;
import utils.DbConnection;
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
        UUID id = UuidUtil.fromResultSet(rs, "id");
        UUID userId = UuidUtil.fromResultSet(rs, "user_id");
        float w = rs.getFloat("weight");
        Float weight = rs.wasNull() ? null : w;
        float h = rs.getFloat("height");
        Float height = rs.wasNull() ? null : h;
        String gender = rs.getString("gender");
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
        String sql = "SELECT p.* FROM `profile_physique` p " +
                "INNER JOIN `app_user` u ON u.`id` = p.`user_id` " +
                "WHERE u.`email_email` = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("gender");
            }
        }
        return null;
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
        String sql = "UPDATE `profile_physique` SET `weight`=?, `height`=?, `gender`=? WHERE `id`=?";
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
        String sql = "DELETE FROM `profile_physique` WHERE `id`=?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(p.getId()));
            stmt.executeUpdate();
        }
    }

    @Override
    public void createPrepared(ProfilePhysique p) throws SQLException {
        if (p.getId() == null) p.setId(UUID.randomUUID());
        String sql = "INSERT INTO `profile_physique` (`id`, `user_id`, `weight`, `height`, `gender`) VALUES (?,?,?,?,?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(p.getId()));
            stmt.setBytes(2, UuidUtil.toBytes16(p.getUserId()));
            if (p.getWeight() != null) stmt.setFloat(3, p.getWeight());
            else stmt.setNull(3, Types.FLOAT);
            if (p.getHeight() != null) stmt.setFloat(4, p.getHeight());
            else stmt.setNull(4, Types.FLOAT);
            stmt.setString(5, p.getGender());
            stmt.executeUpdate();
        }
    }
}
