@insurance-catalog @get-active-plans
Feature: Retrieve only active insurance plans
  As a quote calculator
  I want to stream only active insurance plans
  So that I can quote products that are currently sellable

  Background:
    Given the insurance catalog service is available at "http://localhost:8080"

  Scenario: Stream only active seeded plans as Server-Sent Events
    Given the catalog is seeded with the default insurance plans
    When I send a GET request to "/api/insurance-catalog/active"
    Then the response status code should be 200
    And the response Content-Type should be "text/event-stream"
    And the SSE stream should complete successfully
    And the SSE stream should contain 2 plan events
    And every streamed plan should have active equal to true
    And the SSE events should include the following plan entities:
      | id | name         | basePrice | minAge | maxAge | active |
      | 1  | Plan Básico  | 50.0      | 18     | 65     | true   |
      | 4  | Plan Familia | 200.0     | 18     | 80     | true   |
    And the SSE stream should not contain plans named:
      | Plan Estándar |
      | Plan Premium  |
      | Plan Completo |

  Scenario: Active-plans payload uses the domain entity field names
    Given the catalog is seeded with the default insurance plans
    When I send a GET request to "/api/insurance-catalog/active"
    Then the response status code should be 200
    And each SSE event JSON should have exactly the fields:
      | id        |
      | name      |
      | basePrice |
      | minAge    |
      | maxAge    |
      | active    |
    And no SSE event JSON should contain the fields:
      | namePlan  |
      | pricePlan |
      | scopeAge  |

  Scenario: Active plans expose numeric age bounds instead of a scope string
    Given the catalog is seeded with the default insurance plans
    When I send a GET request to "/api/insurance-catalog/active"
    Then the SSE event for plan named "Plan Básico" should have minAge 18 and maxAge 65
    And the SSE event for plan named "Plan Familia" should have minAge 18 and maxAge 80

  @no-active-plans
  Scenario: No active plans returns 404 with a catalog error body
    Given the insurance catalog has no active plans
    When I send a GET request to "/api/insurance-catalog/active"
    Then the response status code should be 404
    And the error response JSON should contain:
      | field     | value                      |
      | message   | there are not plan active  |
    And the error response JSON should contain a non-empty "timestamp"
    And the error response should not be an SSE stream of plan events
