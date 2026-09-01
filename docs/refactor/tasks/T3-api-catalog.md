# T3 — REST API: catalog

**Worktree:** `../unicornt-worktrees/api-catalog`
**Branch:** `refactor/h4-api-catalog`, cut from `refactor/hito4` after T0
**Covers:** plan stage 4 (products and categories), stage 5 applied to those
services, stage 7 annotations on those controllers
**Rubric:** C1
**Runs in parallel with:** T1, T2, T4

Read [../CONVENTIONS.md](../CONVENTIONS.md) first. You own the product and category
slice only. You never edit `pom.xml`, `application*.yml`, `SecurityConfig`, or any
file whose name starts with `Cart`, `Order`, `Address` or `Auth`.

**Reference style:** `demoApiRest/src/main/java/com/bootcamp/demoApiRest/controller/UserController.java`
and its `dto/` and `mapper/` in the main clone — semantic REST with
`@ResponseStatus(CREATED)`. Untracked, so read it by absolute path from
`C:/Users/Usuario/Proyectos/unicornt-store-backend/`.

---

## Endpoints

```
GET    /api/v1/products            200   (?category=&q=&page=&size=)
GET    /api/v1/products/{id}       200 / 404
POST   /api/v1/products            201 + Location    [ADMIN]
PUT    /api/v1/products/{id}       200 / 404         [ADMIN]
DELETE /api/v1/products/{id}       204 / 404         [ADMIN]

GET    /api/v1/categories          200
POST   /api/v1/categories          201 + Location    [ADMIN]
```

## Conventions (binding)

- `@RestController` + `@RequestMapping("/api/v1/<plural-noun>")`. No verb ever
  appears in a URL.
- The contract is a DTO, never an entity. Immutable `record`s in
  `infrastructure/web/dto`.
- Validation with `jakarta.validation`: `@NotBlank`, `@NotNull`, `@Positive`,
  `@PositiveOrZero`, and `@Valid` on every `@RequestBody`.
- Creation returns `ResponseEntity.created(URI.create("/api/v1/products/" + id))`.
- Deletion is `@ResponseStatus(HttpStatus.NO_CONTENT)` with a `void` method.
- The list endpoint is paginated: `Pageable` in, `Page<ProductResponse>` out.
- Mappers are static utility classes in `infrastructure/web/mapper`, entity to DTO
  and back. Do not introduce MapStruct for this slice — two resources do not justify
  the processor.

## Files you create

```
infrastructure/web/rest/ProductRestController.java
infrastructure/web/rest/CategoryRestController.java
infrastructure/web/dto/ProductDtos.java      (ProductCreateRequest, ProductUpdateRequest, ProductResponse)
infrastructure/web/dto/CategoryDtos.java     (CategoryCreateRequest, CategoryResponse)
infrastructure/web/mapper/ProductMapper.java
infrastructure/web/mapper/CategoryMapper.java
src/test/java/.../ProductRestControllerTest.java
```

## Files you may modify

`domain/service/ProductService`, `ProductServiceImpl`, and the category service if
one exists. Two changes are required there:

1. **Typed exceptions instead of silent absence.** Every lookup that used to return
   `null` or an empty `Optional` becomes
   `orElseThrow(() -> new ResourceNotFoundException("Product", id))`. Those
   exceptions already exist in `domain/exception` — T0 created them, do not
   redeclare them.
2. **A `search(String category, String q, Pageable pageable)` method** backed by a
   derived query or a JPQL `@Query` on `ProductRepository`. No `JdbcTemplate`.

The service layer must stay free of web types: no `org.springframework.web` or
`jakarta.servlet` import may appear under `domain/`.

## OpenAPI annotations (stage 7, done here)

Every endpoint you write carries `@Tag` on the class, `@Operation(summary = ...)` on
the method, and `@ApiResponses` listing the real status codes, with the error entries
pointing at `ErrorResponse`:

```java
@ApiResponse(responseCode = "404", description = "Product does not exist",
    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
```

Every field of every DTO record carries `@Schema(example = "...")`. Doing this now is
what lets T5 be a small task instead of a sweep across every controller.

## Tests

`@WebMvcTest(ProductRestController.class)` with a mocked `ProductService`, covering:

- `GET /api/v1/products/{id}` on a missing id → `404` with an `ErrorResponse` body
  carrying `code: RESOURCE_NOT_FOUND`
- `POST` with an invalid payload → `400` with a populated `errors[]`
- `POST` valid → `201` with a `Location` header
- `DELETE` → `204` with an empty body

## Definition of Done

```bash
mvn -q -DskipTests compile     # green
mvn -q test                    # green
grep -rn "org.springframework.web\|jakarta.servlet" src/main/java/com/unicornt/store/domain   # empty
```

Every endpoint above responds with the listed status code, verified with `curl` or
Bruno against a running instance.

## Handoff note

Write `docs/refactor/handoffs/api-catalog.md` from the template. Record: the exact
service signatures you changed (T4 may depend on `ProductService` for stock), any
`SecurityConfig` request matcher you need T2's file to carry, and the pagination
response shape you settled on.
