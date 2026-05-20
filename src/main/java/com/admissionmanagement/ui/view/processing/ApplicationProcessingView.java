package com.admissionmanagement.ui.view.processing;

import com.admissionmanagement.application.processing.ApplicationScope;
import com.admissionmanagement.controller.processing.ApplicationProcessingController;
import com.admissionmanagement.projection.ApplicationSummaryProjection;
import com.admissionmanagement.ui.view.processing.component.ApplicationCardFactory;
import com.admissionmanagement.ui.view.processing.component.ApplicationFilterPanel;
import com.admissionmanagement.ui.view.processing.dialog.ApplicationDetailsDialog;
import com.admissionmanagement.ui.view.processing.dialog.CommunicationDialog;
import com.admissionmanagement.ui.view.processing.dialog.FinishProcessingDialog;
import com.admissionmanagement.ui.view.processing.support.ApplicationProcessingTaskRunner;
import com.admissionmanagement.ui.view.processing.support.ApplicationProcessingUiSupport;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.Objects;

public class ApplicationProcessingView {
    private final ApplicationProcessingController controller;
    private final BorderPane root;
    private final TabPane scopeTabs;
    private final ApplicationFilterPanel filterPanel;
    private final ApplicationCardFactory cardFactory;
    private final CommunicationDialog communicationDialog;
    private final FinishProcessingDialog finishProcessingDialog;
    private final ApplicationDetailsDialog detailsDialog;
    private final VBox applicationsBox;
    private final Label stateLabel;

    public ApplicationProcessingView(ApplicationProcessingController controller) {
        this.controller = Objects.requireNonNull(controller);
        this.root = new BorderPane();
        this.scopeTabs = new TabPane();
        this.filterPanel = new ApplicationFilterPanel(() -> loadApplications(selectedScope()));
        this.communicationDialog = new CommunicationDialog(this.controller);
        this.finishProcessingDialog = new FinishProcessingDialog(this.controller);
        this.detailsDialog = new ApplicationDetailsDialog(
                this.controller,
                communicationDialog,
                finishProcessingDialog,
                () -> loadApplications(selectedScope())
        );
        this.cardFactory = new ApplicationCardFactory(new ApplicationCardActions());
        this.applicationsBox = new VBox(10);
        this.stateLabel = new Label();

        configureView();
    }

    public Node getRoot() {
        return root;
    }

    public void loadInitialApplications() {
        loadApplications(selectedScope());
    }

    private void configureView() {
        root.setPadding(new Insets(24));
        root.setTop(createHeader());
        root.setCenter(createContent());
    }

    private Node createHeader() {
        Label title = new Label("Application Processing");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700;");
        BorderPane header = new BorderPane(title);
        header.setPadding(new Insets(0, 0, 18, 0));
        return header;
    }

    private Node createContent() {
        VBox content = new VBox(14);
        content.getChildren().addAll(createTabs(), filterPanel.getRoot(), createApplicationsScrollPane());
        VBox.setVgrow(content.getChildren().get(2), Priority.ALWAYS);
        return content;
    }

    private Node createTabs() {
        scopeTabs.getTabs().addAll(
                createScopeTab("Pending", ApplicationScope.PENDING),
                createScopeTab("In Progress", ApplicationScope.IN_PROGRESS),
                createScopeTab("Processed", ApplicationScope.PROCESSED)
        );
        scopeTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        scopeTabs.setMinHeight(42);
        scopeTabs.setPrefHeight(42);
        scopeTabs.setMaxHeight(42);
        scopeTabs.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldTab, newTab) -> loadApplications(selectedScope()));
        return scopeTabs;
    }

    private Tab createScopeTab(String title, ApplicationScope scope) {
        Tab tab = new Tab(title);
        tab.setUserData(scope);
        return tab;
    }

    private Node createApplicationsScrollPane() {
        applicationsBox.setPadding(new Insets(4, 2, 4, 2));

        VBox scrollContent = new VBox(12, stateLabel, applicationsBox);
        scrollContent.setPadding(new Insets(2));

        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    private ApplicationScope selectedScope() {
        Tab selectedTab = scopeTabs.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            return ApplicationScope.PENDING;
        }
        return (ApplicationScope) selectedTab.getUserData();
    }

    private void loadApplications(ApplicationScope scope) {
        stateLabel.setText("Loading applications...");
        applicationsBox.getChildren().clear();

        ApplicationProcessingTaskRunner.runTask(
                "load-applications",
                () -> controller.getApplications(scope, filterPanel.readCriteria()),
                applications -> renderApplications(scope, applications),
                exception -> stateLabel.setText("Could not load applications: " + exception.getMessage())
        );
    }

    private void renderApplications(ApplicationScope scope, List<ApplicationSummaryProjection> applications) {
        applicationsBox.getChildren().clear();
        if (applications.isEmpty()) {
            stateLabel.setText("No applications found.");
            return;
        }

        stateLabel.setText(applications.size() + " applications found");
        applications.stream()
                .map(application -> cardFactory.createCard(scope, application))
                .forEach(card -> applicationsBox.getChildren().add(card));
    }

    private void startApplicationProcessing(Integer applicationId, Button startProcessingButton) {
        Window owner = startProcessingButton.getScene().getWindow();
        startProcessingButton.setDisable(true);
        startProcessingButton.setText("Starting...");

        ApplicationProcessingTaskRunner.runTask(
                "start-application-processing",
                () -> {
                    controller.startApplicationProcessing(applicationId);
                    return null;
                },
                ignored -> loadApplications(selectedScope()),
                exception -> {
                    startProcessingButton.setDisable(false);
                    startProcessingButton.setText("Start Processing");
                    ApplicationProcessingUiSupport.showError(
                            owner,
                            "Could not start application processing: " + exception.getMessage()
                    );
                }
        );
    }

    private final class ApplicationCardActions implements ApplicationCardFactory.CardActions {
        @Override
        public void loadDetails(Integer applicationId, Button detailsButton) {
            detailsDialog.loadAndOpen(applicationId, detailsButton);
        }

        @Override
        public void startProcessing(Integer applicationId, Button startProcessingButton) {
            startApplicationProcessing(applicationId, startProcessingButton);
        }

        @Override
        public void recordCommunication(
                Window owner,
                Integer applicationId,
                String applicantName,
                String programName
        ) {
            communicationDialog.open(
                    owner,
                    applicationId,
                    applicantName,
                    programName,
                    () -> loadApplications(selectedScope())
            );
        }

        @Override
        public void finishProcessing(
                Window owner,
                Integer applicationId,
                String applicantName,
                String programName
        ) {
            finishProcessingDialog.open(
                    owner,
                    applicationId,
                    applicantName,
                    programName,
                    () -> loadApplications(selectedScope())
            );
        }
    }
}
