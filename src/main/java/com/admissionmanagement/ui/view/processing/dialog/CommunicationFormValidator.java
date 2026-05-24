package com.admissionmanagement.ui.view.processing.dialog;

import com.admissionmanagement.domain.application.CommunicationChannel;
import com.admissionmanagement.domain.application.CommunicationResult;
import com.admissionmanagement.dto.CommunicationRequest;

final class CommunicationFormValidator {
    static final String MISSING_CHANNEL_MESSAGE = "Please select communication channel.";
    static final String MISSING_RESULT_MESSAGE = "Please select communication result.";

    private CommunicationFormValidator() {
    }

    static ValidationResult validate(
            CommunicationChannel channel,
            CommunicationResult result,
            String comment
    ) {
        if (channel == null) {
            return ValidationResult.invalid(MISSING_CHANNEL_MESSAGE);
        }
        if (result == null) {
            return ValidationResult.invalid(MISSING_RESULT_MESSAGE);
        }
        return ValidationResult.valid(new CommunicationRequest(
                channel,
                result,
                trimToNull(comment)
        ));
    }

    record ValidationResult(CommunicationRequest request, String errorMessage) {
        static ValidationResult valid(CommunicationRequest request) {
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
