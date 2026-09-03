La evaluación correcta no es “H2 aislado”, sino revisar también **qué partes de H1 y H3 son aplicables al frontend como componente de la aplicación end-to-end**, sin forzar requisitos que son específicamente Java/JPA.

La distinción importante es ésta: la pauta original formula algunos criterios en términos tecnológicos concretos —Java puro, JUnit 5/Mockito, Repository Pattern orientado al backend—, pero detrás de ellos hay propiedades arquitectónicas que **sí pueden evaluarse en el frontend**: pureza del dominio, aislamiento de dependencias, cobertura de lógica crítica, separación entre dominio/aplicación/infraestructura y uso de abstracciones.

La pauta exige precisamente evaluar comportamiento y relaciones reales, no nombres de carpetas. 

## Resultado ampliado del frontend (https://github.com/keber/unicornt-store-frontend/)

| Hito   | Criterio                      |   Máx. | Evaluación frontend                             |             Puntaje |
| ------ | ----------------------------- | -----: | ----------------------------------------------- | ------------------: |
| H1     | Arquitectura de dominio       |      3 | **Aplicable por equivalencia arquitectónica**   |             **2/3** |
| H1     | Suite JUnit 5 + Mockito       |      3 | Tecnología no aplicable; objetivo de testing sí | **3/3 equivalente** |
| H1     | Cobertura de métodos críticos |      4 | **Aplicable**                                   | **0/4 verificable** |
| **H1** |                               | **10** | evaluación adaptada                             |            **5/10** |
| H2     | Tipado TypeScript             |      3 | Aplicable directamente                          |             **3/3** |
| H2     | DOM/formularios               |      3 | Aplicable directamente                          |             **3/3** |
| H2     | Asincronía                    |      4 | Aplicable directamente                          |             **4/4** |
| **H2** |                               | **10** |                                                 |           **10/10** |
| H3     | Separación en capas           |      3 | **Aplicable arquitectónicamente**               |             **2/3** |
| H3     | Patrones tácticos             |      3 | **Parcialmente aplicable**                      |             **2/3** |
| H3     | Repository Pattern            |      4 | Aplicable como ports/adapters                   |             **1/4** |
| **H3** |                               | **10** | evaluación adaptada                             |            **5/10** |
| H4     | Backend REST/JPA/OpenAPI      |     10 | No corresponde a este artefacto                 |                 N/A |

Por tanto, para los criterios **aplicables o razonablemente trasladables al frontend**:

**H1: 5/10**
**H2: 10/10**
**H3: 5/10**

Pero hay que leer esos números con algunas precisiones, especialmente H1.2 y H1.3.

---

# Hito 1 aplicado al frontend — 5/10

## H1.1 — Arquitectura de dominio: **2/3**

La pauta busca un dominio que pueda existir sin infraestructura o frameworks y que tenga alta cohesión y responsabilidades bien separadas. 

En TypeScript, el frontend tiene un núcleo bastante limpio:

```text
src/models/
├── product.model.ts
├── cart.model.ts
└── checkout.model.ts

src/services/
├── product.service.ts
├── cart.service.ts
└── checkout.service.ts
```

Hay varias buenas señales.

`cart.service.ts` es particularmente limpio:

```typescript
export function addItem(
  cart: CartModel,
  productId: number,
  qty = 1,
): CartModel
```

No toca:

* DOM;
* `localStorage`;
* `fetch`;
* Bootstrap;
* Vite.

Las funciones:

```text
addItem
removeItem
setItemQty
clearCart
countItems
calculateTotal
toCartLines
```

son esencialmente lógica de negocio pura.

Además, las operaciones son inmutables:

```typescript
return {
  items: cart.items.map(...)
};
```

y los tests comprueban explícitamente que el carrito original no sea modificado.

Eso es muy buena evidencia de cohesión.

### Sin embargo, el dominio no está completamente aislado

Hay dos filtraciones concretas.

### 1. `ProductModel` conoce `ProductDto`

```typescript
import type { ProductDto } from "@/models/product.dto";
```

y:

```typescript
export function toProductModel(
  dto: ProductDto
): ProductModel
```

