package com.admissionmanagement.application.processing;

import com.admissionmanagement.dto.ApplicationSearchCriteria;
import com.admissionmanagement.domain.application.Application;
import com.admissionmanagement.projection.ApplicationDetailsProjection;
import com.admissionmanagement.projection.ApplicationSummaryProjection;
import com.admissionmanagement.repository.ApplicationQueryRepository;
import com.admissionmanagement.repository.ApplicationRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ApplicationProcessingService {
    private final ApplicationRepository applicationRepository;
    private final ApplicationQueryRepository applicationQueryRepository;

    public ApplicationProcessingService(
            ApplicationRepository applicationRepository,
            ApplicationQueryRepository applicationQueryRepository
    ) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.applicationQueryRepository = Objects.requireNonNull(applicationQueryRepository);
    }

    public List<ApplicationSummaryProjection> getApplications(
            ApplicationScope scope,
            ApplicationSearchCriteria criteria
    ) {
        return applicationQueryRepository.findByCriteria(scope.getStatuses(), criteria);
    }

    public Optional<ApplicationDetailsProjection> getApplicationDetails(Integer applicationId) {
        Objects.requireNonNull(applicationId);
        return applicationQueryRepository.findDetailsById(applicationId);
    }

    public void startApplicationProcessing(Integer applicationId) {
        Objects.requireNonNull(applicationId);
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application was not found"));
        application.startProcessing();
        applicationRepository.save(application);
    }
}
