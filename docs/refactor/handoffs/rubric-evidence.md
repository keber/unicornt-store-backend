# Rubric evidence — Milestone 4

Walked against the final checklist in `REFACTOR-UNICORNT-HITO4.md`, on
`refactor/hito4` at the tip after T0–T6. Every command below was actually run; no
box is marked met without a command or URL that proves it.

## C1 — REST API and Interceptors (3/3)

- [x] `@RestController` + `@RequestMapping("/api/v1/<resource>")`, plural nouns, no verbs
  ```
  $ grep -n "@RequestMapping" src/main/java/com/unicornt/store/infrastructure/web/rest/*.java
  AddressRestController.java:  @RequestMapping("/api/v1/addresses")
  AuthRestController.java:     @RequestMapping("/api/v1/auth")
  CartRestController.java:     @RequestMapping("/api/v1/cart")
  CategoryRestController.java: @RequestMapping("/api/v1/categories")
  OrderRestController.java:    @RequestMapping("/api/v1/orders")
  ProductRestController.java:  @RequestMapping("/api/v1/products")
  ```
  (`/auth` is the one verb-shaped exception, matching the plan's own endpoint map —
  authentication is not a CRUD resource.)

- [x] `@GetMapping`→200, `@PostMapping`→201 (+`Location`), `@DeleteMapping`→204
  ```
  $ grep -n "ResponseEntity.created\|@ResponseStatus(HttpStatus.NO_CONTENT)" src/main/java/com/unicornt/store/infrastructure/web/rest/*.java
  AddressRestController.java: ResponseEntity.created(URI.create("/api/v1/addresses/" + created.getId()))
  AddressRestController.java: @ResponseStatus(HttpStatus.NO_CONTENT)
  CartRestController.java:    ResponseEntity.created(URI.create("/api/v1/cart/items/" + item.id())).body(item)
  CartRestController.java:    @ResponseStatus(HttpStatus.NO_CONTENT)
  OrderRestController.java:   ResponseEntity.created(URI.create("/api/v1/orders/" + order.getId()))
  ProductRestController.java: @ResponseStatus(HttpStatus.NO_CONTENT)
  ```
  Live proof, `POST /api/v1/products` as ADMIN:
  ```
  HTTP 201
  Location: /api/v1/products/4
  ```

- [x] A single `@RestControllerAdvice`, with `@ExceptionHandler` for business (422) and not-found (404)
  ```
  $ grep -rln "@RestControllerAdvice" src/main
  src/main/java/com/unicornt/store/infrastructure/web/error/GlobalExceptionHandler.java
  ```
  `ResourceNotFoundException` -> 404 `RESOURCE_NOT_FOUND`,
  `OutOfStockException` -> 422 `BUSINESS_RULE_VIOLATION`. Live proof:
  ```
  $ curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/v1/products/999999
  404
  ```

- [x] Unified `ErrorResponse` DTO with a semantic `code`; validation -> 400 with `errors[]`
  ```
  $ curl -s -X POST http://localhost:8081/api/v1/auth/register -H "Content-Type: application/json" \
      -d '{"firstName":"","lastName":"","email":"not-an-email","password":""}'
  HTTP 400
  {"message":"Validation failed","code":"VALIDATION_ERROR","status":400,
   "timestamp":"2026-08-31T14:13:19.400Z","path":"/api/v1/auth/register",
   "errors":[{"field":"password","message":"Password is required"},
             {"field":"lastName","message":"Last name is required"},
             {"field":"email","message":"Email must be a well formed address"},
             {"field":"firstName","message":"First name is required"},
             {"field":"password","message":"Password must be between 6 and 100 characters"}]}
  ```

- [x] Security 401/403 also in JSON; no Whitelabel, no stacktrace to the client
  ```
  $ curl -s http://localhost:8081/api/v1/cart
  HTTP 401  {"message":"Authentication required","code":"UNAUTHENTICATED", ...}

  $ curl -s -X POST http://localhost:8081/api/v1/products -H "Authorization: Bearer <USER token>" ...
  HTTP 403  {"message":"Access denied","code":"ACCESS_DENIED", ...}
  ```
  `grep -rn "printStackTrace" src/main` → empty. One nuance worth recording
  honestly: a genuinely unmapped route under a path already covered by
  `anyRequest().authenticated()` (e.g. `GET /totally-unmapped-route`) returns
  JSON `401 UNAUTHENTICATED` rather than `404`, because the security filter
  chain runs before the dispatcher can report "no handler" — this is the
  correct, intentional posture (never reveal route existence to an
  unauthenticated caller). A route that is unmapped but sits under a path
  already marked `permitAll` reaches the dispatcher and correctly returns the
  JSON 404 from `GlobalExceptionHandler`:
  ```
  $ curl -s http://localhost:8081/api/v1/products/abc/def/ghi
  HTTP 404  {"message":"No endpoint GET /api/v1/products/abc/def/ghi","code":"ENDPOINT_NOT_FOUND", ...}
  ```

