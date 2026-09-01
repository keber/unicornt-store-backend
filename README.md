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

## Prerequisites

| Tool | Needed for | Notes |
|------|------------|-------|
| JDK 21+ | building/running outside a container | [Temurin](https://adoptium.net) recommended; the project targets Java 25 |
| Docker + Docker Compose v2 | either running option below | [Docker Desktop](https://www.docker.com/products/docker-desktop/) on Windows/macOS |
| OpenSSL | generating `APP_JWT_SECRET` once | see below — any other way to produce 32+ random base64 bytes works too |

Run the check script to verify your machine has what it needs before going
further — it also validates `.env` once you've created one:

=== "Bash"
```bash
bash scripts/check-env.sh          # Linux / macOS / Git Bash
```

=== "Powershell"
```powershell
.\scripts\check-env.ps1            # Windows PowerShell
```

Each `[FAIL]` line names a real blocker and how to fix it; `[WARN]` lines
(e.g. port 8080 already in use) don't stop the app from running but are
worth reading. Exit code is `1` if anything required is missing.

## Getting started

There are two ways to run the app: everything in containers (recommended — no
local environment wiring needed), or the database in a container with the app
run on the host for a faster edit/compile/run loop.

### Option A — fully containerized (app + db)

```bash
cp .env.example .env      # fill in the placeholders, see below
docker compose up -d      # builds the app image, starts app + PostgreSQL 16
```

Docker Compose reads `.env` itself (`env_file: .env` in `docker-compose.yml`),
so no extra step is needed here. The app is reachable at `http://localhost:8080`
once `docker compose ps` shows both `unicornt-postgres` and
`unicornt-store-app` healthy/running.

### Option B — app on the host, database in a container

```bash
cp .env.example .env      # fill in the placeholders, see below
docker compose up -d db   # starts PostgreSQL 16 with a persistent named volume
```

`mvnw`/`java -jar` do **not** read `.env` automatically — only `docker compose`
does. Load it into your shell before starting the app:

=== "Bash"
```bash
# bash / Git Bash
set -a; source .env; set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

=== "Powershell"
```powershell
# PowerShell
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
  $k, $v = $_.Split('=', 2)
  [System.Environment]::SetEnvironmentVariable($k.Trim(), $v.Trim(), 'Process')
}
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Skipping this step fails fast with `Unsatisfied dependency ... jwtService`,
because `APP_JWT_SECRET` is unset. The loaded variables only apply to that
shell session — a new terminal window needs the same step again.

Either option: the `dev` profile keeps the schema in sync automatically
(`ddl-auto: update`) and exposes the API documentation. The application also
applies the versioned SQL migrations under `src/main/resources/db/migration`
on every startup, seeding the reference catalog data and the
`ROLE_USER`/`ROLE_ADMIN` roles.

Generate a JWT signing key for `APP_JWT_SECRET` in `.env`:

```bash
openssl rand -base64 32
```

`openssl` ships preinstalled on most Linux distributions and macOS. If it's
missing:

```bash
sudo apt install openssl     # Debian / Ubuntu
sudo yum install openssl     # RHEL / Fedora / CentOS
brew install openssl         # macOS (Homebrew)
```

Windows doesn't ship `openssl`. Install it, then run it by its full path
(it isn't added to `PATH` automatically):

```powershell
winget install -e --id ShiningLight.OpenSSL.Light
& 'C:\Program Files\OpenSSL-Win64\bin\openssl' rand -base64 32
```

If that path doesn't exist on your system, find where winget put it:

```powershell
winget list --id ShiningLight.OpenSSL.Light
```

Any other way to produce 32+ random bytes, base64-encoded, works just as
well — `openssl` is a convenience, not a hard dependency of the app itself.

> **Reusing an existing database volume with different credentials?**
> PostgreSQL only applies `POSTGRES_USER`/`POSTGRES_PASSWORD` the first time it
> initializes an empty volume. If you change those values in `.env` after the
> volume already exists, the app will fail to authenticate. Reset with
> `docker compose down -v` (drops the volume — local data only) before
> bringing the stack back up.

### Running outside a local shell (CI/CD, other environments)

`.env` is a local-development convenience — nothing in the app reads a
`.env` file directly. Every value it holds is a plain environment variable
that any deploy target sets its own way. `docker compose`'s `env_file: .env`
is one such way, and manually loading it into a shell (Option B above) is
another; neither applies to a pipeline or hosting platform, which must set
the equivalent variables through its own mechanism instead.

For GitHub Actions specifically: define the values under the repository's
**Settings → Secrets and variables → Actions**, then reference them in the
workflow YAML, e.g.:

```yaml
env:
  SPRING_PROFILES_ACTIVE: prod
  SPRING_DATASOURCE_URL: ${{ secrets.SPRING_DATASOURCE_URL }}
  SPRING_DATASOURCE_USERNAME: ${{ secrets.SPRING_DATASOURCE_USERNAME }}
  SPRING_DATASOURCE_PASSWORD: ${{ secrets.SPRING_DATASOURCE_PASSWORD }}
  APP_JWT_SECRET: ${{ secrets.APP_JWT_SECRET }}
```

The `verify` job in [.github/workflows/main.yml](.github/workflows/main.yml)
needs none of this — its tests run against an in-memory H2 database, not a
real Postgres, so no `.env`/secrets are required just to build and test.

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
