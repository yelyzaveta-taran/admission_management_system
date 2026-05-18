package com.admissionmanagement.domain.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Application {
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
                LocalDateTime.now()
        ));
    }

    public void finishProcessing() {
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
                LocalDateTime.now()
        ));
    }

    public void clearNewEvents() {
        newEvents.clear();
    }
}
