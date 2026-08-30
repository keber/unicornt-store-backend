# T2 — Stateless JWT security and CORS

**Worktree:** `../unicornt-worktrees/security-jwt`
**Branch:** `refactor/h4-security-jwt`, cut from `refactor/hito4` after T0
**Covers:** plan stage 6
**Rubric:** consolidates C1 (401/403 as JSON), enables C3 authentication in Swagger
**Runs in parallel with:** T1, T3, T4

Read [../CONVENTIONS.md](../CONVENTIONS.md) first. You own the security package, the
two authentication error handlers, and the auth endpoint. You never edit `pom.xml`,
`application*.yml` or another worker's controllers.

**Reference implementation:** `actividad_m6_l5/productos-thymeleaf/src/main/java/com/example/app/security/`
in the main clone — `JwtTokenService`, `JwtUtil`, `JwtAuthFilter`, plus
`controller/AuthRestController` and `dto/LoginRequest|LoginResponse`. That directory
is untracked, so it does not exist inside your worktree: read it by absolute path
from `C:/Users/Usuario/Proyectos/unicornt-store-backend/`.

---

## Scope

Token authentication with no server session, authentication and authorization
failures rendered as `ErrorResponse` JSON, and CORS opened for the separate frontend
repository.

## Files you create

```
infrastructure/security/JwtService.java
infrastructure/security/JwtAuthFilter.java
infrastructure/security/SecurityConfig.java          (replaces the T0 stub)
infrastructure/security/CorsConfig.java              (or the bean inside SecurityConfig)
infrastructure/web/error/RestAuthEntryPoint.java
infrastructure/web/error/RestAccessDeniedHandler.java
infrastructure/web/rest/AuthRestController.java
infrastructure/web/dto/AuthDtos.java                 (RegisterRequest, LoginRequest, TokenResponse, MeResponse)
```

## Tasks

1. **`JwtService`** — as in plan stage 6. Key built with
   `Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))` from `${app.jwt.secret}`,
   expiry from `${app.jwt.expiration-ms}`. Both keys already exist in
   `application.yml`; read them, do not add new ones. The secret is **never**
   defaulted to a literal in code — a missing `APP_JWT_SECRET` must fail startup.

2. **`JwtAuthFilter`** — `OncePerRequestFilter`, reads the `Authorization: Bearer`
   header, parses the token, builds a `UsernamePasswordAuthenticationToken` with the
   `roles` claim mapped to `SimpleGrantedAuthority`. A malformed or expired token is
   swallowed silently here; the 401 is emitted by the entry point. Adapt the l5
   version to this project's `User` and `Role` entities under
   `infrastructure.persistence.entity`.

3. **`SecurityConfig`** — the full chain from plan stage 6:
   `cors(withDefaults())`, `csrf` disabled, `SessionCreationPolicy.STATELESS`,
   `@EnableMethodSecurity`, the `BCryptPasswordEncoder` bean, the exception handling
   wired to your entry point and denied handler, and the JWT filter added before
   `UsernamePasswordAuthenticationFilter`.

   Request matchers:
   - `permitAll`: `/api/v1/auth/**`, `GET /api/v1/products/**`,
     `GET /api/v1/categories/**`, `/swagger-ui/**`, `/swagger-ui.html`,
     `/api-docs/**`, `/v3/api-docs/**`
   - `hasRole("ADMIN")`: `POST|PUT|DELETE` on `/api/v1/products/**` and
     `/api/v1/categories/**`
   - everything else `authenticated()`

   **You own this file.** T3 and T4 will request additional matchers in their handoff
   notes; the orchestrator folds them in at integration. Do not try to anticipate
   endpoints that do not exist on your branch yet — the two `permitAll` product and
   category rules above are enough, and `anyRequest().authenticated()` already covers
   cart, orders and addresses.

4. **`CorsConfigurationSource`** — origins from `${app.cors.allowed-origins}`,
   methods `GET POST PUT PATCH DELETE OPTIONS`, allowed headers `Authorization` and
   `Content-Type`, exposed header `Location` (the 201 responses set it).

5. **`RestAuthEntryPoint` / `RestAccessDeniedHandler`** — write an `ErrorResponse`
   as JSON with codes `UNAUTHENTICATED` (401) and `ACCESS_DENIED` (403), reusing the
   `ErrorResponse` type T0 created. Serialize with the injected `ObjectMapper`; do
   not hand-build JSON strings.

6. **`AuthRestController`** at `/api/v1/auth`:
   - `POST /register` → `201`, creates the user with `passwordEncoder.encode` and
     assigns `ROLE_USER`. A duplicate username or email raises
     `DuplicateResourceException` (T0 created it) → `409`.
   - `POST /login` → `200 {token, expiresIn}`; bad credentials → `401` JSON.
   - `GET /me` → `200`, resolved from the `SecurityContext`; no token → `401`.

7. **`UserDetailsService`** — replace any `InMemoryUserDetailsManager` with a real
   implementation over `UserRepository`. `CustomUserDetailsService` already exists in
   `domain/service`; adapt it rather than writing a second one.

8. **OpenAPI annotations on your own endpoints** — `@Tag(name = "Authentication")`,
   `@Operation`, `@ApiResponses`, and `@Schema` on your DTO record fields. T5 adds
   the global `OpenApiConfig`; do not create it here.

9. **Tests** — at minimum:
   - login with valid credentials returns a token
   - a protected endpoint with no token returns 401 with an `ErrorResponse` body
   - a protected ADMIN endpoint with a USER token returns 403 with an `ErrorResponse` body

## Definition of Done

```bash
mvn -q -DskipTests compile     # green
mvn -q test                    # green
```

Behavioural, against a running instance:

- `POST /api/v1/auth/login` with valid credentials → `200` with a token
- `POST /api/v1/products` with no token → `401`, `Content-Type: application/json`, `code: UNAUTHENTICATED`
- the same with a `USER` token → `403`, `code: ACCESS_DENIED`
- the same with an `ADMIN` token → `201`
- an `OPTIONS` preflight from the configured frontend origin returns the CORS headers

No secret literal anywhere:
`grep -rn "hmacShaKeyFor\|jwt.*secret" src/main --include=*.java` must show only
reads of `${app.jwt.secret}`.

## Handoff note

Write `docs/refactor/handoffs/security-jwt.md` from the template. Record the exact
request-matcher list you configured, the claim names in the token, the role naming
convention (`ROLE_ADMIN` vs `hasRole("ADMIN")`), and how T3/T4 should express their
own authorization.
