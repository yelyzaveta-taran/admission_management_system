package com.admissionmanagement.controller.analytics;

import com.admissionmanagement.application.analytics.AnalyticsPeriod;
import com.admissionmanagement.application.analytics.ApplicationAnalyticsService;
import com.admissionmanagement.projection.ApplicationSubmissionStatsProjection;
import com.admissionmanagement.projection.ChannelCommunicationResultStatsProjection;
import com.admissionmanagement.projection.CommunicationResultStatsProjection;
import com.admissionmanagement.projection.FinalResultStatsProjection;

import java.util.List;
import java.util.Objects;

public class ApplicationAnalyticsController {
    private final ApplicationAnalyticsService service;

    public ApplicationAnalyticsController(ApplicationAnalyticsService service) {
        this.service = Objects.requireNonNull(service);
    }

    public List<ApplicationSubmissionStatsProjection> getApplicationSubmissionStats(AnalyticsPeriod period) {
        return service.getApplicationSubmissionStats(period);
    }

    public List<CommunicationResultStatsProjection> getPrimaryCommunicationResultStats(AnalyticsPeriod period) {
        return service.getPrimaryCommunicationResultStats(period);
    }

    public List<ChannelCommunicationResultStatsProjection> getChannelsByCommunicationResultStats(
            AnalyticsPeriod period
    ) {
        return service.getChannelsByCommunicationResultStats(period);
    }

    public List<FinalResultStatsProjection> getFinalProcessingResultStats(AnalyticsPeriod period) {
        return service.getFinalProcessingResultStats(period);
    }
}
