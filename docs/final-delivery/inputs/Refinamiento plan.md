# Refinamiento plan

Contrastando el plan con la guía del profesor, la dirección general está bien, pero haría **tres ajustes importantes de secuencia** y agregaría **cinco entregables/gates explícitos** que en mi plan anterior estaban implícitos o quedaron demasiado tarde.

La guía plantea la entrega final justamente como una **consolidación** de los aprendizajes anteriores en una arquitectura unificada: dominio Java puro, capa de aplicación, infraestructura Spring/JPA/web y frontend Vite + TypeScript conectados mediante un contrato común. 

## Veredicto rápido

| Aspecto del plan                            | Alineación                     | Acción                          |
| ------------------------------------------- | ------------------------------ | ------------------------------- |
| Diseñar primero comportamiento y contrato   | 🟢 Alta                        | Mantener                        |
| OpenAPI como frontera Front ↔ Back          | 🟢 Alta                        | Mantener y formalizar           |
| Backend como dueño de catálogo/stock        | 🟢 Alta                        | Mantener                        |
| PostgreSQL + Docker                         | 🟢 Alta                        | Mantener                        |
| Frontend TS estricto                        | 🟢 Alta                        | Mantener                        |
| Refactor H1/H3 por vertical slices          | 🟢 Alta                        | Mantener                        |
| Hacer **todo** el happy path antes de H1/H3 | 🟠 Riesgo                      | Cambiar                         |
| Carrito híbrido local/servidor              | 🟡 No contradice guía          | Mantener si aporta al requisito |
| JWT                                         | 🟡 Extra                       | Mantener, no expandir           |
| Pago/despacho simplificados                 | 🟢 Correcto                    | Mantener                        |
| CORS                                        | 🔴 Falta explícita en plan     | Agregar gate                    |
| E2E real Front → Back → PostgreSQL → Front  | 🔴 Debe ser gate explícito     | Agregar                         |
| Secrets / `.env`                            | 🔴 Falta explícita             | Agregar                         |
| README de ambos repos                       | 🔴 Falta explícita             | Agregar                         |
| Tests/cobertura durante refactor            | 🟠 Quedaron demasiado al final | Adelantar                       |

Mi conclusión es: **el diseño conceptual no necesita rehacerse; el plan de ejecución sí conviene ajustarlo.**

---

# 1. Contrato primero: totalmente alineado

Éste es probablemente el punto de mayor coincidencia.

La guía comienza su arquitectura final diciendo que, para evitar inconsistencias entre cliente y servidor, debe establecerse un **contrato estandarizado para toda la solución**. 

Eso respalda directamente nuestra decisión de **no considerar automáticamente la API H4 actual como definitiva**.

Por tanto mantenemos:

```text
Frontend actual ──┐
                  ├─> Modelo funcional común
Backend actual  ──┘
                        ↓
                  Contrato OpenAPI
```

Y después ambos lados se adaptan al contrato acordado.

### Decisión

**Sin cambios.**

Antes de una refactorización importante:

1. catálogo;
2. carrito;
3. checkout/order;
4. auth;
5. administración;

deben tener operaciones y payloads definidos.

---

# 2. La arquitectura backend objetivo coincide casi exactamente

La guía recomienda explícitamente:

```text
domain/
  entity/
  valueobject/
  exception/
  repository/

application/
  usecase/

infrastructure/
  web/
  persistence/
  config/
```

con dominio Java puro, repositorio como interfaz interna, adapter JPA e infraestructura exterior. 

Eso coincide prácticamente 1:1 con el patrón que observamos en `otf-sisacad`.

Nuestro objetivo:

```text
Infrastructure
      ↓
Application
      ↓
Domain
```

queda totalmente confirmado.

La guía incluso muestra el caso de uso dependiendo de una interfaz pura de repositorio y la implementación JPA fuera de dominio. 

Y posteriormente:

```java
BookingRepositoryAdapter
    implements BookingRepository
```

