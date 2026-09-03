# Handoff — P1 catalog slice (reference pattern)

**Repos / branches:** `unicornt-store-backend @ final-delivery` · `unicornt-store-frontend @ final-delivery`
**Base commit(s):** backend `cc859e2` · frontend `868bc2d`
**Status:** complete

## What landed

### Backend

- `domain/valueobject/Money` (whole CLP, `>= 0`, `plus`/`times`/`isPositive`),
  `domain/valueobject/Quantity` (`>= 1`, `plus`).
- `domain/model/Product` (invariants: name required + `<= 200`, price positive,
  stock `>= 0`, category/type id positive; `create`, `withStock`, `hasStockFor`),
  `domain/model/Category` (name required + `<= 100`, `slugify` strips combining
  marks then non-alphanumerics), `domain/model/ProductType`.
- `domain/repository`: `ProductRepository`, `CategoryRepository`,
  `ProductTypeRepository` (pure ports) + `PageResult<T>` record.
- `application/usecase/catalog`: `ListProductsUseCase`, `GetProductUseCase`,
  `CreateProductUseCase`, `UpdateProductUseCase`, `DeleteProductUseCase`,
  `ListCategoriesUseCase`, `CreateCategoryUseCase`, `ProductCommand`. Spring
  `@Service` + `@Transactional` (guide layout allows `application` to be Spring-wired;
  no `org.springframework.data` import there).
- `infrastructure/persistence`: entities renamed `*Entity -> *JpaEntity`
  (`ProductJpaEntity` gains the `stock` column); `SpringData{Product,Category,ProductType}Repository`;
  `{Product,Category,ProductType}PersistenceMapper`;
  `{Product,Category,ProductType}RepositoryAdapter` (`@Component`, implement the ports,
  enrich category/type name labels at the boundary).
- `infrastructure/web`: thin `ProductRestController` / `CategoryRestController` over the
  use cases; `ProductDtos` rewritten (`ProductResponse` + **`ProductPageResponse`
  envelope** `{content,page,size,totalElements,totalPages}` — no Spring `Page` on the
  wire); `ProductRestMapper` / `CategoryRestMapper`. `@Tag`/`@Operation`/`@ApiResponses`/`@Schema`
  kept. Writes carry `@PreAuthorize("hasRole('ADMIN')")`.
- Deleted: `ProductService(Impl)`, `CategoryService(Impl)`, web `ProductMapper`/`CategoryMapper`,
  legacy Spring Data `Product/Category/ProductTypeRepository`, and their tests
  (`ProductServiceImplTest`, `ProductServiceImplSearchTest`, `CategoryServiceImplTest`,
  `ProductMapperTest`, `CategoryMapperTest`).
- Collateral rename (kept the build green, cart/ordering territory): `CartServiceImpl`,
  `CheckoutServiceImpl`, `CartItemEntity`, `OrderStockRepository`, `ErrorResponse` example
  string, and the tests `CartServiceImplTest` / `CheckoutServiceImplTest` /
  `ProductSearchQueryTest` now reference `ProductJpaEntity` / `SpringDataProductRepository`.
- Tests added: `MoneyTest`, `QuantityTest`, `ProductTest`, `CategoryTest`, `ProductTypeTest`,
  `PageResultTest`, one `*UseCaseTest` per use case, `Product/Category/ProductTypePersistenceMapperTest`,
  `Product/Category/ProductTypeRepositoryAdapterTest`; `ProductRestControllerTest` /
  `CategoryRestControllerTest` rewritten with `@MockitoBean` of the use cases.

### Frontend

- `src/models/product.dto.ts` rewritten to the backend `ProductResponse` shape;
  `isProductDto`, `isProductDtoArray`, **`isProductPageDto`**, and `toProductModel`
  now live here (reverses the H3 leak: `product.model.ts` no longer imports the DTO).
- `src/models/product.model.ts`: `ProductModel` = `{id,name,category,subcategory,price,
  description,image}` + optional `{categoryId,stock,active}`; `productImageSrc`, `isPurchasable`.
- `src/api/product.api.ts`: `fetchProductsPayload({category?,q?})` builds the query string.
- `src/services/product.service.ts`: validates the page envelope, maps `content` -> `ProductModel[]`,
  forwards `{category,q}`.
- New `src/api/category.api.ts`, `src/models/category.{dto,model}.ts`,
  `src/services/category.service.ts`.
- `src/views/catalog.view.ts`: best-effort category `<select>` filter from
  `GET /api/v1/categories`, re-renders the list on change; omitted silently if the
  request fails.
- Tests: `product.dto.test.ts` / `product.model.test.ts` / `product.service.test.ts`
  rewritten (English); new `category.dto.test.ts`, `category.service.test.ts`,
  `http.test.ts`. Deleted `src/data/products-payload.test.ts` (mock no longer a source).

