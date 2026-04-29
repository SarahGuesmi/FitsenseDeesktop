package services;

import models.ChatMessage;
import utils.DbConnection;
import utils.UuidUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatService {

    private final Connection cnx;

    public ChatService() {
        cnx = DbConnection.getInstance().getCnx();
        ensureTableExists();
    }

    private void ensureTableExists() {
        try (Statement stmt = cnx.createStatement()) {
            // Add is_read column if it doesn't exist
            try {
                stmt.execute("ALTER TABLE `chat_message` ADD COLUMN `is_read` TINYINT(1) DEFAULT 0");
            } catch (SQLException ignored) {
                // Column probably already exists
            }
        } catch (SQLException e) {
            System.err.println("ChatService.ensureTableExists: " + e.getMessage());
        }
    }

    /** Returns all messages between two users, ordered oldest first. */
    public List<ChatMessage> getConversation(UUID userA, UUID userB) throws SQLException {
        String sql = "SELECT * FROM `chat_message` WHERE "
                + "((`sender_id` = ? AND `receiver_id` = ?) OR "
                + "(`sender_id` = ? AND `receiver_id` = ?)) "
                + "AND `is_deleted` = 0 "
                + "ORDER BY `created_at` ASC";
        List<ChatMessage> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(userA));
            ps.setBytes(2, UuidUtil.toBytes16(userB));
            ps.setBytes(3, UuidUtil.toBytes16(userB));
            ps.setBytes(4, UuidUtil.toBytes16(userA));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Returns messages newer than a given timestamp (for polling). */
    public List<ChatMessage> getNewMessages(UUID userA, UUID userB, LocalDateTime after) throws SQLException {
        String sql = "SELECT * FROM `chat_message` WHERE "
                + "((`sender_id` = ? AND `receiver_id` = ?) OR "
                + "(`sender_id` = ? AND `receiver_id` = ?)) "
                + "AND `is_deleted` = 0 "
                + "AND `created_at` > ? ORDER BY `created_at` ASC";
        List<ChatMessage> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(userA));
            ps.setBytes(2, UuidUtil.toBytes16(userB));
            ps.setBytes(3, UuidUtil.toBytes16(userB));
            ps.setBytes(4, UuidUtil.toBytes16(userA));
            ps.setTimestamp(5, Timestamp.valueOf(after));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public ChatMessage send(UUID senderId, UUID receiverId, String content) throws SQLException {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String sql = "INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `is_deleted`, `created_at`, `updated_at`) "
                + "VALUES (?, ?, ?, ?, 0, NOW(), NOW())";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(id));
            ps.setBytes(2, UuidUtil.toBytes16(senderId));
            ps.setBytes(3, UuidUtil.toBytes16(receiverId));
            ps.setString(4, content);
            ps.executeUpdate();
        }
        ChatMessage msg = new ChatMessage();
        msg.setId(id);
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setSentAt(now);
        return msg;
    }

    public void edit(UUID messageId, String newContent) throws SQLException {
        String sql = "UPDATE `chat_message` SET `content` = ?, `updated_at` = ? WHERE `id` = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, newContent);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setBytes(3, UuidUtil.toBytes16(messageId));
            ps.executeUpdate();
        }
    }

    public void deleteMessage(UUID messageId) throws SQLException {
        String sql = "UPDATE `chat_message` SET `is_deleted` = 1, `updated_at` = ? WHERE `id` = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setBytes(2, UuidUtil.toBytes16(messageId));
            ps.executeUpdate();
        }
    }

    public void deleteConversation(UUID userA, UUID userB) throws SQLException {
        String sql = "UPDATE `chat_message` SET `is_deleted` = 1, `updated_at` = ? WHERE "
                + "(`sender_id` = ? AND `receiver_id` = ?) OR "
                + "(`sender_id` = ? AND `receiver_id` = ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setBytes(2, UuidUtil.toBytes16(userA));
            ps.setBytes(3, UuidUtil.toBytes16(userB));
            ps.setBytes(4, UuidUtil.toBytes16(userB));
            ps.setBytes(5, UuidUtil.toBytes16(userA));
            ps.executeUpdate();
        }
    }

    public List<ChatMessage> getAllMessages() throws SQLException {
        String sql = "SELECT * FROM `chat_message` WHERE `is_deleted` = 0 ORDER BY `created_at` ASC";
        List<ChatMessage> list = new ArrayList<>();
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void markAsRead(UUID senderId, UUID receiverId) throws SQLException {
        String sql = "UPDATE `chat_message` SET `is_read` = 1 WHERE `sender_id` = ? AND `receiver_id` = ? AND `is_read` = 0";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(senderId));
            ps.setBytes(2, UuidUtil.toBytes16(receiverId));
            ps.executeUpdate();
        }
    }

    public int countUnread(UUID senderId, UUID receiverId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM `chat_message` WHERE `sender_id` = ? AND `receiver_id` = ? AND `is_read` = 0 AND `is_deleted` = 0";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(senderId));
            ps.setBytes(2, UuidUtil.toBytes16(receiverId));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public LocalDateTime getLastMessageTime(UUID userA, UUID userB) throws SQLException {
        String sql = "SELECT MAX(`created_at`) FROM `chat_message` WHERE "
                + "((`sender_id` = ? AND `receiver_id` = ?) OR "
                + "(`sender_id` = ? AND `receiver_id` = ?)) AND `is_deleted` = 0";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes16(userA));
            ps.setBytes(2, UuidUtil.toBytes16(userB));
            ps.setBytes(3, UuidUtil.toBytes16(userB));
            ps.setBytes(4, UuidUtil.toBytes16(userA));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    return ts != null ? ts.toLocalDateTime() : null;
                }
            }
        }
        return null;
    }

    private static ChatMessage mapRow(ResultSet rs) throws SQLException {
        ChatMessage m = new ChatMessage();
        m.setId(UuidUtil.fromResultSet(rs, "id"));
        m.setSenderId(UuidUtil.fromResultSet(rs, "sender_id"));
        m.setReceiverId(UuidUtil.fromResultSet(rs, "receiver_id"));
        m.setContent(rs.getString("content"));
        Timestamp ts = rs.getTimestamp("created_at");
        m.setSentAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        return m;
    }
}