El modelo de dominio está dependiendo directamente de una estructura de transporte.

Idealmente sería al revés:

```text
ProductModel
     ↑
ProductMapper
     ↑
ProductDto
```

y el modelo no necesitaría saber que existe un DTO.

---

### 2. `checkout.model.ts` conoce `FormData`

Más importante todavía:

```typescript
import {
  requireFormStringField
} from "@/lib/form";
```

y:

```typescript
export function extractRawCheckoutInput(
  formData: FormData
): RawCheckoutInput
```

Eso significa que una clase/archivo que se presenta como modelo de dominio conoce directamente una API del navegador.

El flujo actualmente es:

```text
checkout.model
      ↓
FormData / browser
```

cuando una separación más limpia sería:

```text
View / Form Adapter
      ↓
RawCheckoutInput
      ↓
Domain validation
```

La validación:

```typescript
validateCheckoutInput()
```

sí es perfectamente pura y debería permanecer en el dominio.

Pero:

```typescript
extractRawCheckoutInput()
```

debería probablemente vivir en algo como:

```text
src/adapters/web/
src/mappers/
src/forms/
```

Por estas dos filtraciones, no considero defendible el máximo.

La situación corresponde bastante bien al nivel **competente** de la pauta: buen modelo con pequeñas mezclas de responsabilidades.

**Puntaje equivalente: 2/3.**

---

# H1.2 — Suite automatizada: **3/3 equivalente**

Aquí hay que distinguir tecnología de objetivo.

La pauta dice literalmente **JUnit 5 + Mockito**, lo que obviamente no aplica a un proyecto TypeScript. 

Pero el objetivo arquitectónico sí aplica:

* pruebas automatizadas;
* aislamiento;
* dobles;
* assertions precisas;
* caminos de error;
* dependencias externas simuladas.

El equivalente del proyecto es:

```text
JUnit 5   → Vitest
Mockito   → vi.mock / vi.fn
```

Y encontré **29 archivos de test** distribuidos por toda la aplicación:

```text
api/
components/
data/
lib/
models/
pages/
services/
storage/
views/
```

### La lógica central está bien cubierta por tests unitarios

`cart.service.test.ts`, por ejemplo, prueba:

* agregar;
* incrementar;
* eliminar;
* cantidades <= 0;
* carrito vacío;
* conteo;
* total;
* productos huérfanos;
* líneas de carrito;
* inmutabilidad.

Son tests con assertions semánticas concretas:

```typescript
expect(
  addItem(cart, 1, 3).items
).toEqual([{ id: 1, qty: 5 }]);
```

y:

```typescript
expect(
  calculateTotal(cart, products)
).toBe(25000);
```

### Los servicios externos se aíslan correctamente

`product.service.test.ts` utiliza:

```typescript
const {
  fetchProductsPayload
} = vi.hoisted(() => ({
  fetchProductsPayload: vi.fn(),
}));

vi.mock(
  "@/api/product.api",
  () => ({ fetchProductsPayload })
);
```

Después prueba:

```text
payload correcto
payload que no es array
un elemento inválido
ApiError proveniente de red
```

Esto cumple perfectamente el objetivo que Mockito cumpliría en Java.

### Hay testing de excepciones y errores

Por ejemplo:

```typescript
expect(
  () => extractRawCheckoutInput(formData)
).toThrow(TypeError);
```

y múltiples tests verifican `ApiError`.

### Hay separación entre unit tests y DOM tests

La propia validación de checkout se documenta como función pura y se prueba sin necesidad del DOM.

Esto es una buena señal de testabilidad.

### Veredicto

Si el evaluador interpreta literalmente “debe usar JUnit”, este criterio sería **N/A**, no 3.

Pero si estamos evaluando la aplicación end-to-end conforme al **objetivo técnico de la pauta**, la implementación equivalente satisface completamente ese criterio.

**Puntaje equivalente: 3/3.**

---

# H1.3 — Cobertura de métodos críticos: **0/4 verificable**

Aquí sí aplicaría la pauta literalmente en cuanto al resultado esperado.

Existe infraestructura de cobertura:

