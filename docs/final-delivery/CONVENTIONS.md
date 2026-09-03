# Shared conventions — Final Delivery refactor

Every agent on this refactor reads this file first. It is binding for the
orchestrator and for every worktree worker, across **both repositories**
(`unicornt-store-backend` and `unicornt-store-frontend`).

The source plan is [PLAN.md](PLAN.md). The phase files under [phases/](phases/)
re-cut it by file ownership. Where a phase file and the plan disagree on a
mechanical detail, the phase file wins and states why.

---

## 1. Language

- **Every artifact is written in English**: commit messages, branch names, source
  identifiers, code comments, Javadoc/TSDoc, `README.md`, `docs/**`, OpenAPI
  descriptions, log messages, exception messages, migration file names, test names.
- User-facing API strings (error `message`, Swagger summaries) are English too.
- `docs/final-delivery/inputs/**` and the Milestone 4 root documents
  (`REFACTOR-UNICORNT-HITO4.md`, `INSTRUCCIONES-AGENTE-EVALUADOR-HITO4.md`) are
  inputs, not deliverables. Do not translate or edit them.

## 2. Commits and attribution

- Conventional-commit style, imperative mood, English, no trailing period:
  `feat(catalog): add Product domain model with Money and Quantity value objects`
- Prefixes in use: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `build`, `ci`.
- One coherent change per commit. Every commit leaves the project building
  (`mvn -q -DskipTests compile` for the backend; `npm run build` for the frontend).
  A worktree may not end a session on a broken build.
- **No agent or tool attribution of any kind.** Commit messages and PR bodies carry
  no `Co-Authored-By`, no `Generated with`, no `Claude`, no `Anthropic`, no session
  URL, no emoji footer. The author is the repository owner.
- Every worktree carries `.claude/settings.json` with the block below. It is
  committed on the integration branch of each repo, so worktrees inherit it. Verify
  it is present before the first commit; if absent, copy
  [templates/settings.json](templates/settings.json) into `.claude/settings.json`.

```json
{
  "attribution": {
    "commit": "",
    "pr": "",
    "sessionUrl": false
  }
}
```

- Self-check before finishing, in each repo:
  `git log --format='%an <%ae>%n%B' <base>..HEAD | grep -iE 'claude|anthropic|co-authored|generated with'`
  must return nothing. `<base>` is `baseline-final-delivery`.

## 3. Branches and worktrees

Both repositories use the same names.

- Baseline tag: **`baseline-final-delivery`**, on the tip of `dev` (backend) /
  `dev` (frontend) before P0.
- Integration branch: **`final-delivery`**, cut from that tag.
- Worker branches: **`fd/<slug>`**, always cut from the current tip of
  `final-delivery` at dispatch time.
- Each worker runs in its own git worktree per repo it touches:

```bash
# backend
git worktree add ../unicornt-worktrees/fd-<slug>          -b fd/<slug> final-delivery
# frontend (only if the slice has a frontend delta)
git worktree add ../unicornt-frontend-worktrees/fd-<slug> -b fd/<slug> final-delivery
```

- Worktree roots on this machine:
  `C:/Users/Usuario/Proyectos/unicornt-worktrees/<slug>` and
  `C:/Users/Usuario/Proyectos/unicornt-frontend-worktrees/<slug>`.
- Workers never merge into `final-delivery`, never rebase another worker's branch,
  never push another worker's branch. **The orchestrator owns all integration** in
  both repos.
- The orchestrator does not edit `final-delivery` in either repo while workers are
  running — that tip is the base their branches were cut from.
- `docs/final-delivery/inputs/**` reference material is tracked and present in every
  worktree; it is read-only.

## 4. File ownership — the parallelism contract

A worker creates or modifies files only inside its ownership set. Anything outside
goes into the handoff note as a request to the orchestrator, not into the diff.

Package root: `com.unicornt.store`. Target layout is PLAN.md §2.1.

