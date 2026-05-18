package com.admissionmanagement.domain.application;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Application {
    private static final ZoneId APPLICATION_TIME_ZONE = ZoneId.of("Europe/Kyiv");

    private final Integer applicationId;
    private final Integer programId;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String email;
    private final String comment;
    private ApplicationStatus status;
    private final LocalDateTime createdAt;
    private final List<ApplicationEvent> newEvents;

    public Application(
            Integer applicationId,
            Integer programId,
            String firstName,
            String lastName,
            String phone,
            String email,
            String comment,
            ApplicationStatus status,
            LocalDateTime createdAt
    ) {
        this.applicationId = applicationId;
        this.programId = programId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.comment = comment;
        this.status = status;
        this.createdAt = createdAt;
        this.newEvents = new ArrayList<>();
    }

    public Integer getApplicationId() {
        return applicationId;
    }

    public Integer getProgramId() {
        return programId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getComment() {
        return comment;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ApplicationEvent> getNewEvents() {
        return List.copyOf(newEvents);
    }

    public void startProcessing() {
        if (status != ApplicationStatus.PENDING) {
            throw new IllegalStateException("Only pending applications can be started for processing");
        }

        ApplicationStatus previousStatus = status;
        status = ApplicationStatus.IN_PROGRESS;
        newEvents.add(new StatusChangeEvent(
                previousStatus,
                status,
                "Заявку взято в опрацювання",
                LocalDateTime.now(APPLICATION_TIME_ZONE)
        ));
    }

    public void finishProcessing(ApplicationStatus finalStatus, String reason) {
        Objects.requireNonNull(finalStatus);

        if (status != ApplicationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only applications in progress can be finished");
        }
        if (!isFinalStatus(finalStatus)) {
            throw new IllegalArgumentException("Application can be finished only with a final status");
        }

        ApplicationStatus previousStatus = status;
        status = finalStatus;
        newEvents.add(new StatusChangeEvent(
                previousStatus,
                status,
                reason,
                LocalDateTime.now(APPLICATION_TIME_ZONE)
        ));
    }

    public void recordCommunication(
            CommunicationChannel channel,
            CommunicationResult result,
            String comment
    ) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(result);

        if (status != ApplicationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Communication can be recorded only for applications in progress");
        }

        newEvents.add(new CommunicationEvent(
                channel,
                result,
                comment,
                LocalDateTime.now(APPLICATION_TIME_ZONE)
        ));
    }

    public void clearNewEvents() {
        newEvents.clear();
    }

    private boolean isFinalStatus(ApplicationStatus status) {
        return status == ApplicationStatus.CONFIRMED
                || status == ApplicationStatus.REJECTED
                || status == ApplicationStatus.CANCELLED;
    }
}
