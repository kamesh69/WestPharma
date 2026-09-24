package com.westpharma.aisdet.desktop.screens;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.List;

/**
 * macOS TextEdit via Appium Mac2.
 */
public class MacTextEditWindow {

    private final WebDriver driver;

    public MacTextEditWindow(WebDriver driver) {
        this.driver = driver;
    }

    public void typeSummary(String content) {
        WebElement editor = findEditor();
        editor.click();
        // Select all + replace for a clean document
        new Actions(driver).keyDown(Keys.COMMAND).sendKeys("a").keyUp(Keys.COMMAND).perform();
        int chunk = 200;
        for (int i = 0; i < content.length(); i += chunk) {
            editor.sendKeys(content.substring(i, Math.min(i + chunk, content.length())));
        }
    }

    public String readAllText() {
        WebElement editor = findEditor();
        String text = editor.getText();
        if (text == null || text.isBlank()) {
            text = editor.getAttribute("value");
        }
        return text == null ? "" : text;
    }

    private WebElement findEditor() {
        List<By> candidates = List.of(
                By.xpath("//XCUIElementTypeTextView"),
                AppiumBy.iOSClassChain("**/XCUIElementTypeTextView"),
                By.xpath("//XCUIElementTypeTextField"),
                By.className("XCUIElementTypeTextView")
        );
        for (By by : candidates) {
            try {
                List<WebElement> found = driver.findElements(by);
                if (!found.isEmpty()) {
                    return found.get(0);
                }
            } catch (Exception ignored) {
            }
        }
        throw new IllegalStateException("TextEdit editor area not found");
    }
}
