package com.admissionmanagement.ui;

import com.admissionmanagement.application.analytics.AnalyticsPeriod;
import com.admissionmanagement.controller.analytics.ApplicationAnalyticsController;
import com.admissionmanagement.domain.application.CommunicationChannel;
import com.admissionmanagement.domain.application.CommunicationResult;
import com.admissionmanagement.projection.ApplicationSubmissionStatsProjection;
import com.admissionmanagement.projection.ChannelCommunicationResultStatsProjection;
import com.admissionmanagement.projection.CommunicationResultStatsProjection;
import com.admissionmanagement.projection.FinalResultStatsProjection;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AnalyticsDashboardView {
    private static final DateTimeFormatter DAY_OF_WEEK_FORMATTER = DateTimeFormatter.ofPattern("EEE");
    private static final DateTimeFormatter DAY_OF_MONTH_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM");
    private static final String DASHBOARD_TOOLTIP =
            "Read-only admissions analytics for the selected period.";
    private static final String PERIOD_TOOLTIP =
            "Select the analytics period. Last 7 and 30 days are grouped by day; last 90 days is grouped by month.";
    private static final String SUBMISSIONS_TOOLTIP =
            "Shows how many applications were submitted during the selected period, grouped by date bucket.";
    private static final String PRIMARY_RESULTS_TOOLTIP =
            "Shows the distribution of first communication results for applications that have at least one communication event.";
    private static final String CHANNELS_BY_RESULT_TOOLTIP =
            "Shows which communication channels were used for each primary communication result. Only the first communication event for each application is counted.";
    private static final String FINAL_RESULTS_TOOLTIP =
            "Shows the distribution of applications that reached a final processing status during the selected period. The date is based on the status change event timestamp.";
    private static final String POSITIVE_RESULT_COLOR = "#2e7d32";
    private static final String NEGATIVE_RESULT_COLOR = "#c62828";

    private final ApplicationAnalyticsController controller;
    private final BorderPane root;
    private final ComboBox<AnalyticsPeriod> periodBox;
    private final Label stateLabel;
    private final LineChart<String, Number> submissionsChart;
    private final PieChart primaryResultsChart;
    private final StackedBarChart<String, Number> channelsChart;
    private final PieChart finalResultsChart;

    public AnalyticsDashboardView(ApplicationAnalyticsController controller) {
        this.controller = Objects.requireNonNull(controller);
        this.root = new BorderPane();
        this.periodBox = new ComboBox<>();
        this.stateLabel = new Label();
        this.submissionsChart = createSubmissionsChart();
        this.primaryResultsChart = new PieChart();
        this.channelsChart = createChannelsChart();
        this.finalResultsChart = new PieChart();

        configureView();
    }

    public Node getRoot() {
        return root;
    }

    public void loadAnalytics() {
        loadAnalytics(selectedPeriod());
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

        Label periodLabel = new Label("Period");
        periodLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");
        installTooltip(periodLabel, PERIOD_TOOLTIP);

        periodBox.getItems().setAll(AnalyticsPeriod.values());
        periodBox.setConverter(analyticsPeriodStringConverter());
        periodBox.setButtonCell(analyticsPeriodListCell());
        periodBox.setCellFactory(list -> analyticsPeriodListCell());
        periodBox.setValue(AnalyticsPeriod.LAST_30_DAYS);
        periodBox.setMinWidth(170);
        periodBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue != oldValue) {
                loadAnalytics(newValue);
            }
        });

        VBox periodControl = new VBox(4, periodLabel, periodBox);
        HBox header = new HBox(18, title, periodControl);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 18, 0));
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setMaxWidth(Double.MAX_VALUE);
        return header;
    }

    private Node createContent() {
        configurePieChart(primaryResultsChart, "Primary Results");
        configurePieChart(finalResultsChart, "Final Results");

        GridPane chartGrid = new GridPane();
        chartGrid.setHgap(14);
        chartGrid.setVgap(14);
        chartGrid.add(chartBlock("Submissions", submissionsChart, SUBMISSIONS_TOOLTIP), 0, 0);
        chartGrid.add(chartBlock("Primary Results", primaryResultsChart, PRIMARY_RESULTS_TOOLTIP), 1, 0);
        chartGrid.add(chartBlock("Channels by Result", channelsChart, CHANNELS_BY_RESULT_TOOLTIP), 0, 1);
        chartGrid.add(chartBlock("Final Results", finalResultsChart, FINAL_RESULTS_TOOLTIP), 1, 1);

        for (int index = 0; index < 2; index++) {
            javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
            column.setPercentWidth(50);
            column.setHgrow(Priority.ALWAYS);
            chartGrid.getColumnConstraints().add(column);

            javafx.scene.layout.RowConstraints row = new javafx.scene.layout.RowConstraints();
            row.setMinHeight(290);
            row.setVgrow(Priority.ALWAYS);
            chartGrid.getRowConstraints().add(row);
        }

        VBox content = new VBox(10, stateLabel, chartGrid);
        VBox.setVgrow(chartGrid, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    private Node chartBlock(String title, Node chart, String tooltipText) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");
        installTooltip(titleLabel, tooltipText);

        VBox block = new VBox(8, titleLabel, chart);
        block.setPadding(new Insets(14));
        block.setMinHeight(290);
        block.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #d0d7de;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                """);
        VBox.setVgrow(chart, Priority.ALWAYS);
        GridPane.setHgrow(block, Priority.ALWAYS);
        GridPane.setVgrow(block, Priority.ALWAYS);
        return block;
    }

    private LineChart<String, Number> createSubmissionsChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date bucket");
        xAxis.setAutoRanging(false);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Application count");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Submissions");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setMinHeight(230);
        return chart;
    }

    private StackedBarChart<String, Number> createChannelsChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Primary communication result");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Application count");

        StackedBarChart<String, Number> chart = new StackedBarChart<>(xAxis, yAxis);
        chart.setTitle("Channels by Result");
        chart.setAnimated(false);
        chart.setMinHeight(230);
        return chart;
    }

    private void configurePieChart(PieChart chart, String title) {
        chart.setTitle(title);
        chart.setAnimated(false);
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setMinHeight(230);
    }

    private AnalyticsPeriod selectedPeriod() {
        AnalyticsPeriod selected = periodBox.getValue();
        if (selected == null) {
            return AnalyticsPeriod.LAST_30_DAYS;
        }
        return selected;
    }

    private void loadAnalytics(AnalyticsPeriod period) {
        stateLabel.setText("Loading analytics...");
        clearCharts();

        Task<AnalyticsSnapshot> task = new Task<>() {
            @Override
            protected AnalyticsSnapshot call() {
                return new AnalyticsSnapshot(
                        controller.getApplicationSubmissionStats(period),
                        controller.getPrimaryCommunicationResultStats(period),
                        controller.getChannelsByCommunicationResultStats(period),
                        controller.getFinalProcessingResultStats(period)
                );
            }
        };

        task.setOnSucceeded(event -> renderAnalytics(period, task.getValue()));
        task.setOnFailed(event -> stateLabel.setText("Could not load analytics: "
                + task.getException().getMessage()));

        Thread thread = new Thread(task, "load-analytics");
        thread.setDaemon(true);
        thread.start();
    }

    private void clearCharts() {
        submissionsChart.getData().clear();
        ((CategoryAxis) submissionsChart.getXAxis()).getCategories().clear();
        primaryResultsChart.getData().clear();
        channelsChart.getData().clear();
        finalResultsChart.getData().clear();
    }

    private void renderAnalytics(AnalyticsPeriod period, AnalyticsSnapshot analytics) {
        renderSubmissionStats(period, analytics.submissions());
        renderPrimaryResults(analytics.primaryResults());
        renderChannelStats(analytics.channelStats());
        renderFinalResults(analytics.finalResults());
        stateLabel.setText("Analytics loaded for " + formatAnalyticsPeriod(period));
    }

    private void renderSubmissionStats(
            AnalyticsPeriod period,
            List<ApplicationSubmissionStatsProjection> submissions
    ) {
        Map<LocalDate, Long> countsByPeriodStart = new LinkedHashMap<>();
        submissions.stream()
                .sorted(Comparator.comparing(ApplicationSubmissionStatsProjection::periodStart))
                .forEach(submission -> countsByPeriodStart.put(
                        submission.periodStart(),
                        submission.applicationsCount()
                ));

        List<LocalDate> periodStarts = submissionPeriodStarts(period);
        List<String> categories = new ArrayList<>();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Submissions");

        for (LocalDate periodStart : periodStarts) {
            String label = formatPeriodLabel(period, periodStart);
            Long applicationsCount = countsByPeriodStart.getOrDefault(periodStart, 0L);
            categories.add(label);

            XYChart.Data<String, Number> data = new XYChart.Data<>(label, applicationsCount);
            series.getData().add(data);
            installLater(data, "Period: " + label
                    + "\nSubmitted applications: " + applicationsCount);
        }

        CategoryAxis submissionsAxis = (CategoryAxis) submissionsChart.getXAxis();
        submissionsAxis.getCategories().setAll(categories);
        submissionsChart.getData().clear();
        submissionsChart.getData().add(series);
    }

    private List<LocalDate> submissionPeriodStarts(AnalyticsPeriod period) {
        LocalDate today = LocalDate.now();
        List<LocalDate> periodStarts = new ArrayList<>();
        switch (period) {
            case LAST_7_DAYS -> {
                LocalDate date = today.minusDays(6);
                while (!date.isAfter(today)) {
                    periodStarts.add(date);
                    date = date.plusDays(1);
                }
            }
            case LAST_30_DAYS -> {
                LocalDate date = today.minusDays(29);
                while (!date.isAfter(today)) {
                    periodStarts.add(date);
                    date = date.plusDays(1);
                }
            }
            case LAST_90_DAYS -> {
                LocalDate month = today.minusDays(89).withDayOfMonth(1);
                LocalDate currentMonth = today.withDayOfMonth(1);
                while (!month.isAfter(currentMonth)) {
                    periodStarts.add(month);
                    month = month.plusMonths(1);
                }
            }
        }
        return periodStarts;
    }

    private String formatPeriodLabel(AnalyticsPeriod period, LocalDate periodStart) {
        return switch (period) {
            case LAST_7_DAYS -> periodStart.format(DAY_OF_WEEK_FORMATTER);
            case LAST_30_DAYS -> periodStart.format(DAY_OF_MONTH_FORMATTER);
            case LAST_90_DAYS -> periodStart.format(MONTH_FORMATTER);
        };
    }

    private StringConverter<AnalyticsPeriod> analyticsPeriodStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(AnalyticsPeriod period) {
                return formatAnalyticsPeriod(period);
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
                setText(empty || period == null ? null : formatAnalyticsPeriod(period));
            }
        };
    }

    private String formatAnalyticsPeriod(AnalyticsPeriod period) {
        if (period == null) {
            return "";
        }
        return switch (period) {
            case LAST_7_DAYS -> "Last 7 days";
            case LAST_30_DAYS -> "Last 30 days";
            case LAST_90_DAYS -> "Last 90 days";
        };
    }

    private void renderPrimaryResults(List<CommunicationResultStatsProjection> results) {
        long total = results.stream()
                .mapToLong(CommunicationResultStatsProjection::communicationsCount)
                .sum();
        primaryResultsChart.setData(FXCollections.observableArrayList(results.stream()
                .map(result -> pieData(result.result().name(), result.communicationsCount()))
                .toList()));
        Platform.runLater(() -> {
            applyPieColors(primaryResultsChart);
            primaryResultsChart.getData().forEach(data ->
                    installPieTooltip(data, "Result: " + data.getName()
                            + "\nApplications: " + asLong(data.getPieValue())
                            + "\nShare: " + formatPercent(data.getPieValue(), total) + "%"));
        });
    }

    private void renderChannelStats(List<ChannelCommunicationResultStatsProjection> stats) {
        Map<CommunicationChannel, Map<CommunicationResult, Long>> statsByChannel =
                new EnumMap<>(CommunicationChannel.class);
        for (ChannelCommunicationResultStatsProjection stat : stats) {
            statsByChannel
                    .computeIfAbsent(stat.channel(), key -> new EnumMap<>(CommunicationResult.class))
                    .put(stat.result(), stat.communicationsCount());
        }

        List<CommunicationResult> results = stats.stream()
                .map(ChannelCommunicationResultStatsProjection::result)
                .distinct()
                .toList();

        for (Map.Entry<CommunicationChannel, Map<CommunicationResult, Long>> entry : statsByChannel.entrySet()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(entry.getKey().name());
            for (CommunicationResult result : results) {
                Long count = entry.getValue().getOrDefault(result, 0L);
                XYChart.Data<String, Number> data = new XYChart.Data<>(result.name(), count);
                series.getData().add(data);
                installLater(data, "Result: " + result
                        + "\nChannel: " + entry.getKey()
                        + "\nApplications: " + count);
            }
            channelsChart.getData().add(series);
        }
    }

    private void renderFinalResults(List<FinalResultStatsProjection> results) {
        long total = results.stream()
                .mapToLong(FinalResultStatsProjection::applicationsCount)
                .sum();
        finalResultsChart.setData(FXCollections.observableArrayList(results.stream()
                .map(result -> pieData(result.status().name(), result.applicationsCount()))
                .toList()));
        Platform.runLater(() -> {
            applyPieColors(finalResultsChart);
            finalResultsChart.getData().forEach(data ->
                    installPieTooltip(data, "Final result: " + data.getName()
                            + "\nApplications: " + asLong(data.getPieValue())
                            + "\nShare: " + formatPercent(data.getPieValue(), total) + "%"));
        });
    }

    private PieChart.Data pieData(String name, Long value) {
        return new PieChart.Data(name, value == null ? 0 : value);
    }

    private void applyPieColors(PieChart chart) {
        chart.getData().forEach(data -> {
            String color = pieColor(data.getName());
            if (color != null && data.getNode() != null) {
                data.getNode().setStyle("-fx-pie-color: " + color + ";");
            }
        });

        chart.lookupAll(".chart-legend-item").forEach(item -> {
            if (item instanceof Label label) {
                String color = pieColor(label.getText());
                if (color != null) {
                    label.lookupAll(".chart-legend-item-symbol")
                            .forEach(symbol -> symbol.setStyle("-fx-background-color: " + color + ";"));
                }
            }
        });
    }

    private String pieColor(String value) {
        return switch (value) {
            case "ANSWERED", "CONFIRMED" -> POSITIVE_RESULT_COLOR;
            case "NO_ANSWER", "REJECTED" -> NEGATIVE_RESULT_COLOR;
            default -> null;
        };
    }

    private void installLater(XYChart.Data<String, Number> data, String text) {
        Platform.runLater(() -> {
            if (data.getNode() != null) {
                Tooltip.install(data.getNode(), new Tooltip(text));
            }
        });
    }

    private void installPieTooltip(PieChart.Data data, String text) {
        if (data.getNode() != null) {
            Tooltip.install(data.getNode(), new Tooltip(text));
        }
    }

    private void installTooltip(Node node, String text) {
        Tooltip.install(node, new Tooltip(text));
    }

    private long asLong(double value) {
        return Math.round(value);
    }

    private String formatPercent(double value, long total) {
        if (total == 0) {
            return "0.0";
        }
        return String.format("%.1f", value * 100 / total);
    }

    private record AnalyticsSnapshot(
            List<ApplicationSubmissionStatsProjection> submissions,
            List<CommunicationResultStatsProjection> primaryResults,
            List<ChannelCommunicationResultStatsProjection> channelStats,
            List<FinalResultStatsProjection> finalResults
    ) {
    }
}
