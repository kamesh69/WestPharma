package com.westpharma.aisdet.desktop;

import com.westpharma.aisdet.config.ConfigReader;
import io.appium.java_client.mac.Mac2Driver;
import io.appium.java_client.mac.options.Mac2Options;

import java.net.URL;
import java.time.Duration;

/**
 * Appium Mac2 sessions for Calculator.app / TextEdit. Server default: :4724
 * (Android UiAutomator2 stays on :4723).
 */
public final class MacDesktopDriverFactory {

    private static Mac2Driver driver;

    private MacDesktopDriverFactory() {
    }

    public static Mac2Driver getDriver() {
        return driver;
    }

    public static Mac2Driver launch(String bundleId) throws Exception {
        quit();
        Mac2Options options = new Mac2Options();
        options.setPlatformName("Mac");
        options.setAutomationName("Mac2");
        options.setBundleId(bundleId);
        options.setNewCommandTimeout(Duration.ofSeconds(120));
        // Bring app to foreground; do not wipe user data
        options.setCapability("appium:showServerLogs", false);

        String server = ConfigReader.get("desktop.appiumServerUrl", "http://127.0.0.1:4724/");
        driver = new Mac2Driver(new URL(server), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        return driver;
    }

    public static Mac2Driver launchCalculator() throws Exception {
        String bundle = ConfigReader.get("desktop.mac.calculatorBundleId", "com.apple.calculator");
        return launch(bundle);
    }

    public static Mac2Driver launchTextEdit() throws Exception {
        String bundle = ConfigReader.get("desktop.mac.textEditBundleId", "com.apple.TextEdit");
        return launch(bundle);
    }

    public static void quit() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
            driver = null;
        }
    }
}
