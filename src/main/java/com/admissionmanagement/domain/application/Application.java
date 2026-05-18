package com.admissionmanagement.domain.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    }

    public void finishProcessing() {
    }

    public void recordCommunication() {
    }

    public void clearNewEvents() {
    }
}
