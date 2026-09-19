@insurance-catalog @get-all-plans
Feature: Retrieve the full insurance plan catalog
  As a quoting or catalog consumer
  I want to stream every insurance plan from the catalog
  So that I can display or process both active and inactive products

  Background:
    Given the insurance catalog service is available at "http://localhost:8080"
    And the catalog is seeded with the default insurance plans

  Scenario: Stream all seeded plans as Server-Sent Events
    When I send a GET request to "/api/insurance-catalog"
    Then the response status code should be 200
    And the response Content-Type should be "text/event-stream"
    And the SSE stream should complete successfully
    And the SSE stream should contain 5 plan events
    And the SSE events should include the following catalog DTOs:
      | namePlan       | pricePlan | scopeAge | active |
      | Plan Básico    | 50.0      | 18-65    | true   |
      | Plan Estándar  | 100.0     | 18-70    | false  |
      | Plan Premium   | 150.0     | 25-75    | false  |
      | Plan Familia   | 200.0     | 18-80    | true   |
      | Plan Completo  | 250.0     | 30-85    | false  |

  Scenario: All-plans payload uses catalog DTO field names
    When I send a GET request to "/api/insurance-catalog"
    Then the response status code should be 200
    And each SSE event JSON should have exactly the fields:
      | namePlan  |
      | pricePlan |
      | scopeAge  |
      | active    |
    And no SSE event JSON should contain the fields:
      | id        |
      | name      |
      | basePrice |
      | minAge    |
      | maxAge    |

  Scenario: Age range is exposed as a min-max scope string
    When I send a GET request to "/api/insurance-catalog"
    Then the SSE event for plan "Plan Básico" should have scopeAge "18-65"
    And the SSE event for plan "Plan Familia" should have scopeAge "18-80"
    And every scopeAge value should match the pattern "^\d+-\d+$"

  Scenario: Inactive plans are included in the full catalog
    When I send a GET request to "/api/insurance-catalog"
    Then the SSE stream should contain plans with active true
    And the SSE stream should contain plans with active false
    And the following plans should be inactive:
      | Plan Estándar |
      | Plan Premium  |
      | Plan Completo |

  @empty-catalog
  Scenario: Empty catalog returns an empty SSE stream without error
    Given the insurance catalog has no plans
    When I send a GET request to "/api/insurance-catalog"
    Then the response status code should be 200
    And the response Content-Type should be "text/event-stream"
    And the SSE stream should complete successfully
    And the SSE stream should contain 0 plan events
