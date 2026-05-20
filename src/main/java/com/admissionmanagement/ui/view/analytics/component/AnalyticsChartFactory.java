package com.admissionmanagement.ui.view.analytics.component;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

public class AnalyticsChartFactory {
    private static final String SUBMISSIONS_TOOLTIP =
            "Shows how many applications were submitted during the selected period, grouped by date bucket.";
    private static final String PRIMARY_RESULTS_TOOLTIP =
            "Shows the distribution of first communication results for applications that have at least one communication event.";
    private static final String CHANNELS_BY_RESULT_TOOLTIP =
            "Shows which communication channels were used for each primary communication result. Only the first communication event for each application is counted.";
    private static final String FINAL_RESULTS_TOOLTIP =
            "Shows the distribution of applications that reached a final processing status during the selected period. The date is based on the status change event timestamp.";

    public AnalyticsCharts createCharts() {
        LineChart<String, Number> submissionsChart = createSubmissionsChart();
        PieChart primaryResultsChart = createPieChart("Primary Results");
        StackedBarChart<String, Number> channelsChart = createChannelsChart();
        PieChart finalResultsChart = createPieChart("Final Results");
        GridPane chartGrid = createChartGrid(
                submissionsChart,
                primaryResultsChart,
                channelsChart,
                finalResultsChart
        );

        return new AnalyticsCharts(
                submissionsChart,
                primaryResultsChart,
                channelsChart,
                finalResultsChart,
                chartGrid
        );
    }

    private GridPane createChartGrid(
            LineChart<String, Number> submissionsChart,
            PieChart primaryResultsChart,
            StackedBarChart<String, Number> channelsChart,
            PieChart finalResultsChart
    ) {
        GridPane chartGrid = new GridPane();
        chartGrid.setHgap(14);
        chartGrid.setVgap(14);
        chartGrid.add(chartBlock("Submissions", submissionsChart, SUBMISSIONS_TOOLTIP), 0, 0);
        chartGrid.add(chartBlock("Primary Results", primaryResultsChart, PRIMARY_RESULTS_TOOLTIP), 1, 0);
        chartGrid.add(chartBlock("Channels by Result", channelsChart, CHANNELS_BY_RESULT_TOOLTIP), 0, 1);
        chartGrid.add(chartBlock("Final Results", finalResultsChart, FINAL_RESULTS_TOOLTIP), 1, 1);
        configureGridConstraints(chartGrid);
        return chartGrid;
    }

    private void configureGridConstraints(GridPane chartGrid) {
        for (int index = 0; index < 2; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50);
            column.setHgrow(Priority.ALWAYS);
            chartGrid.getColumnConstraints().add(column);

            RowConstraints row = new RowConstraints();
            row.setMinHeight(290);
            row.setVgrow(Priority.ALWAYS);
            chartGrid.getRowConstraints().add(row);
        }
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

    private PieChart createPieChart(String title) {
        PieChart chart = new PieChart();
        chart.setTitle(title);
        chart.setAnimated(false);
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setMinHeight(230);
        return chart;
    }

    private void installTooltip(Node node, String text) {
        Tooltip.install(node, new Tooltip(text));
    }

    public record AnalyticsCharts(
            LineChart<String, Number> submissionsChart,
            PieChart primaryResultsChart,
            StackedBarChart<String, Number> channelsChart,
            PieChart finalResultsChart,
            GridPane chartGrid
    ) {
    }
}
