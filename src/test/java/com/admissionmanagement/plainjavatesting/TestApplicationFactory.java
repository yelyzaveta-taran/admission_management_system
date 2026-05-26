package com.admissionmanagement.plainjavatesting;

import com.admissionmanagement.domain.application.Application;
import com.admissionmanagement.domain.application.ApplicationStatus;

import java.time.LocalDateTime;

final class TestApplicationFactory {
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 10, 9, 30);

    private TestApplicationFactory() {
    }

    static Application application(Integer id, ApplicationStatus status) {
        return new Application(
                id,
                101,
                "Олена",
                "Коваленко",
                "+380501112233",
                "olena.kovalenko@example.com",
                "Тестова заявка",
                status,
                CREATED_AT
        );
    }
}
