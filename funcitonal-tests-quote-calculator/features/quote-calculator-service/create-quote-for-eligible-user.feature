@quote-calculator @create-quote
Feature: Create insurance quotes for an eligible prospect
  As a sales or underwriting consumer
  I want to stream personalized quotes from the quote calculator
  So that I can offer sellable plans with the correct final price

  Background:
    Given the quote calculator service is available at "http://localhost:8081"
    And the insurance catalog service is available at "http://localhost:8080"
    And the catalog is seeded with the default insurance plans

  Scenario: Eligible non-smoker receives both active plans at base price
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
    And the response Content-Type should be "text/event-stream"
    And the SSE stream should complete successfully
    And the SSE stream should contain 2 quote events
    And the SSE events should include the following quotes:
      | fullname         | planName     | price |
      | Daniel Chanduvi  | Plan Básico  | 50.0  |
      | Daniel Chanduvi  | Plan Familia | 200.0 |

  Scenario: Eligible smoker pays a 20 percent surcharge on each matching plan
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "12345678A",
        "fullname": "Juan García López",
        "occupation": "Ingeniero",
        "annualIncome": 45000.00,
        "isSmoker": true,
        "age": 35
      }
      """
    Then the response status code should be 200
    And the SSE stream should contain 2 quote events
    And the SSE events should include the following quotes:
      | fullname          | planName     | price |
      | Juan García López | Plan Básico  | 60.0  |
      | Juan García López | Plan Familia | 240.0 |

  Scenario: Quote payload uses response DTO field names
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "87654321B",
        "fullname": "María Rodríguez Pérez",
        "occupation": "Médica",
        "annualIncome": 55000.00,
        "isSmoker": false,
        "age": 42
      }
      """
    Then the response status code should be 200
    And each SSE event JSON should have exactly the fields:
      | fullname |
      | planName |
      | price    |
    And no SSE event JSON should contain the fields:
      | dni           |
      | occupation    |
      | annualIncome  |
      | isSmoker      |
      | age           |
      | basePrice     |

  Scenario: Age that only fits Plan Familia returns a single quote
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "99999999D",
        "fullname": "Ana García Sánchez",
        "occupation": "Consultora",
        "annualIncome": 35000.00,
        "isSmoker": false,
        "age": 70
      }
      """
    Then the response status code should be 200
    And the SSE stream should contain 1 quote event
    And the SSE events should include the following quotes:
      | fullname            | planName     | price |
      | Ana García Sánchez  | Plan Familia | 200.0 |
    And the SSE stream should not contain quotes for plans named:
      | Plan Básico   |
      | Plan Estándar |
      | Plan Premium  |
      | Plan Completo |
