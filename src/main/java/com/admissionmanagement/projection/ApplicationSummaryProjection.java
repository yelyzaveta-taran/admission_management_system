package com.admissionmanagement.projection;

import com.admissionmanagement.domain.application.ApplicationStatus;
import com.admissionmanagement.domain.application.CommunicationResult;

import java.time.LocalDateTime;

public record ApplicationSummaryProjection(
        Integer applicationId,
        String fullName,
        String programName,
        ApplicationStatus status,
        CommunicationResult lastCommunicationResult,
        LocalDateTime createdAt
) {
}
