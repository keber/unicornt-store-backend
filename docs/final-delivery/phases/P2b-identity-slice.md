# P2b — Identity slice

**Worktree (backend):** `../unicornt-worktrees/fd-identity`
**Worktree (frontend):** `../unicornt-frontend-worktrees/fd-identity`
**Branch:** `fd/identity`, cut from the tip of `final-delivery` after P1
**Plan:** [PLAN.md](../PLAN.md) §4 Phase 2 (Identity)
**Rubric:** dimensions 1 and 2
**Runs in parallel with:** P2a (cart)
**Follow:** [../slice-recipe.md](../slice-recipe.md)

Read [../CONVENTIONS.md](../CONVENTIONS.md) and the merged P1 slice as your reference
pattern. You own `identity/**`, `infrastructure/security/**`, and `*Auth*` web files.
You never touch `pom.xml`, `package.json`, or any `Product*`, `Category*`, `Cart*`,
`Order*`, `Address*` file.

Keep the existing JWT. Do not add refresh tokens, OAuth, or session cookies.

---

## Scope

Backend: `User` domain model, `RegisterUserUseCase`, the JWT/security wiring moved
under the new layout. Frontend: a single auth client boundary.

## Backend (recipe steps 1–5, lighter)

1. **Domain** — `domain/model/User` (id, name, email, roles; `password` stays a
   hash, never plain). Rules (PLAN.md §2.4): `password.length ≥ 6` (checked before
   hashing, in the use case), email uniqueness (use case + repository), required
   fields. `domain/valueobject/Email` optional — add it only if it earns its place.
   Tests: `UserTest` (+ `EmailTest` if added). Migrate `UserServiceTest` (behaviour).
2. **Port** — `domain/repository/UserRepository`: `findByEmail`, `existsByEmail`,
   `save`. Plus a `RoleRepository` if the default-role lookup needs one.
3. **Use case** — `application/usecase/identity/RegisterUserUseCase` (validate →
   check uniqueness → hash via a `PasswordHasher` port → assign `ROLE_USER` → save).
   Login stays in `AuthRestController` + Spring's `AuthenticationManager` — it is a
   framework concern, not a use case. Tests: `RegisterUserUseCaseTest` (Mockito).
4. **Persistence** — `UserJpaEntity`, `RoleJpaEntity` (rename), Spring Data repos,
   `UserPersistenceMapper`, `UserRepositoryAdapter`. `PasswordEncoder`-backed
   `PasswordHasher` adapter in `infrastructure`.
5. **Security + web** — move `JwtService`, `JwtAuthFilter`, `SecurityConfig`,
   `CustomUserDetailsService` under `infrastructure/security/` in the new layout;
   `AuthRestController` (`register`, `login`, `me`), `AuthDtos`. Wire the P0
   `CorsConfig` bean into `SecurityConfig`. Own the request-matcher rules for the
   whole app: public `GET /api/v1/products*`, `GET /api/v1/categories`,
   `POST /api/v1/auth/**`; everything else authenticated; `hasRole('ADMIN')` for the
   product write matchers **as requested by P1/P4 in their handoffs**. Migrate
   `JwtServiceTest`, `JwtAuthFilterTest`, `CustomUserDetailsServiceTest`,
   `SecurityChainTest`, `GlobalExceptionHandlerTest` auth cases.

Endpoints:

```
POST /api/v1/auth/register     201
POST /api/v1/auth/login        200   { token, expiresIn }
GET  /api/v1/auth/me           200 / 401
```

## Frontend (recipe steps 6–8)

- `src/api/http.ts` (`apiFetch`) exists from P0 — extend it: attach
  `Authorization: Bearer <token>` from token storage, handle `401` centrally.
  Do not replicate the header anywhere else.
- `src/api/auth.api.ts` (`register`, `login`), `src/services/auth.service.ts`,
  token storage module. Login/register views with safe DOM + `preventDefault`.
- Expose an auth-success hook other modules subscribe to (P2a's cart merge needs it);
  keep the hook name in your handoff.
- Vitest: valid login → token stored → protected call carries the header; invalid
  login → visible error; `401` → central handling.

## ArchUnit

Do not edit the baseline. Report the expected drop for `..identity..` and
`infrastructure.security..` in the handoff.

---

## Definition of Done

```bash
mvn -q -DskipTests compile && mvn -q test
grep -rn "org.springframework\|jakarta.persistence" \
  src/main/java/com/unicornt/store/domain/model/User.java     # empty
npm run build && npm test && npm run lint
```

## Gate

Valid login → token → `GET /api/v1/auth/me` 200 and a protected endpoint works;
invalid login → visible error in the UI. Capture the transcript.

## Handoff

`docs/final-delivery/handoffs/identity.md`. Record: the final `SecurityConfig`
matcher list (this is the authoritative one the orchestrator keeps), the
`PasswordHasher` port, the auth-success hook name for P2a, the token storage key,
and the expected ArchUnit drop.
