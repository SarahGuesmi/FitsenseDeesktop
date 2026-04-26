package services;

import utils.DbConnection;

import java.nio.ByteBuffer;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
public class NotificationService {

    private final Connection cnx;

    public NotificationService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    public void createNotification(String userId, String message, String type) {
        try {
            String sql = """
                INSERT INTO notification
                (id, related_user_id, message, type, is_read, created_at)
                VALUES (?, ?, ?, ?, 0, ?)
            """;

            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setBytes(1, uuidToBytes(UUID.randomUUID().toString()));
            ps.setBytes(2, uuidToBytes(userId));
            ps.setString(3, message);
            ps.setString(4, type);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void markAllAsRead(String userId) {
        try {
            String sql = """
            UPDATE notification
            SET is_read = 1
            WHERE related_user_id = ?
        """;

            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setBytes(1, uuidToBytes(userId));
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public List<String> getUserNotifications(String userId) {
        List<String> list = new ArrayList<>();

        try {
            String sql = """
            SELECT message
            FROM notification
            WHERE related_user_id = ?
            ORDER BY created_at DESC
        """;

            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setBytes(1, uuidToBytes(userId));

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(rs.getString("message"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
    private byte[] uuidToBytes(String uuid) {
        UUID u = UUID.fromString(uuid);
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(u.getMostSignificantBits());
        bb.putLong(u.getLeastSignificantBits());
        return bb.array();
    }
}