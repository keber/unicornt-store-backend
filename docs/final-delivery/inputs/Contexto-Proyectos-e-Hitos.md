## Contexto del proyecto y análisis del problema - Parte 1

Para H1 decidí usar un proyecto distinto del proyecto final, por que ese proyecto ya cumplía con el 90% de la pauta de evaluación. En H2 se nos pidió un front, y tomé un front de otro proyecto que era una especie de demo o maqueta para luego implementarlo en springboot thymeleaf mvc - eso fue de un curso anterior, lo reutilicé por optimizar tiempos. Luego en H3, se nos indicó que usáramos el mismo proyecto usado en H1. El propósito detrás de esto era construir evolutivamente un proyecto bien hecho conforme a la pauta, basada en arquitectura limpia. Y finalmente en H4 me tocó refactorizar el proyecto springboot mvc para hacerlo cumplir con la pauta. Finalmente en la entrega final tenemos que "conectar todo", algo que en teoría no debería ser tan complicado, pero en mi caso lo es porque lo que hice en H1 y H2 no me sirve directamente para la entrega final, por que es un proyecto distinto. Por eso mi problemática actual es que tengo que tomar el backend que salió a partir de un refactor de un proyecto 'viejo' y autocontenido, con patrón mvc springboot y thymeleaf, que cumple con la rúbrica del hito4 pero que no pasó por el refinamiento de H1 y H3. A su vez, el front pasó por el refactor para cmplir con la rúbrica de H2, pero no pasó por H3, por lo que ambos DEBERÍAN poder conversar mediante un contrato común a partir de la separación por dominios, pero esa 'discusión', diseño y planificación nunca estuvo, por que esto fue un refactor sobre la marcha. Por lo demás, me parece interesante como ejercicio ya que, después de todo, en la vida real los proyectos rara vez parten sobre una sábana limpia (u hoja en blanco), sino de algo que ya no aguanta más parches y mutaciones.
---
Sí, ahora el problema queda mucho más claro, y cambia bastante la lectura arquitectónica.

Lo que tienes no es realmente “un proyecto que fue evolucionando por H1 → H2 → H3 → H4”. Tienes **tres líneas evolutivas parcialmente desconectadas** que ahora deben converger:

```text
Línea A
Proyecto H1
   ↓
refactor H3
   ↓
Clean Architecture / dominio refinado


Línea B
Frontend maqueta anterior
   ↓
refactor H2
   ↓
TypeScript estricto / DOM seguro / async


Línea C
Spring Boot MVC + Thymeleaf legacy
   ↓
refactor H4
   ↓
REST + JWT + PostgreSQL + OpenAPI
```

Y la entrega final exige convertir:

```text
A + B + C
```

en algo que parezca haber evolucionado coherentemente como:

```text
       Frontend
           ↓
       REST contract
           ↓
      Application
           ↓
        Domain
           ↑
     Repository ports
           ↑
   Persistence adapters
```

Ese es un problema bastante más interesante que simplemente “conectar frontend y backend”.

## El punto central

Creo que tu diagnóstico es correcto en lo esencial:

> frontend y backend **pueden técnicamente conversar ya**, pero no necesariamente comparten todavía un **modelo conceptual coherente**.

Eso importa porque hay dos niveles distintos de integración.

El nivel superficial es:

```text
GET /api/v1/products
       ↓
JSON
       ↓
fetch()
       ↓
ProductDto
```

Si ambos lados coinciden en nombres de campos, tipos y rutas, “funciona”.

Pero el nivel que pretende la arquitectura de los hitos anteriores es:

```text
Concepto de negocio
        ↓
modelo de dominio backend
        ↓
caso de uso
        ↓
contrato REST
        ↓
DTO frontend
        ↓
modelo frontend
```

Ahí la pregunta ya no es solamente:

> “¿El frontend puede consumir el JSON?”

sino:

> “¿Estamos representando el mismo concepto de negocio, con las mismas invariantes y límites, a ambos lados?”

Y ésa es precisamente la conversación de diseño que te saltaste por haber hecho refactors independientes.

---

# Esto se parece mucho a un problema real de integración brownfield

Tu comparación con proyectos reales me parece muy acertada.

En una organización real, raramente ocurre:

```text
requisitos
   ↓
modelo perfecto
   ↓
arquitectura perfecta
   ↓
backend
   ↓
frontend
```

Es más frecuente algo como:

