package utils;

import java.sql.*;

public class DbDebug {
    public static void main(String[] args) {
        try {
            Connection cnx = DbConnection.getInstance().getCnx();
            DatabaseMetaData meta = cnx.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "chat_message", null);
            System.out.println("Columns in chat_message:");
            while (rs.next()) {
                System.out.println("- " + rs.getString("COLUMN_NAME") + " (" + rs.getString("TYPE_NAME") + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
