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

    // ── User-scoped notification methods (feature/favorites) ─────────────────

    /** Creates a notification for a specific user (UUID-based, `notification` table). */
    public void createNotification(String userId, String message, String type) {
        try {
            String sql = """
                INSERT INTO notification
                (id, related_user_id, message, type, is_read, created_at)
                VALUES (?, ?, ?, ?, 0, ?)
            """;
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setBytes(1, uuidToBytes(java.util.UUID.randomUUID().toString()));
            ps.setBytes(2, uuidToBytes(userId));
            ps.setString(3, message);
            ps.setString(4, type);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Marks all user notifications as read. */
    public void markAllAsRead(String userId) {
        try {
            String sql = "UPDATE notification SET is_read = 1 WHERE related_user_id = ?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setBytes(1, uuidToBytes(userId));
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Returns all messages for a user ordered newest first. */
    public List<String> getUserNotifications(String userId) {
        List<String> list = new ArrayList<>();
        try {
            String sql = "SELECT message FROM notification WHERE related_user_id = ? ORDER BY created_at DESC";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setBytes(1, uuidToBytes(userId));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getString("message"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static byte[] uuidToBytes(String uuid) {
        java.util.UUID u = java.util.UUID.fromString(uuid);
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(new byte[16]);
        bb.putLong(u.getMostSignificantBits());
        bb.putLong(u.getLeastSignificantBits());
        return bb.array();
    }
}
