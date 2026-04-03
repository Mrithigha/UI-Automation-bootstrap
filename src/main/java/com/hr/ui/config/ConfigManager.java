package com.hr.ui.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/*
 * ConfigManager — single place to read all test config.
 *
 * How it works:
 *   config.properties is always loaded first (the defaults — base URL, browser, timeouts etc.).
 *   If you pass -Denv=qa on the Maven command line, it also loads config-qa.properties and
 *   lets those values override the values loaded from config.prop.
 * Handy when you work with multiple environments having different base urls

 *
 *   However, any -D flag you pass directly in the command line always takes precedence. So -Dbrowser=firefox will
 *   override whatever is in the properties files and is useful for quick one-off runs.
 *
 * Typical usage:
 *   mvn test                              → just config.properties
 *   mvn test -Denv=qa                     → config.properties + config-qa.properties on top
 *   mvn test -Denv=qa -Dbrowser=firefox   → same as above but browser forced to firefox
 *
 * To read a value anywhere in the framework: ConfigManager.get("base.url")
 */
public class ConfigManager {

    private static final Logger log = LogManager.getLogger(ConfigManager.class);
    private static final Properties properties = new Properties();

    static {
        // always load the base config first
        try (InputStream is = ConfigManager.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is == null) throw new RuntimeException("config.properties not found on classpath");
            properties.load(is);
            log.info("config.properties loaded successfully");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }

        // if -Denv or TEST_ENV is set, load the env-specific file and merge it over the base
        String env = System.getProperty("env",
                System.getenv().getOrDefault("TEST_ENV", "")).toLowerCase().trim();
        if (!env.isEmpty()) {
            String envFile = "config-" + env + ".properties";
            try (InputStream envIs = ConfigManager.class.getClassLoader()
                    .getResourceAsStream(envFile)) {
                if (envIs != null) {
                    Properties envProps = new Properties();
                    envProps.load(envIs);
                    // merge — env values overwrite matching base keys
                    for (String key : envProps.stringPropertyNames()) {
                        properties.setProperty(key, envProps.getProperty(key));
                    }
                    log.info("{} overlay applied ({} keys overridden)", envFile, envProps.size());
                } else {
                    // not a hard failure — just fall back to base config and warn
                    log.warn("Env config '{}' not found on classpath — using base config only", envFile);
                }
            } catch (IOException e) {
                log.warn("Failed to load env config '{}': {}", envFile, e.getMessage());
            }
        }
    }

    private ConfigManager() {}

    // system property wins over the file — lets CI override without editing files
    public static String get(String key) {
        return System.getProperty(key, properties.getProperty(key));
    }

    // same as above but returns defaultValue if the key isn't found anywhere
    public static String get(String key, String defaultValue) {
        return System.getProperty(key, properties.getProperty(key, defaultValue));
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }
}