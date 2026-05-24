package com.admissionmanagement.ui.view.processing.dialog;

import com.admissionmanagement.controller.processing.ApplicationProcessingController;
import com.admissionmanagement.domain.application.ApplicationStatus;
import com.admissionmanagement.dto.FinishProcessingRequest;
import com.admissionmanagement.ui.view.processing.support.ApplicationProcessingTaskRunner;
import com.admissionmanagement.ui.view.processing.support.ApplicationProcessingUiSupport;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;

public final class FinishProcessingDialog {
    private final ApplicationProcessingController controller;

    public FinishProcessingDialog(ApplicationProcessingController controller) {
        this.controller = controller;
    }

    public void open(
            Window owner,
            Integer applicationId,
            String applicantName,
            String programName,
            Runnable onSaved
    ) {
        Dialog<ButtonType> dialog = createDialog(owner);
        ButtonType finishButtonType = new ButtonType("Finish", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, finishButtonType);

        ToggleGroup finalStatusGroup = new ToggleGroup();
        VBox finalStatusButtons = createFinalStatusButtons(finalStatusGroup);
        TextArea reasonArea = createReasonArea();
        Label validationLabel = ApplicationProcessingUiSupport.validationLabel();

        dialog.getDialogPane().setContent(createContent(
                applicantName,
                programName,
                finalStatusButtons,
                reasonArea,
                validationLabel
        ));
        dialog.getDialogPane().setPrefWidth(520);

        Button finishButton = (Button) dialog.getDialogPane().lookupButton(finishButtonType);
        finishButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            finishIfValid(applicationId, finalStatusGroup, reasonArea, finishButton, validationLabel, dialog, onSaved);
        });

        dialog.showAndWait();
    }

    private Dialog<ButtonType> createDialog(Window owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Finish Application Processing");
        dialog.setHeaderText("Finish Application Processing");
        dialog.initOwner(owner);
        return dialog;
    }

    private VBox createFinalStatusButtons(ToggleGroup finalStatusGroup) {
        VBox finalStatusButtons = new VBox(6);
        for (ApplicationStatus status : List.of(
                ApplicationStatus.CONFIRMED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.CANCELLED
        )) {
            RadioButton radioButton = new RadioButton(ApplicationProcessingUiSupport.formatEnumName(status.name()));
            radioButton.setToggleGroup(finalStatusGroup);
            radioButton.setUserData(status);
            finalStatusButtons.getChildren().add(radioButton);
        }
        return finalStatusButtons;
    }

    private TextArea createReasonArea() {
        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Add a reason or additional notes for this decision.");
        reasonArea.setPrefRowCount(4);
        reasonArea.setWrapText(true);
        return reasonArea;
    }

    private VBox createContent(
            String applicantName,
            String programName,
            VBox finalStatusButtons,
            TextArea reasonArea,
            Label validationLabel
    ) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(8, 14, 8, 14));
        content.getChildren().addAll(
                new Label("Applicant: " + applicantName),
                new Label("Program: " + programName),
                new VBox(
                        6,
                        ApplicationProcessingUiSupport.tooltipLabel(
                                "Final Decision",
                                "Select the final decision for this application."
                        ),
                        finalStatusButtons
                ),
                ApplicationProcessingUiSupport.labeledControl(
                        ApplicationProcessingUiSupport.tooltipLabel(
                                "Reason (optional)",
                                "Optional explanation for the selected final decision."
                        ),
                        reasonArea
                ),
                validationLabel
        );
        return content;
    }

    private void finishIfValid(
            Integer applicationId,
            ToggleGroup finalStatusGroup,
            TextArea reasonArea,
            Button finishButton,
            Label validationLabel,
            Dialog<ButtonType> dialog,
            Runnable onSaved
    ) {
        Toggle selectedStatus = finalStatusGroup.getSelectedToggle();
        FinishProcessingFormValidator.ValidationResult validationResult = FinishProcessingFormValidator.validate(
                selectedStatus == null ? null : (ApplicationStatus) selectedStatus.getUserData(),
                reasonArea.getText()
        );
        if (!validationResult.isValid()) {
            validationLabel.setText(validationResult.errorMessage());
            return;
        }

        finishApplicationProcessing(applicationId, validationResult.request(), finishButton, validationLabel, dialog, onSaved);
    }

    private void finishApplicationProcessing(
            Integer applicationId,
            FinishProcessingRequest request,
            Button finishButton,
            Label validationLabel,
            Dialog<ButtonType> dialog,
            Runnable onSaved
    ) {
        finishButton.setDisable(true);
        finishButton.setText("Finishing...");
        validationLabel.setText("");

        ApplicationProcessingTaskRunner.runTask(
                "finish-application-processing",
                () -> {
                    controller.finishApplicationProcessing(applicationId, request);
                    return null;
                },
                ignored -> {
                    dialog.close();
                    onSaved.run();
                },
                exception -> {
                    finishButton.setDisable(false);
                    finishButton.setText("Finish");
                    validationLabel.setText("Could not finish application processing: " + exception.getMessage());
                }
        );
    }
}
