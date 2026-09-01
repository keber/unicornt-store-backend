# T5 — OpenAPI and profile isolation

**Runs:** after T1–T4 are merged into `refactor/hito4`. Sequential — it verifies the
assembled system, so it cannot run in parallel.
**Branch:** directly on `refactor/hito4`, or a short-lived `refactor/h4-openapi-profiles`
**Covers:** plan stage 7
**Rubric:** closes **C3 = 4/4**

Read [../CONVENTIONS.md](../CONVENTIONS.md) first.

---

## Scope

T2, T3 and T4 already annotated their own controllers and DTOs. What is left is the
global configuration, the gaps they left, and the profile isolation proof.

## Tasks

1. **`infrastructure/config/OpenApiConfig.java`** — as in plan stage 7:
   `@SecurityScheme(name = "bearerAuth", type = HTTP, scheme = "bearer", bearerFormat = "JWT")`
   on the class, and an `OpenAPI` bean with the title `Unicornt Store API`, version
   `v1`, an English description, a contact, and
   `.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))` so the
   Authorize button appears.

2. **Annotation sweep.** Walk every `@RestController` and every DTO record on the
   merged branch and fill in what is missing:
   - class-level `@Tag(name, description)`
   - method-level `@Operation(summary)`
   - `@ApiResponses` covering the real status codes, with every error branch pointing
     at `@Schema(implementation = ErrorResponse.class)`
   - `@Schema(example = ...)` on every DTO record field, **including `ErrorResponse`
     and its nested `FieldError`** — the rubric checks the error contract is
     documented too

   Find the gaps mechanically:

   ```bash
   grep -Ln "@Tag" src/main/java/com/unicornt/store/infrastructure/web/rest/*.java
   grep -Ln "@Schema" src/main/java/com/unicornt/store/infrastructure/web/dto/*.java
   ```

3. **Confirm the profile matrix** in the YAML files T1 owns:

   | Property | base | `dev` | `prod` |
   |----------|------|-------|--------|
   | `springdoc.api-docs.enabled` | `false` | `true` | `false` |
   | `springdoc.swagger-ui.enabled` | `false` | `true` | `false` |
   | `springdoc.api-docs.path` | — | `/api-docs` | — |
   | `springdoc.swagger-ui.path` | — | `/swagger-ui.html` | — |

   Base is `false` so that starting with no profile at all already leaves the
   documentation surface closed. `dev` is the only profile that opens it.

4. **Confirm the springdoc routes are `permitAll`** in T2's `SecurityConfig` —
   `/swagger-ui/**`, `/swagger-ui.html`, `/api-docs/**`, `/v3/api-docs/**`. Without
   this, Swagger returns 401 in `dev` instead of rendering.

## Definition of Done

Isolation proof — run both and record the actual output:

```bash
# dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev &
curl -s -o /dev/null -w "swagger dev: %{http_code}\n" localhost:8080/swagger-ui.html   # 200
curl -s -o /dev/null -w "docs   dev: %{http_code}\n" localhost:8080/api-docs           # 200

# prod
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run &
curl -s -o /dev/null -w "swagger prod: %{http_code}\n" localhost:8080/swagger-ui.html  # 404
curl -s -o /dev/null -w "docs   prod: %{http_code}\n" localhost:8080/api-docs          # 404

# no profile at all — must also be closed
./mvnw spring-boot:run &
curl -s -o /dev/null -w "swagger base: %{http_code}\n" localhost:8080/swagger-ui.html  # 404
```

Manual check in `dev`: Swagger-UI lists every resource group, the **Authorize**
button accepts a JWT, and **Try it out** executes an authenticated call successfully.

Suggested commits:

```
feat(openapi): add the OpenAPI configuration with the bearer security scheme
docs(openapi): annotate the remaining controllers and DTO schemas
```
