# Unicornt Store — Backend

REST microservice for the Unicornt store: product catalog, shopping cart, checkout
and JWT authentication. Every route returns JSON; the API is described by an
OpenAPI 3 document (`docs/openapi.json`). This service is the single source of
truth for the [Unicorn't Store frontend](../unicornt-store-frontend) (Vite + strict
TypeScript) — together they are the *Final Delivery* full-stack system.

## Stack

| Layer | Technology |
|-------|------------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.8, Spring MVC (REST only) |
| Security | Spring Security 7, stateless JWT bearer tokens |
| Persistence | Spring Data JPA, PostgreSQL 16 |
| Schema | Versioned SQL under `src/main/resources/db/migration` (applied by Spring SQL init) |
| API docs | springdoc-openapi (Swagger UI) — **`dev` profile only** |
| Build | Maven Wrapper (`./mvnw`), executable JAR |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc, H2; ArchUnit for the architecture rules |

## Architecture — clean layers + tactical DDD

The dependency rule points inward only. It is enforced hard by ArchUnit
(`src/test/java/com/unicornt/store/architecture/`), with **no freeze baseline** —
a new violation fails the build.

```
com.unicornt.store
├── domain/                 pure Java — zero Spring / JPA / Jakarta / web
│   ├── model/              Product, Category, ProductType, Cart, CartItem,
│   │                       Order, OrderItem, OrderStatus, ShippingAddress, User
│   ├── valueobject/        Money, Quantity            (validate in the constructor)
│   ├── repository/         ports: ProductRepository, CategoryRepository,
│   │                       CartRepository, OrderRepository, StockRepository,
│   │                       UserRepository, RoleRepository, PageResult<T>
│   └── exception/          ResourceNotFoundException, OutOfStockException,
│                           DuplicateResourceException
├── application/
│   └── usecase/{catalog,cart,ordering,identity}/   one class per use case,
│                           depends only on domain ports (Spring-wired @Service)
└── infrastructure/
    ├── web/{rest,dto,mapper,error}     thin controllers: request → use case → response
    ├── persistence/{entity,repository,mapper,adapter}
    │        *JpaEntity  ↔  *PersistenceMapper  ↔  domain model
    │        SpringData*Repository (technology)  ←  *RepositoryAdapter implements a domain port
    ├── security/           JWT filter, JwtService, CustomUserDetailsService, SecurityConfig
    └── config/             CorsConfig, OpenApiConfig, ProductionApiDocsGuard
```

```
infrastructure ──► application ──► domain          domain depends on nobody
infrastructure ──► domain  (implements domain.repository, maps to domain.model)
web  -X► persistence      application -X► infrastructure / spring-data
```

## Requirements

- JDK 25 (`java -version`)
- Docker + Docker Compose
- Node 20+ (only to run the frontend)

## Environment variables

Copy `.env.example` to `.env` and replace every `__CHANGE_ME__`. Nothing is
hard-coded; every credential is read from the environment. Key entries:

| Variable | Purpose |
|----------|---------|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` | PostgreSQL container |
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | datasource when running outside compose |
| `APP_JWT_SECRET` | JWT signing key, base64, ≥ 32 bytes (`openssl rand -base64 32`) |
| `APP_CORS_ALLOWED_ORIGINS` | allowed browser origins, comma-separated (default `http://localhost:5173`) |
| `SPRING_PROFILES_ACTIVE` | `dev` (schema kept in sync, Swagger UI) or `prod` (validate only, no docs) |
| `APP_BOOTSTRAP_ADMIN_EMAIL` / `APP_BOOTSTRAP_ADMIN_PASSWORD` | optional one-time admin bootstrap |

## Reproduce from scratch

```bash
# 1. clone both repos side by side
git clone <backend> unicornt-store-backend
git clone <frontend> unicornt-store-frontend
cd unicornt-store-backend
cp .env.example .env            # fill in the __CHANGE_ME__ values

# 2. PostgreSQL
docker compose up -d db

# 3. backend — tests, then run (dev profile)
./mvnw clean verify             # unit + ArchUnit + jacoco:check
set -a && . ./.env && set +a
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
#   API      → http://localhost:8080/api/v1
#   Swagger  → http://localhost:8080/swagger-ui.html   (dev only; 404 under prod)
#   spec     → http://localhost:8080/api-docs          (dev only)

# 4. frontend
cd ../unicornt-store-frontend
cp .env.example .env.local      # VITE_API_BASE_URL=http://localhost:8080
npm install
npm run dev                     # http://localhost:5173
```

Full stack up in Docker (backend + db): `docker compose up -d`.

## Tests

```bash
./mvnw clean verify
```

- 302 tests. `jacoco:check`: `domain` and `application` at **100% / 100%**, the rest
  at 85% / 75%.
- ArchUnit dependency rules are **hard** (no freeze store); every layered rule green.

## API

- Base URL: `http://localhost:8080/api/v1`
- Error shape: `{ "message", "code", "status", "timestamp", "path", "errors": [] }`
  — codes `VALIDATION_ERROR`, `RESOURCE_NOT_FOUND`, `BUSINESS_RULE_VIOLATION`,
  `RESOURCE_CONFLICT`, `UNAUTHORIZED`, `ACCESS_DENIED`, `INTERNAL_ERROR`.
- Frozen contract: `docs/openapi.json`. Design record: `docs/final-delivery/CONTRACT.md`.

Endpoints: `GET/POST/PUT/DELETE /products`, `GET/POST /categories`,
`POST /auth/register`, `POST /auth/login`, `GET /auth/me`,
`GET/POST /cart`, `PUT/DELETE /cart/items/{productId}`, `POST /cart/merge`,
`POST /orders`, `GET /orders`, `GET /orders/{id}`.

## Demo credentials

The app ships **no** accounts. Create one:

```bash
# a regular customer
curl -sX POST http://localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","password":"secret1"}'

# an administrator: start the backend once with
#   APP_BOOTSTRAP_ADMIN_EMAIL=admin@unicornt.local APP_BOOTSTRAP_ADMIN_PASSWORD=admin12345
```

## End-to-end flow (the purchase cycle)

`register → login (JWT) → GET /products (from PostgreSQL) → POST /cart/items →
POST /orders {shippingAddress} → order CONFIRMED, product stock decremented, cart
cleared, all in one transaction`. The anonymous `localStorage` cart is merged into
the server cart on login (`POST /cart/merge`, `qty_final = qty_local + qty_server`
subject to stock).
