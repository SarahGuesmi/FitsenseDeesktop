package services;

import models.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import utils.DbConnection;

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
        // id is int, wrap as UUID for compatibility
        int intId = rs.getInt("id");
        UUID id = new UUID(0, intId);
        return new User(
                id,
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("roles"),
                rs.getString("firstname"),
                rs.getString("lastname"),
                rs.getString("account_status"),
                dateCreation,
                null, null, null, null);
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE `email` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE `email` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, username);
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
        String sql = "SELECT * FROM `user`";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            List<User> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        }
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE `user` SET `email` = ?, `password` = ?, `roles` = ?, "
                + "`firstname` = ?, `lastname` = ?, `account_status` = ?, `date_creation` = ? "
                + "WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, hashPasswordIfPlain(user.getPassword()));
            stmt.setString(3, user.getRolesJson() != null ? user.getRolesJson() : "[\"ROLE_USER\"]");
            stmt.setString(4, user.getFirstname());
            stmt.setString(5, user.getLastname());
            stmt.setString(6, user.getAccountStatus());
            stmt.setTimestamp(7, user.getDateCreation() != null ? Timestamp.valueOf(user.getDateCreation()) : null);
            stmt.setInt(8, (int) user.getId().getLeastSignificantBits());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(User user) throws SQLException {
        String sql = "DELETE FROM `user` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, (int) user.getId().getLeastSignificantBits());
            stmt.executeUpdate();
        }
    }

    @Override
    public void createPrepared(User user) throws SQLException {
        if (user.getRolesJson() == null || user.getRolesJson().isBlank()) {
            user.setRolesJson("[\"ROLE_USER\"]");
        }
        if (user.getDateCreation() == null) {
            user.setDateCreation(LocalDateTime.now());
        }
        if (user.getAccountStatus() == null || user.getAccountStatus().isBlank()) {
            user.setAccountStatus("active");
        }
        String sql = "INSERT INTO `user` (`email`, `password`, `roles`, `firstname`, `lastname`, "
                + "`account_status`, `date_creation`) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, hashPasswordIfPlain(user.getPassword()));
            stmt.setString(3, user.getRolesJson());
            stmt.setString(4, user.getFirstname());
            stmt.setString(5, user.getLastname());
            stmt.setString(6, user.getAccountStatus());
            stmt.setTimestamp(7, Timestamp.valueOf(user.getDateCreation()));
            stmt.executeUpdate();
            // Retrieve generated int id and store as UUID
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    user.setId(new java.util.UUID(0, generatedId));
                }
            }
        }
    }

    private String hashPasswordIfPlain(String password) {
        if (password == null) return null;
        if (password.startsWith("$2a$") || password.startsWith("$2y$") || password.startsWith("$2b$")) {
            return password;
        }
        return passwordEncoder.encode(password);
    }

    public void deleteByIds(List<UUID> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) return;
        String sql = "DELETE FROM `user` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            for (UUID id : ids) {
                stmt.setInt(1, (int) id.getLeastSignificantBits());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}
