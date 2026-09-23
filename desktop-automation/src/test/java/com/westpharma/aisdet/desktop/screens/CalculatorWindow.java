package com.westpharma.aisdet.desktop.screens;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

public class CalculatorWindow {

    private final WebDriver driver;

    public CalculatorWindow(WebDriver driver) {
        this.driver = driver;
    }

    public void clear() {
        clickIfPresent("clearEntry");
        clickIfPresent("clear");
    }

    public String calculate(int left, String operatorKey, int right) {
        clear();
        enterNumber(left);
        driver.findElement(By.name(operatorName(operatorKey))).click();
        enterNumber(right);
        driver.findElement(By.name("Equals")).click();
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
            driver.findElement(By.name(String.valueOf(c))).click();
        }
    }

    private String operatorName(String key) {
        return switch (key) {
            case "plus" -> "Plus";
            case "multiply" -> "Multiply by";
            case "divide" -> "Divide by";
            case "minus" -> "Minus";
            default -> key;
        };
    }

    public String readResult() {
        WebElement result;
        try {
            result = driver.findElement(By.id("CalculatorResults"));
        } catch (Exception ex) {
            result = driver.findElement(By.name("Display is"));
        }
        String text = result.getText();
        if (text != null && text.toLowerCase().contains("display is")) {
            return text.replaceAll("(?i)display is\\s*", "").trim();
        }
        return text == null ? "" : text.trim();
    }

    private void clickIfPresent(String automationId) {
        try {
            driver.findElement(By.id(automationId)).click();
        } catch (Exception ignored) {
            try {
                driver.findElement(By.name("Clear")).click();
            } catch (Exception ignored2) {
            }
        }
    }
}
