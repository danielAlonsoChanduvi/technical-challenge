# Technical Challenge — Insurance Catalog and Quote Calculator

This repository is a two-service insurance quoting system. **Insurance Catalog Service** owns the product catalog. **Quote Calculator Service** evaluates a prospect against that catalog and streams personalized quotes.

There is no parent Maven module. Each service is an independent Spring Boot application. Functional tests are Gherkin `.feature` files in separate directories; they describe the expected HTTP behavior and are not wired to a runner yet.

```
technical-challenge/
├── insurance-catalog-service/          # Catalog API (port 8080)
├── quote-calculator-service/           # Quote API (port 8081)
├── funcitonal-tests-insurance/         # Catalog functional scenarios
└── funcitonal-tests-quote-calculator/  # Quote calculator functional scenarios
```

---

## Architecture

```
Client
  POST /api/quote-calculator  ──►  quote-calculator-service (:8081)
                                         │
                                         │ GET /api/insurance-catalog/active
                                         │ WebClient, 10s timeout
                                         ▼
                                   insurance-catalog-service (:8080)
                                         │
                                         │ R2DBC
                                         ▼
                                   H2 in-memory (insurancedb)
```

Start the catalog first, then the quote calculator. The calculator does not persist plans; it always reads active plans from the catalog.

### Layering

| Service | Layers |
|---------|--------|
| Catalog | Controller → Service → R2DBC repository → H2 |
| Quote calculator | Controller → Service → `InsuranceCatalogProxy` (WebClient) → Catalog API |

Both APIs stream results with **Server-Sent Events** (`text/event-stream`), using Project Reactor `Flux`.

---

## Technical aspects

### Stack (both services)

| Item | Value |
|------|--------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.10 |
| Web | Spring WebFlux |
| Reactive | Project Reactor (`Mono` / `Flux`) |
| Persistence (catalog) | Spring Data R2DBC + H2 in-memory |
| HTTP client (quotes) | Spring `WebClient` |
| Validation | `spring-boot-starter-validation` |
| Boilerplate | Lombok |
| Build | Maven Wrapper (`mvnw`) |
| Unit tests | JUnit 5, Mockito, StepVerifier, AssertJ |

The quote calculator `pom.xml` still lists R2DBC/H2, but the service does not use a database. Catalog integration is HTTP only.

### Runtime

| Service | Port | Base URL | Persistence |
|---------|------|----------|-------------|
| `insurance-catalog-service` | 8080 | `http://localhost:8080` | `r2dbc:h2:mem:///insurancedb` |
| `quote-calculator-service` | 8081 | `http://localhost:8081` | None; `insurance-catalog-service.url=http://localhost:8080` |

Catalog H2 console (local only): `http://localhost:8080/h2-console`  
JDBC URL: `jdbc:h2:mem:///insurancedb` — user `sa`, empty password.

### How to run

