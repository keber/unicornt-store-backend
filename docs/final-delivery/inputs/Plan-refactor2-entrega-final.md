# Plan para agente — Unicornt Store Full-Stack

## Objetivo general

Transformar los artefactos actuales de Unicornt Store en una aplicación full-stack integrada, funcional end-to-end y alineada con los requisitos acumulados de los hitos 1–4, **sin reescritura big-bang** y preservando todo comportamiento existente que siga siendo útil.

El agente debe trabajar bajo estas premisas:

* `unicornt-store-frontend` es la base del frontend y ya satisface sustancialmente Hito 2.
* `unicornt-store-backend` es la base del backend y ya satisface sustancialmente Hito 4.
* `otf-sisacad-back` sirve como **referencia arquitectónica comprobada** para Hitos 1 y 3.
* La API actual del backend **no es un contrato inmutable**: debe considerarse una propuesta inicial y puede modificarse para lograr una integración coherente.
* El producto final debe conservar los atributos evaluables de H1, H2, H3 y H4.
* No se debe introducir complejidad que no aporte al dominio, a la integración o a la rúbrica.
* Cada etapa debe terminar con evidencia ejecutable antes de continuar.

---

# Capa 1 — Plan estratégico

## Etapa 0 — Baseline y definición de alcance

Establecer el estado inicial de ambos repositorios, comprobar que compilan/pasan sus tests actuales y congelar las funcionalidades que deben sobrevivir a la integración.

Resultado esperado:

```text
Frontend baseline conocido
Backend baseline conocido
Historias de usuario cerradas
Alcance explícitamente limitado
```

No modificar arquitectura todavía.

---

## Etapa 1 — Modelo funcional y contrato común

Diseñar primero **cómo debe comportarse el sistema integrado**, resolviendo vocabulario, ownership de datos, operaciones y contrato HTTP.

El resultado debe ser un contrato objetivo consensuado entre frontend y backend, no una simple adaptación unilateral de uno al otro.

Decisiones ya tomadas:

```text
Catalog / Stock     → backend
Anonymous Cart      → frontend/localStorage
Authenticated Cart  → backend
Login               → merge cart local → server
Checkout            → crea Order
Stock               → se descuenta al confirmar Order
Payment             → simulado
Product Image       → URL
Authentication      → JWT existente
API contract        → OpenAPI
```

---

## Etapa 2 — Catalog como primer vertical slice

Usar Catalog/Product como **slice piloto** para introducir Clean Architecture en el backend y realizar la primera conexión real frontend-backend.

El slice debe incorporar simultáneamente:

```text
H1 Domain
+
H3 Application/Ports/Adapters
+
H4 REST/JPA/OpenAPI
+
H2 consumo frontend
```

Este slice será el patrón para los siguientes.

---

## Etapa 3 — Identity y autenticación

Conectar registro/login/JWT al frontend con la menor intervención posible.

No rediseñar seguridad salvo que exista una incompatibilidad real.

El objetivo es habilitar las operaciones autenticadas necesarias para Cart, Ordering y Admin.

---

## Etapa 4 — Cart

Implementar el comportamiento completo de carrito:

```text
Anonymous → localStorage
Login → merge
Authenticated → server cart
```

El backend debe adoptar la arquitectura H1/H3 ya validada con Catalog.

---

## Etapa 5 — Ordering / Checkout

Implementar el caso de uso principal del sistema:

```text
PlaceOrder
```

que valide stock, genere la orden, persista snapshot de productos/precios, descuente inventario y vacíe el carrito de forma transaccional.

El frontend debe mostrar correctamente:

```text
submitting
success
error
```

---

## Etapa 6 — Administración de catálogo

Completar la funcionalidad administrativa requerida:

```text
Create Product
Update Product
Delete Product
```

incluyendo:

```text
name
description
price
category
stock
imageUrl
```

y protección mediante rol `ADMIN`.

---

## Etapa 7 — Hardening transversal

Una vez completados los slices funcionales:

* cerrar brechas arquitectónicas;
* aumentar cobertura;
* asegurar reglas ArchUnit;
* validar contrato OpenAPI;
* verificar CORS;
* revisar secretos;
* validar perfiles;
* ejecutar E2E real.

