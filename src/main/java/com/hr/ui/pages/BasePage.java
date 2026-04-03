package com.hr.ui.pages;

import com.hr.ui.config.ConfigManager;
import com.hr.ui.utils.RetryUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/*
 * BasePage — the base class that all page objects extend.
 *
 * The idea is that page classes shouldn't talk directly to Selenium. They declare their
 * locators as constants and call the helpers here, which handle waiting, logging, and
 * retry logic in one place. That keeps page classes readable — just locators and
 * business-level methods.
 *
 * What's in here:
 *   - waitForVisible / waitForClickable — explicit waits baked into every interaction
 *   - click / typeText / select — everyday Selenium actions with waits included
 *   - retryClick — for buttons that occasionally need a second attempt
 *   - iframe / window / alert helpers — context switching without the boilerplate
 *   - table helpers — grab text from rows/cells without fiddly index arithmetic
 *   - verify methods — assert text, URLs, and element visibility in a readable way
 *   - date picker helpers — covers both native date inputs and custom calendar widgets
 *
 * Everything is logged so when a test fails you can see exactly what the framework
 * was doing right before it broke.
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
    // 1. ELEMENT RETRIEVAL
    // =========================================================================

    /** Waits for the element to be present in the DOM, then returns it. */
    protected WebElement find(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Waits for at least one element matching the locator to be present, then returns all matches.
     * Returns an empty list (never null) when nothing matches after the wait.
     */
    protected List<WebElement> findAll(By locator) {
        wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        return driver.findElements(locator);
    }

    // =========================================================================
    // 2. WAIT METHODS
    // =========================================================================

    /** Waits until the element is visible and returns it. */
    protected WebElement waitForVisible(By locator) {
        log.debug("Waiting for visible: {}", locator);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Waits until the element is clickable (visible + enabled + not obscured) and returns it. */
    protected WebElement waitForClickable(By locator) {
        log.debug("Waiting for clickable: {}", locator);
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Waits until the element is no longer visible (e.g. spinner dismissed, modal closed). */
    protected void waitForInvisible(By locator) {
        log.debug("Waiting for invisible: {}", locator);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /** Waits until {@code document.readyState == "complete"} — call after any full page reload. */
    protected void waitForPageLoad() {
        log.debug("Waiting for page load");
        wait.until(d -> js.executeScript("return document.readyState").equals("complete"));
        log.info("Page load complete — URL: {}", driver.getCurrentUrl());
    }

    /** Waits until the current URL contains {@code urlFragment}. */
    protected void waitForUrlContains(String urlFragment) {
        log.debug("Waiting for URL to contain: {}", urlFragment);
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    /** Waits until the element's text content contains {@code text}. */
    protected void waitForTextInElement(By locator, String text) {
        log.debug("Waiting for text '{}' in: {}", text, locator);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    /**
     * Waits until exactly {@code expectedCount} elements matching {@code locator} are present.
     * Useful for asserting that a table or list has been fully populated.
     */
    protected void waitForElementCount(By locator, int expectedCount) {
        log.debug("Waiting for {} elements matching: {}", expectedCount, locator);
        wait.until(d -> d.findElements(locator).size() == expectedCount);
    }

    /**
     * Waits for {@code durationMs} milliseconds. Prefer explicit waits; use this only when
     * no DOM condition can be observed (e.g. waiting for an animation to finish).
     */
    protected void pause(long durationMs) {
        log.debug("Pausing {}ms", durationMs);
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // =========================================================================
    // 3. CLICK METHODS
    // =========================================================================

    /** Waits for the element to be clickable and clicks it. */
    protected void click(By locator) {
        log.info("Clicking: {}", locator);
        waitForClickable(locator).click();
    }

    /** Semantically identical to {@link #click(By)} — use for anchor elements to aid readability. */
    protected void clickLink(By locator) {
        log.info("Clicking link: {}", locator);
        waitForClickable(locator).click();
    }

    /**
     * Clicks via JavaScript — use when the element is obscured by an overlay or when a
     * native click raises {@link ElementClickInterceptedException}.
     */
    protected void jsClick(By locator) {
        log.info("JS-clicking: {}", locator);
        js.executeScript("arguments[0].click();", find(locator));
    }

    /** Double-clicks the element (triggers dblclick DOM event). */
    protected void doubleClick(By locator) {
        log.info("Double-clicking: {}", locator);
        actions.doubleClick(waitForVisible(locator)).perform();
    }

    /**
     * Right-clicks the element (opens browser context menu or triggers contextmenu event).
     */
    protected void rightClick(By locator) {
        log.info("Right-clicking: {}", locator);
        actions.contextClick(waitForVisible(locator)).perform();
    }

    /**
     * Attempts to click the element up to {@code maxAttempts} times, retrying on any exception.
     * Useful for elements that briefly disappear or re-render after an AJAX call.
     *
     * @param locator     element to click
     * @param maxAttempts total number of tries (must be &ge; 1)
     */
    protected void clickWithRetry(By locator, int maxAttempts) {
        RetryUtils.run(maxAttempts, ConfigManager.getInt("retry.delay.millis", 500),
                () -> { click(locator); return null; },
                "click " + locator);
    }

    // =========================================================================
    // 4. TEXT INPUT METHODS
    // =========================================================================

    /** Clears and types {@code text} into the field. */
    protected void typeText(By locator, String text) {
        log.info("Typing into {}: '{}'", locator, text);
        WebElement field = waitForVisible(locator);
        field.clear();
        field.sendKeys(text);
    }

    /**
     * Clears and types {@code text}, truncating to {@code maxLength} characters if needed.
     * Logs a warning if truncation occurs.
     */
    protected void typeText(By locator, String text, int maxLength) {
        String truncated = text.length() > maxLength ? text.substring(0, maxLength) : text;
        if (truncated.length() < text.length()) {
            log.warn("Input truncated to {} chars for: {}", maxLength, locator);
        }
        typeText(locator, truncated);
    }

    /**
     * Types each character individually with a {@code delayMillis} pause between keystrokes.
     * Use for autocomplete fields that need time to react to each character.
     *
     * @param delayMillis pause in ms between characters (e.g. 80–150)
     */
    protected void typeTextSlowly(By locator, String text, int delayMillis) {
        log.info("Slowly typing into {}", locator);
        WebElement field = waitForVisible(locator);
        field.clear();
        for (char c : text.toCharArray()) {
            field.sendKeys(String.valueOf(c));
            try { Thread.sleep(delayMillis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    /** Clears the field without typing anything new. */
    protected void clearText(By locator) {
        log.info("Clearing: {}", locator);
        waitForVisible(locator).clear();
    }

    /**
     * Sets an {@code <input type="file">} by sending the absolute file path as keyboard input.
     *
     * @param absoluteFilePath the full OS path to the file (e.g. {@code "/home/user/report.pdf"})
     */
    protected void uploadFile(By locator, String absoluteFilePath) {
        log.info("Uploading '{}' via: {}", absoluteFilePath, locator);
        find(locator).sendKeys(absoluteFilePath);
    }

    /**
     * Sends a Selenium {@link Keys} constant to the element (e.g. {@code Keys.ENTER},
     * {@code Keys.TAB}, {@code Keys.ESCAPE}, {@code Keys.chord(Keys.CONTROL, "a")}).
     *
     * @param locator element to send the key to
     * @param key     a {@link Keys} value or chord string
     */
    protected void pressKey(By locator, CharSequence key) {
        log.info("Pressing key '{}' on: {}", key, locator);
        waitForVisible(locator).sendKeys(key);
    }

    /** Presses Enter on the element — equivalent to {@code pressKey(locator, Keys.ENTER)}. */
    protected void pressEnter(By locator) {
        pressKey(locator, Keys.ENTER);
    }

    /** Presses Tab on the element — moves focus to the next focusable element. */
    protected void pressTab(By locator) {
        pressKey(locator, Keys.TAB);
    }

    /** Presses Escape on the element — closes dropdowns, modals, or autocomplete suggestions. */
    protected void pressEscape(By locator) {
        pressKey(locator, Keys.ESCAPE);
    }

    // =========================================================================
    // 5. DROPDOWN METHODS  (<select> elements)
    // =========================================================================

    /** Selects the option whose visible text exactly matches {@code visibleText}. */
    protected void selectByVisibleText(By locator, String visibleText) {
        log.info("Selecting '{}' from: {}", visibleText, locator);
        new Select(waitForVisible(locator)).selectByVisibleText(visibleText);
    }

    /** Selects the option whose {@code value} attribute equals {@code value}. */
    protected void selectByValue(By locator, String value) {
        log.info("Selecting by value '{}' from: {}", value, locator);
        new Select(waitForVisible(locator)).selectByValue(value);
    }

    /** Selects the option at zero-based {@code index}. */
    protected void selectByIndex(By locator, int index) {
        log.info("Selecting index {} from: {}", index, locator);
        new Select(waitForVisible(locator)).selectByIndex(index);
    }

    /** Returns the visible text of the currently selected option. */
    protected String getSelectedOption(By locator) {
        String selected = new Select(waitForVisible(locator)).getFirstSelectedOption().getText();
        log.debug("Selected option in {}: '{}'", locator, selected);
        return selected;
    }

    /** Returns the visible text of every option in the dropdown. */
    protected List<String> getAllDropdownOptions(By locator) {
        return new Select(waitForVisible(locator)).getOptions()
                .stream().map(WebElement::getText).toList();
    }

    // =========================================================================
    // 6. CHECKBOX METHODS
    // =========================================================================

    /** Selects the checkbox if not already checked; no-op if already selected. */
    protected void selectCheckbox(By locator) {
        WebElement cb = waitForVisible(locator);
        if (!cb.isSelected()) { cb.click(); log.info("Checkbox selected: {}", locator); }
        else { log.debug("Checkbox already selected: {}", locator); }
    }

    /** Deselects the checkbox if currently checked; no-op if already unchecked. */
    protected void deselectCheckbox(By locator) {
        WebElement cb = waitForVisible(locator);
        if (cb.isSelected()) { cb.click(); log.info("Checkbox deselected: {}", locator); }
        else { log.debug("Checkbox already unchecked: {}", locator); }
    }

    /** Returns {@code true} if the checkbox is currently checked. */
    protected boolean isCheckboxSelected(By locator) {
        return find(locator).isSelected();
    }

    /**
     * Selects all given checkboxes and asserts each is independently selected.
     * Confirms the UI does not mistakenly treat them as mutually exclusive.
     */
    protected void verifyMultipleCheckboxesSelectable(List<By> locators) {
        log.info("Verifying {} checkboxes are independently selectable", locators.size());
        locators.forEach(this::selectCheckbox);
        for (By loc : locators) {
            if (!isCheckboxSelected(loc))
                throw new AssertionError("Checkbox should be selected but is not: " + loc);
        }
        log.info("All {} checkboxes verified as independently selectable", locators.size());
    }

    // =========================================================================
    // 7. RADIO BUTTON METHODS
    // =========================================================================

    /** Selects the radio button; no-op if already selected. */
    protected void selectRadioButton(By locator) {
        WebElement radio = waitForClickable(locator);
        if (!radio.isSelected()) { radio.click(); log.info("Radio selected: {}", locator); }
        else { log.debug("Radio already selected: {}", locator); }
    }

    /** Returns {@code true} if the radio button is currently selected. */
    protected boolean isRadioButtonSelected(By locator) {
        return find(locator).isSelected();
    }

    /**
     * Verifies that selecting each radio button automatically deselects all others in the group.
     *
     * @param locators all radio buttons in the group
     */
    protected void verifyRadioButtonsMutuallyExclusive(List<By> locators) {
        log.info("Verifying mutual exclusivity for {} radio buttons", locators.size());
        for (By target : locators) {
            selectRadioButton(target);
            for (By other : locators) {
                if (!other.equals(target) && isRadioButtonSelected(other))
                    throw new AssertionError(
                        "Radio should be deselected after selecting another: " + other);
            }
        }
        log.info("Radio buttons verified as mutually exclusive");
    }

    // =========================================================================
    // 8. DRAG AND DROP
    // =========================================================================

    /** Drags the source element and drops it onto the target element. */
    protected void dragAndDrop(By sourceLocator, By targetLocator) {
        log.info("Drag-and-drop: {} → {}", sourceLocator, targetLocator);
        actions.dragAndDrop(waitForVisible(sourceLocator), waitForVisible(targetLocator)).perform();
    }

    /**
     * Drags the source element by a pixel offset from its current position.
     * Use for sliders, resizable panels, or canvas-based drag operations.
     *
     * @param xOffset positive = right, negative = left
     * @param yOffset positive = down, negative = up
     */
    protected void dragAndDropByOffset(By sourceLocator, int xOffset, int yOffset) {
        log.info("Drag by offset ({},{}) from: {}", xOffset, yOffset, sourceLocator);
        actions.dragAndDropBy(waitForVisible(sourceLocator), xOffset, yOffset).perform();
    }

    // =========================================================================
    // 9. HOVER / MOUSE ACTIONS
    // =========================================================================

    /** Moves the mouse pointer over the element, triggering CSS :hover and tooltip states. */
    protected void hoverOver(By locator) {
        log.info("Hovering over: {}", locator);
        actions.moveToElement(waitForVisible(locator)).perform();
    }

    /** Hovers over an element and then clicks a sub-element that appears (e.g. a menu item). */
    protected void hoverAndClick(By hoverTarget, By clickTarget) {
        log.info("Hover {} then click {}", hoverTarget, clickTarget);
        actions.moveToElement(waitForVisible(hoverTarget)).perform();
        waitForClickable(clickTarget).click();
    }

    // =========================================================================
    // 10. SCROLL METHODS
    // =========================================================================

    /** Scrolls the element into the centre of the viewport. */
    protected void scrollToElement(By locator) {
        log.info("Scrolling to: {}", locator);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", find(locator));
    }

    /** Scrolls the page to the very top. */
    protected void scrollToTop() {
        log.info("Scrolling to top");
        js.executeScript("window.scrollTo(0, 0);");
    }

    /** Scrolls the page to the very bottom. Useful for infinite-scroll or footer elements. */
    protected void scrollToBottom() {
        log.info("Scrolling to bottom");
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    /**
     * Scrolls by the given pixel offset relative to the current scroll position.
     *
     * @param xPixels horizontal scroll offset (positive = right)
     * @param yPixels vertical scroll offset (positive = down)
     */
    protected void scrollBy(int xPixels, int yPixels) {
        log.info("Scrolling by ({}, {})", xPixels, yPixels);
        js.executeScript("window.scrollBy(arguments[0], arguments[1]);", xPixels, yPixels);
    }

    // =========================================================================
    // 11. ALERT HANDLING
    // =========================================================================

    /** Waits for a browser alert and accepts it (clicks OK). */
    protected void acceptAlert() {
        log.info("Accepting alert");
        wait.until(ExpectedConditions.alertIsPresent()).accept();
    }

    /** Waits for a browser confirm dialog and dismisses it (clicks Cancel). */
    protected void dismissAlert() {
        log.info("Dismissing alert");
        wait.until(ExpectedConditions.alertIsPresent()).dismiss();
    }

    /** Waits for an alert and returns its message text without accepting or dismissing it. */
    protected String getAlertText() {
        String text = wait.until(ExpectedConditions.alertIsPresent()).getText();
        log.info("Alert text: '{}'", text);
        return text;
    }

    /**
     * Types {@code text} into a JavaScript prompt dialog, then accepts it.
     *
     * @param text the string to enter into the prompt
     */
    protected void typeInAlert(String text) {
        log.info("Typing '{}' in alert prompt", text);
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.sendKeys(text);
        alert.accept();
    }

    // =========================================================================
    // 12. IFRAME HANDLING
    // =========================================================================

    /**
     * Switches WebDriver focus into the iframe matched by {@code locator}.
     * All subsequent interactions target elements inside this frame.
     */
    protected void switchToFrame(By locator) {
        log.info("Switching into iframe: {}", locator);
        driver.switchTo().frame(find(locator));
    }

    /**
     * Switches into the iframe at zero-based {@code index} (index within the current document).
     */
    protected void switchToFrameByIndex(int index) {
        log.info("Switching into iframe by index: {}", index);
        driver.switchTo().frame(index);
    }

    /**
     * Switches into the iframe identified by its {@code name} or {@code id} attribute.
     */
    protected void switchToFrameByNameOrId(String nameOrId) {
        log.info("Switching into iframe by name/id: {}", nameOrId);
        driver.switchTo().frame(nameOrId);
    }

    /** Switches focus to the parent frame (one level up). */
    protected void switchToParentFrame() {
        log.info("Switching to parent frame");
        driver.switchTo().parentFrame();
    }

    /** Switches WebDriver focus back to the top-level document, exiting any iframe. */
    protected void switchToDefaultContent() {
        log.info("Switching to default content");
        driver.switchTo().defaultContent();
    }

    // =========================================================================
    // 13. WINDOW / TAB MANAGEMENT
    // =========================================================================

    /** Returns the window handle of the currently focused window/tab. */
    protected String getCurrentWindowHandle() {
        return driver.getWindowHandle();
    }

    /** Returns all open window/tab handles. */
    protected Set<String> getAllWindowHandles() {
        return driver.getWindowHandles();
    }

    /**
     * Switches to the window/tab whose title contains {@code titleFragment}.
     * Restores the original window and throws if no match is found.
     *
     * @param titleFragment case-sensitive substring of the target window title
     */
    protected void switchToWindowByTitle(String titleFragment) {
        log.info("Switching to window with title containing: '{}'", titleFragment);
        String originalHandle = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
            if (driver.getTitle().contains(titleFragment)) {
                log.info("Switched to window: '{}'", driver.getTitle());
                return;
            }
        }
        driver.switchTo().window(originalHandle);
        throw new NoSuchWindowException("No window found with title containing: " + titleFragment);
    }

    /**
     * Switches to the window/tab whose URL contains {@code urlFragment}.
     * Restores the original window and throws if no match is found.
     *
     * @param urlFragment case-sensitive substring of the target window URL
     */
    protected void switchToWindowByUrl(String urlFragment) {
        log.info("Switching to window with URL containing: '{}'", urlFragment);
        String originalHandle = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
            if (driver.getCurrentUrl().contains(urlFragment)) {
                log.info("Switched to window: '{}'", driver.getCurrentUrl());
                return;
            }
        }
        driver.switchTo().window(originalHandle);
        throw new NoSuchWindowException("No window found with URL containing: " + urlFragment);
    }

    /**
     * Waits for a new window/tab to open and switches to it.
     * Call this immediately after the action that triggers a new window (e.g. clicking an external link).
     *
     * @param originalHandle the handle of the window that was focused before the action
     */
    protected void switchToNewWindow(String originalHandle) {
        log.info("Waiting for new window to open");
        wait.until(d -> d.getWindowHandles().size() > 1);
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(originalHandle)) {
                driver.switchTo().window(handle);
                log.info("Switched to new window: '{}'", driver.getTitle());
                return;
            }
        }
        throw new NoSuchWindowException("No new window found after original handle: " + originalHandle);
    }

    /**
     * Closes the currently focused window/tab and switches focus to {@code targetHandle}.
     *
     * @param targetHandle the handle to switch to after closing (e.g. the original window)
     */
    protected void closeCurrentWindowAndSwitch(String targetHandle) {
        log.info("Closing current window and switching to: {}", targetHandle);
        driver.close();
        driver.switchTo().window(targetHandle);
        log.info("Switched back to: '{}'", driver.getTitle());
    }

    /**
     * Opens a new blank browser tab and switches focus to it.
     * The original window handle is returned so you can switch back later.
     *
     * @return the handle of the window that was focused before the new tab was opened
     */
    protected String openNewTab() {
        String originalHandle = driver.getWindowHandle();
        log.info("Opening new tab");
        js.executeScript("window.open('about:blank', '_blank');");
        switchToNewWindow(originalHandle);
        return originalHandle;
    }

    // =========================================================================
    // 14. DATE PICKER HELPERS
    // =========================================================================

    /**
     * Sets the value of a native HTML date input ({@code <input type="date">}) via JavaScript.
     * The value must be in ISO format: {@code yyyy-MM-dd} (e.g. {@code "2024-12-25"}).
     *
     * <p>Prefer this over {@code sendKeys} for date inputs because browser behaviour
     * varies widely (locale-specific formats, masked inputs, etc.).
     *
     * @param locator element locator for the date input
     * @param isoDate date string in {@code yyyy-MM-dd} format
     */
    protected void setDateInputValue(By locator, String isoDate) {
        log.info("Setting date input {} to: {}", locator, isoDate);
        WebElement input = find(locator);
        js.executeScript("arguments[0].value = arguments[1];", input, isoDate);
        // Fire change event so React/Angular/Vue data-binding picks up the value
        js.executeScript("arguments[0].dispatchEvent(new Event('input', {bubbles:true}));", input);
        js.executeScript("arguments[0].dispatchEvent(new Event('change', {bubbles:true}));", input);
    }

    /**
     * Navigates a custom calendar widget to the correct month by clicking the previous or
     * next navigation button until the month/year label matches {@code targetMonthYear}.
     *
     * @param prevBtn         locator for the "previous month" navigation button
     * @param nextBtn         locator for the "next month" navigation button
     * @param monthYearLabel  locator for the element showing the current month and year
     * @param targetMonthYear the exact text to match, e.g. {@code "December 2024"}
     * @param maxClicks       safety cap on navigation clicks to prevent infinite loops
     */
    protected void navigateCalendarToMonth(By prevBtn, By nextBtn, By monthYearLabel,
                                           String targetMonthYear, int maxClicks) {
        log.info("Navigating calendar to: '{}'", targetMonthYear);
        int clicks = 0;
        while (!getElementText(monthYearLabel).equals(targetMonthYear) && clicks < maxClicks) {
            if (isCalendarBefore(getElementText(monthYearLabel), targetMonthYear)) {
                click(nextBtn);
            } else {
                click(prevBtn);
            }
            clicks++;
        }
        if (clicks == maxClicks) {
            log.warn("Calendar navigation hit max clicks ({}) — landed on: '{}'",
                    maxClicks, getElementText(monthYearLabel));
        } else {
            log.info("Calendar navigated to: '{}'", getElementText(monthYearLabel));
        }
    }

    /**
     * Clicks the day cell matching {@code day} (1–31) within an open calendar widget.
     * Only clicks the first enabled, visible cell whose trimmed text equals the day number.
     *
     * @param dayContainerLocator locator that returns all clickable day cells
     * @param day                 day of the month to select
     */
    protected void selectCalendarDay(By dayContainerLocator, int day) {
        log.info("Selecting calendar day: {}", day);
        String dayStr = String.valueOf(day);
        for (WebElement cell : findAll(dayContainerLocator)) {
            if (cell.getText().trim().equals(dayStr) && cell.isEnabled() && cell.isDisplayed()) {
                cell.click();
                log.info("Calendar day {} selected", day);
                return;
            }
        }
        throw new NoSuchElementException("Calendar day not found or not clickable: " + day);
    }

    private boolean isCalendarBefore(String current, String target) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy");
            return YearMonth.parse(current.trim(), fmt).isBefore(YearMonth.parse(target.trim(), fmt));
        } catch (Exception e) {
            log.warn("Cannot parse calendar labels: '{}' vs '{}'", current, target);
            return false;
        }
    }

    // =========================================================================
    // 15. VERIFICATION / ASSERTION METHODS
    // =========================================================================

    /** Asserts the page body text contains {@code text} (case-sensitive). */
    protected void verifyTextDisplayed(String text) {
        log.info("Verifying page contains: '{}'", text);
        if (!find(By.tagName("body")).getText().contains(text))
            throw new AssertionError("Expected text not found on page: '" + text + "'");
    }

    /** Asserts the element's text exactly equals {@code expectedText}. */
    protected void verifyTextInElement(By locator, String expectedText) {
        log.info("Verifying {} has text: '{}'", locator, expectedText);
        String actual = waitForVisible(locator).getText();
        if (!expectedText.equals(actual))
            throw new AssertionError(
                "Text mismatch in " + locator + " — expected: '" + expectedText + "' but was: '" + actual + "'");
    }

    /** Asserts the element's text contains {@code expectedText} (partial match). */
    protected void verifyTextContains(By locator, String expectedText) {
        log.info("Verifying {} contains text: '{}'", locator, expectedText);
        String actual = waitForVisible(locator).getText();
        if (!actual.contains(expectedText))
            throw new AssertionError(
                "Text in " + locator + " does not contain '" + expectedText + "' — actual: '" + actual + "'");
    }

    /** Asserts the element is visible. */
    protected void verifyElementDisplayed(By locator) {
        log.info("Verifying displayed: {}", locator);
        if (!waitForVisible(locator).isDisplayed())
            throw new AssertionError("Element expected to be visible but is not: " + locator);
    }

    /** Asserts the element is NOT visible (may exist in DOM but must not be displayed). */
    protected void verifyElementNotDisplayed(By locator) {
        log.info("Verifying NOT displayed: {}", locator);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
        List<WebElement> els = driver.findElements(locator);
        if (!els.isEmpty() && els.get(0).isDisplayed())
            throw new AssertionError("Element expected to be hidden but is visible: " + locator);
    }

    /** Asserts the element is enabled (interactive). */
    protected void verifyElementEnabled(By locator) {
        log.info("Verifying enabled: {}", locator);
        if (!find(locator).isEnabled())
            throw new AssertionError("Element expected to be enabled but is disabled: " + locator);
    }

    /** Asserts the element is disabled (visible but not interactive). */
    protected void verifyElementDisabled(By locator) {
        log.info("Verifying disabled: {}", locator);
        if (find(locator).isEnabled())
            throw new AssertionError("Element expected to be disabled but is enabled: " + locator);
    }

    /** Asserts the attribute {@code attribute} of the element equals {@code expectedValue}. */
    protected void verifyAttributeValue(By locator, String attribute, String expectedValue) {
        log.info("Verifying attribute '{}' of {} = '{}'", attribute, locator, expectedValue);
        String actual = find(locator).getAttribute(attribute);
        if (!expectedValue.equals(actual))
            throw new AssertionError(
                "Attribute '" + attribute + "' of " + locator
                + " — expected: '" + expectedValue + "' but was: '" + actual + "'");
    }

    /** Asserts the page title equals {@code expectedTitle}. */
    protected void verifyPageTitle(String expectedTitle) {
        String actual = driver.getTitle();
        log.info("Verifying page title — expected: '{}', actual: '{}'", expectedTitle, actual);
        if (!expectedTitle.equals(actual))
            throw new AssertionError(
                "Page title mismatch — expected: '" + expectedTitle + "' but was: '" + actual + "'");
    }

    // =========================================================================
    // 16. NAVIGATION & URL VERIFICATION
    // =========================================================================

    /** Navigates to {@code url} and waits for the page to fully load. */
    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
        waitForPageLoad();
    }

    /** Asserts the current URL exactly equals {@code expectedUrl}. */
    protected void verifyCurrentUrl(String expectedUrl) {
        String actual = driver.getCurrentUrl();
        log.info("Verifying URL — expected: '{}', actual: '{}'", expectedUrl, actual);
        if (!expectedUrl.equals(actual))
            throw new AssertionError(
                "URL mismatch — expected: '" + expectedUrl + "' but was: '" + actual + "'");
    }

    /** Asserts the current URL contains {@code fragment}. */
    protected void verifyUrlContains(String fragment) {
        String actual = driver.getCurrentUrl();
        log.info("Verifying URL contains '{}' — actual: '{}'", fragment, actual);
        if (!actual.contains(fragment))
            throw new AssertionError(
                "Expected URL to contain '" + fragment + "' but was: " + actual);
    }

    /** Waits for URL to contain {@code expectedUrlFragment} then asserts it. */
    protected void verifyNavigationToPage(String expectedUrlFragment) {
        log.info("Verifying navigation to page containing: '{}'", expectedUrlFragment);
        waitForUrlContains(expectedUrlFragment);
        verifyUrlContains(expectedUrlFragment);
    }

    // =========================================================================
    // 17. DATA RETRIEVAL
    // =========================================================================

    /** Returns the trimmed visible text of the element. */
    protected String getElementText(By locator) {
        String text = waitForVisible(locator).getText().trim();
        log.debug("Text of {}: '{}'", locator, text);
        return text;
    }

    /**
     * Returns the {@code value} attribute of an input field.
     * Preferred over {@code getText()} for {@code <input>} and {@code <textarea>} elements.
     */
    protected String getInputValue(By locator) {
        String value = find(locator).getAttribute("value");
        log.debug("Input value of {}: '{}'", locator, value);
        return value;
    }

    /** Returns the value of the given HTML attribute (e.g. {@code "href"}, {@code "placeholder"}). */
    protected String getElementAttribute(By locator, String attribute) {
        String value = find(locator).getAttribute(attribute);
        log.debug("Attribute '{}' of {}: '{}'", attribute, locator, value);
        return value;
    }

    /** Returns the computed CSS property value of the element (e.g. {@code "color"}, {@code "font-size"}). */
    protected String getElementCssValue(By locator, String property) {
        String value = find(locator).getCssValue(property);
        log.debug("CSS '{}' of {}: '{}'", property, locator, value);
        return value;
    }

    /** Returns the total number of elements currently matching {@code locator}. */
    protected int getElementCount(By locator) {
        int count = driver.findElements(locator).size();
        log.debug("Element count for {}: {}", locator, count);
        return count;
    }

    /** Returns the text of every element matching {@code locator} as an ordered list. */
    protected List<String> getAllElementTexts(By locator) {
        return findAll(locator).stream().map(el -> el.getText().trim()).toList();
    }

    /** Returns the current browser URL. */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /** Returns the current page title. */
    public String getPageTitle() {
        return driver.getTitle();
    }

    // =========================================================================
    // 18. TABLE HELPERS
    // =========================================================================

    /**
     * Returns the text of a specific cell in an HTML {@code <table>}.
     * Rows and columns are 1-based; only {@code <tbody>} rows are counted.
     *
     * @param tableLocator locator for the {@code <table>} element
     * @param rowIndex     1-based row number within {@code <tbody>}
     * @param colIndex     1-based column number
     */
    protected String getTableCellText(By tableLocator, int rowIndex, int colIndex) {
        WebElement cell = find(tableLocator).findElement(By.cssSelector(
                String.format("tbody tr:nth-child(%d) td:nth-child(%d)", rowIndex, colIndex)));
        String text = cell.getText().trim();
        log.debug("Table cell [{},{}]: '{}'", rowIndex, colIndex, text);
        return text;
    }

    /** Returns the number of data rows ({@code <tr>} inside {@code <tbody>}) in the table. */
    protected int getTableRowCount(By tableLocator) {
        int count = find(tableLocator).findElements(By.cssSelector("tbody tr")).size();
        log.debug("Table row count for {}: {}", tableLocator, count);
        return count;
    }

    /** Returns all cell values in a given 1-based column as an ordered list. */
    protected List<String> getTableColumnValues(By tableLocator, int colIndex) {
        List<WebElement> rows = find(tableLocator).findElements(By.cssSelector("tbody tr"));
        List<String> values = new ArrayList<>();
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (colIndex <= cells.size()) {
                values.add(cells.get(colIndex - 1).getText().trim());
            }
        }
        log.debug("Column {} values from {}: {}", colIndex, tableLocator, values);
        return values;
    }

    /**
     * Returns the 1-based row index of the first row whose {@code colIndex} cell contains
     * {@code cellText}, or {@code -1} if not found.
     *
     * @param tableLocator locator for the {@code <table>}
     * @param colIndex     1-based column to search in
     * @param cellText     substring to search for (case-sensitive)
     */
    protected int findTableRowByCellText(By tableLocator, int colIndex, String cellText) {
        List<WebElement> rows = find(tableLocator).findElements(By.cssSelector("tbody tr"));
        for (int i = 0; i < rows.size(); i++) {
            List<WebElement> cells = rows.get(i).findElements(By.tagName("td"));
            if (colIndex <= cells.size() && cells.get(colIndex - 1).getText().trim().contains(cellText)) {
                log.debug("Found '{}' in table row {}", cellText, i + 1);
                return i + 1;
            }
        }
        log.warn("'{}' not found in column {} of {}", cellText, colIndex, tableLocator);
        return -1;
    }

    // =========================================================================
    // 19. ELEMENT STATE QUERIES  (non-throwing — return boolean)
    // =========================================================================

    /**
     * Returns {@code true} if at least one element matching {@code locator} is present
     * in the DOM right now (no wait).
     */
    protected boolean isElementPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    /**
     * Returns {@code true} if the element is present AND currently visible.
     * Does not throw; returns {@code false} on any exception.
     */
    protected boolean isElementVisible(By locator) {
        try {
            List<WebElement> els = driver.findElements(locator);
            return !els.isEmpty() && els.get(0).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns {@code true} if the element is visible and enabled (clickable state),
     * without waiting. Does not throw.
     */
    protected boolean isElementClickable(By locator) {
        try {
            WebElement el = driver.findElement(locator);
            return el.isDisplayed() && el.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================================
    // 20. RETRY WRAPPERS
    // =========================================================================

    /**
     * Attempts to click the element, retrying using framework-configured defaults
     * ({@code retry.max.attempts} and {@code retry.delay.millis}).
     */
    protected void clickWithRetry(By locator) {
        RetryUtils.run(() -> { click(locator); return null; }, "click " + locator);
    }

    /**
     * Attempts to find the element, retrying using framework-configured defaults.
     * Returns the element on the first successful attempt.
     */
    protected WebElement findWithRetry(By locator) {
        return RetryUtils.execute(() -> find(locator), "find " + locator);
    }
}
