@quote-calculator @http-contract
Feature: Quote calculator HTTP contract
  As an API consumer
  I want the quote endpoint to follow a stable HTTP contract
  So that clients can submit prospects and stream quotes reliably

  Background:
    Given the quote calculator service is available at "http://localhost:8081"
    And the insurance catalog service is available at "http://localhost:8080"
    And the catalog is seeded with the default insurance plans

  Scenario: Quote endpoint accepts JSON and streams SSE
    When I send a POST request to "/api/quote-calculator" with headers:
      | Content-Type | application/json |
    And body:
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
    And the response Content-Type should be "text/event-stream"
    And every SSE data payload should be parseable as JSON
    And no SSE event should be empty

  Scenario Outline: Quote endpoint rejects unsupported methods
    When I send a "<method>" request to "/api/quote-calculator"
    Then the response status code should be 405

    Examples:
      | method |
      | GET    |
      | PUT    |
      | DELETE |
      | PATCH  |

  Scenario: Unknown quote path returns 404
    When I send a POST request to "/api/quote-calculator/unknown" with body:
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

  Scenario: No-match error is JSON not an SSE quote stream
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "11111111C",
        "fullname": "Carlos López Martínez",
        "occupation": "Estudiante",
        "annualIncome": 15000.00,
        "isSmoker": false,
        "age": 28
      }
      """
    Then the response status code should be 404
    And the error response JSON should contain:
      | field   | value                       |
      | message | No hay seguros para su perfil |
    And the error response JSON should contain a non-empty "timestamp"
    And the error response should not be an SSE stream of quote events
