# Handoff — P2a cart

**Repos / branches:** `unicornt-store-backend @ fd/cart` · `unicornt-store-frontend @ fd/cart`
**Base commit(s):** backend `3b809de` · frontend `7c9a693` (tip of `final-delivery` after P1)
**Status:** complete (one expected frozen-baseline drift on `packages_are_free_of_cycles` — the orchestrator regenerates the store, see ArchUnit section)

## What landed

### Backend (new cart aggregate + `/api/v1/cart` over use cases)

- `domain/model/Cart` — aggregate scoped to a `userId` (the principal identity string).
  Mutations `addItem` (sums into an existing line), `setItemQuantity` (absolute; `<= 0`
  removes), `removeItem` (returns hit/miss), `clear`. Invariants: non-blank owner,
  one line per product, strictly positive quantities (via `Quantity`).
- `domain/model/CartItem` — `productId` + `Quantity`. Package-private `withAdditionalUnits`
  / `withQuantity`. No price/name (read-model data joined in by the application layer).
- `domain/repository/CartRepository` — port: `Cart findByUserId(String userId)`,
  `Cart save(Cart)`, `void deleteByUserId(String userId)`. Domain types only.
- `application/usecase/cart/` — `GetCartUseCase`, `AddCartItemUseCase` (returns
  `Result(PricedCart, boolean created)`), `UpdateCartItemUseCase`, `RemoveCartItemUseCase`,
  `ClearCartUseCase`, `MergeCartUseCase` (+ `IncomingItem` record), and `PricedCart`
  (`+ Line`) — the priced read model, assembled against `ProductRepository`, dropping
  lines whose product no longer exists. Spring `@Service` / `@Transactional`, same as
  the catalog slice; no `org.springframework.data` import.
- `infrastructure/persistence/mapper/CartPersistenceMapper` — static `CartItemEntity`
  ↔ `CartItem` / `Cart`.
- `infrastructure/persistence/adapter/CartRepositoryAdapter` — `@Component implements
  CartRepository`. Delegates to the **existing** `CartItemRepository` (Spring Data) and
  resolves the principal email → numeric `cart_items.user_id` via the existing
  `infrastructure.persistence.repository.UserRepository`. `save` diffs the row set
  (insert new / update changed / delete removed).
- `infrastructure/web/rest/CartRestController` — thin, `@PreAuthorize("isAuthenticated()")`,
  `@Tag`/`@Operation`/`@ApiResponses`. Reads `Authentication.getName()` as the userId.
  `GET /cart` · `POST /cart/items` (201+Location when a new line, 200 when merged) ·
  `PUT /cart/items/{productId}` (200, quantity 0 removes, 404 if not a line) ·
  `DELETE /cart/items/{productId}` (204 / 404) · `POST /cart/merge` (200). Every
  mutating endpoint answers with the whole `CartResponse`.
- `infrastructure/web/dto/CartDtos` — rewritten: `AddCartItemRequest{productId,quantity}`,
  `UpdateCartItemRequest{quantity}` (`@PositiveOrZero`), `MergeCartRequest{items[]}`
  (`@NotEmpty`, `List<@Valid MergeCartItem>`), `CartItemResponse{productId,productName,
  imageBase,unitPrice,quantity,subtotal}` (CLP ints), `CartResponse{items,itemCount,total}`.
  `@Schema` on every field. Field name is `quantity` everywhere, never `qty`.
- `infrastructure/web/mapper/CartRestMapper` — new (static). **Deleted** the old
  `CartMapper` + `CartMapperTest` (replaced).
- Tests: `CartTest`, one `*UseCaseTest` per use case (+ `CartUseCaseFixtures`),
  `CartPersistenceMapperTest`, `CartRepositoryAdapterTest`, `CartRestMapperTest`,
  `CartRestControllerTest` rewritten (standalone MockMvc + `@Mock` use cases, the
  pattern the migrated test already used). 65 new cart tests. Coverage:
  `application.usecase.cart` 100%/100% instr/branch; `Cart` 100%/100%; `CartItem`
  100%/100%; adapter/mapper/controller/DTO all ≥ 95%.

The 22 `CartServiceImplTest` scenarios are re-expressed across `CartTest` +
`*UseCaseTest` + `CartRestControllerTest` (get/price/roll-up, empty cart, deleted
product dropped, add new line, add sums, non-positive quantity rejected, missing
product 404, update replaces, update 0 removes, unknown line 404, remove, remove
unknown 404, clear). The "line owned by another user → 404" scenario no longer
applies: every endpoint is keyed by the caller's own `userId` + productId, there is
no cross-user line id to pass.

### Frontend (anonymous → authenticated cart transition)

