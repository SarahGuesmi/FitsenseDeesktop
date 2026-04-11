package utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Reads optional / aliased columns from {@link ResultSet} (e.g. Symfony vs Java naming).
 */
public final class ResultSetColumns {

    private ResultSetColumns() {
    }

    public static String getFirstString(ResultSet rs, String... candidates) throws SQLException {
        if (candidates == null || candidates.length == 0) {
            return null;
        }
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            String label = md.getColumnLabel(i);
            for (String c : candidates) {
                if (c != null && c.equalsIgnoreCase(label)) {
                    String v = rs.getString(i);
                    return rs.wasNull() ? null : v;
                }
            }
        }
        return null;
    }

    public static Object getFirstObject(ResultSet rs, String... candidates) throws SQLException {
        if (candidates == null || candidates.length == 0) {
            return null;
        }
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            String label = md.getColumnLabel(i);
            for (String c : candidates) {
                if (c != null && c.equalsIgnoreCase(label)) {
                    return rs.getObject(i);
                }
            }
        }
        return null;
    }

    public static String coalesceNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    /**
     * Normalizes DB gender values (varchar enum, tinyint, bit) to strings understood by the UI layer.
     */
    public static String normalizeGenderDbValue(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Boolean b) {
            return b ? "male" : "female";
        }
        if (o instanceof Number n) {
            int v = n.intValue();
            if (v == 1) {
                return "male";
            }
            if (v == 2) {
                return "female";
            }
            if (v == 0) {
                return "female";
            }
            return String.valueOf(v);
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
