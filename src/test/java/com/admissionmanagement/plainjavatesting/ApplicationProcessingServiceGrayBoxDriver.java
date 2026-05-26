package com.admissionmanagement.plainjavatesting;

import com.admissionmanagement.application.processing.ApplicationProcessingService;
import com.admissionmanagement.application.processing.ApplicationScope;
import com.admissionmanagement.domain.application.Application;
import com.admissionmanagement.domain.application.ApplicationEvent;
import com.admissionmanagement.domain.application.ApplicationStatus;
import com.admissionmanagement.domain.application.CommunicationChannel;
import com.admissionmanagement.domain.application.CommunicationEvent;
import com.admissionmanagement.domain.application.CommunicationResult;
import com.admissionmanagement.domain.application.StatusChangeEvent;
import com.admissionmanagement.dto.ApplicationSearchCriteria;
import com.admissionmanagement.dto.CommunicationRequest;
import com.admissionmanagement.dto.FinishProcessingRequest;
import com.admissionmanagement.dto.LastCommunicationFilterMode;
import com.admissionmanagement.projection.ApplicationSummaryProjection;

import java.time.LocalDateTime;
import java.util.List;

final class ApplicationProcessingServiceGrayBoxDriver {
    private ApplicationProcessingServiceGrayBoxDriver() {
    }

    static void runAll() {
        TestAssertions.run(
                "gray-box service: startApplicationProcessing loads domain object and saves changed aggregate",
                ApplicationProcessingServiceGrayBoxDriver::startApplicationProcessingSavesChangedAggregate
        );
        TestAssertions.run(
                "gray-box service: missing application prevents save",
                ApplicationProcessingServiceGrayBoxDriver::missingApplicationPreventsSave
        );
        TestAssertions.run(
                "gray-box service: recordCommunication adds communication event and saves",
                ApplicationProcessingServiceGrayBoxDriver::recordCommunicationAddsEventAndSaves
        );
        TestAssertions.run(
                "gray-box service: finishApplicationProcessing delegates to aggregate and saves final status",
                ApplicationProcessingServiceGrayBoxDriver::finishApplicationProcessingSavesFinalStatus
        );
        TestAssertions.run(
                "gray-box service: getApplications delegates statuses from ApplicationScope",
                ApplicationProcessingServiceGrayBoxDriver::getApplicationsDelegatesStatusesFromScope
        );
        TestAssertions.run(
                "gray-box service: query methods delegate identifiers to query repository",
                ApplicationProcessingServiceGrayBoxDriver::queryMethodsDelegateIdentifiers
        );
    }

    private static void startApplicationProcessingSavesChangedAggregate() {
        InMemoryApplicationRepositoryStub commandRepository = new InMemoryApplicationRepositoryStub();
        CapturingApplicationQueryRepositoryStub queryRepository = new CapturingApplicationQueryRepositoryStub();
        Application application = TestApplicationFactory.application(7, ApplicationStatus.PENDING);
        commandRepository.put(application);
        ApplicationProcessingService service = new ApplicationProcessingService(commandRepository, queryRepository);

        service.startApplicationProcessing(7);

        TestAssertions.assertEquals(List.of(7), commandRepository.requestedIds());
        TestAssertions.assertEquals(1, commandRepository.saveCount());
        TestAssertions.assertEquals(ApplicationStatus.IN_PROGRESS, commandRepository.lastSavedApplication().getStatus());
        ApplicationEvent event = commandRepository.lastSavedApplication().getNewEvents().get(0);
        TestAssertions.assertTrue(event instanceof StatusChangeEvent, "Expected status-change event");
    }

