Feature: User Login
  As an HR admin
  I want to log in to the HRMS application
  So that I can access the system

  Scenario: Successful login with valid credentials
    Given the user is on the login page
    When the user enters valid credentials
    And clicks the Sign In button
    Then the user should be redirected to the home page
