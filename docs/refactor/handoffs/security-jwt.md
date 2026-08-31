# Handoff — T2 security-jwt

**Branch:** `refactor/h4-security-jwt`
**Base commit:** `2dd80ae` (tip of `refactor/hito4` at the time this branch was cut)
**Status:** complete

## What landed

- `infrastructure/security/JwtService.java` — issues and verifies HMAC-signed tokens.
  Key built with `Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))` from
  `${app.jwt.secret}`; expiry from `${app.jwt.expiration-ms}`. No default literal —
  a missing `APP_JWT_SECRET` fails Spring property resolution at startup. Token
  claims: `sub` = account email, `roles` = list of authority names already prefixed
  with `ROLE_` (e.g. `ROLE_ADMIN`), plus standard `iat`/`exp`.
- `infrastructure/security/JwtAuthFilter.java` — `OncePerRequestFilter` reading
  `Authorization: Bearer <token>`, populating `UsernamePasswordAuthenticationToken`
  with `SimpleGrantedAuthority` built straight from the `roles` claim. Malformed or
  expired tokens are swallowed (request falls through as anonymous); the 401 is
  emitted downstream by `RestAuthEntryPoint`.
- `infrastructure/security/SecurityConfig.java` (replaces the T0 stub) — stateless
  chain: `cors(withDefaults())`, CSRF/formLogin/httpBasic/logout disabled,
  `SessionCreationPolicy.STATELESS`, `@EnableMethodSecurity`, `BCryptPasswordEncoder`
  bean, an explicit `AuthenticationManager` (`DaoAuthenticationProvider` over the
  existing `CustomUserDetailsService`), exception handling wired to
  `RestAuthEntryPoint`/`RestAccessDeniedHandler`, `JwtAuthFilter` added before
  `UsernamePasswordAuthenticationFilter`. CORS bean reads
  `${app.cors.allowed-origins}` (comma-separated list already in `application.yml`,
  owned by T1), methods `GET POST PUT PATCH DELETE OPTIONS`, allowed headers
  `Authorization`/`Content-Type`, exposed header `Location`.
- `infrastructure/web/error/RestAuthEntryPoint.java` / `RestAccessDeniedHandler.java`
  — write the shared `ErrorResponse` (from T0) as JSON via the injected Jackson
  `ObjectMapper` (`tools.jackson.databind.ObjectMapper` — Spring Boot 4.0.8 ships
  Jackson 3 as the primary `ObjectMapper` bean type; `com.fasterxml.jackson...`
  classes are still on the classpath transitively but not what Spring autowires).
  Codes: `UNAUTHENTICATED` (401), `ACCESS_DENIED` (403).
- `infrastructure/web/dto/AuthDtos.java` — `RegisterRequest`, `LoginRequest`,
  `TokenResponse`, `MeResponse`, all `@Schema`-annotated records with bean
  validation on the request records.
