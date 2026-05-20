package com.admissionmanagement.ui.view.eventjournal.dialog;

import com.admissionmanagement.projection.ApplicationEventProjection;
import com.admissionmanagement.ui.view.eventjournal.support.EventJournalFormatter;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

public class EventDetailsDialog {
    private final EventJournalFormatter formatter;

    public EventDetailsDialog() {
        this.formatter = new EventJournalFormatter();
    }

    public void show(Window owner, ApplicationEventProjection event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Event Details");
        dialog.setHeaderText(event.eventType());
        dialog.initOwner(owner);
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

        addEventDetailRow(grid, 0, "Application ID", formatter.valueOrDash(event.applicationId()));
        addEventDetailRow(grid, 1, "Date/Time", formatter.formatEventTime(event.eventTime()));
        addEventDetailRow(grid, 2, "Event Type", formatter.valueOrDash(event.eventType()));
        addDescriptionRow(grid, event);
        return grid;
    }

    private void addDescriptionRow(GridPane grid, ApplicationEventProjection event) {
        Label descriptionLabel = detailLabel("Description");
        TextArea descriptionArea = new TextArea(formatter.valueOrDash(event.description()));
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(5);
        descriptionArea.setMaxWidth(Double.MAX_VALUE);

        grid.add(descriptionLabel, 0, 3);
        grid.add(descriptionArea, 1, 3);
        GridPane.setHgrow(descriptionArea, Priority.ALWAYS);
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
}
