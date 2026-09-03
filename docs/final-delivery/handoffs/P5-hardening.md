# Handoff — P5 hardening

**Lane:** orchestrator, sequential, on `final-delivery` in both repos.
**Status:** complete.

## 5.1 ArchUnit — Group B is now hard

- `DependencyRulesFrozenTest` -> `DependencyRulesTest`: every Group B rule converted from
  `FreezingArchRule.freeze(...)` to a plain `@ArchTest` rule.
- `src/test/resources/archunit_store/` and `src/test/resources/archunit.properties`
  **deleted** — the frozen baseline reached 0 lines after P3 removed the last
  `domain/service/**` class.
- **Every Group C rule enabled** (`TargetArchitectureRulesTest`, 4 rules, 0 skipped):
  repository ports are interfaces under `domain.repository`; `persistence.adapter`
  classes implement a `domain.repository` port; `application.usecase` classes are free
  of `*JpaEntity`; and the whole-codebase `every_repository_interface_lives_in_domain`.
- Residual gap closed on the way: renamed the last two P2b-deferred Spring Data repos
  `infrastructure.persistence.repository.{User,Role}Repository` ->
  `SpringData{User,Role}Repository` (updates `CustomUserDetailsService`,
  `{User,Role,Cart,Order}RepositoryAdapter`, `StoreApplication`, tests).
- `mvn -o clean test` green. **No residual architecture violation.**

## 5.2 Coverage scope

`pom.xml` `jacoco:check` rewritten to the CONVENTIONS §7 scoped rules:

| element | includes | INSTRUCTION | BRANCH |
|---------|----------|-------------|--------|
| PACKAGE | `com.unicornt.store.domain`, `com.unicornt.store.domain.*` | 100% | 100% |
| PACKAGE | `com.unicornt.store.application`, `com.unicornt.store.application.*` | 100% | 95% |
| BUNDLE  | (everything) | 85% | 75% |

Plugin `<excludes>` already cover `web/dto/**`, `persistence/entity/**` (the `*JpaEntity`
classes), `web/error/ErrorResponse*`, `infrastructure/config/**`, `StoreApplication`.

**Final numbers (`target/site/jacoco/jacoco.xml`):**

| bundle | INSTRUCTION | BRANCH |
|--------|-----------:|-------:|
| `..domain..` | **100.00%** | **100.00%** |
| `..application..` | **100.00%** | **100.00%** |

`mvn -o clean verify` → `All coverage checks have been met` → `BUILD SUCCESS`, 302 tests.
Added small coverage tests: `UserCoverageTest`, `DomainExceptionsTest`,
`RegisterUserUseCaseCoverageTest`, plus edge cases in `OrderTest` / `OrderItemTest`.

## 5.3 CORS / Swagger / secrets

| Check | Result |
|-------|--------|
| CORS — `GET /api/v1/products` with `Origin: http://localhost:5173` | `Access-Control-Allow-Origin: http://localhost:5173` (live, dev **and** prod profile) |
| Swagger — dev profile | `/v3/api-docs` → **200**, `/swagger-ui/index.html` → 200 (live) |
| Swagger — prod profile | `/v3/api-docs` → **404**, `/swagger-ui/index.html` → **404**, `/api-docs` → **404** (live, with the guard filter) |
| secrets — `git ls-files \| grep -E '(^\|/)\.env($\|\.)'` (both repos) | only `.env.example` |
| secrets — `git log -p baseline-final-delivery..final-delivery \| grep -iE 'password\s*=\s*\S\|secret\s*=\s*\S'` | no real credential (only `${ENV}` placeholders, `__CHANGE_ME__`, test-only keys) |
| `docker compose config -q` | valid |
| `.gitignore` (both repos) | `.env`, `.env.*` ignored, `!.env.example` kept |

**Swagger prod note.** `application.yml` and `application-prod.yml` both set
`springdoc.api-docs.enabled: false` / `springdoc.swagger-ui.enabled: false`, but
springdoc `3.0.3` (the pre-release Boot-4 line) logs `SpringDoc /v3/api-docs endpoint
is enabled by default` and keeps serving them. `infrastructure/config/ProductionApiDocsGuard`
— a `@Profile("prod")` `OncePerRequestFilter` — closes it: it `setStatus(404)` +
writes a small JSON body for every `/v3/api-docs*` / `/swagger-ui*` / `/api-docs*`
path before the dispatcher (`setStatus`, not `sendError`, so the stateless security
chain does not turn it into a 401). Verified live under the `prod` profile:
`/v3/api-docs` → 404, `/swagger-ui/index.html` → 404, `/api-docs` → 404, while the
catalog stays 200 and CORS still returns the allowed origin. Dev Swagger and CORS
are also verified live.

## 5.4 Frontend minimal port extraction

- `src/gateways/product.gateway.ts` — `ProductGateway` interface + `httpProductGateway`
  over `api/product.api`.
- `src/gateways/checkout.gateway.ts` — `CheckoutGateway` interface + `httpCheckoutGateway`
  over `api/order.api`.
- `product.service.ts` / `checkout.service.ts` take an injected gateway
  (`= httpProductGateway` / `= httpCheckoutGateway` default); no direct `api/` import
  in the service body. `src/gateways/gateways.test.ts` drives the services with fakes.
- This is the minimal extraction PLAN §1 asks for; not a full hexagonal frontend.
- `npm run build` / `npm test` (45 files) / `npm run lint` green.

## 5.5 Quality gates

| | Result |
|---|---|
| backend `mvn -o clean verify` | BUILD SUCCESS, 302 tests, jacoco met, ArchUnit hard rules green |
| `docker compose config` | valid |
| frontend `npm ci && npm test && npm run lint && npm run build` | green |

## Residual items

None blocking. `springdoc 3.0.3` ignores its own `enabled=false` flags; the
`ProductionApiDocsGuard` filter is the enforced control and is verified live. A
future springdoc release that honours the flags would make the filter redundant.
