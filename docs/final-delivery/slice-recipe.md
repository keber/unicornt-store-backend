# Slice recipe — the canonical vertical-slice sequence

Every slice worker (P1–P4) follows this recipe inside its worktree(s). The steps are
ordered: each one compiles before the next. Details for a given domain are in that
slice's phase file; this file is the shape they all share.

Target layout: [PLAN.md](PLAN.md) §2.1. Dependency rule: §2.2. Business rules to
protect: §2.4.

---

## Backend, in order

### 1. Domain model (`domain/model/`, `domain/valueobject/`)

- Plain Java. Zero imports from `org.springframework`, `jakarta.persistence`,
  `jakarta.validation`, `..infrastructure..`, `..application..`.
- Invariants live in the constructor or a static factory — an instance cannot exist
  in an invalid state. Value objects (`Money`, `Quantity`) validate on construction.
- No setters that can break an invariant. Prefer immutability; where the aggregate
  must mutate (e.g. `Cart.addItem`), the mutation method enforces the rule.
- Tests first or alongside: `<Model>Test`, `MoneyTest`, `QuantityTest` — pure JUnit,
  no Mockito. Cover every rule in PLAN.md §2.4 for this slice.

### 2. Repository port (`domain/repository/`)

- One interface per aggregate root, named `<Aggregate>Repository`. Pure: returns
  domain models, takes domain types. No `Page`, no `Pageable`, no Spring Data.
- Only the methods the use cases actually call.

### 3. Use cases (`application/usecase/<context>/`)

- One class per use case (`<Verb><Noun>UseCase`) with a single public method, or a
  small application service when one-class-per-verb would just proliferate.
- Depends only on `domain.*` (models, value objects, repository ports) and other use
  cases. Constructor injection. No `@Transactional` here if it would pull Spring into
  a class you want pure — put the Spring `@Component` + `@Transactional` wrapper in
  `infrastructure` if the slice needs it, or accept a thin Spring-annotated use case
  and let ArchUnit's Group C rule for this context stay `@Disabled` until P5. The
  plan's guide layout allows `application` to be Spring-wired; keep it minimal.
- Tests: `<UseCase>Test` with Mockito over the repository ports. Migrate the matching
  scenarios from the `*ServiceImplTest` this slice replaces (PLAN.md Appendix C).

### 4. Persistence adapter (`infrastructure/persistence/`)

- `entity/<Aggregate>JpaEntity` — the JPA `@Entity`. Rename from any existing
  `*Entity`; update references.
- `repository/SpringData<Aggregate>Repository extends JpaRepository<…>` — the
  technology repository.
- `mapper/<Aggregate>PersistenceMapper` — static methods, `JpaEntity ↔ domain model`.
- `adapter/<Aggregate>RepositoryAdapter implements domain.repository.<Aggregate>Repository`
  — `@Component`, delegates to the Spring Data repository, maps at the boundary.
- Tests: `<Aggregate>PersistenceMapperTest` (pure), `<Aggregate>RepositoryAdapterTest`
  (Mockito over the Spring Data repository).

### 5. Web (`infrastructure/web/`)

- `rest/<Resource>RestController` — thin: read `Authentication`, call the use case,
  map the result. No rules. `@RestController` + `@RequestMapping("/api/v1/<plural>")`;
  no verb in a URL; creation returns `ResponseEntity.created(...)`; deletion is
  `@ResponseStatus(NO_CONTENT)` + `void`.
- `dto/<Resource>Dtos` — immutable `record`s, `@Schema(example=…)` on every field,
  `jakarta.validation` on request records, `@Valid` on every `@RequestBody`.
- `mapper/<Resource>RestMapper` — static, `DTO ↔ domain model`.
- `@Tag` on the class, `@Operation` + `@ApiResponses` on every method, error entries
  pointing at `ErrorResponse`.
- Authorization with `@PreAuthorize` on the method/class. Any request-matcher rule
  you need in `SecurityConfig` goes in the handoff note, not in the diff.
- Tests: `<Resource>RestControllerTest` — `@WebMvcTest`, `@MockitoBean` of the **use
  case** (not a `*ServiceImpl`). Cover: not-found → 404 + `code`; invalid body →
  400 + `errors[]`; created → 201 + `Location`; delete → 204.

---

## Frontend, in order

### 6. Transport + validation (`src/api/`, `src/models/`)

- `api/<resource>.api.ts` — `fetch` via the shared `apiFetch` from `src/api/http.ts`
  (base URL, `Authorization`, JSON, 401). Returns `unknown`.
- `models/<resource>.dto.ts` + a runtime validator (`is<Resource>Dto`).
- `models/<resource>.model.ts` + `to<Resource>Model(dto)` — the mapper the domain
  side consumes. The model must not import the DTO type; the mapper bridges them.

### 7. Service / view wiring

- `services/<resource>.service.ts` stays pure (no `fetch`, `window`, `document`,
  `localStorage`). It calls the api module. (P5 extracts a `Gateway` port; until
  then a direct import is acceptable.)
- View/component renders with safe DOM (`textContent`, typed queries, `preventDefault`
  on forms). No `innerHTML` of untrusted data. Reuse the H2 `submitting/success/error`
  states.
- Register the new view/route with the app entrypoint by **requesting** the wiring
  change in the handoff note — the orchestrator owns `src/main.ts` / the router.

### 8. Frontend tests

- Vitest. Valid payload, invalid payload, HTTP error, loading, render, retry.
- `0` explicit `any`, `0` unsafe non-null assertions.

---

## Finish

1. Backend `mvn -q -DskipTests compile`, `mvn -q test`, `mvn -q verify` green.
2. Frontend `npm run build`, `npm test`, `npm run lint` green.
3. Run the slice **gate** from the phase file; keep the real output.
4. Write `docs/final-delivery/handoffs/<slug>.md` from the template; commit it.
5. Report to the orchestrator: branches, what landed per repo, verification run,
   cross-boundary requests (SecurityConfig matchers, new domain exceptions, router
   wiring), expected ArchUnit violation drop, anything left undone.
