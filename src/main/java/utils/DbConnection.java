package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {

    public static final String DB_URL =
            "jdbc:mysql://127.0.0.1:3306/fitsense?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "root";

    private Connection cnx;
    private static DbConnection instance;

    private DbConnection() {
        try {
            cnx = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to database");
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            System.err.println("SQLState=" + e.getSQLState() + ", errorCode=" + e.getErrorCode());
        }
    }

    /**
     * Quick health check after connect (server reachable, credentials OK, DB exists).
     */
    public boolean isAlive() {
        if (cnx == null) {
            return false;
        }
        try {
            return cnx.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    public Connection getCnx() {
        return cnx;
    }

    public static DbConnection getInstance() {
        if (instance == null) {
            instance = new DbConnection();
        }
        return instance;
    }

    /** Releases the JDBC connection (call at end of a CLI run to help the JVM exit cleanly). */
    public void closeQuietly() {
        if (cnx != null) {
            try {
                cnx.close();
            } catch (SQLException ignored) {
            }
            cnx = null;
        }
    }
}
