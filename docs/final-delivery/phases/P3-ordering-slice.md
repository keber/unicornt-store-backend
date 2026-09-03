# P3 — Ordering slice (the main E2E)

**Worktree (backend):** `../unicornt-worktrees/fd-ordering`
**Worktree (frontend):** `../unicornt-frontend-worktrees/fd-ordering`
**Branch:** `fd/ordering`, cut from the tip of `final-delivery` **after P2a and P2b
are merged**
**Plan:** [PLAN.md](../PLAN.md) §4 Phase 3
**Rubric:** dimensions 1 and 2 — this slice carries the main smoke test
**Follow:** [../slice-recipe.md](../slice-recipe.md)

Read [../CONVENTIONS.md](../CONVENTIONS.md) and the merged Catalog + Cart slices.
You own `ordering/**` and `*Address*`. You never touch `pom.xml`, `package.json`,
`SecurityConfig`, or `Product*` / `Cart*` / `Auth*` files — you consume their
`domain.repository` ports and use cases.

---

## Scope

The `PlaceOrder` use case end to end: cart → validated order → stock decrement →
cart cleared → confirmation, atomically.

## Backend (recipe steps 1–5)

1. **Domain** — `domain/model/Order`, `OrderItem`, `OrderStatus`, `ShippingAddress`.
   `Order` holds a **snapshot** per line: `productId`, `productName`, `unitPrice`,
   `quantity`, `subtotal`; a `total`; a `status`. Reuse `Money`, `Quantity`.
   `ShippingAddress` validates `street`/`city`/`region` required (PLAN.md §2.4).
   Tests: `OrderTest`, `ShippingAddressTest`.
2. **Port** — `domain/repository/OrderRepository`: `save`, `findByIdAndUserId`,
   `findByUserIdOrderByCreatedAtDesc`. A stock port: reuse P1's `ProductRepository`
   with a `decreaseStock(productId, qty) -> boolean` method, or a dedicated
   `StockRepository` port if cleaner — decide and record it.
3. **Use case** — `application/usecase/ordering/PlaceOrderUseCase`:
   `load cart (CartRepository) → assert non-empty → for each line: resolve product,
   check + decrement stock (fail → OutOfStockException) → snapshot price/name →
   accumulate total → build Order → OrderRepository.save → ClearCartUseCase →
   return confirmation`. Plus `GetOrderUseCase`, `ListOrdersUseCase`.
   Tests migrate `CheckoutServiceImplTest` (10) → `OrderTest` + `PlaceOrderUseCaseTest`
   (Mockito over `CartRepository`, `OrderRepository`, stock port); assert `save`
   precedes cart clear, and that nothing is saved / decremented when stock fails or
   the cart is empty.
4. **Persistence** — `OrderJpaEntity`, `OrderItemJpaEntity`, `AddressJpaEntity`
   (rename), Spring Data repos, `OrderPersistenceMapper`, `AddressPersistenceMapper`,
   `OrderRepositoryAdapter`, `AddressRepositoryAdapter`. Migrate `AddressServiceImplTest`.
5. **Web** — `OrderRestController`, `AddressRestController` (thin), `OrderDtos`,
   `AddressDtos`, mappers. `@PreAuthorize("isAuthenticated()")`. The Spring
   `@Transactional` boundary wraps the `PlaceOrderUseCase` call (in an
   `infrastructure` `@Component` facade if keeping the use case Spring-free).
   Migrate `OrderRestControllerTest`.

Endpoints:

```
POST /api/v1/orders     201   { shippingAddress: { street, city, region, zipCode } }  → { id, status: "CONFIRMED", total }
GET  /api/v1/orders/{id} 200 / 404
GET  /api/v1/orders      200    (only if the frontend lists them)
```

## Frontend (recipe steps 6–8)

- `api/order.api.ts` via `apiFetch`; `models/checkout.dto.ts` / `order.model.ts` +
  mappers. Reverse the H3 leak: the checkout model must not import `FormData`;
  a `src/adapters/checkoutForm.ts` (or `src/forms/`) extracts a plain
  `RawCheckoutInput` from the form, then the pure domain validator runs.
- Checkout view reuses the H2 `submitting / success / error` states. Success →
  confirmation with the order id + clear the visible cart. Stock error → actionable
  message, keep the cart.
- Vitest: happy path posts the address; stock error path preserves the cart;
  form extraction is a pure function tested without the DOM.

## ArchUnit

Do not edit the baseline. Report the expected drop for `..ordering..` and
`Order*` / `Address*` persistence classes.

---

## Definition of Done

```bash
mvn -q -DskipTests compile && mvn -q test
grep -rn "org.springframework\|jakarta.persistence" \
  src/main/java/com/unicornt/store/domain/model/Order*.java \
  src/main/java/com/unicornt/store/domain/model/ShippingAddress.java   # empty
npm run build && npm test && npm run lint
```

## Gate (main smoke test)

`Browser → cart → login → checkout → POST /api/v1/orders → PostgreSQL →
stock decremented → cart empty → UI shows CONFIRMED`. Verify the stock row before and
after in the DB. Capture the full transcript — this is the application's headline
demonstration.

## Handoff

`docs/final-delivery/handoffs/ordering.md`. Record: the stock port decision
(`ProductRepository.decreaseStock` vs a dedicated port), the transaction facade
placement, any new `@ExceptionHandler` requested from P0's `GlobalExceptionHandler`,
`SecurityConfig` matchers for `/api/v1/orders/**`, the frontend form-adapter path,
and the expected ArchUnit drop.
