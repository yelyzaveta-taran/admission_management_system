package com.admissionmanagement.ui.view.analytics.support;

import javafx.concurrent.Task;

import java.util.function.Consumer;

public final class AnalyticsTaskRunner {
    private AnalyticsTaskRunner() {
    }

    public static <T> void runTask(
            String threadName,
            TaskOperation<T> operation,
            Consumer<T> onSuccess,
            Consumer<Throwable> onFailure
    ) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.call();
            }
        };

        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onFailure.accept(task.getException()));

        Thread thread = new Thread(task, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    @FunctionalInterface
    public interface TaskOperation<T> {
        T call() throws Exception;
    }
}
