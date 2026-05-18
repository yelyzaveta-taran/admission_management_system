package com.admissionmanagement.repository;

import com.admissionmanagement.domain.application.Application;

import java.util.Optional;

public interface ApplicationRepository {
    Optional<Application> findById(Integer applicationId);

    void save(Application application);
}
