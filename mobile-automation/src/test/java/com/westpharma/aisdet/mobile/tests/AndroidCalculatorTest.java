package com.westpharma.aisdet.mobile.tests;

import com.westpharma.aisdet.BaseTest;
import com.westpharma.aisdet.mobile.AppiumDriverFactory;
import com.westpharma.aisdet.mobile.screens.AndroidCalculatorScreen;
import com.westpharma.aisdet.reporting.ExtentTestListener;
import com.westpharma.aisdet.reporting.ExtentTestManager;
import com.westpharma.aisdet.utils.AssertUtils;
import io.appium.java_client.android.AndroidDriver;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.net.URL;

@Listeners(ExtentTestListener.class)
public class AndroidCalculatorTest extends BaseTest {

    private AndroidDriver driver;
    private AndroidCalculatorScreen calculator;

    @Override
    protected String moduleName() {
        return "mobile";
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        if (!isAppiumReachable()) {
            throw new SkipException("Appium server not reachable at configured URL. Start Appium + emulator first.");
        }
        driver = AppiumDriverFactory.createDriver();
        calculator = new AndroidCalculatorScreen(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        AppiumDriverFactory.quitDriver();
    }

    @Test(description = "Validate three calculator expressions and clear history")
    public void validateCalculatorExpressions() {
        assertCalculation("(25 + 15) × 3 − 10", "40×3-10", "110");
        assertCalculation("125 ÷ 5 + 18", "125÷5+18", "43");
        assertCalculation("99 × 2 − 45", "99×2-45", "153");

        calculator.clearHistoryIfPresent();
        ExtentTestManager.logPass("Cleared calculation history / display after execution");
    }

    private void assertCalculation(String label, String expression, String expected) {
        ExtentTestManager.logInfo("Evaluating: " + label);
        String actual = calculator.evaluateExpression(expression);
        AssertUtils.assertEquals(actual, expected, "Result mismatch for " + label);
        ExtentTestManager.logPass(label + " = " + actual);
        calculator.clear();
    }

    private boolean isAppiumReachable() {
        try {
            String base = com.westpharma.aisdet.config.ConfigReader.get(
                    "mobile.appiumServerUrl", "http://127.0.0.1:4723");
            URL status = new URL(base.endsWith("/") ? base + "status" : base + "/status");
            HttpURLConnection conn = (HttpURLConnection) status.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            return code >= 200 && code < 500;
        } catch (Exception ex) {
            return false;
        }
    }
}
