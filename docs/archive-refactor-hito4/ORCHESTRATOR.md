# Orchestrator playbook

You coordinate the Milestone 4 refactor. You do **not** implement T1–T6 yourself:
you implement T0, then dispatch workers, then integrate, verify and report.

Read [CONVENTIONS.md](CONVENTIONS.md) before anything else.

---

## Phase A — Foundation (you do this yourself, sequentially)

Work directly on `refactor/hito4` in the main clone. Do not fan out yet: T0
rewrites nearly every Java file, so any concurrent worktree would conflict.

```bash
git checkout dev
git tag baseline-hito3          # if it does not exist yet
git checkout -b refactor/hito4
```

Execute [tasks/T0-foundation.md](tasks/T0-foundation.md) end to end. Phase A is
finished only when all of these hold:

- `mvn -q -DskipTests compile` passes and `mvn spring-boot:run` starts the app.
- `grep -rn "@Controller\|ModelAndView\|redirect:\|Thymeleaf" src/main` is empty.
- `find src/main -path "*domain*" -name "*.java" | xargs grep -l "jakarta.persistence"` is empty.
- `pom.xml` already contains every dependency T1–T6 will need. **After this point
  `pom.xml` is frozen** — that is what makes the fan-out conflict-free.
- `.claude/settings.json` with the attribution block is committed, so every
  worktree inherits it.

## Phase B — Parallel fan-out

Create four worktrees from the tip of `refactor/hito4`:

```bash
for s in persistence-docker security-jwt api-catalog api-commerce; do
  git worktree add ../unicornt-worktrees/$s -b refactor/h4-$s refactor/hito4
done
```

Dispatch one `refactor-worker` agent per worktree. Give each agent exactly:

1. its worktree absolute path and branch name;
2. the path to its task file, `docs/refactor/tasks/T<n>-<slug>.md`;
3. the path to `docs/refactor/CONVENTIONS.md`;
4. the absolute path of the main clone, for reading the untracked reference
   projects `actividad_m6_l5/` and `demoApiRest/`.

Do not paste the task content into the prompt — the file is inside the worktree;
tell the agent to read it.

While workers run, do not edit `refactor/hito4`. Any change you make there
invalidates the base the four branches were cut from.

## Phase C — Integration

Merge in this order. The order is not arbitrary: configuration first so the app
can boot, then security so the filter chain exists, then the two API surfaces.

```
1. refactor/h4-persistence-docker
2. refactor/h4-security-jwt
3. refactor/h4-api-catalog
4. refactor/h4-api-commerce
```

For each branch:

```bash
git checkout refactor/hito4
git merge --no-ff refactor/h4-<slug>
mvn -q -DskipTests compile && mvn -q test
```

Expected conflict points and how to resolve them:

| Conflict | Resolution |
|----------|------------|
| `application*.yml` springdoc keys | T1's version wins; T5 finalizes them |
| `SecurityConfig` request matchers | T2's file wins; fold in the rules T3/T4 requested in their handoff notes |
| Duplicate exception types in `domain/exception` | keep the T0 originals; delete the worker copies |
| A worker edited `pom.xml` | revert that hunk, apply the change yourself, note it |

Read each `docs/refactor/handoffs/<slug>.md` as you merge — that is where the
workers recorded the cross-boundary requests you now have to apply.

Only after all four are merged, green and startable:

```bash
git worktree remove ../unicornt-worktrees/<slug>   # for each
```

## Phase D — Finishing tasks (sequential)

These verify the assembled system, so they run one after the other, either
directly on `refactor/hito4` or in a short-lived worktree.

1. [tasks/T5-openapi-profiles.md](tasks/T5-openapi-profiles.md) — Swagger open in
   `dev`, 404 in base and `prod`. Closes C3 at 4/4.
2. [tasks/T6-deliverable.md](tasks/T6-deliverable.md) — README, Bruno collection,
   CI, secret sweep. Closes the deliverable.

## Phase E — Acceptance

Run the global Definition of Done from the plan document:

```bash
docker compose down -v && docker compose up -d
cp .env.example .env    # fill placeholders
./mvnw -q -DskipTests clean package
SPRING_PROFILES_ACTIVE=dev java -jar target/app.jar &
# products list 200 · create without token 401 · create with USER token 403
# create with ADMIN token 201 · swagger-ui.html 200 · api-docs 200
SPRING_PROFILES_ACTIVE=prod java -jar target/app.jar &
# swagger-ui.html 404 · api-docs 404
```

Then walk the rubric checklist at the end of the plan document, C1 / C2 / C3 /
deliverable, and report each box as met or not met with the command that proves
it. Report honestly: an unmet box is stated as unmet.

Final attribution sweep across the whole refactor:

```bash
git log --format='%an <%ae>%n%B' baseline-hito3..refactor/hito4 \
  | grep -iE 'claude|anthropic|co-authored|generated with'    # must be empty
```

## Rules for you

- You own `pom.xml`, all merges, and the integration branch. Nobody else touches them.
- Never let a worker merge, rebase or push another worker's branch.
- If a worker reports a blocker outside its ownership set, you resolve it on
  `refactor/hito4` **only after** that worker's branch is merged, then rebase or
  re-dispatch the remaining workers if the change affects them.
- If two workers need the same new shared file, that file belonged in T0 — add it
  to `refactor/hito4`, merge the affected branches, and reconcile once.
