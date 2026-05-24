package com.admissionmanagement.ui.view.processing.dialog;

import com.admissionmanagement.controller.processing.ApplicationProcessingController;
import com.admissionmanagement.domain.application.CommunicationChannel;
import com.admissionmanagement.domain.application.CommunicationResult;
import com.admissionmanagement.dto.CommunicationRequest;
import com.admissionmanagement.ui.view.processing.support.ApplicationProcessingTaskRunner;
import com.admissionmanagement.ui.view.processing.support.ApplicationProcessingUiSupport;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class CommunicationDialog {
    private final ApplicationProcessingController controller;

    public CommunicationDialog(ApplicationProcessingController controller) {
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
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveButtonType);

        ComboBox<CommunicationChannel> channelBox = createChannelBox();
        ToggleGroup resultGroup = new ToggleGroup();
        VBox resultButtons = createResultButtons(resultGroup);
        TextArea commentArea = createCommentArea();
        Label validationLabel = ApplicationProcessingUiSupport.validationLabel();

        dialog.getDialogPane().setContent(createContent(
                applicantName,
                programName,
                channelBox,
                resultButtons,
                commentArea,
                validationLabel
        ));
        dialog.getDialogPane().setPrefWidth(520);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            saveIfValid(applicationId, channelBox, resultGroup, commentArea, saveButton, validationLabel, dialog, onSaved);
        });

        dialog.showAndWait();
    }

    private Dialog<ButtonType> createDialog(Window owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Record Communication");
        dialog.setHeaderText("Record Communication");
        dialog.initOwner(owner);
        return dialog;
    }

    private ComboBox<CommunicationChannel> createChannelBox() {
        ComboBox<CommunicationChannel> channelBox = new ComboBox<>();
        channelBox.getItems().setAll(CommunicationChannel.values());
        channelBox.setPromptText("Select channel");
        channelBox.setMaxWidth(Double.MAX_VALUE);
        return channelBox;
    }

    private VBox createResultButtons(ToggleGroup resultGroup) {
        VBox resultButtons = new VBox(6);
        for (CommunicationResult result : CommunicationResult.values()) {
            RadioButton radioButton = new RadioButton(ApplicationProcessingUiSupport.formatEnumName(result.name()));
            radioButton.setToggleGroup(resultGroup);
            radioButton.setUserData(result);
            resultButtons.getChildren().add(radioButton);
        }
        return resultButtons;
    }

    private TextArea createCommentArea() {
        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Enter communication details, applicant response, or additional notes.");
        commentArea.setPrefRowCount(4);
        commentArea.setWrapText(true);
        return commentArea;
    }

    private VBox createContent(
            String applicantName,
            String programName,
            ComboBox<CommunicationChannel> channelBox,
            VBox resultButtons,
            TextArea commentArea,
            Label validationLabel
    ) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(8, 14, 8, 14));
        content.getChildren().addAll(
                new Label("Applicant: " + applicantName),
                new Label("Program: " + programName),
                ApplicationProcessingUiSupport.labeledControl(
                        ApplicationProcessingUiSupport.tooltipLabel(
                                "Communication Method",
                                "Select the communication method used to contact the applicant."
                        ),
                        channelBox
                ),
                new VBox(
                        6,
                        ApplicationProcessingUiSupport.tooltipLabel(
                                "Communication Result",
                                "Select the outcome of the communication attempt."
                        ),
                        resultButtons
                ),
                ApplicationProcessingUiSupport.labeledControl(
                        ApplicationProcessingUiSupport.tooltipLabel(
                                "Comment (optional)",
                                "Optional notes about the conversation, applicant response, or follow-up details."
                        ),
                        commentArea
                ),
                validationLabel
        );
        return content;
    }

    private void saveIfValid(
            Integer applicationId,
            ComboBox<CommunicationChannel> channelBox,
            ToggleGroup resultGroup,
            TextArea commentArea,
            Button saveButton,
            Label validationLabel,
            Dialog<ButtonType> dialog,
            Runnable onSaved
    ) {
        CommunicationChannel channel = channelBox.getValue();
        Toggle selectedResult = resultGroup.getSelectedToggle();
        CommunicationFormValidator.ValidationResult validationResult = CommunicationFormValidator.validate(
                channel,
                selectedResult == null ? null : (CommunicationResult) selectedResult.getUserData(),
                commentArea.getText()
        );
        if (!validationResult.isValid()) {
            validationLabel.setText(validationResult.errorMessage());
            return;
        }

        saveCommunication(applicationId, validationResult.request(), saveButton, validationLabel, dialog, onSaved);
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

        ApplicationProcessingTaskRunner.runTask(
                "record-communication",
                () -> {
                    controller.recordCommunication(applicationId, request);
                    return null;
                },
                ignored -> {
                    dialog.close();
                    onSaved.run();
                },
                exception -> {
                    saveButton.setDisable(false);
                    saveButton.setText("Save");
                    validationLabel.setText("Could not record communication: " + exception.getMessage());
                }
        );
    }
}
