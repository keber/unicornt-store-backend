# Contexto del proyecto y análisis del problema - Parte 2

Entiendo, me parecen muy positivas las apreciaciones y me hacen sentido en general. Sólo quiero hacer una precisión relevante: En el caso de otf-sisacad (utilizado en H1 y H3), era un sistema front-back funcional (por lo menos en los tests) punta a punta. En unicornt-store en cambio tenemos un frontend que es una maqueta y tiene avanzados algunos puntos de conexión, pero nunca se conectó con un back. De modo similar, su backend tras la refactorización nunca ha sido probado con un front. Con esto quiero hacer hincapié en que no existe actualmente un contrato que se puede romper o que habría que 'salvar' del rompimiento, como sí ocurría en otf-sisacad. En ese sentido, tenemos un poco más de libertad para tocar ambos mundos, toda vez que no se ha definido aún el contrato entre ambos, más allá de la robusta definición de api del backend (que no quiere decir necesariamente que sea la más idónea, sólo que está trabajada).
---
Sí. Esa precisión cambia una premisa importante de la estrategia anterior.

Yo estaba tratando la API actual de `unicornt-store-backend` como una **frontera ya contratada** que convenía congelar mientras refactorizábamos internamente, replicando la estrategia de Sisacad. Pero en Unicornt eso sería prematuro: **todavía no existe un sistema integrado cuyo comportamiento contractual haya que preservar**.

Lo que existe es más bien esto:

```text
Frontend H2                              Backend H4
───────────                              ──────────

maqueta funcional                       API REST funcional
modelos TS                              DTOs
carrito local                           carrito backend
checkout simulado                       órdenes/checkout
async/error/loading                     errores HTTP uniformes
                                         JWT
                                         OpenAPI
       │                                    │
       │            ? ? ?                   │
       └─────────────╳──────────────────────┘

              CONTRATO NO DISEÑADO
```

Y esa `╳` es ahora probablemente **el punto más importante del proyecto**.

## Eso nos da una ventaja

No estamos obligados a hacer:

```text
Frontend actual → adaptarlo a la API actual
```

ni tampoco:

```text
Backend actual → deformarlo para imitar la maqueta
```

Podemos hacer algo mejor:

```text
Frontend actual ──┐
                  │
                  ├──► DISEÑO DEL SISTEMA FINAL
                  │
Backend actual ───┘
```

y permitir que **ambos cambien** para converger.

Esto reduce muchísimo el riesgo de convertir decisiones accidentales de alguno de los dos refactors en arquitectura definitiva.

---

# La API actual pasa de ser “contrato” a ser “candidato de diseño”

Creo que ésta es la distinción fundamental.

Actualmente `unicornt-store-backend` tiene una API bastante elaborada:

* rutas REST;
* DTOs;
* códigos HTTP;
* errores uniformes;
* JWT;
* OpenAPI;
* perfiles;
* Swagger.

Eso es valioso.

Pero no significa automáticamente:

> “ésta debe ser la API definitiva”.

La lectura correcta ahora sería:

> “Tenemos una implementación H4 técnicamente fuerte que constituye una excelente propuesta inicial de contrato.”

Entonces cada elemento puede clasificarse así:

```text
CONSERVAR
porque está bien diseñado y sirve al sistema final

AJUSTAR
porque la semántica frontend/backend no coincide

ELIMINAR
porque responde al antiguo sistema MVC o a decisiones heredadas

AGREGAR
porque aparece una necesidad al integrar
```

Por ejemplo, no deberíamos conservar `/api/v1/cart` simplemente porque ya existe si finalmente decidimos que el carrito debe vivir únicamente en el cliente hasta checkout.

Y tampoco deberíamos conservar el carrito local del frontend sólo porque H2 lo implementó bien si la experiencia final requiere un carrito asociado a la cuenta.

---

# Entonces ya no empezaría por el refactor Clean Architecture

Con esta nueva información cambiaría el orden que propuse.

Antes decía más o menos:

```text
Sisacad architecture
        ↓
refactor backend
        ↓
connect frontend
```

