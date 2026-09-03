## Diagnóstico General unicornt-store-backend Hitos 1 al 4

Aplicando estrictamente la pauta completa a la rama main de unicornt-store-backend:

| Hito       | Criterio                      |   Máx. |     Puntaje |
| ---------- | ----------------------------- | -----: | ----------: |
| H1         | Arquitectura de dominio       |      3 |       **1** |
| H1         | JUnit 5 + Mockito             |      3 |       **2** |
| H1         | Cobertura de métodos críticos |      4 |       **1** |
| **Hito 1** |                               | **10** |  **4 / 10** |
| H2         | Modelado TypeScript           |      3 |       **0** |
| H2         | DOM y formularios             |      3 |       **0** |
| H2         | Asincronía                    |      4 |       **0** |
| **Hito 2** |                               | **10** |  **0 / 10** |
| H3         | Separación en capas           |      3 |       **1** |
| H3         | Patrones tácticos DDD         |      3 |       **1** |
| H3         | Repository Pattern            |      4 |       **1** |
| **Hito 3** |                               | **10** |  **3 / 10** |
| H4         | API REST y errores            |      3 |       **3** |
| H4         | JPA/PostgreSQL/Docker         |      3 |       **2** |
| H4         | OpenAPI y perfiles            |      4 |       **4** |
| **Hito 4** |                               | **10** |  **9 / 10** |
| **TOTAL**  |                               | **40** | **16 / 40** |

El cambio respecto de mi evaluación errónea anterior es enorme en H4: **el repositorio actual prácticamente cumple ese hito completo**.

La nota global queda artificialmente deprimida por dos razones muy concretas: H2 no existe en este backend y H3 todavía no implementa realmente Clean Architecture, a pesar de que los nombres de paquetes sugieren `domain/infrastructure`.

---

# Hito 1 — Dominio puro, pruebas y cobertura

## H1.1 — Arquitectura de entidades de dominio: **1/3**

La pauta exige que el núcleo de dominio sea Java puro, independiente de Spring, JPA e infraestructura. 

El repositorio tiene:

```text
com.unicornt.store.domain
├── exception
└── service
```

pero el contenido de `domain` no es independiente.

Por ejemplo:

```java
// domain/service/ProductService.java

import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductTypeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Y su implementación:

```java
// domain/service/ProductServiceImpl.java

import com.unicornt.store.infrastructure.persistence.repository.ProductRepository;
import com.unicornt.store.infrastructure.persistence.repository.CategoryRepository;
import com.unicornt.store.infrastructure.persistence.repository.ProductTypeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

El patrón se repite en `AddressService`, `CartService`, `CheckoutService` y `UserService`.

De hecho, la dirección de dependencia actual es explícitamente:

```text
domain → infrastructure.persistence.entity
domain → infrastructure.persistence.repository
domain → Spring
domain → Spring Data
```

que es justamente lo que la pauta prohíbe.

Además, las verdaderas entidades están ubicadas en:

```text
infrastructure/persistence/entity/
```

como:

* `ProductEntity`
* `CategoryEntity`
* `CartItemEntity`
* `OrderEntity`
* `UserEntity`
* `AddressEntity`

No existe paralelamente un:

```text
domain/model/Product
domain/model/Order
domain/model/User
```

independiente de JPA.

Sí hay lógica de negocio real en los services —validación de productos, categorías, carrito, checkout, registro—, por lo que no corresponde 0.

Pero la separación es insuficiente para 2 o 3.

**Puntaje: 1/3.**

---

# H1.2 — JUnit 5 + Mockito: **2/3**

Aquí el proyecto está bastante mejor.

`pom.xml` contiene explícitamente:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

Encontré **10 clases de test** y los reportes Surefire incluidos registran **53 tests ejecutados, 0 failures, 0 errors y 0 skipped**.

Entre ellos:

```text
CategoryServiceImplTest                   4
ProductServiceImplSearchTest             3
UserServiceTest                          7
ProductSearchQueryTest                   3
JwtServiceTest                           4
SecurityChainTest                       11
CartRestControllerTest                   5
CategoryRestControllerTest               4
OrderRestControllerTest                  5
ProductRestControllerTest                7
                                        ──
                                        53
```

Hay además buenos ejemplos de aislamiento:

```java
@Mock
private UserRepository userRepository;

@Mock
private RoleRepository roleRepository;

@Mock
private PasswordEncoder passwordEncoder;

@InjectMocks
private UserServiceImpl userService;
```

