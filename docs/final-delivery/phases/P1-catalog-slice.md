# P1 — Catalog slice (reference pattern)

**Lane:** orchestrator, sequential. Work on `final-delivery` in both main clones.
**Plan:** [PLAN.md](../PLAN.md) §4 Phase 1, §2.
**Rubric:** dimensions 1 and 2.
**Follow:** [../slice-recipe.md](../slice-recipe.md) step by step.

This slice **is the spec** for P2–P4. Its layout, adapter/mapper shape, test split
and DTO conventions are copied by every later slice. Do not fan out until it is
merged, green and its gate demonstrated.

Read [../CONVENTIONS.md](../CONVENTIONS.md) and `docs/final-delivery/CONTRACT.md`
first.

---

## Scope

Backend: `Product`, `Category`, `Money`, `Quantity`. Product write endpoints
(`POST/PUT/DELETE`) are included here with `@PreAuthorize("hasRole('ADMIN')")` — P4
only adds the admin UI.

Frontend: real product + category consumption; `ProductDto → ProductModel`.

## Backend (recipe steps 1–5)

1. **Domain** — `domain/model/Product`, `Category`; `domain/valueobject/Money`,
   `Quantity`. Rules (PLAN.md §2.4): `price > 0`, `name` non-empty, `name.length ≤ 200`,
   `stock ≥ 0`; category name required, `≤ 100`, slug uniqueness + `slugify`.
   Tests: `ProductTest`, `CategoryTest`, `MoneyTest`, `QuantityTest`.
2. **Ports** — `domain/repository/ProductRepository`, `CategoryRepository`. Returns
   `Product` / `Category` / `List<…>`; a domain `PageResult<Product>` record for the
   paginated list (no Spring `Page`).
3. **Use cases** — `application/usecase/catalog/`: `ListProductsUseCase`,
   `GetProductUseCase`, `SearchProductsUseCase`, `CreateProductUseCase`,
   `UpdateProductUseCase`, `DeleteProductUseCase`, `ListCategoriesUseCase`. Full
   `validate` walk in create/update. Tests migrate scenarios from
   `ProductServiceImplTest` (30) + `ProductServiceImplSearchTest` (3) +
   `CategoryServiceImplTest` (9).
4. **Persistence** — rename `ProductEntity` → `ProductJpaEntity`,
   `CategoryEntity` → `CategoryJpaEntity`; `SpringDataProductRepository`,
   `SpringDataCategoryRepository`; `ProductPersistenceMapper`,
   `CategoryPersistenceMapper`; `ProductRepositoryAdapter`,
   `CategoryRepositoryAdapter`. Migrate `ProductSearchQueryTest`.
5. **Web** — `ProductRestController`, `CategoryRestController` (thin);
   `ProductDtos`, `CategoryDtos` (records + `@Schema`); `ProductRestMapper`,
   `CategoryRestMapper`. `@Tag`/`@Operation`/`@ApiResponses`. Migrate
   `ProductRestControllerTest`, `CategoryRestControllerTest` with `@MockitoBean` of
   the use cases.

Endpoints (from CONTRACT.md):

```
GET    /api/v1/products            200   (?category=&q=&page=&size=)
GET    /api/v1/products/{id}       200 / 404
POST   /api/v1/products            201 + Location   [ADMIN]
PUT    /api/v1/products/{id}       200 / 404        [ADMIN]
DELETE /api/v1/products/{id}       204 / 404        [ADMIN]
GET    /api/v1/categories          200
```

## Frontend (recipe steps 6–8)

- `api/product.api.ts` via `apiFetch`; `models/product.dto.ts` + `isProductDto`;
  `models/product.model.ts` + `toProductModel`. Reverse the H3 leak noted in the
  frontend diagnosis: `ProductModel` must not import `ProductDto`; the mapper bridges.
- Catalog view renders `ProductModel[]` from the live API. Category filter uses
  `GET /api/v1/categories`.
- Vitest: valid payload, non-array payload, one invalid element, `ApiError` from the
  network.

## ArchUnit

After the slice compiles and tests pass, enable the Group C rules scoped to
`..catalog..` and to `Product*` / `Category*` persistence classes. Regenerate the
freezing baseline (catalog violations → 0) and commit it.

---

## Definition of Done

```bash
mvn -q -DskipTests compile
mvn -q test
mvn -q verify                         # jacoco:check 100% over domain.model of catalog
grep -rn "org.springframework\|jakarta.persistence" \
  src/main/java/com/unicornt/store/domain/model \
  src/main/java/com/unicornt/store/domain/valueobject     # empty
npm run build && npm test && npm run lint                 # frontend
```

## Gate

`PostgreSQL → backend → GET /api/v1/products → Vite frontend → catalog visible`,
with no mock as primary source, no CORS error, `npm run build` clean, `0 any`.
Capture the request/response and a screenshot-equivalent description.

## Handoff

`docs/final-delivery/handoffs/P1-catalog.md`. Record: the exact `domain.repository`
signatures (P3 ordering reads `ProductRepository` for stock), the `PageResult` shape,
any `SecurityConfig` matcher the write endpoints need, the DTO field names now fixed
in CONTRACT.md, and the new ArchUnit baseline count.
