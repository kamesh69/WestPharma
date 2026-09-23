package com.westpharma.aisdet.web.pages;

import com.westpharma.aisdet.config.ConfigReader;
import com.westpharma.aisdet.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AddEmployeePage {

    private final WebDriver driver;
    private final long waitSeconds;

    private final By firstName = By.name("firstName");
    private final By middleName = By.name("middleName");
    private final By lastName = By.name("lastName");
    private final By employeeId = By.xpath("//label[normalize-space()='Employee Id']/../following-sibling::div//input");
    private final By saveButton = By.xpath("//button[@type='submit' and normalize-space()='Save']");
    private final By loader = By.cssSelector(".oxd-form-loader, .oxd-loading-spinner");
    private final By editHeader = By.cssSelector(".orangehrm-edit-employee-name, .orangehrm-edit-employee");
    private final By toast = By.cssSelector(".oxd-toast");

    public AddEmployeePage(WebDriver driver) {
        this.driver = driver;
        this.waitSeconds = ConfigReader.getInt("web.explicitWaitSeconds", 12);
    }

    /**
     * Creates an employee and returns the Employee Id OrangeHRM assigned
     * (auto-generated — do not force a custom id; demo collisions break search).
     */
    public String createEmployee(String first, String middle, String last) {
        waitForLoaderGone();
        WaitUtils.visible(driver, firstName, waitSeconds).sendKeys(first);
        if (middle != null && !middle.isBlank()) {
            driver.findElement(middleName).sendKeys(middle);
        }
        driver.findElement(lastName).sendKeys(last);

        String empId = "";
        try {
            empId = WaitUtils.visible(driver, employeeId, waitSeconds).getAttribute("value");
            if (empId == null) {
                empId = "";
            }
        } catch (Exception ignored) {
        }

        clickSave();
        waitForLoaderGone();
        waitUntilSaved();

        try {
            String persisted = WaitUtils.visible(driver, employeeId, 5).getAttribute("value");
            if (persisted != null && !persisted.isBlank()) {
                return persisted.trim();
            }
        } catch (Exception ignored) {
        }
        return empId.trim();
    }

    private void waitUntilSaved() {
        WaitUtils.until(driver, waitSeconds, d -> {
            String url = d.getCurrentUrl();
            if (url != null && url.contains("addEmployee")) {
                return null;
            }
            if (url != null && (url.contains("viewPersonalDetails") || url.contains("viewEmployee"))) {
                return true;
            }
            if (!d.findElements(editHeader).isEmpty() && d.findElements(editHeader).get(0).isDisplayed()) {
                return true;
            }
            if (!d.findElements(toast).isEmpty()) {
                return true;
            }
            return null;
        });
        try {
            WaitUtils.visible(driver, editHeader, 5);
        } catch (Exception ignored) {
        }
    }

    private void clickSave() {
        waitForLoaderGone();
        WebElement save = WaitUtils.clickable(driver, saveButton, waitSeconds);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", save);
        waitForLoaderGone();
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", save);
    }

    private void waitForLoaderGone() {
        try {
            WaitUtils.invisible(driver, loader, 8);
        } catch (Exception ignored) {
        }
    }
}
