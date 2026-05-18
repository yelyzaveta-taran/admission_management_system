package com.admissionmanagement.controller.processing;

import com.admissionmanagement.application.processing.ApplicationProcessingService;
import com.admissionmanagement.application.processing.ApplicationScope;
import com.admissionmanagement.dto.ApplicationSearchCriteria;
import com.admissionmanagement.projection.ApplicationDetailsProjection;
import com.admissionmanagement.projection.ApplicationSummaryProjection;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ApplicationProcessingController {
    private final ApplicationProcessingService applicationProcessingService;

    public ApplicationProcessingController(ApplicationProcessingService applicationProcessingService) {
        this.applicationProcessingService = Objects.requireNonNull(applicationProcessingService);
    }

    public List<ApplicationSummaryProjection> getApplications(
            ApplicationScope scope,
            ApplicationSearchCriteria criteria
    ) {
        return applicationProcessingService.getApplications(scope, criteria);
    }

    public Optional<ApplicationDetailsProjection> getApplicationDetails(Integer applicationId) {
        return applicationProcessingService.getApplicationDetails(applicationId);
    }
}
