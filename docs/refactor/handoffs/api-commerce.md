# Handoff — T4 api-commerce

**Branch:** `refactor/h4-api-commerce`
**Base commit:** `2dd80ae7c6a35414c98657fbb71a03272724b9a1` (tip of `refactor/hito4` at branch cut)
**Status:** complete

## What landed

- `CartService`/`CartServiceImpl` rewritten to a priced-line API (`CartLine`, `CartView`)
  computed in `BigDecimal`, with typed `ResourceNotFoundException` on missing product or
  on a cart line that does not belong to the caller (never a `403`, always a `404`).
- `AddressServiceImpl` typed-exception cleanup: `"AddressEntity"`/`"UserEntity"` labels in
  `ResourceNotFoundException` normalized to `"Address"`/`"User"` for consistent error messages.
- New `OrderEntity` / `OrderItemEntity` JPA entities and `OrderRepository` /
  `OrderStockRepository` — orders and stock did not exist before this task (see the schema
  note below).
- `CheckoutService`/`CheckoutServiceImpl` rewritten: `confirm(userEmail, addressId)` now
  validates a non-empty cart, checks address ownership, atomically decrements stock per line
  (raising `OutOfStockException(productId)` when a line has insufficient stock), persists an
  `OrderEntity` with frozen line prices, clears the cart, and returns the saved order — all
  inside one `@Transactional` method, so a failed line never leaves a partial stock decrement.
  Added `findOrders(userEmail)` and `findOrder(userEmail, orderId)` (ownership-checked,
  `404` on mismatch).
- `CartRestController`, `OrderRestController`, `AddressRestController` under
  `infrastructure/web/rest`, each `@PreAuthorize("isAuthenticated()")` at the class level,
  identity resolved from `Authentication.getName()` (never from path/body), full
  `@Tag`/`@Operation`/`@ApiResponses` coverage including `401`/`404`/`422` branches pointing
  at `ErrorResponse`.
- `CartDtos`, `OrderDtos`, `AddressDtos` — immutable records with `@Schema(example = ...)` on
  every field and `jakarta.validation` annotations on every request record.
- `CartMapper`, `OrderMapper`, `AddressMapper` — static, no MapStruct, per the T3 convention
  this task inherits.
- `OrderRestControllerTest` and `CartRestControllerTest` — standalone `MockMvc` slices (see
  "Decisions taken" for why standalone instead of `@WebMvcTest`), covering: `POST /orders`
  out-of-stock -> `422 BUSINESS_RULE_VIOLATION`; `POST /orders` success -> `201` + `Location` +
  body; `POST /orders` missing `addressId` -> `400` with populated `errors[]`;
  `GET /orders/{id}` for another user's id -> `404`; `GET /orders` list; `POST /cart/items`
  success -> `201` + `Location`; `qty: 0` -> `400` with populated `errors[]`;
  `DELETE /cart/items/{id}` -> `204`; delete of another user's line -> `404`.

## Verification actually run

| Check | Command | Result |
|-------|---------|--------|
| compile | `mvn -q -DskipTests compile` | pass |
| tests | `mvn -q test` | pass — `Tests run: 7` (UserServiceTest, pre-existing) + `Tests run: 5` (CartRestControllerTest) + `Tests run: 5` (OrderRestControllerTest) = 17, 0 failures, 0 errors |
| web/servlet leak in domain | `grep -rn "org.springframework.web\|jakarta.servlet" src/main/java/com/unicornt/store/domain` | pass — empty |
| raw RuntimeException in domain | `grep -rn "RuntimeException" src/main/java/com/unicornt/store/domain` | pass — only matches are the typed exception class declarations themselves (`DuplicateResourceException`, `OutOfStockException`, `ResourceNotFoundException`, all owned by T0); no `throw new RuntimeException(...)` remains |
| presentation-layer leftovers | `grep -rn "@Controller\|ModelAndView\|redirect:\|Thymeleaf\|printStackTrace" src/main/java/.../web/rest .../web/dto .../web/mapper .../domain/service` | pass — empty |
| secret literals | manual grep over touched files | pass — none found |
| attribution self-check | `git log --format='%an <%ae>%n%B' refactor/hito4..HEAD \| grep -iE 'claude\|anthropic\|co-authored\|generated with'` | pass — empty |

