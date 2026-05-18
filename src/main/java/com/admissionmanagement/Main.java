package com.admissionmanagement;

import com.admissionmanagement.application.processing.ApplicationProcessingService;
import com.admissionmanagement.controller.processing.ApplicationProcessingController;
import com.admissionmanagement.infrastructure.config.DatabaseConnectionFactory;
import com.admissionmanagement.infrastructure.repository.JdbcApplicationRepository;
import com.admissionmanagement.infrastructure.repository.JdbcApplicationQueryRepository;
import com.admissionmanagement.repository.ApplicationQueryRepository;
import com.admissionmanagement.repository.ApplicationRepository;
import com.admissionmanagement.ui.ApplicationProcessingView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        DatabaseConnectionFactory connectionFactory = DatabaseConnectionFactory.fromApplicationProperties();
        ApplicationRepository applicationRepository = new JdbcApplicationRepository(connectionFactory);
        ApplicationQueryRepository queryRepository =
                new JdbcApplicationQueryRepository(connectionFactory);
        ApplicationProcessingService processingService =
                new ApplicationProcessingService(applicationRepository, queryRepository);
        ApplicationProcessingController processingController = new ApplicationProcessingController(processingService);
        ApplicationProcessingView processingView = new ApplicationProcessingView(processingController);

        BorderPane root = new BorderPane();
        root.setLeft(createSidebar());
        root.setCenter(processingView.getRoot());

        Scene scene = new Scene(root, 1100, 720);

        stage.setTitle("Admission Management");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setMaximized(true);
        stage.setFullScreenExitHint("Press F11 to exit full screen");
        stage.show();

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });
        processingView.loadInitialApplications();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private VBox createSidebar() {
        Label title = new Label("Admissions");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: white;");

        Button processingButton = createNavigationButton("Processing");
        processingButton.setDisable(true);
        Button journalButton = createNavigationButton("Event Journal");
        journalButton.setDisable(true);
        Button analyticsButton = createNavigationButton("Analytics");
        analyticsButton.setDisable(true);

        VBox sidebar = new VBox(10, title, processingButton, journalButton, analyticsButton);
        sidebar.setPrefWidth(190);
        sidebar.setStyle("""
                -fx-background-color: #20242a;
                -fx-padding: 24 16 24 16;
                """);
        VBox.setVgrow(analyticsButton, Priority.NEVER);
        return sidebar;
    }

    private Button createNavigationButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle("""
                -fx-alignment: center-left;
                -fx-background-color: transparent;
                -fx-text-fill: white;
                -fx-font-size: 14px;
                """);
        return button;
    }
}
