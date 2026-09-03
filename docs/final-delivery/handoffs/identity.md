# Handoff — P2b identity

**Repos / branches:** `unicornt-store-backend @ fd/identity` · `unicornt-store-frontend @ fd/identity`
**Base commit(s):** backend `3b809de` (tip of `final-delivery` after P1) · frontend `7c9a693`
**Status:** complete (one orchestrator action required: regenerate the ArchUnit baseline — see below)

## What landed

### Backend (`fd/identity`, 4 commits)

- `domain/model/User` — plain-Java aggregate. Invariants in the constructor: first/last
  name required and `<= 100`, email required and `<= 150`, non-blank stored password
  **hash** (never a raw password), at least one non-blank role, roles exposed as an
  unmodifiable `Set<String>`. `User.register(...)` factory (id `null`), `hasRole`,
  `ROLE_USER` / `ROLE_ADMIN` constants. `equals`/`hashCode` on `id` + `email`.
  The raw-password length rule (`>= 6`) is **not** here — the model only sees the hash.
- `domain/repository/UserRepository` — pure port: `findByEmail`, `existsByEmail`, `save`.
- `domain/repository/RoleRepository` — pure port: `existsByName` (default-role guard).
- `application/usecase/identity/RegisterUserUseCase` — `@Service` + `@Transactional`
  (same "guide layout allows Spring-wired application" stance as P1's catalog use
  cases). validate input → raw-password `>= 6` → `existsByEmail` → `roleRepository.existsByName(ROLE_USER)`
  → `PasswordHasher.hash` → `User.register(..., {ROLE_USER})` → `save`.
- `application/usecase/identity/GetUserByEmailUseCase` — resolves the `/me` principal,
  `ResourceNotFoundException` when absent.
- `application/usecase/identity/PasswordHasher` — port; `hash(raw) -> hash`.
- `infrastructure/security/BcryptPasswordHasher` — `@Component` `PasswordHasher`
  backed by the existing `PasswordEncoder` (BCrypt) bean.
- `infrastructure/persistence/mapper/UserPersistenceMapper` — `UserEntity <-> User`
  (`toDomain`, `copyScalars`).
- `infrastructure/persistence/adapter/UserRepositoryAdapter` — `@Component` implements
  the `UserRepository` port; delegates to the Spring Data repo and resolves role
  names to persisted `RoleEntity` rows at the boundary (unknown role name ->
  `ResourceNotFoundException`).
- `infrastructure/persistence/adapter/RoleRepositoryAdapter` — `@Component` implements
  the `RoleRepository` port.
- `infrastructure/web/rest/AuthRestController` — now thin over the use cases
  (`RegisterUserUseCase`, `GetUserByEmailUseCase`, `AuthenticationManager`,
  `JwtService`). `register` 201 + `Location: /api/v1/auth/me`; `login` 200
  `{token, expiresIn}`; `me` 200/401. `@Tag`/`@Operation`/`@ApiResponses` kept.
- `infrastructure/web/mapper/AuthRestMapper` — `User -> MeResponse` (roles sorted).
- `infrastructure/security/CustomUserDetailsService` — now consumes the
  `UserRepository` **domain port** + `User` model instead of the JPA entity.
- `infrastructure/security/SecurityConfig` — auth matchers tightened (list below);
  CORS wiring (`http.cors(Customizer.withDefaults())` + P0 `CorsConfig`) left intact.
- **Deleted** `domain/service/UserService` + `UserServiceImpl` (replaced by the model +
  use cases). This is the architecture-backlog drop.
- `AuthDtos` unchanged (field names already match CONTRACT.md).
- Tests: new `UserTest`, `RegisterUserUseCaseTest` (migrated every `UserServiceTest`
  scenario), `GetUserByEmailUseCaseTest`, `UserPersistenceMapperTest`,
  `UserRepositoryAdapterTest`, `RoleRepositoryAdapterTest`, `BcryptPasswordHasherTest`,
  `AuthRestControllerTest`. Migrated `CustomUserDetailsServiceTest` and
  `SecurityChainTest` to the new types (`SecurityChainTest` still imports both
  `SecurityConfig` and `infrastructure.config.CorsConfig`). `JwtServiceTest` /
  `JwtAuthFilterTest` unchanged (no `User` coupling). `GlobalExceptionHandlerTest`
  untouched — its auth case (`AuthenticationException -> 401`) is P0-owned and my
  changes do not touch it. Deleted `UserServiceTest`.

