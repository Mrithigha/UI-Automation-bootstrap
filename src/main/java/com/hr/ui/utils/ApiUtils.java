package com.hr.ui.utils;

import com.hr.ui.config.ConfigManager;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/*
 * ApiUtils — REST Assured wrapper for setting up and tearing down test data via API.
 *
 * The idea is to avoid slow, brittle UI-driven setup.
 * For example, If a test needs a particular data or entity to exist, create it via API in @Before
 * rather than setting it up via UI every time.
 *
 * Base URL comes from api.base.url in config (falls back to base.url if not set).
 * If the API needs auth, call setBearerToken() once in @Before and all subsequent
 * requests will include it automatically.
 *
 * Example:
 *   Response r = ApiUtils.post("/users", Map.of("name", "Jane", "email", "jane@co.com"));
 *   ApiUtils.assertStatus(r, 201);
 *   String userId = ApiUtils.extract(r, "id");
 *   // ... run the UI test ...
 *   ApiUtils.delete("/users/" + userId); // cleanup in @After
 */
public final class ApiUtils {

    private static final Logger log = LogManager.getLogger(ApiUtils.class);

    // Lazily built and reset whenever the token or base URI changes
    private static volatile RequestSpecification sharedSpec;
    private static String currentToken;

    private ApiUtils() {}

    // pass null to clear auth
    public static synchronized void setBearerToken(String token) {
        currentToken = token;
        sharedSpec = null; // force rebuild on next call
        log.info("API bearer token {}", token == null ? "cleared" : "updated");
    }

    // forces the spec to rebuild — call between scenarios if base URI or auth changed
    public static synchronized void reset() {
        sharedSpec = null;
        currentToken = null;
        log.debug("ApiUtils spec reset");
    }

    public static Response get(String endpoint) {
        log.info("GET {}", endpoint);
        return spec().get(endpoint);
    }

    // GET with query params
    public static Response get(String endpoint, Map<String, ?> queryParams) {
        log.info("GET {} params={}", endpoint, queryParams);
        return spec().queryParams(queryParams).get(endpoint);
    }

    public static Response post(String endpoint, Object body) {
        log.info("POST {}", endpoint);
        return spec().body(body).post(endpoint);
    }

    public static Response put(String endpoint, Object body) {
        log.info("PUT {}", endpoint);
        return spec().body(body).put(endpoint);
    }

    public static Response patch(String endpoint, Object body) {
        log.info("PATCH {}", endpoint);
        return spec().body(body).patch(endpoint);
    }

    public static Response delete(String endpoint) {
        log.info("DELETE {}", endpoint);
        return spec().delete(endpoint);
    }

    // throws with the response body in the message so you can see what went wrong
    public static void assertStatus(Response response, int expectedCode) {
        int actual = response.statusCode();
        if (actual != expectedCode) {
            throw new AssertionError(
                "Expected HTTP " + expectedCode + " but got " + actual
                + "\nResponse body: " + response.body().asString());
        }
        log.debug("Status assertion passed: {}", actual);
    }

    public static void assertBodyContains(Response response, String text) {
        String body = response.body().asString();
        if (!body.contains(text)) {
            throw new AssertionError(
                "Expected response body to contain: '" + text + "'\nActual body: " + body);
        }
    }

    // pulls a value out of the JSON response using a JsonPath expression e.g. "data.id" or "users[0].email"
    public static String extract(Response response, String jsonPath) {
        String value = response.jsonPath().getString(jsonPath);
        log.debug("Extracted '{}' = '{}'", jsonPath, value);
        return value;
    }

    public static String bodyAsString(Response response) {
        return response.body().asString();
    }


    private static synchronized RequestSpecification spec() {
        if (sharedSpec == null) {
            String baseUri = ConfigManager.get("api.base.url",
                    ConfigManager.get("base.url", "http://localhost:3000"));
            RequestSpecification spec = RestAssured.given()
                    .baseUri(baseUri)
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .log().ifValidationFails();

            if (currentToken != null && !currentToken.isBlank()) {
                spec = spec.auth().oauth2(currentToken);
            }
            sharedSpec = spec;
            log.info("ApiUtils spec built — baseUri: {}", baseUri);
        }
        return sharedSpec;
    }
}
