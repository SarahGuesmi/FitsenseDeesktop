package services;

import models.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import utils.DbConnection;
import utils.ResultSetColumns;
import utils.UuidUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserService implements CRUD<User> {

    public Connection cnx;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("date_creation");
        LocalDateTime dateCreation =
                ts != null ? ts.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime() : null;

        // id is INT in this schema — convert to UUID for the model
        UUID id = null;
        Object idObj = rs.getObject("id");
        if (idObj instanceof Integer i) {
            id = new UUID(0L, i.longValue());
        } else if (idObj instanceof Long l) {
            id = new UUID(0L, l);
        } else if (idObj != null) {
            try { id = UuidUtil.fromResultSet(rs, "id"); } catch (SQLException ignored) {}
        }

        User user = new User(
                id,
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("roles"),
                rs.getString("firstname"),
                rs.getString("lastname"),
                rs.getString("account_status"),
                dateCreation,
                null, // google_authenticator_secret — not in this schema
                null, // phone_number
                null, // photo
                null); // username
        String obj = null;
        try {
            obj = ResultSetColumns.coalesceNonBlank(
                    ResultSetColumns.getFirstString(rs, "objective"),
                    ResultSetColumns.getFirstString(rs, "objectif"));
        } catch (SQLException ignored) {}
        user.setAccountObjective(obj);
        Object genderObj = null;
        try {
            genderObj = ResultSetColumns.getFirstObject(rs, "gender", "genre", "sexe");
        } catch (SQLException ignored) {}
        user.setAccountGender(ResultSetColumns.normalizeGenderDbValue(genderObj));
        return user;
    }

    private String hashPasswordIfPlain(String password) {
        if (password == null) {
            return null;
        }
        if (password.startsWith("$2a$") || password.startsWith("$2y$") || password.startsWith("$2b$")) {
            return password;
        }
        return passwordEncoder.encode(password);
    }

    /**
     * Same as {@link #findByEmail(String)} — matches Symfony login lookup.
     */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE `email` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public User findByUsername(String username) throws SQLException {
        // username column doesn't exist in this schema — fallback to email
        return findByEmail(username);
    }

    @Override
    public void create(User user) throws SQLException {
        createPrepared(user);
    }

    @Override
    public List<User> read() throws SQLException {
        String sql = "SELECT * FROM `user`";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            List<User> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE `user` SET `email` = ?, `password` = ?, `roles` = ?, "
                + "`firstname` = ?, `lastname` = ?, `account_status` = ? "
                + "WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, hashPasswordIfPlain(user.getPassword()));
            stmt.setString(3, user.getRolesJson() != null ? user.getRolesJson() : "[\"ROLE_USER\"]");
            stmt.setString(4, user.getFirstname());
            stmt.setString(5, user.getLastname());
            stmt.setString(6, user.getAccountStatus());
            stmt.setLong(7, user.getId() != null ? user.getId().getLeastSignificantBits() : 0);
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(User user) throws SQLException {
        String sql = "DELETE FROM `user` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setLong(1, user.getId() != null ? user.getId().getLeastSignificantBits() : 0);
            stmt.executeUpdate();
        }
    }

    /**
     * Deletes several users by id (same idea as Symfony {@code bulkDelete}).
     */
    public void deleteByIds(List<UUID> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) return;
        String sql = "DELETE FROM `user` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            for (UUID id : ids) {
                stmt.setLong(1, id.getLeastSignificantBits());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public void createPrepared(User user) throws SQLException {
        if (user.getRolesJson() == null || user.getRolesJson().isBlank()) {
            user.setRolesJson("[\"ROLE_USER\"]");
        }
        if (user.getAccountStatus() == null || user.getAccountStatus().isBlank()) {
            user.setAccountStatus("active");
        }
        String sql = "INSERT INTO `user` (`email`, `password`, `roles`, `firstname`, `lastname`, `account_status`, `date_creation`) "
                + "VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, hashPasswordIfPlain(user.getPassword()));
            stmt.setString(3, user.getRolesJson());
            stmt.setString(4, user.getFirstname());
            stmt.setString(5, user.getLastname());
            stmt.setString(6, user.getAccountStatus());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(new UUID(0L, keys.getLong(1)));
                }
            }
        }
    }
}
