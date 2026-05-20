package com.admissionmanagement.ui.view.analytics.component;

import com.admissionmanagement.application.analytics.AnalyticsPeriod;
import com.admissionmanagement.domain.application.CommunicationChannel;
import com.admissionmanagement.domain.application.CommunicationResult;
import com.admissionmanagement.projection.ApplicationSubmissionStatsProjection;
import com.admissionmanagement.projection.ChannelCommunicationResultStatsProjection;
import com.admissionmanagement.projection.CommunicationResultStatsProjection;
import com.admissionmanagement.projection.FinalResultStatsProjection;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AnalyticsChartRenderer {
    private static final String POSITIVE_RESULT_COLOR = "#2e7d32";
    private static final String NEGATIVE_RESULT_COLOR = "#c62828";

    private final AnalyticsChartFactory.AnalyticsCharts charts;
    private final AnalyticsPeriodFormatter periodFormatter;

    public AnalyticsChartRenderer(
            AnalyticsChartFactory.AnalyticsCharts charts,
            AnalyticsPeriodFormatter periodFormatter
    ) {
        this.charts = Objects.requireNonNull(charts);
        this.periodFormatter = Objects.requireNonNull(periodFormatter);
    }

    public void clearCharts() {
        charts.submissionsChart().getData().clear();
        ((CategoryAxis) charts.submissionsChart().getXAxis()).getCategories().clear();
        charts.primaryResultsChart().getData().clear();
        charts.channelsChart().getData().clear();
        charts.finalResultsChart().getData().clear();
    }

    public void renderAnalytics(
            AnalyticsPeriod period,
            List<ApplicationSubmissionStatsProjection> submissions,
            List<CommunicationResultStatsProjection> primaryResults,
            List<ChannelCommunicationResultStatsProjection> channelStats,
            List<FinalResultStatsProjection> finalResults
    ) {
        renderSubmissionStats(period, submissions);
        renderPrimaryResults(primaryResults);
        renderChannelStats(channelStats);
        renderFinalResults(finalResults);
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

        List<LocalDate> periodStarts = periodFormatter.submissionPeriodStarts(period);
        List<String> categories = new ArrayList<>();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Submissions");

        for (LocalDate periodStart : periodStarts) {
            String label = periodFormatter.formatSubmissionPeriodLabel(period, periodStart);
            Long applicationsCount = countsByPeriodStart.getOrDefault(periodStart, 0L);
            categories.add(label);

            XYChart.Data<String, Number> data = new XYChart.Data<>(label, applicationsCount);
            series.getData().add(data);
            installLater(data, "Period: " + label
                    + "\nSubmitted applications: " + applicationsCount);
        }

        CategoryAxis submissionsAxis = (CategoryAxis) charts.submissionsChart().getXAxis();
        submissionsAxis.getCategories().setAll(categories);
        charts.submissionsChart().getData().clear();
        charts.submissionsChart().getData().add(series);
    }

    private void renderPrimaryResults(List<CommunicationResultStatsProjection> results) {
        long total = results.stream()
                .mapToLong(CommunicationResultStatsProjection::communicationsCount)
                .sum();
        charts.primaryResultsChart().setData(FXCollections.observableArrayList(results.stream()
                .map(result -> pieData(result.result().name(), result.communicationsCount()))
                .toList()));
        Platform.runLater(() -> {
            applyPieColors(charts.primaryResultsChart());
            charts.primaryResultsChart().getData().forEach(data ->
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
            charts.channelsChart().getData().add(series);
        }
    }

    private void renderFinalResults(List<FinalResultStatsProjection> results) {
        long total = results.stream()
                .mapToLong(FinalResultStatsProjection::applicationsCount)
                .sum();
        charts.finalResultsChart().setData(FXCollections.observableArrayList(results.stream()
                .map(result -> pieData(result.status().name(), result.applicationsCount()))
                .toList()));
        Platform.runLater(() -> {
            applyPieColors(charts.finalResultsChart());
            charts.finalResultsChart().getData().forEach(data ->
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

    private long asLong(double value) {
        return Math.round(value);
    }

    private String formatPercent(double value, long total) {
        if (total == 0) {
            return "0.0";
        }
        return String.format("%.1f", value * 100 / total);
    }
}
