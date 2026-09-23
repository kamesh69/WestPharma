package com.westpharma.aisdet.mobile;

import com.westpharma.aisdet.config.ConfigReader;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.By;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class AppiumDriverFactory {

    private static final ThreadLocal<AndroidDriver> DRIVER = new ThreadLocal<>();

    private AppiumDriverFactory() {
    }

    public static AndroidDriver getDriver() {
        return DRIVER.get();
    }

    public static AndroidDriver createDriver() throws Exception {
        String appPackage = ConfigReader.get("mobile.appPackage", "com.google.android.calculator");
        String appActivity = ConfigReader.get("mobile.appActivity", "com.android.calculator2.CalculatorGoogle");

        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName(ConfigReader.get("mobile.platformName", "Android"))
                .setAutomationName(ConfigReader.get("mobile.automationName", "UiAutomator2"))
                .setDeviceName(ConfigReader.get("mobile.deviceName", "emulator-5554"))
                .setAppPackage(appPackage)
                .setAppActivity(appActivity)
                .setAppWaitActivity("com.android.calculator2.*")
                .setAutoGrantPermissions(true)
                .setNoReset(false)
                .setFullReset(false)
                .setNewCommandTimeout(Duration.ofSeconds(120));

        String apk = ConfigReader.get("mobile.appPath", "mobile-automation/apps/calculator.apk");
        if (apk != null && !apk.isBlank()) {
            Path apkPath = ConfigReader.resolveFromRoot(apk);
            if (Files.isRegularFile(apkPath)) {
                options.setApp(apkPath.toAbsolutePath().toString());
            }
        }

        String server = ConfigReader.get("mobile.appiumServerUrl", "http://127.0.0.1:4723");
        AndroidDriver driver = new AndroidDriver(new URL(server), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        DRIVER.set(driver);

        try {
            driver.activateApp(appPackage);
        } catch (Exception ignored) {
        }
        // Fallback explicit start if digit pad still missing
        try {
            driver.executeScript("mobile: startActivity",
                    java.util.Map.of(
                            "component", appPackage + "/" + appActivity));
        } catch (Exception ignored) {
            try {
                Runtime.getRuntime().exec(new String[]{
                        "adb", "shell", "am", "start", "-n",
                        appPackage + "/" + appActivity
                });
            } catch (Exception ignored2) {
            }
        }
        waitForDigitPad(driver);
        return driver;
    }

    private static void waitForDigitPad(AndroidDriver driver) {
        long deadline = System.currentTimeMillis() + 25_000;
        By digit = By.id("com.google.android.calculator:id/digit_5");
        By okButton = By.id("android:id/button1");
        By okText = By.xpath("//android.widget.Button[@text='OK' or @text='Ok']");
        while (System.currentTimeMillis() < deadline) {
            // API 34 shows DeprecatedTargetSdkVersionDialog over old calculator APKs
            dismissIfPresent(driver, okButton);
            dismissIfPresent(driver, okText);
            try {
                if (!driver.findElements(digit).isEmpty()) {
                    return;
                }
            } catch (Exception ignored) {
            }
            try {
                driver.activateApp(ConfigReader.get("mobile.appPackage", "com.google.android.calculator"));
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new IllegalStateException("Calculator digit pad not visible after launch");
    }

    private static void dismissIfPresent(AndroidDriver driver, By by) {
        try {
            var buttons = driver.findElements(by);
            if (!buttons.isEmpty() && buttons.get(0).isDisplayed()) {
                buttons.get(0).click();
                Thread.sleep(300);
            }
        } catch (Exception ignored) {
        }
    }

    public static void quitDriver() {
        AndroidDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
            DRIVER.remove();
        }
    }
}
