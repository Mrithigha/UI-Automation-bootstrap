package com.hr.ui.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads test data from src/test/resources/testdata/testdata.json.
 * Usage: JsonUtils.get("validUser", "email")
 */
public class JsonUtils {

    private static final Logger log = LogManager.getLogger(JsonUtils.class);
    private static final JsonNode testData;

    static {
        try (InputStream is = JsonUtils.class.getClassLoader()
                .getResourceAsStream("testdata/testdata.json")) {
            if (is == null) throw new RuntimeException("testdata.json not found on classpath");
            testData = new ObjectMapper().readTree(is);
            log.info("testdata.json loaded successfully");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load testdata.json", e);
        }
    }

    private JsonUtils() {}

    /**
     * Retrieves a string value by traversing nested keys.
     * Example: JsonUtils.get("validUser", "email")
     */
    public static String get(String... keys) {
        JsonNode node = testData;
        for (String key : keys) {
            node = node.get(key);
            if (node == null) {
                throw new RuntimeException("Key not found in testdata.json: " + String.join(" -> ", keys));
            }
        }
        return node.asText();
    }

    /**
     * Retrieves the raw JsonNode for a top-level key (useful for reading whole objects).
     */
    public static JsonNode getNode(String key) {
        JsonNode node = testData.get(key);
        if (node == null) throw new RuntimeException("Key not found in testdata.json: " + key);
        return node;
    }
}
