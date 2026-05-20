package com.admissionmanagement.ui.view.eventjournal;

import com.admissionmanagement.controller.processing.ApplicationProcessingController;
import com.admissionmanagement.projection.ApplicationEventProjection;
import com.admissionmanagement.ui.view.eventjournal.component.EventJournalTableFactory;
import com.admissionmanagement.ui.view.eventjournal.dialog.EventDetailsDialog;
import com.admissionmanagement.ui.view.eventjournal.support.EventJournalTaskRunner;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.Objects;

public class EventJournalView {
    private final ApplicationProcessingController controller;
    private final BorderPane root;
    private final Label stateLabel;
    private final TableView<ApplicationEventProjection> eventsTable;
    private final EventDetailsDialog detailsDialog;

    public EventJournalView(ApplicationProcessingController controller) {
        this.controller = Objects.requireNonNull(controller);
        this.root = new BorderPane();
        this.stateLabel = new Label();
        this.detailsDialog = new EventDetailsDialog();
        this.eventsTable = new EventJournalTableFactory().createTable(this::showEventDetails);

        configureView();
    }

    public Node getRoot() {
        return root;
    }

    public void loadEvents() {
        stateLabel.setText("Loading event journal...");
        eventsTable.getItems().clear();
        eventsTable.setPlaceholder(new Label("Loading event journal..."));

        EventJournalTaskRunner.runTask(
                "load-event-journal",
                controller::getAllApplicationEvents,
                this::renderEvents,
                exception -> showLoadFailure()
        );
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
        VBox content = new VBox(10, stateLabel, eventsTable);
        VBox.setVgrow(eventsTable, Priority.ALWAYS);
        return content;
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

    private void showLoadFailure() {
        stateLabel.setText("Failed to load event journal. Please try again.");
        eventsTable.setPlaceholder(new Label("Failed to load event journal. Please try again."));
    }

    private void showEventDetails(ApplicationEventProjection event) {
        Window owner = root.getScene().getWindow();
        detailsDialog.show(owner, event);
    }
}
