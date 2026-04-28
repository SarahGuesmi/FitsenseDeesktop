module org.example.fitsense {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires mysql.connector.j;
    requires java.net.http;
    requires spring.security.crypto;
    requires commons.logging;
    requires com.google.gson;

    opens app to javafx.graphics;
    opens controllers to javafx.fxml;
}
