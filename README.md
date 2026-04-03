# UI Automation Bootstrap Framework

A baseline Selenium + Cucumber BDD framework that can be used as a starting point to automate any web application. 
The idea is that the heavy lifting — driver management, config, reporting, logging, screenshots, retries — is already done, so you can focus on writing feature files and page objects for your specific app.

As a sampler, the framework ships with a couple of feature files: one for google.com and one for a privately hosted HRMS application. To point it at your own app, update the URL in `config.properties`, swap in your locators, and adjust the test data in `testdata.json`.

---

## Stack

- **Java 21** + **Maven**
- **Selenium 4** for browser automation, **WebDriverManager** to handle driver binaries automatically
- **Cucumber 7** for BDD-style feature files, **JUnit 4** as the runner
- **AssertJ** for assertions (supports soft assertions out of the box)
- **Extent Reports** for HTML reports after each run
- **REST Assured** for API-based test data setup/teardown
- **Log4j2** for logging, **Jackson** for reading JSON test data

---

## Structure

The framework follows a standard Page Object Model layout:

- `config/` — reads `config.properties` and merges env-specific overlays (e.g. `config-qa.properties`)
- `driver/` — spins up and tears down the browser; uses ThreadLocal so parallel runs don't interfere
- `pages/` — page objects, all extending `BasePage` which handles waits, retries and common interactions
- `utils/` — utilities for JSON test data, screenshots, retries, downloads, and API calls
- `hooks/` — Cucumber `@Before`/`@After` hooks that open the browser, grab screenshots on failure, and flush reports
- `features/` — Cucumber feature files
- `steps/` — step definitions that wire feature files to page objects
- `testdata/testdata.json` — all test data in one place

---

## Prerequisites

- Java 21+, Maven 3.8+
- Chrome (default browser — driver is downloaded automatically, nothing to install manually)

---

## Running Tests

```bash
# run everything
mvn test

# run by tag
mvn test -Dcucumber.filter.tags="@smoke"

# run a specific feature file
mvn test -Dcucumber.features="src/test/resources/features/google.feature"

# run against a specific environment
mvn test -Denv=qa

# change browser
mvn test -Dbrowser=firefox

# headless mode for CI
mvn test -Dheadless=true

# combine as needed
mvn test -Denv=qa -Dbrowser=firefox -Dheadless=true -Dcucumber.filter.tags="@smoke"
```

---

## Configuration

`src/test/resources/config.properties` holds the defaults:

```properties
base.url=http://localhost:3000
browser=chrome
headless=false
implicit.wait=10
download.dir=downloads
```

For environment-specific settings, create a `config-qa.properties` next to it and only put in what's different — typically just the `base.url`. Everything else falls back to the base file.

Any `-D` flag on the command line overrides both files, useful for one-off runs without editing anything.

---

## Test Data

All test data is in `src/test/resources/testdata/testdata.json`. The key passed in the feature file determines which data set is loaded:

```gherkin
When the user enters "valid" credentials     # picks up testdata["validUser"]
When the user enters "invalid" credentials   # picks up testdata["invalidUser"]
```

Update the JSON with credentials and data relevant to your application.

---

## Adding a New Page

Extend `BasePage` and declare locators as constants. The base class handles all the Selenium interactions so the page class stays readable:

```java
public class MyPage extends BasePage {

    private static final By SAVE_BUTTON = By.id("save");

    public MyPage(WebDriver driver) { super(driver); }

    public void clickSave() { click(SAVE_BUTTON); }
}
```

Then create a feature file, write the steps, and wire them to the page object in a steps class.

---

## Reports and Logs

After a run, look in:
- `reports/` — Extent HTML report (timestamped) and Cucumber HTML report
- `logs/automation.log` — full log for the last run (overwritten each time)
- `screenshots/` — failure screenshots, named by scenario
