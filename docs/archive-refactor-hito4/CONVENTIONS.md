# Shared conventions — Milestone 4 refactor

Every agent working on this refactor reads this file first. It is binding for the
orchestrator and for every worktree worker.

## 1. Language

- **All artifacts are written in English**: commit messages, branch names, source
  code identifiers, code comments, Javadoc, README, `docs/**`, OpenAPI
  descriptions, log messages, exception messages, migration file names.
- The planning documents `REFACTOR-UNICORNT-HITO4.md` and
  `INSTRUCCIONES-AGENTE-EVALUADOR-HITO4.md` are inputs, not deliverables. Do not
  translate or edit them.
- User-facing API strings (error `message`, Swagger summaries) are English too.

## 2. Commits

- Conventional-commit style, imperative mood, English, no trailing period:
  `feat(rest): add product catalog endpoints under /api/v1/products`
- Prefixes in use: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `build`, `ci`.
- One coherent change per commit. Every commit must leave the module compiling
  (`mvn -q -DskipTests compile`); a worktree may not end a session on a broken build.
- **No agent or tool attribution of any kind.** Commit messages and PR bodies must
  contain no `Co-Authored-By`, no `Generated with`, no `Claude`, no `Anthropic`,
  no session URL, no emoji footer. The author is the repository owner.
- Every worktree carries `.claude/settings.json` with the attribution block below;
  it is committed on the base branch so worktrees inherit it. Verify it exists
  before the first commit:

```json
{
  "attribution": {
    "commit": "",
    "pr": "",
    "sessionUrl": false
  }
}
```

- Self-check before finishing:
  `git log --format='%an <%ae>%n%B' <base>..HEAD | grep -iE 'claude|anthropic|co-authored|generated with'`
  must return nothing.

## 3. Branches and worktrees

- Integration branch: **`refactor/hito4`**, cut from `dev`.
- Worker branches: `refactor/h4-<slug>`, always cut from the current tip of
  `refactor/hito4`.
- Each worker runs in its own git worktree, created from the main clone:

```bash
git worktree add ../unicornt-worktrees/<slug> -b refactor/h4-<slug> refactor/hito4
```

- Suggested worktree root on this machine:
  `C:/Users/Usuario/Proyectos/unicornt-worktrees/<slug>`.
- **The reference projects `actividad_m6_l5/` and `demoApiRest/` are untracked**,
  so they do not appear inside a worktree. Read them by absolute path from the
  main clone: `C:/Users/Usuario/Proyectos/unicornt-store-backend/demoApiRest/...`.
  Never copy those directories into a worktree and never commit them.
- Workers never merge into `refactor/hito4` themselves and never rebase another
  worker's branch. The orchestrator owns all integration.

## 4. File ownership (the parallelism contract)

A worker may only create or modify files inside its own ownership set. Anything
outside it goes into the handoff note as a request to the orchestrator.

| Owner | Paths |
|-------|-------|
| T0 foundation (orchestrator, sequential) | `pom.xml`, `.gitignore`, `.env.example`, `.mvn/**`, `mvnw*`, package moves, all deletions, `domain/exception/**`, `infrastructure/web/error/ErrorResponse.java`, `infrastructure/web/error/GlobalExceptionHandler.java` |
| T1 persistence-docker | `docker-compose.yml`, `Dockerfile`, `src/main/resources/application*.yml`, `src/main/resources/db/migration/**`, `sql/**`, `src/test/resources/**` |
| T2 security-jwt | `infrastructure/security/**`, `infrastructure/web/error/RestAuthEntryPoint.java`, `RestAccessDeniedHandler.java`, `infrastructure/web/rest/AuthRestController.java`, `infrastructure/web/dto/Auth*.java`, `domain/service/UserService*`, `CustomUserDetailsService` |
| T3 api-catalog | `infrastructure/web/rest/Product*`, `Category*`, matching `dto/`, `mapper/`, `domain/service/Product*`, `Category*`, their tests |
| T4 api-commerce | `infrastructure/web/rest/Cart*`, `Order*`, `Address*`, matching `dto/`, `mapper/`, `domain/service/Cart*`, `Checkout*`, `Address*`, their tests |
| T5 openapi-profiles | `infrastructure/config/OpenApiConfig.java`, springdoc keys in `application*.yml` |
| T6 deliverable | `README.md`, `docs/**` (except `docs/refactor/**`), `.github/workflows/**`, `scripts/**` |

**`pom.xml` is frozen after T0.** T0 adds every dependency the whole refactor
needs, so no worker ever edits it. If a worker finds a missing dependency, it
stops and reports to the orchestrator rather than editing `pom.xml`.

`SecurityConfig` belongs to T2 only. T3/T4 express authorization on their own
controllers with `@PreAuthorize`, and list any required request-matcher rule in
their handoff note.

## 5. Stack baseline (deviations from the plan document)

The plan document assumes Java 21. **This repository is already on Java 25 with
Spring Boot 4.0.8** after a recent upgrade — keep it there. Concretely:

- `<java.version>25</java.version>`, parent `spring-boot-starter-parent:4.0.8`.
- Dockerfile base images: `eclipse-temurin:25-jdk` / `eclipse-temurin:25-jre`
  (not 21, as the plan snippet shows).
- `org.postgresql:postgresql` stays pinned to `42.7.12` with its CVE comment
  until the Boot BOM manages that version or newer.
- The Maven wrapper does not exist yet; T0 generates it, because the README, CI
  and the acceptance script all call `./mvnw`.
- Configuration migrates from `application*.properties` to `application*.yml`.

## 6. Definition of Done for any worker

1. `mvn -q -DskipTests compile` passes.
2. `mvn -q test` passes (no worker may leave a red test it introduced).
3. No leftovers of the presentation layer in the touched code: no `@Controller`,
   `ModelAndView`, `redirect:`, Thymeleaf, `printStackTrace()`.
4. No secret literal in the diff. Credentials come from environment variables only.
5. Every new REST endpoint carries `@Tag`/`@Operation`/`@ApiResponses`, and every
   new DTO record carries `@Schema` on its fields.
6. Commits follow section 2 and the attribution self-check returns nothing.
7. A handoff note is written to `docs/refactor/handoffs/<slug>.md`, using
   `docs/refactor/templates/handoff.md`, and committed with the work.

## 7. Verification commands used throughout

```bash
mvn -q -DskipTests compile
mvn -q test
mvn -q -DskipTests clean package
grep -rn "@Controller\|ModelAndView\|redirect:\|Thymeleaf" src/main            # empty
grep -rni "mysql" pom.xml src/ docker-compose.yml                              # empty
find src/main -path "*domain*" -name "*.java" | xargs grep -l "jakarta.persistence"  # empty
```
