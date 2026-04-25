package utils;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DbConnection {

    public Connection cnx;
    public static DbConnection instance;

    private DbConnection() {
        try {
            Properties props = loadConfig();
            String url = props.getProperty("db.url",
                    "jdbc:mysql://127.0.0.1:3306/fitsensee?serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true&useSSL=false");
            String user = props.getProperty("db.user", "root");
            String password = props.getProperty("db.password", "");

            cnx = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to database: " + url);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw new RuntimeException("Cannot connect to database: " + e.getMessage(), e);
        }
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = DbConnection.class.getResourceAsStream("/config.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}
        return props;
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
