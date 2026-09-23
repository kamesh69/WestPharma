package com.westpharma.aisdet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;

/**
 * Shared suite hooks. Extent lifecycle is owned by {@link com.westpharma.aisdet.reporting.ExtentTestListener}.
 */
public abstract class BaseTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected abstract String moduleName();

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        log.info("Starting suite for module={}", moduleName());
    }
}
