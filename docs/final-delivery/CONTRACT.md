# Target HTTP contract — Final Delivery

Design record, not a spec ceremony. It exists so P1–P4 do not each re-decide field
names, the error shape or ownership. On a mechanical disagreement the phase file
wins and says why. Source: [PLAN.md](PLAN.md) §7, §1, §8.

Base URL: `http://localhost:8080` · API prefix: `/api/v1` · Content type: `application/json`.

---

## 1. Endpoints

### Catalog (public read; writes are `ROLE_ADMIN`)

```
GET    /api/v1/products              200            (?category=&q=&page=&size=)
GET    /api/v1/products/{id}         200 / 404
POST   /api/v1/products              201 + Location  [ADMIN]
PUT    /api/v1/products/{id}         200 / 404       [ADMIN]
DELETE /api/v1/products/{id}         204 / 404       [ADMIN]
GET    /api/v1/categories            200
```

### Authentication

```
POST   /api/v1/auth/register        201            { email, password, firstName?, lastName? }
POST   /api/v1/auth/login           200            -> { token, expiresIn }
GET    /api/v1/auth/me              200 / 401       (current principal)
```

### Cart (authenticated)

```
GET    /api/v1/cart                 200
POST   /api/v1/cart/items           200 / 201       { productId, quantity }
PUT    /api/v1/cart/items/{productId}  200 / 404    { quantity }   (quantity 0 -> remove)
DELETE /api/v1/cart/items/{productId}  204 / 404
POST   /api/v1/cart/merge           200             { items: [{ productId, quantity }] }
```

### Orders / purchase (authenticated)

```
POST   /api/v1/orders               201            { shippingAddress: { street, city, region, zipCode } }
                                                   -> { id, status: "CONFIRMED", total }
GET    /api/v1/orders/{id}          200 / 404
GET    /api/v1/orders               200            (only if the frontend uses it)
```

---

## 2. Error shape (keep the current one)

```json
{ "message": "...", "code": "...", "status": 404, "timestamp": "...", "path": "...", "errors": [] }
```

Codes: `VALIDATION_ERROR`, `RESOURCE_NOT_FOUND`, `BUSINESS_RULE_VIOLATION` (stock),
`RESOURCE_CONFLICT`, `UNAUTHORIZED`, `ACCESS_DENIED`, `INTERNAL_ERROR`.

`errors[]` carries per-field validation entries `{ field, message }` for
`VALIDATION_ERROR`; empty otherwise.

---

## 3. Vocabulary (ubiquitous language, English)

| Concept | Backend (domain) | API (DTO) | Frontend (model) | Note |
|---------|------------------|-----------|------------------|------|
| Product | `Product` | `ProductResponse` | `ProductModel` | |
| Category | `Category` | `CategoryResponse` | `ProductCategory` | |
| Type/Subcategory | `ProductType` | `productTypeId` / `productTypeName` | `ProductSubcategory` | same concept, confirm per slice |
| Price | `Money` (VO) | `number` (CLP integer) | `number` | API exposes CLP integer |
| Cart | `Cart` | `CartResponse` | `CartModel` | |
| Item | `CartItem` | `CartItemResponse` | `CartItemModel` | field name is `quantity`, never `qty` |
| Order | `Order` | `OrderResponse` | `OrderModel` / `CheckoutModel` | the checkout result is an Order |
| Address | `ShippingAddress` | `shippingAddress` | checkout address | |
| User | `User` | auth DTO | — | |

Value objects committed: `Money`, `Quantity`. `Email` / `ProductId` evaluated per
slice. No `ProductName` / `Street` / `CategoryName`.

---

## 4. Ownership (parallelism contract)

| Owner | Backend | Frontend |
|-------|---------|----------|
| P0 | `pom.xml`, `.gitignore`, `.env.example`, `.claude/settings.json`, `architecture/**`, `archunit_store/**`, `infrastructure/config/**`, `infrastructure/web/error/**`, `domain/exception/**` | `.gitignore`, `.env.example`, `src/api/http.ts`, Vite `.env` handling |
| P1 catalog | `Product`/`Category` model, `Money`/`Quantity`, their ports, `usecase/catalog/**`, `*Product*`/`*Category*` persistence + web, tests | `models/product*`, `api/product*`, `services/product*`, catalog views |
| P2a cart | `Cart`/`CartItem` model, `CartRepository`, `usecase/cart/**`, `*Cart*` persistence + web, tests | `models/cart*`, `api/cart*`, `services/cart*`, `storage/cart*`, cart views |
| P2b identity | `User` model, `UserRepository`, `usecase/identity/**`, `infrastructure/security/**`, `*Auth*` web, tests | `api/auth*`, `services/auth*`, token storage, login/register views |
| P3 ordering | `Order`/`OrderItem`/`OrderStatus`/`ShippingAddress`, `OrderRepository`, `usecase/ordering/**`, `*Order*`/`*Address*` persistence + web, tests | `models/checkout*`/`order*`, `api/order*`, `services/checkout*`, checkout views |
| P4 admin | `@PreAuthorize("hasRole('ADMIN')")` + tests on the P1 product write endpoints, if missing | admin product UI |
| P5/P6 | `README.md`, `docs/**`, `.github/workflows/**`, OpenAPI, `jacoco:check` | `README.md`, `package.json` scripts, `ProductGateway`/`CheckoutGateway` |

`SecurityConfig` belongs to P2b. `GlobalExceptionHandler` + `domain/exception/**`
belong to P0. `pom.xml` / `package.json` are frozen after P0.

---

## 5. Architecture backlog (ArchUnit frozen violation count)

Recorded at P0 close; the count is the refactor progress meter. It only ever drops.

| Rule (Group B — frozen) | P0 baseline |
|-------------------------|------------:|
| domain -X-> org.springframework.. | 73 |
| domain -X-> jakarta.persistence.. | 0 |
| domain -X-> jakarta.validation.. | 0 |
| domain -X-> ..infrastructure.. | 235 |
| domain -X-> ..application.. | 0 |
| application -X-> ..infrastructure.. | 0 |
| application -X-> org.springframework.data.. | 0 |
| no package cycles between the top-level slices | 47 |
| LayeredArchitecture (web->application->domain; persistence->application; domain depends on no layer) | 309 |
| **Total violation lines frozen** | **664** |

Counts are the actual `FreezingArchViolationStore` line counts from the first
`mvn test` on `final-delivery` after the ArchUnit dependency was added. The
`domain` package today is `domain/service/*ServiceImpl` (Spring `@Service` beans
that use JPA entities and Spring Data), which is the entire backlog. `jakarta.*`
shows 0 because the services depend on the `*Entity` classes, not on the JPA
annotations directly.

Group C rules carry `@ArchIgnore(reason = "FINAL-DELIVERY: pending slice ...")`
rather than JUnit `@Disabled` — the `@ArchTest` engine does not honour `@Disabled`.
Same effect: rule present, not enforced until its slice lands.

Group A rules (no cycles; `*RestController` placement; `domain` -X-> `web`;
`web` -X-> `persistence.repository`) are green from P0.

Group C rules (`*Repository` interfaces in `domain.repository`; `persistence.adapter`
implements a `domain.repository` port; `usecase` free of `*JpaEntity`) are present
and `@Disabled`, enabled one slice at a time.

> The numbers above are regenerated by the orchestrator after every slice merge.
> See `docs/final-delivery/handoffs/*` for the per-slice drop.
