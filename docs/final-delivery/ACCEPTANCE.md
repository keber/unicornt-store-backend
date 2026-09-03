# Phase E — Acceptance report (Final Delivery, 10 points)

State of `final-delivery` in both repos at the end of the refactor. Every row is
backed by a command or a request/response captured during the run.

| Repo | `final-delivery` tip | baseline tag |
|------|----------------------|--------------|
| unicornt-store-backend | (36 commits after `baseline-final-delivery`) | `baseline-final-delivery` |
| unicornt-store-frontend | (12 commits after `baseline-final-delivery`) | `baseline-final-delivery` |

---

## Dimension 1 — Full-stack integration / data cycle (4 pts) — **MET**

TS ↔ Spring Boot ↔ PostgreSQL, no CORS errors, complete observable flows.

### 1a. Read cycle — catalog from PostgreSQL, no mock, no CORS block — **met**

```
$ docker compose up -d db          # PostgreSQL 16 in Docker
$ SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
$ curl -s -i -H 'Origin: http://localhost:5173' http://localhost:8080/api/v1/products
HTTP/1.1 200
Access-Control-Allow-Origin: http://localhost:5173
{"content":[
  {"id":1,"name":"Classic Unicorn T-shirt",...,"price":14990,"categoryName":"Unicorns","stock":25,"active":true},
  {"id":2,"name":"Rainbow Mug",...}, {"id":3,"name":"Starry Night Poster",...}],
 "page":0,"size":20,"totalElements":3,"totalPages":1}
```

Frontend `src/api/product.api.ts` calls `apiFetch('/api/v1/products')`; the former
`public/data/products.json` mock is gone. `product.service.ts` validates the page
envelope with `isProductPageDto` (same shape the Vitest fixtures use) and maps to
`ProductModel[]`. `?category=unicorns` filters live to one row.

### 1b. Purchase cycle — the headline E2E — **met**

Live against PostgreSQL + the built jar:

```
stock before:  product 1 = 25, product 2 = 40
POST /api/v1/auth/register  -> 201
POST /api/v1/auth/login     -> { token, expiresIn }
POST /api/v1/cart/items {productId:1,quantity:2}   -> cart itemCount 2
POST /api/v1/cart/items {productId:2,quantity:1}   -> cart itemCount 3, total 37970
POST /api/v1/orders {"shippingAddress":{"street":"Av. Providencia 1234","city":"Santiago",
                     "region":"Region Metropolitana","zipCode":"7500000"}}
    -> 201  Location: /api/v1/orders/1   {"id":1,"status":"CONFIRMED","total":37970}
stock after:   product 1 = 23, product 2 = 39        <-- decremented, atomic
GET  /api/v1/cart           -> {"items":[],"itemCount":0,"total":0}   <-- cleared
GET  /api/v1/orders/1       -> full order: CONFIRMED, total 37970, 2 snapshot lines, address
POST /api/v1/orders (empty cart) -> 400
```

`PlaceOrderUseCase` is `@Transactional`: stock decrement → line snapshot → order save
→ cart clear, all-or-nothing (`PlaceOrderUseCaseTest` verifies the interaction order
and that nothing is saved/decremented when the cart is empty or a line is out of stock).

### 1c. Anonymous cart survives login (merge) — **met**

`src/services/cart.session.ts` subscribes to the auth `login` event (and runs once
on load when a token is already present, since login happens on `login.html` then
redirects); it calls `mergeLocalCart(readCart().items)` then `writeCart(EMPTY_CART)`.
Live: server cart qty 2 + local qty 3 for the same product → merged qty **5**
(`POST /api/v1/cart/merge`, clamped to stock).

### 1d. Admin data cycle — **met**

```
login as ROLE_ADMIN -> token
POST /api/v1/products {..., imageBase:"admin-test-hoodie", price:29990, stock:7}
    -> 201  Location: /api/v1/products/4
GET  /api/v1/products?q=Admin%20Test   -> the new product is in the customer catalog
PUT  /api/v1/products/4 {price:24990}  -> 200 ; GET /api/v1/products/4 -> price 24990
POST /api/v1/products  (non-admin token)  -> 403
DELETE /api/v1/products/4  (admin)         -> 204 ; GET /api/v1/products/4 -> 404
```

### 1e. CORS — **met**

`infrastructure/config/CorsConfig` — global `CorsConfigurationSource`, no
`@CrossOrigin` anywhere. `Access-Control-Allow-Origin: http://localhost:5173`
returned live on the `dev` **and** `prod` profiles; the `OPTIONS` preflight for
`GET`/`POST` returns the allow-methods header. `SecurityChainTest` pins it.

---

## Dimension 2 — Cumulative rigor (3 pts) — **MET**

Clean layers + tactical DDD + 100% of the core business rules, in English.

### 2a. `mvn -o clean verify` green — **met**

