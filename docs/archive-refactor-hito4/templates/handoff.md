# Handoff — <task id> <slug>

**Branch:** `refactor/h4-<slug>`
**Base commit:** `<sha of refactor/hito4 this branch was cut from>`
**Status:** complete | partial | blocked

## What landed

- <one line per meaningful change>

## Verification actually run

| Check | Command | Result |
|-------|---------|--------|
| compile | `mvn -q -DskipTests compile` | pass / fail |
| tests | `mvn -q test` | pass / fail |
| <task-specific> | | |

Paste the real output of anything that failed. Do not report a check as passing if
it was not run.

## Requests for the orchestrator

Changes needed outside this task's ownership set. Be precise enough that the
orchestrator can apply them without re-deriving the reasoning.

| File | Change needed | Why |
|------|---------------|-----|
| | | |

## Decisions taken

Choices the task file left open, and the reasoning behind each.

- <decision> — <why>

## Known gaps

Anything left undone, and what it blocks.

- <gap> — <impact>

## Attribution check

```
git log --format='%an <%ae>%n%B' refactor/hito4..HEAD | grep -iE 'claude|anthropic|co-authored|generated with'
```

Result: <empty / list of offending commits>
