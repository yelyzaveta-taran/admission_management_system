package com.admissionmanagement.ui.view.processing.component;

import com.admissionmanagement.application.processing.ApplicationScope;
import com.admissionmanagement.projection.ApplicationSummaryProjection;
import com.admissionmanagement.ui.view.processing.support.ApplicationProcessingUiSupport;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class ApplicationCardFactory {
    private final CardActions actions;

    public ApplicationCardFactory(CardActions actions) {
        this.actions = actions;
    }

    public Node createCard(ApplicationScope scope, ApplicationSummaryProjection application) {
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

        card.getChildren().addAll(
                applicant,
                createDetails(application),
                createActions(scope, application)
        );
        return card;
    }

    private VBox createDetails(ApplicationSummaryProjection application) {
        Label program = new Label("Program: " + application.programName());
        Label created = new Label("Created: "
                + application.createdAt().format(ApplicationProcessingUiSupport.DATE_TIME_FORMATTER));
        Label status = new Label("Status: " + application.status());
        Label communication = new Label("Last communication: "
                + ApplicationProcessingUiSupport.formatLastCommunication(application.lastCommunicationResult()));
        return new VBox(4, program, created, status, communication);
    }

    private HBox createActions(ApplicationScope scope, ApplicationSummaryProjection application) {
        HBox actionButtons = new HBox(8);
        actionButtons.setAlignment(Pos.CENTER_LEFT);
        actionButtons.getChildren().add(createDetailsButton(application.applicationId()));

        if (scope == ApplicationScope.PENDING) {
            actionButtons.getChildren().add(createStartProcessingButton(application.applicationId()));
        } else if (scope == ApplicationScope.IN_PROGRESS) {
            actionButtons.getChildren().addAll(
                    createRecordCommunicationButton(application),
                    createFinishProcessingButton(application)
            );
        }
        return actionButtons;
    }

    private Button createDetailsButton(Integer applicationId) {
        Button detailsButton = new Button("Details");
        detailsButton.setOnAction(event -> actions.loadDetails(applicationId, detailsButton));
        return detailsButton;
    }

    private Button createStartProcessingButton(Integer applicationId) {
        Button startProcessingButton = new Button("Start Processing");
        startProcessingButton.setOnAction(event -> actions.startProcessing(applicationId, startProcessingButton));
        return startProcessingButton;
    }

    private Button createRecordCommunicationButton(ApplicationSummaryProjection application) {
        Button recordCommunicationButton = new Button("Record Communication");
        recordCommunicationButton.setOnAction(event -> actions.recordCommunication(
                recordCommunicationButton.getScene().getWindow(),
                application.applicationId(),
                application.fullName(),
                application.programName()
        ));
        return recordCommunicationButton;
    }

    private Button createFinishProcessingButton(ApplicationSummaryProjection application) {
        Button finishProcessingButton = new Button("Finish Processing");
        finishProcessingButton.setOnAction(event -> actions.finishProcessing(
                finishProcessingButton.getScene().getWindow(),
                application.applicationId(),
                application.fullName(),
                application.programName()
        ));
        return finishProcessingButton;
    }

    public interface CardActions {
        void loadDetails(Integer applicationId, Button detailsButton);

        void startProcessing(Integer applicationId, Button startProcessingButton);

        void recordCommunication(Window owner, Integer applicationId, String applicantName, String programName);

        void finishProcessing(Window owner, Integer applicationId, String applicantName, String programName);
    }
}