```bash
# Terminal 1
cd insurance-catalog-service
./mvnw spring-boot:run

# Terminal 2
cd quote-calculator-service
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

Unit tests:

```bash
cd insurance-catalog-service && ./mvnw test
cd quote-calculator-service && ./mvnw test
```

### Error handling (actual HTTP status)

Both services use `@RestControllerAdvice`.

| Exception | Service | Status | Body |
|-----------|---------|--------|------|
| `PlanNotFoundException` | Catalog | **404** | `{ "message", "timestamp" }` — message `there are not plan active` |
| `PlanNotFoudForUser` | Quote calculator | **404** | `{ "message", "timestamp" }` — message `No hay seguros para su perfil` |

If the catalog is down, the proxy times out after 10 seconds and the quote API surfaces an unhandled error (**500**). A catalog **404** (no active plans) is mapped in the proxy to an empty `Flux`, which then becomes `PlanNotFoudForUser` on the quote side.

The quote-calculator README still describes **400** for ineligible users. The code returns **404**. Functional tests follow the code.

### Response shapes

**Catalog — all plans** (`InsurancePlanResponse` DTO):

```json
{ "namePlan": "Plan Básico", "pricePlan": 50.0, "scopeAge": "18-65", "active": true }
```

`scopeAge` is `minAge + "-" + maxAge`.

**Catalog — active plans** (`InsurancePlan` entity):

```json
{ "id": 1, "name": "Plan Básico", "basePrice": 50.0, "minAge": 18, "maxAge": 65, "active": true }
```

The quote calculator deserializes **this** shape (`id`, `name`, `basePrice`, `minAge`, `maxAge`, `active`).

**Quote — success** (`UserProspectResponse`):

```json
{ "fullname": "Daniel Chanduvi", "planName": "Plan Básico", "price": 50.0 }
```

---

## Functional aspects

### Catalog product model

Table `insuranceplan` (`schema.sql` + `data.sql`):

| Plan | Base price | Age range | Active |
|------|------------|-----------|--------|
| Plan Básico | 50 | 18–65 | yes |
| Plan Estándar | 100 | 18–70 | no |
| Plan Premium | 150 | 25–75 | no |
| Plan Familia | 200 | 18–80 | yes |
| Plan Completo | 250 | 30–85 | no |

Only **Plan Básico** and **Plan Familia** are sellable. Inactive rows remain in the full catalog stream.

### Catalog API

| Method | Path | Success | Notes |
|--------|------|---------|--------|
| GET | `/api/insurance-catalog` | 200 SSE | All plans as DTOs. Empty catalog still 200 with zero events. |
| GET | `/api/insurance-catalog/active` | 200 SSE | Active entities only. Empty active set → 404. |

Unsupported methods on these paths should return **405**. An unknown path under the API should return **404**.

### Quote calculator API

| Method | Path | Success | Notes |
|--------|------|---------|--------|
| POST | `/api/quote-calculator` | 200 SSE | JSON body in, SSE quotes out. |

Request (`UserProspectRequest`):

```json
{
  "dni": "71195649",
  "fullname": "Daniel Chanduvi",
  "occupation": "Backend Developer",
  "annualIncome": 50000.00,
  "isSmoker": false,
  "age": 29
}
```

### Eligibility and pricing

Applied in `QuoteCalculatorServiceImpl` against **active** catalog plans:

1. Age is inclusive: `minAge <= age <= maxAge`.
2. Annual income must be **strictly greater than 30,000** (`30000.00` fails; `30000.01` passes).
3. If `isSmoker` is true, `price = basePrice * 1.2`; otherwise `price = basePrice`.
4. If the filtered stream is empty → 404, `"No hay seguros para su perfil"`.

Examples against default seed data:

| Prospect | Result |
|----------|--------|
| Age 29, income 50,000, non-smoker | Básico **50**, Familia **200** |
| Age 35, income 45,000, smoker | Básico **60**, Familia **240** |
| Age 70, income 35,000, non-smoker | Familia **200** only (Básico max age 65) |
| Age 66, income 80,000, non-smoker | Familia **200** only |
| Age 18, income 40,000 | Both active plans (inclusive min) |
| Age 17 or 81 | 404 |
| Income 15,000 / 25,000 / 30,000 | 404 even if age fits |

Occupation and DNI are accepted on the request; they are not used in eligibility or the quote payload.

### End-to-end example

```bash
curl -N -X POST http://localhost:8081/api/quote-calculator \
  -H "Content-Type: application/json" \
  -d "{\"dni\":\"71195649\",\"fullname\":\"Daniel Chanduvi\",\"occupation\":\"Backend Developer\",\"annualIncome\":50000,\"isSmoker\":false,\"age\":29}"
```

Expected SSE events (order follows catalog active-plan order):

```
data: {"fullname":"Daniel Chanduvi","planName":"Plan Básico","price":50.0}

