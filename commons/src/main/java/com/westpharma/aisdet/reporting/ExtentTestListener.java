package com.westpharma.aisdet.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class ExtentTestListener implements ITestListener {

    private static final Logger LOG = LoggerFactory.getLogger(ExtentTestListener.class);
    private final String moduleName;

    public ExtentTestListener() {
        this("suite");
    }

    public ExtentTestListener(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public void onStart(ITestContext context) {
        String name = context.getSuite().getParameter("moduleName");
        if (name == null || name.isBlank()) {
            name = moduleName;
        }
        ExtentManager.getInstance(name);
        LOG.info("Extent report initialized for module={}", name);
    }

    @Override
    public void onTestStart(ITestResult result) {
        ExtentReports extent = ExtentManager.getInstance(moduleName);
        ExtentTest test = extent.createTest(result.getMethod().getMethodName(),
                result.getMethod().getDescription());
        ExtentTestManager.setTest(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTestManager.logPass("Test passed");
        ExtentTestManager.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Throwable t = result.getThrowable();
        ExtentTest test = ExtentTestManager.getTest();
        if (test != null) {
            if (t != null) {
                test.log(Status.FAIL, t);
            } else {
                test.log(Status.FAIL, "Test failed");
            }
        }
        ExtentTestManager.remove();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();
        if (test != null) {
            test.skip(result.getThrowable() == null ? "Skipped" : result.getThrowable().getMessage());
        }
        ExtentTestManager.remove();
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentManager.flush();
        LOG.info("Extent report written to {}", ExtentManager.getReportPath());
        ExtentManager.reset();
    }
}
