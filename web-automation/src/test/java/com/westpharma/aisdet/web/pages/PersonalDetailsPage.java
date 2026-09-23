package com.westpharma.aisdet.web.pages;

import com.westpharma.aisdet.config.ConfigReader;
import com.westpharma.aisdet.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class PersonalDetailsPage {

    private final WebDriver driver;
    private final long waitSeconds;

    private final By middleName = By.name("middleName");
    private final By firstName = By.name("firstName");
    private final By lastName = By.name("lastName");
    private final By saveButton = By.xpath("//h6[text()='Personal Details']/../form//button[@type='submit']");
    private final By loader = By.cssSelector(".oxd-form-loader");
    private final By employeeHeader = By.cssSelector(".orangehrm-edit-employee-name");

    public PersonalDetailsPage(WebDriver driver) {
        this.driver = driver;
        this.waitSeconds = ConfigReader.getInt("web.explicitWaitSeconds", 20);
    }

    public void waitUntilLoaded() {
        WaitUtils.visible(driver, firstName, waitSeconds);
    }

    public void updateMiddleName(String newMiddle) {
        waitUntilLoaded();
        try {
            WaitUtils.invisible(driver, loader, waitSeconds);
        } catch (Exception ignored) {
        }
        WebElement field = WaitUtils.visible(driver, middleName, waitSeconds);
        field.click();
        Keys modifier = System.getProperty("os.name", "").toLowerCase().contains("mac")
                ? Keys.COMMAND : Keys.CONTROL;
        field.sendKeys(Keys.chord(modifier, "a"));
        field.sendKeys(Keys.DELETE);
        field.sendKeys(newMiddle);
        WebElement save = WaitUtils.clickable(driver, saveButton, waitSeconds);
        try {
            save.click();
        } catch (org.openqa.selenium.ElementClickInterceptedException ex) {
            try {
                WaitUtils.invisible(driver, loader, waitSeconds);
            } catch (Exception ignored) {
            }
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", save);
        }
        try {
            WaitUtils.invisible(driver, loader, waitSeconds);
        } catch (Exception ignored) {
        }
    }

    public String getMiddleName() {
        return WaitUtils.visible(driver, middleName, waitSeconds).getAttribute("value");
    }

    public String getEmployeeId() {
        By idField = By.xpath("//label[normalize-space()='Employee Id']/../following-sibling::div//input");
        return WaitUtils.visible(driver, idField, waitSeconds).getAttribute("value");
    }

    public String getHeaderName() {
        return WaitUtils.visible(driver, employeeHeader, waitSeconds).getText();
    }
}
