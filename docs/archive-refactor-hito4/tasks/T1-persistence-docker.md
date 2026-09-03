# T1 — Persistence and virtualization

**Worktree:** `../unicornt-worktrees/persistence-docker`
**Branch:** `refactor/h4-persistence-docker`, cut from `refactor/hito4` after T0
**Covers:** plan stage 3
**Rubric:** closes **C2 = 3/3**
**Runs in parallel with:** T2, T3, T4

Read [../CONVENTIONS.md](../CONVENTIONS.md) first. You own only the paths listed for
T1 in its ownership table. You touch no Java source under `web/` or `security/`, and
you never edit `pom.xml`.

---

## Scope

Bring up a containerized PostgreSQL 16 with a persistent volume, point the
application at it through environment variables, and remove every MySQL and
Supabase trace from the repository.

## Tasks

1. **`docker-compose.yml`** — replace the current single-service file with the
   two-service version from plan stage 3, task 1: an `app` service building from the
   Dockerfile and a `db` service on `postgres:16-alpine`. Required properties:
   - `db` environment from `${POSTGRES_DB}`, `${POSTGRES_USER}`, `${POSTGRES_PASSWORD}`
   - named volume `postgres_data` mounted at `/var/lib/postgresql/data`
   - `pg_isready` healthcheck, and `app` waiting on `depends_on: db: { condition: service_healthy }`
   - drop the obsolete `version: '3'` key
   No credential literal in the file — every value comes from the environment.

2. **`.env.example`** — T0 created it; extend or correct it so it covers every key
   the compose file and `application.yml` read, all with `__CHANGE_ME__` placeholders.
   Document that `APP_JWT_SECRET` is generated with `openssl rand -base64 32`.

3. **`application.yml` (base)** — secure by default, exactly the shape in plan stage
   3, task 3: datasource from environment, `ddl-auto: ${JPA_DDL_AUTO:validate}`,
   `open-in-view: false`, `hibernate.jdbc.time_zone: UTC`, the `app.jwt` and
   `app.cors` keys, and springdoc `api-docs`/`swagger-ui` both `enabled: false`.
   Keep the `spring.mvc.throw-exception-if-no-handler-found: true` T0 added.

4. **`application-dev.yml`** — `ddl-auto: update`, `show-sql: true`, springdoc
   enabled with `path: /api-docs` and `path: /swagger-ui.html`, `try-it-out-enabled: true`.

5. **`application-prod.yml`** — `ddl-auto: validate`, springdoc both disabled.

6. **Purge MySQL and Supabase** — remove any `jdbc:mysql`, `com.mysql.*`,
   `connection-init-sql`, `hibernate.default_schema` pointing at a Supabase schema,
   and any leftover `application-*.properties`. Migrate `sql/security_tables.sql` to
   PostgreSQL syntax if it is still used as a seed; otherwise fold it into the
   Flyway migration below.

7. **Flyway** (the plan's recommended option — take it, because `prod` runs with
   `ddl-auto: validate` and would otherwise need a schema created by hand):
   - `src/main/resources/db/migration/V1__init.sql` with the full schema:
     products, categories, product types, users, roles, users_roles, cart items,
     addresses, orders.
   - a `V2__seed_reference_data.sql` if a seed is needed — reference data only, no
     user credentials.
   - Flyway dependencies are already in `pom.xml` if T0 added them; if they are
     missing, **stop and report to the orchestrator** rather than editing `pom.xml`.
   - If Flyway is not viable, fall back to the documented option: `dev` creates the
     schema with `ddl-auto: update`, and the README says so. Record which option you
     took in the handoff note.

8. **`Dockerfile`** — multi-stage, copying `target/app.jar` (T0 set `finalName` to
   `app`). Use **`eclipse-temurin:25-jdk` and `eclipse-temurin:25-jre`**, not the 21
   images shown in the plan snippet: this project is on Java 25.

9. **`src/test/resources/application.properties`** — align the test configuration
   with the new setup. Either an in-memory profile or Testcontainers; do not let
   tests reach for a real database that may not be running.

## Definition of Done

```bash
docker compose up -d db                       # container reports healthy
docker compose ps                             # db healthy
mvn -q -DskipTests compile                     # green
mvn -q test                                    # green
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # connects, schema created
docker compose restart db                      # data survives the restart
grep -rni "mysql\|supabase" . --exclude-dir=.git --exclude-dir=target \
  --exclude=REFACTOR-UNICORNT-HITO4.md --exclude=INSTRUCCIONES-AGENTE-EVALUADOR-HITO4.md   # empty
grep -rniE "password\s*[:=]\s*[^$\{]" docker-compose.yml src/main/resources/*.yml           # empty
```

Rubric boxes this closes: compose at the root with a `postgres:16-alpine` service,
environment variables, a `postgres_data` volume, the app connecting over
`jdbc:postgresql://` with the `org.postgresql` driver.

## Handoff note

Write `docs/refactor/handoffs/persistence-docker.md` from the template. Record at
minimum: whether you took Flyway or the documented-schema fallback, the exact set of
environment variables you introduced, and any configuration key another task now has
to respect.
