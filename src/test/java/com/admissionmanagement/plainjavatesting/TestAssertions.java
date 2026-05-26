package com.admissionmanagement.plainjavatesting;

import java.util.Objects;

final class TestAssertions {
    private static int passed;
    private static int failed;

    private TestAssertions() {
    }

    static void run(String testName, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("PASS " + testName);
        } catch (AssertionError | RuntimeException exception) {
            failed++;
            System.out.println("FAIL " + testName);
            exception.printStackTrace(System.out);
        }
    }

    static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected <%s>, but was <%s>".formatted(expected, actual));
        }
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static <T extends Throwable> T assertThrows(Class<T> expectedType, Runnable operation) {
        try {
            operation.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return expectedType.cast(throwable);
            }
            throw new AssertionError(
                    "Expected exception <%s>, but was <%s>"
                            .formatted(expectedType.getName(), throwable.getClass().getName()),
                    throwable
            );
        }
        throw new AssertionError("Expected exception <%s>, but nothing was thrown".formatted(expectedType.getName()));
    }

    static void printSummaryAndFailIfNeeded() {
        System.out.println();
        System.out.println("Test driver summary: passed=" + passed + ", failed=" + failed);
        if (failed > 0) {
            throw new AssertionError("Some test driver checks failed");
        }
    }
}
