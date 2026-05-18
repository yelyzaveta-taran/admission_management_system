package com.admissionmanagement.projection;

import com.admissionmanagement.domain.application.CommunicationResult;

public record CommunicationResultStatsProjection(
        CommunicationResult result,
        Long communicationsCount
) {
}