```json
"test:coverage": "vitest run --coverage"
```

y:

```typescript
coverage: {
  provider: "v8",
  reporter: [
    "text",
    "html",
    "json",
    "json-summary"
  ],
  include: ["src/**/*.ts"]
}
```

Además existe un workflow específico:

```text
Unit tests report
```

que ejecuta:

```bash
npm run test:report
```

y produce:

```text
coverage/coverage-summary.json
coverage/coverage-final.json
coverage HTML
badge de coverage
```

Eso está muy bien diseñado.

Sin embargo, en el ZIP que recibí **no vienen los resultados de cobertura**.

No tengo:

```text
coverage-summary.json
coverage-final.json
lcov.info
```

ni un porcentaje verificable.

Y la pauta dice expresamente que el evaluador debe determinar si la cobertura **puede verificarse**, no basta con que exista la herramienta. 

Además intenté ejecutar la suite en el análisis anterior, pero la instalación de dependencias no pudo completarse en este entorno.

Por tanto, estrictamente:

> herramienta: sí
> tests: sí
> cobertura verificable en esta entrega: no

El nivel definido por la pauta para ausencia de evidencia verificable es **0**.

**Puntaje verificable: 0/4.**

Este 0 probablemente es **administrativo/evidencial**, no una señal de baja cobertura real.

De hecho, por la cantidad y distribución de tests sospecho que la cobertura efectiva puede ser alta. Pero la propia pauta prohíbe inferirla.

---

## Una salvedad importante sobre H1

Si me proporcionas el:

```text
coverage/coverage-summary.json
```

del workflow actual, H1.3 podría cambiar inmediatamente.

Por ejemplo, si demuestra:

```text
Lines ≥ 80%
Branches ≥ 80%
```

y los módulos de negocio críticos están bien cubiertos, el hito podría pasar de:

**5/10 → 7/10 o 9/10**

dependiendo de branches y caminos críticos.

---

# Hito 3 aplicado al frontend — 5/10

Aquí la evaluación es especialmente interesante porque el frontend **sí tiene una arquitectura por responsabilidades**, pero todavía no implementa Clean Architecture de manera estricta.

---

# H3.1 — Separación en capas desacopladas: **2/3**

La estructura actual es aproximadamente:

```text
pages/
   ↓
views/
   ↓
services/
   ↓
api/

views/
   ↓
storage/

services/
   ↓
models/

components/
   ↓
models / lib
```

Eso es mucho mejor que una aplicación en la que todas las responsabilidades estén mezcladas.

### Separaciones claras

#### API

```text
src/api/
```

encapsula `fetch()`.

`product.api.ts` no conoce siquiera `ProductModel`:

```typescript
export async function
fetchProductsPayload(): Promise<unknown>
```

Eso es una decisión arquitectónica muy buena.

---

#### Persistencia browser

```text
src/storage/
```

encapsula `localStorage`.

`cart.storage.ts` recibe incluso:

```typescript
storage: Storage = window.localStorage
```

lo que permite sustituir la dependencia.

---

#### Lógica de negocio

```text
src/services/cart.service.ts
```

es pura.

No accede a:

```text
window
document
fetch
localStorage
```

---

#### Presentación

```text
views/
components/
```

concentra DOM y Bootstrap.

Ésta es una separación bastante limpia.

### Pero las dependencias no están invertidas

La pauta exige:

```text
Infrastructure
      ↓
Application
      ↓
Domain
```



Actualmente encontramos:

```typescript
// product.service.ts
import {
  fetchProductsPayload
} from "@/api/product.api";
```

y:

```typescript
// checkout.service.ts
import {
  submitOrder
} from "@/api/checkout.api";
```

Por tanto:

```text
Application / Service
       ↓
Concrete API Adapter
```

La capa de negocio conoce directamente el adapter concreto.

Una arquitectura limpia sería:

```text
ProductService
      ↓
ProductGateway
      ↑
HttpProductGateway
```

Hoy falta esa inversión.

También hay las filtraciones ya mencionadas:

```text
ProductModel → ProductDto
CheckoutModel → FormData helper
```

