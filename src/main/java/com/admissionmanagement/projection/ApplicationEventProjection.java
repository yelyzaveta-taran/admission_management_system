package com.admissionmanagement.projection;

import java.time.LocalDateTime;

public record ApplicationEventProjection(
        Integer applicationId,
        LocalDateTime eventTime,
        String eventType,
        String description
) {
}
