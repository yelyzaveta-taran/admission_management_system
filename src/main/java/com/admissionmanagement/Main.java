package com.admissionmanagement;

import com.admissionmanagement.application.analytics.ApplicationAnalyticsService;
import com.admissionmanagement.application.processing.ApplicationProcessingService;
import com.admissionmanagement.controller.analytics.ApplicationAnalyticsController;
import com.admissionmanagement.controller.processing.ApplicationProcessingController;
import com.admissionmanagement.infrastructure.config.DatabaseConnectionFactory;
import com.admissionmanagement.infrastructure.repository.JdbcApplicationRepository;
import com.admissionmanagement.infrastructure.repository.JdbcApplicationQueryRepository;
import com.admissionmanagement.repository.ApplicationQueryRepository;
import com.admissionmanagement.repository.ApplicationRepository;
import com.admissionmanagement.ui.view.analytics.AnalyticsDashboardView;
import com.admissionmanagement.ui.view.EventJournalView;
import com.admissionmanagement.ui.view.processing.ApplicationProcessingView;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.TimeZone;

public class Main extends Application {
    private static final String APPLICATION_TIME_ZONE = "Europe/Kyiv";
    private static final double DEFAULT_STAGE_WIDTH_RATIO = 0.80;
    private static final double DEFAULT_STAGE_HEIGHT_RATIO = 0.86;
    private static final double MIN_STAGE_WIDTH = 900;
    private static final double MIN_STAGE_HEIGHT = 600;
    private static final String DEFAULT_NAVIGATION_BUTTON_STYLE = """
            -fx-alignment: center-left;
            -fx-background-color: transparent;
            -fx-text-fill: #c7cdd4;
            -fx-font-size: 14px;
            -fx-font-weight: 400;
            """;
    private static final String SELECTED_NAVIGATION_BUTTON_STYLE = """
            -fx-alignment: center-left;
            -fx-background-color: #343a42;
            -fx-background-radius: 6;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: 700;
            """;

    @Override
    public void start(Stage stage) {
        DatabaseConnectionFactory connectionFactory = DatabaseConnectionFactory.fromApplicationProperties();
        ApplicationRepository applicationRepository = new JdbcApplicationRepository(connectionFactory);
        ApplicationQueryRepository queryRepository =
                new JdbcApplicationQueryRepository(connectionFactory);
        ApplicationProcessingService processingService =
                new ApplicationProcessingService(applicationRepository, queryRepository);
        ApplicationAnalyticsService analyticsService =
                new ApplicationAnalyticsService(queryRepository);
        ApplicationProcessingController processingController = new ApplicationProcessingController(processingService);
        ApplicationAnalyticsController analyticsController = new ApplicationAnalyticsController(analyticsService);
        ApplicationProcessingView processingView = new ApplicationProcessingView(processingController);
        EventJournalView eventJournalView = new EventJournalView(processingController);
        AnalyticsDashboardView analyticsView = new AnalyticsDashboardView(analyticsController);

        BorderPane root = new BorderPane();
        root.setLeft(createSidebar(root, processingView, eventJournalView, analyticsView));
        root.setCenter(processingView.getRoot());

        Scene scene = new Scene(root, 1100, 720);

        stage.setTitle("Admission Management");
        stage.setScene(scene);
        configureStage(stage);
        stage.show();

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });
        processingView.loadInitialApplications();
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(APPLICATION_TIME_ZONE));
        launch(args);
    }

    private void configureStage(Stage stage) {
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double width = Math.max(MIN_STAGE_WIDTH, visualBounds.getWidth() * DEFAULT_STAGE_WIDTH_RATIO);
        double height = Math.max(MIN_STAGE_HEIGHT, visualBounds.getHeight() * DEFAULT_STAGE_HEIGHT_RATIO);
        width = Math.min(width, visualBounds.getWidth());
        height = Math.min(height, visualBounds.getHeight());

        stage.setResizable(true);
        stage.setMinWidth(MIN_STAGE_WIDTH);
        stage.setMinHeight(MIN_STAGE_HEIGHT);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(visualBounds.getMinX() + (visualBounds.getWidth() - width) / 2);
        stage.setY(visualBounds.getMinY() + (visualBounds.getHeight() - height) / 2);
        stage.setFullScreenExitHint("Press F11 to exit full screen");
    }

    private VBox createSidebar(
            BorderPane root,
            ApplicationProcessingView processingView,
            EventJournalView eventJournalView,
            AnalyticsDashboardView analyticsView
    ) {
        Label title = new Label("Admissions");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: white;");

        Button processingButton = createNavigationButton("Processing");
        Button journalButton = createNavigationButton("Event Journal");
        Button analyticsButton = createNavigationButton("Analytics");

        processingButton.setOnAction(event -> {
            root.setCenter(processingView.getRoot());
            selectNavigationButton(processingButton, journalButton, analyticsButton);
            processingView.loadInitialApplications();
        });
        journalButton.setOnAction(event -> {
            root.setCenter(eventJournalView.getRoot());
            selectNavigationButton(journalButton, processingButton, analyticsButton);
            eventJournalView.loadEvents();
        });
        analyticsButton.setOnAction(event -> {
            root.setCenter(analyticsView.getRoot());
            selectNavigationButton(analyticsButton, processingButton, journalButton);
            analyticsView.loadAnalytics();
        });
        selectNavigationButton(processingButton, journalButton, analyticsButton);

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
        button.setStyle(DEFAULT_NAVIGATION_BUTTON_STYLE);
        return button;
    }

    private void selectNavigationButton(Button selectedButton, Button... otherButtons) {
        selectedButton.setStyle(SELECTED_NAVIGATION_BUTTON_STYLE);
        for (Button button : otherButtons) {
            button.setStyle(DEFAULT_NAVIGATION_BUTTON_STYLE);
        }
    }
}
