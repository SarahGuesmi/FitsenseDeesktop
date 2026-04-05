package services;

import models.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import utils.DbConnection;
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
        return new User(
                UuidUtil.fromResultSet(rs, "id"),
                rs.getString("email_email"),
                rs.getString("password"),
                rs.getString("roles"),
                rs.getString("name_firstname"),
                rs.getString("name_lastname"),
                rs.getString("account_status"),
                dateCreation,
                rs.getString("google_authenticator_secret"),
                rs.getString("phone_number"),
                rs.getString("photo"),
                rs.getString("username"));
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
        String sql = "SELECT * FROM `app_user` WHERE `email_email` = ?";
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

    @Override
    public void create(User user) throws SQLException {
        createPrepared(user);
    }

    @Override
    public List<User> read() throws SQLException {
        String sql = "SELECT * FROM `app_user`";
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
        String sql = "UPDATE `app_user` SET `email_email` = ?, `password` = ?, `roles` = ?, "
                + "`name_firstname` = ?, `name_lastname` = ?, `account_status` = ?, `date_creation` = ?, "
                + "`google_authenticator_secret` = ?, `phone_number` = ?, `photo` = ?, `username` = ? "
                + "WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            int i = 1;
            stmt.setString(i++, user.getEmail());
            stmt.setString(i++, hashPasswordIfPlain(user.getPassword()));
            stmt.setString(i++, user.getRolesJson() != null ? user.getRolesJson() : "[\"ROLE_USER\"]");
            stmt.setString(i++, user.getFirstname());
            stmt.setString(i++, user.getLastname());
            stmt.setString(i++, user.getAccountStatus());
            if (user.getDateCreation() != null) {
                stmt.setTimestamp(i++, Timestamp.valueOf(user.getDateCreation()));
            } else {
                stmt.setNull(i++, Types.TIMESTAMP);
            }
            stmt.setString(i++, user.getGoogleAuthenticatorSecret());
            stmt.setString(i++, user.getPhoneNumber());
            stmt.setString(i++, user.getPhoto());
            stmt.setString(i++, user.getUsername());
            stmt.setBytes(i, UuidUtil.toBytes16(user.getId()));
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(User user) throws SQLException {
        String sql = "DELETE FROM `app_user` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(user.getId()));
            stmt.executeUpdate();
        }
    }

    /**
     * Deletes several users by id (same idea as Symfony {@code bulkDelete}).
     */
    public void deleteByIds(List<UUID> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String sql = "DELETE FROM `app_user` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            for (UUID id : ids) {
                stmt.setBytes(1, UuidUtil.toBytes16(id));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public void createPrepared(User user) throws SQLException {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        if (user.getRolesJson() == null || user.getRolesJson().isBlank()) {
            user.setRolesJson("[\"ROLE_USER\"]");
        }
        if (user.getDateCreation() == null) {
            user.setDateCreation(LocalDateTime.now(ZoneOffset.UTC));
        }
        if (user.getAccountStatus() == null || user.getAccountStatus().isBlank()) {
            user.setAccountStatus("active");
        }
        String sql = "INSERT INTO `app_user` (`id`, `email_email`, `password`, `roles`, `name_firstname`, "
                + "`name_lastname`, `account_status`, `date_creation`, `google_authenticator_secret`, "
                + "`phone_number`, `photo`, `username`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            int i = 1;
            stmt.setBytes(i++, UuidUtil.toBytes16(user.getId()));
            stmt.setString(i++, user.getEmail());
            stmt.setString(i++, hashPasswordIfPlain(user.getPassword()));
            stmt.setString(i++, user.getRolesJson());
            stmt.setString(i++, user.getFirstname());
            stmt.setString(i++, user.getLastname());
            stmt.setString(i++, user.getAccountStatus());
            stmt.setTimestamp(i++, Timestamp.valueOf(user.getDateCreation()));
            stmt.setString(i++, user.getGoogleAuthenticatorSecret());
            stmt.setString(i++, user.getPhoneNumber());
            stmt.setString(i++, user.getPhoto());
            stmt.setString(i, user.getUsername());
            stmt.executeUpdate();
        }
    }
}
