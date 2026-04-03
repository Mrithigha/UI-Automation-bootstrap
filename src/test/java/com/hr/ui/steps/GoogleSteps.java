package com.hr.ui.steps;

import com.hr.ui.driver.DriverManager;
import com.hr.ui.pages.GooglePage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.assertj.core.api.SoftAssertions;

/**
 * Step definitions for Google search page scenarios.
 *
 * The page object is initialised lazily on first use (after the driver is
 * ready in @Before) rather than in the constructor — safe for parallel runs.
 */
public class GoogleSteps {

    private GooglePage googlePage;

    private GooglePage page() {
        if (googlePage == null) {
            googlePage = new GooglePage(DriverManager.getDriver());
        }
        return googlePage;
    }

    @Given("the user opens Google homepage")
    public void theUserOpensGoogleHomepage() {
        page().open();
    }

    @Then("the Google homepage should load correctly")
    public void theGoogleHomepageShouldLoadCorrectly() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(page().getTitle()).as("Page title").contains("Google");
        soft.assertThat(page().getCurrentPageUrl()).as("URL").contains("google.com");
        soft.assertThat(page().isSearchBoxDisplayed()).as("Search box visible").isTrue();
        soft.assertAll();
    }
}
