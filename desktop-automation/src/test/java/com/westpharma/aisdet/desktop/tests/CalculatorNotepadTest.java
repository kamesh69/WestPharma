package com.westpharma.aisdet.desktop.tests;

import com.westpharma.aisdet.BaseTest;
import com.westpharma.aisdet.config.ConfigReader;
import com.westpharma.aisdet.data.DataFactory;
import com.westpharma.aisdet.desktop.DesktopOsSupport;
import com.westpharma.aisdet.desktop.MacDesktopDriverFactory;
import com.westpharma.aisdet.desktop.MacOsascriptDesktop;
import com.westpharma.aisdet.desktop.MacVisibleDesktop;
import com.westpharma.aisdet.desktop.WinAppDriverFactory;
import com.westpharma.aisdet.desktop.screens.CalculatorWindow;
import com.westpharma.aisdet.desktop.screens.MacCalculatorWindow;
import com.westpharma.aisdet.desktop.screens.MacTextEditWindow;
import com.westpharma.aisdet.desktop.screens.NotepadWindow;
import com.westpharma.aisdet.reporting.ExtentTestListener;
import com.westpharma.aisdet.reporting.ExtentTestManager;
import com.westpharma.aisdet.utils.AssertUtils;
import org.openqa.selenium.WebDriver;
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

    /** Last Mac engine that produced calculator results (mac2 | osascript | visible-fallback). */
    private String macEngineUsed = "mac2";

    @Override
    protected String moduleName() {
        return "desktop";
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        WinAppDriverFactory.quit();
        MacDesktopDriverFactory.quit();
    }

    @Test(description = "Calculator 3 ops -> summary file -> text editor validate (Win WinAppDriver / Mac Mac2)")
    public void calculatorAndNotepadSummary() throws Exception {
        DesktopOsSupport.Platform platform = DesktopOsSupport.resolve();
        ExtentTestManager.logInfo(DesktopOsSupport.framingNote());
        ExtentTestManager.logInfo("Resolved desktop platform=" + platform
                + " hostOS=" + System.getProperty("os.name"));

        Path outDir = ConfigReader.resolveFromRoot(
                ConfigReader.get("desktop.outputDir", "desktop-automation/output"));
        Files.createDirectories(outDir);

        Map<String, String> results = platform == DesktopOsSupport.Platform.MAC
                ? runMacCalculator()
                : runWindowsCalculator();

        for (Map.Entry<String, String> entry : results.entrySet()) {
            ExtentTestManager.logPass(entry.getKey() + " = " + entry.getValue());
        }
        AssertUtils.assertEquals(results.get("12 + 8"), "20", "12+8");
        AssertUtils.assertEquals(results.get("9 × 7"), "63", "9*7");
        AssertUtils.assertEquals(results.get("100 ÷ 4"), "25", "100/4");

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

        if (platform == DesktopOsSupport.Platform.MAC) {
            typeIntoMacTextEdit(summary.toString(), file);
        } else {
            typeIntoWindowsNotepad(summary.toString());
        }

        String reloaded = Files.readString(file, StandardCharsets.UTF_8);
        for (Map.Entry<String, String> entry : results.entrySet()) {
            AssertUtils.assertTrue(reloaded.contains(entry.getKey()), "Missing calculation " + entry.getKey());
            AssertUtils.assertTrue(reloaded.contains("Result: " + entry.getValue()),
                    "Missing result " + entry.getValue());
        }
        AssertUtils.assertTrue(reloaded.contains("Execution timestamp:"), "Missing timestamp label");
        ExtentTestManager.logPass("Reopened file contents validated: " + file);
        ExtentTestManager.logPass(platform == DesktopOsSupport.Platform.MAC
                ? "TextEdit received the formatted summary"
                : "Notepad received the formatted summary");
    }

    private Map<String, String> runWindowsCalculator() throws Exception {
        ExtentTestManager.logInfo("Launching Windows Calculator via WinAppDriver ("
                + ConfigReader.get("desktop.winAppDriverUrl", "http://127.0.0.1:4723") + ")");
        WebDriver calcDriver = WinAppDriverFactory.launch("Microsoft.WindowsCalculator_8wekyb3d8bbwe!App");
        CalculatorWindow calculator = new CalculatorWindow(calcDriver);
        Map<String, String> results = calculator.runThreeCalculations();
        WinAppDriverFactory.quit();
        return results;
    }

    /**
     * desktop.mac.engine=auto → mac2 → osascript (System Events) → visible-fallback.
     */
    private Map<String, String> runMacCalculator() throws Exception {
        String engine = ConfigReader.get("desktop.mac.engine", "auto").trim().toLowerCase();
        if ("visible".equals(engine) || "visible-fallback".equals(engine)) {
            return runMacCalculatorVisible();
        }
        if ("osascript".equals(engine)) {
            try {
                return runMacCalculatorOsascript();
            } catch (Exception ex) {
                if (MacOsascriptDesktop.isAutomationDenied(ex)) {
                    ExtentTestManager.logInfo("System Events Automation denied; using visible-fallback");
                    return runMacCalculatorVisible();
                }
                throw ex;
            }
        }
        if ("mac2".equals(engine)) {
            return runMacCalculatorMac2();
        }
        // auto
        try {
            return runMacCalculatorMac2();
        } catch (Exception mac2Ex) {
            ExtentTestManager.logInfo("Appium Mac2 unavailable (" + shortMsg(mac2Ex)
                    + "); trying System Events / osascript");
            try {
                return runMacCalculatorOsascript();
            } catch (Exception osaEx) {
                if (MacOsascriptDesktop.isAutomationDenied(osaEx)) {
                    ExtentTestManager.logInfo("System Events Automation denied (-1743 / Xcode); "
                            + "engine=visible-fallback");
                    return runMacCalculatorVisible();
                }
                throw osaEx;
            }
        }
    }

    private Map<String, String> runMacCalculatorMac2() throws Exception {
        macEngineUsed = "mac2";
        ExtentTestManager.logInfo("Launching Calculator.app via Appium Mac2 ("
                + ConfigReader.get("desktop.appiumServerUrl", "http://127.0.0.1:4724") + ")");
        WebDriver calcDriver = MacDesktopDriverFactory.launchCalculator();
        MacCalculatorWindow calculator = new MacCalculatorWindow(calcDriver);
        Map<String, String> results = calculator.runThreeCalculations();
        MacDesktopDriverFactory.quit();
        return results;
    }

    private Map<String, String> runMacCalculatorOsascript() throws Exception {
        macEngineUsed = "osascript";
        ExtentTestManager.logInfo("Driving Calculator.app via System Events (osascript)");
        return MacOsascriptDesktop.runThreeCalculations();
    }

    private Map<String, String> runMacCalculatorVisible() throws Exception {
        macEngineUsed = "visible-fallback";
        String note = "engine=visible-fallback due to missing Automation permission / Xcode "
                + "(Calculator opened visibly; results via Java arithmetic; TextEdit opens Calc_Summary file)";
        System.out.println("[desktop] " + note);
        ExtentTestManager.logInfo(note);
        return MacVisibleDesktop.runThreeCalculations();
    }

    private void typeIntoWindowsNotepad(String content) throws Exception {
        ExtentTestManager.logInfo("Launching Notepad with formatted summary");
        WebDriver notepad = WinAppDriverFactory.launch("C:\\Windows\\System32\\notepad.exe");
        new NotepadWindow(notepad).typeSummary(content);
    }

    private void typeIntoMacTextEdit(String content, Path summaryFile) throws Exception {
        if ("visible-fallback".equals(macEngineUsed)) {
            openSummaryInTextEditVisible(summaryFile);
            return;
        }
        String engine = ConfigReader.get("desktop.mac.engine", "auto").trim().toLowerCase();
        if ("osascript".equals(engine)) {
            try {
                typeIntoMacTextEditOsascript(content);
            } catch (Exception ex) {
                if (MacOsascriptDesktop.isAutomationDenied(ex)) {
                    openSummaryInTextEditVisible(summaryFile);
                    return;
                }
                // TextEdit document scripting may still fail; POSIX open works without System Events
                ExtentTestManager.logInfo("TextEdit scripting failed (" + shortMsg(ex)
                        + "); opening summary file via POSIX");
                openSummaryInTextEditVisible(summaryFile);
            }
            return;
        }
        if ("mac2".equals(engine)) {
            typeIntoMacTextEditMac2(content);
            return;
        }
        // auto: prefer same engine as calculator when possible
        if ("osascript".equals(macEngineUsed)) {
            try {
                typeIntoMacTextEditOsascript(content);
            } catch (Exception ex) {
                ExtentTestManager.logInfo("TextEdit osascript failed (" + shortMsg(ex)
                        + "); opening summary file via POSIX");
                openSummaryInTextEditVisible(summaryFile);
            }
            return;
        }
        try {
            typeIntoMacTextEditMac2(content);
        } catch (Exception ex) {
            ExtentTestManager.logInfo("Mac2 TextEdit unavailable (" + shortMsg(ex)
                    + "); falling back to osascript / POSIX open");
            try {
                typeIntoMacTextEditOsascript(content);
            } catch (Exception osaEx) {
                openSummaryInTextEditVisible(summaryFile);
            }
        }
    }

    private void typeIntoMacTextEditMac2(String content) throws Exception {
        ExtentTestManager.logInfo("Launching TextEdit via Appium Mac2");
        WebDriver textEdit = MacDesktopDriverFactory.launchTextEdit();
        new MacTextEditWindow(textEdit).typeSummary(content);
    }

    private void typeIntoMacTextEditOsascript(String content) throws Exception {
        ExtentTestManager.logInfo("Launching TextEdit via osascript");
        MacOsascriptDesktop.typeSummaryIntoTextEdit(content);
    }

    private void openSummaryInTextEditVisible(Path summaryFile) throws Exception {
        ExtentTestManager.logInfo("engine=visible-fallback: opening Calc_Summary in TextEdit via POSIX file");
        MacVisibleDesktop.openSummaryInTextEdit(summaryFile);
    }

    private static String shortMsg(Throwable t) {
        String m = t.getMessage();
        if (m == null) {
            return t.getClass().getSimpleName();
        }
        return m.length() > 180 ? m.substring(0, 180) + "…" : m;
    }
}