Así que no alcanza 3/3.

Pero la separación global es bastante buena y claramente superior a una organización meramente cosmética.

**Puntaje: 2/3.**

---

# H3.2 — Patrones tácticos: **2/3**

Aquí hay bastante más DDD ligero del que parecía inicialmente.

La pauta busca:

* entidades;
* identidad;
* Value Objects;
* invariantes;
* inmutabilidad;
* autovalidación. 

### Hay modelos de dominio explícitos

```typescript
ProductModel
CartModel
CartItemModel
CheckoutModel
```

### Hay vocabularios cerrados

```typescript
ProductCategory
ProductSubcategory
CheckoutStatus
```

Por ejemplo:

```typescript
export const CHECKOUT_STATUSES = [
  "idle",
  "submitting",
  "success",
  "error",
] as const;
```

Esto funciona casi como un Value Object ligero / enum de dominio.

### Existe inmutabilidad

Los modelos usan:

```typescript
readonly
```

y las operaciones del carrito construyen objetos nuevos.

El test incluso verifica la invariancia:

```typescript
expect(cart.items)
  .toEqual([{ id: 1, qty: 1 }]);
```

después de llamar `addItem`.

### Hay invariantes

Por ejemplo:

```typescript
isFinitePositiveInteger()
```

para IDs y cantidades persistidas.

También:

```typescript
validateCheckoutInput()
```

protege:

* nombre;
* email;
* dirección.

Y los DTOs externos pasan por validadores antes de transformarse en modelos internos.

Eso es bastante positivo.

---

## Pero todavía existe primitive obsession

El dominio continúa representando conceptos como:

```typescript
id: number
qty: number
price: number
email: string
address: string
```

No hay objetos equivalentes a:

```text
ProductId
Quantity
Money
EmailAddress
ShippingAddress
```

Tampoco los modelos se autovalidan al construirse porque son interfaces:

```typescript
const product: ProductModel = { ... }
```

puede construirse desde código interno sin pasar necesariamente por una factory.

Las validaciones están externalizadas:

```text
isProductDto
validateCheckoutInput
isCartItemModel
```

en lugar de que la construcción de un objeto válido sea obligatoria.

Por eso no considero apropiado asignar el máximo.

**Puntaje: 2/3.**

---

# H3.3 — Repository Pattern / contratos: **1/4**

Ésta es la mayor brecha de H3.

La pauta espera:

```text
Use Case
   ↓
Repository Interface
   ↑
Repository Implementation
```



El frontend sí tiene adapters claramente identificables:

```text
api/product.api.ts
api/checkout.api.ts
storage/cart.storage.ts
```

Pero no existen contratos que los abstraigan.

El flujo actual de productos es:

```text
View
  ↓
ProductService
  ↓
fetchProductsPayload()
  ↓
fetch()
```

Y checkout:

```text
View
  ↓
CheckoutService
  ↓
submitOrder()
  ↓
simulated adapter
```

### No existe algo como

```typescript
export interface ProductRepository {
  findAll(): Promise<ProductModel[]>;
}
```

ni:

```typescript
export interface CheckoutGateway {
  submit(
    checkout: CheckoutModel
  ): Promise<void>;
}
```

El service importa directamente la implementación:

```typescript
import {
  fetchProductsPayload
} from "@/api/product.api";
```

Eso es el equivalente TypeScript de:

```java
application
    → JpaRepository
```

que la pauta penaliza.

### Hay desacople funcional, pero no inversión de dependencias

El comentario:

```typescript
"Cuando exista el backend real,
esta es la única función que cambia"
```

es conceptualmente correcto.

Pero la pauta es explícita: no basta con que exista una función que actúe como frontera. Debe existir una **abstracción real** consumida por la capa interna.

En otras palabras:

```text
actual:
Service → Adapter

esperado:
Service → Port ← Adapter
```

Por eso el patrón está insinuado pero no implementado completamente.

Esto corresponde muy bien al nivel **1**, donde la abstracción es superficial y las capas internas terminan conociendo componentes concretos. 

**Puntaje: 1/4.**

---

# Resumen arquitectónico del frontend

