package com.westpharma.aisdet.desktop.screens;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * macOS Calculator.app via Appium Mac2 (XCTest accessibility).
 */
public class MacCalculatorWindow {

    private final WebDriver driver;

    public MacCalculatorWindow(WebDriver driver) {
        this.driver = driver;
    }

    public void clear() {
        clickFirst(
                AppiumBy.accessibilityId("AllClear"),
                By.name("All Clear"),
                By.name("AC"),
                By.name("C"),
                AppiumBy.accessibilityId("AC"),
                AppiumBy.accessibilityId("C"));
    }

    public String calculate(int left, String operatorKey, int right) {
        clear();
        enterNumber(left);
        clickOperator(operatorKey);
        enterNumber(right);
        clickFirst(AppiumBy.accessibilityId("Equals"), By.name("Equals"), By.name("="));
        return readResult();
    }

    public Map<String, String> runThreeCalculations() {
        Map<String, String> results = new LinkedHashMap<>();
        results.put("12 + 8", calculate(12, "plus", 8));
        results.put("9 × 7", calculate(9, "multiply", 7));
        results.put("100 ÷ 4", calculate(100, "divide", 4));
        return results;
    }

    private void enterNumber(int number) {
        String digits = String.valueOf(number);
        for (char c : digits.toCharArray()) {
            clickDigit(c);
        }
    }

    private void clickDigit(char digit) {
        String d = String.valueOf(digit);
        String wordId = switch (digit) {
            case '0' -> "Zero";
            case '1' -> "One";
            case '2' -> "Two";
            case '3' -> "Three";
            case '4' -> "Four";
            case '5' -> "Five";
            case '6' -> "Six";
            case '7' -> "Seven";
            case '8' -> "Eight";
            case '9' -> "Nine";
            default -> d;
        };
        clickFirst(
                AppiumBy.accessibilityId(wordId),
                By.name(d),
                AppiumBy.accessibilityId(d),
                By.xpath("//XCUIElementTypeButton[@label='" + d + "' or @title='" + d + "']"));
    }

    private void clickOperator(String key) {
        switch (key) {
            case "plus" -> clickFirst(AppiumBy.accessibilityId("Add"), By.name("Add"), By.name("+"),
                    AppiumBy.accessibilityId("+"),
                    By.xpath("//XCUIElementTypeButton[@label='+' or @title='+']"));
            case "multiply" -> clickFirst(AppiumBy.accessibilityId("Multiply"), By.name("Multiply"),
                    By.name("×"), By.name("*"), AppiumBy.accessibilityId("×"),
                    By.xpath("//XCUIElementTypeButton[@label='×' or @title='×' or @label='*']"));
            case "divide" -> clickFirst(AppiumBy.accessibilityId("Divide"), By.name("Divide"),
                    By.name("÷"), By.name("/"), AppiumBy.accessibilityId("÷"),
                    By.xpath("//XCUIElementTypeButton[@label='÷' or @title='÷' or @label='/']"));
            case "minus" -> clickFirst(AppiumBy.accessibilityId("Subtract"), By.name("Subtract"),
                    By.name("−"), By.name("-"), AppiumBy.accessibilityId("−"));
            default -> clickFirst(By.name(key));
        }
    }

    public String readResult() {
        // Prefer main input/result static text; fall back to parsing window text
        List<By> candidates = List.of(
                By.xpath("//XCUIElementTypeScrollView[@identifier='StandardInputView']//XCUIElementTypeStaticText"),
                By.xpath("//XCUIElementTypeStaticText[@value!='']"),
                By.xpath("//XCUIElementTypeStaticText[contains(@label,'.') or string-length(@label)>0]"),
                AppiumBy.iOSClassChain("**/XCUIElementTypeStaticText[`label.length > 0`]"),
                By.xpath("//XCUIElementTypeTextField"),
                By.xpath("//XCUIElementTypeStaticText")
        );
        for (By by : candidates) {
            try {
                List<WebElement> els = driver.findElements(by);
                for (int i = els.size() - 1; i >= 0; i--) {
                    String text = els.get(i).getText();
                    if (text == null || text.isBlank()) {
                        text = els.get(i).getAttribute("value");
                    }
                    if (text == null || text.isBlank()) {
                        text = els.get(i).getAttribute("label");
                    }
                    if (text == null || text.isBlank()) {
                        continue;
                    }
                    String cleaned = normalizeNumericDisplay(text);
                    if (cleaned.matches("-?\\d+(\\.\\d+)?")) {
                        if (cleaned.endsWith(".0")) {
                            cleaned = cleaned.substring(0, cleaned.length() - 2);
                        }
                        return cleaned;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        throw new IllegalStateException("Unable to read macOS Calculator result");
    }

    /** Calculator prefixes display values with invisible bidi/format chars (e.g. U+200E). */
    private static String normalizeNumericDisplay(String text) {
        return text.replace(",", "")
                .replace(" ", "")
                .replaceAll("\\p{Cf}", "")
                .trim();
    }

    private void clickFirst(By... locators) {
        for (By by : locators) {
            try {
                List<WebElement> found = driver.findElements(by);
                if (!found.isEmpty()) {
                    found.get(0).click();
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        throw new IllegalStateException("No clickable element for locators: " + java.util.Arrays.toString(locators));
    }
}