Endpoint-by-endpoint `curl`/Bruno verification against a running instance was **not** run: the
app cannot boot end-to-end in this worktree without T1's datasource/migrations and T2's real
security filter chain (current `SecurityConfig` is an explicit permit-all stub, see below), and
host port 8080 is occupied by an unrelated container per the dispatch instructions. All status
codes are instead verified through the MockMvc slice tests listed above, which exercise the
controllers, DTO validation, mappers and `GlobalExceptionHandler` together.

## Requests for the orchestrator

| File | Change needed | Why |
|------|---------------|-----|
| `infrastructure/security/SecurityConfig.java` (T2) | Add request matchers requiring authentication for `/api/v1/cart/**`, `/api/v1/orders/**`, `/api/v1/addresses/**` | These three controllers rely on `@PreAuthorize("isAuthenticated()")` at the class level, which only fires if method security is active *and* the request first passes the filter chain's own authorization rule. The current stub `permitAll()`s everything, so today these endpoints are reachable at the filter level — that is fine for local dev but not the final posture. No role restriction is needed beyond "authenticated"; there is no `[ADMIN]` endpoint in this slice. |
| T1's Flyway migration (schema) | See "Decisions taken" below — table/column names to reconcile | This task created `OrderEntity`/`OrderItemEntity`/`OrderStockRepository` against assumed table and column names because no `orders`/`order_items`/`products.stock` schema exists yet in this worktree. T1 owns the actual migration; names must match exactly or Hibernate validation (`ddl-auto: validate`) will fail at boot. |
| `ProductService` (T3) | No new method needed | Checkout only needs the product's current price and name, both already on `ProductEntity`/available through `ProductRepository.findById`, which `CartService`/`CheckoutService` already use. Stock is read/written through a new repository (`OrderStockRepository`) added in this task, deliberately bypassing `ProductService`/`ProductRepository` so T3's owned files stay untouched. |
| `ProductEntity` (T3, informational only — do not edit on my say-so) | Possible duplicate `stock` mapping | If T3 also adds a `stock` field to `ProductEntity` for the catalog admin CRUD (create/update with stock, per the plan's example DTO), the column is the same one `OrderStockRepository` decrements. That is expected and fine — both slices should point at `products.stock` — but the orchestrator should confirm T3's `ProductEntity` and this task's native-SQL repository agree on the column name (assumed `stock`, see below) so there is exactly one source of truth for inventory. |

## Decisions taken

- **Schema names assumed for orders and stock** (my task file's biggest open item, T1 owns the
  actual migration and cannot be seen from this worktree):
  - `products.stock` — `INTEGER NOT NULL`, added to the existing `products` table.
  - `orders` — columns `id` (PK, identity), `user_id` (FK to `users.id`, not null),
    `address_id` (FK to `addresses.id`, not null), `shipping_address` (`VARCHAR(300)`, a frozen
    single-line snapshot so an order survives a later address edit or deletion),
    `total` (`NUMERIC(12,2)`), `status` (`VARCHAR(20)`, stores the `OrderStatus` enum name —
    only `CONFIRMED` is produced today, `CANCELLED` is modeled but unused), `created_at`
    (`TIMESTAMP`).
  - `order_items` — columns `id` (PK, identity), `order_id` (FK to `orders.id`, not null),
    `product_id` (int, not null, no FK enforced so a later product deletion never blocks
    historical orders), `product_name` (`VARCHAR(200)`, frozen at purchase time),
    `unit_price` (`NUMERIC(12,2)`, frozen at purchase time), `quantity` (int),
    `subtotal` (`NUMERIC(12,2)`).
  - The orchestrator must diff these against T1's actual Flyway migration once both branches
    are visible together, and either adjust this task's entities/repository or ask T1 to align
    column names. With `spring.jpa.hibernate.ddl-auto: validate` in the base profile, a name
    mismatch fails fast at boot rather than silently, which is the safer failure mode here.
- **Stock decrement is native SQL, not JPA-managed.** `OrderStockRepository.decreaseStock`
  uses a single conditional `UPDATE ... WHERE stock >= :quantity` and reports back the affected
  row count. This makes the check-then-decrement atomic at the database level (no read-modify-write
  race between concurrent checkouts) and keeps `ProductEntity`/`ProductRepository`, both owned by
  T3, completely untouched. `OrderStockRepository` extends plain `Repository<ProductEntity, Integer>`
  rather than `JpaRepository`, so it exposes only the two methods it needs and cannot be mistaken
  for a general-purpose product repository.
- **`CartService` API reshaped** from primitive `(userEmail, int productId, int quantity)` methods
  returning nothing/`int` to a `CartLine`/`CartView` record-based API returning priced results.
  The original methods (`addToCart`, `updateQuantity`, `getCartTotal` returning `int`) predate the
  cart-line-id-addressable contract the task file's endpoint table requires
  (`PATCH /cart/items/{id}`, `DELETE /cart/items/{id}` — both keyed by the cart line's own id, not
  by `productId`). `updateItemQuantity`/`removeItem` now take the cart line id and are
  ownership-checked against the caller before any mutation. Money moved from `int`/primitive
  arithmetic to `BigDecimal` throughout, per the task file's explicit instruction.
- **`CheckoutService.confirm` returns the persisted `OrderEntity`** instead of the old
  `OrderSummary` record. The old `OrderSummary` had no order id and no persisted order to
  point `GET /orders/{id}` at — once orders became real rows, returning the entity (and mapping
  it to `OrderResponse` in the controller, per convention) was the only way to give the `201`
  response a `Location` header with a real id.
- **`AddressEntity`, `AddressService`, `AddressRepository` kept structurally as T0/base left
  them** (first address becomes default, ownership-checked lookups) — only the
  `ResourceNotFoundException` resource labels were normalized from the entity class name
  (`"AddressEntity"`, `"UserEntity"`) to the domain noun (`"Address"`, `"User"`) so error
  messages read naturally (`"Address not found: 7"` instead of `"AddressEntity not found: 7"`).
- **Test strategy: standalone `MockMvc`, not `@WebMvcTest`.** This Spring Boot 4 / Spring
  Security 7 combination requires either a real `SecurityFilterChain` bean (which only
  `SecurityConfig`, owned by T2 and currently a permit-all stub with no `HttpSecurity`-producing
  `WebSecurityConfiguration`, can supply) or a fully wired `springSecurityFilterChain` bean for
  `springSecurity()`'s MockMvc configurer to attach to — neither is available in a `@WebMvcTest`
  slice that only imports this task's controller. Building the controller directly
  (`MockMvcBuilders.standaloneSetup(new CartRestController(mockService))`) with
  `.setControllerAdvice(new GlobalExceptionHandler())` and
  `.setValidator(new LocalValidatorFactoryBean())`, and attaching the authenticated principal
  per-request via `.principal(AUTH)` (an `Authentication` instance, which Spring MVC's built-in
  `PrincipalMethodArgumentResolver` reads off `HttpServletRequest.getUserPrincipal()`) reproduces
  the exact runtime behavior the controllers rely on (`Authentication authentication` as a
  method parameter) without depending on T2's in-progress `SecurityConfig`. This is a test-only
  decision; it does not change how the controllers resolve identity in production
  (`Authentication.getName()`, populated by the real filter chain once T2 lands it).

## Known gaps

- **No live end-to-end verification.** As noted above, `curl`/Bruno verification against a
  running instance was not performed because a full boot needs T1's datasource/migrations and
  T2's real filter chain, neither available in this worktree. Once integration lands, the
  orchestrator (or T6) should smoke-test all ten endpoints listed in the task file.
- **Schema reconciliation with T1 is unresolved** until both branches are visible together — see
  "Requests for the orchestrator" and "Decisions taken" above for the exact names assumed.
- **`SecurityConfig` still permits all requests.** Until T2 lands the real filter chain and the
  matcher this note requests, these endpoints are reachable at the filter-chain level under the
  current stub; `@EnableMethodSecurity` + `@PreAuthorize("isAuthenticated()")` will still reject a
  fully anonymous `Authentication`, but there is no JWT/CORS wiring yet to authenticate a real
  user. Needs a post-integration smoke test once T2's branch merges.
- **`OrderEntity.OrderStatus.CANCELLED` is modeled but nothing sets it.** No cancel endpoint
  exists; the task file's endpoint table does not ask for one. Left as a straightforward future
  extension point.

## Attribution check

```
git log --format='%an <%ae>%n%B' refactor/hito4..HEAD | grep -iE 'claude|anthropic|co-authored|generated with'
```

Result: empty
