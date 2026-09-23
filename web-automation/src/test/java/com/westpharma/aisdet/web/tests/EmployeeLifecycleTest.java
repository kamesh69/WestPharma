package com.westpharma.aisdet.web.tests;

import com.westpharma.aisdet.BaseTest;
import com.westpharma.aisdet.ai.AiDataLoader;
import com.westpharma.aisdet.config.ConfigReader;
import com.westpharma.aisdet.data.DataFactory;
import com.westpharma.aisdet.reporting.ExtentTestListener;
import com.westpharma.aisdet.reporting.ExtentTestManager;
import com.westpharma.aisdet.utils.AssertUtils;
import com.westpharma.aisdet.utils.WebDriverFactory;
import com.westpharma.aisdet.web.pages.AddEmployeePage;
import com.westpharma.aisdet.web.pages.DashboardPage;
import com.westpharma.aisdet.web.pages.EmployeeListPage;
import com.westpharma.aisdet.web.pages.LoginPage;
import com.westpharma.aisdet.web.pages.PersonalDetailsPage;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Listeners(ExtentTestListener.class)
public class EmployeeLifecycleTest extends BaseTest {

    private WebDriver driver;
    private String empAFirst;
    private String empAMiddle;
    private String empALast;
    private String empBFirst;
    private String empBMiddle;
    private String empBLast;
    private String empAId;
    private String empBId;
    private String updatedMiddle;

    @Override
    protected String moduleName() {
        return "web";
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        driver = WebDriverFactory.createDriver();
        resolveEmployeeData();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE && driver != null) {
            try {
                File shot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                Path dest = ConfigReader.getProjectRoot().resolve("reports/web/screenshots")
                        .resolve(result.getMethod().getMethodName() + ".png");
                Files.createDirectories(dest.getParent());
                Files.copy(shot.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                ExtentTestManager.logFail("Screenshot: " + dest);
            } catch (Exception ignored) {
            }
        }
        WebDriverFactory.quitDriver();
    }

    @Test(description = "Login, create 2 employees, search by name, update one, delete one, verify other remains")
    public void employeeLifecycleScenario() {
        LoginPage loginPage = new LoginPage(driver);
        DashboardPage dashboard = new DashboardPage(driver);
        EmployeeListPage listPage = new EmployeeListPage(driver);
        AddEmployeePage addPage = new AddEmployeePage(driver);
        PersonalDetailsPage detailsPage = new PersonalDetailsPage(driver);

        ExtentTestManager.logInfo("Logging into OrangeHRM");
        loginPage.open().login(
                ConfigReader.get("web.username", "Admin"),
                ConfigReader.get("web.password", "admin123"));
        dashboard.waitUntilLoaded();
        ExtentTestManager.logPass("Login successful");

        dashboard.goToPim();
        listPage.openEmployeeList();

        // Create A → update middle name immediately on personal-details (no list round-trip)
        ExtentTestManager.logInfo("Creating employee A: " + empAFirst + " " + empALast);
        listPage.clickAdd();
        empAId = addPage.createEmployee(empAFirst, empAMiddle, empALast);
        ExtentTestManager.logPass("Employee A created with id " + empAId);

        updatedMiddle = "Up" + DataFactory.uniqueToken().substring(0, 3);
        detailsPage.updateMiddleName(updatedMiddle);
        AssertUtils.assertEquals(detailsPage.getMiddleName(), updatedMiddle, "Middle name should be updated");
        ExtentTestManager.logPass("Employee A middle name updated to " + updatedMiddle);

        // Create B
        listPage.openEmployeeList();
        ExtentTestManager.logInfo("Creating employee B: " + empBFirst + " " + empBLast);
        listPage.clickAdd();
        empBId = addPage.createEmployee(empBFirst, empBMiddle, empBLast);
        ExtentTestManager.logPass("Employee B created with id " + empBId);

        // Search by unique short names (Employee Id search is unreliable on the public demo)
        listPage.searchByEmployeeName(empAFirst, empALast);
        AssertUtils.assertTrue(listPage.isEmployeePresent(empAFirst, empALast),
                "Employee A should be searchable: " + empAFirst + " " + empALast);
        listPage.searchByEmployeeName(empBFirst, empBLast);
        AssertUtils.assertTrue(listPage.isEmployeePresent(empBFirst, empBLast),
                "Employee B should be searchable: " + empBFirst + " " + empBLast);
        ExtentTestManager.logPass("Both employees validated via Employee Name search");

        listPage.deleteEmployee(empAFirst, empALast);
        listPage.searchByEmployeeName(empAFirst, empALast);
        AssertUtils.assertTrue(
                !listPage.isEmployeePresent(empAFirst, empALast),
                "Employee A should be deleted");
        ExtentTestManager.logPass("Employee A deleted and validated");

        listPage.searchByEmployeeName(empBFirst, empBLast);
        AssertUtils.assertTrue(listPage.isEmployeePresent(empBFirst, empBLast),
                "Employee B must remain unaffected after deleting A");
        ExtentTestManager.logPass("Employee B remains unaffected");
    }

    /** Distinct short names per employee — never derive shared prefixes from AI (AutoAI* → "Au" for both). */
    private void resolveEmployeeData() {
        String token = DataFactory.uniqueToken().substring(0, 4);
        boolean usedAi = false;
        try {
            List<AiDataLoader.EmployeeData> employees = AiDataLoader.loadEmployees();
            usedAi = employees.size() >= 2;
        } catch (Exception ex) {
            log.info("AI employee data unavailable: {}", ex.getMessage());
        }
        // Guaranteed-unique, short, distinct names (OrangeHRM-safe)
        empAFirst = "Aa" + token;
        empAMiddle = "Ma" + token.substring(0, 2);
        empALast = "La" + token;
        empBFirst = "Bb" + token;
        empBMiddle = "Mb" + token.substring(0, 2);
        empBLast = "Lb" + token;
        log.info("Employee names ready (aiValidated={}, suffix={}): {} {} / {} {}",
                usedAi, token, empAFirst, empALast, empBFirst, empBLast);
    }
}
