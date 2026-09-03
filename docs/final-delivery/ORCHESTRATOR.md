# Orchestrator playbook — Final Delivery

You coordinate the Final Delivery refactor across two repositories. You implement
**P0 and P1 yourself** (sequential foundation + reference slice), then dispatch
workers for P2–P4, integrate their branches, run P5–P6, and verify against the
rubric.

Read [CONVENTIONS.md](CONVENTIONS.md) and [PLAN.md](PLAN.md) before anything else.

Repos, side by side:

```
C:/Users/Usuario/Proyectos/unicornt-store-backend      (this repo, branch dev)
C:/Users/Usuario/Proyectos/unicornt-store-frontend     (external repo, branch dev)
```

---

## Phase 0 — Foundation (you, sequential, on `final-delivery`)

```bash
# backend
git checkout dev && git tag baseline-final-delivery && git checkout -b final-delivery
# frontend
cd ../unicornt-store-frontend
git checkout dev && git tag baseline-final-delivery && git checkout -b final-delivery
```

Execute [phases/P0-foundation.md](phases/P0-foundation.md) end to end. Phase 0 is
done only when all of these hold:

- **Secrets (dim 3):** `git ls-files | grep -E '\.env($|\.)'` empty in both repos;
  `.gitignore` covers `.env*`, build output, IDE files; `JWT_SECRET` / `DB_PASSWORD`
  read from env only; `.env.example` documents every key with placeholder values.
- **Contract sketch:** [phases/P0-foundation.md](phases/P0-foundation.md) produces a
  one-page `docs/final-delivery/CONTRACT.md` covering the endpoints in PLAN.md §7,
  ownership, and the error shape. Reviewed by you.
- **Global CORS:** `infrastructure/config/CorsConfig.java` allows
  `http://localhost:5173`; a slice test proves the `GET /api/v1/products` preflight.
- **ArchUnit:** dependency added to `pom.xml`; Group A rules green; Group B rules
  frozen with a committed baseline in `src/test/resources/archunit_store/`; Group C
  rules present and `@Disabled`. Record the frozen violation count.
- **Read E2E:** the Vite app renders the catalog from `GET /api/v1/products` against
  PostgreSQL in Docker, no mock as primary source, no CORS error.
- `.claude/settings.json` with the attribution block is committed on `final-delivery`
  in both repos, so worktrees inherit it.
- `pom.xml` and `package.json` contain every dependency P1–P6 will need. **After
  this point both are frozen.**

## Phase 1 — Catalog slice (you, sequential, on `final-delivery`)

Execute [phases/P1-catalog-slice.md](phases/P1-catalog-slice.md) following
[slice-recipe.md](slice-recipe.md). This slice is the **reference pattern**: its
package layout, its adapter/mapper shape, its test split and its DTO conventions are
what P2–P4 copy. Do not fan out until P1 is merged, green and its gate demonstrated.

When P1 is done: enable the Group C ArchUnit rules for `catalog`, regenerate the
freezing baseline (catalog violations → 0), commit it.

## Phase 2 — Parallel fan-out (Cart ‖ Identity)

Create worktrees from the tip of `final-delivery` in both repos:

```bash
cd ../unicornt-store-backend
git worktree add ../unicornt-worktrees/fd-cart     -b fd/cart     final-delivery
git worktree add ../unicornt-worktrees/fd-identity -b fd/identity final-delivery
cd ../unicornt-store-frontend
git worktree add ../unicornt-frontend-worktrees/fd-cart     -b fd/cart     final-delivery
git worktree add ../unicornt-frontend-worktrees/fd-identity -b fd/identity final-delivery
```

Dispatch one `final-delivery-worker` per slice. Give each agent exactly:

1. its backend worktree absolute path and its frontend worktree absolute path;
2. its branch name `fd/<slug>`;
3. the path to its phase file, `docs/final-delivery/phases/P2<x>-<slug>.md`;
4. the paths to `docs/final-delivery/CONVENTIONS.md` and
   `docs/final-delivery/slice-recipe.md`;