| Owner | Backend paths | Frontend paths |
|-------|---------------|----------------|
| **P0** (orchestrator, sequential) | `pom.xml`, `.gitignore`, `.env.example`, `.claude/settings.json`, `src/test/java/**/architecture/**`, `src/test/resources/archunit_store/**`, `infrastructure/config/**` (incl. new `CorsConfig`), `infrastructure/web/error/**`, `domain/exception/**` | `.gitignore`, `.env.example`, `src/api/http.ts` (shared `apiFetch`), Vite `.env` handling |
| **P1** catalog (orchestrator, sequential) | `domain/model/{Product,Category}.java`, `domain/valueobject/{Money,Quantity}.java`, `domain/repository/{Product,Category}Repository.java`, `application/usecase/catalog/**`, `infrastructure/persistence/**/*Product*`, `**/*Category*`, `infrastructure/web/rest/{Product,Category}RestController.java`, `infrastructure/web/dto/{Product,Category}Dtos.java`, `infrastructure/web/mapper/{Product,Category}RestMapper.java`, their tests | `src/models/product*`, `src/api/product*`, `src/services/product*`, catalog views/components |
| **P2a** cart (`fd/cart`) | `domain/model/{Cart,CartItem}.java`, `domain/repository/CartRepository.java`, `application/usecase/cart/**`, `infrastructure/persistence/**/*Cart*`, `infrastructure/web/**/*Cart*`, their tests | `src/models/cart*`, `src/api/cart*`, `src/services/cart*`, `src/storage/cart*`, cart views/components |
| **P2b** identity (`fd/identity`) | `domain/model/User.java`, `domain/repository/UserRepository.java`, `application/usecase/identity/**`, `infrastructure/security/**`, `infrastructure/web/**/*Auth*`, `CustomUserDetailsService`, their tests | `src/api/auth*`, `src/services/auth*`, token storage, login/register views |
| **P3** ordering (`fd/ordering`) | `domain/model/{Order,OrderItem,OrderStatus,ShippingAddress}.java`, `domain/repository/OrderRepository.java`, `application/usecase/ordering/**`, `infrastructure/persistence/**/*Order*`, `**/*Address*`, `infrastructure/web/**/*Order*`, their tests | `src/models/checkout*` / `src/models/order*`, `src/api/order*`, `src/services/checkout*`, checkout views |
| **P4** admin (`fd/admin`) | only `@PreAuthorize("hasRole('ADMIN')")` + tests on the P1 product write endpoints, if missing | admin product UI (list, form, delete) |
| **P5/P6** (orchestrator) | `README.md`, `docs/**` (except `docs/final-delivery/inputs/**` and `docs/archive-refactor-hito4/**`), `.github/workflows/**`, OpenAPI finalization, `jacoco:check` rules in `pom.xml` | `README.md`, `package.json` scripts, frontend port extraction (`ProductGateway`, `CheckoutGateway`) |

**`pom.xml` and `package.json` are frozen after P0.** P0 adds every dependency the
whole refactor needs. A worker that finds a missing dependency stops and reports it.

**`SecurityConfig` belongs to P2b only.** Other phases express authorization with
`@PreAuthorize` on their own controllers and list any required request-matcher rule
in their handoff note; the orchestrator folds it into `SecurityConfig` at integration.

**`GlobalExceptionHandler` and `domain/exception/**` belong to P0.** A slice that
needs a new domain exception requests it in its handoff note; the orchestrator adds
it to `final-delivery` and re-bases the affected branch.

## 5. Architecture rules (enforced by ArchUnit)

The dependency rule (PLAN.md §2.2), restated:

```
infrastructure ──► application ──► domain          domain depends on nobody
infrastructure ──► domain  (implements domain.repository, maps to domain.model)

domain      -X► org.springframework..
domain      -X► jakarta.persistence.. / jakarta.validation..
domain      -X► ..infrastructure..   /  ..application..
application -X► ..infrastructure..    /  org.springframework.data..
web (rest)  -X► ..persistence..  (always via a use case)
```

