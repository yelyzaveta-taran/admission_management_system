package com.admissionmanagement.dto;

import com.admissionmanagement.domain.application.ApplicationStatus;

public record FinishProcessingRequest(
        ApplicationStatus status,
        String reason
) {
}
