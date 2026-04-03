# The feature file demonstrates the use of the testdata.json file to select the test data to be used for tests
Feature: User Login
  As an HR admin
  I want to log in to the HRMS application
  So that I can access the system

  @login
    #The validation step is not updated for invalid user.
    #this is left intentionally to depict how failures appear in reports
  Scenario Outline: Successful login with valid credentials
    Given the user is on the login page
    When the user enters "<userType>" credentials
    And clicks the Sign In button
    Then the user should be redirected to the home page

    Examples:
    |userType|
    |valid   |
    |invalid |
