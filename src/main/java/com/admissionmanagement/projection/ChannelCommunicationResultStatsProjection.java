package com.admissionmanagement.projection;

import com.admissionmanagement.domain.application.CommunicationChannel;
import com.admissionmanagement.domain.application.CommunicationResult;

public record ChannelCommunicationResultStatsProjection(
        CommunicationChannel channel,
        CommunicationResult result,
        Long communicationsCount
) {
}
