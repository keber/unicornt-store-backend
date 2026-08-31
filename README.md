# Unicornt Store — Backend

Pure REST microservice for the Unicornt store: product catalog, shopping cart,
checkout and JWT-based authentication. No server-rendered views — every route
returns JSON, and the API is fully described by an OpenAPI 3 document.

## Stack

| Layer | Technology |
|-------|------------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.8 |
| Web | Spring MVC (REST controllers only, no view resolver) |
| Security | Spring Security 7, stateless JWT bearer tokens |
| Persistence | Spring Data JPA, PostgreSQL 16 |
| Schema | Versioned SQL migrations under `src/main/resources/db/migration` |
| Containers | Docker, Docker Compose (app + PostgreSQL with a persistent volume) |
| API docs | springdoc-openapi (Swagger UI), `dev` profile only |
| Build | Maven Wrapper (`./mvnw`), executable JAR |
| Tests | JUnit 5, Mockito, MockMvc, H2 (unit/slice), Testcontainers (integration) |

## Architecture

```
com.unicornt.store
├── StoreApplication.java
├── domain/
│   ├── service/            business use cases (Product, Cart, Checkout, Order, ...)
│   └── exception/          ResourceNotFoundException, OutOfStockException, DuplicateResourceException
└── infrastructure/
    ├── persistence/
    │   ├── entity/         @Entity classes (ProductEntity, OrderEntity, ...)
    │   └── repository/     Spring Data JpaRepository interfaces
    ├── security/            JWT issuing/parsing, the filter chain, SecurityConfig
    ├── web/
    │   ├── rest/           @RestController classes, one per resource
    │   ├── dto/             request/response records, bean-validated
    │   ├── mapper/          entity <-> DTO translation
    │   └── error/           ErrorResponse, GlobalExceptionHandler
    └── config/              OpenApiConfig
```

`domain` never imports `jakarta.persistence`, `jakarta.servlet` or
`org.springframework.web`: business rules do not know they are backed by JPA or
exposed over HTTP.

## Getting started

```bash
cp .env.example .env      # fill in the placeholders, see below
docker compose up -d      # starts PostgreSQL 16 with a persistent named volume
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile keeps the schema in sync automatically (`ddl-auto: update`) and
exposes the API documentation. The application also applies the versioned SQL
migrations under `src/main/resources/db/migration` on every startup, seeding the
reference catalog data and the `ROLE_USER`/`ROLE_ADMIN` roles.

Generate a JWT signing key for `APP_JWT_SECRET` in `.env`:

```bash
openssl rand -base64 32
```

### Running against the container stack end to end

```bash
docker compose up -d          # app + db, built from the local Dockerfile
```

## API documentation

Available only when `SPRING_PROFILES_ACTIVE=dev`:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

Every endpoint that requires a token carries the **Authorize** button in
Swagger UI — obtain one from `POST /api/v1/auth/login` and paste it in as
`Bearer <token>`.

## Endpoints

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/v1/auth/register` | public |
| POST | `/api/v1/auth/login` | public |
| GET | `/api/v1/auth/me` | authenticated |
| GET | `/api/v1/products` | public |
| GET | `/api/v1/products/{id}` | public |
| POST | `/api/v1/products` | ADMIN |
| PUT | `/api/v1/products/{id}` | ADMIN |
| DELETE | `/api/v1/products/{id}` | ADMIN |
| GET | `/api/v1/categories` | public |
| POST | `/api/v1/categories` | ADMIN |
| GET | `/api/v1/cart` | authenticated |
| POST | `/api/v1/cart/items` | authenticated |
| PATCH | `/api/v1/cart/items/{id}` | authenticated |
| DELETE | `/api/v1/cart/items/{id}` | authenticated |
| GET | `/api/v1/addresses` | authenticated |
| POST | `/api/v1/addresses` | authenticated |
| DELETE | `/api/v1/addresses/{id}` | authenticated |
| POST | `/api/v1/orders` | authenticated |
| GET | `/api/v1/orders` | authenticated |
| GET | `/api/v1/orders/{id}` | authenticated |

All resource endpoints resolve the caller's identity from the JWT
(`Authentication.getName()`), never from a path or body parameter, so a user can
only see and modify their own cart, addresses and orders.

## Security notes

- Every credential — database, JWT secret, CORS origins — comes from an
  environment variable; nothing is a literal in source control. `.env` is
  gitignored, only `.env.example` (placeholders) is versioned.
- Swagger UI and `/api-docs` are only served under the `dev` profile; the base
  profile and `prod` return `404` for both.
- Passwords are hashed with BCrypt. Tokens are HMAC-signed JWTs
  (`io.jsonwebtoken`), stateless — no server-side session.

## Error format

Every error response, including validation failures, uses one shape:

```json
{
  "message": "Product not found: 999999",
  "code": "RESOURCE_NOT_FOUND",
  "status": 404,
  "timestamp": "2026-08-31T12:00:00Z",
  "path": "/api/v1/products/999999",
  "errors": []
}
```

## Testing

```bash
./mvnw test              # unit and slice tests (H2, MockMvc)
./mvnw verify             # same, plus the packaging step CI runs
```

## API client collection

A ready-to-import [Bruno](https://www.usebruno.com/) collection with folders for
Auth, Products, Cart, Orders and Addresses lives in
[`docs/bruno/unicornt-store`](docs/bruno/unicornt-store). It uses `{{baseUrl}}`
and `{{token}}` environment variables; the login request stores the returned
token automatically for the rest of the collection.

## More documentation

- [docs/configuration.md](docs/configuration.md) — environment variables
- [docs/deployment.md](docs/deployment.md) — Docker Compose and manual deployment
- [docs/development.md](docs/development.md) — local development workflow
- [docs/security.md](docs/security.md) — authentication and authorization details
