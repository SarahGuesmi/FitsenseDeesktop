module org.example.fitsense {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires mysql.connector.j;
    requires spring.security.crypto;
    requires commons.logging;

    opens app to javafx.graphics;
    opens controllers to javafx.fxml;
}