### Frontend (`fd/identity`, 2 commits)

- `src/storage/auth.storage.ts` — the only module that touches `localStorage` for
  auth. Key **`unicornt.auth.token`**. `readAuthToken` / `writeAuthToken` /
  `clearAuthToken`, with a memory fallback when `localStorage` is unavailable.
- `src/models/auth.model.ts` — `AuthUser`, `LoginInput`, `RegisterInput`, `isAdmin`,
  `fullName`, `ROLE_ADMIN`.
- `src/models/auth.dto.ts` — `TokenDto` / `AccountDto` + `isTokenDto` / `isAccountDto`
  runtime guards + `toAuthUser(dto)` bridge (model never imports the DTO type).
- `src/api/auth.api.ts` — `loginRequest` / `registerRequest` / `meRequest` over the
  shared `apiFetch`; returns `unknown`.
- `src/services/auth.service.ts` — `signIn`, `signUp`, `signOut`, `isAuthenticated`,
  `fetchCurrentUser`, and the auth hook `onAuthChange`. On import it calls
  `setAuthTokenProvider(() => readAuthToken())` from `src/api/http.ts` — the single
  place that binds token storage to the bearer header. No per-file `Authorization`.
- `src/views/login.view.ts` / `src/views/register.view.ts` — `preventDefault`, safe
  DOM (`textContent`, typed `@/lib/dom` queries), disabled+`aria-busy` button while
  in flight, visible error text on failure (invalid login → "Email or password is
  incorrect."). `register.view` signs the new account in immediately (register
  returns no token).
