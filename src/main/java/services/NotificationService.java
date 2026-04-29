package services;

import models.Notification;
import utils.DbConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    private final Connection cnx;

    public NotificationService() {
        cnx = DbConnection.getInstance().getCnx();
        ensureTableExists();
    }

    /** Creates the notifications table if it doesn't exist yet. */
    private void ensureTableExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS `admin_notification` (
                    `id`           INT AUTO_INCREMENT PRIMARY KEY,
                    `type`         VARCHAR(64)   NOT NULL DEFAULT 'NEW_USER',
                    `message`      VARCHAR(512)  NOT NULL,
                    `target_email` VARCHAR(255)  NOT NULL DEFAULT '',
                    `is_read`      TINYINT(1)    NOT NULL DEFAULT 0,
                    `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
        try (Statement stmt = cnx.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Inserts a new notification. */
    public void create(Notification n) {
        String sql = "INSERT INTO `admin_notification` (`type`, `message`, `target_email`, `is_read`, `created_at`) VALUES (?, ?, ?, 0, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, n.getType());
            stmt.setString(2, n.getMessage());
            stmt.setString(3, n.getTargetEmail() == null ? "" : n.getTargetEmail());
            stmt.setTimestamp(4, Timestamp.valueOf(n.getCreatedAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Returns all notifications ordered newest first. */
    public List<Notification> readAll() {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM `admin_notification` ORDER BY `created_at` DESC";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Count of unread notifications. */
    public int countUnread() {
        String sql = "SELECT COUNT(*) FROM `admin_notification` WHERE `is_read` = 0";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** Marks a single notification as read. */
    public void markRead(int id) {
        String sql = "UPDATE `admin_notification` SET `is_read` = 1 WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Marks all notifications as read. */
    public void markAllRead() {
        String sql = "UPDATE `admin_notification` SET `is_read` = 1";
        try (Statement stmt = cnx.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setType(rs.getString("type"));
        n.setMessage(rs.getString("message"));
        n.setTargetEmail(rs.getString("target_email"));
        n.setRead(rs.getInt("is_read") == 1);
        Timestamp ts = rs.getTimestamp("created_at");
        n.setCreatedAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        return n;
    }
}
