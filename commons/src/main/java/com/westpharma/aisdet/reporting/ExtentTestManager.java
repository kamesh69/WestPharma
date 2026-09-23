package com.westpharma.aisdet.reporting;

import com.aventstack.extentreports.ExtentTest;

public final class ExtentTestManager {

    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    private ExtentTestManager() {
    }

    public static void setTest(ExtentTest test) {
        CURRENT.set(test);
    }

    public static ExtentTest getTest() {
        return CURRENT.get();
    }

    public static void logInfo(String message) {
        ExtentTest test = CURRENT.get();
        if (test != null) {
            test.info(message);
        }
    }

    public static void logPass(String message) {
        ExtentTest test = CURRENT.get();
        if (test != null) {
            test.pass(message);
        }
    }

    public static void logFail(String message) {
        ExtentTest test = CURRENT.get();
        if (test != null) {
            test.fail(message);
        }
    }

    public static void remove() {
        CURRENT.remove();
    }
}
