# Consolidated Plan — Unicornt Store Full-Stack Final Delivery

> Final version. Consolidates `Plan-refactor2-entrega-final.md` + `Refinamiento plan.md`
> with the agreed adjustments: recalibration to the real 10-point rubric, risk-first
> ordering (E2E first), ArchUnit as architecture TDD, package layout from the
> instructor's guide, and awareness of the test rework from the coverage session.
>
> **Language:** this plan is written in English; **all backend code, names, comments,
> messages and tests go in English** (required by dimension 2).

---

## 0. Framing

### 0.1 Where we come from

Three partially disconnected evolutionary lines that now must converge:

```
Line A — H1 project → H3 refactor → Clean Architecture   (otf-sisacad, different domain)
Line B — frontend mockup → H2 refactor → strict TS        (unicornt-store-frontend)
Line C — Spring MVC + Thymeleaf → H4 refactor → REST/JWT/JPA/OpenAPI  (this repo)
```

There is no prior contract between B and C: they were never connected. That gives
**freedom to modify both sides** toward a common design, instead of deforming one to
fit the other.

### 0.2 What is being graded now — "Final Delivery" rubric (10 points)

| Dim | Criterion | Pts | Status as of 2026-09-02 | Distance |
|-----|-----------|----:|-------------------------|----------|
| **1** | Full-stack integration / data cycle: TS ↔ Spring Boot ↔ PostgreSQL, no CORS errors, complete observable flow | **4** | front and back never talked to each other | **large** |
| **2** | Cumulative rigor: backend with clean layers + tactical DDD + 100% of core business rules covered by JUnit 5 + Mockito, **in English** | **3** | tests 96%/93% (coverage session) ✅ · coupled layered architecture ❌ | medium |
| **3** | Production-grade security: zero versioned secrets (env vars + `.gitignore`), Swagger only in `dev` | **3** | Swagger dev-only ✅ · secrets to be verified | **small** |

**Guiding priority principle:** dimension 1 (4 pts) weighs more than the whole of
dimension 2 (3 pts) and is at zero. A working E2E with mediocre architecture scores
higher than pristine architecture that does not connect. The refactor is interleaved
with the integration, but the **risk ordering** is: prove the read cycle → prove the
purchase cycle → perfect the architecture underneath.

### 0.3 Repositories

| Repo | Role | Local path (adjust) |
|------|------|---------------------|
| `unicornt-store-backend` | this repo; Spring Boot backend | `c:\Users\Usuario\Proyectos\unicornt-store-backend` |
| `unicornt-store-frontend` | Vite + TS; `github.com/keber/unicornt-store-frontend` | *(clone next to the backend)* |
| `otf-sisacad` | own architectural reference (H1/H3) | *(reference, not touched)* |
| `sebavidal10/neonpulse-*` | the instructor's reference; this is what the grader compares against | *(read)* |

---

## 1. Closed decisions

Unless a requirement contradicts them:

| Topic | Decision |
|-------|----------|
| Catalog, price, stock, image | Backend is the single source of truth |
| Anonymous cart | `localStorage` in the frontend |
| Authenticated cart | Backend |
| Login with a local cart | `POST /cart/merge`; rule `qty_final = qty_local + qty_server` subject to stock; then the local cart is cleared |
| Stock reservation on add-to-cart | No — the cart is intent, not a reservation |
| Stock decrement | Only on Order confirmation, transactional |
| Checkout | Creates an `Order` (`POST /api/v1/orders`), not a `/checkout` resource |
| Checkout transaction | Atomic: all or nothing (order + items + stock-- + cart clear) |
| Payment | Simulated → `OrderStatus.CONFIRMED`. No real gateway |
| Shipping | State only (`CONFIRMED` → optional `DISPATCHED`), no subsystem |
| Authentication | Existing JWT. JWT vs cookie is not reopened |
| Product image | Persisted `imageUrl`; no upload/multipart/S3 |
| Admin Product | REST CRUD protected by `ROLE_ADMIN` |
| Contract | OpenAPI. Hand-written TS types at first; generation from OpenAPI only if drift appears |
| Backend architecture | Instructor's guide layout (see §2) |
| Refactor strategy | Vertical slices; the H1/H3 refactor happens **inside** each slice, not at the end |
| First slice | Catalog (reference pattern) |
| Big-bang rewrite | No — strangler strategy inside the same repo |
| Frontend clean-arch | Low priority: only minimal port extraction (`ProductGateway`, `CheckoutGateway`). Do not replicate full hexagonal |
| Value Objects | `Money`, `Quantity` committed; `Email`, `ProductId` to be evaluated. No `ProductName`/`Street`/`CategoryName` |
| Bounded contexts | Conceptual groupings (`catalog`, `cart`, `ordering`, `identity`), not strategic DDD |