- `src/models/cart.dto.ts` — `CartItemDto` / `CartDto` + `isCartItemDto` / `isCartDto`
  runtime validators; `toCartModel(dto)` maps `{productId,quantity}` → the legacy
  `{id,qty}` `CartModel` so `storage/cart.storage.ts`, `cart.view.ts` and `CartPanel`
  consume it unchanged; `CartMergeItem` type + `toMergeItems(localItems)`.
- `src/api/cart.api.ts` — `fetchCartPayload()` (`GET /api/v1/cart`), `mergeCartPayload(items)`
  (`POST /api/v1/cart/merge`), both via the shared `apiFetch`, returning `unknown`.
- `src/services/cart.sync.ts` — `fetchRemoteCart()` and `mergeLocalCart(localItems)`:
  call the api module, validate, map to `CartModel`. Pure (no `fetch`/`window`/
  `localStorage`). `mergeLocalCart([])` falls back to `fetchRemoteCart()` (nothing to
  merge, and the backend rejects an empty `items[]`).
- The existing `localStorage` anonymous cart (`storage/cart.storage.ts`, the pure
  `services/cart.service.ts`, `views/cart.view.ts`, `CartPanel`) is **untouched** — no
  backend call while logged out.
- Tests: `src/models/cart.dto.test.ts`, `src/api/cart.api.test.ts`,
  `src/services/cart.sync.test.ts` (valid payload, invalid payload, HTTP error,
  merge request built from local items, empty-local fallback). 19 new tests. English.
  0 explicit `any`, 0 non-null assertions.

## Verification actually run

| Check | Command | Result |
|-------|---------|--------|
| backend compile | `mvn -q -o -DskipTests compile` | pass |
| backend tests | `mvn -o test` | **296 pass / 1 fail** — only `DependencyRulesFrozenTest.packages_are_free_of_cycles` (expected frozen-baseline drift, see below). Every cart test, every catalog/security/other test, and every other ArchUnit rule pass. 1 skipped (pre-existing). |
| backend verify | `mvn -o verify -Dmaven.test.failure.ignore=true` | `jacoco:check` (BUNDLE 92%/88%) **pass**; report generated; only the same one test fails |
| frontend build | `npm run build` (`tsc --noEmit && vite build`) | pass |
| frontend tests | `npm test` (`vitest run`) | pass — 34 files, 196 tests |
| frontend lint | `npm run lint` (`eslint .`) | pass (exit 0) |
| slice gate | live backend (dev profile) + PostgreSQL + `curl` | pass — transcript below |

### `packages_are_free_of_cycles` failure (the only red)

```
DependencyRulesFrozenTest.packages_are_free_of_cycles
Architecture Violation - Rule 'no package cycles between the top-level slices' was violated (2 times):
Cycle detected: Slice application -> Slice domain -> Slice infrastructure -> Slice application
Cycle detected: Slice domain -> Slice infrastructure -> Slice domain
```

