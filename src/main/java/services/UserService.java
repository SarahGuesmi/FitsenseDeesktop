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

        // id is UUID (CHAR 36 or BINARY 16) in the new schema
        UUID id = UuidUtil.fromResultSet(rs, "id");

        User user = new User(
                id,
                rs.getString("email_email"),
                rs.getString("password"),
                rs.getString("roles"),
                rs.getString("name_firstname"),
                rs.getString("name_lastname"),
                rs.getString("account_status"),
                dateCreation,
                ResultSetColumns.getFirstString(rs, "google_authenticator_secret"),
                null, // phone_number
                ResultSetColumns.getFirstString(rs, "photo"),
                ResultSetColumns.getFirstString(rs, "username"));

        // objective and gender are now directly in app_user
        String obj = ResultSetColumns.getFirstString(rs, "objective");
        user.setAccountObjective(obj);

        Object genderObj = ResultSetColumns.getFirstObject(rs, "gender");
        user.setAccountGender(ResultSetColumns.normalizeGenderDbValue(genderObj));

        return user;
    }

    private String hashPasswordIfPlain(String password) {
        if (password == null) return null;
        if (password.startsWith("$2a$") || password.startsWith("$2y$") || password.startsWith("$2b$")) {
            return password;
        }
        return passwordEncoder.encode(password);
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
        String sql = "SELECT * FROM `app_user` WHERE `username` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return findByEmail(username);
    }

    public User findById(UUID id) throws SQLException {
        String sql = "SELECT * FROM `app_user` WHERE `id` = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, id.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
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
            while (rs.next()) list.add(mapRow(rs));
            return list;
        }
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE `app_user` SET `email_email` = ?, `password` = ?, `roles` = ?, "
                + "`name_firstname` = ?, `name_lastname` = ?, `account_status` = ?, "
                + "`objective` = ?, `gender` = ? "
                + "WHERE `id` = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, hashPasswordIfPlain(user.getPassword()));
            stmt.setString(3, user.getRolesJson() != null ? user.getRolesJson() : "[\"ROLE_USER\"]");
            stmt.setString(4, user.getFirstname());
            stmt.setString(5, user.getLastname());
            stmt.setString(6, user.getAccountStatus());
            stmt.setString(7, user.getAccountObjective());
            stmt.setString(8, user.getAccountGender());
            stmt.setString(9, user.getId().toString());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(User user) throws SQLException {
        String sql = "DELETE FROM `app_user` WHERE `id` = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, user.getId().toString());
            stmt.executeUpdate();
        }
    }

    public void deleteByIds(List<UUID> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) return;
        String sql = "DELETE FROM `app_user` WHERE `id` = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            for (UUID id : ids) {
                stmt.setString(1, id.toString());
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
        // Generate UUID if not set
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        String sql = "INSERT INTO `app_user` (`id`, `email_email`, `password`, `roles`, "
                + "`name_firstname`, `name_lastname`, `account_status`, `username`, `date_creation`) "
                + "VALUES (UNHEX(REPLACE(?, '-', '')), ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, user.getId().toString());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, hashPasswordIfPlain(user.getPassword()));
            stmt.setString(4, user.getRolesJson());
            stmt.setString(5, user.getFirstname());
            stmt.setString(6, user.getLastname());
            stmt.setString(7, user.getAccountStatus());
            stmt.setString(8, user.getUsername());
            stmt.executeUpdate();
        }
    }
}

