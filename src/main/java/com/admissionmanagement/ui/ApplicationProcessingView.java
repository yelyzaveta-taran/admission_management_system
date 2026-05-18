package com.admissionmanagement.ui;

import com.admissionmanagement.application.processing.ApplicationScope;
import com.admissionmanagement.controller.processing.ApplicationProcessingController;
import com.admissionmanagement.domain.application.ApplicationStatus;
import com.admissionmanagement.domain.application.CommunicationChannel;
import com.admissionmanagement.domain.application.CommunicationResult;
import com.admissionmanagement.dto.ApplicationSearchCriteria;
import com.admissionmanagement.dto.CommunicationRequest;
import com.admissionmanagement.projection.ApplicationDetailsProjection;
import com.admissionmanagement.projection.ApplicationSummaryProjection;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
        return labeledControl(label, control);
    }

    private Node labeledControl(Label label, Node control) {
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
        Button detailsButton = new Button("Details");
        detailsButton.setOnAction(event -> loadApplicationDetails(application.applicationId(), detailsButton));
        actions.getChildren().add(detailsButton);
        if (scope == ApplicationScope.PENDING) {
            Button startProcessingButton = new Button("Start Processing");
            startProcessingButton.setOnAction(event ->
                    startApplicationProcessing(application.applicationId(), startProcessingButton));
            actions.getChildren().add(startProcessingButton);
        } else if (scope == ApplicationScope.IN_PROGRESS) {
            Button recordCommunicationButton = new Button("Record Communication");
            recordCommunicationButton.setOnAction(event -> openRecordCommunicationDialog(
                    recordCommunicationButton.getScene().getWindow(),
                    application.applicationId(),
                    application.fullName(),
                    application.programName(),
                    () -> loadApplications(selectedScope())
            ));
            actions.getChildren().addAll(recordCommunicationButton, new Button("Finish Processing"));
        }

        card.getChildren().addAll(applicant, details, actions);
        return card;
    }

    private void loadApplicationDetails(Integer applicationId, Button detailsButton) {
        Window owner = detailsButton.getScene().getWindow();
        detailsButton.setDisable(true);
        detailsButton.setText("Loading...");

        Task<Optional<ApplicationDetailsProjection>> task = new Task<>() {
            @Override
            protected Optional<ApplicationDetailsProjection> call() {
                return controller.getApplicationDetails(applicationId);
            }
        };

        task.setOnSucceeded(event -> {
            detailsButton.setDisable(false);
            detailsButton.setText("Details");
            Optional<ApplicationDetailsProjection> details = task.getValue();
            if (details.isEmpty()) {
                showError(owner, "Application was not found.");
                return;
            }
            createDetailsDialog(owner, details.get()).showAndWait();
        });
        task.setOnFailed(event -> {
            detailsButton.setDisable(false);
            detailsButton.setText("Details");
            Throwable exception = task.getException();
            showError(owner, "Could not load application details: " + exception.getMessage());
        });

        Thread thread = new Thread(task, "load-application-details");
        thread.setDaemon(true);
        thread.start();
    }

    private void startApplicationProcessing(Integer applicationId, Button startProcessingButton) {
        Window owner = startProcessingButton.getScene().getWindow();
        startProcessingButton.setDisable(true);
        startProcessingButton.setText("Starting...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                controller.startApplicationProcessing(applicationId);
                return null;
            }
        };

        task.setOnSucceeded(event -> loadApplications(selectedScope()));
        task.setOnFailed(event -> {
            startProcessingButton.setDisable(false);
            startProcessingButton.setText("Start Processing");
            Throwable exception = task.getException();
            showError(owner, "Could not start application processing: " + exception.getMessage());
        });

        Thread thread = new Thread(task, "start-application-processing");
        thread.setDaemon(true);
        thread.start();
    }

    private Dialog<ButtonType> createDetailsDialog(Window owner, ApplicationDetailsProjection details) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Application Details");
        dialog.setHeaderText(details.fullName());
        dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(createDetailsContent(dialog, details));
        dialog.getDialogPane().setPrefWidth(560);
        return dialog;
    }

    private Node createDetailsContent(Dialog<ButtonType> dialog, ApplicationDetailsProjection details) {
        VBox content = new VBox(18);
        content.setPadding(new Insets(8, 14, 8, 14));
        content.getChildren().addAll(
                section("Applicant Information", applicantDetailsGrid(details)),
                section("Application Information", applicationDetailsGrid(details)),
                section("Actions", actionControls(dialog, details))
        );
        return content;
    }

    private Node applicantDetailsGrid(ApplicationDetailsProjection details) {
        GridPane grid = detailsGrid();
        addDetailRow(grid, 0, "Full name", details.fullName());
        addDetailRow(grid, 1, "Phone", details.phone());
        addDetailRow(grid, 2, "Email", details.email());
        return grid;
    }

    private Node applicationDetailsGrid(ApplicationDetailsProjection details) {
        GridPane grid = detailsGrid();
        addDetailRow(grid, 0, "Application id", details.applicationId());
        addDetailRow(grid, 1, "Program", details.programName());
        addDetailRow(grid, 2, "Status", details.status());
        addDetailRow(grid, 3, "Created at", details.createdAt().format(DATE_TIME_FORMATTER));
        addDetailRow(grid, 4, "Comment", valueOrDash(details.comment()));
        addDetailRow(grid, 5, "Last communication", formatLastCommunication(details.lastCommunicationResult()));
        return grid;
    }

    private GridPane detailsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.setStyle("""
                -fx-background-color: #f7f8fa;
                -fx-border-color: #dde1e6;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                """);
        return grid;
    }

    private void addDetailRow(GridPane grid, int row, String labelText, Object value) {
        Label label = new Label(labelText + ":");
        label.setMinWidth(120);
        label.setStyle("-fx-font-weight: 700; -fx-text-fill: #374151;");

        Label valueLabel = new Label(valueOrDash(value));
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(360);

        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private Node section(String titleText, Node content) {
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 700;");
        return new VBox(8, title, content);
    }

    private Node actionControls(Dialog<ButtonType> dialog, ApplicationDetailsProjection details) {
        if (details.status() == ApplicationStatus.PENDING) {
            Button startProcessingButton = new Button("Start Processing");
            startProcessingButton.setDisable(true);
            return new HBox(8, startProcessingButton);
        }
        if (details.status() == ApplicationStatus.IN_PROGRESS) {
            Button recordCommunicationButton = new Button("Record Communication");
            Button finishProcessingButton = new Button("Finish Processing");
            recordCommunicationButton.setOnAction(event -> openRecordCommunicationDialog(
                    recordCommunicationButton.getScene().getWindow(),
                    details.applicationId(),
                    details.fullName(),
                    details.programName(),
                    () -> refreshDetailsDialog(dialog, details.applicationId())
            ));
            finishProcessingButton.setDisable(true);
            return new HBox(8, recordCommunicationButton, finishProcessingButton);
        }
        return new Label("Application processing is completed.");
    }

    private void refreshDetailsDialog(Dialog<ButtonType> dialog, Integer applicationId) {
        dialog.getDialogPane().setContent(new Label("Refreshing application details..."));

        Task<Optional<ApplicationDetailsProjection>> task = new Task<>() {
            @Override
            protected Optional<ApplicationDetailsProjection> call() {
                return controller.getApplicationDetails(applicationId);
            }
        };

        task.setOnSucceeded(event -> {
            Optional<ApplicationDetailsProjection> details = task.getValue();
            if (details.isEmpty()) {
                dialog.close();
                showError(dialog.getOwner(), "Application was not found.");
                loadApplications(selectedScope());
                return;
            }
            dialog.setHeaderText(details.get().fullName());
            dialog.getDialogPane().setContent(createDetailsContent(dialog, details.get()));
            loadApplications(selectedScope());
        });
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            dialog.getDialogPane().setContent(new Label(
                    "Could not refresh application details: " + exception.getMessage()
            ));
            loadApplications(selectedScope());
        });

        Thread thread = new Thread(task, "refresh-application-details");
        thread.setDaemon(true);
        thread.start();
    }

    private void openRecordCommunicationDialog(
            Window owner,
            Integer applicationId,
            String applicantName,
            String programName,
            Runnable onSaved
    ) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Record Communication");
        dialog.setHeaderText("Record Communication");
        dialog.initOwner(owner);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveButtonType);

        ComboBox<CommunicationChannel> channelBox = new ComboBox<>();
        channelBox.getItems().setAll(CommunicationChannel.values());
        channelBox.setPromptText("Select channel");
        channelBox.setMaxWidth(Double.MAX_VALUE);

        ToggleGroup resultGroup = new ToggleGroup();
        VBox resultButtons = new VBox(6);
        for (CommunicationResult result : CommunicationResult.values()) {
            RadioButton radioButton = new RadioButton(formatEnumName(result.name()));
            radioButton.setToggleGroup(resultGroup);
            radioButton.setUserData(result);
            resultButtons.getChildren().add(radioButton);
        }

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Enter communication details, applicant response, or additional notes.");
        commentArea.setPrefRowCount(4);
        commentArea.setWrapText(true);

        Label validationLabel = new Label();
        validationLabel.setStyle("-fx-text-fill: #b42318;");

        VBox content = new VBox(12);
        content.setPadding(new Insets(8, 14, 8, 14));
        content.getChildren().addAll(
                new Label("Applicant: " + applicantName),
                new Label("Program: " + programName),
                labeledControl(
                        tooltipLabel(
                                "Communication Method",
                                "Select the communication method used to contact the applicant."
                        ),
                        channelBox
                ),
                new VBox(
                        6,
                        tooltipLabel(
                                "Communication Result",
                                "Select the outcome of the communication attempt."
                        ),
                        resultButtons
                ),
                labeledControl(
                        tooltipLabel(
                                "Comment (optional)",
                                "Optional notes about the conversation, applicant response, or follow-up details."
                        ),
                        commentArea
                ),
                validationLabel
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(520);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();

            CommunicationChannel channel = channelBox.getValue();
            if (channel == null) {
                validationLabel.setText("Please select communication channel.");
                return;
            }

            Toggle selectedResult = resultGroup.getSelectedToggle();
            if (selectedResult == null) {
                validationLabel.setText("Please select communication result.");
                return;
            }

            CommunicationRequest request = new CommunicationRequest(
                    channel,
                    (CommunicationResult) selectedResult.getUserData(),
                    blankToNull(commentArea.getText())
            );
            saveCommunication(applicationId, request, saveButton, validationLabel, dialog, onSaved);
        });

        dialog.showAndWait();
    }

    private void saveCommunication(
            Integer applicationId,
            CommunicationRequest request,
            Button saveButton,
            Label validationLabel,
            Dialog<ButtonType> dialog,
            Runnable onSaved
    ) {
        saveButton.setDisable(true);
        saveButton.setText("Saving...");
        validationLabel.setText("");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                controller.recordCommunication(applicationId, request);
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            dialog.close();
            onSaved.run();
        });
        task.setOnFailed(event -> {
            saveButton.setDisable(false);
            saveButton.setText("Save");
            Throwable exception = task.getException();
            validationLabel.setText("Could not record communication: " + exception.getMessage());
        });

        Thread thread = new Thread(task, "record-communication");
        thread.setDaemon(true);
        thread.start();
    }

    private Label tooltipLabel(String text, String tooltipText) {
        Label label = new Label(text);
        label.setTooltip(new Tooltip(tooltipText));
        return label;
    }

    private void showError(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.initOwner(owner);
        alert.showAndWait();
    }

    private String formatLastCommunication(CommunicationResult result) {
        if (result == null) {
            return "None";
        }
        return formatEnumName(result.name());
    }

    private String valueOrDash(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            return "-";
        }
        return text;
    }

    private String formatEnumName(String value) {
        return value.replace('_', ' ');
    }

    private String formatLastCommunication(ApplicationSummaryProjection application) {
        return formatLastCommunication(application.lastCommunicationResult());
    }
}
