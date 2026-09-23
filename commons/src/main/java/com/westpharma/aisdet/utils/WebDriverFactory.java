package com.westpharma.aisdet.utils;

import com.westpharma.aisdet.config.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

public final class WebDriverFactory {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private WebDriverFactory() {
    }

    public static WebDriver getDriver() {
        return DRIVER.get();
    }

    public static WebDriver createDriver() {
        String browser = ConfigReader.get("web.browser", "chrome").toLowerCase();
        boolean headless = ConfigReader.getBoolean("web.headless", false);
        WebDriver driver;
        switch (browser) {
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions options = new FirefoxOptions();
                if (headless) {
                    options.addArguments("-headless");
                }
                driver = new FirefoxDriver(options);
            }
            default -> {
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.addArguments("--window-size=1440,900");
                options.addArguments("--disable-gpu");
                options.addArguments("--no-sandbox");
                driver = new ChromeDriver(options);
            }
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(
                ConfigReader.getInt("web.implicitWaitSeconds", 0)));
        driver.manage().window().maximize();
        DRIVER.set(driver);
        return driver;
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }
}