y excepciones verificadas correctamente:

```java
assertThrows(DuplicateResourceException.class, ...);
assertThrows(ResourceNotFoundException.class, ...);
assertThrows(IllegalArgumentException.class, ...);
```

`ProductServiceImplSearchTest` incluso construye explícitamente el SUT mediante constructor:

```java
productService = new ProductServiceImpl(
    productRepository,
    categoryRepository,
    productTypeRepository
);
```

Los controllers tienen web-slice tests y seguridad tiene una suite bastante sólida.

### ¿Por qué no 3?

Porque la pauta exige para 3 puntos una suite que cubra sistemáticamente los principales comportamientos y aísle las dependencias relevantes. 

Hay services críticos prácticamente sin unit tests:

```text
AddressServiceImpl
CartServiceImpl
CheckoutServiceImpl
```

y la cobertura confirma que actualmente esos tres tienen **0% line coverage**.

Por tanto, la suite es claramente competente, pero no puede considerarse completa.

**Puntaje: 2/3.**

---

# H1.3 — Cobertura: **1/4**

Aquí ya no tenemos que inferir nada: el ZIP contiene el reporte JaCoCo real.

`pom.xml` configura:

```xml
<minimum>0.80</minimum>   <!-- instruction -->
...
<minimum>0.70</minimum>   <!-- branch -->
```

Pero el `jacoco.xml` incluido muestra:

| Métrica      | Cubierto | Total | Cobertura |
| ------------ | -------: | ----: | --------: |
| Instructions |     1310 |  2558 | **51.2%** |
| Lines        |      283 |   528 | **53.6%** |
| Branches     |       37 |   118 | **31.4%** |
| Methods      |       85 |   161 | **52.8%** |

Por paquete:

| Paquete                     |     Lines |  Branches |
| --------------------------- | --------: | --------: |
| `domain.service`            | **28.2%** | **25.6%** |
| `domain.exception`          |     80.0% |         — |
| `infrastructure.security`   |     85.5% |     62.5% |
| `infrastructure.web.error`  |     66.7% |        0% |
| `infrastructure.web.mapper` |     80.0% |     50.0% |
| `infrastructure.web.rest`   |     77.3% |     50.0% |

Los resultados de algunos componentes críticos son especialmente reveladores:

```text
AddressServiceImpl       0 / 27 lines      0%
CartServiceImpl          0 / 56 lines      0%
CheckoutServiceImpl      0 / 39 lines      0%
ProductServiceImpl      23 / 85 lines     27.1%
CategoryServiceImpl     22 / 25 lines     88%
UserServiceImpl         27 / 28 lines     96.4%
```

Esto entra exactamente en el nivel de **menos de 80% con múltiples caminos críticos sin pruebas**. 

Un detalle adicional: el `pom.xml` tiene un `jacoco:check` de 80% instructions / 70% branch vinculado a `verify`. Con el reporte actual, un `mvn verify` debería fallar por cobertura.

Intenté ejecutar el wrapper desde el entorno de análisis, pero Maven Wrapper necesita descargar Maven desde Maven Central y este sandbox no tiene salida de red. Por eso no pude regenerar el resultado independientemente; estoy usando los reportes JaCoCo/Surefire que venían dentro de tu ZIP.

**Puntaje: 1/4.**

### Hito 1: **4/10**

---

# Hito 2 — TypeScript, DOM y asincronía

## H2.1 — TypeScript: **0/3**

## H2.2 — DOM/formularios: **0/3**

## H2.3 — Asincronía: **0/4**

No existe frontend TypeScript en este repositorio.

La pauta exige específicamente `.ts`, modelado mediante `interface/type/enum`, DOM seguro y `async/await`. 

Por tanto, **si aplicamos literalmente la pauta completa únicamente a este repositorio**, corresponde:

**0/10.**

Pero haría una anotación importante en un informe formal:

> Hito 2 parece corresponder a otro artefacto/frontend del proyecto y no al microservicio backend evaluado.

Si existe `unicornt-store-frontend` y H2 debe evaluarse sobre él, **no sumaría este 0 al resultado definitivo del proyecto completo**. Evaluaría H2 contra ese repositorio y consolidaría ambos.

Por ahora, como pediste aplicar la pauta completa a `unicornt-store-backend`, la puntuación estricta es 0.

---

