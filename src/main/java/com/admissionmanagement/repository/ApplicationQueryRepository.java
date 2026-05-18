package com.admissionmanagement.repository;

import com.admissionmanagement.domain.application.ApplicationStatus;
import com.admissionmanagement.application.analytics.AnalyticsGrouping;
import com.admissionmanagement.dto.ApplicationSearchCriteria;
import com.admissionmanagement.projection.ApplicationDetailsProjection;
import com.admissionmanagement.projection.ApplicationEventProjection;
import com.admissionmanagement.projection.ApplicationSubmissionStatsProjection;
import com.admissionmanagement.projection.ApplicationSummaryProjection;
import com.admissionmanagement.projection.ChannelCommunicationResultStatsProjection;
import com.admissionmanagement.projection.CommunicationResultStatsProjection;
import com.admissionmanagement.projection.FinalResultStatsProjection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationQueryRepository {
    List<ApplicationSummaryProjection> findByCriteria(
            List<ApplicationStatus> statuses,
            ApplicationSearchCriteria criteria
    );

    Optional<ApplicationDetailsProjection> findDetailsById(Integer applicationId);

    List<ApplicationEventProjection> findEventsByApplicationId(Integer applicationId);

    List<ApplicationEventProjection> findAllApplicationEvents();

    List<ApplicationSubmissionStatsProjection> countByPeriod(
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            AnalyticsGrouping grouping
    );

    List<CommunicationResultStatsProjection> countByCommunicationResult(
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    );

    List<ChannelCommunicationResultStatsProjection> countChannelByCommunicationResult(
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    );

    List<FinalResultStatsProjection> countByFinalProcessingResult(
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    );
}
