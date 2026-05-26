package com.admissionmanagement.dto;

import com.admissionmanagement.domain.application.CommunicationResult;

import java.time.LocalDateTime;

public record ApplicationSearchCriteria(
        String phone,
        String email,
        CommunicationResult lastCommunicationResult,
        LastCommunicationFilterMode lastCommunicationFilterMode,
        LocalDateTime dateFrom,
        LocalDateTime dateTo
) {
}
