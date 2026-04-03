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

/*
 * ExtentReportManager — handles the HTML report that gets generated after a test run.
 *
 * The report is created once (getInstance()), each scenario gets its own node (createTest()),
 * and the whole thing is written to disk at the end (flushReport()). The Hooks class takes
 * care of calling these at the right times, so in step definitions you mainly just call
 * getTest().info(...) or getTest().pass(...) to add log entries to the current scenario.
 *
 * The report file lands in reports/ dir with a timestamp in the name so runs don't overwrite each other.
 */
public class ExtentReportManager {

    private static final Logger log = LogManager.getLogger(ExtentReportManager.class);
    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> currentTest = new ThreadLocal<>();

    private ExtentReportManager() {}

    // creates the report on first call; subsequent calls return the same instance
    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String reportPath = "reports/ExtentReport_" + timestamp + ".html";

            new File("reports").mkdirs();

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setDocumentTitle("AppName — UI Automation Report");
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

    // registers a new test node for the scenario and binds it to the current thread
    public static ExtentTest createTest(String scenarioName) {
        ExtentTest test = getInstance().createTest(scenarioName);
        currentTest.set(test);
        return test;
    }

    // returns the ExtentTest for the current thread — use this in steps to log info/pass/fail
    public static ExtentTest getTest() {
        return currentTest.get();
    }

    // writes everything to the HTML file — called in @After so the report is always up to date
    public static synchronized void flushReport() {
        if (extent != null) {
            extent.flush();
        }
    }
}
