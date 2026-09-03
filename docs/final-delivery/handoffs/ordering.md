# Handoff — P3 ordering slice (orchestrator-run)

**Repos / branches:** merged into `unicornt-store-backend @ final-delivery` and
`unicornt-store-frontend @ final-delivery`.
**Base:** backend `1add75c` · frontend `e4393ef`.
**Status:** complete, integrated, gate green.

## What landed

### Backend
- `domain/model`: `Order`, `OrderItem`, `OrderStatus`, `ShippingAddress` (all invariants
  in the constructor; `ShippingAddress` = value object, street/city/region required,
  zipCode optional; `Order.place` computes the total, born `CONFIRMED`).
- `domain/repository`: `OrderRepository` port + **new `StockRepository` port**
  (`decreaseStock(productId, qty) -> boolean`, a single conditional `UPDATE`).
- `application/usecase/ordering`: `PlaceOrderUseCase` (`@Transactional`), `GetOrderUseCase`,
  `ListOrdersUseCase`, `OrderConfirmation`. PlaceOrder: load priced cart → assert
  non-empty → per line decrement stock (fail → `OutOfStockException`, rolls back) →
  snapshot name+price → `Order.place` → `save` → `ClearCartUseCase`.
- `infrastructure/persistence`: `OrderJpaEntity`, `OrderItemJpaEntity` (renamed),
  `SpringDataOrderRepository`, `OrderPersistenceMapper`, `OrderRepositoryAdapter`
  (resolves principal email → numeric `user_id` via the legacy `UserRepository`),
  `SpringDataStockRepository` (renamed from `OrderStockRepository`), `StockRepositoryAdapter`.
- `infrastructure/web`: thin `OrderRestController` (`@PreAuthorize("isAuthenticated()")`,
  reads `java.security.Principal`), `OrderDtos` (`PlaceOrderRequest` with an inline
  `ShippingAddressRequest`, `OrderConfirmationResponse`, `OrderResponse`), `OrderRestMapper`.
- **Migration `V3__order_inline_shipping_address.sql`**: relaxes `orders.address_id`
  (drops the FK + NOT NULL), relaxes `shipping_address` NOT NULL, adds
  `ship_street/city/region/zip`. Wired into `application.yml` `schema-locations`.
- **Legacy `domain/service/**` deleted entirely** — `Checkout`, `Cart`, `Address`
  services + impls. The stored-address subsystem is gone too (`AddressRestController`,
  `AddressDtos`, `AddressMapper`, `AddressEntity`, `AddressRepository`) — shipping is
  state only (PLAN §1). `CartItemEntity`/`CartItemRepository` renamed to `*JpaEntity` /
  `SpringDataCartItemRepository`.
- Tests: `OrderTest`, `OrderItemTest`, `ShippingAddressTest`, `OrderStatusTest`,
  `PlaceOrderUseCaseTest` (asserts stock→snapshot→save→clear order via `inOrder`, and
  no save/clear on empty cart or stock failure), `GetAndListOrdersUseCaseTest`,
  `OrderPersistenceMapperTest`, `OrderRepositoryAdapterTest`, `StockRepositoryAdapterTest`,
  `OrderRestMapperTest`, `OrderRestControllerTest` (7). Deleted the migrated
  `CheckoutServiceImplTest` / `CartServiceImplTest` / `AddressServiceImplTest` /
  `AddressMapperTest` / `OrderMapperTest`.

### Frontend
- `src/adapters/checkoutForm.ts` — `FormData` boundary out of `checkout.model.ts`
  (reverses the H3 leak). Checkout form gains `street/city/region/zipCode` inputs.
- `src/api/order.api.ts` + `src/models/order.dto.ts`; `checkout.service.ts` posts only
  the address and validates the confirmation; `checkout.view.ts` success shows the
  order id + clears the cart, a 422 keeps the cart with an actionable message.
- Deleted the simulated `src/api/checkout.api.ts`.

## Verification

| Check | Result |
|-------|--------|
| backend `mvn -o clean verify` | **green** — 293 tests, 1 skip; jacoco "All coverage checks met"; ArchUnit green |
| frontend `npm run build` / `npm test` / `npm run lint` | **green** — 42 files, 231 tests |
| **main E2E gate** (live: PostgreSQL + jar) | **green** — see transcript |

### Gate transcript

```
stock before:  product 1 = 25, product 2 = 40
register 201 -> login -> token
cart: + product 1 x2, + product 2 x1   (itemCount 3, total 37970)
POST /api/v1/orders {"shippingAddress":{"street":"Av. Providencia 1234","city":"Santiago",
                     "region":"Region Metropolitana","zipCode":"7500000"}}
   -> 201  Location: /api/v1/orders/1   {"id":1,"status":"CONFIRMED","total":37970}
stock after:   product 1 = 23, product 2 = 39            <-- decremented atomically
GET /api/v1/cart   -> {"items":[],"itemCount":0,"total":0}   <-- cart cleared
GET /api/v1/orders/1 -> full order: status CONFIRMED, total 37970,
   shippingAddress {street,city,region,zipCode}, 2 snapshot lines
POST /api/v1/orders (empty cart) -> 400
```

## ArchUnit

Removing the entire `domain/service/**` legacy layer eliminated **every remaining
Group B violation**: the frozen store went from **407 lines to 0**. All Group B rules
are effectively hard now; P5 converts them to hard rules and deletes the store.
Group C catalog-scoped rules already cover the new `ordering` classes and stay green.

## Decisions

- **Stock port:** dedicated `StockRepository` (not a method on the catalog
  `ProductRepository`, which P3 must not touch). Adapter delegates to the renamed
  `SpringDataStockRepository` native `UPDATE ... WHERE stock >= :qty`.
- **Transaction boundary:** `@Transactional` on `PlaceOrderUseCase` itself (same
  guide-layout stance as P1/P2 use cases). No separate infrastructure facade.
- **Inline address, no subsystem:** `POST /orders` carries the address; V3 relaxes the
  old `address_id` FK. The address CRUD subsystem is deleted (PLAN §1 "state only").
- **`OrderRestController` reads `java.security.Principal`** (not `Authentication`) so
  the `@WebMvcTest(addFilters=false)` slice can supply the principal with `.principal(...)`.
- Empty cart → `IllegalArgumentException` → 400 (matches the legacy behaviour and
  `GlobalExceptionHandler`'s existing mapping). Out of stock → `OutOfStockException` →
  422 `BUSINESS_RULE_VIOLATION` (existing handler).

## Requests for the orchestrator

- `GlobalExceptionHandler` / `domain/exception`: **no change** — `OutOfStockException`
  (422), `ResourceNotFoundException` (404), `IllegalArgumentException` (400) already cover it.
- `SecurityConfig`: **no change** — `/api/v1/orders/**` is covered by
  `anyRequest().authenticated()` + `@PreAuthorize` on the controller.
- `pom.xml` / `package.json`: untouched.
