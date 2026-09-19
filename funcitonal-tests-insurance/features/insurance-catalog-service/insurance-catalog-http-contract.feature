@insurance-catalog @http-contract
Feature: Insurance catalog HTTP contract
  As an API consumer
  I want catalog endpoints to follow a stable HTTP contract
  So that clients can stream plans reliably

  Background:
    Given the insurance catalog service is available at "http://localhost:8080"
    And the catalog is seeded with the default insurance plans

  Scenario Outline: Catalog read endpoints accept GET and stream SSE
    When I send a GET request to "<path>"
    Then the response status code should be 200
    And the response Content-Type should be "text/event-stream"
    And the SSE stream should complete successfully

    Examples:
      | path                         |
      | /api/insurance-catalog       |
      | /api/insurance-catalog/active|

  Scenario Outline: Catalog read endpoints reject unsupported methods
    When I send a "<method>" request to "<path>"
    Then the response status code should be 405

    Examples:
      | method | path                          |
      | POST   | /api/insurance-catalog        |
      | PUT    | /api/insurance-catalog        |
      | DELETE | /api/insurance-catalog        |
      | PATCH  | /api/insurance-catalog        |
      | POST   | /api/insurance-catalog/active |
      | PUT    | /api/insurance-catalog/active |
      | DELETE | /api/insurance-catalog/active |

  Scenario: Unknown catalog path returns 404
    When I send a GET request to "/api/insurance-catalog/unknown"
    Then the response status code should be 404

  Scenario: SSE events for catalog plans are valid JSON objects
    When I send a GET request to "/api/insurance-catalog"
    Then every SSE data payload should be parseable as JSON
    And no SSE event should be empty

  Scenario: Active catalog SSE events are valid JSON objects
    When I send a GET request to "/api/insurance-catalog/active"
    Then every SSE data payload should be parseable as JSON
    And no SSE event should be empty
