package utils;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Doctrine/Symfony often stores UUIDs as BINARY(16); some setups use CHAR(36).
 */
public final class UuidUtil {

    private UuidUtil() {
    }

    public static UUID fromResultSet(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        if (o == null || rs.wasNull()) {
            return null;
        }
        if (o instanceof byte[] bytes) {
            if (bytes.length == 16) {
                return fromBytes16(bytes);
            }
            if (bytes.length == 36) {
                return UUID.fromString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        if (o instanceof String s && !s.isBlank()) {
            return UUID.fromString(s.trim());
        }
        throw new SQLException("Unsupported UUID storage for column " + column + ": " + o.getClass().getName());
    }

    public static byte[] toBytes16(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID fromBytes16(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }
}
