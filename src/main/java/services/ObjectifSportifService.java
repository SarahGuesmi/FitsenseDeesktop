package services;

import models.ObjectifSportif;
import utils.DbConnection;
import utils.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ObjectifSportifService implements CRUD<ObjectifSportif> {

    public Connection cnx;

    public ObjectifSportifService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    private static ObjectifSportif mapRow(ResultSet rs) throws SQLException {
        return new ObjectifSportif(
                UuidUtil.fromResultSet(rs, "id"),
                rs.getString("name"),
                UuidUtil.fromResultSet(rs, "profile_physique_id"));
    }

    /**
     * Same idea as Symfony {@code ObjectifSportifRepository::findByProfilePhysique}.
     */
    public List<ObjectifSportif> findByProfilePhysiqueId(UUID profilePhysiqueId) throws SQLException {
        String sql = "SELECT * FROM `objectif_sportif` WHERE `profile_physique_id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(profilePhysiqueId));
            try (ResultSet rs = stmt.executeQuery()) {
                List<ObjectifSportif> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    @Override
    public void create(ObjectifSportif objectifSportif) throws SQLException {
        createPrepared(objectifSportif);
    }

    @Override
    public List<ObjectifSportif> read() throws SQLException {
        String sql = "SELECT * FROM `objectif_sportif`";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            List<ObjectifSportif> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    @Override
    public void update(ObjectifSportif objectifSportif) throws SQLException {
        String sql = "UPDATE `objectif_sportif` SET `name` = ?, `profile_physique_id` = ? WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, objectifSportif.getName());
            stmt.setBytes(2, UuidUtil.toBytes16(objectifSportif.getProfilePhysiqueId()));
            stmt.setBytes(3, UuidUtil.toBytes16(objectifSportif.getId()));
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(ObjectifSportif objectifSportif) throws SQLException {
        String sql = "DELETE FROM `objectif_sportif` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(objectifSportif.getId()));
            stmt.executeUpdate();
        }
    }

    @Override
    public void createPrepared(ObjectifSportif objectifSportif) throws SQLException {
        if (objectifSportif.getId() == null) {
            objectifSportif.setId(UUID.randomUUID());
        }
        String sql = "INSERT INTO `objectif_sportif` (`id`, `name`, `profile_physique_id`) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(objectifSportif.getId()));
            stmt.setString(2, objectifSportif.getName());
            stmt.setBytes(3, UuidUtil.toBytes16(objectifSportif.getProfilePhysiqueId()));
            stmt.executeUpdate();
        }
    }
}