---

## 2. Target architecture (backend)

### 2.1 Package layout

Follows the instructor's guide (repository interface **inside `domain/`**, not
`application/port/out`; simple vocabulary, not ceremonial hexagonal):

```
com.unicornt.store
├── domain/                         # pure Java — ZERO Spring, JPA, Jakarta, web
│   ├── model/                      # domain entities (Product, Category, Cart,
│   │                               #   CartItem, Order, OrderItem, User)
│   │                               # note: "model/" is used instead of "entity/" to
│   │                               #   avoid colliding with the JPA *Entity classes;
│   │                               #   the guide uses "entity/", both defensible
│   ├── valueobject/                # Money, Quantity, (Email, ProductId to evaluate)
│   ├── exception/                  # already exists: DuplicateResourceException,
│   │                               #   OutOfStockException, ResourceNotFoundException
│   └── repository/                 # pure interfaces: ProductRepository,
│                                   #   CategoryRepository, CartRepository,
│                                   #   OrderRepository, UserRepository
├── application/
│   └── usecase/
│       ├── catalog/    ListProductsUseCase, GetProductUseCase, CreateProductUseCase,
│       │               UpdateProductUseCase, DeleteProductUseCase, ListCategoriesUseCase
│       ├── cart/        GetCartUseCase, AddCartItemUseCase, UpdateCartItemUseCase,
│       │               RemoveCartItemUseCase, MergeCartUseCase
│       ├── ordering/    PlaceOrderUseCase, GetOrderUseCase, ListOrdersUseCase
│       └── identity/    RegisterUserUseCase   (login stays in AuthController +
│                                              AuthenticationManager)
└── infrastructure/
    ├── web/
    │   ├── rest/                   # thin controllers: request → useCase → response
    │   ├── dto/
    │   ├── mapper/                 # DTO ↔ domain
    │   └── error/                  # GlobalExceptionHandler, ErrorResponse
    ├── persistence/
    │   ├── entity/                 # JPA: *JpaEntity  (renamed from *Entity)
    │   ├── repository/             # Spring Data: SpringData*Repository / *JpaRepository
    │   ├── adapter/                # *RepositoryAdapter implements domain.repository.*
    │   └── mapper/                 # JPA entity ↔ domain model
    ├── security/                   # unchanged except where necessary
    └── config/
```

### 2.2 Dependency rule

```
infrastructure ──► application ──► domain          (domain depends on nobody)
infrastructure ──► domain (implements domain.repository, maps to domain.model)

domain     -X► org.springframework..
domain     -X► jakarta.persistence.. / jakarta.validation..
domain     -X► ..infrastructure..
domain     -X► ..application..
application -X► ..infrastructure..
application -X► org.springframework.data..
web (rest) -X► ..persistence..  (direct; always via a use case)
```

### 2.3 The five models (pedagogically explicit, little code)

```
ProductJpaEntity      (infrastructure.persistence.entity)
      ↕  ProductPersistenceMapper
Product               (domain.model)
      ↕  ProductRestMapper
ProductResponse       (infrastructure.web.dto)
      │  OpenAPI
ProductDto            (frontend transport)
      ↕  toProductModel
ProductModel          (frontend domain)
```