```text
Sistema A de 2018
Sistema B adquirido
Frontend nuevo
API heredada
Base de datos histórica
reglas duplicadas
nombres inconsistentes
```

y entonces aparece una iniciativa de modernización.

Tu situación académica, accidentalmente, se parece bastante a un escenario de:

**brownfield modernization + system integration + architectural convergence**.

De hecho, yo evitaría pensar ahora:

> “¿Cómo llevo el código del H1/H3 al backend?”

porque eso puede inducir a trasladar clases mecánicamente.

La pregunta más útil sería:

> **¿Qué propiedades arquitectónicas aprendidas y demostradas en H1/H3 necesito introducir ahora en el sistema final?**

No necesariamente necesitas reutilizar físicamente el proyecto H1/H3.

Necesitas reutilizar **el diseño aprendido**.

---

# Hay una decisión arquitectónica importante

Yo veo dos estrategias posibles.

## Estrategia 1 — Portar literalmente el dominio de H1/H3

Sería algo como:

```text
Proyecto H1/H3
      ↓
copiar domain/
copiar application/
copiar ports/
      ↓
adaptarlo a Unicornt Store
```

Esto sólo tiene sentido si el dominio del proyecto H1/H3 se parece realmente al e-commerce.

Por lo que has contado anteriormente, sospecho que **no es así**.

Si aquel proyecto modelaba otra cosa, reutilizar sus clases no tendría sentido.

En ese caso, el verdadero valor de H1/H3 no está en su código sino en sus patrones:

* dominio puro;
* entidades;
* Value Objects;
* invariantes;
* application/use cases;
* ports;
* adapters;
* inversión de dependencias.

Esas propiedades hay que **reaplicarlas al dominio Unicornt**, no copiar las implementaciones anteriores.

---

# Estrategia 2 — Usar H1/H3 como patrón de referencia

Ésta me parece mucho más adecuada.

Tomas el backend H4 actual:

```text
REST controllers
DTOs
services
JPA entities
Spring Data
PostgreSQL
JWT
OpenAPI
```

y haces una segunda refactorización cuya meta sea:

```text
mantener H4
+
incorporar H1
+
incorporar H3
```

sin romper la API.

Eso es exactamente una modernización incremental.

---

# Lo bueno es que H4 ya te dio una frontera

La decisión de convertir el proyecto MVC a REST fue más importante de lo que parece.

Antes tenías:

```text
Thymeleaf
   ↕
Controller
   ↕
Service
   ↕
Persistence
```

Todo estaba dentro de una aplicación.

Ahora tienes:

```text
Frontend
   ↓ HTTP
REST API
   ↓
Backend
```

Eso introduce una **frontera física de proceso**.

Y esa frontera puede convertirse ahora en el contrato estable mientras refactorizas internamente.

En otras palabras, puedes hacer:

```text
                    CONTRATO ESTABLE

Frontend ───────────── REST API ───────────── Backend

                                         ↓ refactor interno

                                   Application
                                        ↓
                                      Domain
                                        ↑
                                      Ports
                                        ↑
                                     Adapters
```

El frontend no necesita enterarse de que internamente cambias:

```text
ProductEntity
```

por:

```text
Product
ProductJpaEntity
ProductMapper
```

mientras el JSON contractual permanezca estable.

Ésta es precisamente una de las grandes ventajas de haber hecho H4 primero.

---

# Yo empezaría por definir el dominio común, no por mover paquetes

Ahora mismo el riesgo sería hacer esto:

```text
domain/
application/
infrastructure/
```

y empezar a redistribuir clases.

Eso puede producir una **Clean Architecture cosmética**, exactamente el problema que detectamos en el backend actual.

Antes de cambiar código, haría una pequeña fase de modelado.

Para Unicornt probablemente tienes algo así:

```text
Catalog
  Product
  Category
  ProductType

Cart
  Cart
  CartItem
  Quantity

Checkout
  Order
  OrderItem
  Address
  Money

Identity
  User
  Role
  Credentials
```

Y aquí aparece una decisión interesante:

¿todo esto es realmente un único dominio?

Probablemente no.

Podrías pensar en bounded contexts simples:

```text
Catalog
Cart
Ordering
Identity
```

No necesitas implementar DDD estratégico completo ni microservicios.

Sólo necesitas evitar construir un enorme:

```text
domain/
  Product
  User
  Cart
  Order
  Role
  Address
```

sin límites conceptuales.

---

# Un ejemplo concreto: Product

Hoy, según lo que vimos, tu backend tiene aproximadamente:

```text
ProductEntity
```

que cumple simultáneamente roles de:

* modelo persistente;
* objeto manipulado por servicios;
* fuente para DTO REST.

Una evolución razonable sería:

```text
domain/catalog/Product.java
```

algo como:

```java
public final class Product {
    private final ProductId id;
    private final String name;
    private final Money price;
    private final CategoryId categoryId;

    ...
}
```

No digo que debas llenar todo de Value Objects sólo por DDD.

Pero `Money` tiene bastante sentido.

`Quantity` también.

Tal vez `Email`.

Tal vez `ProductId`.

En cambio crear:

```text
ProductName
ProductDescription
CategoryName
```

sólo para sumar Value Objects sería ceremonial.

---

# Persistencia

Entonces tendrías:

```text
domain.catalog.Product
```

y separadamente:

```text
infrastructure.persistence.jpa.ProductJpaEntity
```

con:

```text
ProductPersistenceMapper
```

Así:

```text
ProductJpaEntity
       ↕
ProductPersistenceMapper
       ↕
Product
```

Esto arreglaría simultáneamente varias cosas que hoy penalizan H1/H3/H4.2.

---

# Pero la mejora más importante no son los entities

La modificación decisiva sería ésta:

Hoy:

```text
ProductServiceImpl
       ↓
Spring Data ProductRepository
```

Objetivo:

```text
SearchProductsUseCase
       ↓
ProductRepository
       ↑
JpaProductRepositoryAdapter
       ↓
SpringDataProductJpaRepository
```

Es decir:

```text
application → port
infrastructure → port
```

en vez de:

```text
domain/service → infrastructure
```

Eso es realmente H3.

Mover `ProductRepository.java` de carpeta sin cambiar esta dirección no arreglaría nada.

---

# Application layer

También creo que tu backend actual probablemente está usando la palabra `service` para mezclar dos cosas distintas:

1. casos de uso;
2. lógica de dominio.

Por ejemplo:

```text
CheckoutServiceImpl
```

probablemente coordina:

* recuperar carrito;
* verificar stock;
* calcular totales;
* crear orden;
* guardar orden;
* limpiar carrito.

Eso no es fundamentalmente un **domain service**.

Es un **application use case**.

Sería más expresivo algo como:

```text
application/
  checkout/
    CheckoutUseCase.java
    CheckoutService.java
```

y dentro del dominio:

```text
domain/
  order/
    Order.java
    OrderItem.java
    Money.java
```

El application service coordina.

El dominio decide reglas.

---

# Y aquí se conecta el frontend

El frontend hoy tiene justamente una frontera bastante parecida:

```text
view
  ↓
service
  ↓
api
```

La debilidad que identificamos era:

```text
service → api concreto
```

Así que podrías aplicar el mismo concepto de ports.

Pero aquí haría una advertencia.

## No sobrediseñaría el frontend

No intentaría convertir una pequeña aplicación vanilla TS en una réplica completa de Hexagonal Architecture del backend.

Algo como:

```text
src/
  domain/
  application/
  ports/
  adapters/
  infrastructure/
  factories/
```

puede terminar siendo peor que el problema.

Para el frontend bastaría probablemente con:

```text
src/
  domain/
  application/
  api/
  storage/
  ui/
```

y dos o tres contratos donde realmente aportan valor.

Por ejemplo:

```typescript
export interface ProductGateway {
  getProducts(): Promise<Product[]>;
}
```

y:

```typescript
export interface CheckoutGateway {
  checkout(order: CheckoutRequest): Promise<CheckoutResult>;
}
```

Después:

```text
HttpProductGateway
HttpCheckoutGateway
```

Eso elimina la dependencia:

```text
service → fetch implementation
```

sin meter veinte capas.

---

# El contrato compartido merece atención especial

Aquí está probablemente el punto más importante de la integración.

No intentaría hacer que frontend y backend compartan literalmente las mismas clases.

Eso crea acoplamiento innecesario.

Compartiría **el contrato**, no el modelo interno.

La frontera debería ser:

```text
Backend Domain
      ↓
REST DTO
      ↓
OpenAPI
      ↓
Frontend DTO
      ↓
Frontend Domain Model
```

Eso significa que:

```text
Product.java
```

y:

```text
ProductModel.ts
```

no necesitan tener exactamente la misma forma.

Tienen que poder traducirse mediante un contrato estable.

---

# OpenAPI puede convertirse en la pieza central