Ahora creo que el orden correcto es:

```text
1. Reconciliar el dominio
        ↓
2. Definir comportamiento end-to-end
        ↓
3. Diseñar contrato frontend/backend
        ↓
4. Refactorizar backend hacia H1/H3
        ↓
5. Adaptar frontend al contrato
        ↓
6. Integrar
        ↓
7. Evaluar H1-H4 sobre resultado final
```

La razón es sencilla: si primero hacemos una gran refactorización del backend alrededor de los conceptos actuales, podemos terminar construyendo una arquitectura muy limpia **para un modelo de interacción que después descubramos que no queremos**.

---

# Primero: ¿qué aplicación queremos realmente?

No en términos visuales, sino en términos de comportamiento.

Por lo que ya conocemos, hay al menos cuatro áreas:

```text
CATALOG
Products
Categories
Product types/subcategories
Search/filter

CART
Add
Remove
Change quantity
Totals

CHECKOUT / ORDERING
Customer data
Address
Checkout
Order
Order status

IDENTITY
Register
Login
JWT/session
Authorization
```

Para cada una habría que decidir:

1. qué concepto existe;
2. quién es dueño del estado;
3. qué reglas tiene;
4. qué operaciones necesita el usuario;
5. cuáles requieren backend.

Eso define mucho mejor la API que mirar los controllers actuales.

---

# Cart es el ejemplo perfecto

Hoy tienes dos implementaciones independientes.

### Frontend

Conceptualmente:

```text
CartModel
   ↓
cart.service
   ↓
localStorage
```

### Backend

Conceptualmente:

```text
CartRestController
   ↓
CartService
   ↓
CartRepository
   ↓
DB
```

Si simplemente “los conectamos”, probablemente construiremos algo redundante.

Antes tenemos que decidir qué significa **Cart en el sistema final**.

### Alternativa A — carrito persistente del usuario

```text
Browser
   ↓
Cart API
   ↓
Backend
   ↓
PostgreSQL
```

Ventajas:

* disponible en distintos dispositivos;
* asociado al usuario;
* fuente única de verdad.

Entonces `localStorage` deja de ser repository principal. Puede desaparecer o convertirse en cache.

### Alternativa B — carrito anónimo local

```text
Browser/localStorage
       ↓
   Checkout
       ↓
Backend crea Order
```

Mucho más simple.

Aquí probablemente ni siquiera necesitas `CartEntity` ni endpoints CRUD de carrito en backend.

### Alternativa C — anónimo local que se sincroniza al autenticar

Es probablemente la experiencia más completa:

```text
anonymous cart
    ↓ login
merge
    ↓
server cart
```

pero añade una complejidad que quizá la evaluación académica no recompensa en absoluto.

Yo probablemente escogería A o B dependiendo de los requisitos funcionales.

Lo importante es que **ahora tenemos libertad real de decidirlo**.

---

# Lo mismo ocurre con Checkout y Order

El frontend actualmente tiene algo llamado aproximadamente:

```text
CheckoutModel
```

mientras que el backend tiene:

```text
Order
Address
Checkout
```

No tenemos por qué hacer:

```text
CheckoutModel.ts == OrderRequest.java
```

De hecho probablemente sería incorrecto.

Podemos definir la interacción real:

```text
Checkout command
────────────────

customer/contact
shipping address
cart/items
```

y como consecuencia:

```text
POST /api/v1/orders
```

o quizá:

```text
POST /api/v1/checkout
```

dependiendo de qué significado queramos darle.

El backend puede crear internamente:

```text
Order
OrderItem
Money
Address
```

sin que el frontend tenga que conocer esa estructura completa.

---

# Aquí es donde H1/H3 puede servir realmente

Sisacad no debería dictarnos **qué endpoints** debe tener Unicornt.

Debe dictarnos aproximadamente **cómo evitar que esas decisiones externas contaminen el núcleo**.

Por ejemplo, después de diseñar que necesitamos:

```http
POST /api/v1/orders
```

con:

```json
{
  "items": [...],
  "shippingAddress": {...}
}
```

el interior del backend podría quedar:

```text
OrderRestController
       │
       ▼
PlaceOrderUseCase
       │
       ▼
Order / Cart / Money / Quantity
       │
       ▼
OrderRepository
       ▲
       │
JpaOrderRepositoryAdapter
```

Así:

* H4 gobierna la frontera HTTP;
* H1 gobierna el modelo;
* H3 gobierna dependencias;
* H2 gobierna el cliente.

Eso sí constituye una evolución coherente de los cuatro hitos.

---

# Tampoco deberíamos obligar al frontend a usar el dominio del backend

Hay una tentación comprensible:

> “Definamos el dominio común y hagamos que ambos representen exactamente lo mismo.”

Yo evitaría eso.

Deberíamos compartir **lenguaje y contrato**, no clases.

Por ejemplo:

```text
              concepto PRODUCT

                    │
       ┌────────────┴────────────┐
       ▼                         ▼
BACKEND                       FRONTEND

Product                       ProductModel
Money                         displayPrice
ProductId                     number/id
Category                      ProductCategory
```

Y entre ambos:

```text
ProductResponse
      ↕
OpenAPI
      ↕
ProductDto
```

El frontend podría tener información derivada para presentación que no existe en backend.

El backend podría tener invariantes que nunca necesita enviar al navegador.

Eso está bien.

---

# El verdadero “contrato común” es semántico + OpenAPI

Yo diría que tiene dos niveles.

## Nivel 1 — lenguaje

Por ejemplo resolver cosas como:

```text
Category vs ProductType vs Subcategory

Checkout vs Order

Customer vs User

Address vs ShippingAddress

price vs Money

CartItem.quantity vs qty
```

Eso es lo que evita que ambos sistemas hablen idiomas parecidos pero diferentes.

## Nivel 2 — contrato técnico

Una vez resuelto lo anterior:

```text
OpenAPI
```

se convierte en la especificación ejecutable.

Entonces:

```text
Backend REST DTO
       ↓
     OpenAPI
       ↓
generated TypeScript types
       ↓
frontend adapter
```

Y aquí creo que generar tipos TypeScript desde el OpenAPI sería especialmente valioso en este proyecto.

No porque sea imprescindible, sino porque demuestra muy claramente que:

> **el contrato entre aplicaciones es explícito y verificable.**

---

# Esto cambia también qué hacer con los characterization tests

En Sisacad tenían un propósito muy claro:

```text
Tenemos consumidor funcional
         ↓
no debemos romper contrato
```

En Unicornt todavía no.

Así que **no congelaría ahora toda la API existente mediante characterization tests**.

Congelaríamos accidentalmente decisiones que todavía queremos discutir.

Primero:

```text
API H4 actual
      +
frontend H2 actual
      ↓
diseño
      ↓
API objetivo
```

Cuando lleguemos a ese acuerdo, **ahí sí** escribimos contract/characterization tests.

Es decir:

```text
ANTES DE DEFINIR CONTRATO
tests actuales = evidencia / safety net

DESPUÉS DE DEFINIR CONTRATO
contract tests = especificación que no debe romperse
```

Es una diferencia bastante importante.

---

# Incluso algunas cosas “buenas” del backend podrían cambiar

Por ejemplo, H4 actualmente tiene DTOs y endpoints bien documentados.

Pero imaginemos que encontramos:

```text
GET /api/v1/products
```

que devuelve una entidad demasiado rica:

```json
{
  "id": 1,
  "name": "...",
  "categoryId": 4,
  "productTypeId": 7,
  ...
}
```

mientras el frontend necesita:

```json
{
  "id": 1,
  "name": "...",
  "category": "Polera",
  "subcategory": "Linux",
  "price": 12990,
  "image": "..."
}
```

No deberíamos introducir cinco llamadas adicionales desde el frontend sólo para preservar el DTO actual.

Podría ser más adecuado que el backend exponga una representación de catálogo orientada al caso de uso.

La API debe servir al consumidor, no reflejar la base de datos.

Y H3 precisamente nos permite hacerlo sin contaminar el dominio.

---