- Domain models are plain Java. Value objects (`Money`, `Quantity`) validate in
  their constructor. Minimal set only — no `ProductName`, `Street`, `CategoryName`.
- Controllers are thin: `request → use case → response`. No business rule in a
  controller.
- JPA entities are `*JpaEntity` under `infrastructure/persistence/entity`; a
  `*PersistenceMapper` converts them to and from domain models; a
  `*RepositoryAdapter` implements the `domain.repository` interface.

## 6. ArchUnit freezing baseline

- P0 writes the rules (PLAN.md §3.3) and freezes the current violations with
  `FreezingArchViolationStore` into `src/test/resources/archunit_store/`. The build
  is green; a **new** violation fails it.
- **Workers do not edit or commit the baseline.** After a slice removes violations,
  the worker reports the expected drop in its handoff note; the **orchestrator**
  regenerates and commits the baseline on `final-delivery` right after integrating
  that slice (the store file is cross-cutting — single writer only).
- P5 converts Group B rules to hard rules and deletes the store once the count is 0.

## 7. Tests

- English throughout: `@DisplayName` prose, behaviour-named methods, AAA / GWT.
- Mockito only for boundary collaborators (`domain.repository` ports, `PasswordEncoder`,
  collaborating use cases). Never mock the class under test.
- AssertJ in new files. `BigDecimal` compared with `isEqualByComparingTo`.
- **Migrate, do not delete.** The 145 tests on `test/coverage-boost` map to the new
  layout per PLAN.md Appendix C. Move the scenarios; do not drop coverage.
- `jacoco:check` scope (P5 finalizes, PLAN.md §2.5): `..domain..` 100%/100%,
  `..application..` 100%/95%, rest 85%/75%.
- Java 25 + Mockito needs `-Dnet.bytebuddy.experimental=true`; it is already in the
  surefire `argLine` as `@{argLine} -Dnet.bytebuddy.experimental=true`. Keep it.

## 8. Definition of Done for any worker

1. Backend: `mvn -q -DskipTests compile` and `mvn -q test` green. ArchUnit tests
   green (frozen or zero violations for the slice's own rules).
2. Frontend (if touched): `npm run build`, `npm test`, `npm run lint` green;
   `0` explicit `any`, `0` unsafe non-null assertions.
3. No secret literal in the diff. Credentials via environment variables only.
4. Every new REST endpoint carries `@Tag` / `@Operation` / `@ApiResponses`; every new
   DTO record carries `@Schema` on its fields.
5. Commits follow section 2; the attribution self-check returns nothing.
6. The slice's **gate** (PLAN.md §4, the phase file repeats it) is demonstrated with
   real command / request output.
7. A handoff note is written from [templates/handoff.md](templates/handoff.md) to
   `docs/final-delivery/handoffs/<slug>.md` and committed with the work.

## 9. Verification commands

```bash
# backend
mvn -q -DskipTests compile
mvn -q test
mvn -q verify                                   # runs jacoco:check + ArchUnit
grep -rn "org.springframework\|jakarta.persistence\|jakarta.validation" \
  src/main/java/com/unicornt/store/domain        # empty
grep -rn "org.springframework.data\|infrastructure" \
  src/main/java/com/unicornt/store/application    # empty (except application-internal imports)

# secrets (both repos)
git ls-files | grep -E '(^|/)\.env($|\.)'        # empty
git log -p baseline-final-delivery..HEAD | grep -iE 'password\s*=\s*\S|secret\s*=\s*\S|BEGIN (RSA|OPENSSH) PRIVATE KEY'   # empty

# frontend
npm run build && npm test && npm run lint
grep -rn ": any\b\|as any\b\|!\." src                # review each hit; target is 0 unsafe
```