Y aquí tienes una ventaja enorme: H4 ya exige OpenAPI.

Entonces en vez de mantener dos mundos manualmente:

```text
Java DTO
TypeScript DTO
```

puedes declarar:

```text
OpenAPI
```

como contrato de integración.

Conceptualmente:

```text
                   OpenAPI

Backend DTO ─────────┼───────── Frontend DTO
                     │
                 API contract
```

Incluso podrías generar tipos TypeScript desde OpenAPI.

Por ejemplo conceptualmente:

```text
openapi.yaml
    ↓
openapi-typescript
    ↓
generated/api-types.ts
```

Entonces desaparece una clase completa de problemas:

```text
backend:
price: int

frontend:
price: string
```

o:

```text
backend:
productType

frontend:
subcategory
```

El compilador puede ayudarte a detectar drift.

---

# Pero no haría del OpenAPI el dominio

Ésta es otra distinción importante.

No querrías:

```text
OpenAPI schema
      ↓
Domain objects en todos lados
```

Porque el contrato HTTP y el dominio tienen objetivos diferentes.

Por ejemplo el backend podría tener:

```text
Money
```

internamente.

La API podría exponer:

```json
{
  "price": 12990,
  "currency": "CLP"
}
```

y el frontend convertir eso en:

```typescript
interface Money {
  amount: number;
  currency: Currency;
}
```

Perfectamente válido.

---

# Creo que hay un problema concreto que deberías resolver primero: vocabulario

Antes de integrar, haría una tabla muy simple:

| Concepto  | Backend       | API                | Frontend             |
| --------- | ------------- | ------------------ | -------------------- |
| Producto  | `Product`     | `ProductResponse`  | `ProductModel`       |
| Categoría | `Category`    | `CategoryResponse` | `ProductCategory`    |
| Tipo      | `ProductType` | ?                  | `ProductSubcategory` |
| Precio    | `Money`/int   | number             | number               |
| Carrito   | `Cart`        | `CartResponse`     | `CartModel`          |
| Ítem      | `CartItem`    | `CartItemResponse` | `CartItemModel`      |
| Orden     | `Order`       | `OrderResponse`    | `CheckoutModel` / ?  |
| Dirección | `Address`     | `AddressRequest`   | checkout address     |
| Usuario   | `User`        | auth DTO           | ?                    |

Estoy casi seguro de que ahí van a aparecer las primeras discrepancias reales.

Por ejemplo, lo que el backend llama:

```text
ProductType
```

y el frontend:

```text
ProductSubcategory
```

pueden o no ser el mismo concepto.

Eso no es un problema de TypeScript ni de Java.

Es un problema de **ubiquitous language**.

Y resolverlo antes del código te va a ahorrar mucho parche.

---

# Cart puede ser el punto más conflictivo

Hay otro aspecto que me parece importante.

Por lo que vimos, el frontend actualmente tiene:

```text
localStorage cart
```

mientras el backend H4 tiene:

```text
CartEntity
CartRestController
```

Entonces tienes potencialmente **dos fuentes de verdad para el carrito**.

Eso hay que decidir explícitamente.

No dejaría:

```text
frontend localStorage cart
+
backend persistent cart
```

funcionando simultáneamente sin una política.

Tienes tres alternativas.

### A. Backend owns cart

```text
Frontend
  ↓
Cart API
  ↓
Backend Cart
```

localStorage podría quedar sólo como cache.

Para una aplicación autenticada, probablemente es la opción más coherente.

### B. Frontend owns cart hasta checkout

```text
localStorage
   ↓
CheckoutRequest
   ↓
backend
```

El backend no tendría realmente un carrito persistente.

Más simple para una demo.

### C. Híbrido con sincronización

```text
local cart ↔ backend cart
```

No lo recomendaría para este proyecto salvo que sea un requisito.

Introduce:

* conflictos;
* merge;
* login/logout;
* recuperación;
* expiración;
* sincronización.

No parece que aporte valor académico.

---

# Authentication también cambia la arquitectura del frontend

H4 introdujo JWT.

Así que al integrar tendrás que decidir:

```text
¿Dónde vive el token?
¿Quién lo adjunta?
¿Qué ocurre ante 401?
¿Qué ocurre ante 403?
¿Cómo cambia la UI?
```

Yo evitaría que cada módulo haga:

```typescript
fetch(url, {
  headers: {
    Authorization: `Bearer ${token}`
  }
});
```

Sería mejor una frontera común:

```text
HttpClient
   ↓
Auth token provider
```

o al menos:

```typescript
apiFetch(...)
```

que centralice:

* base URL;
* Authorization;
* JSON;
* errores;
* 401.

Eso también ayuda a H3 porque la infraestructura HTTP queda localizada.

---

# Me parece útil pensar la integración como cuatro reconciliaciones

No como “conectar front y back”.

## 1. Reconciliación de dominio

```text
¿Qué significa Product?
¿Qué significa Category?
¿Qué significa Order?
¿Qué reglas pertenecen a cada uno?
```

## 2. Reconciliación contractual

```text
¿Qué JSON intercambiamos?
¿Qué endpoints?
¿Qué errores?
¿Qué estados HTTP?
```

## 3. Reconciliación arquitectónica

```text
¿Quién depende de quién?
¿Dónde están los use cases?
¿Dónde están ports y adapters?
```

## 4. Reconciliación operacional

```text
CORS
JWT
base URL
profiles
Docker
deploy
environment variables
```

Y las haría en ese orden.

---

# No intentaría hacer un big-bang refactor

Tu backend ahora funciona y cumple prácticamente H4.

Desarmarlo entero para introducir H1/H3 tiene mucho riesgo.

Preferiría una estrategia tipo **strangler**, incluso dentro del mismo repositorio.

Por ejemplo, empezar sólo con `Product`.

Estado inicial:

```text
ProductRestController
        ↓
ProductServiceImpl
        ↓
ProductRepository
        ↓
ProductEntity
```

Paso 1:

```text
domain/catalog/Product
```

Paso 2:

```text
application/catalog/ProductRepository
```

Paso 3:

```text
JpaProductRepositoryAdapter
```

Paso 4:

```text
ProductPersistenceMapper
```

Paso 5:

```text
ProductRestMapper
```

Resultado:

```text
ProductRestController
        ↓
SearchProductsUseCase
        ↓
ProductRepository port
        ↑
JpaProductRepositoryAdapter
        ↓
SpringDataProductRepository
        ↓
ProductJpaEntity
```

Y una vez validado, repetir:

```text
Category
Cart
Order
User
```

---

# El orden importa

Yo no empezaría por `User` ni `JWT`.

Authentication suele contaminar rápidamente todo el diseño y agrega Spring Security a la discusión.

Empezaría por:

1. **Catalog/Product**
2. **Cart**
3. **Checkout/Order**
4. **Identity/Auth**

Porque Product es probablemente el slice más sencillo para demostrar el patrón.

Luego Cart/Checkout te obligarán a resolver las invariantes reales.

---

# Hay una oportunidad académica muy buena aquí

Lo que accidentalmente ocurrió puede convertirse incluso en una fortaleza de la entrega.

Puedes explicar que el proyecto final no nació como greenfield Clean Architecture.

La evolución fue:

```text
Legacy MVC monolith
        ↓
REST extraction
        ↓
frontend decoupling
        ↓
architectural convergence
        ↓
Clean/Hexagonal boundaries
```

Eso es un relato técnicamente bastante más interesante que:

> “Creamos una aplicación nueva siguiendo Clean Architecture desde cero.”

Porque demuestra algo mucho más realista:

**refactoring toward architecture**.

La arquitectura no es solamente una estructura inicial.

También puede ser un **estado objetivo hacia el que converges mediante cambios seguros**.

---

# Y eso cambia incluso cómo evaluaría el proyecto

Cuando finalmente tengas:

```text
frontend
backend
```

integrados, no evaluaría H1/H3 mirando dónde se desarrollaron originalmente.

Evaluaría el **estado final del producto**.

Por ejemplo, para H3 preguntaría:

```text
¿El backend final respeta dependency inversion?
¿El dominio final está limpio?
¿Frontend y backend tienen adapters claros?
¿El contrato HTTP está aislado?
```

Si la respuesta es sí, entonces no importa que la implementación histórica haya sido:

```text
H1 → proyecto A
H2 → proyecto B
H3 → proyecto A
H4 → proyecto C
```

La entrega final demostraría que **trasladastes los aprendizajes arquitectónicos hacia el sistema integrado**.

Y ahí creo que está exactamente el trabajo que te queda ahora.

No es tanto “conectar dos repositorios”.

Es:

> **hacer converger dos artefactos modernizados de manera independiente hacia una sola arquitectura coherente, preservando lo que ya funciona.**

Eso sí es un problema de arquitectura de software de verdad.
