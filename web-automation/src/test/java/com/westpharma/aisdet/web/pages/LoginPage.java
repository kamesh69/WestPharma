package com.westpharma.aisdet.web.pages;

import com.westpharma.aisdet.config.ConfigReader;
import com.westpharma.aisdet.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage {

    private final WebDriver driver;
    private final long waitSeconds;

    private final By username = By.name("username");
    private final By password = By.name("password");
    private final By loginButton = By.cssSelector("button[type='submit']");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.waitSeconds = ConfigReader.getInt("web.explicitWaitSeconds", 20);
    }

    public LoginPage open() {
        driver.get(ConfigReader.get("web.baseUrl"));
        WaitUtils.visible(driver, username, waitSeconds);
        return this;
    }

    public void login(String user, String pass) {
        WaitUtils.visible(driver, username, waitSeconds).clear();
        driver.findElement(username).sendKeys(user);
        driver.findElement(password).clear();
        driver.findElement(password).sendKeys(pass);
        WaitUtils.clickable(driver, loginButton, waitSeconds).click();
    }
}