5. the absolute path of the frontend dev clone (for reading the real module layout
   the plan describes abstractly).

Do not paste phase content into the prompt — the files are in the worktree.

While workers run, do not touch `final-delivery` in either repo.

## Phase 3 — Ordering slice

After both P2 branches are merged (see Phase C below), dispatch one worker for
[phases/P3-ordering-slice.md](phases/P3-ordering-slice.md). It depends on the Cart
and Catalog domain models being on `final-delivery`, so it cannot start earlier.

## Phase 4 — Admin product

Dispatch one worker for [phases/P4-admin-product.md](phases/P4-admin-product.md).
Mostly frontend admin UI plus the admin E2E gate; backend product CRUD already
exists from P1.

## Phase C — Integration (you, after each worker reports)

Merge into `final-delivery` in this order, **per repo**:

```
1. fd/identity      (security first — the filter chain other slices assume)
2. fd/cart
3. fd/ordering
4. fd/admin
```

For each branch, in each repo it touched:

```bash
git checkout final-delivery
git merge --no-ff fd/<slug>
# backend
mvn -q -DskipTests compile && mvn -q test && mvn -q verify
# frontend
npm ci && npm run build && npm test && npm run lint
```

Expected conflict points:

| Conflict | Resolution |
|----------|------------|
| `SecurityConfig` request matchers | `fd/identity`'s file wins; fold in the matchers other slices asked for in their handoff notes |
| `src/test/resources/archunit_store/**` | never a worker's version — you regenerate the baseline after the merge and commit it |
| Duplicate domain exceptions | keep P0's originals; delete worker copies |
| `infrastructure/web/error/GlobalExceptionHandler.java` | P0's version wins; add only the `@ExceptionHandler` a slice requested |
| A worker edited `pom.xml` / `package.json` | revert that hunk, apply the change yourself, note it |
| Frontend `src/main.ts` / router wiring | you own the wiring; apply each slice's registration request from its handoff |

Read each `docs/final-delivery/handoffs/<slug>.md` as you merge.

After each merge: regenerate the ArchUnit baseline, enable that slice's Group C
rules, commit. Then remove the worktrees:

```bash
git worktree remove ../unicornt-worktrees/fd-<slug>
git worktree remove ../unicornt-frontend-worktrees/fd-<slug>
```

## Phase 5 — Hardening (you, sequential)

Execute [phases/P5-hardening.md](phases/P5-hardening.md): Group B rules become hard,
store deleted; `jacoco:check` scoped per CONVENTIONS §7; CORS / Swagger / secrets
re-verified; frontend minimal port extraction; both quality gates green.

## Phase 6 — Delivery (you, sequential)

Execute [phases/P6-delivery.md](phases/P6-delivery.md): `README.md` in both repos,
final OpenAPI, reproduce-from-scratch check, re-score the 10 points.

## Phase E — Acceptance

Run the Definition of Done from PLAN.md §6. Walk the three rubric dimensions and
report each as met or not met **with the command or request/response that proves
it**. An unmet box is stated as unmet.

Final attribution sweep, both repos:

```bash
git log --format='%an <%ae>%n%B' baseline-final-delivery..final-delivery \
  | grep -iE 'claude|anthropic|co-authored|generated with'      # must be empty
```

## Rules for you

- You own `pom.xml`, `package.json`, every merge, the ArchUnit baseline, and both
  integration branches. Nobody else touches them.
- Never let a worker merge, rebase or push another worker's branch.
- A blocker outside a worker's ownership set is resolved by you on `final-delivery`
  **only after** that worker's branch is merged; then re-base or re-dispatch the
  remaining workers if the change affects them.
- If two workers need the same new shared file, it belonged in P0 — add it to
  `final-delivery`, merge the affected branches, reconcile once.
- Report honestly. Tests that fail are reported failing, with output.
