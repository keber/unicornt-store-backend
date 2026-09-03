# P6 — Delivery

**Lane:** orchestrator, sequential. On `final-delivery` in both repos.
**Plan:** [PLAN.md](../PLAN.md) §4 Phase 6, §6.
**Rubric:** all three dimensions — the final walk.

---

## 6.1 README, both repos

Each `README.md` (English) covers: purpose, stack, architecture (the layered diagram
from PLAN.md §2.1), requirements, environment variables (point at `.env.example`),
bring up PostgreSQL, run the backend, run the frontend, run the tests, Swagger URL
(dev only), API base URL, demo credentials, and the E2E flow.

It must reproduce the system from scratch:

```bash
# backend
docker compose up -d
./mvnw clean test
./mvnw spring-boot:run
# frontend
npm install
npm run dev
```

Run those commands on a clean checkout and fix anything that does not work as
written.

Commit: `docs: add reproducible README for the integrated full-stack system`

## 6.2 OpenAPI finalization

- Every endpoint in `docs/final-delivery/CONTRACT.md` is present, documented
  (`@Operation`, `@ApiResponses`), and matches the served `/v3/api-docs`.
- Export the served spec to `docs/openapi.json` (or `.yaml`) for the record.

Commit: `docs(openapi): freeze the delivered API contract`

## 6.3 Re-score the 10 points

Walk the three dimensions against the final state of the integrated system and
report each with the command / request-response that proves it:

| Dimension | Pts | Evidence to produce |
|-----------|----:|---------------------|
| 1 — end-to-end integration | 4 | the P3 main-gate transcript (browser → cart → login → checkout → DB stock decrement → CONFIRMED) + the P0 read-gate + the P4 admin-gate; `curl -H 'Origin: http://localhost:5173'` shows no CORS block |
| 2 — cumulative rigor | 3 | `mvn -q verify` green with `jacoco:check` 100% on `domain`+`application`; ArchUnit hard rules green with no freeze store; `grep` shows no framework import under `domain/`; all backend code/comments/tests in English |
| 3 — production security | 3 | `git ls-files | grep -E '\.env'` empty both repos; secret grep over the range empty; Swagger 200 in dev / 404 in prod |

State any unmet item as unmet, with the failing output.

## 6.4 Final attribution sweep

```bash
git log --format='%an <%ae>%n%B' baseline-final-delivery..final-delivery \
  | grep -iE 'claude|anthropic|co-authored|generated with'      # empty, both repos
```

## 6.5 Open the PRs

One PR per repo, `final-delivery` → default branch. PR body: summary of the
converged architecture, the three-dimension evidence table, the reproduce-from-scratch
block. No attribution, no session URL, no emoji footer.

---

## Definition of Done

Both READMEs reproduce the system on a clean checkout. `docs/openapi.*` committed.
The re-score table is filled with real evidence. The attribution sweep is empty in
both repos. Two PRs open.
