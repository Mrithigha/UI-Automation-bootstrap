package com.hr.ui.runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Entry point for the Cucumber test suite.
 *
 * Run all tests:       mvn test
 * Run by tag:          mvn test -Dcucumber.filter.tags="@smoke"
 * Run specific feature: mvn test -Dcucumber.features="src/test/resources/features/login.feature"
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.hr.ui.hooks", "com.hr.ui.steps"},
        plugin = {
                "pretty",
                "html:reports/cucumber.html",
                "json:reports/cucumber.json"
        },
        monochrome = true,
        publish = false
)
public class TestRunner {}
