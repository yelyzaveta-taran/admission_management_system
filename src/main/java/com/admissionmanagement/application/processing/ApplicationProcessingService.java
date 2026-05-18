package com.admissionmanagement.application.processing;

import com.admissionmanagement.dto.ApplicationSearchCriteria;
import com.admissionmanagement.projection.ApplicationDetailsProjection;
import com.admissionmanagement.projection.ApplicationSummaryProjection;
import com.admissionmanagement.repository.ApplicationQueryRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ApplicationProcessingService {
    private final ApplicationQueryRepository applicationQueryRepository;

    public ApplicationProcessingService(ApplicationQueryRepository applicationQueryRepository) {
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
}
