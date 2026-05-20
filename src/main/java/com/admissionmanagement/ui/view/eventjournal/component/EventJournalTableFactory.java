package com.admissionmanagement.ui.view.eventjournal.component;

import com.admissionmanagement.projection.ApplicationEventProjection;
import com.admissionmanagement.ui.view.eventjournal.support.EventJournalFormatter;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;

import java.util.Objects;
import java.util.function.Consumer;

public class EventJournalTableFactory {
    private final EventJournalFormatter formatter;

    public EventJournalTableFactory() {
        this.formatter = new EventJournalFormatter();
    }

    public TableView<ApplicationEventProjection> createTable(Consumer<ApplicationEventProjection> onEventSelected) {
        Objects.requireNonNull(onEventSelected);

        TableView<ApplicationEventProjection> eventsTable = new TableView<>();
        eventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        eventsTable.setPlaceholder(new Label("No application events found."));
        eventsTable.getColumns().add(textColumn("Application ID", ApplicationEventProjection::applicationId, 95));
        eventsTable.getColumns().add(textColumn(
                "Date/Time",
                event -> formatter.formatEventTime(event.eventTime()),
                140
        ));
        eventsTable.getColumns().add(textColumn("Event Type", ApplicationEventProjection::eventType, 130));
        eventsTable.getColumns().add(textColumn("Description", ApplicationEventProjection::description, 300));
        eventsTable.setRowFactory(table -> eventRow(onEventSelected));
        return eventsTable;
    }

    private TableColumn<ApplicationEventProjection, String> textColumn(
            String title,
            EventValueProvider valueProvider,
            double preferredWidth
    ) {
        TableColumn<ApplicationEventProjection, String> column = new TableColumn<>(title);
        column.setPrefWidth(preferredWidth);
        column.setCellValueFactory(cell ->
                new SimpleStringProperty(formatter.valueOrDash(valueProvider.value(cell.getValue()))));
        return column;
    }

    private TableRow<ApplicationEventProjection> eventRow(Consumer<ApplicationEventProjection> onEventSelected) {
        TableRow<ApplicationEventProjection> row = new TableRow<>();
        row.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && !row.isEmpty()) {
                onEventSelected.accept(row.getItem());
            }
        });
        return row;
    }

    @FunctionalInterface
    private interface EventValueProvider {
        Object value(ApplicationEventProjection event);
    }
}
