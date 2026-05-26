package com.admissionmanagement.ui.view.processing.component;

import com.admissionmanagement.domain.application.CommunicationResult;
import com.admissionmanagement.dto.ApplicationSearchCriteria;
import com.admissionmanagement.dto.LastCommunicationFilterMode;
import com.admissionmanagement.ui.view.processing.support.ApplicationProcessingUiSupport;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class ApplicationFilterPanel {
    private final Runnable onSearch;
    private final TextField phoneField;
    private final TextField emailField;
    private final DatePicker dateFromPicker;
    private final DatePicker dateToPicker;
    private final ComboBox<LastCommunicationFilterOption> communicationResultBox;
    private final FlowPane root;

    public ApplicationFilterPanel(Runnable onSearch) {
        this.onSearch = onSearch;
        this.phoneField = new TextField();
        this.emailField = new TextField();
        this.dateFromPicker = new DatePicker();
        this.dateToPicker = new DatePicker();
        this.communicationResultBox = new ComboBox<>();
        this.root = createRoot();
    }

    public Node getRoot() {
        return root;
    }

    public ApplicationSearchCriteria readCriteria() {
        LocalDate fromDate = dateFromPicker.getValue();
        LocalDate toDate = dateToPicker.getValue();
        LocalDateTime dateFrom = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime dateTo = toDate == null ? null : toDate.atTime(LocalTime.MAX);
        LastCommunicationFilterOption selectedOption = communicationResultBox.getValue();

        return new ApplicationSearchCriteria(
                ApplicationProcessingUiSupport.trimToNull(phoneField.getText()),
                ApplicationProcessingUiSupport.trimToNull(emailField.getText()),
                selectedOption == null ? null : selectedOption.result(),
                selectedOption == null ? LastCommunicationFilterMode.ANY : selectedOption.mode(),
                dateFrom,
                dateTo
        );
    }

    private FlowPane createRoot() {
        configureInputs();

        FlowPane filters = new FlowPane(12, 10);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getChildren().addAll(
                ApplicationProcessingUiSupport.labeledControl("Phone", phoneField),
                ApplicationProcessingUiSupport.labeledControl("Email", emailField),
                ApplicationProcessingUiSupport.labeledControl("Date From", dateFromPicker),
                ApplicationProcessingUiSupport.labeledControl("Date To", dateToPicker),
                ApplicationProcessingUiSupport.labeledControl("Last Result", communicationResultBox),
                createSearchButton(),
                createResetButton()
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

    private void configureInputs() {
        phoneField.setPromptText("Phone");
        emailField.setPromptText("Email");
        dateFromPicker.setPromptText("Date from");
        dateToPicker.setPromptText("Date to");
        communicationResultBox.setPromptText("Last result");
        communicationResultBox.getItems().setAll(createCommunicationFilterOptions());
    }

    private Button createSearchButton() {
        Button searchButton = new Button("Search");
        searchButton.setDefaultButton(true);
        searchButton.setOnAction(event -> onSearch.run());
        return searchButton;
    }

    private Button createResetButton() {
        Button resetButton = new Button("Reset");
        resetButton.setOnAction(event -> {
            clear();
            onSearch.run();
        });
        return resetButton;
    }

    private void clear() {
        phoneField.clear();
        emailField.clear();
        dateFromPicker.setValue(null);
        dateToPicker.setValue(null);
        communicationResultBox.setValue(null);
    }

    private LastCommunicationFilterOption[] createCommunicationFilterOptions() {
        CommunicationResult[] results = CommunicationResult.values();
        LastCommunicationFilterOption[] options = new LastCommunicationFilterOption[results.length + 1];

        for (int index = 0; index < results.length; index++) {
            options[index] = LastCommunicationFilterOption.byResult(results[index]);
        }
        options[results.length] = LastCommunicationFilterOption.withoutCommunications();
        return options;
    }

    private record LastCommunicationFilterOption(
            String label,
            CommunicationResult result,
            LastCommunicationFilterMode mode
    ) {
        private static LastCommunicationFilterOption byResult(CommunicationResult result) {
            return new LastCommunicationFilterOption(result.name(), result, LastCommunicationFilterMode.BY_RESULT);
        }

        private static LastCommunicationFilterOption withoutCommunications() {
            return new LastCommunicationFilterOption(
                    "No communications yet",
                    null,
                    LastCommunicationFilterMode.WITHOUT_COMMUNICATIONS
            );
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