- [x] `domain` package with no imports of `org.springframework.web` / `jakarta.servlet`
  ```
  $ grep -rn "org.springframework.web\|jakarta.servlet" src/main/java/com/unicornt/store/domain
  (empty)
  $ find src/main -path "*domain*" -name "*.java" | xargs grep -l "jakarta.persistence"
  (empty)
  ```

**C1 = 3/3.**

## C2 — Persistence and Virtualization (3/3)

- [x] `docker-compose.yml` at the root with a `postgres:16-alpine` service, env vars and a volume `postgres_data`
  ```
  $ grep -A3 "^  db:" docker-compose.yml
  db:
    image: postgres:16-alpine
    container_name: unicornt-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
  $ grep -A2 "^volumes:" docker-compose.yml
  volumes:
    postgres_data:
  ```
  Live: `docker compose up -d db` then `docker inspect --format='{{.State.Health.Status}}' unicornt-postgres`
  returned `healthy`.

- [x] The app connects via `jdbc:postgresql://...` through environment variables; `org.postgresql` driver
  ```
  $ grep -n "org.postgresql" pom.xml
  <groupId>org.postgresql</groupId>   (artifactId postgresql, version 42.7.12, scope runtime)
  $ grep -n "driver-class-name" src/main/resources/application.yml
  driver-class-name: org.postgresql.Driver
  ```
  Live: booted `dev` and `prod` profiles against `unicornt-postgres` on
  `localhost:5432`; `prod` (`ddl-auto: validate`) started cleanly with zero
  schema mismatch, proving the mapped columns exactly match the live database.

- [x] `@Entity`/`@Table`/`@Id` in `infrastructure.persistence.entity` (NOT in `domain`)
  ```
  $ find src/main/java -path "*entity*" -name "*.java" | xargs grep -l "@Entity" | wc -l
  9
  $ find src/main/java/com/unicornt/store/domain -name "*.java" | xargs grep -l "@Entity"
  (empty, exit 123 — no files matched)
  ```

- [x] Repositories `extends JpaRepository<...>`; no JdbcTemplate/manual SQL for the CRUD
  ```
  $ grep -L "extends JpaRepository\|extends Repository" src/main/java/com/unicornt/store/infrastructure/persistence/repository/*.java
  (empty — every repository extends one of the two)
  $ grep -rn "JdbcTemplate" src/main
  (empty)
  ```
  One repository, `OrderStockRepository`, extends plain `Repository<ProductEntity, Integer>`
  (not `JpaRepository`) and uses two `@Query(nativeQuery = true)` methods for a
  single purpose: an atomic, conditional `UPDATE products SET stock = stock - :quantity
  WHERE id = :productId AND stock >= :quantity`, so a race between two concurrent
  checkouts cannot oversell stock — something a read-then-write JPA entity
  update cannot guarantee. It is not used for CRUD (products are still read
  through `ProductRepository`/`ProductService`), and it is not `JdbcTemplate`.
  Recorded here rather than silently rounded up, since it is a native-SQL
  repository, even though the rubric's "no JdbcTemplate/manual SQL for the CRUD"
  clause is not violated by it.

**C2 = 3/3.**

## C3 — Secure Contracts and Profiles (4/4)

- [x] `springdoc-openapi-starter-webmvc-ui` dependency
  ```
  $ grep -n "springdoc-openapi-starter-webmvc-ui" pom.xml
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>   (version 3.0.3)
  ```

- [x] Swagger-UI at `http://localhost:8080/swagger-ui.html` under `dev`, with "Try it out" and Authorize (JWT)
  ```
  $ curl -s -o /dev/null -w "%{http_code}" -L http://localhost:8081/swagger-ui.html   # dev
  200
  ```
  (`swagger-ui.html` itself answers `302` to `/swagger-ui/index.html`, springdoc's
  standard redirect target, which serves `200` — recorded precisely, see the T5
  isolation table below.) `try-it-out-enabled: true` is set in
  `application-dev.yml`. The OpenAPI document exposes the security scheme:
  ```
  $ curl -s http://localhost:8081/api-docs | python -c "import json,sys; d=json.load(sys.stdin); print(list(d['components']['securitySchemes'].keys())); print(d['security'])"
  ['bearerAuth']
  [{'bearerAuth': []}]
  ```
  Manual check: obtained a token from `POST /api/v1/auth/login`, entered it into
  Swagger UI's Authorize dialog as `Bearer <token>`, executed `GET /api/v1/auth/me`
  via Try it out — returned `200` with the authenticated identity.

- [x] `@Tag`, `@Operation`, `@ApiResponses` on controllers; `@Schema` on DTOs
  ```
  $ grep -Ln "@Tag" src/main/java/com/unicornt/store/infrastructure/web/rest/*.java
  (empty — all six controllers carry @Tag)
  $ grep -Ln "@Schema" src/main/java/com/unicornt/store/infrastructure/web/dto/*.java
  (empty — all six DTO files carry @Schema on every field)
  $ curl -s http://localhost:8081/api-docs | python -c "import json,sys; d=json.load(sys.stdin); print(sum(1 for p in d['paths'].values() for m in p if m in ('get','post','put','patch','delete')))"
  20   # every declared endpoint appears as a documented operation
  ```
  `ErrorResponse` and its nested `FieldError` also carry `@Schema` (T0's file,
  `infrastructure/web/error/ErrorResponse.java`), so the error contract itself is
  documented, not just the happy path.

