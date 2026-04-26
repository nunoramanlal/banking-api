# Eagle Bank API

A REST API for Eagle Bank implementing user, account, and transaction management with JWT-based authentication. Built with Spring Boot 4 and Java 21.

## Overview

Stateless REST API with JWT authentication, refresh token rotation, and PostgreSQL persistence. The API contract is defined in [`src/main/resources/openapi.yaml`](src/main/resources/openapi.yaml).

## Tech stack

- **Java 21** with **Spring Boot 4.0.5**
- **Spring Security** for JWT authentication
- **Spring Data JPA** with **PostgreSQL 17**
- **Flyway** for database migrations
- **jjwt** for JWT generation and parsing
- **Lombok** for boilerplate reduction
- **JUnit 5** with **Mockito** and **AssertJ** for testing
- **JaCoCo** for test coverage reporting
- **Springdoc OpenAPI** for API documentation
- **Spotless** with **Palantir Java Format** for code formatting
- **Docker** 

## Prerequisites

- Java 21
- Maven (or use the included `./mvnw` wrapper)
- Docker

## Quick start

### Development workflow

```bash
# 1. Start PostgreSQL in Docker
./src/test/resources/docker/dockerStart.sh

# 2. Run the application
./mvnw spring-boot:run

# 3. When done, tear down
./src/test/resources/docker/dockerShutdown.sh
```

The API will be available at `http://localhost:8080`.

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Health check**: http://localhost:8080/actuator/health (requires a authenticated user)

### Running tests

Integration tests run against a real PostgreSQL container, so Docker must be running:

```bash
# Start PostgreSQL
./src/test/resources/docker/dockerStart.sh

# Run all tests (unit + integration) and generate coverage report
./mvnw verify

# Tear down
./src/test/resources/docker/dockerShutdown.sh
```

To run only unit tests (no Docker required):

```bash
./mvnw test
```

## Configuration

### Database migrations

Flyway runs automatically on startup, applying any pending migrations from `src/main/resources/db/migration/`:
## Example usage

```bash
# Create a user
curl -X POST http://localhost:8080/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "address": {
      "line1": "10 Example Street",
      "town": "London",
      "county": "Greater London",
      "postcode": "SW1A 1AA"
    },
    "phoneNumber": "+447911123456",
    "email": "test@example.com"
  }'

# Log in
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'

# Fetch user (replace USER_ID and ACCESS_TOKEN)
curl http://localhost:8080/v1/users/usr-abc123 \
  -H "Authorization: Bearer eyJhbGciOi..."

# Refresh tokens
curl -X POST http://localhost:8080/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "550e8400-e29b-41d4-a716-446655440000"}'
```

## Architecture

The codebase follows a layered architecture with explicit boundaries between concerns:

```
Controller  (HTTP/JSON concerns)   Request DTOs  →  Response DTOs
    ↓ mapper
Service     (domain concerns)      Command objects  →  Domain entities
    ↓ repository
Persistence (DB concerns)          Entities
```

Each layer has its own data shape, and mappers translate between them. This keeps the domain layer independent of the API contract.

### Package structure

```
com.eaglebank.banking_api
├── config/         # Spring configuration (security, auditing)
├── controller/     # REST controllers
├── dto/            # Request/response DTOs
├── entity/         # JPA entities
├── exception/      # Custom exceptions and global handler
├── mapper/         # Object mappers between layers
├── repository/     # Spring Data repositories
├── scheduler/      # Scheduled tasks (refresh token cleanup)
├── security/       # JWT service, auth filter, token hasher
└── service/        # Business logic
    ├── command/    # Service input objects
    └── result/     # Service output objects
```

## Authentication

Authentication uses **short-lived JWT access tokens** (15 min) and **long-lived refresh tokens** (7 days) stored in the database.

```
Login → { accessToken, refreshToken }
       ↓
Every protected request uses Authorization: Bearer <accessToken>
       ↓
Access token expires → POST /v1/auth/refresh with refresh token
       ↓
Get new { accessToken, refreshToken } (refresh token rotated)
```

**Design choices:**

- **Email-only login** — the OpenAPI spec does not specify credentials for authentication. Login validates that a user exists with the given email.
- **Stateless access tokens** — no session storage; signature verification per request.
- **Refresh tokens hashed** — refresh tokens are SHA-256 hashed before storage. The raw token is returned to the client at issue time only and never persisted.
- **Refresh token rotation** — each successful refresh revokes the used token and issues a new one. If a *revoked* token is presented again, all sessions for that user are revoked, requiring re-authentication.
- **Single-session login** — on login, any existing refresh tokens for the user are revoked.
- **Scheduled cleanup** — expired and revoked refresh tokens are purged hourly by a scheduled task.

## Validation & error handling

- **Bean Validation** on DTOs for request body validation (`@NotBlank`, `@Email`, `@Pattern`)
- **Path parameter validation** via `@Validated` on controllers
- **DB-level constraints** — check constraints on balance ranges, account types, currencies, and transaction amounts; unique constraint on email
- **Centralised exception handling** via `@RestControllerAdvice` mapping exception types to HTTP status codes

## Persistence

- **UUID-prefixed IDs** — users get `usr-<uuid>` format matching the spec
- **Sequence-generated account numbers** — bank accounts use a Postgres sequence to produce unique 8-digit numbers in the format `01XXXXXX`
- **`BigDecimal` for monetary values** (`DECIMAL(10,2)`)
- **Optimistic locking** — `@Version` on `User` and `Account` 
- **Pessimistic locking on transactions** — `findByAccountNumberForUpdate` acquires a `SELECT ... FOR UPDATE` row lock, preventing race conditions on balance updates.
- **Auditing** — `@CreatedBy` / `@LastModifiedBy` automatically populated via `AuditorAware` and the authenticated principal
- **Cascade deletes** — deleting a user cascades to their refresh tokens and accounts

## Observability

- **Structured logging** via SLF4J with contextual information at each layer
- **Spring Boot Actuator** exposes `/actuator/health`, `/actuator/info`, and `/actuator/metrics`
- HTTP request metrics auto-instrumented at `/actuator/metrics/http.server.requests`

## Testing

**Unit tests** (`src/test/java`)

**Integration tests** (`*IT.java` in `src/test/java`)

```bash
# Run only unit tests (fast, no Docker needed)
./mvnw test

# Run all tests (unit + integration)
./src/test/resources/docker/dockerStart.sh
./mvnw verify
./src/test/resources/docker/dockerShutdown.sh
```

### Coverage
After running `./mvnw verify`, open the report at:

```
target/site/jacoco/index.html
```

In CI, the report is uploaded as a workflow artifact named `jacoco-coverage-report` and can be downloaded from the GitHub Actions run page.

## Docker

### Helper scripts

Located in `src/test/resources/docker/`:

- **`dockerStart.sh`** — starts PostgreSQL, waits for healthy, cleans up any orphaned containers
- **`dockerShutdown.sh`** — stops all services and removes volumes (clean slate)

## Code formatting

```bash
# Check formatting
./mvnw spotless:check

# Auto-format all code
./mvnw spotless:apply
```

The project uses **Palantir Java Format** enforced via Spotless. Formatting runs automatically during the build (`process-sources` phase).

## CI/CD

GitHub Actions runs `./mvnw verify` on every push to `main`. The workflow starts PostgreSQL via `dockerStart.sh`, runs the full test suite, and uploads the JaCoCo coverage report as a downloadable artifact. See [`.github/workflows/ci.yml`](.github/workflows/ci.yml).
