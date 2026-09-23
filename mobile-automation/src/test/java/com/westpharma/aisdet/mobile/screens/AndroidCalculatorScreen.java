package com.westpharma.aisdet.mobile.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class AndroidCalculatorScreen {

    private static final String PKG = "com.google.android.calculator:id/";

    private final AndroidDriver driver;
    private final WebDriverWait shortWait;

    public AndroidCalculatorScreen(AndroidDriver driver) {
        this.driver = driver;
        this.shortWait = new WebDriverWait(driver, Duration.ofSeconds(8));
    }

    public void clear() {
        if (clickFirst(AppiumBy.accessibilityId("clear"), AppiumBy.id(PKG + "clr"))) {
            return;
        }
        for (int i = 0; i < 12; i++) {
            if (!clickFirst(AppiumBy.accessibilityId("delete"), AppiumBy.id(PKG + "del"))) {
                break;
            }
        }
    }

    public String evaluateExpression(String expression) {
        clear();
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            tapToken(c, expression, i);
        }
        clickFirst(AppiumBy.accessibilityId("equals"), AppiumBy.id(PKG + "eq"));
        return readResult();
    }

    private void tapToken(char c, String expression, int index) {
        boolean ok = switch (c) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> clickDigit(c);
            case '+' -> clickFirst(AppiumBy.accessibilityId("plus"), AppiumBy.id(PKG + "op_add"));
            case '-' -> clickFirst(AppiumBy.accessibilityId("minus"), AppiumBy.id(PKG + "op_sub"));
            case '×', '*' -> clickFirst(AppiumBy.accessibilityId("multiply"), AppiumBy.id(PKG + "op_mul"));
            case '÷', '/' -> clickFirst(AppiumBy.accessibilityId("divide"), AppiumBy.id(PKG + "op_div"));
            case '(' -> clickFirst(
                    AppiumBy.accessibilityId("left parenthesis"),
                    AppiumBy.id(PKG + "parens"));
            case ')' -> clickFirst(
                    AppiumBy.accessibilityId("right parenthesis"),
                    AppiumBy.id(PKG + "parens"));
            default -> throw new IllegalArgumentException(
                    "Unsupported token '" + c + "' in " + expression + " @" + index);
        };
        if (!ok) {
            throw new IllegalStateException("Unable to tap token '" + c + "'");
        }
    }

    private boolean clickDigit(char digit) {
        return clickFirst(
                AppiumBy.id(PKG + "digit_" + digit),
                AppiumBy.accessibilityId(String.valueOf(digit)),
                AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceId(\"" + PKG + "digit_" + digit + "\")"),
                AppiumBy.androidUIAutomator(
                        "new UiSelector().description(\"" + digit + "\")"),
                AppiumBy.xpath("//android.widget.Button[@text='" + digit + "']"));
    }

    public String readResult() {
        By[] candidates = new By[]{
                AppiumBy.id(PKG + "result_final"),
                AppiumBy.id(PKG + "result"),
                AppiumBy.id(PKG + "formula")
        };
        for (By by : candidates) {
            try {
                WebElement el = shortWait.until(d -> {
                    var found = d.findElements(by);
                    return found.isEmpty() ? null : found.get(0);
                });
                if (el != null) {
                    String text = el.getText();
                    if (text != null && !text.isBlank()) {
                        return text.replace(",", "").replaceAll("[^0-9.\\-]", "").trim();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        throw new IllegalStateException("Unable to read calculator result");
    }

    public void clearHistoryIfPresent() {
        try {
            if (exists(AppiumBy.accessibilityId("More options"))) {
                driver.findElement(AppiumBy.accessibilityId("More options")).click();
                if (exists(By.xpath("//*[@text='History']"))) {
                    driver.findElement(By.xpath("//*[@text='History']")).click();
                }
                if (exists(AppiumBy.accessibilityId("Clear history"))) {
                    driver.findElement(AppiumBy.accessibilityId("Clear history")).click();
                } else if (exists(By.xpath("//*[@text='Clear']"))) {
                    driver.findElement(By.xpath("//*[@text='Clear']")).click();
                }
            }
        } catch (Exception ignored) {
        }
        clear();
    }

    private boolean clickFirst(By... locators) {
        for (By by : locators) {
            try {
                WebElement el = shortWait.until(d -> {
                    var found = d.findElements(by);
                    if (found.isEmpty()) {
                        return null;
                    }
                    WebElement candidate = found.get(0);
                    return candidate.isDisplayed() ? candidate : null;
                });
                if (el != null) {
                    el.click();
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean exists(By by) {
        try {
            return !driver.findElements(by).isEmpty();
        } catch (Exception ex) {
            return false;
        }
    }
}
