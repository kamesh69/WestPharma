package com.westpharma.aisdet.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

public final class WaitUtils {

    private WaitUtils() {
    }

    public static WebDriverWait wait(WebDriver driver, long seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    public static WebElement visible(WebDriver driver, By locator, long seconds) {
        return wait(driver, seconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement clickable(WebDriver driver, By locator, long seconds) {
        return wait(driver, seconds).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static boolean invisible(WebDriver driver, By locator, long seconds) {
        return wait(driver, seconds).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static List<WebElement> visibles(WebDriver driver, By locator, long seconds) {
        return wait(driver, seconds).until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public static <T> T until(WebDriver driver, long seconds, Function<WebDriver, T> condition) {
        return wait(driver, seconds).until(condition);
    }
}
