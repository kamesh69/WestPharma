package com.westpharma.aisdet.web.pages;

import com.westpharma.aisdet.config.ConfigReader;
import com.westpharma.aisdet.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class DashboardPage {

    private final WebDriver driver;
    private final long waitSeconds;

    private final By pimMenu = By.xpath("//span[text()='PIM']");
    private final By employeeListTab = By.xpath("//a[normalize-space()='Employee List']");
    private final By userDropdown = By.cssSelector(".oxd-userdropdown-tab");

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        this.waitSeconds = ConfigReader.getInt("web.explicitWaitSeconds", 20);
    }

    public void waitUntilLoaded() {
        WaitUtils.visible(driver, userDropdown, waitSeconds);
    }

    public void goToPim() {
        WaitUtils.clickable(driver, pimMenu, waitSeconds).click();
        try {
            WaitUtils.clickable(driver, employeeListTab, 8).click();
        } catch (Exception ignored) {
            // already on Employee List in some OrangeHRM builds
        }
    }
}
