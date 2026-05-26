package com.admissionmanagement.plainjavatesting;

import com.admissionmanagement.domain.application.Application;
import com.admissionmanagement.domain.application.ApplicationEvent;
import com.admissionmanagement.domain.application.ApplicationStatus;
import com.admissionmanagement.domain.application.StatusChangeEvent;

import java.util.List;

final class ApplicationFinishProcessingBasisPathDriver {
    private ApplicationFinishProcessingBasisPathDriver() {
    }

    static void runAll() {
        TestAssertions.run(
                "white-box finishProcessing: null final status throws NullPointerException",
                ApplicationFinishProcessingBasisPathDriver::nullFinalStatusThrows
        );
        TestAssertions.run(
                "white-box finishProcessing: not in progress throws IllegalStateException",
                ApplicationFinishProcessingBasisPathDriver::notInProgressThrows
        );
        TestAssertions.run(
                "white-box finishProcessing: PENDING is not a final status",
                () -> nonFinalStatusThrows(ApplicationStatus.PENDING)
        );
        TestAssertions.run(
                "white-box finishProcessing: IN_PROGRESS is not a final status",
                () -> nonFinalStatusThrows(ApplicationStatus.IN_PROGRESS)
        );
        TestAssertions.run(
                "white-box finishProcessing: confirmed creates status-change event",
                () -> finalStatusChangesState(ApplicationStatus.CONFIRMED)
        );
        TestAssertions.run(
                "white-box finishProcessing: rejected creates status-change event",
                () -> finalStatusChangesState(ApplicationStatus.REJECTED)
        );
        TestAssertions.run(
                "white-box finishProcessing: cancelled creates status-change event",
                () -> finalStatusChangesState(ApplicationStatus.CANCELLED)
        );
        TestAssertions.run(
                "white-box finishProcessing: clearNewEvents removes recorded event",
                ApplicationFinishProcessingBasisPathDriver::clearNewEventsRemovesRecordedEvent
        );
    }

    private static void nullFinalStatusThrows() {
        Application application = TestApplicationFactory.application(1, ApplicationStatus.IN_PROGRESS);

        TestAssertions.assertThrows(
                NullPointerException.class,
                () -> application.finishProcessing(null, "Фінальний статус не передано")
        );
    }

    private static void notInProgressThrows() {
        Application application = TestApplicationFactory.application(1, ApplicationStatus.PENDING);

        TestAssertions.assertThrows(
                IllegalStateException.class,
                () -> application.finishProcessing(ApplicationStatus.CONFIRMED, "Зараховано")
        );
    }

    private static void nonFinalStatusThrows(ApplicationStatus status) {
        Application application = TestApplicationFactory.application(1, ApplicationStatus.IN_PROGRESS);

        TestAssertions.assertThrows(
                IllegalArgumentException.class,
                () -> application.finishProcessing(status, "Некоректний фінальний статус")
        );
    }

    private static void finalStatusChangesState(ApplicationStatus finalStatus) {
        Application application = TestApplicationFactory.application(1, ApplicationStatus.IN_PROGRESS);

        application.finishProcessing(finalStatus, "Рішення прийнято");

        TestAssertions.assertEquals(finalStatus, application.getStatus());
        List<ApplicationEvent> events = application.getNewEvents();
        TestAssertions.assertEquals(1, events.size());
        TestAssertions.assertTrue(
                events.get(0) instanceof StatusChangeEvent,
                "Expected StatusChangeEvent after finishing processing"
        );

        StatusChangeEvent event = (StatusChangeEvent) events.get(0);
        TestAssertions.assertEquals(ApplicationStatus.IN_PROGRESS, event.getPreviousStatus());
        TestAssertions.assertEquals(finalStatus, event.getNewStatus());
        TestAssertions.assertEquals("Рішення прийнято", event.getReason());
        TestAssertions.assertTrue(event.getOccurredAt() != null, "Event time must be recorded");
    }

    private static void clearNewEventsRemovesRecordedEvent() {
        Application application = TestApplicationFactory.application(1, ApplicationStatus.IN_PROGRESS);

        application.finishProcessing(ApplicationStatus.CONFIRMED, "Зараховано");
        application.clearNewEvents();

        TestAssertions.assertTrue(application.getNewEvents().isEmpty(), "New event list must be empty");
    }
}
