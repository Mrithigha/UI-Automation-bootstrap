package com.hr.ui.utils;

import com.hr.ui.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * RetryUtils — wraps any action in a retry loop for when the UI is being flaky.
 *
 * Some interactions (clicking a button that's still animating, typing into a field
 * that hasn't finished loading) fail occasionally even with explicit waits. Rather than
 * introducing sleep() calls everywhere, wrap the action here and it'll retry automatically.
 *
 * Defaults come from config.properties:
 *   retry.max.attempts — how many times to try before giving up (default 3)
 *   retry.delay.millis — pause between attempts in ms (default 500)
 *
 * Quick example:
 *   RetryUtils.execute(() -> click(SAVE_BUTTON), "click Save button");
 *   RetryUtils.execute(5, 1000, () -> typeText(SEARCH_BOX, "term"), "type search term");
 */
public final class RetryUtils {

    private static final Logger log = LogManager.getLogger(RetryUtils.class);

    private RetryUtils() {}

    @FunctionalInterface
    public interface RetryAction<T> {
        T run() throws Exception;
    }

    // uses attempt count and delay from config.properties
    public static <T> T execute(RetryAction<T> action, String description) {
        int maxAttempts = ConfigManager.getInt("retry.max.attempts", 3);
        int delayMillis = ConfigManager.getInt("retry.delay.millis", 500);
        return execute(maxAttempts, delayMillis, action, description);
    }

    // explicit attempt count and delay — use when the defaults aren't right for a specific case
    public static <T> T execute(int maxAttempts, int delayMillis,
                                RetryAction<T> action, String description) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                T result = action.run();
                if (attempt > 1) {
                    log.info("Retry succeeded on attempt {}/{} for: {}", attempt, maxAttempts, description);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for '{}': {}", attempt, maxAttempts, description, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(delayMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted for: " + description, ie);
                    }
                }
            }
        }
        throw new RuntimeException(
            "All " + maxAttempts + " attempts failed for: " + description, lastException);
    }

    // convenience for void actions — no return value needed
    public static void run(RetryAction<Void> action, String description) {
        execute(action, description);
    }

    // same as above but with explicit attempt count and delay
    public static void run(int maxAttempts, int delayMillis,
                           RetryAction<Void> action, String description) {
        execute(maxAttempts, delayMillis, action, description);
    }
}