No introducir funcionalidades nuevas aquí.

---

## Etapa 8 — Validación de entrega

Ejecutar la pauta completa sobre el sistema final y corregir únicamente incumplimientos demostrables.

Debe quedar reproducible desde cero según README.

La guía pone explícitamente como verificaciones finales: tests, dominio sin frameworks, PostgreSQL real, CORS, TypeScript sin `any`, ciclo completo frontend→DB→UI, secretos fuera del repositorio y Swagger aislado en `dev`. 

---

# Capa 2 — Plan táctico y técnico

# Etapa 0 — Baseline

## Objetivo

Disponer de una referencia objetiva antes de modificar ambos proyectos.

## Backend

Ejecutar y registrar:

```bash
./mvnw clean test
./mvnw verify
docker compose config
```

Cuando sea posible:

```bash
docker compose up -d
```

Verificar:

* tests;
* cobertura;
* configuración PostgreSQL;
* Swagger dev;
* Swagger prod;
* endpoints actuales;
* migraciones;
* roles/auth;
* CI.

Crear una tabla:

| Área             | Estado actual | Debe conservarse |
| ---------------- | ------------- | ---------------- |
| REST             | funcional     | sí               |
| JWT              | funcional     | sí               |
| OpenAPI          | funcional     | sí               |
| PostgreSQL       | funcional     | sí               |
| Domain isolation | no            | corregir         |
| Repository ports | no            | corregir         |

## Frontend

Ejecutar:

```bash
npm ci
npm test
npm run lint
npm run build
```

Identificar:

* mocks actuales;
* localStorage cart;
* checkout simulado;
* modelos;
* DTOs;
* API layer;
* páginas;
* estados UI.

## Gate

No avanzar hasta poder distinguir claramente:

```text
funcionalidad existente
vs
funcionalidad que debe modificarse
```

---

# Etapa 1 — Modelo y contrato

## 1.1 Definir vocabulario final

Resolver explícitamente estos términos:

```text
Product
Category
ProductType/Subcategory
Cart
CartItem
Order
OrderItem
ShippingAddress
User
Role
Stock
Price/Money
Quantity
```

Evitar vocabulario distinto entre frontend y backend para el mismo concepto.

Especial atención a:

```text
ProductType ↔ ProductSubcategory
Checkout ↔ Order
Address ↔ ShippingAddress
qty ↔ quantity
```

---

## 1.2 Definir ownership

Fijar:

| Estado              | Fuente de verdad                     |
| ------------------- | ------------------------------------ |
| catálogo            | backend                              |
| precio              | backend                              |
| stock               | backend                              |
| carrito anónimo     | localStorage                         |
| carrito autenticado | backend                              |
| órdenes             | backend                              |
| usuarios            | backend                              |
| JWT                 | backend emitido / frontend consumido |

---

## 1.3 Definir API objetivo

Contrato mínimo recomendado:

### Catalog

```http
GET /api/v1/products
GET /api/v1/products/{id}
GET /api/v1/categories
```

### Auth

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### Cart

```http
GET    /api/v1/cart
POST   /api/v1/cart/items
PUT    /api/v1/cart/items/{productId}
DELETE /api/v1/cart/items/{productId}
POST   /api/v1/cart/merge
```

### Orders

```http
POST /api/v1/orders
GET  /api/v1/orders/{id}
```

Agregar listado de órdenes sólo si realmente se utiliza.

### Admin Product

```http
POST   /api/v1/products
PUT    /api/v1/products/{id}
DELETE /api/v1/products/{id}
```

---

## 1.4 Definir errores

Mantener respuesta común:

```json
{
  "message": "...",
  "errorCode": "...",
  "timestamp": "..."
}
```

Como mínimo distinguir:

```text
VALIDATION_ERROR
RESOURCE_NOT_FOUND
OUT_OF_STOCK
CONFLICT
UNAUTHORIZED
FORBIDDEN
INTERNAL_ERROR
```

---

## 1.5 Formalizar OpenAPI

Actualizar OpenAPI antes de integrar masivamente.

Tratarlo como:

```text
Backend DTO
    ↓
OpenAPI
    ↓
Frontend transport types
```

