package com.admissionmanagement.ui.view.processing.support;

import com.admissionmanagement.domain.application.CommunicationResult;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;

public final class ApplicationProcessingUiSupport {
    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ApplicationProcessingUiSupport() {
    }

    public static Node labeledControl(String labelText, Node control) {
        return labeledControl(fieldLabel(labelText), control);
    }

    public static Node labeledControl(Label label, Node control) {
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");

        if (control instanceof Region region) {
            region.setMinWidth(150);
        }

        VBox wrapper = new VBox(4, label, control);
        wrapper.setAlignment(Pos.BOTTOM_LEFT);
        return wrapper;
    }

    public static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");
        return label;
    }

    public static Label tooltipLabel(String text, String tooltipText) {
        Label label = new Label(text);
        label.setTooltip(new Tooltip(tooltipText));
        return label;
    }

    public static Label validationLabel() {
        Label label = new Label();
        label.setStyle("-fx-text-fill: #b42318;");
        return label;
    }

    public static void showError(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.initOwner(owner);
        alert.showAndWait();
    }

    public static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static String formatNullableValue(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            return "-";
        }
        return text;
    }

    public static String formatEnumName(String value) {
        return value.replace('_', ' ');
    }

    public static String formatLastCommunication(CommunicationResult result) {
        if (result == null) {
            return "No communications yet";
        }
        return formatEnumName(result.name());
    }
}
