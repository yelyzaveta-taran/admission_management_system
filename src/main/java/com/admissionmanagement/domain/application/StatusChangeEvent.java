package com.admissionmanagement.domain.application;

import java.time.LocalDateTime;

public class StatusChangeEvent extends ApplicationEvent {
    private final ApplicationStatus previousStatus;
    private final ApplicationStatus newStatus;
    private final String reason;

    public StatusChangeEvent(
            ApplicationStatus previousStatus,
            ApplicationStatus newStatus,
            String reason,
            LocalDateTime occurredAt
    ) {
        super(occurredAt);
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    public ApplicationStatus getPreviousStatus() {
        return previousStatus;
    }

    public ApplicationStatus getNewStatus() {
        return newStatus;
    }

    public String getReason() {
        return reason;
    }
}