    private static void missingApplicationPreventsSave() {
        InMemoryApplicationRepositoryStub commandRepository = new InMemoryApplicationRepositoryStub();
        CapturingApplicationQueryRepositoryStub queryRepository = new CapturingApplicationQueryRepositoryStub();
        ApplicationProcessingService service = new ApplicationProcessingService(commandRepository, queryRepository);

        TestAssertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.startApplicationProcessing(404)
        );
        TestAssertions.assertEquals(List.of(404), commandRepository.requestedIds());
        TestAssertions.assertEquals(0, commandRepository.saveCount());
    }

    private static void recordCommunicationAddsEventAndSaves() {
        InMemoryApplicationRepositoryStub commandRepository = new InMemoryApplicationRepositoryStub();
        CapturingApplicationQueryRepositoryStub queryRepository = new CapturingApplicationQueryRepositoryStub();
        Application application = TestApplicationFactory.application(8, ApplicationStatus.IN_PROGRESS);
        commandRepository.put(application);
        ApplicationProcessingService service = new ApplicationProcessingService(commandRepository, queryRepository);
        CommunicationRequest request = new CommunicationRequest(
                CommunicationChannel.PHONE,
                CommunicationResult.ANSWERED,
                "Кандидат підтвердив зацікавленість"
        );

        service.recordCommunication(8, request);

        TestAssertions.assertEquals(1, commandRepository.saveCount());
        Application savedApplication = commandRepository.lastSavedApplication();
        TestAssertions.assertEquals(ApplicationStatus.IN_PROGRESS, savedApplication.getStatus());
        ApplicationEvent event = savedApplication.getNewEvents().get(0);
        TestAssertions.assertTrue(event instanceof CommunicationEvent, "Expected communication event");
        CommunicationEvent communicationEvent = (CommunicationEvent) event;
        TestAssertions.assertEquals(CommunicationChannel.PHONE, communicationEvent.getChannel());
        TestAssertions.assertEquals(CommunicationResult.ANSWERED, communicationEvent.getResult());
        TestAssertions.assertEquals("Кандидат підтвердив зацікавленість", communicationEvent.getComment());
    }

    private static void finishApplicationProcessingSavesFinalStatus() {
        InMemoryApplicationRepositoryStub commandRepository = new InMemoryApplicationRepositoryStub();
        CapturingApplicationQueryRepositoryStub queryRepository = new CapturingApplicationQueryRepositoryStub();
        Application application = TestApplicationFactory.application(9, ApplicationStatus.IN_PROGRESS);
        commandRepository.put(application);
        ApplicationProcessingService service = new ApplicationProcessingService(commandRepository, queryRepository);

        service.finishApplicationProcessing(
                9,
                new FinishProcessingRequest(ApplicationStatus.REJECTED, "Недостатній рівень підготовки")
        );

        TestAssertions.assertEquals(1, commandRepository.saveCount());
        TestAssertions.assertEquals(ApplicationStatus.REJECTED, commandRepository.lastSavedApplication().getStatus());
        ApplicationEvent event = commandRepository.lastSavedApplication().getNewEvents().get(0);
        TestAssertions.assertTrue(event instanceof StatusChangeEvent, "Expected status-change event");
    }

    private static void getApplicationsDelegatesStatusesFromScope() {
        InMemoryApplicationRepositoryStub commandRepository = new InMemoryApplicationRepositoryStub();
        CapturingApplicationQueryRepositoryStub queryRepository = new CapturingApplicationQueryRepositoryStub();
        ApplicationProcessingService service = new ApplicationProcessingService(commandRepository, queryRepository);
        ApplicationSearchCriteria criteria = new ApplicationSearchCriteria(
                "+38050",
                "example.com",
                CommunicationResult.ANSWERED,
                LastCommunicationFilterMode.BY_RESULT,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 31, 23, 59)
        );
        List<ApplicationSummaryProjection> expectedResult = List.of(new ApplicationSummaryProjection(
                12,
                "Іван Петренко",
                "Java",
                ApplicationStatus.CONFIRMED,
                CommunicationResult.ANSWERED,
                LocalDateTime.of(2026, 1, 12, 10, 0)
        ));
        queryRepository.returnApplications(expectedResult);

        List<ApplicationSummaryProjection> actualResult = service.getApplications(ApplicationScope.PROCESSED, criteria);

        TestAssertions.assertEquals(expectedResult, actualResult);
        TestAssertions.assertEquals(
                List.of(ApplicationStatus.CONFIRMED, ApplicationStatus.REJECTED, ApplicationStatus.CANCELLED),
                queryRepository.lastStatuses()
        );
        TestAssertions.assertEquals(criteria, queryRepository.lastCriteria());
    }

    private static void queryMethodsDelegateIdentifiers() {
        InMemoryApplicationRepositoryStub commandRepository = new InMemoryApplicationRepositoryStub();
        CapturingApplicationQueryRepositoryStub queryRepository = new CapturingApplicationQueryRepositoryStub();
        ApplicationProcessingService service = new ApplicationProcessingService(commandRepository, queryRepository);

        service.getApplicationDetails(33);
        service.getApplicationEvents(34);
        service.getAllApplicationEvents();

        TestAssertions.assertEquals(33, queryRepository.lastDetailsId());
        TestAssertions.assertEquals(34, queryRepository.lastEventsApplicationId());
    }
}
