package com.hr.ui.utils;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Captures browser screenshots on test failure.
 * Screenshots are saved to screenshots/<scenarioName>_<timestamp>.png
 */
public class ScreenshotUtils {

    private static final Logger log = LogManager.getLogger(ScreenshotUtils.class);
    private static final String SCREENSHOT_DIR = "screenshots/";

    private ScreenshotUtils() {}

    /**
     * Saves a screenshot to disk and returns the absolute file path.
     * Returns null if capture fails.
     */
    public static String captureAndSave(WebDriver driver, String scenarioName) {
        try {
            File dir = new File(SCREENSHOT_DIR);
            if (!dir.exists()) dir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String safeName = scenarioName.replaceAll("[^a-zA-Z0-9]", "_");
            String filePath = SCREENSHOT_DIR + safeName + "_" + timestamp + ".png";

            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dest = new File(filePath);
            FileUtils.copyFile(src, dest);

            log.info("Screenshot saved: {}", dest.getAbsolutePath());
            return dest.getAbsolutePath();
        } catch (IOException e) {
            log.error("Failed to save screenshot for scenario: {}", scenarioName, e);
            return null;
        }
    }

    /**
     * Returns the screenshot as a raw byte array (for attaching to Cucumber report).
     */
    public static byte[] captureAsBytes(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }
}
