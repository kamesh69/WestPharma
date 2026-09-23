package com.westpharma.aisdet.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.westpharma.aisdet.config.ConfigReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ExtentManager {

    private static ExtentReports extent;
    private static Path reportPath;

    private ExtentManager() {
    }

    public static synchronized ExtentReports getInstance(String moduleName) {
        if (extent == null) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path dir = ConfigReader.getProjectRoot().resolve("reports").resolve(moduleName);
            try {
                Files.createDirectories(dir);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to create report directory: " + dir, e);
            }
            reportPath = dir.resolve("ExtentReport_" + ts + ".html");
            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath.toString());
            spark.config().setDocumentTitle("WestPharma AI-SDET - " + moduleName);
            spark.config().setReportName(moduleName + " Execution Report");
            spark.config().setTheme(Theme.STANDARD);

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Module", moduleName);
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java", System.getProperty("java.version"));
            extent.setSystemInfo("Project Root", ConfigReader.getProjectRoot().toString());
        }
        return extent;
    }

    public static Path getReportPath() {
        return reportPath;
    }

    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
        }
    }

    public static synchronized void reset() {
        extent = null;
        reportPath = null;
    }
}
