package com.admissionmanagement.plainjavatesting;

public final class AllTestDrivers {
    private AllTestDrivers() {
    }

    public static void main(String[] args) {
        System.out.println("Admission Management test drivers");
        System.out.println();

        ApplicationFinishProcessingBasisPathDriver.runAll();
        ApplicationProcessingServiceGrayBoxDriver.runAll();

        TestAssertions.printSummaryAndFailIfNeeded();
    }
}
