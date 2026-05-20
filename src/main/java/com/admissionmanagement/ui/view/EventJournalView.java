package com.admissionmanagement.ui.view;

import com.admissionmanagement.controller.processing.ApplicationProcessingController;
import com.admissionmanagement.projection.ApplicationEventProjection;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class EventJournalView {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ApplicationProcessingController controller;
    private final BorderPane root;
    private final Label stateLabel;
    private final TableView<ApplicationEventProjection> eventsTable;

    public EventJournalView(ApplicationProcessingController controller) {
        this.controller = Objects.requireNonNull(controller);
        this.root = new BorderPane();
        this.stateLabel = new Label();
        this.eventsTable = new TableView<>();

        configureView();
    }

    public Node getRoot() {
        return root;
    }

    public void loadEvents() {
        stateLabel.setText("Loading event journal...");
        eventsTable.getItems().clear();
        eventsTable.setPlaceholder(new Label("Loading event journal..."));

        Task<List<ApplicationEventProjection>> task = new Task<>() {
            @Override
            protected List<ApplicationEventProjection> call() {
                return controller.getAllApplicationEvents();
            }
        };

        task.setOnSucceeded(event -> renderEvents(task.getValue()));
        task.setOnFailed(event -> {
            stateLabel.setText("Failed to load event journal. Please try again.");
            eventsTable.setPlaceholder(new Label("Failed to load event journal. Please try again."));
        });

        Thread thread = new Thread(task, "load-event-journal");
        thread.setDaemon(true);
        thread.start();
    }

    private void configureView() {
        root.setPadding(new Insets(24));
        root.setTop(createHeader());
        root.setCenter(createContent());
    }

    private Node createHeader() {
        Label title = new Label("Event Journal");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700;");

        Label subtitle = new Label("Global history of application events");
        subtitle.setStyle("-fx-text-fill: #4b5563;");

        VBox header = new VBox(6, title, subtitle);
        header.setPadding(new Insets(0, 0, 18, 0));
        return header;
    }

    private Node createContent() {
        configureTable();

        VBox content = new VBox(10, stateLabel, eventsTable);
        VBox.setVgrow(eventsTable, Priority.ALWAYS);
        return content;
    }

    private void configureTable() {
        eventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        eventsTable.setPlaceholder(new Label("No application events found."));

        eventsTable.getColumns().add(textColumn("Application ID", ApplicationEventProjection::applicationId, 95));
        eventsTable.getColumns().add(textColumn(
                "Date/Time",
                event -> event.eventTime().format(DATE_TIME_FORMATTER),
                140
        ));
        eventsTable.getColumns().add(textColumn("Event Type", ApplicationEventProjection::eventType, 130));
        eventsTable.getColumns().add(textColumn("Description", ApplicationEventProjection::description, 300));
        eventsTable.setRowFactory(table -> {
            TableRow<ApplicationEventProjection> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && !row.isEmpty()) {
                    showEventDetails(row.getItem());
                }
            });
            return row;
        });
    }

    private TableColumn<ApplicationEventProjection, String> textColumn(
            String title,
            EventValueProvider valueProvider,
            double preferredWidth
    ) {
        TableColumn<ApplicationEventProjection, String> column = new TableColumn<>(title);
        column.setPrefWidth(preferredWidth);
        column.setCellValueFactory(cell ->
                new SimpleStringProperty(valueOrDash(valueProvider.value(cell.getValue()))));
        return column;
    }

    private void renderEvents(List<ApplicationEventProjection> events) {
        eventsTable.getItems().setAll(events);
        if (events.isEmpty()) {
            stateLabel.setText("No application events found.");
            eventsTable.setPlaceholder(new Label("No application events found."));
            return;
        }
        stateLabel.setText(events.size() + " events found");
    }

    private void showEventDetails(ApplicationEventProjection event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Event Details");
        dialog.setHeaderText(event.eventType());
        dialog.initOwner(root.getScene().getWindow());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(createEventDetailsContent(event));
        dialog.getDialogPane().setPrefWidth(560);
        dialog.showAndWait();
    }

    private Node createEventDetailsContent(ApplicationEventProjection event) {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setPadding(new Insets(8, 14, 8, 14));

        addEventDetailRow(grid, 0, "Application ID", valueOrDash(event.applicationId()));
        addEventDetailRow(grid, 1, "Date/Time", event.eventTime().format(DATE_TIME_FORMATTER));
        addEventDetailRow(grid, 2, "Event Type", valueOrDash(event.eventType()));

        Label descriptionLabel = detailLabel("Description");
        TextArea descriptionArea = new TextArea(valueOrDash(event.description()));
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(5);
        descriptionArea.setMaxWidth(Double.MAX_VALUE);

        grid.add(descriptionLabel, 0, 3);
        grid.add(descriptionArea, 1, 3);
        GridPane.setHgrow(descriptionArea, Priority.ALWAYS);
        return grid;
    }

    private void addEventDetailRow(GridPane grid, int row, String labelText, String value) {
        Label label = detailLabel(labelText);
        Label valueLabel = new Label(value);
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(360);

        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private Label detailLabel(String text) {
        Label label = new Label(text + ":");
        label.setMinWidth(110);
        label.setStyle("-fx-font-weight: 700; -fx-text-fill: #374151;");
        return label;
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

    @FunctionalInterface
    private interface EventValueProvider {
        Object value(ApplicationEventProjection event);
    }
}
