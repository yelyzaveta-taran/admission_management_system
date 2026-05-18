package com.admissionmanagement.projection;

import com.admissionmanagement.domain.application.ApplicationStatus;
import com.admissionmanagement.domain.application.CommunicationResult;

import java.time.LocalDateTime;

public record ApplicationDetailsProjection(
        Integer applicationId,
        String fullName,
        String programName,
        String phone,
        String email,
        String comment,
        ApplicationStatus status,
        CommunicationResult lastCommunicationResult,
        LocalDateTime createdAt
) {
}
