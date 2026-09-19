@quote-calculator @catalog-integration
Feature: Quote calculator depends on the insurance catalog
  As a quote calculator
  I want to use only active plans from the catalog service
  So that quotes stay aligned with the current product catalog

  Background:
    Given the quote calculator service is available at "http://localhost:8081"

  Scenario: Quotes are built from catalog active plans
    Given the insurance catalog service is available at "http://localhost:8080"
    And the catalog is seeded with the default insurance plans
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "71195649",
        "fullname": "Daniel Chanduvi",
        "occupation": "Backend Developer",
        "annualIncome": 50000.00,
        "isSmoker": false,
        "age": 29
      }
      """
    Then the response status code should be 200
    And every quoted planName should exist in the catalog active plans
    And every non-smoker quote price should equal the matching active plan basePrice

  @no-active-plans
  Scenario: Catalog with no active plans yields no quotes for the prospect
    Given the insurance catalog service is available at "http://localhost:8080"
    And the insurance catalog has no active plans
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "71195649",
        "fullname": "Daniel Chanduvi",
        "occupation": "Backend Developer",
        "annualIncome": 50000.00,
        "isSmoker": false,
        "age": 29
      }
      """
    Then the response status code should be 404
    And the error response JSON should contain:
      | field   | value                       |
      | message | No hay seguros para su perfil |

  @catalog-down
  Scenario: Catalog unavailable is treated as a calculator failure
    Given the insurance catalog service is not available
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "71195649",
        "fullname": "Daniel Chanduvi",
        "occupation": "Backend Developer",
        "annualIncome": 50000.00,
        "isSmoker": false,
        "age": 29
      }
      """
    Then the response status code should be 500
