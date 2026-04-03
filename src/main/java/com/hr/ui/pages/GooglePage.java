package com.hr.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for www.google.com.
 *
 * Locators and page-specific actions live here.
 * All interactions delegate to BasePage generic helpers.
 */
public class GooglePage extends BasePage {

    // -------------------------------------------------------------------------
    // Locators
    // -------------------------------------------------------------------------
    private static final By SEARCH_BOX  = By.name("q");
    private static final By GOOGLE_LOGO = By.id("hplogo");          // desktop SVG logo
    private static final By BODY        = By.tagName("body");

    public GooglePage(WebDriver driver) {
        super(driver);
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    public void open() {
        navigateTo("https://www.google.com");
    }

    // -------------------------------------------------------------------------
    // Data accessors — return values so the step can assert with SoftAssertions
    // -------------------------------------------------------------------------

    public String getTitle() {
        return getPageTitle();
    }

    public String getCurrentPageUrl() {
        return getCurrentUrl();
    }

    public boolean isSearchBoxDisplayed() {
        return isElementDisplayed(SEARCH_BOX);
    }
}
