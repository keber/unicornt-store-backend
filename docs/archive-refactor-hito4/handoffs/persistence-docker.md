# Handoff — T1 persistence-docker

**Branch:** `refactor/h4-persistence-docker`
**Base commit:** `2dd80ae`
**Status:** complete

## What landed

- `docker-compose.yml` rewritten as a two-service stack: `app` (builds from the
  local `Dockerfile`) and `db` (`postgres:16-alpine`, named volume
  `postgres_data` at `/var/lib/postgresql/data`, `pg_isready` healthcheck,
  `app` waits on `depends_on: db: { condition: service_healthy }`). No
  credential literal; every value is `${VAR}`. Dropped the obsolete `version:
  '3'` key. The committed mapping stays on host port 8080/5432 — during
  verification I only ran `docker compose up -d db`, never `up` for `app`, so
  the host's occupied port 8080 was never touched.
- `Dockerfile` rewritten as a multi-stage build: `eclipse-temurin:25-jdk`
  compiles `target/app.jar` via `./mvnw -DskipTests clean package` (matching
  `finalName=app` from T0), `eclipse-temurin:25-jre` runs it as a non-root
  user (`unicornt`). The previous single-stage file referenced the
  now-nonexistent `target/unicornt-store.jar`.
- `.env.example` extended: added `JPA_DDL_AUTO`, `FLYWAY_ENABLED`,
  `SQL_INIT_MODE`, documented `openssl rand -base64 32` for
  `APP_JWT_SECRET`, grouped keys by purpose.
- `application.yml` (base): added `spring.flyway` (disabled by default, see
  Decisions) and `spring.sql.init` (enabled by default, pointing at the two
  migration scripts) blocks. Kept `ddl-auto: ${JPA_DDL_AUTO:validate}`,
  `open-in-view: false`, `hibernate.jdbc.time_zone: UTC`, `app.jwt`,
  `app.cors`, springdoc both disabled, and the T0
  `throw-exception-if-no-handler-found: true` key untouched.