Same situation CONTRACT.md §5 records for P1 ("the cycle-edge count rose 47 → 117
because the new application layer now participates in the domain ↔ infrastructure
cycle through the surviving legacy `domain.service.*`"). The two cycles are caused
by legacy `domain.service.{CartServiceImpl,CheckoutServiceImpl,AddressServiceImpl,
UserServiceImpl}` → `infrastructure.persistence.repository.*` (P2b/P3 territory, not
touched here). Adding the new `application.usecase.cart.*` + `infrastructure` cart
classes — all with legal `application→domain` / `infrastructure→application` /
`infrastructure→domain` edges — enumerates ~26 new dependency lines inside those two
frozen cycle violations, so the frozen text no longer matches and the ratchet
reports it as new. No other frozen rule is disturbed (`domain -X→ infrastructure`
129, `domain -X→ spring` 43, `LayeredArchitecture` 168 — all unchanged); the new
code adds **zero** genuine dependency-rule or layer violations. Group C
`TargetArchitectureRulesTest` stays green (catalog rules only; no cart Group C rule
exists yet).

**Action for the orchestrator:** regenerate `src/test/resources/archunit_store/`
after integrating this branch (`-Darchunit.freeze.refreeze=true` on one `mvn test`),
exactly as after P1. The store is not in this diff.

### Gate transcript (logged-out add → login → merge → summed quantity)

```
# 1. catalog, logged out:  products 1 (stock 25), 2 (stock 40), 3 (stock 15)
# 2. GET /api/v1/cart with no token                       -> HTTP 401

# 3. POST /api/v1/auth/register + /login  (fresh user)    -> 201, token acquired
# 4. as the user, POST /api/v1/cart/items {productId:1,quantity:2}
#      -> 201  {"items":[{productId:1,...,quantity:2,subtotal:29980}],itemCount:2}

# 5. anonymous localStorage cart held while logged out:
#      unicornt_cart = [{"id":1,"qty":3},{"id":2,"qty":1}]
# 6. on login the client POSTs /api/v1/cart/merge {"items":[{productId:1,quantity:3},{productId:2,quantity:1}]}
#      -> 200  {"items":[
#            {productId:1,productName:"Classic Unicorn T-shirt",unitPrice:14990,quantity:5,subtotal:74950},
#            {productId:2,productName:"Rainbow Mug",unitPrice:7990,quantity:1,subtotal:7990}],
#          itemCount:6,total:82940}
#      >>> product 1: server 2 + local 3 = quantity 5  (SUMMED)
# 7. GET /api/v1/cart  -> 200  identical body (server cart is now the source of truth)

# 8. stock clamp:  merge /api/v1/cart/merge {"items":[{productId:1,quantity:999}]}
#      -> product 1 quantity -> 25   (clamped to stock 25)
# 9. PUT /api/v1/cart/items/1 {"quantity":0}   -> 200, line 1 removed
# 10. DELETE /api/v1/cart/items/999 (not a line) -> 404 {"code":"RESOURCE_NOT_FOUND","message":"Cart item not found: 999"}
# 11. POST /api/v1/cart/items {"productId":2,"quantity":0} -> 400 {"code":"VALIDATION_ERROR","errors":[{"field":"quantity","message":"quantity must be greater than 0"}]}
```

A browser render of the transition was not captured (no headless browser here, and
the frontend auth client that would call `mergeLocalCart` is P2b's). The contract is
proven end to end against a live backend + PostgreSQL; the frontend `cart.sync`
builds exactly the `POST /cart/merge` body shown and validates the response with
`isCartDto` (same shape as the Vitest fixtures).

## ArchUnit

| Rule (Group B, frozen) | Before (final-delivery) | After this slice |
|------|-------:|------:|
| `domain -X→ org.springframework..` | 43 | 43 (unchanged) |
| `domain -X→ jakarta.persistence..` | 0 | 0 |
| `domain -X→ jakarta.validation..` | 0 | 0 |
| `domain -X→ ..infrastructure..` | 129 | 129 (unchanged) |
| `application -X→ ..infrastructure..` | 0 | 0 |
| `application -X→ org.springframework.data..` | 0 | 0 |
| `LayeredArchitecture` | 168 | 168 (unchanged) |
| `no package cycles between the top-level slices` | 117 (2 cycles) | text churns: ~26 `..usecase.cart..` / cart-`infrastructure` dependency lines now enumerated inside the same 2 legacy cycles; **no new cycle, no new edge type** — orchestrator must refreeze |

Group C: no cart-scoped rule exists yet (only catalog rules are enabled). When P3
removes `CartServiceImpl` / `CheckoutServiceImpl`, the `domain.service ↔
infrastructure.persistence` cycle collapses and the cycle count drops sharply
(toward 0), taking the cart lines with it.

The baseline file is **not** in this diff.

## Requests for the orchestrator

| File / area | Change needed | Why |
|-------------|---------------|-----|
| `src/test/resources/archunit_store/**` | Regenerate after integrating `fd/cart` (`mvn test -Darchunit.freeze.refreeze=true`, then commit on `final-delivery`). | New cart classes change the enumerated membership of the two pre-existing legacy package cycles; identical to the post-P1 refreeze. Nothing else in the store moves. |
| `SecurityConfig` | **No change required.** `/api/v1/cart/**` is already covered by the `anyRequest().authenticated()` default + the controller's `@PreAuthorize("isAuthenticated()")`. | Confirmed against the live filter chain (GET /api/v1/cart with no token → 401). |
| `GlobalExceptionHandler` / `domain/exception/**` | **No change required.** Reuses `ResourceNotFoundException` (404) and bean-validation → `VALIDATION_ERROR` (400). No new domain exception. | — |
| frontend router / `src/main.ts` | **No change required for this slice.** There is no `src/main.ts` (multi-page); the cart view is already wired by `catalog.main.ts` / `product.main.ts` and stays on `localStorage` while anonymous. | — |
| frontend auth-success hook (P2b owns `src/api/auth*` / `src/services/auth*`) | On successful login, call `mergeLocalCart(readCart().items)` from `@/services/cart.sync`, then `writeCart(EMPTY_CART)` (from `@/storage/cart.storage` + `@/models/cart.model`) so the server cart becomes the source of truth and the local cart is cleared. On failure, keep the local cart. | The transition must run from the auth flow, which P2a does not own. `mergeLocalCart` is pure and ready; it needs one call site. |
| frontend token provider (P2b owns token storage) | Register the token getter with `setAuthTokenProvider(fn)` from `@/api/http` so `cart.api.ts` sends `Authorization: Bearer …`. | `apiFetch` only attaches the header when a provider is registered; P2a's cart calls are authenticated. |
| P3 ordering | When `CheckoutServiceImpl` migrates, retire legacy `domain/service/CartService(+Impl)` and `CartServiceImplTest`, move checkout onto `application.usecase.cart` (`GetCartUseCase` / `ClearCartUseCase`) or a dedicated read use case, and rename `CartItemEntity → CartItemJpaEntity` / `CartItemRepository → SpringDataCartItemRepository`. | See "Decisions taken". |

## Decisions taken

- **`CartRepository` is keyed by `String userId` (the principal identity / email), not a
  numeric id.** The JWT subject and `Authentication.getName()` are the email; there is
  no domain `UserRepository` port yet (P2b). The controller can't resolve email→id
  without a `web → persistence` edge (a hard ArchUnit rule). So the port takes the
  principal string and `CartRepositoryAdapter` translates it to `cart_items.user_id`
  via the existing legacy `UserRepository`. `Cart.userId()` is that string.
- **Legacy `domain/service/CartService` + `CartServiceImpl` + `CartServiceImplTest` are
  kept untouched.** They are outside P2a's ownership set (`domain/service/**` is not
  listed) and are still consumed by P3's `CheckoutServiceImpl` (`getCartItems`,
  `clearCart`); deleting them would break the build. The new aggregate runs alongside;
  the REST API uses only the new use cases.
