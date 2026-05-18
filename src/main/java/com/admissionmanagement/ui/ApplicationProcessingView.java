package com.admissionmanagement.ui;

import com.admissionmanagement.application.processing.ApplicationScope;
import com.admissionmanagement.controller.processing.ApplicationProcessingController;
import com.admissionmanagement.domain.application.CommunicationResult;
import com.admissionmanagement.dto.ApplicationSearchCriteria;
import com.admissionmanagement.projection.ApplicationSummaryProjection;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class ApplicationProcessingView {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ApplicationProcessingController controller;
    private final BorderPane root;
    private final TabPane scopeTabs;
    private final TextField phoneField;
    private final TextField emailField;
    private final DatePicker dateFromPicker;
    private final DatePicker dateToPicker;
    private final ComboBox<CommunicationResult> communicationResultBox;
    private final VBox applicationsBox;
    private final Label stateLabel;

    public ApplicationProcessingView(ApplicationProcessingController controller) {
        this.controller = Objects.requireNonNull(controller);
        this.root = new BorderPane();
        this.scopeTabs = new TabPane();
        this.phoneField = new TextField();
        this.emailField = new TextField();
        this.dateFromPicker = new DatePicker();
        this.dateToPicker = new DatePicker();
        this.communicationResultBox = new ComboBox<>();
        this.applicationsBox = new VBox(10);
        this.stateLabel = new Label();

        configureView();
    }

    public Node getRoot() {
        return root;
    }

    public void loadInitialApplications() {
        loadApplications(selectedScope());
    }

    private void configureView() {
        root.setPadding(new Insets(24));
        root.setTop(createHeader());
        root.setCenter(createContent());
    }

    private Node createHeader() {
        Label title = new Label("Application Processing");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700;");
        BorderPane header = new BorderPane(title);
        header.setPadding(new Insets(0, 0, 18, 0));
        return header;
    }

    private Node createContent() {
        VBox content = new VBox(14);
        content.getChildren().addAll(createTabs(), createFilterPanel(), createApplicationsScrollPane());
        VBox.setVgrow(content.getChildren().get(2), Priority.ALWAYS);
        return content;
    }

    private Node createTabs() {
        scopeTabs.getTabs().addAll(
                createScopeTab("Pending", ApplicationScope.PENDING),
                createScopeTab("In Progress", ApplicationScope.IN_PROGRESS),
                createScopeTab("Processed", ApplicationScope.PROCESSED)
        );
        scopeTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        scopeTabs.setMinHeight(42);
        scopeTabs.setPrefHeight(42);
        scopeTabs.setMaxHeight(42);
        scopeTabs.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldTab, newTab) -> loadApplications(selectedScope()));
        return scopeTabs;
    }

    private Tab createScopeTab(String title, ApplicationScope scope) {
        Tab tab = new Tab(title);
        tab.setUserData(scope);
        return tab;
    }

    private Node createFilterPanel() {
        phoneField.setPromptText("Phone");
        emailField.setPromptText("Email");
        dateFromPicker.setPromptText("Date from");
        dateToPicker.setPromptText("Date to");
        communicationResultBox.setPromptText("Last result");
        communicationResultBox.getItems().setAll(CommunicationResult.values());

        Button searchButton = new Button("Search");
        searchButton.setDefaultButton(true);
        searchButton.setOnAction(event -> loadApplications(selectedScope()));

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(event -> {
            phoneField.clear();
            emailField.clear();
            dateFromPicker.setValue(null);
            dateToPicker.setValue(null);
            communicationResultBox.setValue(null);
            loadApplications(selectedScope());
        });

        FlowPane filters = new FlowPane(12, 10);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getChildren().addAll(
                labeledControl("Phone", phoneField),
                labeledControl("Email", emailField),
                labeledControl("Date From", dateFromPicker),
                labeledControl("Date To", dateToPicker),
                labeledControl("Last Result", communicationResultBox),
                searchButton,
                resetButton
        );
        filters.setPadding(new Insets(14));
        filters.setStyle("""
                -fx-background-color: #f7f8fa;
                -fx-border-color: #dde1e6;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                """);
        return filters;
    }

    private Node labeledControl(String labelText, Node control) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");

        if (control instanceof Region region) {
            region.setMinWidth(150);
        }

        VBox wrapper = new VBox(4, label, control);
        wrapper.setAlignment(Pos.BOTTOM_LEFT);
        return wrapper;
    }

    private Node createApplicationsScrollPane() {
        applicationsBox.setPadding(new Insets(4, 2, 4, 2));

        VBox scrollContent = new VBox(12, stateLabel, applicationsBox);
        scrollContent.setPadding(new Insets(2));

        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    private ApplicationScope selectedScope() {
        Tab selectedTab = scopeTabs.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            return ApplicationScope.PENDING;
        }
        return (ApplicationScope) selectedTab.getUserData();
    }

    private void loadApplications(ApplicationScope scope) {
        ApplicationSearchCriteria criteria = readCriteria();
        stateLabel.setText("Loading applications...");
        applicationsBox.getChildren().clear();

        Task<List<ApplicationSummaryProjection>> task = new Task<>() {
            @Override
            protected List<ApplicationSummaryProjection> call() {
                return controller.getApplications(scope, criteria);
            }
        };

        task.setOnSucceeded(event -> renderApplications(scope, task.getValue()));
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            stateLabel.setText("Could not load applications: " + exception.getMessage());
        });

        Thread thread = new Thread(task, "load-applications");
        thread.setDaemon(true);
        thread.start();
    }

    private ApplicationSearchCriteria readCriteria() {
        LocalDate fromDate = dateFromPicker.getValue();
        LocalDate toDate = dateToPicker.getValue();
        LocalDateTime dateFrom = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime dateTo = toDate == null ? null : toDate.atTime(LocalTime.MAX);

        return new ApplicationSearchCriteria(
                blankToNull(phoneField.getText()),
                blankToNull(emailField.getText()),
                communicationResultBox.getValue(),
                dateFrom,
                dateTo
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void renderApplications(ApplicationScope scope, List<ApplicationSummaryProjection> applications) {
        applicationsBox.getChildren().clear();
        if (applications.isEmpty()) {
            stateLabel.setText("No applications found.");
            return;
        }

        stateLabel.setText(applications.size() + " applications found");
        applications.stream()
                .map(application -> createApplicationCard(scope, application))
                .forEach(card -> applicationsBox.getChildren().add(card));
    }

    private Node createApplicationCard(ApplicationScope scope, ApplicationSummaryProjection application) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #d0d7de;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                """);

        Label applicant = new Label(application.fullName());
        applicant.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");

        Label program = new Label("Program: " + application.programName());
        Label created = new Label("Created: " + application.createdAt().format(DATE_TIME_FORMATTER));
        Label status = new Label("Status: " + application.status());
        Label communication = new Label("Last communication: " + formatLastCommunication(application));

        VBox details = new VBox(4, program, created, status, communication);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getChildren().add(new Button("Details"));
        if (scope == ApplicationScope.PENDING) {
            actions.getChildren().add(new Button("Start Processing"));
        } else if (scope == ApplicationScope.IN_PROGRESS) {
            actions.getChildren().addAll(new Button("Record communication"), new Button("Finish Processing"));
        }

        card.getChildren().addAll(applicant, details, actions);
        return card;
    }

    private String formatLastCommunication(ApplicationSummaryProjection application) {
        if (application.lastCommunicationResult() == null) {
            return "None";
        }
        return application.lastCommunicationResult().name();
    }
}
