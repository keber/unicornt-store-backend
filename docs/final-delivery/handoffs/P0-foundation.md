# Handoff — P0 foundation

**Repos / branches:** `unicornt-store-backend @ final-delivery` · `unicornt-store-frontend @ final-delivery`
**Base commit(s):** backend `1ee70bd` · frontend `f9f15e2` (both tagged `baseline-final-delivery`)
**Status:** complete

## What landed

- backend: `.gitignore` cleaned to Java/Maven/OS/IDE/secrets scope; verified no tracked
  `.env` / secret (only `.env.example`, all `${...}` / `__CHANGE_ME__` placeholders).
- backend: `docs/final-delivery/CONTRACT.md` — endpoint list, error shape + codes,
  vocabulary table, ownership table, architecture backlog counts.
- backend: ArchUnit `archunit-junit5` dependency added to `pom.xml`;
  `src/test/java/com/unicornt/store/architecture/` with `LayeredArchitectureRulesTest`
  (Group A, hard), `DependencyRulesFrozenTest` (Group B, frozen), `TargetArchitectureRulesTest`
  (Group C, `@ArchIgnore`). Frozen baseline committed under `src/test/resources/archunit_store/`.
- backend: `infrastructure/config/CorsConfig.java` — global `CorsConfigurationSource`
  for `http://localhost:5173` (env `APP_CORS_ALLOWED_ORIGINS`), moved out of `SecurityConfig`;
  `SecurityChainTest` extended with a `GET /api/v1/products` preflight assertion and an
  unknown-origin rejection assertion.
- backend: `StoreApplication.generatePassword()` fixed — `new SecureRandom(1000)` did not
  compile (`SecureRandom(int)` does not exist) and seeding a SecureRandom is an anti-pattern;
  now `new SecureRandom()`.
- frontend: `src/api/http.ts` — shared `apiFetch(path, options)` (base URL from
  `VITE_API_BASE_URL`, JSON headers, pluggable bearer-token provider, 401/`ApiError`
  translation, `204 -> null`).
- frontend: `src/api/product.api.ts` now calls `apiFetch('/api/v1/products')`; the
  `public/data/products.json` mock is demoted to a Vitest fixture only.
- frontend: `src/vite-env.d.ts` types `VITE_API_BASE_URL`; `.env.example` documents it.

## Verification actually run

| Check | Command | Result |
|-------|---------|--------|
| backend compile | `mvn -q -o -DskipTests compile` | pass |
| backend tests | `mvn -q -o test` (full suite) | pass |
| backend arch | `LayeredArchitectureRulesTest`, `DependencyRulesFrozenTest`, `TargetArchitectureRulesTest` | pass (Group B frozen, Group C ignored) |
| frontend build | `npm run build` | pass |
| frontend tests | `npm test` | pass — 29 files, 160 tests |
| frontend lint | `npm run lint` | pass |
| CORS preflight | `SecurityChainTest#getProductsPreflightFromTheViteOriginIsAllowed` | pass |
| read E2E | docker `db` up + backend `dev` + `curl` — see P0 report | see report |

## ArchUnit — frozen baseline (P0)

CORS origin key: `app.cors.allowed-origins` / env `APP_CORS_ALLOWED_ORIGINS`.

| Rule (Group B, frozen) | Frozen count |
|------------------------|-------------:|
| domain -X-> org.springframework.. | 73 |
| domain -X-> jakarta.persistence.. | 0 |
| domain -X-> jakarta.validation.. | 0 |
| domain -X-> ..infrastructure.. | 225 |
| domain -X-> ..application.. | 0 |
| application -X-> ..infrastructure.. | 0 |
| application -X-> org.springframework.data.. | 0 |
| no package cycles between top-level slices | 47 |
| LayeredArchitecture | 299 |
| **total lines** | **644** |

Group C (`@ArchIgnore`, reason `FINAL-DELIVERY: pending slice ...`), enable per slice:
`repository_interfaces_live_in_domain`, `persistence_adapters_implement_a_domain_port`,
`use_cases_do_not_depend_on_jpa_entities`.

## Frozen build files — do not re-add

- `pom.xml`: `archunit-junit5 ${archunit.version}` (test) is the only P0 addition;
  everything else P1–P6 needs (JUnit 5, Mockito, AssertJ, spring-security-test,
  spring-boot-starter-webmvc-test, testcontainers postgres + junit-jupiter, H2) is
  already present. **`pom.xml` is frozen.**
- frontend `package.json`: Vitest + `@vitest/coverage-v8` + eslint + typescript-eslint +
  prettier already wired (`build`, `test`, `test:coverage`, `lint`, `format`).
  **`package.json` is frozen.**

## Requests for the orchestrator

None — P0 is orchestrator-owned.

## Decisions taken

- Group C rules use ArchUnit's `@ArchIgnore(reason = "FINAL-DELIVERY: pending slice ...")`
  instead of JUnit `@Disabled`: the `@ArchTest` engine silently ignores `@Disabled` on
  its fields/methods (verified — it ran the rules anyway). Same effect: present, not enforced.
- `archunit.properties` uses the default `ViolationLineMatcher` (line numbers ignored on
  match); the `freeze.lineMatcher` shortcut string is not a real class and broke discovery.
- CORS bean does not set `allowCredentials` — auth is a bearer header, not a cookie.
- Frontend `product.api.ts` rewired in P0 (nominally P1's file) because P0.6 mandates the
  read-cycle switch; P1 (same author, next) does the full DTO/model reconciliation + English
  test rewrite. No conflict since P1 has no worktree.

## Known gaps

- `GET /api/v1/products` still returns a Spring `Page` (`{content:[...]}`), not a bare
  array; the frontend `isProductDtoArray` guard expects an array. Full render is completed
  in P1 (`PageResult` + DTO reconciliation). P0 proves transport + CORS + DB reachability.

## Attribution check

```
git log --format='%an <%ae>%n%B' baseline-final-delivery..HEAD | grep -iE 'claude|anthropic|co-authored|generated with'
```

Result (backend): empty. Result (frontend): empty.
