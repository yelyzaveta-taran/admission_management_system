package com.admissionmanagement.ui.view.eventjournal.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventJournalFormatter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public String formatEventTime(LocalDateTime eventTime) {
        if (eventTime == null) {
            return "-";
        }
        return eventTime.format(DATE_TIME_FORMATTER);
    }

    public String valueOrDash(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            return "-";
        }
        return text;
    }
}
