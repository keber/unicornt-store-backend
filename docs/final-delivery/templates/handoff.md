# Handoff — <phase id> <slug>

**Repos / branches:** `unicornt-store-backend @ fd/<slug>` · `unicornt-store-frontend @ fd/<slug>`
**Base commit(s):** `<sha of final-delivery this branch was cut from, per repo>`
**Status:** complete | partial | blocked

## What landed

- backend: <one line per meaningful change>
- frontend: <one line per meaningful change>

## Verification actually run

| Check | Command | Result |
|-------|---------|--------|
| backend compile | `mvn -q -DskipTests compile` | pass / fail |
| backend tests | `mvn -q test` | pass / fail |
| backend verify | `mvn -q verify` | pass / fail |
| frontend build | `npm run build` | pass / fail |
| frontend tests | `npm test` | pass / fail |
| frontend lint | `npm run lint` | pass / fail |
| slice gate | <the phase file's gate command / request> | pass / fail |

Paste the real output of anything that failed. Do not report a check as passing if
it was not run.

## ArchUnit

Expected violation drop for this slice's rules (Group B / Group C):

| Rule | Before | After |
|------|-------:|------:|
| | | |

The baseline file is **not** in this diff — the orchestrator regenerates it.

## Requests for the orchestrator

Changes needed outside this phase's ownership set. Precise enough to apply without
re-deriving the reasoning.

| File / area | Change needed | Why |
|-------------|---------------|-----|
| `SecurityConfig` | | |
| `GlobalExceptionHandler` | | |
| frontend router / `src/main.ts` | | |
| other | | |

## Decisions taken

Choices the phase file left open, and the reasoning.

- <decision> — <why>

## Known gaps

Anything left undone, and what it blocks.

- <gap> — <impact>

## Attribution check

```
git log --format='%an <%ae>%n%B' final-delivery..HEAD | grep -iE 'claude|anthropic|co-authored|generated with'
```

Result (per repo): <empty / list of offending commits>
