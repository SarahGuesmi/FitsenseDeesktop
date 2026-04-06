package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {

    public static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/fitsense?serverTimezone=UTC&characterEncoding=utf8";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "root";

    public Connection cnx;
    public static DbConnection instance;

    private DbConnection() {
        try {
            cnx = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to database");
        } catch (SQLException e) {
            System.out.println("error" + e.getMessage());
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
