package com.admissionmanagement.application.analytics;

import com.admissionmanagement.projection.ApplicationSubmissionStatsProjection;
import com.admissionmanagement.projection.ChannelCommunicationResultStatsProjection;
import com.admissionmanagement.projection.CommunicationResultStatsProjection;
import com.admissionmanagement.projection.FinalResultStatsProjection;
import com.admissionmanagement.repository.ApplicationQueryRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public class ApplicationAnalyticsService {
    private final ApplicationQueryRepository repository;

    public ApplicationAnalyticsService(ApplicationQueryRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<ApplicationSubmissionStatsProjection> getApplicationSubmissionStats(AnalyticsPeriod period) {
        AnalyticsRange range = rangeFor(period);
        return repository.countByPeriod(range.dateFrom(), range.dateTo(), groupingFor(period));
    }

    public List<CommunicationResultStatsProjection> getPrimaryCommunicationResultStats(AnalyticsPeriod period) {
        AnalyticsRange range = rangeFor(period);
        return repository.countByCommunicationResult(range.dateFrom(), range.dateTo());
    }

    public List<ChannelCommunicationResultStatsProjection> getChannelsByCommunicationResultStats(
            AnalyticsPeriod period
    ) {
        AnalyticsRange range = rangeFor(period);
        return repository.countChannelByCommunicationResult(range.dateFrom(), range.dateTo());
    }

    public List<FinalResultStatsProjection> getFinalProcessingResultStats(AnalyticsPeriod period) {
        AnalyticsRange range = rangeFor(period);
        return repository.countByFinalProcessingResult(range.dateFrom(), range.dateTo());
    }

    private AnalyticsRange rangeFor(AnalyticsPeriod period) {
        Objects.requireNonNull(period);

        LocalDate today = LocalDate.now();
        LocalDate dateFrom = switch (period) {
            case LAST_7_DAYS -> today.minusDays(6);
            case LAST_30_DAYS -> today.minusDays(29);
            case LAST_90_DAYS -> today.minusDays(89);
        };
        return new AnalyticsRange(dateFrom.atStartOfDay(), today.atTime(LocalTime.MAX));
    }

    private AnalyticsGrouping groupingFor(AnalyticsPeriod period) {
        Objects.requireNonNull(period);
        return switch (period) {
            case LAST_7_DAYS, LAST_30_DAYS -> AnalyticsGrouping.DAY;
            case LAST_90_DAYS -> AnalyticsGrouping.MONTH;
        };
    }

    private record AnalyticsRange(LocalDateTime dateFrom, LocalDateTime dateTo) {
    }
}
