package com.admissionmanagement.domain.application;

import java.time.LocalDateTime;

public abstract class ApplicationEvent {
    private final LocalDateTime occurredAt;

    protected ApplicationEvent(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
