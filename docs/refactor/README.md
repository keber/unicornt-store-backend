# Milestone 4 refactor — agent instruction set

Execution package for the plan in [REFACTOR-UNICORNT-HITO4.md](../../REFACTOR-UNICORNT-HITO4.md):
turning `unicornt-store-springboot` (Spring MVC + Thymeleaf) into
`unicornt-store-backend`, a pure REST microservice on PostgreSQL with JWT and
OpenAPI, scoring C1 3/3 · C2 3/3 · C3 4/4.

## Files

| File | Read it when |
|------|--------------|
| [CONVENTIONS.md](CONVENTIONS.md) | Always, first. Language, commits, attribution, file ownership, DoD. |
| [ORCHESTRATOR.md](ORCHESTRATOR.md) | You are coordinating the refactor. |
| [tasks/](tasks/) | You are a worker: read only your own task file (plus CONVENTIONS). |
| [templates/handoff.md](templates/handoff.md) | Every worker, at the end of its task. |
| [templates/settings.json](templates/settings.json) | Attribution suppression, if a worktree lacks `.claude/settings.json`. |

Agent definitions live in [.claude/agents/](../../.claude/agents/):
`refactor-orchestrator` and `refactor-worker`.

## Why this cut of the work

The plan document is written as 9 sequential stages. Those stages are not the
right unit of parallelism: stages 1 and 2 rewrite every Java file (any
concurrency there is a guaranteed conflict), while stage 7 spreads annotations
across every controller written in stage 4.

So the work is re-cut **by file ownership** instead of by stage. One sequential
foundation makes the tree safe to fan out from; four workers then own disjoint
file sets; two finishing tasks close the rubric and the deliverable.

## Execution graph

```
T0  foundation                      (sequential, on refactor/hito4)
    plan stages 0, 1, 2 + all pom changes + shared error contract
                    │
        ┌───────────┼───────────┬───────────────┐
        ▼           ▼           ▼               ▼
  T1 persistence  T2 security  T3 api-catalog  T4 api-commerce
     -docker         -jwt      products,       cart, orders,
     stage 3         stage 6   categories      addresses
                               stage 4a        stage 4b
        └───────────┴───────────┴───────────────┘
                    │  orchestrator integrates, in this order
                    ▼
              T5 openapi-profiles      (stage 7)
                    │
                    ▼
              T6 deliverable           (stage 8)
                    │
                    ▼
        orchestrator: E2E acceptance + rubric checklist
```

Parallel band: T1 · T2 · T3 · T4, each in its own git worktree.
T5 and T6 are sequential because they verify the assembled whole.

## Task index

| Task | Branch slug | Plan stages | Rubric |
|------|-------------|-------------|--------|
| [T0](tasks/T0-foundation.md) | *(none — on `refactor/hito4`)* | 0, 1, 2, 5-core | enabler |
| [T1](tasks/T1-persistence-docker.md) | `persistence-docker` | 3 | C2 3/3 |
| [T2](tasks/T2-security-jwt.md) | `security-jwt` | 6 | C1 auth, enables C3 |
| [T3](tasks/T3-api-catalog.md) | `api-catalog` | 4 (catalog), 5, 7 | C1 |
| [T4](tasks/T4-api-commerce.md) | `api-commerce` | 4 (commerce), 5, 7 | C1 3/3 |
| [T5](tasks/T5-openapi-profiles.md) | `openapi-profiles` | 7 | C3 4/4 |
| [T6](tasks/T6-deliverable.md) | `deliverable` | 8 | deliverable |

## Quick start

```bash
# from the main clone
git checkout dev && git tag baseline-hito3 && git checkout -b refactor/hito4
# then run T0 to completion, and only then fan out:
git worktree add ../unicornt-worktrees/persistence-docker -b refactor/h4-persistence-docker refactor/hito4
git worktree add ../unicornt-worktrees/security-jwt      -b refactor/h4-security-jwt      refactor/hito4
git worktree add ../unicornt-worktrees/api-catalog       -b refactor/h4-api-catalog       refactor/hito4
git worktree add ../unicornt-worktrees/api-commerce      -b refactor/h4-api-commerce      refactor/hito4
```
