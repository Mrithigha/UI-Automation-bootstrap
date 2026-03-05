package com.hr.ui.steps;

import com.hr.ui.driver.DriverManager;
import com.hr.ui.pages.LandingPage;
import com.hr.ui.pages.LoginPage;
import com.hr.ui.reporting.ExtentReportManager;
import com.hr.ui.utils.JsonUtils;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

public class LoginSteps {

    private static final Logger log = LogManager.getLogger(LoginSteps.class);

    // Page objects are initialised lazily inside steps (driver is ready by the time steps run)
    private LoginPage loginPage;
    private LandingPage landingPage;

    @Given("the user is on the login page")
    public void theUserIsOnTheLoginPage() {
        loginPage = new LoginPage(DriverManager.getDriver());
        loginPage.navigateTo();
        ExtentReportManager.getTest().info("Navigated to login page");
        log.info("Navigated to login page");
    }

    @When("the user enters valid credentials")
    public void theUserEntersValidCredentials() {
        String email = JsonUtils.get("validUser", "email");
        String password = JsonUtils.get("validUser", "password");
        loginPage.enterEmail(email);
        loginPage.enterPassword(password);
        ExtentReportManager.getTest().info("Entered valid credentials for: " + email);
        log.info("Entered credentials for: {}", email);
    }

    @And("clicks the Sign In button")
    public void clicksTheSignInButton() {
        loginPage.clickSignIn();
        ExtentReportManager.getTest().info("Clicked Sign In button");
        log.info("Clicked Sign In button");
    }

    @Then("the user should be redirected to the home page")
    public void theUserShouldBeRedirectedToTheHomePage() {
        landingPage = new LandingPage(DriverManager.getDriver());
        boolean loaded = landingPage.isLoaded();
        ExtentReportManager.getTest().info("Current URL: " + landingPage.getCurrentUrl());
        Assert.assertTrue("Expected redirect to /home but was at: " + landingPage.getCurrentUrl(), loaded);
        log.info("Verified redirect to home page");
    }
}
