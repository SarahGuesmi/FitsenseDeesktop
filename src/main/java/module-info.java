module org.example.fitsense {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.web;
    requires java.sql;
    requires mysql.connector.j;
    requires java.net.http;
    requires spring.security.crypto;
    requires commons.logging;
    requires com.google.gson;
    requires jdk.httpserver;
    requires org.json;

    opens app to javafx.graphics;
    opens controllers to javafx.fxml;
}