- `infrastructure/web/rest/AuthRestController.java` at `/api/v1/auth`:
  `POST /register` returns `201` (delegates to `UserService.register`, which already
  assigns `ROLE_USER` and hashes the password; a duplicate email surfaces as
  `DuplicateResourceException` -> T0's handler turns it into `409`);
  `POST /login` returns `200 {token, expiresIn}` via `AuthenticationManager.authenticate`,
  bad credentials bubble up as `AuthenticationException` -> `401` JSON (T0's
  `GlobalExceptionHandler.unauthenticated` handles this branch, since it is raised
  inside the dispatcher, not the filter chain);
  `GET /me` returns `200` from `SecurityContext`, throws
  `InsufficientAuthenticationException` when anonymous -> same 401 path.
  Each endpoint carries `@Operation`/`@ApiResponses`, controller carries
  `@Tag(name = "Authentication")`.
- `infrastructure/security/CustomUserDetailsService.java` — reused as-is (T0's
  existing implementation over `UserRepository` already matched the task), only
  touched the not-found message to drop the leaking `UserEntity` suffix.
- Tests: `SecurityChainTest` (`@WebMvcTest` slice with `@ImportAutoConfiguration`
  of Spring Security's servlet auto-configuration classes, since `@WebMvcTest`
  alone does not pull in `ServletWebSecurityAutoConfiguration` in this Boot 4.0.8
  setup) covering login success/failure, `/api/v1/products` POST with no token
  (401 + `UNAUTHENTICATED`), with a USER token (403 + `ACCESS_DENIED`), with an
  ADMIN token (201, against a throwaway stub controller mounted only in the test
  context, since T3 owns the real `ProductRestController` and it does not exist on
  this branch), `GET /me` with/without token, `register`, an expired/garbage
  bearer token, and a CORS preflight from the configured origin. `JwtServiceTest`
  is a plain unit test of token issuing/parsing: correct claims, rejection of a
  token signed by a different key, rejection of an expired token.

## Verification actually run

| Check | Command | Result |
|-------|---------|--------|
| compile | `mvn -q -DskipTests compile` | pass |
| tests | `mvn -q test` | pass — Tests run: 22, Failures: 0, Errors: 0, Skipped: 0 (7 UserServiceTest, 4 JwtServiceTest, 11 SecurityChainTest) |
| presentation leftovers | `grep -rn "@Controller\|ModelAndView\|redirect:\|Thymeleaf" src/main` | empty |
| secret literal | `grep -rn "hmacShaKeyFor\|jwt.*secret" src/main --include=*.java` | only `Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))` and the `${app.jwt.secret}` `@Value` read, no literal |
| mysql | `grep -rni "mysql" pom.xml src/` | empty |
| domain purity | `find src/main -path "*domain*" -name "*.java" \| xargs grep -l "jakarta.persistence"` | empty |
| attribution | see below | empty |

Behavioural DoD items (login 200, POST products 401/403/201, CORS preflight) were
exercised through `SecurityChainTest` via MockMvc rather than a booted instance,
per the environment note about port 8080 being occupied by an unrelated container
and the preference for slice tests. The full application was not booted manually.

## Requests for the orchestrator

| File | Change needed | Why |
|------|---------------|-----|
| infrastructure/web/rest/Product*, Category* (T3) | Nothing required from T2; T3's write endpoints are already covered by the ADMIN_CATALOG_PATHS matcher (POST/PUT/PATCH/DELETE on /api/v1/products/** and /api/v1/categories/**) and reads by PUBLIC_CATALOG_PATHS. | Confirms the contract; no action needed unless T3 introduces paths outside /api/v1/products/** or /api/v1/categories/**. |
| infrastructure/web/rest/Cart*, Order*, Address* (T4) | Nothing required; anyRequest().authenticated() already covers these. If T4 needs a role-restricted admin-only order/report endpoint, it should use @PreAuthorize("hasRole('ADMIN')") on the controller method rather than a new path matcher. | Keeps SecurityConfig free of anticipatory rules for endpoints that do not exist on this branch, per the task file's own instruction. |
| docs/refactor/tasks/T3-*, T4-* (informational only) | None; documenting expectations here since T2 owns SecurityConfig exclusively. | See "How T3/T4 should express authorization" in Decisions below. |

## Decisions taken

- **ObjectMapper type for the error handlers** — Spring Boot 4.0.8 autowires
  `tools.jackson.databind.ObjectMapper` (Jackson 3) as the primary bean, not
  `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2, still present
  transitively via jjwt-jackson and other libraries). `RestAuthEntryPoint` and
  `RestAccessDeniedHandler` both depend on `tools.jackson.databind.ObjectMapper`
  to match what the application context actually publishes. The task file's
  snippet used the Jackson 2 package name from the l5 reference project (Spring
  Boot 3 era); adapted for Boot 4.0.8's Jackson 3 default.
- **GET /api/v1/auth/me requires authentication explicitly** — added
  `.requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()` ahead of
  the broader `/api/v1/auth/**` `permitAll()` rule, since Spring Security's
  `authorizeHttpRequests` evaluates matchers in declaration order and the first
  match wins. Without this, `/me` would inherit the blanket permitAll meant for
  register/login.
- **Explicit AuthenticationManager bean** — built a ProviderManager wrapping a
  DaoAuthenticationProvider(userDetailsService) with the PasswordEncoder set
  explicitly, rather than relying on Boot's implicit auto-configured manager, so
  AuthRestController.login gets a manager it can inject directly and bad
  credentials throw a plain AuthenticationException that T0's
  GlobalExceptionHandler.unauthenticated (or, if raised in the filter chain,
  RestAuthEntryPoint) turns into the same 401 JSON shape either way.
- **Role naming convention** — the token's roles claim carries fully prefixed
  authority names (ROLE_USER, ROLE_ADMIN), matching RoleEntity.name and
  UserServiceImpl.DEFAULT_ROLE. SecurityConfig uses hasRole("ADMIN") (Spring
  Security strips/re-adds the ROLE_ prefix internally, so hasRole("ADMIN")
  correctly matches a SimpleGrantedAuthority("ROLE_ADMIN")). Any @PreAuthorize
  T3/T4 write should use the same hasRole('ADMIN') / hasRole('USER') form, not
  hasAuthority('ROLE_ADMIN'), to stay consistent, though both work given the
  claim already carries the ROLE_ prefix.
- **InsufficientAuthenticationException for anonymous /me** — chosen over a
  manual 401 response so the failure flows through the same
  GlobalExceptionHandler.unauthenticated path T0 built for
  AuthenticationException subtypes raised inside the dispatcher, keeping the
  JSON shape identical to the filter-chain 401 emitted by RestAuthEntryPoint.
- **SecurityChainTest stub controller for /api/v1/products** — T3's real
  ProductRestController does not exist on this branch. A minimal
  @TestConfiguration-scoped stub controller (GET returns 200, POST returns 201)
  is mounted only inside the test's @Import, never touching
  infrastructure/web/rest/Product*, to exercise the ADMIN path matcher end to
  end without depending on T3's work landing first.
- **@ImportAutoConfiguration in the security slice test** — @WebMvcTest in this
  Spring Boot 4.0.8 setup does not automatically pull in
  ServletWebSecurityAutoConfiguration/SecurityFilterAutoConfiguration (package
  split from earlier Boot versions); imported them explicitly alongside
  SecurityAutoConfiguration so the real filter chain (not Spring's default
  permit-all-with-basic-auth fallback) is exercised in the slice.

## Known gaps

- No manual/behavioural verification against a running instance (curl against a
  live port) was performed — the environment note steered explicitly toward
  MockMvc/slice tests and away from booting the app on the occupied 8080. All five
  behavioural DoD bullets are instead covered by SecurityChainTest assertions
  (status code, Content-Type, code field, and for the CORS case the
  Access-Control-Allow-Origin/Access-Control-Allow-Methods headers). If the
  orchestrator wants an end-to-end manual check, run with SERVER_PORT=8081 and a
  valid APP_JWT_SECRET/SPRING_DATASOURCE_* set, once T1's persistence stack and
  T3/T4's endpoints are merged in.
- SecurityConfig's ADMIN_CATALOG_PATHS and PUBLIC_CATALOG_PATHS matchers only
  cover /api/v1/products/** and /api/v1/categories/**, exactly as scoped by the
  task file. T3/T4 controllers not yet present on refactor/hito4 were
  deliberately not anticipated.

## Attribution check

```
git log --format='%an <%ae>%n%B' refactor/hito4..HEAD | grep -iE 'claude|anthropic|co-authored|generated with'
```

Result: empty
