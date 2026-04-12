package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {

    /**
     * Override with {@code -Dfitsense.db.url=jdbc:mysql://host:port/db?serverTimezone=UTC}
     */
    public static final String DB_URL = System.getProperty(
            "fitsense.db.url",
            "jdbc:mysql://127.0.0.1:3306/fitsense?serverTimezone=UTC&characterEncoding=utf8");
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "root";

    public Connection cnx;
    public static DbConnection instance;

    private DbConnection() {
        try {
            cnx = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("FitSense connected: " + DB_URL);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw new RuntimeException("Cannot connect to database: " + e.getMessage(), e);
        }
    }

    public Connection getCnx() {
        return cnx;
    }

    public static DbConnection getInstance() {
        if (instance == null)
            instance = new DbConnection();
        return instance;
    }
}
