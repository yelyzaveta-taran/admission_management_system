package com.admissionmanagement;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("Admission Management");
        StackPane root = new StackPane(title);
        Scene scene = new Scene(root, 900, 600);

        stage.setTitle("Admission Management");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
