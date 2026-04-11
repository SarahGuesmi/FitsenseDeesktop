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
        Object genderObj = ResultSetColumns.getFirstObject(rs, "gender", "genre", "sexe");
        String gender = ResultSetColumns.normalizeGenderDbValue(genderObj);
        return new ProfilePhysique(
                UuidUtil.fromResultSet(rs, "id"),
                weight,
                height,
                gender,
                UuidUtil.fromResultSet(rs, "user_id"));
    }

    public List<ProfilePhysique> findByUserId(UUID userId) throws SQLException {
        String sql = "SELECT * FROM `profile_physique` WHERE `user_id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(userId));
            try (ResultSet rs = stmt.executeQuery()) {
                List<ProfilePhysique> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                if (!list.isEmpty()) {
                    return list;
                }
            }
        }
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, userId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                List<ProfilePhysique> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    /**
     * Gender from any physique row for this login, using the same {@code email_email} join as manual SQL.
     */
    public String findGenderByUserEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return null;
        }
        String sql = "SELECT p.* FROM `profile_physique` p "
                + "INNER JOIN `app_user` u ON u.`id` = p.`user_id` "
                + "WHERE u.`email_email` = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object g = ResultSetColumns.getFirstObject(rs, "gender", "genre", "sexe");
                    String n = ResultSetColumns.normalizeGenderDbValue(g);
                    if (n != null && !n.isBlank()) {
                        return n;
                    }
                }
                return null;
            }
        }
    }

    @Override
    public void create(ProfilePhysique profilePhysique) throws SQLException {
        createPrepared(profilePhysique);
    }

    @Override
    public List<ProfilePhysique> read() throws SQLException {
        String sql = "SELECT * FROM `profile_physique`";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            List<ProfilePhysique> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    @Override
    public void update(ProfilePhysique profilePhysique) throws SQLException {
        String sql = "UPDATE `profile_physique` SET `weight` = ?, `height` = ?, `gender` = ?, `user_id` = ? "
                + "WHERE `id` = ?";
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
            stmt.setBytes(i++, UuidUtil.toBytes16(profilePhysique.getUserId()));
            stmt.setBytes(i, UuidUtil.toBytes16(profilePhysique.getId()));
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(ProfilePhysique profilePhysique) throws SQLException {
        String sql = "DELETE FROM `profile_physique` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(profilePhysique.getId()));
            stmt.executeUpdate();
        }
    }

    @Override
    public void createPrepared(ProfilePhysique profilePhysique) throws SQLException {
        if (profilePhysique.getId() == null) {
            profilePhysique.setId(UUID.randomUUID());
        }
        String sql = "INSERT INTO `profile_physique` (`id`, `weight`, `height`, `gender`, `user_id`) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            int i = 1;
            stmt.setBytes(i++, UuidUtil.toBytes16(profilePhysique.getId()));
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
            stmt.setBytes(i, UuidUtil.toBytes16(profilePhysique.getUserId()));
            stmt.executeUpdate();
        }
    }
}
