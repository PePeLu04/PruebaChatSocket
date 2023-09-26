module org.example {
    requires javafx.controls;
    requires javafx.fxml;

    exports org.example.model;
    opens org.example to javafx.fxml;
    exports org.example;
}