- Tests: `auth.storage.test.ts`, `auth.dto.test.ts`, `auth.api.test.ts`,
  `auth.service.test.ts` (incl. "valid login → token stored → next protected call
  carries `Authorization: Bearer …`"), `login.view.test.ts`, `register.view.test.ts`.
- `src/api/http.ts` **not modified** — P0 had already added `setAuthTokenProvider`,
  the bearer attach and the central `401` handling; nothing left to extend.

## Verification actually run

| Check | Command | Result |
|-------|---------|--------|
| backend compile | `mvn -q -o -DskipTests compile` | pass |
| backend tests | `mvn -q -o test` | **271 pass, 1 fail** — only `DependencyRulesFrozenTest.packages_are_free_of_cycles` (frozen-baseline text churn, see ArchUnit); 1 pre-existing skip |
| backend tests (baseline regenerated) | `mvn -o test -Darchunit.freeze.refreeze=true` | pass — `Tests run: 272, Failures: 0, Errors: 0, Skipped: 1` |
| backend verify (baseline regenerated) | `mvn -o verify -Darchunit.freeze.refreeze=true` | pass — `BUILD SUCCESS`; jacoco `All coverage checks have been met` |
| frontend build | `npm run build` (`tsc --noEmit && vite build`) | pass |
| frontend tests | `npm test` | pass — 37 files, 207 tests |
| frontend lint | `npm run lint` | pass — 0 errors (`no-explicit-any`, `no-non-null-assertion`, `no-unsafe-assignment` all `error`) |
| slice gate | live backend (`spring-boot:run`, dev profile) + PostgreSQL + `curl` | pass — transcript below |

`node_modules/` was `npm ci`-installed in the frontend worktree (not present in a
fresh worktree); `package.json` / `package-lock.json` unchanged.

### Why `mvn -q test` shows one failure without `-Darchunit.freeze.refreeze=true`

`DependencyRulesFrozenTest.packages_are_free_of_cycles` reports the cycle
`application -> domain -> infrastructure -> application` "violated (2 times)". The
**slice-level cycle count is unchanged (117 -> 117)** — the frozen entry's *enumeration
text* moved because the new, fully compliant hexagonal classes
(`application.usecase.identity.*`, `infrastructure.persistence.adapter.{User,Role}RepositoryAdapter`,
`BcryptPasswordHasher`) joined an already-frozen cycle. This is the exact situation
P1 hit ("the cycle-edge count rose ... Baseline refrozen and committed on
`final-delivery`", commit `c887aea`). Per CONVENTIONS §6 and the phase file a worker
must not touch `src/test/resources/archunit_store/**`; regenerating it locally and
then reverting it, I confirmed `mvn verify` is **fully green** and the net effect is a
**drop of 50 frozen violation lines**. The store is **not** in this diff.

### Gate transcript (live: `spring-boot:run` dev profile → PostgreSQL 16)

```
1. POST /api/v1/auth/register {firstName,lastName,email,password}
   201  {"id":1,"firstName":"Ada","lastName":"Lovelace","email":"ada.<ts>@example.com","roles":["ROLE_USER"]}
2. POST /api/v1/auth/login {email,password}            (valid)
   200  {"token":"eyJhbGciOiJIUzI1NiJ9…","expiresIn":3600000}
3. GET  /api/v1/auth/me    Authorization: Bearer <token>
   200  {"id":1,"firstName":"Ada","lastName":"Lovelace","email":"ada.<ts>@example.com","roles":["ROLE_USER"]}
4. GET  /api/v1/cart       Authorization: Bearer <token>   (a protected endpoint)
   200  {"items":[],"itemCount":0,"total":0}
5. POST /api/v1/auth/login {email,password:"wrong-pass"}   (invalid login)
   401  {"message":"Authentication required","code":"UNAUTHORIZED","status":401,"path":"/api/v1/auth/login","errors":[]}
6. GET  /api/v1/auth/me    (no token)
   401  {"code":"UNAUTHENTICATED","status":401,"path":"/api/v1/auth/me"}
7a.POST /api/v1/products   (no token)
   401  {"code":"UNAUTHENTICATED","status":401}
7b.POST /api/v1/products   Authorization: Bearer <USER token>
   403  {"code":"ACCESS_DENIED","status":403}
8. POST /api/v1/auth/register  (same email again)
   409  {"message":"User already exists with email: ada.<ts>@example.com","code":"RESOURCE_CONFLICT","status":409}
9. POST /api/v1/auth/register  {"firstName":"","lastName":"L","email":"nope","password":"x"}
   400  {"code":"VALIDATION_ERROR","status":400,"errors":[{"field":"password",…},{"field":"email",…},{"field":"firstName",…}]}
```

Invalid-login → visible UI error is covered by `src/views/login.view.test.ts`
("shows an invalid-credentials message on an HTTP error" → error `<p>` shows
"Email or password is incorrect."). No headless browser in this environment, same
limitation P1 noted.

Note: the issued JWT `roles` claim contains an extra `FACTOR_PASSWORD` authority
next to `ROLE_USER`. That comes from Spring Security's `DaoAuthenticationProvider`
(framework version behaviour) flowing through the pre-existing
`authentication.getAuthorities()` mapping in `login`; it carries no `ROLE_` prefix,
does not affect authorization, and is not introduced by this slice. Flagging only.

## Final `SecurityConfig` request-matcher list (authoritative)

Order as declared in `chain(...)`:

| # | Matcher | Rule |
|---|---------|------|
| 1 | `GET  /api/v1/auth/me` | `authenticated()` |
| 2 | `POST /api/v1/auth/**` | `permitAll()` — register + login |
| 3 | `/swagger-ui/**`, `/swagger-ui.html`, `/api-docs/**`, `/v3/api-docs/**` | `permitAll()` |
| 4 | `GET  /api/v1/products/**`, `GET /api/v1/categories/**` | `permitAll()` |
| 5 | `POST/PUT/PATCH/DELETE /api/v1/products/**`, `…/categories/**` | `hasRole('ADMIN')` |
| 6 | `anyRequest()` | `authenticated()` — cart / orders / addresses refine with `@PreAuthorize` |

Changes vs. the P1 tip: `/api/v1/auth/**` is no longer a blanket `permitAll`; only
`POST /api/v1/auth/**` is public (matches the phase file), so any future non-POST
`/api/v1/auth/*` endpoint is authenticated by default. Everything else is identical.
`method security` (`@EnableMethodSecurity`), the JWT filter, entry point and
access-denied handler wiring are unchanged.

## ArchUnit

Measured by regenerating the store locally (`-Darchunit.freeze.refreeze=true`) then
reverting it. Counts are non-blank lines per rule file.

| Rule (Group B — frozen) | Before (P1 tip) | After P2b |
|-------------------------|----------------:|----------:|
| `domain -> org.springframework..` | 43 | **36** (−7) |
| `domain -> ..infrastructure..` | 129 | **111** (−18) |
| `LayeredArchitecture (web→app→domain; persistence→app; domain depends on no layer)` | 168 | **143** (−25) |
| `no package cycles between the top-level slices` | 117 | 117 (count unchanged; enumeration text shifted — needs refreeze) |
| `domain -> jakarta.persistence..` / `jakarta.validation..` / `..application..` | 0 | 0 |
| `application -> ..infrastructure..` / `org.springframework.data..` | 0 | 0 |
| **Total frozen violation lines** | **457** | **407** (−50) |

Group C (`TargetArchitectureRulesTest`, active catalog-scoped rules): all pass with
the new classes —
`catalog_repository_ports_are_interfaces_in_domain` (the new `UserRepository` /
`RoleRepository` ports are interfaces under `domain.repository`),
`persistence_adapters_implement_a_domain_port` (the two new adapters do),
`use_cases_do_not_depend_on_jpa_entities` (the identity use cases do not). No Group C
rule was enabled or disabled by this slice; the `..identity..`-scoped rules are not
yet in the file — enable them alongside the refreeze if desired (they already pass).

**Orchestrator action required:** regenerate `src/test/resources/archunit_store/**`
after integrating this branch (same as the P1 `c887aea` refreeze). Nothing else in
this slice fails.

## Requests for the orchestrator

| File / area | Change needed | Why |
|-------------|---------------|-----|
| `src/test/resources/archunit_store/**` (P0) | Refreeze the baseline after merge (`-Darchunit.freeze.refreeze=true`, then commit). Expected: −50 lines total, `packages_are_free_of_cycles` re-recorded. | Worker may not touch the store; the new hexagonal classes shift the frozen cycle enumeration. |
| `infrastructure/persistence/entity/UserEntity`, `RoleEntity`; `infrastructure/persistence/repository/UserRepository`, `RoleRepository` (rename to `*JpaEntity` / `SpringData*Repository`) | **Deferred, not done.** The phase file asks for this rename. It is blocked for a worker: `CartServiceImpl`, `CheckoutServiceImpl`, `AddressServiceImpl` (P2a / P3 territory, P2a runs in parallel) and `StoreApplication` still reference all four types, and renaming them mutates frozen ArchUnit violation text that only the orchestrator can refreeze. Recommend the orchestrator do the mechanical rename + refreeze at integration, or fold it into P3 when the last `domain.service.*` consumer goes. My new code is written so the swap is local: `UserRepositoryAdapter` / `RoleRepositoryAdapter` reference the Spring Data repos by **fully-qualified name** (to dodge the clash with the new `domain.repository.*` ports) — after the rename, replace those two FQNs with imports of `SpringDataUserRepository` / `SpringDataRoleRepository`. `UserPersistenceMapper` and the adapter/mapper tests reference `UserEntity` / `RoleEntity` by simple name and would follow the rename mechanically. | Phase file item 4 vs. CONVENTIONS §6 + parallel-slice ownership. |
| `GlobalExceptionHandler` (P0) | No change needed. `DuplicateResourceException → 409`, `IllegalArgumentException → 400`, `MethodArgumentNotValidException → 400 VALIDATION_ERROR`, `AuthenticationException → 401` all already cover the auth flows (verified live). | — |
| frontend router / entrypoint | **Wiring request.** There is no `src/main.ts`/router — the app is multi-page (`index.html`, `product.html` via `vite.config.ts` `rollupOptions.input`). To surface sign-in/up: add a `login.html` (+ `register.html` or a single page) with the markup documented in `login.view.ts` / `register.view.ts` JSDoc, a `src/pages/auth.main.ts` entry that calls `initLoginView({ onSuccess: () => { location.href = "/"; } })` / `initRegisterView(...)`, and a `login` input in `vite.config.ts` `rollupOptions.input`. `vite.config.ts` is build config I treated as out of scope. Importing `@/services/auth.service` anywhere (the entry does) auto-registers the bearer-token provider. | `src/main.ts` / router / `vite.config.ts` are not P2b's. |
| P2a (cart) | The auth-success hook is **`onAuthChange(listener) => unsubscribe`** from `src/services/auth.service.ts`; it emits `{ type: "login" | "logout" }`. Subscribe to `type === "login"` to trigger the cart merge (`POST /api/v1/cart/merge`). | P2b phase file asks to name the hook here. |

## Decisions taken

- **`RegisterRequest` keeps `firstName` / `lastName` required** (`@NotBlank`, existing
  DTO). CONTRACT.md lists them `firstName?, lastName?`, but the phase file only fixes
  the endpoint + status, every migrated test and the domain model treat them as
  required, and relaxing them would weaken the `User` invariant with no caller
  needing it. Field **names** follow CONTRACT.md exactly. Flag if the orchestrator
  wants them optional.
- **Two identity use cases** (`RegisterUserUseCase`, `GetUserByEmailUseCase`) rather
  than one `IdentityService` — matches the P1 one-class-per-verb shape. Login stays
  in the controller over `AuthenticationManager` (framework concern, per the phase
  file).
- **`PasswordHasher` port in `application`** (not `domain`): it is a hexagonal port
  the use case owns; `application` is already allowed to be Spring-wired here. The
  infra adapter lives in `infrastructure/security/`.
- **`User` requires `>= 1` role.** The old `CustomUserDetailsService` had a
  "no roles → no authorities" path; that test was dropped and the empty-roles
  rejection is covered in `UserTest`. A real account always has `ROLE_USER`.
- **`RoleRepository` port kept** (only `existsByName`) so the "default role not
  seeded" rule (`ResourceNotFoundException`) stays a first-class, Mockito-testable
  path in the use case, as in the legacy `UserServiceImpl`.
- **Token storage key `unicornt.auth.token`**; storage degrades to an in-memory
  value if `localStorage` throws.
- **Frontend `register` auto-signs-in** after a successful create, because the
  backend `register` returns the account with no token.

## Known gaps

- **ArchUnit baseline not refrozen** (worker may not). `mvn test` shows one expected
  failure until the orchestrator regenerates the store; `mvn verify` is green once it
  does. Quantified above (−50 lines).
- **`UserEntity` / `RoleEntity` / Spring Data repo rename not done** — deferred to the
  orchestrator / P3 (see requests). Functionally invisible; the new layer maps to the
  current names.
- **No login/register HTML page or route** — the views, service and tests exist and
  pass; page + `vite.config.ts` input + entry wiring is an orchestrator request.
- **No live browser render** of the auth views (no headless browser here); contract
  and behaviour proven by the live `curl` gate + the Vitest suite.
- `FACTOR_PASSWORD` authority in the JWT `roles` claim (framework behaviour, pre-
  existing) — harmless, noted for awareness.

## Attribution check

```
git log --format='%an <%ae>%n%B' 3b809de..HEAD | grep -iE 'claude|anthropic|co-authored|generated with'   # backend
git log --format='%an <%ae>%n%B' 7c9a693..HEAD | grep -iE 'claude|anthropic|co-authored|generated with'   # frontend
```

Result — backend: empty · frontend: empty.