# Hito 3 — Clean Architecture y patrones tácticos

Aquí está la principal deuda técnica actual.

## H3.1 — Separación en capas: **1/3**

La estructura visual parece prometedora:

```text
domain/
infrastructure/
```

pero la pauta exige mirar dependencias reales y no nombres. 

Y las dependencias reales están invertidas.

Actualmente:

```text
Web Controller
      ↓
domain.service
      ↓
infrastructure.persistence.repository
      ↓
infrastructure.persistence.entity
```

En otras palabras:

```text
Domain → Infrastructure
```

Ejemplo directo:

```java
package com.unicornt.store.domain.service;

import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import com.unicornt.store.infrastructure.persistence.repository.ProductRepository;
import org.springframework.stereotype.Service;
```

También las interfaces del dominio filtran Spring Data:

```java
Page<ProductEntity> search(..., Pageable pageable);
```

Esto introduce:

```text
domain → org.springframework.data.domain.Page
domain → org.springframework.data.domain.Pageable
```

La arquitectura esperada sería:

```text
Infrastructure
      ↓
Application
      ↓
Domain
```



y hoy no existe siquiera una capa `application`.

Hay cierta organización de responsabilidades, por lo que 0 sería excesivo, pero arquitectónicamente sigue siendo un layered architecture acoplado.

**Puntaje: 1/3.**

---

# H3.2 — Patrones tácticos DDD: **1/3**

La pauta espera:

* entidades con identidad;
* Value Objects;
* invariantes;
* inmutabilidad;
* autovalidación;
* agregados cuando corresponda. 

Existen entidades con identidad:

```text
ProductEntity
OrderEntity
UserEntity
AddressEntity
CartItemEntity
...
```

y reglas de negocio como:

```java
if (product.getPrice() <= 0) {
    throw new IllegalArgumentException(...);
}
```

o:

```java
if (quantity <= 0) {
    throw new IllegalArgumentException(...);
}
```

Pero esas reglas están primordialmente en services, no protegidas por el modelo.

Conceptos de negocio siguen siendo primitivas:

```java
int productId
int categoryId
int productTypeId
int price
String email
String firstName
String lastName
```

No encontré Value Objects como:

```text
ProductId
Money
Email
Quantity
Address
OrderId
```

ni aggregates explícitos.

Los JPA entities son además mutables mediante setters.

Eso corresponde directamente al nivel 1 de la pauta: entidades reconocibles, pero con primitive obsession y reglas dispersas.

**Puntaje: 1/3.**

---

# H3.3 — Repository Pattern: **1/4**

Este punto es bastante inequívoco.

La pauta espera:

```text
Use Case
       ↓
Repository Port
       ↑
JPA Adapter
```



Lo actual es:

```text
ProductServiceImpl
       ↓
ProductRepository
```

pero ese `ProductRepository` está directamente en:

```text
infrastructure.persistence.repository
```

y es el repository tecnológico.

Por tanto:

```text
domain.service
     ↓
Spring Data/JPA repository
```

No existe algo equivalente a:

```text
domain.repository.ProductRepository
```

o:

```text
application.port.out.ProductRepositoryPort
```

implementado después por:

```text
infrastructure.persistence.JpaProductRepositoryAdapter
```

La misma situación se repite con usuarios, categorías, carrito, direcciones, órdenes, etc.

El constructor injection sí es correcto:

```java
public ProductServiceImpl(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        ProductTypeRepository productTypeRepository) {
    ...
}
```

pero inyectar una dependencia concreta de infraestructura mediante constructor **no convierte esa dependencia en un port arquitectónico**.

**Puntaje: 1/4.**

### Hito 3: **3/10**

---

# Hito 4 — REST, PostgreSQL y OpenAPI

Aquí el refactor sí es claramente exitoso.

# H4.1 — API REST + errores: **3/3**

El backend actual implementa REST propiamente tal.

Los controllers están bajo:

```text
infrastructure/web/rest/
```

con recursos como:

* `ProductRestController`
* `CategoryRestController`
* `CartRestController`
* `OrderRestController`
* `AddressRestController`
* `AuthRestController`

Los endpoints siguen rutas de recursos del tipo:

```text
/api/v1/products
/api/v1/categories
/api/v1/cart
/api/v1/orders
/api/v1/addresses
/api/v1/auth
```

`ProductRestController`, por ejemplo, utiliza correctamente:

```java
@GetMapping
@GetMapping("/{id}")
@PostMapping
@PutMapping("/{id}")
@DeleteMapping("/{id}")
```

y estados como:

```java
@ResponseStatus(HttpStatus.CREATED)
@ResponseStatus(HttpStatus.NO_CONTENT)
```

Hay DTOs separados:

```text
ProductDtos
CategoryDtos
CartDtos
OrderDtos
AddressDtos
AuthDtos
```

y mappers web explícitos.

El manejo de excepciones está centralizado mediante:

```java
@RestControllerAdvice
public class GlobalExceptionHandler
```

que transforma consistentemente:

| Excepción                    | HTTP |
| ---------------------------- | ---: |
| `ResourceNotFoundException`  |  404 |
| `OutOfStockException`        |  422 |
| `DuplicateResourceException` |  409 |
| validation                   |  400 |
| malformed body               |  400 |
| data integrity               |  409 |
| access denied                |  403 |
| authentication               |  401 |
| unknown endpoint             |  404 |
| generic exception            |  500 |

con un `ErrorResponse` JSON común.

Además hay handlers específicos para errores producidos antes de llegar al dispatcher:

```text
RestAuthEntryPoint
RestAccessDeniedHandler
```

Esto es justamente lo que la pauta exige para el nivel máximo. 

Los tests también ejercitan estas rutas y códigos HTTP.

**Puntaje: 3/3.**

---

# H4.2 — JPA + PostgreSQL + Docker: **2/3**

Hay bastante cumplimiento.

`docker-compose.yml` define explícitamente:

```yaml
db:
  image: postgres:16-alpine
```

con:

* database;
* user;
* password;
* volumen persistente;
* `pg_isready` healthcheck.

La aplicación depende de la base mediante:

```yaml
depends_on:
  db:
    condition: service_healthy
```

y recibe:

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${POSTGRES_DB}
```

También hay:

```text
V1__init.sql
V2__seed_reference_data.sql
```

y el proyecto utiliza Spring Data JPA/PostgreSQL.

Las entidades JPA están además correctamente ubicadas físicamente en:

```text
infrastructure/persistence/entity/
```

Esto ya es una mejora sustancial respecto del proyecto original.

### ¿Por qué no 3?

Porque el máximo exige **separación entre modelo de dominio y modelo de persistencia**, con mapper explícito entre ambos. 

Aquí sólo existe:

```text
ProductEntity
CategoryEntity
OrderEntity
...
```

y esos mismos objetos viajan hacia `domain.service`.

No existe:

```text
domain.Product
      ↕
ProductPersistenceMapper
      ↕
ProductEntity
```

Los mappers existentes son:

```text
infrastructure/web/mapper/
```

y realizan principalmente:

```text
JPA Entity ↔ REST DTO
```

no:

```text
JPA Entity ↔ Domain Entity
```

Por tanto PostgreSQL/JPA/Docker están bien implementados, pero falta la separación requerida por la rúbrica.

**Puntaje: 2/3.**

---

# H4.3 — OpenAPI + perfiles seguros: **4/4**

Este criterio sí parece implementado exactamente de acuerdo con la pauta.

`pom.xml` incluye:

```xml
org.springdoc:
springdoc-openapi-starter-webmvc-ui
```

Pero, más importante, no me estoy limitando a esa evidencia.

La configuración base dice:

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

Por tanto el sistema es **secure by default**.

`application-prod.yml` vuelve a imponer:

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

Mientras que sólo `application-dev.yml` activa:

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    try-it-out-enabled: true
```

Además existe configuración OpenAPI real:

```java
@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig
```

con:

* título;
* versión;
* descripción;
* contacto;
* esquema JWT.

Y los controllers están documentados mediante:

```java
@Operation
@ApiResponse
@ApiResponses
@Parameter
@SecurityRequirement
```

Por ejemplo se documentan 200/201/204/400/401/403/404, schemas de error y requisitos de autenticación.

Esto satisface la parte **estructural y documental** del máximo.

La única evidencia que no pude reproducir dentro del sandbox es arrancar ambas configuraciones y comprobar dinámicamente:

```text
dev  → /swagger-ui.html = disponible
prod → /swagger-ui.html = 404
```

pero la configuración es explícita y verificable y, bajo la pauta, considero suficiente la evidencia disponible para asignar el nivel 4. La propia documentación del refactor incluso define esa aceptación dev/prod.

**Puntaje: 4/4.**