data: {"fullname":"Daniel Chanduvi","planName":"Plan Familia","price":200.0}
```

---

## Functional tests

Functional tests are **Cucumber/Gherkin** specifications of black-box HTTP behavior. They live next to the services, not inside them. There is no step-definition project yet; the `.feature` files are the executable specification once a runner (Cucumber, Karate, or similar) is added.

They assume:

- Catalog at `http://localhost:8080`
- Quote calculator at `http://localhost:8081`
- Default `data.sql` seed unless a scenario tag says otherwise

### Catalog — `funcitonal-tests-insurance`

```
funcitonal-tests-insurance/features/insurance-catalog-service/
├── get-all-insurance-plans.feature
├── get-active-insurance-plans.feature
└── insurance-catalog-http-contract.feature
```

| Feature | What it asserts |
|---------|-----------------|
| Get all plans | 200 SSE, 5 events, DTO fields (`namePlan`, `pricePlan`, `scopeAge`, `active`), inactive plans included, `scopeAge` pattern `min-max` |
| Get active plans | 200 SSE, only Básico (id 1) and Familia (id 4), entity fields, inactive names absent |
| HTTP contract | GET streams SSE, POST/PUT/DELETE/PATCH → 405, unknown path → 404, SSE payloads are JSON |

Special tags (need extra fixtures in the runner):

| Tag | Setup | Expected |
|-----|--------|----------|
| `@empty-catalog` | No rows in `insuranceplan` | GET all → 200, 0 events |
| `@no-active-plans` | Catalog has no `active = true` rows | GET active → 404, `there are not plan active` |

### Quote calculator — `funcitonal-tests-quote-calculator`

```
funcitonal-tests-quote-calculator/features/quote-calculator-service/
├── create-quote-for-eligible-user.feature
├── quote-eligibility-rules.feature
├── quote-calculator-http-contract.feature
└── quote-calculator-catalog-integration.feature
```

| Feature | What it asserts |
|---------|-----------------|
| Create quote for eligible user | Non-smoker gets both active plans at base price; smoker +20%; response DTO fields only; age 70 quotes Familia only |
| Eligibility rules | Too young/old and income `<= 30000` → 404; income `30000.01` eligible; inclusive ages 18/65/80; inactive plans never quoted |
| HTTP contract | POST JSON → SSE; GET/PUT/DELETE/PATCH → 405; unknown path → 404; no-match error is JSON (`message` + `timestamp`), not an SSE quote stream |
| Catalog integration | Quote `planName`/`price` match catalog active plans; empty active catalog → 404; catalog down → 500 |

Special tags:

| Tag | Setup | Expected |
|-----|--------|----------|
| `@no-active-plans` | Catalog up, no active plans | Quote POST → 404, `No hay seguros para su perfil` |
| `@catalog-down` | Catalog not running | Quote POST → 500 |

### How to read a scenario

Gherkin steps are HTTP-level:

- **Given** — services up and catalog seed (or a tagged empty/down fixture)
- **When** — GET/POST to a path, optionally with JSON
- **Then** — status, `Content-Type`, SSE event count, JSON fields, or error body

Example (quote happy path): POST a 29-year-old non-smoker earning 50,000 → 200 SSE with two quotes at 50 and 200.

Example (eligibility): POST income `30000.00` → 404 even though age 35 fits both active plans.

### Implementing the runner later

A future Cucumber (or Karate) module should:

1. Start or wait for both services (catalog first).
2. Parse SSE (`data:` lines) into JSON for assertions.
3. Implement tagged fixtures: empty catalog, no active plans, catalog down.
4. Keep assertions aligned with **code**, especially 404 for no quotes and catalog-empty-active.

---

## Unit tests vs functional tests

| Kind | Location | Scope |
|------|----------|--------|
| Unit | `src/test/java` in each service | Controllers and services with mocks (`InsuranceCatalogService`, `InsuranceCatalogProxy`) |
| Functional | `funcitonal-tests-*` `.feature` files | Real HTTP against running apps and seeded catalog |

Unit tests prove mapping and filter logic in isolation. Functional tests prove ports, SSE, seed data, and service-to-service integration.

---

## License

This project is part of a technical challenge.
