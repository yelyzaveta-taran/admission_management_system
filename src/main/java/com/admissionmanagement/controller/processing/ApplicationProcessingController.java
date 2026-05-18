package com.admissionmanagement.controller.processing;

import com.admissionmanagement.application.processing.ApplicationProcessingService;
import com.admissionmanagement.application.processing.ApplicationScope;
import com.admissionmanagement.dto.ApplicationSearchCriteria;
import com.admissionmanagement.dto.CommunicationRequest;
import com.admissionmanagement.dto.FinishProcessingRequest;
import com.admissionmanagement.projection.ApplicationDetailsProjection;
import com.admissionmanagement.projection.ApplicationEventProjection;
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

    public List<ApplicationEventProjection> getApplicationEvents(Integer applicationId) {
        return applicationProcessingService.getApplicationEvents(applicationId);
    }

    public void startApplicationProcessing(Integer applicationId) {
        applicationProcessingService.startApplicationProcessing(applicationId);
    }

    public void recordCommunication(Integer applicationId, CommunicationRequest communicationData) {
        applicationProcessingService.recordCommunication(applicationId, communicationData);
    }

    public void finishApplicationProcessing(Integer applicationId, FinishProcessingRequest result) {
        applicationProcessingService.finishApplicationProcessing(applicationId, result);
    }
}