### Hito 4: **9/10**

---

# Lectura global

La situación real del repositorio es bastante distinta de la que sugería mi análisis anterior.

El refactor consiguió claramente convertir:

```text
Spring MVC + Thymeleaf
```

en:

```text
REST API
+ JWT
+ DTOs
+ centralized JSON errors
+ JPA
+ PostgreSQL
+ Docker
+ OpenAPI
+ dev/prod isolation
+ CI
```

Y eso explica que **H4 pase de prácticamente incumplido a 9/10**.

La arquitectura actual, simplificada, es:

```text
                 ┌─────────────────┐
HTTP ───────────▶│ REST Controller │
                 └────────┬────────┘
                          │
                          ▼
                  domain.service
                          │
                          ▼
               Spring Data Repository
                          │
                          ▼
                    JPA Entity
                          │
                          ▼
                     PostgreSQL
```

La arquitectura requerida por H3 sería en cambio:

```text
                 Infrastructure / Web
                        │
                        ▼
                   Application
                        │
                        ▼
                      Domain
                        ▲
                        │
                 Repository Port
                        ▲
                        │
          Infrastructure / Persistence
                        │
                        ▼
                  JPA Entity
                        │
                        ▼
                    PostgreSQL
```

Ese es ahora el **gap principal**.

---

# Los cuatro problemas que más puntos cuestan

## 1. `domain` depende de `infrastructure`

Este es el problema estructural más evidente.

Por ejemplo:

```java
domain.service.ProductService
    → infrastructure.persistence.entity.ProductEntity

domain.service.ProductServiceImpl
    → infrastructure.persistence.repository.ProductRepository
```

Mover archivos no basta: hay que **invertir esas dependencias**.

---

## 2. No existe modelo de dominio independiente

Hoy:

```text
ProductEntity == persistencia + objeto usado por negocio
```

La rúbrica espera aproximadamente:

```text
domain/model/Product.java
infrastructure/persistence/entity/ProductEntity.java
infrastructure/persistence/mapper/ProductPersistenceMapper.java
```

---

## 3. Faltan ports de repositorio

Hoy:

```text
ProductServiceImpl
      ↓
Spring Data ProductRepository
```

Debería ser:

```text
ProductUseCase
      ↓
ProductRepositoryPort
      ↑
JpaProductRepositoryAdapter
      ↓
SpringDataProductRepository
```

Eso solo recuperaría una parte grande de H3 y permitiría obtener H4.2 completo.

---

## 4. Cobertura del core sigue baja

Este es el otro gran problema medible.

Lo urgente no son más controller tests: son estos:

```text
AddressServiceImpl        0%
CartServiceImpl           0%
CheckoutServiceImpl       0%
ProductServiceImpl       27%
```

Especialmente `CheckoutServiceImpl`: por su naturaleza contiene probablemente algunas de las reglas más críticas de todo el e-commerce, y hoy JaCoCo muestra **0 líneas cubiertas**.

---

# Potencial de mejora sin tocar H2

Con una segunda fase de refactor relativamente focalizada, el backend podría pasar aproximadamente de:

**16/40**

a:

| Criterio          | Actual | Alcanzable |
| ----------------- | -----: | ---------: |
| H1.1 Dominio      |      1 |          3 |
| H1.2 Tests        |      2 |          3 |
| H1.3 Coverage     |      1 |          4 |
| H3.1 Capas        |      1 |          3 |
| H3.2 DDD          |      1 |          3 |
| H3.3 Repositories |      1 |          4 |
| H4.1 REST         |      3 |          3 |
| H4.2 Persistencia |      2 |          3 |
| H4.3 OpenAPI      |      4 |          4 |

Eso llevaría el backend a **30/30 en los hitos backend**, manteniendo H2 separado.

Y ésa me parece la forma más justa de leer el estado actual:

> **Como backend REST:** está bastante avanzado y H4 está prácticamente terminado.
> **Como implementación de Clean Architecture/DDD:** el refactor todavía es mayormente nominal; `domain` continúa dependiendo directamente de infraestructura.
> **Como proyecto completo de la pauta:** falta evaluar el frontend TypeScript correspondiente a H2.

Por cierto, haber quitado `.git` fue una buena decisión para este análisis: **no vuelvas a subirlo por ahora**. Sólo te lo pediría si después quieres que compare el estado actual con PR #1 commit por commit o evalúe la calidad/historia del refactor.