## Verification actually run

| Check | Command | Result |
|-------|---------|--------|
| backend compile | `mvn -q -o -DskipTests compile` | pass |
| backend tests | `mvn -q -o test` | pass — 246 tests, 1 skipped |
| backend verify | `mvn -q -o verify` (jacoco + ArchUnit) | pass |
| catalog coverage | jacoco.xml | `domain.model` 100%/100%, `domain.valueobject` 100%/100%, `domain.repository` 100%/100%, `application.usecase.catalog` 100%/100% |
| frontend build | `npm run build` | pass |
| frontend tests | `npm test` | pass — 31 files, 177 tests |
| frontend lint | `npm run lint` | pass |
| gate — read cycle | docker `db` + backend `dev` + `curl` | pass (see below) |

### Gate transcript (PostgreSQL -> backend -> API)

```
GET /api/v1/products            200  {"content":[{ id, name, description, imageBase, price,
                                      categoryId, categoryName, productTypeId, productTypeName,
                                      stock, active } x3], page:0, size:20, totalElements:3, totalPages:1}
                                     Access-Control-Allow-Origin: http://localhost:5173
GET /api/v1/products?category=unicorns  200  content length 1
GET /api/v1/products/2          200  single ProductResponse
GET /api/v1/products/999        404
POST /api/v1/products (no token) 401
GET /api/v1/categories          200  [{id,name,slug} x3]
```

The response body is exactly what the frontend `isProductPageDto` + `isProductDto`
guards accept (same shape as the Vitest fixtures). A live browser render was not
captured (no headless browser in this environment); the contract match is proven by
the shared fixture shape and the passing frontend tests.

## ArchUnit

| Rule (Group B frozen) | Before P1 | After P1 |
|------|-------:|-------:|
| domain -X-> org.springframework.. | 73 | 43 |
| domain -X-> ..infrastructure.. | 225 | 129 |
| LayeredArchitecture | 299 | 168 |
| no package cycles | 47 | 117 (see note) |
| **total lines** | 644 | **457** |

Cycle-edge count rose because the new `application` layer joins the
`domain <-> infrastructure` cycle via the still-present legacy `domain.service.*`;
it collapses as P2/P3 delete those. Baseline refrozen and committed on `final-delivery`.
Catalog Group C rules enabled.

## Requests for the orchestrator

None outside P1's own ownership (P1 is orchestrator-run). For the workers branching
from this tip:

| Area | Note |
|------|------|
| `SpringDataProductRepository` | now `JpaRepository<ProductJpaEntity, Integer>` with `search(category,q,Pageable)` + `SEARCH_QUERY`. Cart reads it directly today; P2a may keep that or add a `Cart`-owned method. |
| `ProductRepository` (domain port) | P3 ordering reads stock via `findById(long) -> Optional<Product>` then `product.stock()` / `product.hasStockFor(n)` / `product.withStock(n)`. |
| `PageResult<T>` | `record PageResult<T>(List<T> content, int page, int size, long totalElements)` with `totalPages()`. |
| DTO field names (frozen in CONTRACT.md) | product: `id,name,description,imageBase,price,categoryId,categoryName,productTypeId,productTypeName,stock,active`; page envelope: `content,page,size,totalElements,totalPages`. |
| `SecurityConfig` | unchanged; product/category write matchers already present from before. |

## Decisions taken

- `GET /api/v1/products` returns a hand-rolled `ProductPageResponse` envelope, not a
  Spring `Page` (that leaked `pageable`/`sort` internals to the client). The frontend
  validates this envelope with `isProductPageDto`.
- `ProductModel` keeps its legacy field names so the existing component tree
  (`ProductCard`, `ProductDetail`, `cart.view`, ...) needs no change; `categoryId`,
  `stock`, `active` are optional to keep legacy fixtures compiling.
- Legacy JPA entities were **renamed** (not dual-mapped) to `*JpaEntity` per the phase
  file; the ~6 cart/ordering references were updated mechanically.
- `Category` creation (`POST /api/v1/categories` [ADMIN]) kept so the slug-uniqueness
  business rule (PLAN §2.4) has a real code path and test.

## Known gaps

- Live browser render of the catalog not captured (environment has no headless
  browser). Everything up to and including the HTTP contract is verified.
- `domain.service.*` (cart, checkout, address, user) still violate the layer rules —
  expected; owned by P2/P3, frozen in the baseline.

## Attribution check

```
git log --format='%an <%ae>%n%B' baseline-final-delivery..HEAD | grep -iE 'claude|anthropic|co-authored|generated with'
```

Result — backend: empty · frontend: empty.
