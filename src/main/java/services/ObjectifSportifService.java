package services;

import models.ObjectifSportif;
import utils.DbConnection;
import utils.UuidUtil;

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
        String name = rs.getString("name");
        int intId = rs.getInt("id");
        UUID id = new UUID(0, intId);
        int profileIntId = rs.getInt("profile_physique_id");
        UUID profilePhysiqueId = new UUID(0, profileIntId);
        return new ObjectifSportif(id, name, profilePhysiqueId);
    }

    public String findPrimaryObjectiveLabelByUserEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) return "";
        String sql = "SELECT o.* FROM `objectif_sportif` o "
                + "INNER JOIN `profile_physique` p ON p.`id` = o.`profile_physique_id` "
                + "INNER JOIN `user` u ON u.`id` = p.`user_id` "
                + "WHERE u.`email` = ?";
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
            ps.setBytes(1, UuidUtil.toBytes16(appUserId));
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
            stmt.setInt(1, (int) profilePhysiqueId.getLeastSignificantBits());
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
            stmt.setInt(2, (int) o.getId().getLeastSignificantBits());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(ObjectifSportif o) throws SQLException {
        String sql = "DELETE FROM `objectif_sportif` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, (int) o.getId().getLeastSignificantBits());
            stmt.executeUpdate();
        }
    }

    @Override
    public void createPrepared(ObjectifSportif objectifSportif) throws SQLException {
        // id is auto_increment, don't insert it
        String sql = "INSERT INTO `objectif_sportif` (`name`, `profile_physique_id`) VALUES (?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, objectifSportif.getName());
            stmt.setInt(2, (int) objectifSportif.getProfilePhysiqueId().getLeastSignificantBits());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    objectifSportif.setId(new UUID(0, keys.getInt(1)));
                }
            }
        }
    }
}
