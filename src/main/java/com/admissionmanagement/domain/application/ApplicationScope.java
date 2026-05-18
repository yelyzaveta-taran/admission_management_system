package com.admissionmanagement.domain.application;

import java.util.List;

public enum ApplicationScope {
    PENDING(List.of(ApplicationStatus.PENDING)),
    IN_PROGRESS(List.of(ApplicationStatus.IN_PROGRESS)),
    PROCESSED(List.of(
            ApplicationStatus.CONFIRMED,
            ApplicationStatus.REJECTED,
            ApplicationStatus.CANCELLED
    ));

    private final List<ApplicationStatus> statuses;

    ApplicationScope(List<ApplicationStatus> statuses) {
        this.statuses = statuses;
    }

    public List<ApplicationStatus> getStatuses() {
        return statuses;
    }
}