Preferencia: generar tipos TypeScript o, como mínimo, comprobar manualmente equivalencia.

## Gate

No avanzar si frontend y backend aún tienen interpretaciones incompatibles de Product, Cart u Order.

---

# Etapa 2 — Catalog vertical slice

Éste debe establecer el patrón técnico definitivo.

## 2.1 Backend — Domain

Crear algo equivalente a:

```text
domain/
  catalog/
    model/
      Product.java
      Category.java
    valueobject/
      Money.java
      Quantity.java
    repository/
      ProductRepository.java
      CategoryRepository.java
```

El dominio:

* no importa Spring;
* no importa JPA;
* no importa DTOs;
* no importa infraestructura.

La guía requiere explícitamente un dominio Java puro y repositorios como interfaces internas. 

### Product

Debe proteger reglas relevantes, por ejemplo:

```text
price >= 0
stock >= 0
name no vacío
```

No crear Value Objects innecesarios.

Mantener como candidatos claros:

```text
Money
Quantity
```

---

## 2.2 Application

Crear casos de uso concretos:

```text
ListProductsUseCase
GetProductUseCase
CreateProductUseCase
UpdateProductUseCase
DeleteProductUseCase
```

Pueden implementarse mediante un application service si eso evita proliferación innecesaria de clases.

Regla:

```text
application
    → domain
    → repository interfaces

application
   -X→ Spring Data
   -X→ JpaEntity
   -X→ web
```

---

## 2.3 Infrastructure persistence

Separar:

```text
ProductJpaEntity
ProductPersistenceMapper
SpringDataProductRepository
JpaProductRepositoryAdapter
```

Flujo:

```text
Product
  ↕ mapper
ProductJpaEntity
```

El adapter implementa:

```text
ProductRepository
```

La guía exige exactamente esta separación entre modelo de dominio, entidad JPA y adapter. 

---

## 2.4 Infrastructure web

Mantener:

```text
ProductRestController
ProductRequest
ProductResponse
ProductRestMapper
```

El controller sólo coordina HTTP:

```text
HTTP request
→ DTO
→ Use Case
→ Domain result
→ DTO response
```

No introducir reglas de negocio.

---

## 2.5 Frontend

Sustituir la fuente mock por:

```text
HttpProductApi
```

Mantener:

```text
unknown
→ runtime validation
→ ProductDto
→ ProductModel
```

Ajustar `ProductDto` al OpenAPI acordado.

---

## 2.6 Tests

Backend:

```text
ProductTest
MoneyTest
QuantityTest
Product use-case tests
ProductPersistenceMapperTest
JpaProductRepositoryAdapterTest
ProductRestControllerTest
```

Agregar `ArchitectureTest` desde esta etapa.

Reglas mínimas:

```text
domain -X→ Spring
domain -X→ JPA
domain -X→ infrastructure
application -X→ infrastructure
web -X→ persistence directo
```

Frontend:

* payload válido;
* payload inválido;
* error HTTP;
* loading;
* render;
* retry.

## Gate Catalog

Debe existir demostración real:

```text
PostgreSQL
   ↓
backend
   ↓
GET /products
   ↓
frontend
   ↓
catálogo visible
```

Sin mocks como fuente primaria.

---

# Etapa 3 — Identity

## Objetivo

Habilitar autenticación suficiente para historias posteriores.

## Backend

Conservar JWT actual salvo necesidad real de cambio.

Exponer:

```text
register
login
```

Mantener:

```text
CLIENT
ADMIN
```

No introducir refresh tokens, OAuth ni features adicionales salvo requisito.

## Frontend

Crear una frontera única para autenticación:

```text
auth.api
auth.service
token storage
```

Centralizar llamadas autenticadas mediante algo equivalente a:

```typescript
apiFetch()
```

Responsabilidades:

* base URL;
* `Authorization`;
* JSON;
* errores HTTP;
* 401.

Evitar replicar header JWT en cada archivo.

## Gate

```text
login válido
→ token
→ endpoint protegido funciona

login inválido
→ error visible
```

---

# Etapa 4 — Cart

## 4.1 Anonymous cart

