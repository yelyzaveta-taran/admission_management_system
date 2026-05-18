package com.admissionmanagement.dto;

import com.admissionmanagement.domain.application.CommunicationChannel;
import com.admissionmanagement.domain.application.CommunicationResult;

public record CommunicationRequest(
        CommunicationChannel channel,
        CommunicationResult result,
        String comment
) {
}
