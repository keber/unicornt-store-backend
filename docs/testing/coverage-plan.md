# Plan: aumento de cobertura de tests unitarios

Rama de trabajo: `test/coverage-boost` (desde `dev`).

## Resultado (post-exclusiones JaCoCo, 2026-09-01)

| Métrica | Baseline | Final | Umbral `jacoco:check` |
|---|---|---|---|
| Instrucciones | 51.2 % | **96.1 %** | ≥ 92 % |
| Ramas | 31.4 % | **93.2 %** | ≥ 88 % |
| Líneas | 53.6 % | 96.0 % | — |
| Métodos | 52.8 % | 93.2 % | — |

- Tests: 53 → **198** (+145). `mvn verify` completo en ~80 s.
- Sin ningún `@SpringBootTest` nuevo; solo unit tests y los slices `@WebMvcTest` preexistentes.
- CSVs: `docs/testing/coverage-baseline.csv`, `docs/testing/coverage-after.csv`.

### Archivos de test añadidos / modificados
| Archivo | Tests |
|---|---|
| `domain/service/CartServiceImplTest` (nuevo) | 22 |
| `domain/service/CheckoutServiceImplTest` (nuevo) | 10 |
| `domain/service/AddressServiceImplTest` (nuevo) | 20 |
| `domain/service/ProductServiceImplTest` (nuevo) | 30 |
| `domain/service/CategoryServiceImplTest` (ampliado) | 3 → 9 |
| `infrastructure/web/mapper/*MapperTest` (5 nuevos) | 33 |
| `infrastructure/security/CustomUserDetailsServiceTest` (nuevo) | 4 |
| `infrastructure/security/JwtAuthFilterTest` (nuevo) | 9 |
| `infrastructure/web/error/GlobalExceptionHandlerTest` (nuevo) | 12 |

### Exclusiones de cobertura (en `pom.xml`, plugin jacoco)
- `StoreApplication`
- `infrastructure/config/**`
- `infrastructure/web/dto/**` (records de transporte)
- `infrastructure/persistence/entity/**` (POJOs)
- `infrastructure/web/error/ErrorResponse*`

### Requisito de entorno (ya aplicado en `pom.xml`)
Java 25 + Mockito exige `-Dnet.bytebuddy.experimental=true`. Añadido al `argLine`
de surefire como `@{argLine} -Dnet.bytebuddy.experimental=true` (preserva el agente
de JaCoCo). Sin esto, todo test con mocks falla al cargar el contexto.

## Contrato de testing (obligatorio para todos los subagentes)

1. AAA / Given-When-Then explícito; un concepto por test.
2. Nombres de comportamiento + `@DisplayName` en prosa (ver `CategoryServiceImplTest`).
3. Mockito solo para colaboradores de frontera (repositorios, `PasswordEncoder`,
   servicios colaboradores). Nunca mockear la clase bajo prueba.
4. AssertJ (`assertThat`, `assertThatThrownBy`) como estándar en archivos nuevos.
5. `verify(...)` solo cuando la interacción es parte del contrato (p. ej. `clearCart`
   tras `confirm`, `save` NO invocado en caminos de error). Sin over-verification.
6. Casos límite y de error primero: null/blank, cantidades ≤ 0, límites de longitud,
   `Optional.empty()`, colecciones vacías, ownership (recurso de otro usuario → 404).
7. Datos de prueba vía builders / object mothers privados (`aProduct()`, `aUser()`).
8. Determinismo: sin `Thread.sleep`, sin dependencia de orden. SUT en `@BeforeEach`.
9. `@Nested` para agrupar por método/escenario en clases grandes.
10. `@ParameterizedTest` para tablas de validación.
11. Sin lógica en el test (nada de `if`/`for` que reproduzca el SUT).
12. `BigDecimal`: comparar con `isEqualByComparingTo`, no `equals`.
13. Test en paquete espejo del `main`. **Prohibido** `@SpringBootTest`. Los slices
    `@WebMvcTest` existentes se mantienen; no se añaden nuevos salvo indicación.
14. No tocar `pom.xml` ni ningún archivo bajo `src/main/`. Solo crear/editar
    los `*Test.java` del lote asignado.

## Lotes (paralelos, archivos disjuntos)

### Lote A — Cart
- Nuevo: `src/test/java/com/unicornt/store/domain/service/CartServiceImplTest.java`
- SUT: `CartServiceImpl` (mock de `CartItemRepository`, `UserRepository`, `ProductRepository`)
- Cubrir: `getCart` (total, itemCount, líneas), `getCartItems` (filtra productos borrados),
  `addItem` (línea nueva vs merge de cantidad, qty ≤ 0 → `IllegalArgumentException`,
  producto inexistente → `ResourceNotFoundException`, usuario inexistente → 404),
  `updateItemQuantity` (ok, qty ≤ 0, item de otro usuario → 404, producto borrado → 404),
  `removeItem` (ok, ownership), `clearCart`.
- BigDecimal con `isEqualByComparingTo`.

### Lote B — Checkout
- Nuevo: `src/test/java/com/unicornt/store/domain/service/CheckoutServiceImplTest.java`
- SUT: `CheckoutServiceImpl` (mock de `CartService`, `AddressService`, `OrderRepository`,
  `OrderStockRepository`, `UserRepository`)
