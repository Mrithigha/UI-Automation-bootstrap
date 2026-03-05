package com.hr.ui.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.hr.ui.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Manages the ExtentReports lifecycle.
 * - getInstance()   → creates/returns the shared ExtentReports object
 * - createTest()    → registers a new test node for the running scenario
 * - getTest()       → returns the ExtentTest for the current thread
 * - flushReport()   → writes results to the HTML report file
 */
public class ExtentReportManager {

    private static final Logger log = LogManager.getLogger(ExtentReportManager.class);
    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> currentTest = new ThreadLocal<>();

    private ExtentReportManager() {}

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String reportPath = "reports/ExtentReport_" + timestamp + ".html";

            new File("reports").mkdirs();

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setDocumentTitle("HR System — UI Automation Report");
            spark.config().setReportName("Test Execution Results");
            spark.config().setTheme(Theme.DARK);
            spark.config().setTimeStampFormat("dd MMM yyyy HH:mm:ss");

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Environment", ConfigManager.get("base.url", "http://localhost:3000"));
            extent.setSystemInfo("Browser", ConfigManager.get("browser", "chrome"));
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java", System.getProperty("java.version"));

            log.info("Extent report initialised: {}", reportPath);
        }
        return extent;
    }

    public static ExtentTest createTest(String scenarioName) {
        ExtentTest test = getInstance().createTest(scenarioName);
        currentTest.set(test);
        return test;
    }

    public static ExtentTest getTest() {
        return currentTest.get();
    }

    public static synchronized void flushReport() {
        if (extent != null) {
            extent.flush();
        }
    }
}
