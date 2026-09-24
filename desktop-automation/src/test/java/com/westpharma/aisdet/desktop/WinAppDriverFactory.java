package com.westpharma.aisdet.desktop;

import com.westpharma.aisdet.config.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.time.Duration;

/**
 * WinAppDriver session factory for Windows Calculator / Notepad.
 * Requires Windows + WinAppDriver listening on desktop.winAppDriverUrl.
 */
public final class WinAppDriverFactory {

    private static WebDriver driver;

    private WinAppDriverFactory() {
    }

    public static WebDriver getDriver() {
        return driver;
    }

    @SuppressWarnings("deprecation")
    public static WebDriver launch(String appIdOrPath) throws Exception {
        if (DesktopOsSupport.resolve() != DesktopOsSupport.Platform.WINDOWS) {
            throw new IllegalStateException(
                    "WinAppDriver is Windows-only. On macOS use Appium Mac2 (desktop.platform=mac). "
                            + DesktopOsSupport.framingNote());
        }
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("platformName", "Windows");
        caps.setCapability("deviceName", "WindowsPC");
        caps.setCapability("app", appIdOrPath);
        String hub = ConfigReader.get("desktop.winAppDriverUrl", "http://127.0.0.1:4723");
        driver = new RemoteWebDriver(new URL(hub), caps);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        return driver;
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