usando internamente una entidad JPA. 

### Decisión

**Adoptar como patrón oficial para Unicornt la arquitectura de Sisacad**, que además coincide con la recomendación del profesor.

No necesitamos inventar otra.

---

# 3. Cambio importante: H1/H3 no debe quedar para la etapa 6

Éste es el principal ajuste que haría a mi plan anterior.

Yo propuse:

```text
1 contrato/catalog
2 login
3 cart
4 ordering
5 admin
6 refactor H1/H3
7 hardening
```

Eso tiene un problema: podríamos construir toda la integración utilizando los actuales:

```text
domain.service
    ↓
Spring Data Repository
    ↓
JpaEntity
```

y después tener que desarmarlo.

La guía espera que el backend final tenga desde el punto de vista estructural:

```text
Controller
    ↓
Application Use Case
    ↓
Domain
    ↓
Repository Port
    ↑
Persistence Adapter
```

No que Clean Architecture sea un embellecimiento posterior. 

### Cambio

Haremos **refactor + integración por slice**, no primero integración y después refactor.

Por ejemplo:

```text
CATALOG

1. definir contrato
       ↓
2. crear dominio limpio Product
       ↓
3. crear ProductRepository port
       ↓
4. crear application use cases
       ↓
5. adaptar JPA existente
       ↓
6. adaptar REST controller
       ↓
7. conectar frontend
       ↓
8. tests
```

Después pasamos a Cart.

Eso reduce retrabajo.

---

# 4. El plan de Value Objects está alineado

La guía recomienda explícitamente objetos como:

```text
Email
Seats
```

y muestra que deben ser inmutables y autovalidantes. 

Pero tampoco exige convertir cada `String` en una clase.

Por tanto mantendría nuestra decisión minimalista:

### VO claramente justificados

```text
Money
Quantity
Email
```

y evaluaría:

```text
ProductId
OrderId
UserId
```

si simplifican identidad/repositorios.

### No crear por defecto

```text
ProductName
ProductDescription
ImageUrl
CategoryName
Street
City
```

La guía recompensa DDD táctico, no ceremonial.

---

# 5. OpenAPI sigue siendo una muy buena decisión

La guía no sólo pide Swagger. Construye el frontend y backend alrededor de un modelo contractual común. 

Y el frontend de referencia tiene explícitamente DTOs distintos de las estructuras de dominio/UI. 

Así que mantendría:

```text
Backend Domain
      ↓
REST DTO
      ↓
OpenAPI
      ↓
Frontend DTO
      ↓
Frontend Model
```

Aquí incluso **generar los tipos de transporte TypeScript desde OpenAPI** sería perfectamente coherente con la intención de la guía.

No es obligatorio, pero reduce drift.

---

# 6. Gap encontrado: CORS debe aparecer explícitamente en el plan

La guía es muy explícita:

```text
Frontend : localhost:5173
Backend  : localhost:8080
```



y más adelante incluye CORS como parte concreta de infraestructura. Incluso el checklist exige comprobar que `http://localhost:5173` esté autorizado. 

En nuestro plan anterior prácticamente lo dimos por supuesto.

### Acción

Agregar desde la primera integración:

```text
CorsConfig
  allowedOrigins:
    http://localhost:5173
```

Preferiría configuración global en backend en vez de llenar controllers con:

```java
@CrossOrigin
```

aunque la guía lo muestre así.

Lo que importa es el comportamiento.

### Gate

Desde frontend Vite real:

```text
GET /api/v1/products
```

debe funcionar sin error CORS.

---

# 7. Gap encontrado: necesitamos un E2E real como criterio de aceptación

Ésta es probablemente la recomendación más importante de toda la guía para nuestro caso particular.

El checklist dice explícitamente:

> **Ciclo completo:** formulario web inserta en PostgreSQL y refresca la UI. 

Esto es justamente lo que **Unicornt todavía nunca ha demostrado**.

Frontend y backend funcionan por separado.

