# P5 — Hardening

**Lane:** orchestrator, sequential. Short-lived worktrees for any isolated fix.
**Plan:** [PLAN.md](../PLAN.md) §4 Phase 5.
**Rubric:** dimensions 1, 2 and 3.

No new features. Close the architecture, coverage, and security gates on the
assembled `final-delivery`.

---

## 5.1 ArchUnit — Group B becomes hard

- The freezing baseline for the Group B rules should now be at **0** across all
  slices. Verify: temporarily set `freeze.refreeze=false` and delete
  `src/test/resources/archunit_store/` — `mvn -q test` must still be green.
- Convert Group B rules from `FreezingArchViolationStore` to plain
  `@ArchTest` rules. Remove the store directory and the `archunit.properties` freeze
  keys.
- Enable every remaining Group C rule (`@Disabled` removed).
- If a residual violation exists, it is a real gap: fix it in a short-lived worktree,
  or record it explicitly in the delivery report as an unmet item.

Commit: `test(arch): enforce layered dependency rules without a freeze baseline`

## 5.2 Coverage scope

Set `jacoco:check` (`pom.xml`) to the scoped rules from CONVENTIONS §7:

```
- BUNDLE  ..domain..        INSTRUCTION 100% / BRANCH 100%
- BUNDLE  ..application..   INSTRUCTION 100% / BRANCH  95%
- BUNDLE  <rest>            INSTRUCTION  85% / BRANCH  75%
```

Add the new records/DTOs and every `*JpaEntity` to the JaCoCo `<excludes>`.
`mvn -q verify` green. If a `domain` / `application` class is below 100%, add the
missing tests (short-lived worktree per slice owner if substantial).

Commit: `build(coverage): scope jacoco:check to domain and application at 100%`

## 5.3 CORS / Swagger / secrets re-verify

```bash
# CORS
curl -si -H 'Origin: http://localhost:5173' http://localhost:8080/api/v1/products | grep -i access-control-allow-origin
# Swagger
SPRING_PROFILES_ACTIVE=dev  ... curl -s -o /dev/null -w '%{http_code}' /swagger-ui.html   # 200
SPRING_PROFILES_ACTIVE=prod ... curl -s -o /dev/null -w '%{http_code}' /swagger-ui.html   # 404
SPRING_PROFILES_ACTIVE=prod ... curl -s -o /dev/null -w '%{http_code}' /v3/api-docs       # 404
# secrets, both repos
git ls-files | grep -E '(^|/)\.env($|\.)'                                                  # empty
git log -p baseline-final-delivery..final-delivery | grep -iE 'password\s*=\s*\S|secret\s*=\s*\S'   # empty
```

## 5.4 Frontend minimal port extraction

- Introduce `ProductGateway` and `CheckoutGateway` interfaces (`src/application/port/`
  or the repo's convention). `HttpProductGateway`, `HttpCheckoutGateway` implement
  them over the existing `api/` modules.
- Services depend on the port, injected — not on the concrete `api/` import.
- Vitest: a fake gateway drives the service in tests.

Commit: `refactor(frontend): depend on gateway ports instead of concrete adapters`

## 5.5 Quality gates green

```bash
# backend
mvn -q clean verify && docker compose config
# frontend
npm ci && npm test && npm run lint && npm run build
```

---

## Definition of Done

All of 5.1–5.5 committed and green. The `archunit_store/` directory is gone. Both
`mvn -q clean verify` and the frontend triad pass. CORS / Swagger / secret checks
produce the expected values.

## Handoff

`docs/final-delivery/handoffs/P5-hardening.md`. Record: the final coverage numbers
for `domain` and `application`, any residual ArchUnit gap and its disposition, and
the CORS / Swagger / secret check outputs.
