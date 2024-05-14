module com.nanfito.inveso {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;  // Often needed explicitly

    // Spring Boot modules - using generic module names (adjust based on actual automatic module names)
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.web;

    // Jackson JSON modules
    requires com.fasterxml.jackson.databind;

    // Logging
    requires org.slf4j;  // Make sure SLF4J modules are correct, might need to check actual JAR for automatic module name

    // HTTP components
    requires org.apache.httpcomponents.httpclient;

    // Standard Java modules
    requires java.net.http;
    requires static lombok;
    requires spring.beans;
    requires org.apache.httpcomponents.httpclient.fluent;
    requires org.apache.httpcomponents.httpcore;
    requires org.json;
    requires java.desktop;

    // Open packages for reflection, necessary for frameworks like Spring and JavaFX
    opens com.nanfito.inveso to javafx.fxml, spring.core;

    // Export your main package
    exports com.nanfito.inveso;
}