# Eso nos lleva a una distinción de tres modelos

Creo que para el proyecto final sería pedagógicamente muy potente mostrar explícitamente:

```text
PERSISTENCE MODEL
ProductJpaEntity

       ↕ mapper

DOMAIN MODEL
Product

       ↕ mapper

API MODEL
ProductResponse

       │ OpenAPI
       ▼

FRONTEND TRANSPORT MODEL
ProductDto

       ↕ mapper

FRONTEND MODEL
ProductModel
```

Puede parecer mucho para una aplicación pequeña, pero en este caso **cada separación está directamente asociada a algo que la pauta evalúa**.

Y no necesariamente implica mucho código.

---

# También tenemos libertad para mejorar la API sin perder H4

Esto es crucial.

H4 no exige las rutas concretas actuales.

Exige:

* REST semántico;
* verbos adecuados;
* códigos HTTP;
* manejo centralizado de errores;
* JSON uniforme;
* PostgreSQL/JPA;
* OpenAPI;
* perfiles dev/prod.



Por tanto podemos modificar:

```text
paths
DTOs
payloads
resource boundaries
```

y **seguir obteniendo 9–10/10 en H4** si mantenemos esas propiedades.

Eso nos da bastante libertad para diseñar bien la integración.

---

# Entonces reformularía el problema actual

Ya no diría:

> “Tenemos que integrar el frontend H2 con el backend H4 y evitar romper el backend.”

Diría:

> **Tenemos dos prototipos maduros pero independientes —uno del cliente y otro del servidor— y una arquitectura H1/H3 de referencia. Tenemos que diseñar la frontera que nunca existió y después hacer converger ambos lados hacia ella.**

Eso es mucho más exacto.

Visualmente:

```text
                     ┌────────────────┐
                     │ otf-sisacad    │
                     │ H1/H3 pattern  │
                     └───────┬────────┘
                             │
                       arquitectura
                             │
                             ▼

┌──────────────────┐    DISEÑO FINAL    ┌──────────────────┐
│ Unicornt Front   │ ────── ? ──────── │ Unicornt Back    │
│                  │                    │                  │
│ H2               │                    │ H4               │
│ TS / UI / async  │                    │ REST/JPA/JWT     │
└────────┬─────────┘                    └────────┬─────────┘
         │                                       │
         └────────── ambos modificables ─────────┘
                             │
                             ▼
                    Sistema integrado
```

---

## Y creo que esto mejora considerablemente nuestra siguiente etapa

Con todos los ZIP disponibles, ya no empezaría implementando.

Haría primero una **reconciliación concreta de los dos Unicornt**:

1. inventario de conceptos del frontend;
2. inventario de conceptos del backend;
3. endpoints backend actuales;
4. operaciones que el frontend realmente necesita;
5. modelo de estado de ambos;
6. incompatibilidades semánticas;
7. decisiones de ownership;
8. propuesta de contrato objetivo;
9. qué conservar/modificar/eliminar de la API actual;
10. recién entonces arquitectura interna final.

El producto de esa fase debería ser una matriz del tipo:

| Capacidad  | Front actual        | Back actual            | Decisión objetivo     |
| ---------- | ------------------- | ---------------------- | --------------------- |
| catálogo   | mock/data service   | REST `/products`       | backend               |
| categorías | local vocabulary    | Category/ProductType   | reconciliar           |
| carrito    | localStorage        | persistente            | **decidir ownership** |
| checkout   | simulado            | implementación backend | redefinir contrato    |
| órdenes    | casi inexistente    | backend                | backend               |
| auth       | inexistente/parcial | JWT                    | integrar              |
| errores    | UI states           | JSON errors            | mapear                |
| tipos      | TS propios          | OpenAPI DTOs           | generar/adaptar       |

Y creo que precisamente **Cart, Product taxonomy, Checkout/Order y Authentication** serán las cuatro decisiones que determinarán casi toda la integración restante.

En ese sentido, que todavía no exista un contrato no es un problema adicional. Es, de hecho, **una oportunidad de diseñarlo bien una sola vez antes de convertirlo en una restricción**.
