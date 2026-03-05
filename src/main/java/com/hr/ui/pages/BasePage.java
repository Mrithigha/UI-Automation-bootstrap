package com.hr.ui.pages;

import com.hr.ui.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * BasePage — the single generic foundation for all page objects in the framework.
 *
 * <p>Design principles:
 * <ul>
 *   <li>Every method accepts a {@link By} locator so page classes declare locators as
 *       constants and call generic actions without holding stale {@link WebElement} references.</li>
 *   <li>All interactions are preceded by an explicit wait, making tests resilient to
 *       slow rendering, animations, and AJAX responses.</li>
 *   <li>Assertion helpers use JUnit {@link Assert} so failures are reported as test failures,
 *       not uncaught exceptions.</li>
 *   <li>Every method is logged at DEBUG/INFO level to give full traceability in logs and reports.</li>
 * </ul>
 *
 * <p>Page classes should extend this class, declare their locators as {@code private static final By}
 * constants, and expose business-readable methods that delegate to these generic helpers.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final Actions actions;
    protected final JavascriptExecutor js;

    private static final Logger log = LogManager.getLogger(BasePage.class);

    protected BasePage(WebDriver driver) {
        this.driver  = driver;
        this.wait    = new WebDriverWait(driver, Duration.ofSeconds(ConfigManager.getInt("explicit.wait", 15)));
        this.actions = new Actions(driver);
        this.js      = (JavascriptExecutor) driver;
    }

    // =========================================================================
    // ELEMENT RETRIEVAL
    // =========================================================================

    /**
     * Finds a single element after waiting for it to be present in the DOM.
     *
     * @param locator {@link By} strategy used to locate the element
     * @return the located {@link WebElement}
     */
    protected WebElement find(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Finds all elements matching the locator after waiting for at least one to be present.
     *
     * @param locator {@link By} strategy used to locate elements
     * @return list of matching {@link WebElement}s (never null; may be empty)
     */
    protected List<WebElement> findAll(By locator) {
        wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        return driver.findElements(locator);
    }

    // =========================================================================
    // WAIT METHODS
    // =========================================================================

    /**
     * Waits until the element identified by {@code locator} is visible on the page.
     *
     * @param locator {@link By} strategy for the target element
     * @return the visible {@link WebElement}
     */
    protected WebElement waitForVisible(By locator) {
        log.debug("Waiting for element to be visible: {}", locator);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits until the element identified by {@code locator} is clickable
     * (visible and enabled, not obscured).
     *
     * @param locator {@link By} strategy for the target element
     * @return the clickable {@link WebElement}
     */
    protected WebElement waitForClickable(By locator) {
        log.debug("Waiting for element to be clickable: {}", locator);
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Waits until the element identified by {@code locator} is no longer visible or present.
     * Useful for confirming that loading spinners or modals have disappeared.
     *
     * @param locator {@link By} strategy for the element expected to disappear
     */
    protected void waitForInvisible(By locator) {
        log.debug("Waiting for element to become invisible: {}", locator);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Waits until the browser reports {@code document.readyState == "complete"}.
     * Call this after any navigation or action that triggers a full page reload.
     */
    protected void waitForPageLoad() {
        log.debug("Waiting for page to fully load");
        wait.until(d -> js.executeScript("return document.readyState").equals("complete"));
        log.info("Page load complete — URL: {}", driver.getCurrentUrl());
    }

    /**
     * Waits until the current URL contains the given fragment.
     * Useful for confirming navigation after clicks that trigger routing.
     *
     * @param urlFragment substring expected to appear in the URL (e.g. "/home")
     */
    protected void waitForUrlContains(String urlFragment) {
        log.debug("Waiting for URL to contain: {}", urlFragment);
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    /**
     * Waits until the text content of the element identified by {@code locator}
     * contains the given {@code text}.
     *
     * @param locator {@link By} strategy for the element
     * @param text    the text expected to appear inside the element
     */
    protected void waitForTextInElement(By locator, String text) {
        log.debug("Waiting for text '{}' in element: {}", text, locator);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    // =========================================================================
    // CLICK METHODS
    // =========================================================================

    /**
     * Waits for the element to be clickable and then clicks it.
     * Suitable for buttons, links, icons, and any interactive element.
     *
     * @param locator {@link By} strategy for the element to click
     */
    protected void click(By locator) {
        log.info("Clicking element: {}", locator);
        waitForClickable(locator).click();
    }

    /**
     * Clicks an anchor/link element after waiting for it to be clickable.
     * Semantically identical to {@link #click(By)} but named for readability in step definitions.
     *
     * @param locator {@link By} strategy for the link element
     */
    protected void clickLink(By locator) {
        log.info("Clicking link: {}", locator);
        waitForClickable(locator).click();
    }

    /**
     * Clicks an element using JavaScript instead of a native Selenium click.
     * Use this when an element is obscured by an overlay or when native click
     * raises {@link ElementClickInterceptedException}.
     *
     * @param locator {@link By} strategy for the element to click via JS
     */
    protected void jsClick(By locator) {
        log.info("JS-clicking element: {}", locator);
        WebElement element = find(locator);
        js.executeScript("arguments[0].click();", element);
    }

    // =========================================================================
    // TEXT INPUT METHODS
    // =========================================================================

    /**
     * Clears any existing content in a text input field and types the given text.
     *
     * @param locator {@link By} strategy for the input field
     * @param text    the text to enter
     */
    protected void typeText(By locator, String text) {
        log.info("Typing into {}: '{}'", locator, text);
        WebElement field = waitForVisible(locator);
        field.clear();
        field.sendKeys(text);
    }

    /**
     * Types text into a field, truncating to {@code maxLength} characters if the input
     * exceeds the limit. Useful for fields with a database or UI character cap.
     *
     * @param locator   {@link By} strategy for the input field
     * @param text      the text to enter
     * @param maxLength maximum number of characters allowed in the field
     */
    protected void typeText(By locator, String text, int maxLength) {
        String truncated = text.length() > maxLength ? text.substring(0, maxLength) : text;
        if (truncated.length() < text.length()) {
            log.warn("Input truncated to {} characters for field: {}", maxLength, locator);
        }
        typeText(locator, truncated);
    }

    /**
     * Types text character-by-character with a short pause between keystrokes.
     * Use this for fields with autocomplete suggestions that need time to react to input.
     *
     * @param locator       {@link By} strategy for the input field
     * @param text          the text to enter
     * @param delayMillis   pause in milliseconds between each character (e.g. 100)
     */
    protected void typeTextSlowly(By locator, String text, int delayMillis) {
        log.info("Slowly typing into {}", locator);
        WebElement field = waitForVisible(locator);
        field.clear();
        for (char c : text.toCharArray()) {
            field.sendKeys(String.valueOf(c));
            try { Thread.sleep(delayMillis); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
    }

    /**
     * Clears the content of a text input field without typing anything new.
     *
     * @param locator {@link By} strategy for the input field to clear
     */
    protected void clearText(By locator) {
        log.info("Clearing field: {}", locator);
        waitForVisible(locator).clear();
    }

    /**
     * Sets the value of a file-upload {@code <input type="file">} element by
     * sending the absolute path of the file as keyboard input.
     *
     * @param locator          {@link By} strategy for the file input element
     * @param absoluteFilePath the absolute path to the file to upload (e.g. "/home/user/file.csv")
     */
    protected void uploadFile(By locator, String absoluteFilePath) {
        log.info("Uploading file '{}' via input: {}", absoluteFilePath, locator);
        find(locator).sendKeys(absoluteFilePath);
    }

    // =========================================================================
    // DROPDOWN METHODS
    // =========================================================================

    /**
     * Selects an option from a {@code <select>} dropdown by its visible text label.
     *
     * @param locator      {@link By} strategy for the {@code <select>} element
     * @param visibleText  the exact visible text of the option to select (e.g. "Senior Developer")
     */
    protected void selectByVisibleText(By locator, String visibleText) {
        log.info("Selecting '{}' from dropdown: {}", visibleText, locator);
        new Select(waitForVisible(locator)).selectByVisibleText(visibleText);
    }

    /**
     * Selects an option from a {@code <select>} dropdown by its {@code value} attribute.
     *
     * @param locator {@link By} strategy for the {@code <select>} element
     * @param value   the {@code value} attribute of the option to select (e.g. "senior_dev")
     */
    protected void selectByValue(By locator, String value) {
        log.info("Selecting by value '{}' from dropdown: {}", value, locator);
        new Select(waitForVisible(locator)).selectByValue(value);
    }

    /**
     * Selects an option from a {@code <select>} dropdown by its zero-based index position.
     *
     * @param locator {@link By} strategy for the {@code <select>} element
     * @param index   zero-based index of the option to select (0 = first option)
     */
    protected void selectByIndex(By locator, int index) {
        log.info("Selecting index {} from dropdown: {}", index, locator);
        new Select(waitForVisible(locator)).selectByIndex(index);
    }

    /**
     * Returns the visible text of the currently selected option in a {@code <select>} dropdown.
     *
     * @param locator {@link By} strategy for the {@code <select>} element
     * @return the visible text of the selected option
     */
    protected String getSelectedOption(By locator) {
        String selected = new Select(waitForVisible(locator)).getFirstSelectedOption().getText();
        log.debug("Currently selected option in {}: '{}'", locator, selected);
        return selected;
    }

    /**
     * Returns the visible text of every option in a {@code <select>} dropdown.
     * Useful for asserting that the correct options are populated.
     *
     * @param locator {@link By} strategy for the {@code <select>} element
     * @return ordered list of visible option texts
     */
    protected List<String> getAllDropdownOptions(By locator) {
        return new Select(waitForVisible(locator)).getOptions()
                .stream()
                .map(WebElement::getText)
                .toList();
    }

    // =========================================================================
    // CHECKBOX METHODS
    // =========================================================================

    /**
     * Selects a checkbox if it is not already checked.
     * Has no effect if the checkbox is already selected.
     *
     * @param locator {@link By} strategy for the checkbox {@code <input type="checkbox">}
     */
    protected void selectCheckbox(By locator) {
        WebElement cb = waitForVisible(locator);
        if (!cb.isSelected()) {
            cb.click();
            log.info("Checkbox selected: {}", locator);
        } else {
            log.debug("Checkbox already selected, skipping: {}", locator);
        }
    }

    /**
     * Deselects a checkbox if it is currently checked.
     * Has no effect if the checkbox is already unchecked.
     *
     * @param locator {@link By} strategy for the checkbox {@code <input type="checkbox">}
     */
    protected void deselectCheckbox(By locator) {
        WebElement cb = waitForVisible(locator);
        if (cb.isSelected()) {
            cb.click();
            log.info("Checkbox deselected: {}", locator);
        } else {
            log.debug("Checkbox already deselected, skipping: {}", locator);
        }
    }

    /**
     * Returns {@code true} if the checkbox identified by {@code locator} is currently checked.
     *
     * @param locator {@link By} strategy for the checkbox element
     * @return {@code true} if selected, {@code false} otherwise
     */
    protected boolean isCheckboxSelected(By locator) {
        return find(locator).isSelected();
    }

    /**
     * Verifies that multiple checkboxes can be selected simultaneously (i.e. they are
     * independent of each other). Selects all provided checkboxes and asserts each is checked.
     * This confirms the UI does not behave like mutually exclusive radio buttons.
     *
     * @param locators list of {@link By} locators, one per checkbox to verify
     */
    protected void verifyMultipleCheckboxesSelectable(List<By> locators) {
        log.info("Verifying {} checkboxes can all be selected simultaneously", locators.size());
        locators.forEach(this::selectCheckbox);
        for (By locator : locators) {
            if (!isCheckboxSelected(locator))
                throw new AssertionError("Checkbox should be selected but is not: " + locator);
        }
        log.info("Verified: all {} checkboxes are independently selectable", locators.size());
    }

    // =========================================================================
    // RADIO BUTTON METHODS
    // =========================================================================

    /**
     * Selects a radio button. If the radio button is already selected this is a no-op.
     *
     * @param locator {@link By} strategy for the radio button {@code <input type="radio">}
     */
    protected void selectRadioButton(By locator) {
        WebElement radio = waitForClickable(locator);
        if (!radio.isSelected()) {
            radio.click();
            log.info("Radio button selected: {}", locator);
        } else {
            log.debug("Radio button already selected: {}", locator);
        }
    }

    /**
     * Returns {@code true} if the radio button identified by {@code locator} is selected.
     *
     * @param locator {@link By} strategy for the radio button element
     * @return {@code true} if selected, {@code false} otherwise
     */
    protected boolean isRadioButtonSelected(By locator) {
        return find(locator).isSelected();
    }

    /**
     * Verifies that radio buttons in a group are mutually exclusive: selecting one
     * automatically deselects all others. For each radio button in {@code locators},
     * this method selects it and then confirms that every other radio button in the
     * list is deselected.
     *
     * @param locators list of {@link By} locators representing all radio buttons in a group
     */
    protected void verifyRadioButtonsMutuallyExclusive(List<By> locators) {
        log.info("Verifying mutual exclusivity for {} radio buttons", locators.size());
        for (By target : locators) {
            selectRadioButton(target);
            for (By other : locators) {
                if (!other.equals(target) && isRadioButtonSelected(other))
                    throw new AssertionError(
                        "Radio button should be deselected after selecting another: " + other);
            }
        }
        log.info("Verified: radio buttons in the group are mutually exclusive");
    }

    // =========================================================================
    // DRAG AND DROP
    // =========================================================================

    /**
     * Drags the source element and drops it onto the target element.
     * Both elements must be visible and interactable.
     *
     * @param sourceLocator {@link By} strategy for the element to drag
     * @param targetLocator {@link By} strategy for the element to drop onto
     */
    protected void dragAndDrop(By sourceLocator, By targetLocator) {
        log.info("Drag and drop: {} → {}", sourceLocator, targetLocator);
        WebElement source = waitForVisible(sourceLocator);
        WebElement target = waitForVisible(targetLocator);
        actions.dragAndDrop(source, target).perform();
    }

    /**
     * Drags the source element by the given pixel offset from its current position.
     * Use this when there is no explicit drop target element (e.g. resizable panels,
     * slider handles, or canvas-based drag operations).
     *
     * @param sourceLocator {@link By} strategy for the element to drag
     * @param xOffset       horizontal pixel offset — positive moves right, negative moves left
     * @param yOffset       vertical pixel offset — positive moves down, negative moves up
     */
    protected void dragAndDropByOffset(By sourceLocator, int xOffset, int yOffset) {
        log.info("Drag by offset ({}, {}) from: {}", xOffset, yOffset, sourceLocator);
        WebElement source = waitForVisible(sourceLocator);
        actions.dragAndDropBy(source, xOffset, yOffset).perform();
    }

    // =========================================================================
    // HOVER / MOUSE ACTIONS
    // =========================================================================

    /**
     * Moves the mouse pointer over the element without clicking it.
     * Triggers CSS {@code :hover} states and opens hover menus or tooltips.
     *
     * @param locator {@link By} strategy for the element to hover over
     */
    protected void hoverOver(By locator) {
        log.info("Hovering over element: {}", locator);
        actions.moveToElement(waitForVisible(locator)).perform();
    }

    // =========================================================================
    // SCROLL METHODS
    // =========================================================================

    /**
     * Scrolls the element identified by {@code locator} into the browser viewport.
     * Useful when an element exists in the DOM but is outside the visible area.
     *
     * @param locator {@link By} strategy for the element to scroll to
     */
    protected void scrollToElement(By locator) {
        log.info("Scrolling to element: {}", locator);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", find(locator));
    }

    /**
     * Scrolls the page to the very top (0, 0).
     */
    protected void scrollToTop() {
        log.info("Scrolling to top of page");
        js.executeScript("window.scrollTo(0, 0);");
    }

    /**
     * Scrolls the page to the very bottom.
     * Useful for triggering infinite-scroll content or reaching footer elements.
     */
    protected void scrollToBottom() {
        log.info("Scrolling to bottom of page");
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    // =========================================================================
    // ALERT HANDLING
    // =========================================================================

    /**
     * Waits for a browser alert/confirm/prompt dialog to appear and accepts it (clicks OK).
     */
    protected void acceptAlert() {
        log.info("Accepting browser alert");
        wait.until(ExpectedConditions.alertIsPresent()).accept();
    }

    /**
     * Waits for a browser alert/confirm dialog to appear and dismisses it (clicks Cancel).
     */
    protected void dismissAlert() {
        log.info("Dismissing browser alert");
        wait.until(ExpectedConditions.alertIsPresent()).dismiss();
    }

    /**
     * Waits for a browser alert to appear and returns its message text without
     * accepting or dismissing it.
     *
     * @return the text displayed in the alert dialog
     */
    protected String getAlertText() {
        String text = wait.until(ExpectedConditions.alertIsPresent()).getText();
        log.info("Alert text: '{}'", text);
        return text;
    }

    // =========================================================================
    // IFRAME HANDLING
    // =========================================================================

    /**
     * Switches the WebDriver focus into an iframe identified by {@code locator}.
     * All subsequent interactions apply to elements inside this frame until
     * {@link #switchToDefaultContent()} is called.
     *
     * @param locator {@link By} strategy for the {@code <iframe>} element
     */
    protected void switchToFrame(By locator) {
        log.info("Switching into iframe: {}", locator);
        driver.switchTo().frame(find(locator));
    }

    /**
     * Switches WebDriver focus back to the top-level document, exiting any iframe.
     * Always call this after finishing interactions inside a frame.
     */
    protected void switchToDefaultContent() {
        log.info("Switching back to default content");
        driver.switchTo().defaultContent();
    }

    // =========================================================================
    // VERIFICATION / ASSERTION METHODS
    // =========================================================================

    /**
     * Asserts that the given {@code text} is visible somewhere on the current page
     * by checking the full page body text (case-sensitive).
     *
     * @param text the exact text expected to appear on the page
     */
    protected void verifyTextDisplayed(String text) {
        log.info("Verifying page contains text: '{}'", text);
        String bodyText = find(By.tagName("body")).getText();
        if (!bodyText.contains(text))
            throw new AssertionError("Expected text not found on page: '" + text + "'");
    }

    /**
     * Asserts that the text content of the element identified by {@code locator}
     * exactly equals {@code expectedText}.
     *
     * @param locator      {@link By} strategy for the element to inspect
     * @param expectedText the exact text expected inside the element
     */
    protected void verifyTextInElement(By locator, String expectedText) {
        log.info("Verifying element {} contains text: '{}'", locator, expectedText);
        String actual = waitForVisible(locator).getText();
        if (!expectedText.equals(actual))
            throw new AssertionError(
                "Text mismatch in element " + locator +
                " — expected: '" + expectedText + "' but was: '" + actual + "'");
    }

    /**
     * Asserts that the element identified by {@code locator} is visible on the page.
     *
     * @param locator {@link By} strategy for the element expected to be displayed
     */
    protected void verifyElementDisplayed(By locator) {
        log.info("Verifying element is displayed: {}", locator);
        if (!waitForVisible(locator).isDisplayed())
            throw new AssertionError("Element expected to be visible but is not: " + locator);
    }

    /**
     * Asserts that the element identified by {@code locator} is NOT visible on the page.
     * The element may still exist in the DOM but must not be displayed.
     *
     * @param locator {@link By} strategy for the element expected to be hidden
     */
    protected void verifyElementNotDisplayed(By locator) {
        log.info("Verifying element is NOT displayed: {}", locator);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
        List<WebElement> elements = driver.findElements(locator);
        boolean hidden = elements.isEmpty() || !elements.get(0).isDisplayed();
        if (!hidden) throw new AssertionError("Element expected to be hidden but is visible: " + locator);
    }

    /**
     * Asserts that the element identified by {@code locator} is enabled (interactive).
     * A disabled element is visible but cannot receive user input.
     *
     * @param locator {@link By} strategy for the element
     */
    protected void verifyElementEnabled(By locator) {
        log.info("Verifying element is enabled: {}", locator);
        if (!find(locator).isEnabled())
            throw new AssertionError("Element expected to be enabled but is disabled: " + locator);
    }

    /**
     * Asserts that the element identified by {@code locator} is disabled (not interactive).
     *
     * @param locator {@link By} strategy for the element
     */
    protected void verifyElementDisabled(By locator) {
        log.info("Verifying element is disabled: {}", locator);
        if (find(locator).isEnabled())
            throw new AssertionError("Element expected to be disabled but is enabled: " + locator);
    }

    // =========================================================================
    // NAVIGATION & URL VERIFICATION
    // =========================================================================

    /**
     * Navigates the browser to the given absolute URL and waits for the page to load.
     *
     * @param url the full URL to navigate to (e.g. "http://localhost:3000/login")
     */
    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
        waitForPageLoad();
    }

    /**
     * Asserts that the current browser URL exactly matches {@code expectedUrl}.
     *
     * @param expectedUrl the full URL expected (e.g. "http://localhost:3000/home")
     */
    protected void verifyCurrentUrl(String expectedUrl) {
        String actual = driver.getCurrentUrl();
        log.info("Verifying URL — expected: '{}', actual: '{}'", expectedUrl, actual);
        if (!expectedUrl.equals(actual))
            throw new AssertionError("URL mismatch — expected: '" + expectedUrl + "' but was: '" + actual + "'");
    }

    /**
     * Asserts that the current browser URL contains the given fragment.
     * Preferred over exact-match when query params or hashes may vary.
     *
     * @param fragment substring expected to be present in the URL (e.g. "/home")
     */
    protected void verifyUrlContains(String fragment) {
        String actual = driver.getCurrentUrl();
        log.info("Verifying URL contains '{}' — actual: '{}'", fragment, actual);
        if (!actual.contains(fragment))
            throw new AssertionError("Expected URL to contain '" + fragment + "' but was: " + actual);
    }

    /**
     * Waits for navigation to complete and then verifies the URL contains
     * the expected path fragment. Combines wait + assertion in a single step.
     *
     * @param expectedUrlFragment the URL path fragment expected after navigation (e.g. "/users/create")
     */
    protected void verifyNavigationToPage(String expectedUrlFragment) {
        log.info("Verifying navigation to page containing: '{}'", expectedUrlFragment);
        waitForUrlContains(expectedUrlFragment);
        verifyUrlContains(expectedUrlFragment);
    }

    // =========================================================================
    // DATA RETRIEVAL
    // =========================================================================

    /**
     * Returns the visible text content of the element identified by {@code locator}.
     *
     * @param locator {@link By} strategy for the element
     * @return trimmed visible text of the element
     */
    protected String getElementText(By locator) {
        String text = waitForVisible(locator).getText().trim();
        log.debug("Element text for {}: '{}'", locator, text);
        return text;
    }

    /**
     * Returns the value of the specified HTML attribute of the element.
     * Examples: {@code getAttribute("href")}, {@code getAttribute("value")},
     * {@code getAttribute("class")}, {@code getAttribute("placeholder")}.
     *
     * @param locator   {@link By} strategy for the element
     * @param attribute the HTML attribute name (e.g. "href", "value", "disabled")
     * @return the attribute value, or {@code null} if the attribute does not exist
     */
    protected String getElementAttribute(By locator, String attribute) {
        String value = find(locator).getAttribute(attribute);
        log.debug("Attribute '{}' of {}: '{}'", attribute, locator, value);
        return value;
    }

    /**
     * Returns the computed CSS property value of the element.
     * Examples: {@code getCssValue("color")}, {@code getCssValue("font-size")}.
     *
     * @param locator  {@link By} strategy for the element
     * @param property the CSS property name (e.g. "background-color", "display")
     * @return the computed CSS value as a string
     */
    protected String getElementCssValue(By locator, String property) {
        String value = find(locator).getCssValue(property);
        log.debug("CSS '{}' of {}: '{}'", property, locator, value);
        return value;
    }

    /**
     * Returns the current URL of the browser.
     *
     * @return the full URL string of the currently loaded page
     */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Returns the title of the currently loaded page (the {@code <title>} tag content).
     *
     * @return page title string
     */
    public String getPageTitle() {
        return driver.getTitle();
    }

    // =========================================================================
    // TABLE HELPERS
    // =========================================================================

    /**
     * Returns the text content of a specific cell in an HTML {@code <table>}.
     * Rows and columns are 1-based (row 1 = first data row, col 1 = first column).
     * Header rows ({@code <thead>}) are not counted; this targets {@code <tbody>} rows only.
     *
     * @param tableLocator {@link By} strategy for the {@code <table>} element
     * @param rowIndex     1-based row index within the table body
     * @param colIndex     1-based column index
     * @return the trimmed text content of the specified cell
     */
    protected String getTableCellText(By tableLocator, int rowIndex, int colIndex) {
        WebElement cell = find(tableLocator)
                .findElement(By.cssSelector(
                    String.format("tbody tr:nth-child(%d) td:nth-child(%d)", rowIndex, colIndex)));
        String text = cell.getText().trim();
        log.debug("Table cell [{},{}]: '{}'", rowIndex, colIndex, text);
        return text;
    }

    /**
     * Returns the number of data rows ({@code <tr>} inside {@code <tbody>}) in an HTML table.
     *
     * @param tableLocator {@link By} strategy for the {@code <table>} element
     * @return count of {@code <tbody>} rows; 0 if the table body is empty
     */
    protected int getTableRowCount(By tableLocator) {
        List<WebElement> rows = find(tableLocator).findElements(By.cssSelector("tbody tr"));
        log.debug("Table row count for {}: {}", tableLocator, rows.size());
        return rows.size();
    }
}
