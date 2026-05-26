package com.admissionmanagement.plainjavatesting;

import com.admissionmanagement.domain.application.Application;
import com.admissionmanagement.repository.ApplicationRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class InMemoryApplicationRepositoryStub implements ApplicationRepository {
    private final Map<Integer, Application> applications = new HashMap<>();
    private final List<Integer> requestedIds = new ArrayList<>();
    private final List<Application> savedApplications = new ArrayList<>();

    void put(Application application) {
        applications.put(application.getApplicationId(), application);
    }

    List<Integer> requestedIds() {
        return List.copyOf(requestedIds);
    }

    int saveCount() {
        return savedApplications.size();
    }

    Application lastSavedApplication() {
        if (savedApplications.isEmpty()) {
            return null;
        }
        return savedApplications.get(savedApplications.size() - 1);
    }

    @Override
    public Optional<Application> findById(Integer applicationId) {
        requestedIds.add(applicationId);
        return Optional.ofNullable(applications.get(applicationId));
    }

    @Override
    public void save(Application application) {
        savedApplications.add(application);
        applications.put(application.getApplicationId(), application);
    }
}
