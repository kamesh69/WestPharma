package com.westpharma.aisdet.utils;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Single-retry analyzer for flaky public demo environments.
 * Retries only when the previous attempt failed.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int attempt;
    private static final int MAX = 1;

    @Override
    public boolean retry(ITestResult result) {
        if (result.isSuccess()) {
            return false;
        }
        if (attempt < MAX) {
            attempt++;
            return true;
        }
        return false;
    }
}
