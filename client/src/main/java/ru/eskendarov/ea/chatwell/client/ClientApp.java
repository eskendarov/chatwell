package ru.eskendarov.ea.chatwell.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {
    
    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/sample.fxml"));
        final Parent root = fxmlLoader.load();
        final Controller controller = fxmlLoader.getController();
        primaryStage.setOnCloseRequest(windowEvent -> {
            if (controller.getIn() != null) {
                controller.closeConnection();
                controller.getExecutor().shutdownNow();
            }
        });
        primaryStage.setTitle("ChatWell");
        primaryStage.setScene(new Scene(root, 900, 550));
        primaryStage.show();
    }
}