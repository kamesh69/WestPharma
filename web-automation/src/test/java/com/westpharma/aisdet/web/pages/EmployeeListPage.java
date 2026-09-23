package com.westpharma.aisdet.web.pages;

import com.westpharma.aisdet.config.ConfigReader;
import com.westpharma.aisdet.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class EmployeeListPage {

    private final WebDriver driver;
    private final long waitSeconds;

    private final By addButton = By.xpath("//button[normalize-space()='Add']");
    private final By employeeIdInput = By.xpath(
            "//label[normalize-space()='Employee Id']/../following-sibling::div//input");
    private final By employeeNameInput = By.xpath(
            "//label[normalize-space()='Employee Name']/ancestor::div[contains(@class,'oxd-input-group')]"
                    + "//input");
    private final By filterHeader = By.cssSelector(".oxd-table-filter-header");
    private final By filterChevron = By.cssSelector(
            ".oxd-table-filter-header .bi-chevron-down, .oxd-table-filter-header .bi-chevron-up, "
                    + ".oxd-table-filter-header-options button, .oxd-table-filter-header-options .oxd-icon-button");
    private final By searchButton = By.xpath(
            "//button[@type='submit' and contains(normalize-space(),'Search')]");
    private final By resetButton = By.xpath("//button[normalize-space()='Reset']");
    private final By tableRows = By.cssSelector(".oxd-table-body .oxd-table-card, .oxd-table-body .oxd-table-row");
    private final By loader = By.cssSelector(".oxd-form-loader, .oxd-loading-spinner");
    private final By deleteSelected = By.xpath("//button[normalize-space()='Delete Selected']");
    private final By confirmDelete = By.xpath("//button[normalize-space()='Yes, Delete']");
    private final By toast = By.cssSelector(".oxd-toast");
    private final By noRecords = By.xpath("//*[contains(normalize-space(),'No Records Found')]");

    public EmployeeListPage(WebDriver driver) {
        this.driver = driver;
        this.waitSeconds = ConfigReader.getInt("web.explicitWaitSeconds", 12);
    }

    public void openEmployeeList() {
        String base = ConfigReader.get("web.baseUrl", "https://opensource-demo.orangehrmlive.com/");
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        driver.get(base + "web/index.php/pim/viewEmployeeList");
        waitForList();
    }

    public void waitForList() {
        waitForLoaderGone();
        WaitUtils.visible(driver, addButton, waitSeconds);
        expandSearchFilters();
    }

    public void clickAdd() {
        WaitUtils.clickable(driver, addButton, waitSeconds).click();
        WaitUtils.visible(driver, By.name("firstName"), waitSeconds);
    }

    /**
     * Name search is the reliable OrangeHRM path: type unique first name, pick autocomplete, Search.
     */
    public void searchByEmployeeName(String firstName, String lastName) {
        openEmployeeList();
        expandSearchFilters();
        try {
            WaitUtils.clickable(driver, resetButton, 3).click();
            waitForLoaderGone();
            expandSearchFilters();
        } catch (Exception ignored) {
        }

        WebElement input = WaitUtils.visible(driver, employeeNameInput, waitSeconds);
        clearAndType(input, firstName);
        try {
            By option = By.xpath(
                    "//div[@role='listbox']//span[contains(.,'" + firstName + "')"
                            + (lastName == null || lastName.isBlank() ? "" : " and contains(.,'" + lastName + "')")
                            + "]");
            WaitUtils.clickable(driver, option, 5).click();
        } catch (Exception ignored) {
            // Some builds allow free-text search without picking a hint
        }
        WaitUtils.clickable(driver, searchButton, waitSeconds).click();
        waitForLoaderGone();
        WaitUtils.until(driver, waitSeconds, d -> {
            if (!d.findElements(noRecords).isEmpty()) {
                return Boolean.TRUE;
            }
            if (lastName != null && !lastName.isBlank()
                    && !d.findElements(cardContaining(firstName, lastName)).isEmpty()) {
                return Boolean.TRUE;
            }
            if (!d.findElements(cardContaining(firstName)).isEmpty()) {
                return Boolean.TRUE;
            }
            return null;
        });
    }

    public void searchByEmployeeId(String employeeId) {
        openEmployeeList();
        expandSearchFilters();
        try {
            WaitUtils.clickable(driver, resetButton, 3).click();
            waitForLoaderGone();
            expandSearchFilters();
        } catch (Exception ignored) {
        }
        WebElement input = WaitUtils.visible(driver, employeeIdInput, waitSeconds);
        clearAndType(input, employeeId);
        WaitUtils.clickable(driver, searchButton, waitSeconds).click();
        waitForLoaderGone();
        WaitUtils.until(driver, waitSeconds, d -> {
            if (!d.findElements(noRecords).isEmpty()) {
                return Boolean.TRUE;
            }
            if (!d.findElements(cardContaining(employeeId)).isEmpty()) {
                return Boolean.TRUE;
            }
            return null;
        });
    }

    private By cardContaining(String... tokens) {
        StringBuilder xp = new StringBuilder("//div[contains(@class,'oxd-table-card')");
        for (String token : tokens) {
            if (token != null && !token.isBlank()) {
                xp.append(" and contains(normalize-space(.),'").append(token).append("')");
            }
        }
        xp.append("]");
        return By.xpath(xp.toString());
    }

    private void expandSearchFilters() {
        for (int attempt = 0; attempt < 4; attempt++) {
            if (!driver.findElements(employeeNameInput).isEmpty()
                    && driver.findElements(employeeNameInput).get(0).isDisplayed()) {
                return;
            }
            try {
                List<WebElement> chevrons = driver.findElements(filterChevron);
                if (!chevrons.isEmpty()) {
                    jsClick(chevrons.get(0));
                } else {
                    List<WebElement> headers = driver.findElements(filterHeader);
                    if (!headers.isEmpty()) {
                        jsClick(headers.get(0));
                    }
                }
            } catch (Exception ignored) {
            }
            try {
                ((JavascriptExecutor) driver).executeScript(
                        "document.querySelectorAll('.oxd-table-filter-area,.oxd-table-filter form')"
                                + ".forEach(function(el){el.style.display='block';el.style.maxHeight='none';"
                                + "el.style.visibility='visible';el.style.opacity='1';});");
            } catch (Exception ignored) {
            }
            waitForLoaderGone();
        }
    }

    private void clearAndType(WebElement input, String value) {
        jsClick(input);
        Keys modifier = System.getProperty("os.name", "").toLowerCase().contains("mac")
                ? Keys.COMMAND : Keys.CONTROL;
        input.sendKeys(Keys.chord(modifier, "a"));
        input.sendKeys(Keys.DELETE);
        input.sendKeys(value);
    }

    private void jsClick(WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
    }

    public void resetSearch() {
        try {
            expandSearchFilters();
            WaitUtils.clickable(driver, resetButton, 5).click();
            waitForLoaderGone();
        } catch (Exception ex) {
            openEmployeeList();
        }
    }

    public boolean isEmployeePresent(String firstName, String lastName) {
        waitForLoaderGone();
        if (!driver.findElements(noRecords).isEmpty()) {
            return false;
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return !driver.findElements(cardContaining(firstName, lastName)).isEmpty();
            } catch (org.openqa.selenium.StaleElementReferenceException ex) {
                waitForLoaderGone();
            }
        }
        return false;
    }

    public boolean isEmployeeIdPresent(String employeeId) {
        waitForLoaderGone();
        if (!driver.findElements(noRecords).isEmpty()) {
            return false;
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return !driver.findElements(cardContaining(employeeId)).isEmpty();
            } catch (org.openqa.selenium.StaleElementReferenceException ex) {
                waitForLoaderGone();
            }
        }
        return false;
    }

    public void deleteEmployee(String firstName, String lastName) {
        searchByEmployeeName(firstName, lastName);
        if (!driver.findElements(noRecords).isEmpty()) {
            throw new IllegalStateException("Employee not found for delete (no records): "
                    + firstName + " " + lastName);
        }
        By trash = By.xpath(
                "//div[contains(@class,'oxd-table-card') and contains(normalize-space(.),'" + firstName + "')"
                        + " and contains(normalize-space(.),'" + lastName + "')]"
                        + "//i[contains(@class,'bi-trash')]");
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                List<WebElement> icons = driver.findElements(trash);
                if (!icons.isEmpty()) {
                    jsClick(icons.get(0));
                    WaitUtils.clickable(driver, confirmDelete, waitSeconds).click();
                    waitForLoaderGone();
                    try {
                        WaitUtils.visible(driver, toast, 5);
                    } catch (Exception ignored) {
                    }
                    // Wait until the card disappears from the current results
                    WaitUtils.until(driver, waitSeconds, d -> {
                        if (!d.findElements(noRecords).isEmpty()) {
                            return Boolean.TRUE;
                        }
                        return d.findElements(cardContaining(firstName, lastName)).isEmpty()
                                ? Boolean.TRUE : null;
                    });
                    return;
                }
                By checkbox = By.xpath(
                        "//div[contains(@class,'oxd-table-card') and contains(normalize-space(.),'" + firstName + "')"
                                + " and contains(normalize-space(.),'" + lastName + "')]"
                                + "//div[contains(@class,'oxd-checkbox-input')]");
                List<WebElement> boxes = driver.findElements(checkbox);
                if (!boxes.isEmpty()) {
                    jsClick(boxes.get(0));
                    WaitUtils.clickable(driver, deleteSelected, waitSeconds).click();
                    WaitUtils.clickable(driver, confirmDelete, waitSeconds).click();
                    waitForLoaderGone();
                    return;
                }
                throw new IllegalStateException("Employee not found for delete: " + firstName + " " + lastName);
            } catch (org.openqa.selenium.StaleElementReferenceException ex) {
                waitForLoaderGone();
            }
        }
        throw new IllegalStateException("Employee not found for delete (retries exhausted): "
                + firstName + " " + lastName);
    }

    public void deleteEmployeeById(String employeeId) {
        searchByEmployeeId(employeeId);
        if (!driver.findElements(noRecords).isEmpty()) {
            throw new IllegalStateException("Employee not found for delete (no records): " + employeeId);
        }
        By trash = By.xpath(
                "//div[contains(@class,'oxd-table-card') and contains(normalize-space(.),'" + employeeId + "')]"
                        + "//i[contains(@class,'bi-trash')]");
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                List<WebElement> icons = driver.findElements(trash);
                if (!icons.isEmpty()) {
                    jsClick(icons.get(0));
                    WaitUtils.clickable(driver, confirmDelete, waitSeconds).click();
                    waitForLoaderGone();
                    return;
                }
                throw new IllegalStateException("Employee not found for delete: " + employeeId);
            } catch (org.openqa.selenium.StaleElementReferenceException ex) {
                waitForLoaderGone();
            }
        }
        throw new IllegalStateException("Employee not found for delete (retries exhausted): " + employeeId);
    }

    public void waitForLoaderGone() {
        try {
            WaitUtils.invisible(driver, loader, 8);
        } catch (Exception ignored) {
        }
    }
}
