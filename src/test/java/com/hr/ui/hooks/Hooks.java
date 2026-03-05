package com.hr.ui.hooks;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.hr.ui.driver.DriverManager;
import com.hr.ui.reporting.ExtentReportManager;
import com.hr.ui.utils.ScreenshotUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

/**
 * Cucumber hooks — wired around every scenario.
 *
 * @Before  → start browser, create Extent test node
 * @After   → capture screenshot on failure, log result, quit browser, flush report
 */
public class Hooks {

    private static final Logger log = LogManager.getLogger(Hooks.class);

    @Before
    public void setUp(Scenario scenario) {
        log.info("========================================");
        log.info("STARTING: {}", scenario.getName());
        log.info("========================================");

        DriverManager.initDriver();
        ExtentReportManager.createTest(scenario.getName())
                .info("Scenario started: " + scenario.getName());
    }

    @After
    public void tearDown(Scenario scenario) {
        WebDriver driver = DriverManager.getDriver();

        if (scenario.isFailed()) {
            log.error("FAILED: {}", scenario.getName());

            if (driver != null) {
                // Attach screenshot to Cucumber HTML report
                byte[] screenshotBytes = ScreenshotUtils.captureAsBytes(driver);
                scenario.attach(screenshotBytes, "image/png", "Failure Screenshot");

                // Save screenshot to disk and link from Extent report
                String screenshotPath = ScreenshotUtils.captureAndSave(driver, scenario.getName());
                if (screenshotPath != null) {
                    try {
                        ExtentReportManager.getTest()
                                .fail("Scenario FAILED — screenshot captured",
                                        MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
                    } catch (Exception e) {
                        ExtentReportManager.getTest().fail("Scenario FAILED: " + e.getMessage());
                    }
                }
            }
        } else {
            log.info("PASSED: {}", scenario.getName());
            ExtentReportManager.getTest().pass("Scenario PASSED");
        }

        DriverManager.quitDriver();
        ExtentReportManager.flushReport();

        log.info("========================================");
    }
}
