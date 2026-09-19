@quote-calculator @eligibility
Feature: Quote eligibility rules
  As an underwriting consumer
  I want prospects who fail age or income rules to receive no quotes
  So that only sellable plans are offered

  Background:
    Given the quote calculator service is available at "http://localhost:8081"
    And the insurance catalog service is available at "http://localhost:8080"
    And the catalog is seeded with the default insurance plans

  Scenario Outline: Prospect younger than every active plan age range is rejected
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "<dni>",
        "fullname": "<fullname>",
        "occupation": "<occupation>",
        "annualIncome": 45000.00,
        "isSmoker": false,
        "age": <age>
      }
      """
    Then the response status code should be 404
    And the error response JSON should contain:
      | field   | value                       |
      | message | No hay seguros para su perfil |

    Examples:
      | dni       | fullname        | occupation | age |
      | 11111111A | Carlos López    | Estudiante | 17  |
      | 11111111B | Lucas Moreno    | Estudiante | 10  |

  Scenario: Prospect older than every active plan age range is rejected
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "99999999E",
        "fullname": "Ana García Sánchez",
        "occupation": "Jubilada",
        "annualIncome": 35000.00,
        "isSmoker": false,
        "age": 81
      }
      """
    Then the response status code should be 404
    And the error response JSON should contain:
      | field   | value                       |
      | message | No hay seguros para su perfil |
    And the error response JSON should contain a non-empty "timestamp"

  Scenario Outline: Annual income must be strictly greater than 30000
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "<dni>",
        "fullname": "<fullname>",
        "occupation": "<occupation>",
        "annualIncome": <annualIncome>,
        "isSmoker": false,
        "age": 35
      }
      """
    Then the response status code should be 404
    And the error response JSON should contain:
      | field   | value                       |
      | message | No hay seguros para su perfil |

    Examples:
      | dni       | fullname          | occupation | annualIncome |
      | 22222222B | Pedro Sánchez     | Empleado   | 25000.00     |
      | 44444444E | Francisco Torres  | Técnico    | 30000.00     |
      | 11111111C | Carlos López      | Estudiante | 15000.00     |

  Scenario: Income just above 30000 is eligible
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "55555555F",
        "fullname": "Elena Ruiz",
        "occupation": "Consultora",
        "annualIncome": 30000.01,
        "isSmoker": false,
        "age": 30
      }
      """
    Then the response status code should be 200
    And the SSE stream should contain 2 quote events
    And the SSE events should include the following quotes:
      | fullname    | planName     | price |
      | Elena Ruiz  | Plan Básico  | 50.0  |
      | Elena Ruiz  | Plan Familia | 200.0 |

  Scenario Outline: Age on the inclusive plan bounds is eligible
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "<dni>",
        "fullname": "<fullname>",
        "occupation": "Empleado",
        "annualIncome": 40000.00,
        "isSmoker": false,
        "age": <age>
      }
      """
    Then the response status code should be 200
    And the SSE stream should contain quotes for plans named:
      | <expectedPlan> |

    Examples:
      | dni       | fullname       | age | expectedPlan |
      | 18000001A | Bound Min User | 18  | Plan Básico  |
      | 18000001B | Bound Min User | 18  | Plan Familia |
      | 65000001A | Bound Max User | 65  | Plan Básico  |
      | 80000001A | Bound Max User | 80  | Plan Familia |

  Scenario: Age 66 is outside Plan Básico but still matches Plan Familia
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "66000001A",
        "fullname": "Rosa Martínez",
        "occupation": "Doctora",
        "annualIncome": 80000.00,
        "isSmoker": false,
        "age": 66
      }
      """
    Then the response status code should be 200
    And the SSE stream should contain 1 quote event
    And the SSE events should include the following quotes:
      | fullname       | planName     | price |
      | Rosa Martínez  | Plan Familia | 200.0 |

  Scenario: Inactive catalog plans are never quoted
    When I send a POST request to "/api/quote-calculator" with body:
      """
      {
        "dni": "77777777H",
        "fullname": "José María López",
        "occupation": "Abogado",
        "annualIncome": 75000.00,
        "isSmoker": false,
        "age": 30
      }
      """
    Then the response status code should be 200
    And the SSE stream should not contain quotes for plans named:
      | Plan Estándar |
      | Plan Premium  |
      | Plan Completo |
