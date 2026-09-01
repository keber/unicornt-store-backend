# T4 — REST API: commerce

**Worktree:** `../unicornt-worktrees/api-commerce`
**Branch:** `refactor/h4-api-commerce`, cut from `refactor/hito4` after T0
**Covers:** plan stage 4 (cart, orders, addresses), stage 5 applied to those
services, stage 7 annotations on those controllers
**Rubric:** completes **C1 = 3/3** together with T3
**Runs in parallel with:** T1, T2, T3

Read [../CONVENTIONS.md](../CONVENTIONS.md) first. You own the cart, order and
address slice only. You never edit `pom.xml`, `application*.yml`, `SecurityConfig`,
or any file whose name starts with `Product`, `Category` or `Auth`.

---

## Endpoints

```
GET    /api/v1/cart                200                          [USER]
POST   /api/v1/cart/items          201  {productId, qty}         [USER]
PATCH  /api/v1/cart/items/{id}     200  {qty}                    [USER]
DELETE /api/v1/cart/items/{id}     204                           [USER]

POST   /api/v1/orders              201  (confirms the cart)      [USER]  422 if out of stock
GET    /api/v1/orders              200                           [USER]
GET    /api/v1/orders/{id}         200 / 404                     [USER]

GET    /api/v1/addresses           200                           [USER]
POST   /api/v1/addresses           201 + Location                [USER]
DELETE /api/v1/addresses/{id}      204 / 404                     [USER]
```

## Conventions

Identical to T3 — read the "Conventions (binding)" section of
[T3-api-catalog.md](T3-api-catalog.md) and apply it verbatim. In particular: DTO
records as the contract, `@Valid` on every body, `201` with `Location` on creation,
`204` on deletion, static mappers.

Two rules specific to this slice:

- **The authenticated user is never a request parameter.** Resolve identity from the
  `SecurityContext` (`Authentication` or `@AuthenticationPrincipal`), never from a
  `userId` in the path or body — otherwise any user can read another user's cart.
- **An order or address that belongs to another user is a `404`, not a `403`.** Do
  not leak the existence of other users' resources.

## Files you create

```
infrastructure/web/rest/CartRestController.java
infrastructure/web/rest/OrderRestController.java
infrastructure/web/rest/AddressRestController.java
infrastructure/web/dto/CartDtos.java       (CartItemRequest, CartItemQuantityRequest, CartResponse, CartItemResponse)
infrastructure/web/dto/OrderDtos.java      (OrderResponse, OrderLineResponse)
infrastructure/web/dto/AddressDtos.java    (AddressCreateRequest, AddressResponse)
infrastructure/web/mapper/CartMapper.java
infrastructure/web/mapper/OrderMapper.java
infrastructure/web/mapper/AddressMapper.java
src/test/java/.../OrderRestControllerTest.java
```

## Files you may modify

`domain/service/CartService`, `CartServiceImpl`, the checkout service and the
address service. Required changes:

1. **Typed exceptions.** A missing cart item, order or address raises
   `ResourceNotFoundException`. The checkout stock check — currently
   `throw new RuntimeException(...)` — becomes `OutOfStockException(productId)`,
   which the global handler maps to `422 BUSINESS_RULE_VIOLATION`. These exception
   types already exist in `domain/exception`; do not redeclare them.
2. **Checkout is transactional.** Stock check, order creation and inventory
   decrement happen inside one `@Transactional` method. A partial order that
   decremented stock and then failed is a defect.
3. **Subtotal and total are computed in the service**, not in the controller and not
   in the mapper. Use `BigDecimal`, never `double`, for money.

The service layer must stay free of web types: no `org.springframework.web` or
`jakarta.servlet` import may appear under `domain/`.

### Boundary with T3

Checkout reads product stock, so it touches `ProductService` — which T3 owns and is
editing at the same time. Rules:

- Call `ProductService` through the signature that exists on your branch base. Do
  **not** edit `ProductService`, `ProductServiceImpl` or `ProductRepository`.
- If you need a method they do not have (for example a stock decrement), add it to
  your own service or repository, and record the request in your handoff note so the
  orchestrator can reconcile it after both branches merge.

## OpenAPI annotations (stage 7, done here)

`@Tag` per controller (`Cart`, `Orders`, `Addresses`), `@Operation` per method,
`@ApiResponses` including the `401`, `404` and `422` branches with
`@Schema(implementation = ErrorResponse.class)`, and `@Schema(example = ...)` on
every DTO record field.

## Tests

`@WebMvcTest` with mocked services, covering at least:

- `POST /api/v1/orders` when a product is out of stock → `422` with
  `code: BUSINESS_RULE_VIOLATION`
- `GET /api/v1/orders/{id}` for an id that is not the caller's → `404`
- `POST /api/v1/cart/items` with `qty: 0` → `400` with a populated `errors[]`
- `DELETE /api/v1/cart/items/{id}` → `204`

## Definition of Done

```bash
mvn -q -DskipTests compile     # green
mvn -q test                    # green
grep -rn "org.springframework.web\|jakarta.servlet" src/main/java/com/unicornt/store/domain   # empty
grep -rn "RuntimeException" src/main/java/com/unicornt/store/domain                            # empty
```

Every endpoint above responds with the listed status code, verified with `curl` or
Bruno against a running instance.

## Handoff note

Write `docs/refactor/handoffs/api-commerce.md` from the template. Record: every
method you needed from `ProductService` and whether it existed, any `SecurityConfig`
request matcher T2 has to carry, and how you resolved the authenticated user.