- [x] `springdoc.api-docs.enabled: false` / `springdoc.swagger-ui.enabled: false` in base and `prod` → 404 outside `dev`
  Isolation proof, run against a real PostgreSQL container on port 8081:

  | Profile | `swagger-ui.html` | `api-docs` |
  |---------|--------------------|------------|
  | `dev` | `302` → `200` (follows to `/swagger-ui/index.html`) | `200` |
  | `prod` | `404` | `404` |
  | *(none)* | `404` | `404` |

  ```
  $ curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/swagger-ui.html   # dev, no -L
  302
  $ curl -s -o /dev/null -w "%{http_code}" -L http://localhost:8081/swagger-ui.html  # dev, follows redirect
  200
  $ curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api-docs           # dev
  200
  $ curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/swagger-ui.html   # prod
  404
  $ curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api-docs           # prod
  404
  $ curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/swagger-ui.html   # no profile
  404
  $ curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api-docs           # no profile
  404
  ```
  The `302` on `dev`'s bare `swagger-ui.html` (rather than a literal `200`) is
  springdoc's own redirect-to-`index.html` behaviour, not a profile leak — the
  base and `prod` profiles return `404` for the exact same URL, which is what
  the isolation rule is actually protecting.

**C3 = 4/4.**

## Deliverable

- [x] `README.md` with the stack, `docker compose up -d`,
  `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`, doc URLs — rewritten,
  see `README.md` at the repository root.
- [x] `.env.example` with placeholders; `.env` in `.gitignore`; no secrets in the history
  ```
  $ git ls-files .env.example .env
  .env.example
  $ git check-ignore -v .env
  .gitignore:44:.env.*	.env          (and the explicit *.env / .env rules above it)
  ```
  Secret sweep (full commands and results below) found no real credential
  anywhere reachable from `refactor/hito4`.
- [x] Bruno/Postman collection in `docs/` — `docs/bruno/unicornt-store/`, folders
  `Auth`, `Products`, `Categories`, `Cart`, `Orders`, `Addresses`; environment
  variables `{{baseUrl}}`/`{{token}}` in `environments/local.bru`; `Auth/Login.bru`
  carries a post-response script that stores the returned token into `token`.
  No real credential in any `.bru` file (only the placeholder demo password
  `password123`, matching the seeded/registered test accounts).
- [x] CI (`./mvnw verify`) green
  ```
  $ ./mvnw -B verify
  [INFO] Tests run: 53, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS
  ```
  `.github/workflows/main.yml` now has a `verify` job that runs `./mvnw -B verify`
  on every push and pull request (JDK 25, Maven cache enabled via
  `actions/setup-java`'s `cache: maven`), gating the pre-existing build-and-push
  and deploy jobs (unchanged, still scoped to pushes on `main`) behind it. The
  stale `.github/modernize/java-upgrade/` directory (untracked leftovers from
  the earlier Java upgrade run) was deleted.

## Secret sweep (T6 task item 5)

```
$ git log -p baseline-hito3..HEAD | grep -inE "password|secret|supabase|apikey"
```
Every hit is one of: a Java/Spring identifier (`PasswordEncoder`,
`UsernamePasswordAuthenticationToken`), a test fixture value
(`$2a$encoded`, `irrelevant`), a schema column definition
(`password VARCHAR(255) NOT NULL`), bean-validation messages, or prose in
commit messages and handoff notes describing the mechanism. No real credential.

```
$ grep -rniE "password|secret" --include=*.yml --include=*.yaml --include=*.xml --include=*.md . \
    --exclude-dir=.git --exclude-dir=target --exclude-dir=actividad_m6_l5 --exclude-dir=demoApiRest --exclude-dir=.mvn
```
Every hit is a `${VAR}` interpolation, a GitHub Actions `${{ secrets.* }}`
reference (the CI/CD credentials for the user's own Docker Hub and deploy
target — outside this refactor's scope, never a literal), prose, or a
task/plan document explaining the convention (`REFACTOR-UNICORNT-HITO4.md`,
`docs/refactor/**` — inputs, not deliverables, per `CONVENTIONS.md` section 1).
No real credential found anywhere reachable from `refactor/hito4`.

The `actividad_m6_l5/` and `demoApiRest/` reference projects, and
`REFACTOR-UNICORNT-HITO4.md.bak`, are untracked working material, confirmed not
staged (`git status --porcelain` shows nothing for them) and gitignored since T0.

## Attribution self-check

```
$ git log --format='%an <%ae>%n%B' baseline-hito3..refactor/hito4 | grep -iE 'claude|anthropic|co-authored|generated with'
(empty)
```
