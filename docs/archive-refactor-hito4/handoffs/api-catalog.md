# Handoff — T3 api-catalog

**Branch:** `refactor/h4-api-catalog`
**Base commit:** `2dd80ae7c6a35414c98657fbb71a03272724b9a1` (tip of `refactor/hito4` at branch creation)
**Status:** complete

## What landed

- `domain/service/ProductService(Impl)`: added `search(String category, String q, Pageable pageable)`,
  backed by a new JPQL query on `ProductRepository`. Existing lookups already used
  `ResourceNotFoundException`; error messages were normalized to drop the `Entity` suffix
  (`"Product not found: 42"` instead of `"ProductEntity not found: 42"`) since that text is
  user-facing (see `ErrorResponse.message`).
- `domain/service/CategoryService(Impl)`: added `create(CategoryEntity category)` — validates a
  required name (max 100 chars), derives a URL-safe slug from the name when none is supplied
  (accent-stripping, lower-case, hyphen-separated), and throws `DuplicateResourceException` on a
  slug collision.
- `infrastructure/persistence/repository/ProductRepository`: added `search(...)`, a JPQL query
  matching an optional category (by slug or name, case-insensitive) and an optional free-text
  filter (name or description). The query string is exposed as the public constant
  `SEARCH_QUERY` so a unit test can run it directly against H2 without booting a repository bean.
- `infrastructure/web/dto/ProductDtos.java`: `ProductCreateRequest`, `ProductUpdateRequest`,
  `ProductResponse` records, `jakarta.validation` annotated, `@Schema` on every field.
- `infrastructure/web/dto/CategoryDtos.java`: `CategoryCreateRequest`, `CategoryResponse`.
- `infrastructure/web/mapper/ProductMapper.java`, `CategoryMapper.java`: static entity↔DTO
  translation, no MapStruct.
- `infrastructure/web/rest/ProductRestController.java`: `GET /api/v1/products` (paginated,
  `?category=&q=&page=&size=`), `GET /api/v1/products/{id}`, `POST` (201 + `Location`), `PUT`
  (200), `DELETE` (204). Every endpoint carries `@Tag`/`@Operation`/`@ApiResponses`. Writes carry
  `@PreAuthorize("hasRole('ADMIN')")` and `@SecurityRequirement(name = "bearerAuth")`.
- `infrastructure/web/rest/CategoryRestController.java`: `GET /api/v1/categories` (public),
  `POST /api/v1/categories` (201 + `Location`, ADMIN only).
- Tests:
  - `src/test/java/.../web/rest/ProductRestControllerTest.java` — `@WebMvcTest`, mocked
    `ProductService`, covers 404/`RESOURCE_NOT_FOUND`, 400 with populated `errors[]`, 201 with
    `Location`, 204 empty body, plus a page-envelope smoke check.
  - `src/test/java/.../web/rest/CategoryRestControllerTest.java` — same style for categories,
    including the 409 conflict path.
  - `src/test/java/.../domain/service/CategoryServiceImplTest.java` — unit tests for slug
    derivation, duplicate-slug rejection, blank-name rejection, unknown-id 404.
  - `src/test/java/.../domain/service/ProductServiceImplSearchTest.java` — unit tests that null
    filters reach the repository as empty strings, an unsorted `Pageable` gets the default `id`
    sort, and results are enriched with category/product-type names.
  - `src/test/java/.../persistence/repository/ProductSearchQueryTest.java` — runs
    `ProductRepository.SEARCH_QUERY` against a hand-bootstrapped Hibernate persistence unit on
    H2 (the Spring Data repository slice is not on this classpath), to catch a JPQL typo at build
    time instead of at application start-up.

## Verification actually run

| Check | Command | Result |
|-------|---------|--------|
| compile | `mvn -q -DskipTests compile` | pass |
| tests | `mvn -q test` | pass — `Tests run: 28, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`) |
| domain purity | `grep -rn "org.springframework.web\|jakarta.servlet" src/main/java/com/unicornt/store/domain` | empty, as required |
| presentation leftovers | `grep -rn "@Controller\|ModelAndView\|redirect:\|Thymeleaf" src/main` | empty |
| stack traces | `grep -rn "printStackTrace" src/main` | empty |
| live endpoints | packaged the jar (`mvn -q -DskipTests package`) and ran it against the real `unicornt-postgres` container on `SERVER_PORT=8090` (host port 8080 was left untouched) | all listed endpoints returned the documented status code — see below |

Live verification detail (against the seeded dev data):

- `GET /api/v1/products?page=0&size=5` → 200, paginated envelope with `content`, `totalElements`,
  `totalPages`, `categoryName`/`productTypeName` populated.