Por eso debemos convertir esto en un milestone, no dejarlo como consecuencia accidental.

Para Unicornt propondría dos pruebas de humo E2E.

### E2E mínimo de lectura

```text
PostgreSQL
    ↓
Backend
    ↓
GET /products
    ↓
Frontend
    ↓
catálogo visible
```

### E2E mínimo de escritura

El más representativo sería:

```text
Frontend
  ↓
Checkout
  ↓
POST /orders
  ↓
Backend
  ↓
PostgreSQL
  ↓
Order persistida
+
stock decrementado
  ↓
respuesta CONFIRMED
  ↓
UI confirma compra
```

Este segundo flujo demuestra casi todo el sistema.

---

# 8. Incluso agregaría un tercer E2E administrativo

Por tus historias:

```text
Admin
  ↓
Create Product
  ↓
PostgreSQL
  ↓
Catalog
  ↓
Frontend muestra nuevo producto
```

Eso es tremendamente demostrable.

En una presentación puedes literalmente:

1. crear una polera;
2. refrescar catálogo;
3. verla;
4. comprarla;
5. comprobar reducción de stock.

Con eso muestras en cinco minutos:

```text
Frontend
REST
Auth
Application
Domain
JPA
PostgreSQL
```

La guía busca precisamente una integración full-stack observable.

---

# 9. Gap: los tests no deben quedar para “hardening”

Mi plan anterior puso al final:

```text
ArchUnit
coverage
contract tests
E2E
```

Eso es demasiado tarde.

La guía pone las pruebas en el corazón del backend y muestra JUnit 5 + Mockito sobre application use cases con:

```text
Arrange
Act
Assert
assertThrows
verify
```



Y en la revisión final exige:

```text
./mvnw clean test
→ 100% passing
```



### Cambio

Cada vertical slice debe salir con sus tests.

Por ejemplo Catalog:

```text
ProductTest
MoneyTest
QuantityTest

CreateProductUseCaseTest
UpdateProductUseCaseTest
DeleteProductUseCaseTest
SearchProductsUseCaseTest

ProductPersistenceMapperTest
JpaProductRepositoryAdapterTest

ProductRestControllerTest
```

Después Cart, después Ordering.

No acumular deuda de pruebas para el final.

---

# 10. Y tampoco dejaría ArchUnit para el último día

En esto la pauta y la experiencia Sisacad son muy claras.

La arquitectura final debe respetar:

```text
Infrastructure → Application → Domain

Domain -X→ Spring
Domain -X→ JPA
Domain -X→ Infrastructure
```

La rúbrica evalúa explícitamente esas dependencias reales. 

Por tanto, cuando creemos el primer slice Clean:

```text
Catalog
```

también crearíamos inmediatamente:

```text
ArchitectureTest
```

Esto evita que Cart o Ordering vuelvan a introducir dependencias prohibidas.

---

# 11. Frontend: nuestro plan supera lo mínimo recomendado

Aquí estamos bien.

La guía pide:

```text
models/
services/
components/
main.ts
```

con modelos TypeScript y operaciones `fetch` asíncronas. 

El `unicornt-store-frontend` actual ya es más robusto que el ejemplo:

* `strict`;
* sin `any`;
* DTO → validation → model;
* DOM helpers;
* loading/success/error;
* tests.

El checklist final enfatiza específicamente:

```text
Cero any
npm run build sin errores
```



### Decisión

No tocar innecesariamente esa arquitectura.

Sólo introducir:

```text
real API adapters
auth client
contract types
```

y adaptar modelos cuando el contrato lo necesite.

---

# 12. Cuidado: la guía usa `innerHTML`, pero nosotros no debemos retroceder

El ejemplo del profesor usa cosas como:

```typescript
listContainer.innerHTML =
  bookings.map(generateBookingCardHtml).join("");
```

e incluso `catch(error: any)`. 

Esto está por debajo de la calidad actual de nuestro frontend y también contradice parcialmente la rúbrica detallada de H2.

