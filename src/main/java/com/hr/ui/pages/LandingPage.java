package com.hr.ui.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for /home — the User Management dashboard.
 */
public class LandingPage extends BasePage {

    private static final Logger log = LogManager.getLogger(LandingPage.class);

    // -------------------------------------------------------------------------
    // Locators
    // -------------------------------------------------------------------------
    private static final By PAGE_HEADING       = By.tagName("h2");
    private static final By CREATE_USER_BUTTON = By.cssSelector("button.btn-success");
    private static final By SEARCH_INPUT       = By.cssSelector("input.search-input");
    private static final By SEARCH_BUTTON      = By.cssSelector("button.btn-primary");
    private static final By USER_TABLE         = By.tagName("table");
    private static final By ERROR_MESSAGE      = By.className("error-message");

    public LandingPage(WebDriver driver) {
        super(driver);
    }

    // -------------------------------------------------------------------------
    // Page actions
    // -------------------------------------------------------------------------

    public void clickCreateUser() {
        click(CREATE_USER_BUTTON);
    }

    public void searchUsers(String keyword) {
        typeText(SEARCH_INPUT, keyword);
        click(SEARCH_BUTTON);
    }

    // -------------------------------------------------------------------------
    // Assertions / state queries
    // -------------------------------------------------------------------------

    public boolean isLoaded() {
        try {
            waitForUrlContains("/home");
            log.info("Landing page loaded — URL: {}", driver.getCurrentUrl());
            return true;
        } catch (Exception e) {
            log.error("Landing page did not load. URL: {}", driver.getCurrentUrl());
            return false;
        }
    }

    public void verifyOnLandingPage() {
        verifyNavigationToPage("/home");
    }

    public String getHeading() {
        return getElementText(PAGE_HEADING);
    }

    public String getUserTableCellText(int row, int col) {
        return getTableCellText(USER_TABLE, row, col);
    }

    public int getUserTableRowCount() {
        return getTableRowCount(USER_TABLE);
    }
}
