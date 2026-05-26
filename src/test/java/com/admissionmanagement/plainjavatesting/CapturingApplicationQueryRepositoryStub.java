package com.admissionmanagement.plainjavatesting;

import com.admissionmanagement.application.analytics.AnalyticsGrouping;
import com.admissionmanagement.domain.application.ApplicationStatus;
import com.admissionmanagement.dto.ApplicationSearchCriteria;
import com.admissionmanagement.projection.ApplicationDetailsProjection;
import com.admissionmanagement.projection.ApplicationEventProjection;
import com.admissionmanagement.projection.ApplicationSubmissionStatsProjection;
import com.admissionmanagement.projection.ApplicationSummaryProjection;
import com.admissionmanagement.projection.ChannelCommunicationResultStatsProjection;
import com.admissionmanagement.projection.CommunicationResultStatsProjection;
import com.admissionmanagement.projection.FinalResultStatsProjection;
import com.admissionmanagement.repository.ApplicationQueryRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

final class CapturingApplicationQueryRepositoryStub implements ApplicationQueryRepository {
    private List<ApplicationSummaryProjection> applicationsResult = List.of();
    private Optional<ApplicationDetailsProjection> detailsResult = Optional.empty();
    private List<ApplicationEventProjection> eventsResult = List.of();
    private List<ApplicationEventProjection> allEventsResult = List.of();

    private List<ApplicationStatus> lastStatuses;
    private ApplicationSearchCriteria lastCriteria;
    private Integer lastDetailsId;
    private Integer lastEventsApplicationId;

    void returnApplications(List<ApplicationSummaryProjection> applicationsResult) {
        this.applicationsResult = List.copyOf(applicationsResult);
    }

    void returnDetails(Optional<ApplicationDetailsProjection> detailsResult) {
        this.detailsResult = detailsResult;
    }

    void returnEvents(List<ApplicationEventProjection> eventsResult) {
        this.eventsResult = List.copyOf(eventsResult);
    }

    void returnAllEvents(List<ApplicationEventProjection> allEventsResult) {
        this.allEventsResult = List.copyOf(allEventsResult);
    }

    List<ApplicationStatus> lastStatuses() {
        return lastStatuses;
    }

    ApplicationSearchCriteria lastCriteria() {
        return lastCriteria;
    }

    Integer lastDetailsId() {
        return lastDetailsId;
    }

    Integer lastEventsApplicationId() {
        return lastEventsApplicationId;
    }

    @Override
    public List<ApplicationSummaryProjection> findByCriteria(
            List<ApplicationStatus> statuses,
            ApplicationSearchCriteria criteria
    ) {
        lastStatuses = List.copyOf(statuses);
        lastCriteria = criteria;
        return applicationsResult;
    }

    @Override
    public Optional<ApplicationDetailsProjection> findDetailsById(Integer applicationId) {
        lastDetailsId = applicationId;
        return detailsResult;
    }

    @Override
    public List<ApplicationEventProjection> findEventsByApplicationId(Integer applicationId) {
        lastEventsApplicationId = applicationId;
        return eventsResult;
    }

    @Override
    public List<ApplicationEventProjection> findAllApplicationEvents() {
        return allEventsResult;
    }

    @Override
    public List<ApplicationSubmissionStatsProjection> countByPeriod(
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            AnalyticsGrouping grouping
    ) {
        return List.of();
    }

    @Override
    public List<CommunicationResultStatsProjection> countByCommunicationResult(
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        return List.of();
    }

    @Override
    public List<ChannelCommunicationResultStatsProjection> countChannelByCommunicationResult(
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        return List.of();
    }

    @Override
    public List<FinalResultStatsProjection> countByFinalProcessingResult(
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        return List.of();
    }
}
