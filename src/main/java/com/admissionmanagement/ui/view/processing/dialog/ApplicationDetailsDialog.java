package com.admissionmanagement.ui.view.processing.dialog;

import com.admissionmanagement.controller.processing.ApplicationProcessingController;
import com.admissionmanagement.domain.application.ApplicationStatus;
import com.admissionmanagement.projection.ApplicationDetailsProjection;
import com.admissionmanagement.projection.ApplicationEventProjection;
import com.admissionmanagement.ui.view.processing.support.ApplicationProcessingTaskRunner;
import com.admissionmanagement.ui.view.processing.support.ApplicationProcessingUiSupport;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

public final class ApplicationDetailsDialog {
    private final ApplicationProcessingController controller;
    private final CommunicationDialog communicationDialog;
    private final FinishProcessingDialog finishProcessingDialog;
    private final Runnable reloadApplications;

    public ApplicationDetailsDialog(
            ApplicationProcessingController controller,
            CommunicationDialog communicationDialog,
            FinishProcessingDialog finishProcessingDialog,
            Runnable reloadApplications
    ) {
        this.controller = controller;
        this.communicationDialog = communicationDialog;
        this.finishProcessingDialog = finishProcessingDialog;
        this.reloadApplications = reloadApplications;
    }

    public void loadAndOpen(Integer applicationId, Button detailsButton) {
        Window owner = detailsButton.getScene().getWindow();
        detailsButton.setDisable(true);
        detailsButton.setText("Loading...");

        ApplicationProcessingTaskRunner.runTask(
                "load-application-details",
                () -> controller.getApplicationDetails(applicationId),
                details -> {
                    detailsButton.setDisable(false);
                    detailsButton.setText("Details");
                    if (details.isEmpty()) {
                        ApplicationProcessingUiSupport.showError(owner, "Application was not found.");
                        return;
                    }
                    createDialog(owner, details.get()).showAndWait();
                },
                exception -> {
                    detailsButton.setDisable(false);
                    detailsButton.setText("Details");
                    ApplicationProcessingUiSupport.showError(
                            owner,
                            "Could not load application details: " + exception.getMessage()
                    );
                }
        );
    }