Conservar implementación `localStorage` actual.

No llamar backend mientras no exista usuario autenticado.

---

## 4.2 Domain backend

Modelar:

```text
Cart
CartItem
Quantity
```

Reglas:

```text
quantity > 0
same product → increment
0 → remove
```

El carrito no descuenta stock.

---

## 4.3 Repository port

Crear:

```java
interface CartRepository
```

sin Spring Data.

Infrastructure implementa mediante adapter.

---

## 4.4 Merge

Implementar:

```http
POST /api/v1/cart/merge
```

Request mínimo:

```json
{
  "items": [
    {
      "productId": 10,
      "quantity": 2
    }
  ]
}
```

Regla:

```text
same ProductId
→ server quantity + local quantity
```

No implementar lógica de conflictos más sofisticada.

---

## 4.5 Frontend transition

Flujo:

```text
anonymous cart
    ↓
login
    ↓
merge request
    ↓
backend cart response
    ↓
clear local cart
    ↓
server becomes source of truth
```

## Gate

Prueba obligatoria:

```text
1. añadir producto sin login
2. iniciar sesión
3. verificar producto en carrito autenticado
```

Esto cubre directamente la historia de usuario crítica.

---

# Etapa 5 — Ordering

## 5.1 Domain

Crear:

```text
Order
OrderItem
OrderStatus
ShippingAddress
Money
Quantity
```

Una Order debe preservar snapshot:

```text
productId
productName
unitPrice
quantity
```

---

## 5.2 Application

Caso de uso:

```text
PlaceOrderUseCase
```

Responsabilidades:

```text
load cart
validate non-empty
validate products
validate stock
calculate totals
create Order
persist Order
decrement stock
clear Cart
return confirmation
```

No colocar estas reglas en controller.

---

## 5.3 Transaction boundary

La implementación Spring de la operación debe ser transaccional.

Garantía:

```text
ALL
or
NOTHING
```

Nunca:

```text
Order persisted
pero stock no actualizado
```

o:

```text
stock reducido
pero cart no procesado
```

---

## 5.4 Payment

No implementar gateway real.

Resultado:

```text
OrderStatus.CONFIRMED
```

Pago se considera exitoso dentro del flujo académico.

---

## 5.5 Frontend

Reutilizar infraestructura H2 existente:

```text
submitting
success
error
```

Al éxito:

```text
show confirmation
clear visible cart state
show order id / confirmation
```

Al error de stock:

```text
mostrar mensaje accionable
preservar cart
```

## Gate E2E principal

```text
Browser
→ cart
→ login
→ checkout
→ POST /orders
→ PostgreSQL
→ stock decremented
→ cart empty
→ UI CONFIRMED
```

Éste debe convertirse en el principal smoke test de la aplicación.

---

# Etapa 6 — Admin

## Backend

Endpoints protegidos:

```text
POST Product
PUT Product
DELETE Product
```

Sólo `ADMIN`.

Product contiene:

```text
name
description
price
category
stock
imageUrl
```

No implementar almacenamiento de archivos.

---

## Frontend

UI mínima:

```text
product list
product form
delete action
```

No construir dashboard complejo.

## Gate

```text
Admin creates Product
      ↓
PostgreSQL
      ↓
Customer catalog
      ↓
Product visible
```

---

# Etapa 7 — Hardening

## Arquitectura

Ejecutar ArchUnit y fallar ante:

```text
Domain → Spring
Domain → JPA
Domain → Infrastructure

Application → Infrastructure
Application → Web

Controller → Persistence
```

---

## Cobertura

Priorizar lógica crítica:

```text
Cart
PlaceOrder
Stock
Product mutations
Auth-critical logic
```

No perseguir 100% global sólo por estética.

La rúbrica sí exige cobertura lógica completa de métodos críticos para máximo puntaje, incluyendo ramas relevantes. 

---

## Backend quality gate

```bash
./mvnw clean test
./mvnw verify
docker compose config
```

---

## Frontend quality gate

```bash
npm test
npm run lint
npm run build
```

Debe permanecer:

```text
0 explicit any
0 unsafe non-null assertions
```

---

## CORS

Configurar globalmente:

```text
http://localhost:5173
```

