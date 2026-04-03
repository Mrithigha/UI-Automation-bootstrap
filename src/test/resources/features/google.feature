#This is a sample feature file that demonstrates the use of the bootstrap framework to build UI automation for any web application
# wherein the generic methods in the BasePage class are leveraged to perform most of the UI interactions

@google @smoke
Feature: Google Homepage

  Scenario: Open Google and verify the page loads correctly
    Given the user opens Google homepage
    Then the Google homepage should load correctly