Each separation maps to something the rubric grades. It does not mean much code.

### 2.4 Business rules that must reach 100% coverage

These are the "core business rules" of dimension 2. Today they have tests (coverage
session); on refactor they **split** into `domain.model` tests (pure) + use-case
tests (Mockito over ports). The scenarios transfer ~1:1.

| Area | Rules |
|------|-------|
| Product | `price > 0`; `name` non-empty; `name.length <= 200`; `stock >= 0`; category and type must exist |
| Category | `name` required; `name.length <= 100`; slug uniqueness; `slugify` (accents, non-alphanumeric → error) |
| Cart | `quantity > 0`; same product → sum; `quantity == 0` → remove; ownership (another user's item → 404) |
| PlaceOrder | cart non-empty; validate stock (`decreaseStock == 0` → `OutOfStockException`); name and price snapshot; total calculation; stock decrement; cart clear; **atomicity** |
| User | `password.length >= 6`; email uniqueness; required fields; default role `ROLE_USER` |
| Address | `street`/`city`/`region` required; first address → `default = true` |

### 2.5 `jacoco:check` — scope adjustment

The current check (added in the coverage session) requires 92%/88% over the whole
bundle. Dimension 2 asks for **100% of business rules**, not global coverage. Adjust
to two rules:

```
- BUNDLE  ..domain..        → INSTRUCTION 100% / BRANCH 100%
- BUNDLE  ..application..    → INSTRUCTION 100% / BRANCH  95%
- BUNDLE  rest              → INSTRUCTION  85% / BRANCH  75%   (safety net)
```

Exclusions already in effect: `StoreApplication`, `infrastructure.config`,
`**/dto/**`, `**/persistence/entity/**`, `web/error/ErrorResponse*`. Add the new
records/DTOs and the `*JpaEntity` classes to the exclusion.

---

## 3. ArchUnit as architecture TDD

### 3.1 Setup

Add to `pom.xml` (only the version property is present):

```xml
<dependency>
  <groupId>com.tngtech.archunit</groupId>
  <artifactId>archunit-junit5</artifactId>
  <version>${archunit.version}</version>
  <scope>test</scope>
</dependency>
```

Package `src/test/java/com/unicornt/store/architecture/`.

### 3.2 Red → green → lock mechanics

1. **Write the rules BEFORE the refactor** (full §2.2).
2. They run **red** with the exact list of every offending class → **that is the
   refactor backlog**.
3. Freeze with `FreezingArchViolationStore` (baseline in
   `src/test/resources/archunit_store/`): the build stays **green**, but fails if a
   *new* violation appears. Ratchet.
4. Each refactored slice → violations drop → regenerate the baseline.
5. When the baseline reaches 0 → hard rules, delete the store.

### 3.3 Rules

**Group A — green from now on (protect what works):**

- No package cycles.
- `*RestController` only in `..web.rest` and annotated `@RestController`.
- `..domain..` does not depend on `..web..`.
- `..web..` does not access `..persistence.repository..` directly.

**Group B — frozen with freezing (the backlog):**

- `no classes in ..domain.. should depend on org.springframework..`
- `no classes in ..domain.. should depend on jakarta.persistence..`
- `no classes in ..domain.. should depend on ..infrastructure..`
- `no classes in ..application.. should depend on ..infrastructure..`
- `no classes in ..application.. should depend on org.springframework.data..`
- `LayeredArchitecture`: `web → application → domain`; `persistence → application`
  (implementing ports); `domain` depends on no layer.

**Group C — target, `@Disabled("FINAL-DELIVERY: pending slice X")`:**

- Interfaces ending in `Repository` reside in `..domain.repository..`.
- Classes in `..persistence.adapter..` implement an interface from
  `..domain.repository..`.
- Classes in `..application.usecase..` do not depend on JPA types (`*JpaEntity`).

They are enabled one by one as each slice progresses.

---

## 4. Phased execution plan

Each phase ends with **executable evidence** before moving on. A verified slice is
not deliberately broken to advance another.

### Phase 0 — Foundation (sequential)

| Step | Work | Gate |
|------|------|------|
| 0.1 | **Secrets** (dim 3, ~1h): `.env` / `.env.*` out of git; backend `.gitignore` covers `target/`, `.env*`, `*.class`, IDE; frontend `.gitignore` covers `node_modules/`, `dist/`, `.env*`. `JWT_SECRET`, `DB_PASSWORD` via env var only. Commit the pending `.gitignore`. | `git ls-files` does not list `.env*` or secrets; `grep` for prod credentials in the tree = 0 |
| 0.2 | **Contract sketch** (lightweight, 1 page): the ~12 endpoints from §7, ownership (already in §1), error shape. Not a ceremony. | document reviewed; front and back agree on `Product`, `Cart`, `Order` |
| 0.3 | **Global CORS** in the backend: `http://localhost:5173` (global config, no `@CrossOrigin` on controllers) | slice test: `GET /api/v1/products` with `Origin: http://localhost:5173` responds with CORS headers |
| 0.4 | **ArchUnit**: dependency + Group A green + Group B frozen + Group C `@Disabled` | `mvn test` green; baseline committed; number of frozen violations recorded |
| 0.5 | **Minimal read E2E**: frontend points at the real `GET /api/v1/products` (drops the mock as primary source); catalog renders from PostgreSQL via Docker | in the Vite browser: catalog visible with Postgres data, no CORS error, no mock |

**Phase 0 output:** dimension 3 essentially closed; dimension 1 de-risked (read cycle
proven); architecture board visible.

### Phase 1 — Catalog slice (reference pattern)

Backend (`domain → application → adapters → REST`), in this order:

1. Pure `domain/model/Product`, `Category`; `domain/valueobject/Money`, `Quantity`.
   Rules §2.4 protected in the model (constructor/factory validates).
2. `domain/repository/ProductRepository`, `CategoryRepository` (pure interfaces).
3. `application/usecase/catalog/*` (may be grouped in one application service if it
   avoids class proliferation).
4. `infrastructure/persistence`: rename `ProductEntity` → `ProductJpaEntity`;
   `SpringDataProductRepository`; `ProductRepositoryAdapter implements
   domain.repository.ProductRepository`; `ProductPersistenceMapper`.
5. `infrastructure/web`: thin `ProductRestController`; `ProductRestMapper`
   (DTO ↔ domain). The controller has no rules.

Frontend: real `HttpProductApi`; `unknown → runtime validation → ProductDto →
ProductModel`; adjust `ProductDto` to the contract.

Slice tests (all in English):
`ProductTest`, `MoneyTest`, `QuantityTest`, `CategoryTest`,
`*UseCaseTest` (Mockito over `ProductRepository`),
`ProductPersistenceMapperTest`, `ProductRepositoryAdapterTest`,
`ProductRestControllerTest` (`@WebMvcTest`, use-case mock).
Migrate the equivalent scenarios from `CategoryServiceImplTest` /
`ProductServiceImplTest` / `ProductServiceImplSearchTest`.

Enable the Group C ArchUnit rules for `catalog`. Regenerate the freezing baseline
(catalog violations → 0).

**Catalog gate:** `PostgreSQL → backend → GET /products → frontend → catalog
visible`, with no mock as primary source. `mvn verify` green with `jacoco:check`
100% over catalog's `domain.model`. `mvn test` ArchUnit green. `npm run build` with
no errors, zero `any`.

### Phase 2 — Cart ‖ Identity slices (parallel)

Little domain logic; they go to two parallel workers with Catalog as reference and
the ArchUnit rules active. Disjoint package ownership.

**Cart:**
- `domain/model/Cart`, `CartItem`; rules `quantity > 0`, merge, ownership.
- `domain/repository/CartRepository` (no Spring Data).
- `application/usecase/cart/*` incl. `MergeCartUseCase` (`qty_local + qty_server`,
  subject to stock).
- JPA adapter + mapper.
- `POST /api/v1/cart/merge` with payload `{ items: [{ productId, quantity }] }`.
- Frontend: `anonymous cart → login → merge → server cart → clear local` transition.
- Tests: migrate `CartServiceImplTest` (22) scenarios to `CartTest` +
  `*UseCaseTest`.

**Cart gate:** add a product while logged out → log in → the product is present in
the authenticated cart.

**Identity:**
- `domain/model/User` (or keep minimal); `RegisterUserUseCase`.
- Keep the current JWT. `register` + `login` exposed. Roles `ROLE_USER` /
  `ROLE_ADMIN`.
- Frontend: single boundary `auth.api` / `auth.service` / token storage; `apiFetch()`
  centralizes base URL, `Authorization`, JSON, errors, 401. Do not replicate the
  header in every file.
- Tests: migrate `UserServiceTest`, `JwtServiceTest`, `CustomUserDetailsServiceTest`,
  `JwtAuthFilterTest` (this session) to the new layout.

**Identity gate:** valid login → token → protected endpoint works; invalid login →
visible error in the UI.

### Phase 3 — Ordering slice (the E2E that matters)

Depends on Cart + Catalog. It is the system's most representative flow.

- `domain/model/Order`, `OrderItem`, `OrderStatus`, `ShippingAddress`.
  `Order` preserves a snapshot: `productId`, `productName`, `unitPrice`, `quantity`.
- `application/usecase/ordering/PlaceOrderUseCase`:
  `load cart → validate non-empty → validate stock → calculate totals →
   create Order → persist → decrement stock → clear cart → return confirmation`.
  None of this in the controller.
- Transaction boundary: the Spring implementation of the use case is
  `@Transactional`. All-or-nothing guarantee.
- Payment: simulated → `OrderStatus.CONFIRMED`.
- `POST /api/v1/orders` with `{ shippingAddress: {...} }`; items come from the server
  cart, not the client. Response `{ id, status, total }`.
- Frontend: reuses the H2 states `submitting / success / error`. Success →
  confirmation + clear the visible cart state. Stock error → actionable message,
  preserve the cart.
- Tests: migrate `CheckoutServiceImplTest` (10) → `OrderTest` +
  `PlaceOrderUseCaseTest` (Mockito over `CartRepository`, `OrderRepository`,
  `OrderStockRepository`). Verify interaction order (persist before clear).

**Main E2E gate:**
`Browser → cart → login → checkout → POST /orders → PostgreSQL →
stock decremented → cart empty → UI CONFIRMED`.
This is the application's main smoke test.

### Phase 4 — Admin Product

- `POST/PUT/DELETE /api/v1/products` protected by `ROLE_ADMIN`.
  `Product` includes `name, description, price, category, stock, imageUrl`.
  No file storage: `imageUrl` is a URL.
- Frontend: minimal UI (list + form + delete). No dashboard.
- Tests: `CreateProductUseCaseTest`, `UpdateProductUseCaseTest`,
  `DeleteProductUseCaseTest` (already partly covered by this session's
  `ProductServiceImplTest`; migrate).

**Admin gate:** admin creates a product → PostgreSQL → customer catalog → product
visible.

### Phase 5 — Cross-cutting hardening

No new development except fixes.

| Area | Action | Gate |
|------|--------|------|
| ArchUnit | Group B: freezing baseline at 0 → convert to hard rules, delete the store | `mvn test` green with no freezing |
| Coverage | `jacoco:check` §2.5: 100% `domain` + `application` | `mvn verify` green |
| Backend quality | `mvn clean verify`; `docker compose config` | green |
| Frontend quality | `npm test`; `npm run lint`; `npm run build` | 0 `any`, 0 unsafe non-null assertions |
| CORS | global `http://localhost:5173` | `GET /products` from Vite with no error |
| Swagger | `dev` available / `prod` 404 | verified with both profiles |
| Secrets | `.env`, `DB_PASSWORD`, `JWT_SECRET` not versioned | tree scan = 0 |
| Frontend clean-arch (minimal) | extract `ProductGateway`, `CheckoutGateway` (interfaces) + `Http*` impl; services depend on the port, not the concrete adapter | build green; `fetch`/concrete-adapter import outside services |

### Phase 6 — Delivery

- `README.md` in **both** repos: purpose, stack, architecture, requirements,
  environment variables, bring up PostgreSQL, run the backend, run the frontend,
  run the tests, Swagger, API URL, demo credentials, E2E flow.
  It must allow reproducing the system from scratch:
  `docker compose up -d` → `./mvnw clean test` → `./mvnw spring-boot:run` →
  `npm install` → `npm run dev`.
- Final OpenAPI.
- Re-evaluation of the 10 points against the final state of the integrated system.

---

## 5. Coordinator structure and parallelization

Same pattern as `.claude/agents/refactor-orchestrator.md`.

```
Phase 0  Foundation                  ─┐  sequential (coordinator)
Phase 1  Catalog slice (pattern)      │  sequential: domain→app→adapter does not parallelize
                                      ▼
Phase 2  Cart slice  ‖  Identity slice    2 workers in parallel
        · each: ArchUnit rules + Catalog slice as reference
        · disjoint package ownership (cart/* vs identity/*)
                                      ▼
Phase 3  Ordering slice                   1 worker (depends on Cart + Catalog)
                                      ▼
Phase 4  Admin Product                    1 worker
                                      ▼
Phase 5  Hardening                        coordinator + targeted workers
Phase 6  Delivery                         coordinator
```

Rules for the workers:

1. No big-bang rewrite. One vertical slice at a time.
2. Do not break an already-verified slice.
3. Keep existing tests unless they contradict an explicitly replaced functional
   decision (migrate them, do not delete them).
4. Do not keep APIs/classes/structures just because they already exist.
5. Do not add patterns without a demonstrable need. No new frontend frameworks.
6. Backend domain free of Spring/JPA. Controllers do not know persistence.
   `application` does not know concrete adapters.
7. Any contract change updates OpenAPI and the corresponding frontend.
8. Each phase leaves green tests (unit + ArchUnit) and, where applicable, an E2E gate.
9. Faced with "ideal architecture" vs academic scope → the **minimal solution that
   fully satisfies the rubric**.
10. Do not touch `pom.xml` except the coordinator.

---

## 6. Definition of done

```
✓ Frontend TypeScript compiles with no errors; tests green; 0 any
✓ Backend tests green (unit + ArchUnit with no freezing)
✓ jacoco:check 100% over domain + application
✓ Java domain with no Spring/JPA/Jakarta/infrastructure
✓ application depends only on domain + repository ports
✓ persistence implements the ports; JPA↔domain mapper
✓ Thin controllers: request → use case → response
✓ PostgreSQL comes up with Docker Compose
✓ Swagger only in dev (404 in prod)
✓ CORS allows the Vite frontend
✓ Catalog comes from the real backend (no mock as source)
✓ Anonymous cart survives login (merge)
✓ Purchase creates an Order + decrements stock + clears the cart, transactional
✓ UI confirms the purchase
✓ Admin creates/edits/deletes products; persists in PostgreSQL
✓ No versioned production secrets
✓ README reproduces the system from scratch in both repos
✓ Backend code, names, comments and tests in English
```

Critical path:

```
Contract → Catalog → (Cart ‖ Identity) → Ordering → Admin → Hardening → Delivery
```

---

## 7. Appendix A — Endpoint contract (minimal target)

### Catalog (public)

```
GET /api/v1/products
GET /api/v1/products/{id}
GET /api/v1/categories
```

### Authentication

```
POST /api/v1/auth/register
POST /api/v1/auth/login          → { token, expiresIn }
```

### Authenticated cart

```
GET    /api/v1/cart
POST   /api/v1/cart/items         { productId, quantity }
PUT    /api/v1/cart/items/{productId}   { quantity }
DELETE /api/v1/cart/items/{productId}
POST   /api/v1/cart/merge         { items: [{ productId, quantity }] }
```

### Purchase (authenticated)

```
POST /api/v1/orders              { shippingAddress: { street, city, region, zipCode } }
                                 → { id, status: "CONFIRMED", total }
GET  /api/v1/orders/{id}
GET  /api/v1/orders              (only if the frontend uses it)
```

### Administration (`ROLE_ADMIN`)

```
POST   /api/v1/products          { name, description, price, categoryId, productTypeId, stock, imageUrl, active }
PUT    /api/v1/products/{id}
DELETE /api/v1/products/{id}
```

### Error shape (keep the current one)

```json
{ "message": "...", "code": "...", "status": 404, "timestamp": "...", "path": "...", "errors": [] }
```

Codes: `VALIDATION_ERROR`, `RESOURCE_NOT_FOUND`, `BUSINESS_RULE_VIOLATION`
(stock), `RESOURCE_CONFLICT`, `UNAUTHORIZED`, `ACCESS_DENIED`, `INTERNAL_ERROR`.

---

## 8. Appendix B — Vocabulary reconciliation

Resolve before coding each slice (ubiquitous language, in English):

| Concept | Backend (domain) | API (DTO) | Frontend (model) | Note |
|---------|------------------|-----------|------------------|------|
| Product | `Product` | `ProductResponse` | `ProductModel` | |
| Category | `Category` | `CategoryResponse` | `ProductCategory` | |
| Type/Subcategory | `ProductType` | `productTypeId` / `productTypeName` | `ProductSubcategory` | **verify they are the same concept** |
| Price | `Money` (VO) / `int` | `number` | `number` | API exposes CLP integer |
| Cart | `Cart` | `CartResponse` | `CartModel` | |
| Item | `CartItem` | `CartItemResponse` | `CartItemModel` | `qty` ↔ `quantity`: use `quantity` |
| Order | `Order` | `OrderResponse` | `OrderModel` / `CheckoutModel` | the checkout result is an Order |
| Address | `ShippingAddress` | `shippingAddress` | checkout address | `Address` ↔ `ShippingAddress` |
| User | `User` | auth DTO | — | |

---

## 9. Appendix C — Test rework mapping (coverage session → new layout)

The 145 tests on the `test/coverage-boost` branch are **migrated**, not discarded.
The scenarios transfer; the SUT changes.

| Current test | Destination after refactor |
|--------------|----------------------------|
| `CategoryServiceImplTest` (9) | `CategoryTest` (domain) + `*CategoryUseCaseTest` |
| `ProductServiceImplTest` (30) + `ProductServiceImplSearchTest` (3) | `ProductTest`, `MoneyTest`, `QuantityTest` + `*ProductUseCaseTest` |
| `CartServiceImplTest` (22) | `CartTest` (domain) + `*CartUseCaseTest` + `MergeCartUseCaseTest` |
| `CheckoutServiceImplTest` (10) | `OrderTest` (domain) + `PlaceOrderUseCaseTest` |
| `AddressServiceImplTest` (20) | `ShippingAddressTest` + `*AddressUseCaseTest` |
| `*MapperTest` (33) | `ProductRestMapperTest` + `ProductPersistenceMapperTest` (split into DTO↔domain and JPA↔domain) |
| `CustomUserDetailsServiceTest`, `JwtAuthFilterTest`, `JwtServiceTest`, `UserServiceTest`, `SecurityChainTest` | same, relocated to the `infrastructure.security` / `application.usecase.identity` layout |
| `GlobalExceptionHandlerTest` (12) | same, in `infrastructure.web.error` |
| `ProductRestControllerTest`, `CartRestControllerTest`, `CategoryRestControllerTest`, `OrderRestControllerTest` | same (`@WebMvcTest`), but `@MockitoBean` of the **use case** instead of the `ServiceImpl` |

The `-Dnet.bytebuddy.experimental=true` in the surefire `argLine` (Java 25 + Mockito)
is kept.
