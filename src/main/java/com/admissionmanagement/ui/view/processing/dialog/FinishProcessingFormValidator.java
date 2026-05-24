package com.admissionmanagement.ui.view.processing.dialog;

import com.admissionmanagement.domain.application.ApplicationStatus;
import com.admissionmanagement.dto.FinishProcessingRequest;

final class FinishProcessingFormValidator {
    static final String MISSING_FINAL_STATUS_MESSAGE = "Please select a final decision.";

    private FinishProcessingFormValidator() {
    }

    static ValidationResult validate(ApplicationStatus finalStatus, String reason) {
        if (finalStatus == null) {
            return ValidationResult.invalid(MISSING_FINAL_STATUS_MESSAGE);
        }
        return ValidationResult.valid(new FinishProcessingRequest(
                finalStatus,
                trimToNull(reason)
        ));
    }

    record ValidationResult(FinishProcessingRequest request, String errorMessage) {
        static ValidationResult valid(FinishProcessingRequest request) {
            return new ValidationResult(request, null);
        }

        static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(null, errorMessage);
        }

        boolean isValid() {
            return errorMessage == null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
