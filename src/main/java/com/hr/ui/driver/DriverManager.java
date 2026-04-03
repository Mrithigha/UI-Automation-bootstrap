package com.hr.ui.driver;

import com.hr.ui.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/*
 * DriverManager — creates and cleans up the browser for each test.
 *
 * Uses a ThreadLocal so each thread gets its own driver, which means parallel runs
 * don't interfere with each other. The Hooks class calls initDriver() and quitDriver()
 * automatically around every scenario, so you won't usually touch this directly.
 *
 * What you can set in config.properties (or pass as -D flags on the command line):
 *   browser       — chrome (default), firefox, or edge
 *   headless      — true if you're running on CI without a display
 *   implicit.wait — how many seconds Selenium waits for elements before failing (default 10)
 *   download.dir  — where the browser drops downloaded files (default: downloads/)
 */
public class DriverManager {

    private static final Logger log = LogManager.getLogger(DriverManager.class);
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverManager() {}

    public static void initDriver() {
        String browser     = ConfigManager.get("browser", "chrome").toLowerCase();
        boolean headless   = ConfigManager.getBoolean("headless", false);
        int implicitWait   = ConfigManager.getInt("implicit.wait", 10);
        String downloadDir = resolveDownloadDir();

        log.info("Initialising {} driver (headless={}, downloadDir={})", browser, headless, downloadDir);

        WebDriver driver = switch (browser) {
            case "firefox" -> buildFirefox(headless, downloadDir);
            case "edge"    -> buildEdge(headless);
            default        -> buildChrome(headless, downloadDir);
        };

        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        driverThreadLocal.set(driver);
        log.info("Driver initialised: {}", driver.getClass().getSimpleName());
    }

    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
            log.info("Driver quit and removed from ThreadLocal");
        }
    }

    // -------------------------------------------------------------------------
    // Browser builders
    // -------------------------------------------------------------------------

    private static WebDriver buildChrome(boolean headless, String downloadDir) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        }

        // Configuring download behaviour
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("plugins.always_open_pdf_externally", true); // download PDFs instead of previewing
        options.setExperimentalOption("prefs", prefs);

        // Suppress "Chrome is being controlled by automated test software" bar
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.addArguments("--disable-infobars", "--disable-extensions");

        return new ChromeDriver(options);
    }

    private static WebDriver buildFirefox(boolean headless, String downloadDir) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();

        if (headless) options.addArguments("--headless");

        // Configure Firefox download behaviour via profile preferences
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("browser.download.folderList", 2);
        profile.setPreference("browser.download.dir", downloadDir);
        profile.setPreference("browser.download.useDownloadDir", true);
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                "application/pdf,application/octet-stream,text/csv,application/zip," +
                "application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        profile.setPreference("pdfjs.disabled", true);
        options.setProfile(profile);

        return new FirefoxDriver(options);
    }

    private static WebDriver buildEdge(boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();
        if (headless) options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        return new EdgeDriver(options);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String resolveDownloadDir() {
        String raw = ConfigManager.get("download.dir", "downloads");
        File dir = new File(raw);
        // Convert relative paths to absolute so the browser can find them
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), raw);
        }
        if (!dir.exists()) dir.mkdirs();
        return dir.getAbsolutePath();
    }
}