La guía lo incluye explícitamente como verificación de integración. 

---

## Swagger

Validar:

```text
dev  → available
prod → unavailable
```

La guía exige aislamiento explícito por perfil. 

---

## Secrets

Verificar que no estén versionados:

```text
.env
DB_PASSWORD
JWT_SECRET
production credentials
```

El checklist final lo considera explícitamente. 

---

# Etapa 8 — Validación final y documentación

## Ejecutar pauta completa

Evaluar de nuevo:

```text
H1 / 10
H2 / 10
H3 / 10
H4 / 10
```

sobre **el estado final de la aplicación**, no sobre los proyectos históricos.

La rúbrica exige analizar las relaciones reales:

```text
Infrastructure → Application → Domain
Application → Repository Interface
Persistence Adapter → Repository Interface
JPA Entity ↔ Mapper ↔ Domain
HTTP → Controller → Application → Domain
```



---

## README

Backend y frontend deben explicar como mínimo:

* propósito;
* stack;
* arquitectura;
* requisitos;
* configuración;
* variables de entorno;
* levantar PostgreSQL;
* ejecutar backend;
* ejecutar frontend;
* ejecutar tests;
* Swagger;
* URL API;
* credenciales/demo si corresponden;
* flujo E2E.

La guía pide explícitamente documentación reproducible para ambos repositorios. 

---

# Reglas de ejecución para el agente

El agente debe seguir además estas reglas durante todo el trabajo:

1. **No hacer big-bang rewrite.**
2. Modificar un vertical slice por vez.
3. No romper deliberadamente un slice ya verificado para avanzar en otro.
4. Mantener tests existentes salvo que contradigan una decisión funcional explícitamente reemplazada.
5. No conservar APIs, clases o estructuras sólo porque ya existen.
6. No agregar patrones arquitectónicos sin una necesidad demostrable.
7. No introducir frameworks frontend adicionales.
8. No implementar pagos reales.
9. No implementar almacenamiento/upload de imágenes.
10. No implementar sincronización compleja/offline del carrito.
11. No convertir todos los primitives en Value Objects.
12. Mantener el dominio backend libre de Spring/JPA.
13. Evitar que los controllers conozcan persistencia.
14. Evitar que application conozca adapters concretos.
15. Toda modificación de contrato debe actualizar OpenAPI y frontend correspondiente.
16. Cada etapa debe dejar tests verdes.
17. No posponer tests arquitectónicos y unitarios para el final.
18. Ante discrepancia entre “arquitectura ideal” y alcance académico, privilegiar la **solución mínima que satisface íntegramente la pauta**.

# Definición de terminado

El agente sólo debe considerar la migración terminada cuando pueda demostrar, como mínimo:

```text
✓ Frontend TypeScript compila sin errores
✓ Frontend tests verdes
✓ Backend tests verdes
✓ Dominio Java sin Spring/JPA
✓ Application depende sólo de Domain/ports
✓ Persistence implementa ports
✓ PostgreSQL levanta con Docker Compose
✓ Swagger funciona sólo en dev
✓ CORS permite frontend Vite
✓ Catálogo proviene del backend real
✓ Carrito anónimo sobrevive al login
✓ Compra crea Order
✓ Compra descuenta stock
✓ Compra limpia carrito
✓ UI confirma la compra
✓ Admin puede crear/editar/eliminar productos
✓ Cambio administrativo persiste en PostgreSQL
✓ No existen secretos de producción versionados
✓ README permite reproducir el sistema desde cero
```

El **camino crítico** es, por tanto:

```text
Contrato
   ↓
Catalog
   ↓
Identity
   ↓
Cart
   ↓
Ordering
   ↓
Admin
   ↓
Hardening
   ↓
Evaluación final
```

y en cada slice:

```text
Diseño
→ Domain
→ Application
→ Ports
→ Adapters
→ REST
→ Frontend
→ Tests
→ Gate E2E
```

Ese sería el plan que le entregaría al agente como hoja de ruta principal: suficientemente específico para evitar decisiones improvisadas, pero sin convertir cada etapa en una receta rígida que impida adaptar el diseño cuando el código real revele una restricción legítima.
