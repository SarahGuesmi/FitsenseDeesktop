package services;

import models.ObjectifSportif;
import utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ObjectifSportifService implements CRUD<ObjectifSportif> {

    public Connection cnx;

    public ObjectifSportifService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    private static ObjectifSportif mapRow(ResultSet rs) throws SQLException {
        UUID id = utils.UuidUtil.fromResultSet(rs, "id");
        String name = null;
        try { name = rs.getString("name"); } catch (SQLException ignored) {}
        if (name == null) try { name = rs.getString("libelle"); } catch (SQLException ignored) {}
        UUID profilePhysiqueId = utils.UuidUtil.fromResultSet(rs, "profile_physique_id");
        return new ObjectifSportif(id, name, profilePhysiqueId);
    }

    public String findPrimaryObjectiveLabelByUserEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) return "";
        String sql = "SELECT o.* FROM `objectif_sportif` o "
                + "INNER JOIN `profile_physique` p ON p.`id` = o.`profile_physique_id` "
                + "INNER JOIN `app_user` u ON u.`id` = p.`user_id` "
                + "WHERE u.`email_email` = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                String last = "";
                while (rs.next()) {
                    ObjectifSportif o = mapRow(rs);
                    if (o.getName() != null && !o.getName().isBlank()) last = o.getName().trim();
                }
                return last;
            }
        }
    }

    public List<ObjectifSportif> findAllByAppUserId(UUID appUserId) throws SQLException {
        String sql = "SELECT o.* FROM `objectif_sportif` o "
                + "INNER JOIN `profile_physique` p ON p.`id` = o.`profile_physique_id` "
                + "WHERE p.`user_id` = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBytes(1, utils.UuidUtil.toBytes16(appUserId));
            try (ResultSet rs = ps.executeQuery()) {
                List<ObjectifSportif> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    public List<ObjectifSportif> findByProfilePhysiqueId(UUID profilePhysiqueId) throws SQLException {
        String sql = "SELECT * FROM `objectif_sportif` WHERE `profile_physique_id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, utils.UuidUtil.toBytes16(profilePhysiqueId));
            try (ResultSet rs = stmt.executeQuery()) {
                List<ObjectifSportif> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    @Override
    public void create(ObjectifSportif o) throws SQLException { createPrepared(o); }

    @Override
    public List<ObjectifSportif> read() throws SQLException {
        String sql = "SELECT * FROM `objectif_sportif`";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            List<ObjectifSportif> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        }
    }

    @Override
    public void update(ObjectifSportif o) throws SQLException {
        String sql = "UPDATE `objectif_sportif` SET `name` = ? WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, o.getName());
            stmt.setBytes(2, utils.UuidUtil.toBytes16(o.getId()));
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(ObjectifSportif o) throws SQLException {
        String sql = "DELETE FROM `objectif_sportif` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, utils.UuidUtil.toBytes16(o.getId()));
            stmt.executeUpdate();
        }
    }

    public void deleteByProfilePhysiqueId(UUID profileId) throws SQLException {
        String sql = "DELETE FROM `objectif_sportif` WHERE `profile_physique_id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, utils.UuidUtil.toBytes16(profileId));
            stmt.executeUpdate();
        }
    }

    @Override
    public void createPrepared(ObjectifSportif o) throws SQLException {
        UUID newId = java.util.UUID.randomUUID();
        String sql = "INSERT INTO `objectif_sportif` (`id`, `name`, `profile_physique_id`) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, utils.UuidUtil.toBytes16(newId));
            stmt.setString(2, o.getName());
            stmt.setBytes(3, utils.UuidUtil.toBytes16(o.getProfilePhysiqueId()));
            stmt.executeUpdate();
            o.setId(newId);
        }
    }
}