- Cubrir: `confirm` con carrito vacío → `IllegalArgumentException`; stock insuficiente
  (`decreaseStock` devuelve 0) → `OutOfStockException`; camino feliz: calcula total,
  arma `OrderItemEntity`, persiste, invoca `clearCart`; `findOrders`; `findOrder`
  (ok / inexistente → `ResourceNotFoundException`); usuario inexistente → 404.

### Lote C — Address
- Nuevo: `src/test/java/com/unicornt/store/domain/service/AddressServiceImplTest.java`
- SUT: `AddressServiceImpl` (mock de `AddressRepository`, `UserRepository`)
- Cubrir: `findByUser`; `findByUserAndId` (ok / de otro usuario → 404 / inexistente → 404);
  `create` (`@ParameterizedTest` sobre `validate`: street/city/region null y blank;
  primer address del usuario → `default = true`, siguiente → `false`; fuerza `id = null`);
  `delete` (delega en `findByUserAndId`); usuario inexistente → 404.

### Lote D — Product service
- Nuevo: `src/test/java/com/unicornt/store/domain/service/ProductServiceImplTest.java` (con `@Nested`)
- SUT: `ProductServiceImpl` (mock de `ProductRepository`, `CategoryRepository`, `ProductTypeRepository`)
- **No duplicar** lo ya cubierto por `ProductServiceImplSearchTest` (`search`); extender lo demás.
- Cubrir: `findAll(name, categoryId)` con y sin categoría; `findAll(name, cat, limit, offset)`
  (`limit ≤ 0` y `offset < 0` → `IllegalArgumentException`, cálculo de página);
  `countAll` (con/sin categoría); `findById` (ok / inexistente → `ResourceNotFoundException`);
  `create` y `update` recorriendo `validate` completo (nombre null/blank, nombre > 200,
  price ≤ 0, categoryId ≤ 0, categoría inexistente, productTypeId ≤ 0, tipo inexistente);
  `update` de id inexistente → 404; `delete` (existe / no → 404); `enrich` (rellena
  `categoryName` y `productTypeName`); `findAllProductTypes`.

### Lote E — Mappers + gaps de Category
- Nuevos: `ProductMapperTest`, `AddressMapperTest`, `OrderMapperTest`, `CartMapperTest`,
  `CategoryMapperTest` (en `.../infrastructure/web/mapper/`)
- Editar (ownership exclusivo de este lote):
  `src/test/java/com/unicornt/store/domain/service/CategoryServiceImplTest.java`
- Cubrir mappers: `toResponse` y `toEntity` incluyendo ramas null/`trim`, `price`/`categoryId`/
  `productTypeId` null → 0, `active` null → `true`, `OrderMapper` con `status` null,
  colecciones vacías.
- Category (ampliar): `findAll`; `create` con slug explícito (no derivado del nombre);
  nombre > 100 → `IllegalArgumentException`; slug que queda vacío tras `slugify` →
  `IllegalArgumentException`.

### Lote F — Seguridad + manejo de errores
- Nuevos: `CustomUserDetailsServiceTest`, `JwtAuthFilterTest`
  (en `.../infrastructure/security/`), `GlobalExceptionHandlerTest`
  (en `.../infrastructure/web/error/`)
- `CustomUserDetailsService`: usuario inexistente → `UsernameNotFoundException`;
  mapea roles a `SimpleGrantedAuthority`; email y password propagados.
- `JwtAuthFilter`: sin header → contexto anónimo, `chain.doFilter` invocado;
  header sin `Bearer ` → ignorado; token inválido (`JwtService` real con secreto de test
  o mock que lanza `JwtException`) → contexto limpio; roles null en claims → sin authorities;
  no sobreescribe autenticación existente. Usar `MockHttpServletRequest/Response` y
  `MockFilterChain`.
- `GlobalExceptionHandler`: instanciar directo, pasar cada excepción + `MockHttpServletRequest`,
  verificar `HttpStatus` y `code` del `ErrorResponse`: `ResourceNotFoundException` → 404,
  `OutOfStockException` → 422, `DuplicateResourceException` → 409,
  `MethodArgumentNotValidException` y `ConstraintViolationException` → 400 con `errors`,
  `HttpMessageNotReadableException` → 400, `IllegalArgumentException` → 400,
  `DataIntegrityViolationException` → 409, `NoResourceFoundException` → 404,
  `AccessDeniedException` → 403, `AuthenticationException` → 401, `Exception` → 500.

## Fases de ejecución (coordinador)

0. **[hecho]** baseline + `pom.xml` (exclusiones, `jacoco:check`, argLine bytebuddy). Commit.
1. Despachar lotes A–F en paralelo (subagentes `general-purpose`), cada uno con este
   documento + su sección.
2. Por lote entregado: `./mvnw -B test -Dtest=<Clase>` en verde; si falla, devolver al worker.
3. `./mvnw -B clean verify` completo → JaCoCo + `jacoco:check`. Si un umbral no se cumple,
   worker de relleno dirigido a las líneas rojas restantes.
4. Reporte final: tabla antes/después por clase, nº de tests añadidos, tiempo de `mvn test`,
   confirmación de que no se introdujo `@SpringBootTest`.
