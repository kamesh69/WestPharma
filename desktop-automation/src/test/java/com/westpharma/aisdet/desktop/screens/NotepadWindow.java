package com.westpharma.aisdet.desktop.screens;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.nio.file.Path;

public class NotepadWindow {

    private final WebDriver driver;

    public NotepadWindow(WebDriver driver) {
        this.driver = driver;
    }

    public void typeSummary(String content) {
        WebElement editor = findEditor();
        editor.click();
        editor.clear();
        // sendKeys may truncate large text on some WinAppDriver builds; chunk it
        int chunk = 200;
        for (int i = 0; i < content.length(); i += chunk) {
            editor.sendKeys(content.substring(i, Math.min(i + chunk, content.length())));
        }
    }

    public void saveAs(Path absolutePath) {
        new Actions(driver).keyDown(Keys.CONTROL).sendKeys("s").keyUp(Keys.CONTROL).perform();
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            WebElement edit = driver.findElement(By.className("Edit"));
            edit.clear();
            edit.sendKeys(absolutePath.toString());
        } catch (Exception ex) {
            new Actions(driver).sendKeys(absolutePath.toString()).perform();
        }
        try {
            driver.findElement(By.name("Save")).click();
        } catch (Exception ex) {
            new Actions(driver).sendKeys(Keys.ENTER).perform();
        }
        try {
            driver.findElement(By.name("Yes")).click();
        } catch (Exception ignored) {
        }
    }

    public String readAllText() {
        return findEditor().getText();
    }

    private WebElement findEditor() {
        try {
            return driver.findElement(By.name("Text Editor"));
        } catch (Exception ex) {
            return driver.findElement(By.className("Edit"));
        }
    }
}
