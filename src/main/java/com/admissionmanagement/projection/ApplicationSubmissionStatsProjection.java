package com.admissionmanagement.projection;

import java.time.LocalDate;

public record ApplicationSubmissionStatsProjection(
        LocalDate periodStart,
        Long applicationsCount
) {
}
