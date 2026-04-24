# Eagle Bank API

A REST API for Eagle Bank implementing user, account and transaction management and JWT-based authentication, built with Spring Boot 4 and Java 21.

## Overview

This service exposes endpoints for CRUD operations, with stateless JWT authentication and refresh token rotation. The API contract is defined in [`src/main/resources/openapi.yaml`](src/main/resources/openapi.yaml).

## Tech stack

- **Java 21** with **Spring Boot 4.0.5**
- **Spring Security** for JWT authentication
- **Spring Data JPA** with **H2**
- **jjwt** for JWT generation and parsing
- **Lombok** for boilerplate reduction
- **JUnit 5** with **Mockito** and **AssertJ** for testing
- **Springdoc OpenAPI** for API documentation
- **Spotless** with **Palantir Java Format** for code formatting

## Prerequisites

- Java 21
- Maven (or use the included `./mvnw` wrapper)

## Quick start

```bash
# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw verify

# Format code
./mvnw spotless:apply
```

The API will be available at `http://localhost:8080`.

### Environment variables

| Variable | Description | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Database URL | (required) |
| `SPRING_DATASOURCE_USERNAME` | Database username | (required) |
| `SPRING_DATASOURCE_PASSWORD` | Database password | (required) |
| `JWT_SECRET` | JWT signing secret (32+ chars) | falls back to a dev secret |

Quick local setup:

```bash
export SPRING_DATASOURCE_URL=jdbc:h2:mem:bankingdb
export SPRING_DATASOURCE_USERNAME=sa
export SPRING_DATASOURCE_PASSWORD=password
./mvnw spring-boot:run
```

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

Each layer has its own data shape, and mappers translate between them. This keeps the domain layer reusable and means API contract changes don't affect business logic.

### Package structure

```
com.eaglebank.banking_api
├── config/         # Spring configuration (security, CORS, auditing)
├── controller/     # REST controllers
├── dto/            # Request/response DTOs
├── entity/         # JPA entities
├── exception/      # Custom exceptions and global handler
├── mapper/         # Object mappers between layers
├── repository/     # Spring Data repositories
├── scheduler/      # Scheduled tasks
├── security/       # JWT service and authentication filter
└── service/        # Business logic
    └── command/    # Service-layer command objects
    └── result/     # Service-layer result objects
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
- **Single-session** — on login, any existing refresh tokens for the user are revoked.
- **Refresh token rotation** — each successful refresh revokes the used token and issues a new one. Aiming to limit the window of exploitation if a token is stolen.
- **Scheduled cleanup** — expired/revoked refresh tokens are purged hourly by a scheduled task.

## Security

- **Method-level authorisation** via `@PreAuthorize`
- **Bearer token authentication entry point** — returns 401 (not Spring's default 403) for missing/invalid tokens

## Validation

- **Bean Validation** on DTOs for request body validation (`@NotBlank`, `@Email`, `@Pattern`)
- **Path parameter validation** via `@Validated` on controllers

## Persistence

- **UUID-prefixed IDs** — users get `usr-<uuid>` format matching the spec
- **Optimistic locking** — `@Version` on `User` prevents lost updates under concurrent modification
- **Auditing** — `@CreatedBy` / `@LastModifiedBy` automatically populated via `AuditorAware` and the authenticated principal
- **Cascade deletes** — deleting a user cascades to their refresh tokens
- **Unique email constraint** — enforced at the DB level

## Observability

- **Structured logging** via SLF4J with contextual information at each layer
- **Spring Boot Actuator** exposes `/actuator/health`, `/actuator/info`, and `/actuator/metrics`
- HTTP request metrics auto-instrumented at `/actuator/metrics/http.server.requests`

## Testing

**Unit tests** (`src/test/java`)

**Integration tests** (`*IT.java`) 
```bash
./mvnw verify  # runs both unit tests (Surefire) and IT tests (Failsafe)
```

## CI/CD

GitHub Actions runs `./mvnw verify` on every push to `main` and every pull request. See [`.github/workflows/ci.yml`](.github/workflows/ci.yml).
