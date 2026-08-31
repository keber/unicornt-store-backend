# Configuration

## Prerequisites

**Local execution (without Docker):**

- JDK 21+ (the project targets Java 25)
- Maven 3.8+ (or the bundled `./mvnw`)
- PostgreSQL 16, reachable from the app

**Execution with Docker:**

- Docker and Docker Compose v2

Run `bash scripts/check-env.sh` (Linux/macOS/Git Bash) or
`.\scripts\check-env.ps1` (PowerShell) to verify these are all present and that
`.env` is filled in correctly — see the root [README](../README.md#prerequisites)
for details.

## Environment variables

Every credential comes from an environment variable; none is a literal in source
control. `.env.example` documents every key with a `__CHANGE_ME__` placeholder;
`.env` (the real file) is gitignored.

```bash
cp .env.example .env
# edit .env with real values
```

| Variable | Description | Example |
|----------|-------------|---------|
| `SERVER_PORT` | HTTP port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` or `prod` |
| `POSTGRES_DB` | Database name used by the `db` compose service | `unicornt_db` |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Credentials for the `db` compose service | — |
| `SPRING_DATASOURCE_URL` | Full JDBC URL used by the app | `jdbc:postgresql://localhost:5432/unicornt_db` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Credentials used by the app | — |
| `APP_JWT_SECRET` | Base64, at least 32 bytes. Generate with `openssl rand -base64 32` (Windows install steps in the [README](../README.md#getting-started)) | — |
| `APP_JWT_EXPIRATION_MS` | Token lifetime in milliseconds | `3600000` |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed browser origins | `http://localhost:5173` |

Spring Boot maps `SPRING_DATASOURCE_URL` to `spring.datasource.url` automatically
(relaxed binding); no extra configuration is required.

## Spring profiles

| Profile | `ddl-auto` | Swagger UI / `/api-docs` | Use |
|---------|-----------|---------------------------|-----|
| *(none)* | `validate` | disabled | secure default, closed documentation |
| `dev` | `update` | enabled | local development |
| `prod` | `validate` | disabled | production |

```bash
SPRING_PROFILES_ACTIVE=dev   # or prod
```

## Schema

The schema lives in versioned SQL scripts under
`src/main/resources/db/migration/` (`V1__init.sql`, `V2__seed_reference_data.sql`).
They run through Spring's SQL initializer on every startup — see the comment at
the top of `application.yml` for why (Flyway's Spring Boot 4 autoconfiguration
module is not yet on the classpath) and how to switch to real Flyway once it is.
All statements are idempotent, so re-running them on an existing database is safe.

## JPA

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:validate}   # update in dev, validate in prod
    open-in-view: false
```

`open-in-view` is disabled: every repository call happens inside the service
layer, never lazily during view rendering — there is no view layer to begin with.
