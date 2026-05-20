package com.admissionmanagement.ui.view.analytics;

import com.admissionmanagement.application.analytics.AnalyticsPeriod;
import com.admissionmanagement.controller.analytics.ApplicationAnalyticsController;
import com.admissionmanagement.projection.ApplicationSubmissionStatsProjection;
import com.admissionmanagement.projection.ChannelCommunicationResultStatsProjection;
import com.admissionmanagement.projection.CommunicationResultStatsProjection;
import com.admissionmanagement.projection.FinalResultStatsProjection;
import com.admissionmanagement.ui.view.analytics.component.AnalyticsChartFactory;
import com.admissionmanagement.ui.view.analytics.component.AnalyticsChartRenderer;
import com.admissionmanagement.ui.view.analytics.component.AnalyticsPeriodControl;
import com.admissionmanagement.ui.view.analytics.component.AnalyticsPeriodFormatter;
import com.admissionmanagement.ui.view.analytics.support.AnalyticsTaskRunner;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;

public class AnalyticsDashboardView {
    private static final String DASHBOARD_TOOLTIP =
            "Read-only admissions analytics for the selected period.";

    private final ApplicationAnalyticsController controller;
    private final BorderPane root;
    private final AnalyticsPeriodControl periodControl;
    private final Label stateLabel;
    private final AnalyticsChartFactory.AnalyticsCharts charts;
    private final AnalyticsChartRenderer chartRenderer;

    public AnalyticsDashboardView(ApplicationAnalyticsController controller) {
        this.controller = Objects.requireNonNull(controller);
        this.root = new BorderPane();
        this.stateLabel = new Label();

        AnalyticsPeriodFormatter periodFormatter = new AnalyticsPeriodFormatter();
        AnalyticsChartFactory chartFactory = new AnalyticsChartFactory();
        this.periodControl = new AnalyticsPeriodControl(periodFormatter, this::loadAnalytics);
        this.charts = chartFactory.createCharts();
        this.chartRenderer = new AnalyticsChartRenderer(charts, periodFormatter);

        configureView();
    }

    public Node getRoot() {
        return root;
    }

    public void loadAnalytics() {
        loadAnalytics(periodControl.selectedPeriod());
    }

    private void configureView() {
        root.setPadding(new Insets(24));
        stateLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #374151;");
        root.setTop(createHeader());
        root.setCenter(createContent());
    }

    private Node createHeader() {
        Label title = new Label("Analytics Dashboard");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700;");
        installTooltip(title, DASHBOARD_TOOLTIP);

        HBox header = new HBox(18, title, periodControl.getRoot());
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 18, 0));
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setMaxWidth(Double.MAX_VALUE);
        return header;
    }

    private Node createContent() {
        VBox content = new VBox(10, stateLabel, charts.chartGrid());
        VBox.setVgrow(charts.chartGrid(), Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    private void loadAnalytics(AnalyticsPeriod period) {
        stateLabel.setText("Loading analytics...");
        chartRenderer.clearCharts();

        AnalyticsTaskRunner.runTask(
                "load-analytics",
                () -> new AnalyticsSnapshot(
                        controller.getApplicationSubmissionStats(period),
                        controller.getPrimaryCommunicationResultStats(period),
                        controller.getChannelsByCommunicationResultStats(period),
                        controller.getFinalProcessingResultStats(period)
                ),
                analytics -> renderAnalytics(period, analytics),
                exception -> stateLabel.setText("Could not load analytics: " + exception.getMessage())
        );
    }

    private void renderAnalytics(AnalyticsPeriod period, AnalyticsSnapshot analytics) {
        chartRenderer.renderAnalytics(
                period,
                analytics.submissions(),
                analytics.primaryResults(),
                analytics.channelStats(),
                analytics.finalResults()
        );
        stateLabel.setText("Analytics loaded for " + periodControl.formatPeriod(period));
    }

    private void installTooltip(Node node, String text) {
        Tooltip.install(node, new Tooltip(text));
    }

    private record AnalyticsSnapshot(
            List<ApplicationSubmissionStatsProjection> submissions,
            List<CommunicationResultStatsProjection> primaryResults,
            List<ChannelCommunicationResultStatsProjection> channelStats,
            List<FinalResultStatsProjection> finalResults
    ) {
    }
}
