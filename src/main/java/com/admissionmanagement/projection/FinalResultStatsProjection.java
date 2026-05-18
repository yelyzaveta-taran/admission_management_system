package com.admissionmanagement.projection;

import com.admissionmanagement.domain.application.ApplicationStatus;

public record FinalResultStatsProjection(
        ApplicationStatus status,
        Long applicationsCount
) {
}