No debemos imitarlo.

La guía funciona como orientación de integración, no como obligación de copiar literalmente cada ejemplo.

Mantendremos:

```text
no any
safe DOM
textContent
templates internos
typed error handling
```

La pauta además dice expresamente que no deben penalizarse mejoras adicionales mientras no contradigan el requisito. 

---

# 13. PostgreSQL/Docker: totalmente alineado

La guía exige PostgreSQL real levantado vía Docker Compose. 

Nuestro backend ya cumple gran parte de esto.

Lo único que cambiaremos será la capa inmediatamente superior:

Hoy:

```text
Spring Data
   ↓
JpaEntity
```

Después:

```text
Domain Repository
      ↑
JpaRepositoryAdapter
      ↓
SpringDataRepository
      ↓
JpaEntity
```

La base y Docker pueden permanecer prácticamente intactos.

---

# 14. Swagger dev-only: no tocar

La guía requiere:

```text
dev:
Swagger ON

prod:
Swagger OFF
```



y vuelve a verificarlo en el checklist. 

Eso ya está bien implementado en nuestro backend.

### Acción

**Preservarlo como regression requirement**, no rediseñarlo.

---

# 15. Gap: gestión de secretos

No lo incluimos en nuestro plan funcional, pero la entrega sí lo menciona expresamente.

La guía exige no subir:

```text
.env
.env.*
contraseñas producción
```

y mantener `.gitignore` tanto para backend como frontend.  

### Gate final

Backend:

```text
.env
application-prod secrets
target/
IDE files
```

Frontend:

```text
node_modules/
dist/
.env
.env.*
```

Y cualquier:

```text
JWT_SECRET
DB_PASSWORD
API credentials
```

debe venir por environment.

Esto es barato y no conviene regalar puntos aquí.

---

# 16. Gap: README de ambos repos

La guía dedica una parte específica a exigir README profesional para **frontend y backend**, incluyendo stack y puesta en marcha. 

Y muestra que debe permitir ejecutar:

```bash
docker compose up -d
./mvnw clean test
./mvnw spring-boot:run

npm install
npm run dev
```



### Añadir al plan

No dejar documentación como algo “si queda tiempo”.

Debe haber:

```text
backend/README.md
frontend/README.md
```

con:

* arquitectura;
* stack;
* requisitos;
* variables de entorno;
* startup;
* tests;
* endpoints;
* Swagger;
* integración frontend/backend.

---

# 17. Sobre nuestro carrito híbrido

La guía no especifica carrito, por lo que **no existe incompatibilidad**.

Nuestra decisión:

```text
anonymous
→ localStorage

login
→ merge

authenticated
→ server cart
```

es válida.

Pero pragmáticamente pondría una condición:

> No invertir más tiempo en el merge del necesario para demostrar la historia requerida.

Implementación mínima:

```text
POST /cart/merge
items: ProductId + Quantity
```

Backend:

```text
existing + incoming
```

y listo.

Nada de sincronización offline, timestamps, conflicto distribuido, etc.

---

# 18. Sobre `POST /orders` frente a `/checkout`

La guía tampoco impone esto.

Mantendría:

```http
POST /api/v1/orders
```

porque el recurso creado es una orden.

Eso es además REST semánticamente fuerte, coherente con H4.

Checkout sería el **caso de uso**, no necesariamente el nombre del endpoint:

```text
POST /orders
      ↓
PlaceOrderUseCase
```

---

# 19. Pago simulado y despacho simplificado: siguen siendo correctos

No encontré en la guía ninguna exigencia de gateway real de pagos ni sistema logístico.

Así que mantendría:

```text
Payment = simulated success

Order
PENDING/CONFIRMED
```

y si queremos representar despacho:

```text
DISPATCHED
```

pero sin construir un subsistema que no entrega puntos.

---

# Plan corregido

Con la guía sobre la mesa, mi plan ya no sería el anterior de siete etapas.

Sería éste:

