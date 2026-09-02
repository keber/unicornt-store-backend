# T0 — Foundation

**Runs:** sequentially, in the main clone, directly on `refactor/hito4`. No worktree.
**Covers:** plan stages 0 (bootstrap), 1 (pruning), 2 (package restructuring), and the
shared error contract from stage 5.
**Why it is not parallel:** it rewrites nearly every Java file and moves every package.
**Blocks:** everything. T1–T4 are cut from its final commit.

Read [../CONVENTIONS.md](../CONVENTIONS.md) first.

---

## 1. Project identity and toolchain

- `pom.xml`: `<artifactId>unicornt-store-backend</artifactId>`, matching `<name>` and
  an English `<description>`. Keep `<groupId>com.unicornt.store</groupId>`.
- `<build><finalName>app</finalName>` so the Dockerfile does not depend on the version.
- Keep `<java.version>25</java.version>` and parent `spring-boot-starter-parent:4.0.8`.
  The plan document says Java 21; this repo is already on 25 and stays there.
- Generate the Maven wrapper — it does not exist, and README, CI and the acceptance
  script all call `./mvnw`:

```bash
mvn -N wrapper:wrapper
```

  Commit `mvnw`, `mvnw.cmd` and `.mvn/wrapper/maven-wrapper.properties`.

## 2. Dependencies — this is the only task that may edit `pom.xml`

Add everything the whole refactor needs, now, so `pom.xml` can be frozen afterwards.

**Remove:** `spring-boot-starter-thymeleaf`, `thymeleaf-extras-springsecurity6`,
`spring-boot-starter-jdbc`, `com.mysql:mysql-connector-j`, the `provided`-scoped
`spring-boot-starter-tomcat` (the app is a standalone service now, not a WAR on an
external Tomcat), and `spring-boot-devtools`.

**Keep:** `spring-boot-starter-web`, `spring-boot-starter-data-jpa`,
`spring-boot-starter-security`, `org.postgresql:postgresql` (runtime, pinned to
`42.7.12` with its CVE comment), and the existing test dependencies.

**Add:**
- `spring-boot-starter-validation`
- `io.jsonwebtoken:jjwt-api`, `jjwt-impl` (runtime), `jjwt-jackson` (runtime), `0.12.x`
- `org.springdoc:springdoc-openapi-starter-webmvc-ui`, at a version compatible with
  Boot 4 — check the springdoc compatibility matrix, do not guess
- `org.testcontainers:postgresql` and `org.testcontainers:junit-jupiter` (test scope)
- keep `com.h2database:h2` (test) only if a test still needs it, otherwise drop it

Run `mvn -q -DskipTests compile` right after the dependency edit, before touching
code, so a resolution failure is not mistaken for a compile failure.

## 3. Secret hygiene

- `.gitignore` ignores `.env.*`, which would also swallow `.env.example`. Add the
  negation `!.env.example`.
- Create `.env.example` with the placeholder values listed in plan stage 3, task 2.
  `__CHANGE_ME__` only — never a real credential.
- `git rm --cached .env .env.dev 2>/dev/null || true`. Keep `.env-template` or fold
  it into `.env.example` and delete it.
- `src/main/resources/application.properties` carries a real Supabase host in its
  comment header. Remove those comments; the file itself is replaced in step 6.

## 4. Prune the presentation layer

Delete:
- `src/main/resources/templates/**` (whole tree)
- `src/main/resources/static/**`
- controllers: `AdminProductController`, `AuthController`, `CartController`,
  `CatalogController`, `CheckoutController`, `CustomErrorController`,
  `HomeController`, `CartAdvice`
- `config/CustomAuthSuccessHandler.java`
- DAOs and row mappers: `dao/ProductDAO`, `dao/CategoryDAO`, `dao/ProductTypeDAO`,
  `mapper/ProductRowMapper`, `mapper/CategoryRowMapper`, `mapper/ProductTypeRowMapper`

**Before deleting each controller, extract its business logic into a `@Service`.**
These rules must survive the pruning:

- create / edit / delete a product, with price, stock and category validation
- add / remove / clear cart items, and subtotal recalculation
- confirm checkout: stock check, order creation, inventory decrement (its current
  `throw new RuntimeException(...)` becomes `OutOfStockException` in step 7)
- address registration and management
- user creation with `ROLE_USER` assignment

