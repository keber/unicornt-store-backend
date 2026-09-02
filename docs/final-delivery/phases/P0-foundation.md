# P0 — Foundation

**Lane:** orchestrator, sequential. No worktree — work on `final-delivery` in both
main clones.
**Plan:** [PLAN.md](../PLAN.md) §4 Phase 0, §3 (ArchUnit), §0.2.
**Rubric:** closes dimension 3 (secrets); enables dimensions 1 and 2.

Read [../CONVENTIONS.md](../CONVENTIONS.md) first. This phase makes the tree safe to
fan out from: after it, `pom.xml` and `package.json` are frozen and the architecture
board (ArchUnit) is live.

---

## 0.1 Branches

```bash
cd unicornt-store-backend  && git checkout dev  && git tag baseline-final-delivery && git checkout -b final-delivery
cd ../unicornt-store-frontend && git checkout dev && git tag baseline-final-delivery && git checkout -b final-delivery
```

Confirm `.claude/settings.json` with the attribution block exists on `final-delivery`
in both repos; if not, copy [../templates/settings.json](../templates/settings.json)
and commit it.

## 0.2 Secrets (dimension 3, ~1h)

- Backend `.gitignore`: `target/`, `*.class`, `.env`, `.env.*` (keep `.env.example`),
  `.idea/`, `*.iml`, `.vscode/`, `.DS_Store`.
- Frontend `.gitignore`: `node_modules/`, `dist/`, `dist-ssr/`, `*.local`, `.env`,
  `.env.*`, `.DS_Store`.
- Remove any tracked `.env` / secret file: `git rm --cached` + move the real values
  out of the repo. `application-prod.yml` must contain **no** literal credential —
  only `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`, `${JWT_SECRET}`,
  `${APP_CORS_ALLOWED_ORIGINS}` style placeholders.
- `.env.example` in both repos: every key documented, placeholder values only.
- Verify: `git ls-files | grep -E '(^|/)\.env($|\.)'` empty;
  `git log -p baseline-final-delivery..HEAD | grep -iE 'password\s*=\s*\S|secret\s*=\s*\S'`
  empty.

Commit: `chore(security): exclude env files and route all secrets through env vars`

## 0.3 Contract sketch

Write `docs/final-delivery/CONTRACT.md` — one page. Content: the endpoint list from
PLAN.md §7 verbatim, the ownership table from PLAN.md §1, the error shape and codes
from PLAN.md §7, and the vocabulary table from PLAN.md §8. This is a design record,
not a spec ceremony — it exists so P1–P4 do not each re-decide field names.

Commit: `docs(final-delivery): add the target HTTP contract sketch`

## 0.4 Global CORS

- `infrastructure/config/CorsConfig.java`: a `CorsConfigurationSource` bean allowing
  origin `http://localhost:5173` (configurable via `${APP_CORS_ALLOWED_ORIGINS}`),
  methods `GET,POST,PUT,DELETE,OPTIONS`, headers `Authorization,Content-Type`,
  credentials as needed. Wire it into `SecurityConfig` (`.cors(Customizer.withDefaults())`).
- No `@CrossOrigin` on any controller.
- Verify with a slice test that a `GET /api/v1/products` request carrying
  `Origin: http://localhost:5173` gets `Access-Control-Allow-Origin` back.

Commit: `feat(config): add global CORS configuration for the Vite dev origin`

## 0.5 ArchUnit

- Add to `pom.xml` (only the version property exists today):

```xml
<dependency>
  <groupId>com.tngtech.archunit</groupId>
  <artifactId>archunit-junit5</artifactId>
  <version>${archunit.version}</version>
  <scope>test</scope>
</dependency>
```

- `src/test/java/com/unicornt/store/architecture/`:
  - `LayeredArchitectureTest` / `DependencyRulesTest` with **Group A** rules (green
    now) and **Group B** rules (PLAN.md §3.3) using `FreezingArchViolationStore`
    (`archunit.properties` → `freeze.store.default.path=src/test/resources/archunit_store`,
    `freeze.refreeze=false`).
  - **Group C** rules present but `@Disabled("FINAL-DELIVERY: pending slice <x>")`.
- Run `mvn -q test`; it must be green (Group B frozen). Record the frozen violation
  count per rule in `docs/final-delivery/CONTRACT.md` under an "Architecture backlog"
  heading — that count is the refactor progress meter.
- Commit the baseline directory.

Commit: `test(arch): add ArchUnit dependency rules with a frozen violation baseline`

## 0.6 Minimal read E2E

- Backend: `docker compose up -d`; app on `dev` serving real `GET /api/v1/products`
  and `GET /api/v1/categories` from PostgreSQL (seed data via existing migrations).
- Frontend: point the product list at `apiFetch('/api/v1/products')`; the mock
  catalog is demoted from primary source (keep it only as a test fixture if useful).
- Verify in the Vite browser: catalog renders with Postgres rows, no CORS error, no
  mock. Capture the network tab / a `curl` transcript.

Commit (frontend): `feat(catalog): read the product list from the real backend API`

## 0.7 pom.xml / package.json freeze

Before closing P0, add every dependency P1–P6 will need so no worker edits the build
files:

- Backend: `archunit-junit5` (done above). Everything else is already present
  (JUnit 5, Mockito, AssertJ, spring-security-test, webmvc-test, testcontainers, H2).
- Frontend: confirm Vitest + coverage + lint are wired in `package.json` scripts.

Commit: `build: freeze backend and frontend dependency sets for the refactor`

---

## Definition of Done

```bash
# backend, on final-delivery
mvn -q -DskipTests compile            # green
mvn -q test                           # green (ArchUnit Group B frozen)
git ls-files | grep -E '(^|/)\.env($|\.)'    # empty
test -f infrastructure/config/CorsConfig.java || true
test -d src/test/resources/archunit_store
# frontend, on final-delivery
npm ci && npm run build && npm test   # green

# read E2E — captured transcript in the handoff
```

## Gate

The Vite app shows the catalog served from PostgreSQL, with no CORS error and no
mock as primary source.

## Handoff

Write `docs/final-delivery/handoffs/P0-foundation.md` from the template. Record: the
frozen ArchUnit violation count per rule, the CORS origin key name, the exact
`.env.example` keys added, and anything a later phase must not re-add to `pom.xml` /
`package.json`.
