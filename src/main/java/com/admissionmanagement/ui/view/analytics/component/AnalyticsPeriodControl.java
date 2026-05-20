package com.admissionmanagement.ui.view.analytics.component;

import com.admissionmanagement.application.analytics.AnalyticsPeriod;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.Objects;
import java.util.function.Consumer;

public class AnalyticsPeriodControl {
    private static final String PERIOD_TOOLTIP =
            "Select the analytics period. Last 7 and 30 days are grouped by day; last 90 days is grouped by month.";

    private final AnalyticsPeriodFormatter periodFormatter;
    private final ComboBox<AnalyticsPeriod> periodBox;
    private final VBox root;

    public AnalyticsPeriodControl(
            AnalyticsPeriodFormatter periodFormatter,
            Consumer<AnalyticsPeriod> onPeriodChanged
    ) {
        this.periodFormatter = Objects.requireNonNull(periodFormatter);
        this.periodBox = new ComboBox<>();
        this.root = createRoot(Objects.requireNonNull(onPeriodChanged));
    }

    public Node getRoot() {
        return root;
    }

    public AnalyticsPeriod selectedPeriod() {
        AnalyticsPeriod selected = periodBox.getValue();
        if (selected == null) {
            return AnalyticsPeriod.LAST_30_DAYS;
        }
        return selected;
    }

    public String formatPeriod(AnalyticsPeriod period) {
        return periodFormatter.formatAnalyticsPeriod(period);
    }

    private VBox createRoot(Consumer<AnalyticsPeriod> onPeriodChanged) {
        Label periodLabel = new Label("Period");
        periodLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");
        installTooltip(periodLabel, PERIOD_TOOLTIP);

        configurePeriodBox(onPeriodChanged);
        return new VBox(4, periodLabel, periodBox);
    }

    private void configurePeriodBox(Consumer<AnalyticsPeriod> onPeriodChanged) {
        periodBox.getItems().setAll(AnalyticsPeriod.values());
        periodBox.setConverter(analyticsPeriodStringConverter());
        periodBox.setButtonCell(analyticsPeriodListCell());
        periodBox.setCellFactory(list -> analyticsPeriodListCell());
        periodBox.setValue(AnalyticsPeriod.LAST_30_DAYS);
        periodBox.setMinWidth(170);
        periodBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue != oldValue) {
                onPeriodChanged.accept(newValue);
            }
        });
    }

    private StringConverter<AnalyticsPeriod> analyticsPeriodStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(AnalyticsPeriod period) {
                return periodFormatter.formatAnalyticsPeriod(period);
            }

            @Override
            public AnalyticsPeriod fromString(String value) {
                return null;
            }
        };
    }

    private ListCell<AnalyticsPeriod> analyticsPeriodListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(AnalyticsPeriod period, boolean empty) {
                super.updateItem(period, empty);
                setText(empty || period == null ? null : periodFormatter.formatAnalyticsPeriod(period));
            }
        };
    }

    private void installTooltip(Node node, String text) {
        Tooltip.install(node, new Tooltip(text));
    }
}
