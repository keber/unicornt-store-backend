# P2a — Cart slice

**Worktree (backend):** `../unicornt-worktrees/fd-cart`
**Worktree (frontend):** `../unicornt-frontend-worktrees/fd-cart`
**Branch:** `fd/cart`, cut from the tip of `final-delivery` after P1
**Plan:** [PLAN.md](../PLAN.md) §4 Phase 2 (Cart)
**Rubric:** dimensions 1 and 2
**Runs in parallel with:** P2b (identity)
**Follow:** [../slice-recipe.md](../slice-recipe.md)

Read [../CONVENTIONS.md](../CONVENTIONS.md), `docs/final-delivery/CONTRACT.md`, and
the merged P1 slice as your reference pattern. You own `cart/**` only. You never
touch `pom.xml`, `package.json`, `SecurityConfig`, or any file whose name matches
`Product*`, `Category*`, `Order*`, `Address*`, `Auth*`, `User*`.

---

## Scope

Backend cart aggregate + `POST /api/v1/cart/merge`. Frontend anonymous→authenticated
cart transition.

## Backend (recipe steps 1–5)

1. **Domain** — `domain/model/Cart`, `CartItem`. Rules (PLAN.md §2.4):
   `quantity > 0`; same product → sum; `quantity == 0` → remove; ownership check is
   the use case's job but `Cart` exposes it (`Cart` is scoped to a `userId`). Reuse
   `domain/valueobject/Quantity` from P1. Tests: `CartTest`.
2. **Port** — `domain/repository/CartRepository`: `findByUserId`, `save`,
   `deleteByUserId`. Domain types only.
3. **Use cases** — `application/usecase/cart/`: `GetCartUseCase`, `AddCartItemUseCase`,
   `UpdateCartItemUseCase`, `RemoveCartItemUseCase`, `ClearCartUseCase`,
   `MergeCartUseCase`. `MergeCartUseCase`: for each incoming
   `{ productId, quantity }`, `server_qty + local_qty`, clamped to available stock
   (read via `ProductRepository` from P1 — depend on the port, not the impl).
   Tests migrate `CartServiceImplTest` (22) scenarios → `CartTest` + `*UseCaseTest`.
4. **Persistence** — `CartItemJpaEntity` (rename from existing), `SpringDataCartItemRepository`,
   `CartPersistenceMapper`, `CartRepositoryAdapter`. Adapter assembles a `Cart`
   aggregate from the item rows.
5. **Web** — `CartRestController` (thin), `CartDtos` (records + `@Schema`),
   `CartRestMapper`. `@PreAuthorize("isAuthenticated()")`. Migrate
   `CartRestControllerTest` with `@MockitoBean` of the use cases.

Endpoints:

```
GET    /api/v1/cart                         200
POST   /api/v1/cart/items                   200/201   { productId, quantity }
PUT    /api/v1/cart/items/{productId}       200       { quantity }
DELETE /api/v1/cart/items/{productId}       204
POST   /api/v1/cart/merge                   200       { items: [{ productId, quantity }] }
```

## Frontend (recipe steps 6–8)

- Keep the existing `localStorage` anonymous cart (`src/storage/cart.storage.ts`).
  No backend call while unauthenticated.
- `api/cart.api.ts` via `apiFetch`; `models/cart.dto.ts` + validator;
  `toCartModel`.
- Transition on login: `POST /api/v1/cart/merge` with the local items → response
  becomes the source of truth → clear the local cart. Wire this into the auth
  success path (P2b owns the auth client; **request** the hook in your handoff —
  do not edit auth files).
- Vitest: merge request built from local items; local cart cleared after a
  successful merge; authenticated cart reads from the API.

## ArchUnit

Do not edit the baseline. Report the expected violation drop for `..cart..` and
`Cart*` persistence classes in the handoff; the orchestrator regenerates the store.

---

## Definition of Done

```bash
# backend worktree
mvn -q -DskipTests compile && mvn -q test
grep -rn "org.springframework\|jakarta.persistence" \
  src/main/java/com/unicornt/store/domain/model/Cart*.java   # empty
# frontend worktree
npm run build && npm test && npm run lint
```

## Gate

Add a product while logged out → log in → the product is present in the authenticated
cart with the summed quantity. Capture the transcript.

## Handoff

`docs/final-delivery/handoffs/cart.md`. Record: `CartRepository` signature, the
merge stock-clamp decision, the auth-success hook you need P2b / the orchestrator to
wire, any `SecurityConfig` matcher for `/api/v1/cart/**`, and the expected ArchUnit
drop.