Actualmente:

```text
                  ┌────────────┐
                  │   Pages    │
                  └─────┬──────┘
                        ↓
                  ┌────────────┐
                  │   Views    │
                  └─────┬──────┘
                        ↓
              ┌──────────────────┐
              │    Services      │
              └───┬──────────┬───┘
                  ↓          ↓
               Models      API
                            ↓
                          fetch

Views ──────────────→ Storage
                       ↓
                  localStorage
```

Está **bien modularizada**, pero no es todavía Clean Architecture estricta.

Una evolución compatible con H3 sería:

```text
                  UI / Views
                       ↓
                Application
                       ↓
                    Domain
                       ↑
          ┌────────────┴────────────┐
          │                         │
    ProductPort                 CartPort
          ↑                         ↑
          │                         │
 HttpProductAdapter       LocalStorageCartAdapter
```

Y, para checkout:

```text
CheckoutUseCase
      ↓
CheckoutGateway
      ↑
HttpCheckoutAdapter
```

---

# Impacto de una refactorización relativamente pequeña

Lo interesante es que **el frontend está bastante cerca de mejorar mucho H3 sin reescribir la aplicación**.

Podría hacerse algo como:

```text
src/
├── domain/
│   ├── model/
│   │   ├── product.ts
│   │   ├── cart.ts
│   │   └── checkout.ts
│   └── service/
│       └── cart.ts
│
├── application/
│   ├── port/
│   │   ├── product.repository.ts
│   │   ├── checkout.gateway.ts
│   │   └── cart.repository.ts
│   └── usecase/
│       ├── load-products.ts
│       └── submit-checkout.ts
│
└── infrastructure/
    ├── api/
    │   ├── http-product.repository.ts
    │   └── http-checkout.gateway.ts
    ├── storage/
    │   └── local-storage-cart.repository.ts
    └── web/
        ├── views/
        └── components/
```

Sin necesidad de introducir un framework.

Entonces:

```text
domain
   -X→ fetch
   -X→ DOM
   -X→ FormData
   -X→ localStorage

application
   → domain
   → ports

infrastructure
   → application
   → domain
```

Eso permitiría probablemente pasar:

| Criterio | Actual | Con refactor |
| -------- | -----: | -----------: |
| H1.1     |      2 |        **3** |
| H3.1     |      2 |        **3** |
| H3.2     |      2 |        **3** |
| H3.3     |      1 |        **4** |

sin modificar sustancialmente la experiencia de usuario.

---

# Evaluación consolidada de lo que conocemos hasta ahora

Con los dos artefactos evaluados:

| Artefacto                 |        H1 |        H2 |        H3 |       H4 |
| ------------------------- | --------: | --------: | --------: | -------: |
| `unicornt-store-frontend` | **5/10*** | **10/10** | **5/10*** |      N/A |
| `unicornt-store-backend`  |      4/10 |       N/A |      3/10 | **9/10** |

* Evaluación adaptada a los criterios arquitectónicos aplicables al frontend.

Pero **no sumaría estos números entre sí** como si cada repo tuviera que implementar todos los hitos.

Para una evaluación end-to-end del proyecto completo, la forma adecuada será seleccionar la mejor evidencia del artefacto donde corresponde cada requisito:

```text
H1 → proyecto usado para H1
H2 → unicornt-store-frontend
H3 → proyecto usado para H3
H4 → unicornt-store-backend
```

y después hacer una **segunda pasada transversal** sobre frontend + backend para verificar que el sistema final no haya reintroducido violaciones de H1/H3.

Esa segunda pasada es precisamente donde esta evaluación del frontend aporta valor: aunque el Hito 3 original se haya realizado en otro proyecto, el frontend actual tiene dos desviaciones arquitectónicas relevantes para el estado end-to-end:

1. **el dominio conoce detalles externos (`ProductDto`, `FormData`)**;
2. **los services dependen directamente de adapters concretos y no de ports**.

Por tanto, si la expectativa final es que **la aplicación completa conserve las propiedades arquitectónicas aprendidas en H1 y H3**, esos son gaps reales que sí convendría corregir.