### Fase 0 — Contrato y modelo común

Cerrar:

```text
Product
Category
Cart
Order
User
```

y decidir:

* ownership;
* DTOs;
* endpoints;
* errores;
* auth;
* OpenAPI.

**No programar todavía grandes cambios.**

---

### Fase 1 — Catalog vertical slice

Backend:

```text
pure Product domain
Value Objects mínimos
repository port
application use cases
JPA adapter
persistence mapper
REST DTO/mapper
```

Frontend:

```text
GET real /products
GET real /categories
```

Quality gates:

```text
JUnit/Mockito
ArchUnit
TS build
CORS
```

Resultado observable:

**catálogo servido desde PostgreSQL y renderizado por frontend.**

---

### Fase 2 — Identity

Conectar lo que ya existe:

```text
register
login
JWT
```

Sin rediseñar seguridad más de lo necesario.

---

### Fase 3 — Cart vertical slice

```text
anonymous local cart
        ↓ login
merge
        ↓
authenticated server cart
```

Backend ya bajo H1/H3.

Tests incluidos en la misma fase.

---

### Fase 4 — Ordering vertical slice

Caso de uso central:

```text
PlaceOrder
```

transaccional:

```text
validate stock
→ snapshot prices
→ create order
→ decrement stock
→ clear cart
→ CONFIRMED
```

Frontend reutiliza sus estados:

```text
submitting
success
error
```

---

### Fase 5 — Admin Product

```text
create
update
delete
imageUrl
stock
```

E2E:

```text
Admin UI
→ REST
→ PostgreSQL
→ Catalog UI
```

---

### Fase 6 — Validación integral

No desarrollo nuevo salvo correcciones.

Ejecutar:

```text
Backend tests
JaCoCo
ArchUnit
Frontend tests
npm lint
npm build
Docker Compose
Swagger dev/prod
CORS
secret scan/check
```

Y dos E2E obligatorios:

```text
catalog DB → browser

checkout browser → DB → confirmation
```

---

### Fase 7 — Entrega

README de ambos repos.

Instrucciones reproducibles.

OpenAPI final.

Reevaluación completa de los 40 puntos.

---

# Lo que cambia respecto del plan anterior

En términos muy concretos:

| Antes                         | Ahora                                                   |
| ----------------------------- | ------------------------------------------------------- |
| Integrar primero              | **Contrato primero**                                    |
| Refactor H1/H3 en etapa 6     | **Refactor por slice antes de integrar cada capacidad** |
| Tests al final                | **Tests en cada slice**                                 |
| ArchUnit al final             | **Desde el primer slice**                               |
| CORS implícito                | **Gate explícito**                                      |
| E2E implícito                 | **Milestone explícito**                                 |
| Secrets omitidos              | **Gate explícito**                                      |
| README omitido                | **Entregable obligatorio**                              |
| API H4 actual casi preservada | **API actual sólo como materia prima**                  |

## Conclusión

**No veo un gap conceptual grave entre nuestro plan y la guía.** De hecho, las decisiones fundamentales —contrato explícito, frontend Vite/TS, backend REST, dominio puro, application layer, repository ports/adapters, JPA separada, PostgreSQL/Docker y OpenAPI— están muy alineadas con la arquitectura que el profesor propone como referencia. 

El principal error potencial de nuestro plan anterior era **de secuencia**: dejar H1/H3 para después de integrar todo. La guía refuerza que esas propiedades deben formar parte del backend consolidado. Por eso conviene aplicar la arquitectura Sisacad **slice por slice mientras integramos**, no como limpieza final.

Y hay cuatro cosas operativas que no deberíamos perder de vista porque el profesor las convierte explícitamente en checklist: **CORS, ciclo E2E real contra PostgreSQL, exclusión de secretos y README reproducible**. 

Con ese ajuste, considero el plan **bien alineado con la entrega y suficientemente acotado para ejecutarlo sin convertir Unicornt Store en un proyecto de seis meses**.
