package services;

import models.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import utils.DbConnection;
import utils.UuidUtil;

import java.sql.*;
import java.time.LocalDateTime;
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
        LocalDateTime dateCreation = ts != null ? ts.toLocalDateTime() : null;
        UUID id = UuidUtil.fromResultSet(rs, "id");
        return new User(
                id,
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

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM `app_user` WHERE `email_email` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public User findByUsername(String username) throws SQLException {
        return findByEmail(username);
    }

    @Override
    public void create(User user) throws SQLException { createPrepared(user); }

    @Override
    public List<User> read() throws SQLException {
        String sql = "SELECT * FROM `app_user`";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            List<User> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        }
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE `app_user` SET `email_email`=?, `password`=?, `roles`=?, `name_firstname`=?, `name_lastname`=?, `account_status`=? WHERE `id`=?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, hashPasswordIfPlain(user.getPassword()));
            stmt.setString(3, user.getRolesJson() != null ? user.getRolesJson() : "[\"ROLE_USER\"]");
            stmt.setString(4, user.getFirstname());
            stmt.setString(5, user.getLastname());
            stmt.setString(6, user.getAccountStatus());
            stmt.setBytes(7, UuidUtil.toBytes16(user.getId()));
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(User user) throws SQLException {
        String sql = "DELETE FROM `app_user` WHERE `id`=?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(user.getId()));
            stmt.executeUpdate();
        }
    }

    @Override
    public void createPrepared(User user) throws SQLException {
        if (user.getId() == null) user.setId(UUID.randomUUID());
        if (user.getRolesJson() == null) user.setRolesJson("[\"ROLE_USER\"]");
        if (user.getDateCreation() == null) user.setDateCreation(LocalDateTime.now());
        if (user.getAccountStatus() == null) user.setAccountStatus("active");

        String sql = "INSERT INTO `app_user` (`id`,`email_email`,`password`,`roles`,`name_firstname`,`name_lastname`,`account_status`,`date_creation`) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(user.getId()));
            stmt.setString(2, user.getEmail());
            stmt.setString(3, hashPasswordIfPlain(user.getPassword()));
            stmt.setString(4, user.getRolesJson());
            stmt.setString(5, user.getFirstname());
            stmt.setString(6, user.getLastname());
            stmt.setString(7, user.getAccountStatus());
            stmt.setTimestamp(8, Timestamp.valueOf(user.getDateCreation()));
            stmt.executeUpdate();
        }
    }

    private String hashPasswordIfPlain(String password) {
        if (password == null) return null;
        if (password.startsWith("$2a$") || password.startsWith("$2y$") || password.startsWith("$2b$")) return password;
        return passwordEncoder.encode(password);
    }

    public void deleteByIds(List<UUID> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) return;
        String sql = "DELETE FROM `app_user` WHERE `id`=?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            for (UUID id : ids) {
                stmt.setBytes(1, UuidUtil.toBytes16(id));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}