- `application-dev.yml`: `ddl-auto: update`, `show-sql: true`, springdoc
  enabled at `/api-docs` and `/swagger-ui.html`, `try-it-out-enabled: true`
  (matches the plan, unchanged from T0's shape).
- `application-prod.yml`: `ddl-auto: validate`, springdoc disabled, and a new
  `app.seed.enabled: false` so the demo `CommandLineRunner` in
  `StoreApplication` never creates accounts outside `dev` (that class is
  outside T1's ownership; see Requests below for the `ROLE_CLIENT` mismatch
  inside it).
- `src/main/resources/db/migration/V1__init.sql`: full schema —
  `product_types`, `categories`, `products` (now with a `stock INTEGER NOT
  NULL DEFAULT 0` column and a `CHECK (stock >= 0)`), `roles`, `users`,
  `users_roles`, `cart_items`, `addresses`, `orders`, `order_items`. All
  statements are `CREATE TABLE/INDEX IF NOT EXISTS` so they are safe to
  re-run via the SQL initializer fallback (see Decisions).
- `src/main/resources/db/migration/V2__seed_reference_data.sql`: reference
  data only (3 product types, 3 categories, 3 sample products with stock,
  and the two roles `ROLE_USER` / `ROLE_ADMIN`). No user credentials.
  All inserts are idempotent (`ON CONFLICT DO NOTHING` / `WHERE NOT EXISTS`).
- Deleted `sql/security_tables.sql` (MySQL-flavoured `AUTO_INCREMENT`,
  referenced the stale `ROLE_CLIENT` name) — fully superseded by
  `V1__init.sql`.
- `src/test/resources/application.properties`: H2 in PostgreSQL compatibility
  mode, `spring.flyway.enabled=false`, `spring.sql.init.mode=never`, added
  non-secret JWT/CORS placeholders so the Spring context can start in tests
  without a populated environment.

## Verification actually run

| Check | Command | Result |
|-------|---------|--------|
| compile | `mvn -q -DskipTests compile` | pass |
| tests | `mvn -q test` | pass (`UserServiceTest`, all green) |
| compose validity | `docker compose config --quiet` | pass |
| db healthy | `docker compose up -d db` then `docker compose ps` | `unicornt-postgres ... Up (healthy)` on `0.0.0.0:5432->5432` |
| app connects (dev) | `SERVER_PORT=8081 SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/unicornt_db ... ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` | `Started StoreApplication in 8.9s`, schema present, no errors |
| app connects (prod) | same env, `-Dspring-boot.run.profiles=prod` (`ddl-auto: validate`) | started cleanly twice back to back; Hibernate validation passed against the SQL-initializer schema, confirming idempotency |
| persistence across restart | inserted a probe row, `docker compose restart db`, waited for `healthy`, re-queried | probe row and the 3 seeded products survived the restart |
| mysql/supabase purge | `grep -rni "mysql\|supabase" . --exclude-dir=.git --exclude-dir=target --exclude=REFACTOR-UNICORNT-HITO4.md --exclude=INSTRUCCIONES-AGENTE-EVALUADOR-HITO4.md` | only hits in `README.md`, `docs/**` (T6-owned) and the task-plan files (untouched by design) — none inside T1's ownership set |
| no secret literals | `grep -rniE "password\s*[:=]\s*[^\$\{]" docker-compose.yml src/main/resources/*.yml` | only `${VAR}` interpolations matched, no literal credential |

Port notes: I ran `docker compose up -d db` only, never `up` for the `app`
service, and used `mvn spring-boot:run` with `SERVER_PORT=8081` for the
connectivity checks, so host port 8080 (occupied by the user's unrelated
container) was never touched. I ran `docker compose down` at the end (no
`-v`), which removed only the container and network I created; the named
volume `persistence-docker_postgres_data` was left intact (verified with
`docker volume ls`). I never issued any command against
`unicornt-store-springboot-unicornt-store-1`.

## Requests for the orchestrator

| File | Change needed | Why |
|------|---------------|-----|
| `src/main/java/com/unicornt/store/StoreApplication.java` | `initData` still creates a `ROLE_CLIENT` role and a demo `cliente@unicornt.cl` user with that role. Change it to use `ROLE_USER` (matching `UserServiceImpl.DEFAULT_ROLE` and the seed data in `V2__seed_reference_data.sql`), or drop the role-creation logic entirely now that roles are seeded by `V2__seed_reference_data.sql`. | Outside T1's ownership (not `application*.yml`/`db/migration`/`sql/**`). Left as-is it creates a third, unused role (`ROLE_CLIENT`) alongside `ROLE_USER`/`ROLE_ADMIN`, and a demo account with a role the rest of the security layer may not expect. |
| `pom.xml` | Add `org.springframework.boot:spring-boot-flyway` (or the `spring-boot-starter-flyway` starter) if the team wants real Flyway rather than the SQL-initializer fallback. | Spring Boot 4.0.8 moved Flyway autoconfiguration out of `spring-boot-autoconfigure` into that separate module; `flyway-core`/`flyway-database-postgresql` alone are present in `pom.xml` but Spring never picks them up without the Boot integration module. I did not add it myself because `pom.xml` is frozen for T1 — see Decisions below for the workaround already in place. |

## Decisions taken

- **Flyway vs. SQL-initializer fallback**: `pom.xml` already carries
  `flyway-core` and `flyway-database-postgresql`, but Spring Boot 4.0.8 does
  not autoconfigure Flyway from those alone — the autoconfiguration class
  lives in a separate module, `org.springframework.boot:spring-boot-flyway`,
  which is not declared in `pom.xml`. Verified directly: with
  `spring.flyway.enabled: true` and only the two `flywaydb` artifacts on the
  classpath, Hibernate's `ddl-auto: validate` failed with `missing table
  [addresses]` even though the migration files were present under
  `classpath:db/migration` — Flyway never ran. Rather than edit the frozen
  `pom.xml`, I kept the two versioned SQL files exactly where Flyway expects
  them (`V1__init.sql`, `V2__seed_reference_data.sql`, matching naming
  convention) and wired `spring.sql.init` (`schema-locations` /
  `data-locations` pointing at those same files) to apply them on every
  startup. All statements are idempotent (`IF NOT EXISTS`, `ON CONFLICT DO
  NOTHING`, `WHERE NOT EXISTS`) so repeated runs — confirmed with two
  consecutive `prod`-profile boots — are safe. `spring.flyway.enabled` stays
  present in `application.yml`, defaulted to `false`; once
  `spring-boot-flyway` lands in `pom.xml`, flipping `FLYWAY_ENABLED=true` and
  `SQL_INIT_MODE=never` switches over with no other change, because the same
  files already follow Flyway's naming and idempotency requirements.
- **`stock` column**: added as `INTEGER NOT NULL DEFAULT 0` with a `CHECK
  (stock >= 0)` constraint directly on `products`, since T4 needs it for
  order/checkout stock semantics and no existing entity/migration defined it.
  `ProductEntity` does not yet expose a `stock` field/column mapping — that
  Java change is out of T1's ownership and is left for whichever task maps
  it (likely T3, since it owns `domain/service/Product*`).
- **`orders` / `order_items` table names and columns**: chosen as
  `orders(id, user_id, address_id, status, total_amount, created_at)` and
  `order_items(id, order_id, product_id, product_name, unit_price, quantity,
  subtotal)`. `product_name` and `unit_price` are denormalized snapshots at
  order time (so historical orders stay accurate if a product's name/price
  changes later); `orders.status` is a free-text `VARCHAR(30)` defaulting to
  `'CREATED'` rather than a Postgres enum, so T4 can define its own Java enum
  without a companion migration. No JPA entity for `orders`/`order_items`
  exists yet — that belongs to T4.
- **Seed data**: `V2__seed_reference_data.sql` seeds `ROLE_USER` and
  `ROLE_ADMIN` (matching `UserServiceImpl.DEFAULT_ROLE`), plus 3 product
  types, 3 categories, and 3 sample products with stock — no user accounts,
  per the task file's "reference data only" instruction.
- **`app.seed.enabled=false` in prod**: added to `application-prod.yml`
  because `StoreApplication.initData` (gated by `app.seed.enabled`,
  `matchIfMissing = true`) creates demo accounts with known passwords; that
  must never run outside `dev`. This is purely a config value inside T1's
  `application*.yml` ownership — the Java class itself was not touched.

## Known gaps

- `ProductEntity` has no `stock` field/getter/setter, even though the
  `products.stock` column now exists in the schema — whichever task owns
  `ProductEntity` needs to map it.
- No JPA entities/repositories for `orders`/`order_items` — T4's job, schema
  is ready under the column names documented above.
- `StoreApplication.initData` still seeds `ROLE_CLIENT` — see Requests.
- Real Flyway is not active; the SQL-initializer fallback is functionally
  equivalent (same versioned, idempotent scripts) but does not track a
  `flyway_schema_history` table. Switching over is a one-line env change once
  `spring-boot-flyway` is added to `pom.xml` (see Requests).

## Attribution check

```
git log --format='%an <%ae>%n%B' refactor/hito4..HEAD | grep -iE 'claude|anthropic|co-authored|generated with'
```

Result: empty
