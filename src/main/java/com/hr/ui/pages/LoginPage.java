package com.hr.ui.pages;

import com.hr.ui.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for /login.
 *
 * All locators are declared as private constants and all actions delegate to
 * BasePage generic methods, keeping this class focused purely on what the
 * login page can do rather than how Selenium finds elements.
 */
public class LoginPage extends BasePage {

    private static final Logger log = LogManager.getLogger(LoginPage.class);

    // -------------------------------------------------------------------------
    // Locators
    // -------------------------------------------------------------------------
    private static final By EMAIL_FIELD    = By.id("email");
    private static final By PASSWORD_FIELD = By.id("password");
    private static final By SIGN_IN_BUTTON = By.cssSelector("button[type='submit']");
    private static final By ERROR_MESSAGE  = By.className("error-message");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // -------------------------------------------------------------------------
    // Page actions
    // -------------------------------------------------------------------------

    public void navigateTo() {
        String url = ConfigManager.get("base.url") + "/login";
        log.info("Navigating to login page: {}", url);
        navigateTo(url);
    }

    public void enterEmail(String email) {
        typeText(EMAIL_FIELD, email);
    }

    public void enterPassword(String password) {
        typeText(PASSWORD_FIELD, password);
    }

    public void clickSignIn() {
        click(SIGN_IN_BUTTON);
    }

    /** Convenience method: enters credentials and clicks Sign In in one call. */
    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickSignIn();
    }

    // -------------------------------------------------------------------------
    // Assertions / state queries
    // -------------------------------------------------------------------------

    public String getErrorMessage() {
        return getElementText(ERROR_MESSAGE);
    }

    public boolean isErrorDisplayed() {
        try {
            return waitForVisible(ERROR_MESSAGE).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void verifyErrorMessage(String expectedMessage) {
        verifyTextInElement(ERROR_MESSAGE, expectedMessage);
    }

    public void verifyOnLoginPage() {
        verifyUrlContains("/login");
    }
}