- **No `CartItemEntity → CartItemJpaEntity` / `CartItemRepository → SpringDataCartItemRepository`
  rename**, though the phase file asks for it. The rename forces edits to
  `CheckoutServiceImpl`, `CheckoutServiceImplTest`, `CartServiceImpl`,
  `CartServiceImplTest` — all outside P2a's ownership. The new `CartRepositoryAdapter`
  reuses the existing entity/repository names; they are ArchUnit-clean (the
  `*JpaEntity` predicate only gates `usecase` classes, and cart use cases touch no
  entity). Left for P3 (request above).
- **Every mutating cart endpoint returns the whole `CartResponse`** (not just the
  touched line). Consistent, and the frontend transition wants the full cart. `POST
  /cart/items` still distinguishes 201 (+`Location: /api/v1/cart/items/{productId}`)
  for a new line from 200 for a merged quantity, per the phase file's "200/201".
- **`PUT /cart/items/{productId}` is keyed by product id**, not a cart-line id (the
  legacy `PATCH /items/{cartItemId}`). Matches CONTRACT.md and removes the notion of a
  client-visible line id. `quantity: 0` removes; the product must already be a line
  (else 404).
- **Merge stock clamp:** for each incoming `{productId, quantity}` the new quantity is
  `server_qty + local_qty`, then `min(that, product.stock())`. Incoming lines whose
  product no longer exists, or whose clamped quantity would be `<= 0` (no stock), are
  dropped silently (a stale local cart must not 404 the whole login). The clamp can
  lower a pre-existing server line if stock has since fallen below it.
- **`GetCartUseCase` prices the cart and drops lines whose product was deleted** (as
  the legacy `getCart` did). `ClearCartUseCase` has no REST endpoint — it exists for
  P3's checkout to call after an order is confirmed.
- **Frontend model keeps the legacy `{id, qty}` shape.** `toCartModel` bridges the
  backend `{productId, quantity}` so `cart.storage`, `cart.view` and `CartPanel` need
  no change — same call P1 made for `ProductModel`.
- **`CartRestControllerTest` uses standalone MockMvc** (like the test it migrates), not
  `@WebMvcTest`: it pins the HTTP contract and supplies the principal explicitly, with
  no dependency on the P2b `SecurityConfig`.

## Known gaps

- **`packages_are_free_of_cycles`** fails on this branch — expected baseline drift, the
  orchestrator refreezes at integration (see above). Nothing else is red.
- **Live browser render of the anonymous→authenticated transition not captured** — no
  headless browser here and the auth client that calls `mergeLocalCart` is P2b's. The
  HTTP contract is proven end to end against a live backend; `cart.sync` + its Vitest
  fixtures match that contract.
- **Two cart implementations coexist** (legacy `CartServiceImpl` for checkout; new
  aggregate for the API) until P3 unifies them. No functional overlap — different
  beans, same `cart_items` table, both consistent.
- **`CartItemEntity` / `CartItemRepository` keep their pre-`*JpaEntity` names** until
  P3 (request above).

## Attribution check

```
git log --format='%an <%ae>%n%B' final-delivery..HEAD | grep -iE 'claude|anthropic|co-authored|generated with'
```

Result — backend: empty · frontend: empty.