```
[INFO] Tests run: 302, Failures: 0, Errors: 0, Skipped: 0
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

### 2b. `jacoco:check` — 100% of the business rules — **met**

`pom.xml` `jacoco:check` scoped per CONVENTIONS §7:

| element | INSTRUCTION | BRANCH | measured |
|---------|-------------|--------|----------|
| PACKAGE `com.unicornt.store.domain(.*)`  | 100% | 100% | **100.00% / 100.00%** |
| PACKAGE `com.unicornt.store.application(.*)` | 100% | 95% | **100.00% / 100.00%** |
| BUNDLE (rest) | 85% | 75% | met |

The rules of PLAN §2.4 (Product price/name/stock, Category name + slugify + slug
uniqueness, Cart quantity/merge/ownership, PlaceOrder non-empty/stock/snapshot/
total/decrement/clear/atomicity, User password length + email uniqueness + default
role, ShippingAddress required fields) each map to a `*Test` in `domain` or
`application.usecase`.

### 2c. ArchUnit — hard rules, no freeze store — **met**

`src/test/resources/archunit_store/` and `archunit.properties` **deleted**.
`DependencyRulesTest` (9 plain `@ArchTest` rules), `LayeredArchitectureRulesTest`
(3), `TargetArchitectureRulesTest` (4, **0 skipped** — every Group C rule enabled).
All green. The frozen backlog went 664 → 457 → 407 → **0** across P1–P3.

### 2d. `domain` has no framework import — **met**

```
$ grep -rn "import org.springframework\|import jakarta.persistence\|import jakarta.validation" \
    src/main/java/com/unicornt/store/domain/
(no output)
$ grep -rn "import com.unicornt.store.infrastructure\|import org.springframework.data" \
    src/main/java/com/unicornt/store/application/
(no output)
```

`domain/model` and `domain/valueobject` are plain Java; invariants in the
constructor. `infrastructure/persistence/{entity,repository,mapper,adapter}` follows
the `*JpaEntity` / `SpringData*Repository` / `*PersistenceMapper` / `*RepositoryAdapter`
shape; adapters implement a `domain.repository` port. Controllers are thin
(request → use case → response).

### 2e. English throughout the backend — **met**

All identifiers, comments, Javadoc, `@DisplayName` prose, commit messages, migration
file names and OpenAPI descriptions are English. The only accented literal in a Java
source is `"Ñandú & Café!!"` in `CategoryTest` — deliberate test data for the
accent-stripping `slugify`.

---

## Dimension 3 — Production-grade security (3 pts) — **MET**

### 3a. Zero versioned secrets — **met**

```
$ git ls-files | grep -E '(^|/)\.env'          # backend
.env.example
$ git ls-files | grep -E '(^|/)\.env'          # frontend
.env.example
$ git log -p baseline-final-delivery..final-delivery | grep -iE 'password\s*=\s*\S|secret\s*=\s*\S'
(only ${ENV} placeholders, __CHANGE_ME__, and test-only keys)
```

`application-prod.yml` carries **no** literal credential — only `${DB_URL}`,
`${SPRING_DATASOURCE_*}`, `${APP_JWT_SECRET}`, `${APP_CORS_ALLOWED_ORIGINS}` style
placeholders. `.gitignore` (both repos) ignores `.env`, `.env.*`, keeps `.env.example`.
`.env.example` documents every key with placeholder values.

### 3b. Swagger only in dev (404 in prod) — **met**

```
# dev profile
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api-docs
200
# prod profile
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/v3/api-docs
404
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/swagger-ui/index.html
404
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api-docs
404
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/v1/products   # still works
200
```

`application*.yml` sets `springdoc.api-docs.enabled: false` / `swagger-ui.enabled: false`;
because springdoc 3.0.3 (pre-release Boot-4 line) ignores those flags,
`infrastructure/config/ProductionApiDocsGuard` — a `@Profile("prod")`
`OncePerRequestFilter` — returns a plain `404` for every `/v3/api-docs*`,
`/swagger-ui*`, `/api-docs*` path. Verified live under `prod`.

### 3c. CORS restricted to the frontend origin — **met** (see 1e).

---

## Definition of Done (PLAN §6)

| ✓ | Item |
|---|------|
| ✓ | Frontend TS compiles, tests green, 0 `any` / 0 unsafe non-null assertions |
| ✓ | Backend tests green (unit + ArchUnit, no freezing) — 302 tests |
| ✓ | `jacoco:check` 100% over `domain` + `application` |
| ✓ | Java domain free of Spring / JPA / Jakarta / infrastructure |
| ✓ | `application` depends only on domain + ports |
| ✓ | persistence implements the ports; JPA↔domain mapper |
| ✓ | Thin controllers: request → use case → response |
| ✓ | PostgreSQL comes up with Docker Compose (`docker compose config` valid) |
| ✓ | Swagger only in dev (404 in prod) |
| ✓ | CORS allows the Vite frontend |
| ✓ | Catalog from the real backend (no mock as source) |
| ✓ | Anonymous cart survives login (merge) |
| ✓ | Purchase creates an Order + decrements stock + clears the cart, transactional |
| ✓ | UI confirms the purchase (order id + cart cleared; 422 keeps the cart) |
| ✓ | Admin creates / edits / deletes products; persists in PostgreSQL |
| ✓ | No versioned production secrets |
| ✓ | README reproduces the system from scratch, both repos |
| ✓ | Backend code, names, comments and tests in English |

## Score

| Dimension | Points | Result |
|-----------|-------:|--------|
| 1 — end-to-end integration | 4 | **4 / 4** |
| 2 — cumulative rigor | 3 | **3 / 3** |
| 3 — production security | 3 | **3 / 3** |
| **Total** | **10** | **10 / 10** |

## Attribution sweep

```
git log --format='%an <%ae>%n%B' baseline-final-delivery..final-delivery \
  | grep -iE 'claude|anthropic|co-authored|generated with'
```

Backend: **empty**. Frontend: **empty**.