- `GET /api/v1/products/1` → 200.
- `GET /api/v1/products/999999` → 404, `code: RESOURCE_NOT_FOUND`.
- `GET /api/v1/products?category=unicorns` → 200, filtered to 1 item.
- `GET /api/v1/categories` → 200, seeded categories.
- `POST /api/v1/categories` unauthenticated → 403 (`ACCESS_DENIED`) — confirms `@PreAuthorize`
  fails closed even under T2's current permissive stub filter chain.
- `POST /api/v1/products`, `PUT /api/v1/products/1`, `DELETE /api/v1/products/1` unauthenticated →
  403 each.
- The manual instance was stopped cleanly afterward (`taskkill`); the pre-existing
  `unicornt-store-springboot-unicornt-store-1` container on host port 8080 was never touched.

## Requests for the orchestrator

| File | Change needed | Why |
|------|---------------|-----|
| `infrastructure/security/SecurityConfig.java` (T2) | No change strictly required — `@PreAuthorize` already fails closed for anonymous requests under the current `permitAll()` stub, and will keep working once T2 wires the JWT filter and `hasRole`-aware authorities. For defense in depth, T2 may still want explicit matchers: `GET /api/v1/products/**` and `GET /api/v1/categories/**` → `permitAll()`; `POST/PUT/DELETE /api/v1/products/**` and `POST /api/v1/categories/**` → `hasRole("ADMIN")`. | Keeps the filter-chain-level rule consistent with the method-level one and gives a matcher-based 401 (via `RestAuthEntryPoint`) instead of falling through to method security's 403 when there is no token at all. |

No other cross-boundary changes were needed. `pom.xml` was not touched; every dependency used
(`spring-boot-starter-validation`, `springdoc-openapi-starter-webmvc-ui`,
`spring-boot-starter-security`, H2, Hibernate) was already present.

## Decisions taken

- **Pagination response shape**: kept Spring Data's native `Page<ProductResponse>` serialization
  (via `Page::map`) rather than introducing a custom envelope or `PagedModel`. The JSON exposes
  `content`, `totalElements`, `totalPages`, `number`, `size`, `pageable`, `sort`, `first`, `last`,
  `empty`. Spring Boot logs a `WarningLoggingModifier` notice at each request warning this shape
  is not guaranteed stable; if T5 or the orchestrator wants a stable contract later, switch to
  `@EnableSpringDataWebSupport(pageSerializationMode = PagedModel)` globally — that is a
  cross-cutting `@SpringBootApplication`-level change outside this task's ownership set, so it is
  left as a suggestion, not applied.
- **`search` signature**: `Page<ProductEntity> search(String category, String q, Pageable pageable)`
  on `ProductService`, matching the task file's suggested signature exactly. Both filters accept
  `null` and are normalized to `""` before hitting the repository, which disables them in the
  JPQL `where` clause. `category` matches either the category slug or its display name,
  case-insensitively, since the reference UI exposes both.
- **Sort default**: when the caller's `Pageable` carries no explicit sort (the common case from
  `?page=&size=` alone), the service substitutes `Sort.by(ASC, "id")` so list order is
  deterministic; an explicit `?sort=name,desc` from the caller is preserved as-is.
- **Error message text**: normalized `"ProductEntity not found: 42"` → `"Product not found: 42"`
  and `"CategoryEntity not found: 3"` → `"Category not found: 3"` in the exceptions thrown by
  `ProductServiceImpl`/`CategoryServiceImpl`, since `ResourceNotFoundException#getMessage()` is
  surfaced verbatim in `ErrorResponse.message`, which CONVENTIONS ties to the "English, user
  facing" rule. `GlobalExceptionHandler` itself was not touched.
- **`active` field default**: `ProductCreateRequest.active` and `ProductUpdateRequest.active` are
  nullable `Boolean`; the mapper treats a missing/`null` value as `true` so a minimal payload
  still creates a visible product, matching the previous admin-controller default.
- **Category slug validation**: `CategoryCreateRequest.slug` is optional and, when given, is
  additionally validated with a `@Pattern` for `lower-case-with-hyphens`-shaped input at the DTO
  level; the service still re-derives/normalizes it through `slugify()` for the actual persisted
  value, so a client-supplied slug and a name-derived slug go through the same collision check.

## Known gaps

- No gap in the endpoints or DTOs specified by the task file. `PUT` currently requires every
  field (full replacement, per the "Conventions" section — no `PATCH` was requested).
- The `Page` JSON envelope is Spring Data's default, not a custom stable contract; see "Decisions
  taken" above if a locked-down shape is wanted for the public API docs before the milestone
  ships.
- Category update/delete endpoints were not requested by the task's endpoint list and were not
  added.

## Attribution check

```
git log --format='%an <%ae>%n%B' refactor/hito4..HEAD | grep -iE 'claude|anthropic|co-authored|generated with'
```

Result: empty