Any DAO query with no `JpaRepository` equivalent becomes a derived query or a JPQL
`@Query` on the matching repository. No `JdbcTemplate` may remain anywhere.

`SecurityConfig` is reduced to a stub that lets the app start; T2 rewrites it:

```java
@Bean
SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http.csrf(c -> c.disable())
        .authorizeHttpRequests(a -> a.anyRequest().permitAll());
    return http.build();
}
```

Tests that assert on views or form login (`SecurityIntegrationTest`) are deleted or
reduced to what still holds. Do not leave a red suite behind.

## 5. Package restructuring

Target layout under `com.unicornt.store`:

```
StoreApplication.java
domain/
  service/        <- current @Service classes
  exception/      <- created in step 7
infrastructure/
  persistence/entity/      <- current model/ (@Entity, @Table, @Id)
  persistence/repository/  <- current repository/
  web/rest/  web/dto/  web/mapper/  web/error/
  security/
  config/
```

- Move `model/**` to `infrastructure/persistence/entity/`. Renaming `Product` to
  `ProductEntity` and so on is recommended for readability; if you rename, keep
  `@Table(name = "...")` pointing at the unchanged table names.
- Move `repository/**` to `infrastructure/persistence/repository/`.
- Move `service/**` to `domain/service/`.
- `StoreApplication` stays in the root package so component scanning still reaches
  the whole tree.
- `domain` classes may depend on entities and repository interfaces — that is the
  trade-off recorded in the plan. What they may not contain is a
  `jakarta.persistence` annotation, or any import of `org.springframework.web` or
  `jakarta.servlet`.

## 6. Configuration files

Convert `application.properties`, `application-dev.properties` and
`application-prod.properties` into `application.yml`, `application-dev.yml` and
`application-prod.yml`, with the content given in plan stage 3, tasks 3–5. Delete
the `.properties` originals. T1 owns these files from here on; you are only seeding
them so the app starts.

## 7. Shared error contract

Both API workers depend on these types, so they are created here, once:

- `domain/exception/ResourceNotFoundException`, `OutOfStockException`,
  `DuplicateResourceException` — signatures as in plan stage 5.
- `infrastructure/web/error/ErrorResponse` — the record with the nested `FieldError`
  and the `of(...)` factory.
- `infrastructure/web/error/GlobalExceptionHandler` — the single
  `@RestControllerAdvice`, handling the three domain exceptions,
  `MethodArgumentNotValidException`, `ConstraintViolationException`,
  `HttpMessageNotReadableException` (400), `DataIntegrityViolationException` (409),
  `NoResourceFoundException` (404), and a final `Exception` safety net that logs the
  stack trace server-side and returns `INTERNAL_ERROR` with no trace in the body.

Set `spring.mvc.throw-exception-if-no-handler-found: true` in `application.yml`.

`RestAuthEntryPoint` and `RestAccessDeniedHandler` are **not** created here — they
belong to T2, which owns the security stack.

## 8. Attribution settings

Confirm `.claude/settings.json` exists at the repository root with the attribution
block from CONVENTIONS section 2, and that it is committed on `refactor/hito4` so
all four worktrees inherit it.

---

## Definition of Done

```bash
mvn -q -DskipTests compile                                            # green
mvn -q test                                                            # green
mvn spring-boot:run -Dspring-boot.run.profiles=dev                     # starts
grep -rn "@Controller\|ModelAndView\|redirect:\|Thymeleaf" src/main    # empty
grep -rni "mysql" pom.xml src/ docker-compose.yml                      # empty
grep -rn "JdbcTemplate" src/main                                       # empty
find src/main -path "*domain*" -name "*.java" | xargs grep -l "jakarta.persistence"   # empty
git status --porcelain                                                 # clean
```

Plus: the `baseline-hito3` tag exists, `.env.example` is versioned, no real `.env`
is in the index, and `pom.xml` holds every dependency T1–T6 will need.

Suggested commit sequence:

```
build: rename project to unicornt-store-backend and add the Maven wrapper
build: replace view and MySQL dependencies with the REST stack
chore: stop tracking environment files and add .env.example
refactor: remove the Thymeleaf presentation layer and JdbcTemplate DAOs
refactor: split packages into domain and infrastructure
feat(error): add ErrorResponse, domain exceptions and the global handler
```
