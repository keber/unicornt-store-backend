# Final Delivery refactor — agent instruction set

Execution package for [PLAN.md](PLAN.md): converging `unicornt-store-backend`
(H4 REST microservice) and `unicornt-store-frontend` (H2 Vite + TS) into one
integrated full-stack application that scores the 10-point *Final Delivery* rubric —
end-to-end integration (4), cumulative rigor / clean architecture + tactical DDD +
100% business-rule coverage in English (3), and production-grade security (3).

## Files

| File | Read it when |
|------|--------------|
| [PLAN.md](PLAN.md) | The source plan. Everyone reads it. Phase files re-cut it by ownership; on a mechanical disagreement the phase file wins and says why. |
| [CONVENTIONS.md](CONVENTIONS.md) | Always, first. Language, commits, attribution, branches/worktrees (both repos), file ownership, ArchUnit handling, Definition of Done. |
| [ORCHESTRATOR.md](ORCHESTRATOR.md) | You are coordinating the refactor. |
| [slice-recipe.md](slice-recipe.md) | You are building a vertical slice (P1–P4): the canonical domain → application → adapter → REST → frontend → tests recipe. |
| [phases/](phases/) | You are a worker: read only your own phase file, plus CONVENTIONS and slice-recipe. |
| [templates/handoff.md](templates/handoff.md) | Every worker, at the end of its phase. |
| [templates/settings.json](templates/settings.json) | Attribution suppression, if a worktree lacks `.claude/settings.json`. |
| [inputs/](inputs/) | Background only: the diagnostics and earlier drafts the plan was built from. Not deliverables; do not edit or translate. |

Agent definitions live in [`.claude/agents/`](../../.claude/agents/):
`final-delivery-orchestrator` and `final-delivery-worker`.

## Why this cut of the work

The plan is nine phases, but phases are not the unit of parallelism. Phase 0 rewrites
shared foundations and Phase 1 sets the architectural pattern every later slice
copies — both are sequential by nature. Once the pattern exists, the domains are
disjoint: `cart/**` and `identity/**` never touch the same file, so they fan out.
Ordering depends on Cart and Catalog, so it follows. Hardening and delivery verify
the assembled whole, so they are sequential again.

## Execution graph

```
P0  foundation                         (sequential, orchestrator, on `final-delivery`)
    secrets · contract sketch · global CORS · ArchUnit rules+baseline · read E2E
                    │
P1  catalog slice                      (sequential, orchestrator — the reference pattern)
    domain → application → ports → JPA adapter → REST → frontend → tests
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
  P2a cart slice           P2b identity slice     (parallel, one worker + worktree each)
     cart/** + merge          user/** + auth client
        └───────────┬───────────┘
                    │  orchestrator integrates, regenerates ArchUnit baseline
                    ▼
P3  ordering slice                     (one worker; depends on P1 + P2)
    PlaceOrder — the main E2E gate
                    │
                    ▼
P4  admin product                      (one worker; frontend admin UI + admin E2E gate)
                    │
                    ▼
P5  hardening                          (sequential, orchestrator + targeted workers)
P6  delivery                           (sequential, orchestrator)
```

Parallel band: **P2a · P2b**, each in its own backend worktree and (where it has a
frontend delta) its own frontend worktree.

## Phase index

| Phase | Branch slug | Plan section | Rubric dimension | Lane |
|-------|-------------|--------------|------------------|------|
| [P0](phases/P0-foundation.md) | *(none — on `final-delivery`)* | §4 Phase 0 | 3 (secrets) + enabler | orchestrator |
| [P1](phases/P1-catalog-slice.md) | *(none — on `final-delivery`)* | §4 Phase 1, §2 | 1 + 2 | orchestrator |
| [P2a](phases/P2a-cart-slice.md) | `fd/cart` | §4 Phase 2 (Cart) | 1 + 2 | worker |
| [P2b](phases/P2b-identity-slice.md) | `fd/identity` | §4 Phase 2 (Identity) | 1 + 2 | worker |
| [P3](phases/P3-ordering-slice.md) | `fd/ordering` | §4 Phase 3 | 1 + 2 | worker |
| [P4](phases/P4-admin-product.md) | `fd/admin` | §4 Phase 4 | 1 | worker |
| [P5](phases/P5-hardening.md) | *(short-lived worktrees)* | §4 Phase 5 | 1 + 2 + 3 | orchestrator |
| [P6](phases/P6-delivery.md) | *(on `final-delivery`)* | §4 Phase 6 | all | orchestrator |

## Quick start

```bash
# backend main clone
git checkout dev && git tag baseline-final-delivery && git checkout -b final-delivery
# frontend main clone (sibling directory)
cd ../unicornt-store-frontend && git checkout dev && git tag baseline-final-delivery && git checkout -b final-delivery

# run P0 then P1 to completion on `final-delivery`, then fan out P2:
cd ../unicornt-store-backend
git worktree add ../unicornt-worktrees/fd-cart     -b fd/cart     final-delivery
git worktree add ../unicornt-worktrees/fd-identity -b fd/identity final-delivery
cd ../unicornt-store-frontend
git worktree add ../unicornt-frontend-worktrees/fd-cart     -b fd/cart     final-delivery
git worktree add ../unicornt-frontend-worktrees/fd-identity -b fd/identity final-delivery
```
