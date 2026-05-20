package com.admissionmanagement.ui.view.analytics.component;

import com.admissionmanagement.application.analytics.AnalyticsPeriod;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsPeriodFormatter {
    private static final DateTimeFormatter DAY_OF_WEEK_FORMATTER = DateTimeFormatter.ofPattern("EEE");
    private static final DateTimeFormatter DAY_OF_MONTH_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM");

    public List<LocalDate> submissionPeriodStarts(AnalyticsPeriod period) {
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

    public String formatSubmissionPeriodLabel(AnalyticsPeriod period, LocalDate periodStart) {
        return switch (period) {
            case LAST_7_DAYS -> periodStart.format(DAY_OF_WEEK_FORMATTER);
            case LAST_30_DAYS -> periodStart.format(DAY_OF_MONTH_FORMATTER);
            case LAST_90_DAYS -> periodStart.format(MONTH_FORMATTER);
        };
    }

    public String formatAnalyticsPeriod(AnalyticsPeriod period) {
        if (period == null) {
            return "";
        }
        return switch (period) {
            case LAST_7_DAYS -> "Last 7 days";
            case LAST_30_DAYS -> "Last 30 days";
            case LAST_90_DAYS -> "Last 90 days";
        };
    }
}
