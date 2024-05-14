package com.nanfito.inveso;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan("com.nanfito.inveso")
public class Application extends javafx.application.Application {

    @Getter
    private static ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        // Initialize Spring Application Context
        springContext = new SpringApplicationBuilder(com.nanfito.inveso.Application.class).run();
        // Check if RestTemplate bean is loaded
        RestTemplate restTemplate = springContext.getBean(RestTemplate.class);
        if (restTemplate == null) {
            System.err.println("RestTemplate bean is not initialized!");
        } else {
            System.out.println("RestTemplate bean is successfully initialized.");
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Ensure FXMLLoader is set up with a controller factory that uses Spring context
        FXMLLoader stock = new FXMLLoader(getClass().getResource("/com/nanfito/inveso/Stock.fxml"));
        stock.setControllerFactory(springContext::getBean);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nanfito/inveso/MainMenu.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);

        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        //primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}