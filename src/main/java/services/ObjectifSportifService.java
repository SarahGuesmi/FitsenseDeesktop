package services;

import models.ObjectifSportif;
import utils.DbConnection;
import utils.ResultSetColumns;
import utils.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ObjectifSportifService implements CRUD<ObjectifSportif> {

    public Connection cnx;

    public ObjectifSportifService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    @FunctionalInterface
    private interface PreparedBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private static ObjectifSportif mapRow(ResultSet rs) throws SQLException {
        String name = extractObjectiveLabel(rs);
        UUID id = safeUuidFromRow(rs, "id");
        UUID profilePhysiqueId = safeUuidFromRow(rs, "profile_physique_id", "profile_id", "physique_id");
        return new ObjectifSportif(id, name, profilePhysiqueId);
    }

    private static UUID safeUuidFromRow(ResultSet rs, String... columnCandidates) {
        try {
            return UuidUtil.fromResultSetFirst(rs, columnCandidates);
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Symfony / custom schemas may use other column names or non-UUID primary keys; still resolve visible label text.
     */
    private static String extractObjectiveLabel(ResultSet rs) throws SQLException {
        String known = ResultSetColumns.coalesceNonBlank(
                ResultSetColumns.getFirstString(rs, "name", "libelle", "titre", "label", "title", "nom", "intitule",
                        "designation"));
        if (known != null) {
            return known;
        }
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            String label = md.getColumnLabel(i).toLowerCase(Locale.ROOT);
            if (label.endsWith("_id") || "id".equals(label)) {
                continue;
            }
            int sqlType = md.getColumnType(i);
            if (sqlType == Types.BINARY || sqlType == Types.VARBINARY || sqlType == Types.LONGVARBINARY
                    || sqlType == Types.BLOB) {
                continue;
            }
            String v = rs.getString(i);
            if (v == null || v.isBlank()) {
                continue;
            }
            String t = v.trim();
            if (t.matches(
                    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
                continue;
            }
            return t;
        }
        return null;
    }

    /**
     * Same joins as manual SQL using {@code u.email_email} — avoids binding {@code app_user.id} as Java UUID bytes.
     */
    public String findPrimaryObjectiveLabelByUserEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return "";
        }
        final String sql = "SELECT o.* FROM `objectif_sportif` o "
                + "INNER JOIN `profile_physique` p ON p.`id` = o.`profile_physique_id` "
                + "INNER JOIN `user` u ON u.`id` = p.`user_id` "
                + "WHERE u.`email` = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                List<ObjectifSportif> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
                return rows.stream()
                        .filter(o -> o.getName() != null && !o.getName().isBlank())
                        .max(Comparator.comparing(ObjectifSportif::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(o -> o.getName().trim())
                        .orElseGet(() -> rows.stream()
                                .map(ObjectifSportif::getName)
                                .filter(n -> n != null && !n.isBlank())
                                .map(String::trim)
                                .reduce((a, b) -> b)
                                .orElse(""));
            }
        }
    }

    /**
     * Loads every {@code objectif_sportif} row tied to any {@code profile_physique} of this app user.
     * Uses SQL so MySQL compares FK and profile ids directly (avoids Java UUID bind mismatches with Doctrine).
     */
    public List<ObjectifSportif> findAllByAppUserId(UUID appUserId) throws SQLException {
        final String joinSql = "SELECT o.* FROM `objectif_sportif` o "
                + "INNER JOIN `profile_physique` p ON p.`id` = o.`profile_physique_id` "
                + "WHERE p.`user_id` = ?";
        final String inSql = "SELECT * FROM `objectif_sportif` WHERE `profile_physique_id` IN ("
                + "SELECT `id` FROM `profile_physique` WHERE `user_id` = ?)";
        List<ObjectifSportif> out = tryFetchObjectifsForUser(joinSql, appUserId);
        if (!out.isEmpty()) {
            return out;
        }
        out = tryFetchObjectifsForUser(inSql, appUserId);
        return out;
    }

    private List<ObjectifSportif> tryFetchObjectifsForUser(String sql, UUID appUserId) throws SQLException {
        List<ObjectifSportif> a = fetchObjectifsWithUserParam(sql, ps -> ps.setBytes(1, UuidUtil.toBytes16(appUserId)));
        if (!a.isEmpty()) {
            return a;
        }
        return fetchObjectifsWithUserParam(sql, ps -> ps.setString(1, appUserId.toString()));
    }

    private List<ObjectifSportif> fetchObjectifsWithUserParam(String sql, PreparedBinder binder) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<ObjectifSportif> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
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
                if (!list.isEmpty()) {
                    return list;
                }
            }
        }
        // Symfony / some schemas store FK as CHAR(36); BINARY(16) bind would match nothing.
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, profilePhysiqueId.toString());
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