    private Dialog<ButtonType> createDialog(Window owner, ApplicationDetailsProjection details) {
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
                createSection("Applicant Information", applicantDetailsGrid(details)),
                createSection("Application Information", applicationDetailsGrid(details)),
                createSection("Actions", actionControls(dialog, details)),
                createSection("Event History", eventHistoryPanel(details.applicationId()))
        );
        return content;
    }

    private Node applicantDetailsGrid(ApplicationDetailsProjection details) {
        GridPane grid = createDetailsGrid();
        addDetailRow(grid, 0, "Full name", details.fullName());
        addDetailRow(grid, 1, "Phone", details.phone());
        addDetailRow(grid, 2, "Email", details.email());
        return grid;
    }

    private Node applicationDetailsGrid(ApplicationDetailsProjection details) {
        GridPane grid = createDetailsGrid();
        addDetailRow(grid, 0, "Application id", details.applicationId());
        addDetailRow(grid, 1, "Program", details.programName());
        addDetailRow(grid, 2, "Status", details.status());
        addDetailRow(grid, 3, "Created at",
                details.createdAt().format(ApplicationProcessingUiSupport.DATE_TIME_FORMATTER));
        addDetailRow(grid, 4, "Comment",
                ApplicationProcessingUiSupport.formatNullableValue(details.comment()));
        addDetailRow(grid, 5, "Last communication",
                ApplicationProcessingUiSupport.formatLastCommunication(details.lastCommunicationResult()));
        return grid;
    }

    private GridPane createDetailsGrid() {
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

        Label valueLabel = new Label(ApplicationProcessingUiSupport.formatNullableValue(value));
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(360);

        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private Node createSection(String titleText, Node content) {
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 700;");
        return new VBox(8, title, content);
    }

    private Node actionControls(Dialog<ButtonType> dialog, ApplicationDetailsProjection details) {
        if (details.status() == ApplicationStatus.PENDING) {
            Button startProcessingButton = new Button("Start Processing");
            startProcessingButton.setOnAction(event ->
                    startApplicationProcessingFromDetails(dialog, details.applicationId(), startProcessingButton));
            return new HBox(8, startProcessingButton);
        }
        if (details.status() == ApplicationStatus.IN_PROGRESS) {
            Button recordCommunicationButton = new Button("Record Communication");
            Button finishProcessingButton = new Button("Finish Processing");
            recordCommunicationButton.setOnAction(event -> communicationDialog.open(
                    recordCommunicationButton.getScene().getWindow(),
                    details.applicationId(),
                    details.fullName(),
                    details.programName(),
                    () -> refreshDetailsDialog(dialog, details.applicationId())
            ));
            finishProcessingButton.setOnAction(event -> finishProcessingDialog.open(
                    finishProcessingButton.getScene().getWindow(),
                    details.applicationId(),
                    details.fullName(),
                    details.programName(),
                    () -> refreshDetailsDialog(dialog, details.applicationId())
            ));
            return new HBox(8, recordCommunicationButton, finishProcessingButton);
        }
        return new Label("Application processing is completed.");
    }

    private void startApplicationProcessingFromDetails(
            Dialog<ButtonType> dialog,
            Integer applicationId,
            Button startProcessingButton
    ) {
        startProcessingButton.setDisable(true);
        startProcessingButton.setText("Starting...");

        ApplicationProcessingTaskRunner.runTask(
                "start-application-processing-from-details",
                () -> {
                    controller.startApplicationProcessing(applicationId);
                    return null;
                },
                ignored -> refreshDetailsDialog(dialog, applicationId),
                exception -> {
                    startProcessingButton.setDisable(false);
                    startProcessingButton.setText("Start Processing");
                    ApplicationProcessingUiSupport.showError(
                            dialog.getOwner(),
                            "Could not start application processing: " + exception.getMessage()
                    );
                }
        );
    }

    private void refreshDetailsDialog(Dialog<ButtonType> dialog, Integer applicationId) {
        dialog.getDialogPane().setContent(new Label("Refreshing application details..."));

        ApplicationProcessingTaskRunner.runTask(
                "refresh-application-details",
                () -> controller.getApplicationDetails(applicationId),
                details -> refreshDialogContent(dialog, applicationId, details),
                exception -> {
                    dialog.getDialogPane().setContent(new Label(
                            "Could not refresh application details: " + exception.getMessage()
                    ));
                    reloadApplications.run();
                }
        );
    }

    private void refreshDialogContent(
            Dialog<ButtonType> dialog,
            Integer applicationId,
            Optional<ApplicationDetailsProjection> details
    ) {
        if (details.isEmpty()) {
            dialog.close();
            ApplicationProcessingUiSupport.showError(dialog.getOwner(), "Application was not found.");
            reloadApplications.run();
            return;
        }
        dialog.setHeaderText(details.get().fullName());
        dialog.getDialogPane().setContent(createDetailsContent(dialog, details.get()));
        reloadApplications.run();
    }

    private Node eventHistoryPanel(Integer applicationId) {
        Label state = new Label("Event history is not loaded.");
        VBox eventsBox = new VBox(6);

        Button loadEventsButton = new Button("Load Event History");
        loadEventsButton.setOnAction(event ->
                loadApplicationEvents(applicationId, loadEventsButton, state, eventsBox));

        VBox panel = new VBox(8, loadEventsButton, state, eventsBox);
        panel.setPadding(new Insets(12));
        panel.setStyle("""
                -fx-background-color: #f7f8fa;
                -fx-border-color: #dde1e6;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                """);
        return panel;
    }

    private void loadApplicationEvents(
            Integer applicationId,
            Button loadEventsButton,
            Label state,
            VBox eventsBox
    ) {
        loadEventsButton.setDisable(true);
        loadEventsButton.setText("Loading...");
        state.setText("Loading event history...");
        eventsBox.getChildren().clear();

        ApplicationProcessingTaskRunner.runTask(
                "load-application-events",
                () -> controller.getApplicationEvents(applicationId),
                events -> {
                    loadEventsButton.setDisable(false);
                    loadEventsButton.setText("Refresh Event History");
                    renderApplicationEvents(events, state, eventsBox);
                },
                exception -> {
                    loadEventsButton.setDisable(false);
                    loadEventsButton.setText("Load Event History");
                    state.setText("Could not load event history: " + exception.getMessage());
                }
        );
    }

    private void renderApplicationEvents(
            List<ApplicationEventProjection> events,
            Label state,
            VBox eventsBox
    ) {
        eventsBox.getChildren().clear();
        if (events.isEmpty()) {
            state.setText("No events found.");
            return;
        }

        state.setText(events.size() + " events found");
        ScrollPane scrollPane = new ScrollPane(eventHistoryTable(events));
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(180);
        scrollPane.setStyle("-fx-background-color: transparent;");
        eventsBox.getChildren().add(scrollPane);
    }

    private Node eventHistoryTable(List<ApplicationEventProjection> events) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(6);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-background-color: white; -fx-border-color: #dde1e6;");

        addEventHeader(grid, 0, "Date/Time");
        addEventHeader(grid, 1, "Event Type");
        addEventHeader(grid, 2, "Description");

        for (int index = 0; index < events.size(); index++) {
            ApplicationEventProjection event = events.get(index);
            int row = index + 1;
            addEventCell(grid, 0, row,
                    event.eventTime().format(ApplicationProcessingUiSupport.DATE_TIME_FORMATTER), 120);
            addEventCell(grid, 1, row, event.eventType(), 120);
            addEventCell(grid, 2, row,
                    ApplicationProcessingUiSupport.formatNullableValue(event.description()), 250);
        }

        return grid;
    }

    private void addEventHeader(GridPane grid, int column, String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: 700; -fx-text-fill: #374151;");
        grid.add(label, column, 0);
    }

    private void addEventCell(GridPane grid, int column, int row, String text, double maxWidth) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(maxWidth);
        grid.add(label, column, row);
    }
}
