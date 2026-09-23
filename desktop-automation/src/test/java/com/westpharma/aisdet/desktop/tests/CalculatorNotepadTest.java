package com.westpharma.aisdet.desktop.tests;

import com.westpharma.aisdet.BaseTest;
import com.westpharma.aisdet.config.ConfigReader;
import com.westpharma.aisdet.data.DataFactory;
import com.westpharma.aisdet.desktop.WinAppDriverFactory;
import com.westpharma.aisdet.desktop.screens.CalculatorWindow;
import com.westpharma.aisdet.desktop.screens.NotepadWindow;
import com.westpharma.aisdet.reporting.ExtentTestListener;
import com.westpharma.aisdet.reporting.ExtentTestManager;
import com.westpharma.aisdet.utils.AssertUtils;
import org.openqa.selenium.WebDriver;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Listeners(ExtentTestListener.class)
public class CalculatorNotepadTest extends BaseTest {

    @Override
    protected String moduleName() {
        return "desktop";
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        WinAppDriverFactory.quit();
    }

    @Test(description = "Calculator 3 ops -> Notepad summary -> save/reopen validate")
    public void calculatorAndNotepadSummary() throws Exception {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            throw new SkipException("Desktop Q3 requires Windows + WinAppDriver. Current OS="
                    + System.getProperty("os.name"));
        }

        Path outDir = ConfigReader.resolveFromRoot(
                ConfigReader.get("desktop.outputDir", "desktop-automation/output"));
        Files.createDirectories(outDir);

        ExtentTestManager.logInfo("Launching Windows Calculator");
        WebDriver calcDriver = WinAppDriverFactory.launch("Microsoft.WindowsCalculator_8wekyb3d8bbwe!App");
        CalculatorWindow calculator = new CalculatorWindow(calcDriver);
        Map<String, String> results = calculator.runThreeCalculations();
        for (Map.Entry<String, String> entry : results.entrySet()) {
            ExtentTestManager.logPass(entry.getKey() + " = " + entry.getValue());
        }
        AssertUtils.assertEquals(results.get("12 + 8"), "20", "12+8");
        AssertUtils.assertEquals(results.get("9 × 7"), "63", "9*7");
        AssertUtils.assertEquals(results.get("100 ÷ 4"), "25", "100/4");
        WinAppDriverFactory.quit();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        StringBuilder summary = new StringBuilder();
        for (Map.Entry<String, String> entry : results.entrySet()) {
            summary.append("Calculation: ").append(entry.getKey()).append('\n')
                    .append("Result: ").append(entry.getValue()).append('\n')
                    .append("Execution timestamp: ").append(timestamp).append("\n\n");
        }

        String fileName = DataFactory.timestampFileName("Calc_Summary", "txt");
        Path file = outDir.resolve(fileName).toAbsolutePath();
        Files.writeString(file, summary.toString(), StandardCharsets.UTF_8);
        ExtentTestManager.logInfo("Saved summary file: " + file);

        ExtentTestManager.logInfo("Launching Notepad with formatted summary");
        WebDriver notepad = WinAppDriverFactory.launch("C:\\Windows\\System32\\notepad.exe");
        NotepadWindow notepadWindow = new NotepadWindow(notepad);
        String reloaded = Files.readString(file, StandardCharsets.UTF_8);
        notepadWindow.typeSummary(reloaded);

        for (Map.Entry<String, String> entry : results.entrySet()) {
            AssertUtils.assertTrue(reloaded.contains(entry.getKey()), "Missing calculation " + entry.getKey());
            AssertUtils.assertTrue(reloaded.contains("Result: " + entry.getValue()),
                    "Missing result " + entry.getValue());
        }
        AssertUtils.assertTrue(reloaded.contains("Execution timestamp:"), "Missing timestamp label");
        ExtentTestManager.logPass("Reopened file contents validated: " + file);
        ExtentTestManager.logPass("Notepad displays the formatted summary");
    }
}
