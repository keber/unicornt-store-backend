# Contexto parte 3 y Borrador Plan

Genial. Ahora que estamos alineados, necesito:

Que entres en modo pragmático por encima de sobre analítico.
Que acotemos las discusiones y tomemos decisiones.
Que armemos un plan realista y aterrizado.
Que nos concentremos en lo que importa.

Con lo anterior como premisa, vamos acotando el contexto:

Unicornt Store es una tienda de venta en línea de poleras y tazones de memes informáticos. Su misión es ser un bus de transporte de alegría y humor entre personas y equipos, mediante productos de calidad discutible, compra compulsiva y pago inmediato, estampados con memes clásicos y contingentes sin derecho a retracto. (La descripción es humor informático, no la cuestionemos).
Como tienda de venta online, cuenta con elementos comunes al rubro: Productos en un Catálogo, con ciertas Categorías y determinado stock disponible, donde el cliente puede seleccionar 1 o más productos y añadirlos a un carro de compras, para luego (proceso de check out) finalizar y pagar los elementos del carro, continuar comprando, o cancelar la compra. En caso de confirmar la compra, se deduce la cantidad de productos comprados del inventario de productos, pasando a un proceso de despacho hasta el domicilio indicado por el cliente.
Como cliente quiero añadir productos del catálogo a mi carro de compras. para tener una selección de productos que potencialmente compraré.
Como cliente quiero que los productos que agrego al Carro antes de iniciar sesión, permanezcan en el carro después de iniciar sesión.
Como cliente quiero comprar los productos de mi Carro de compras.
Como cliente quiero que al comprar los productos el sistema me avise si se confirmó la compra.
Como administrador, quiero modificar las características de un producto y que persistan los cambios,
Como administrador, quiero agregar y eliminar productos del catálogo, incluyendo una imagen ilustrativa del producto.

---
Perfecto. Con esas historias ya podemos dejar de discutir arquitectura en abstracto y fijar decisiones.

Yo establecería desde ahora este **alcance funcional objetivo** para la entrega final.

## 1. Dominios que realmente necesitamos

No intentaría modelar más que esto:

```text
Catalog
 ├─ Product
 ├─ Category
 └─ Stock

Cart
 ├─ Cart
 └─ CartItem

Ordering
 ├─ Order
 ├─ OrderItem
 └─ ShippingAddress

Identity
 └─ User
```

Nada de promociones, wishlist, medios de pago reales, tracking sofisticado, recomendaciones, etc., salvo que la pauta lo exija después.

Y tampoco introduciría bounded contexts formales ni una estructura DDD enorme. Son **agrupaciones conceptuales útiles**, no una excusa para multiplicar carpetas.

---

# 2. Decisión: el backend será dueño del catálogo y stock

Esto es directo.

```text
Backend = fuente de verdad

Product
Category
Price
Stock
Image
```

El frontend sólo consume esos datos.

Por tanto eliminaremos gradualmente cualquier catálogo/mock que funcione como fuente de verdad en el frontend.

El frontend puede mantener:

```typescript
ProductDto -> ProductModel
```

si esa separación sigue aportando valor, pero el dato original vendrá del backend.

---

# 3. Decisión: carrito híbrido simple

Aquí sí necesitamos resolver la historia:

> Los productos agregados antes de iniciar sesión deben permanecer después del login.

No haría sincronización bidireccional compleja.

Usaría este comportamiento:

### Usuario anónimo

```text
Cart
  ↓
localStorage
```

### Al iniciar sesión

```text
local cart
    +
server cart
    ↓
merge
    ↓
backend cart
    ↓
local cart se limpia
```

### Usuario autenticado

```text
Backend cart = fuente de verdad
```

Eso satisface directamente:

> Como cliente quiero que los productos que agrego al Carro antes de iniciar sesión permanezcan en el carro después de iniciar sesión.

La regla de merge puede ser extremadamente simple:

```text
mismo ProductId:
cantidad final = cantidad local + cantidad servidor
```

sujeta a disponibilidad de stock.

No necesitamos resolver conflictos distribuidos ni sincronización offline.

---

# 4. Decisión: no reservar stock al agregar al carrito

El carrito es intención de compra, no reserva.

Por tanto:

```text
Add to cart
    ≠
Reduce stock
```

El stock sólo cambia cuando se confirma la compra.

Eso evita bloquear inventario por usuarios que abandonan carros.

---

# 5. Decisión: checkout crea una Order y descuenta stock

El proceso importante será:

```text
Cart
 ↓
Checkout
 ↓
Validar stock
 ↓
Crear Order
 ↓
Crear OrderItems
 ↓
Descontar stock
 ↓
Vaciar Cart
 ↓
Confirmación
```

Todo eso debe ejecutarse como una sola operación transaccional backend.

Algo conceptualmente como:

```java
@Transactional
placeOrder(...)
```

Si falla cualquier elemento:

```text
no Order
no descuento parcial
no cart vacío
```

Esto es un excelente candidato para el caso de uso central de H1/H3.

---

# 6. Decisión: no implementar pago real

La historia dice:

> comprar los productos

y tu descripción humorística habla de pago inmediato.

Pero salvo que haya un requisito académico específico de integración de pagos, no metería:

* Stripe;
* Mercado Pago;
* Webpay;
* mocks complejos de payment gateways.

Definiría la compra como:

```text
checkout confirmado
→ pago simulado aprobado
→ Order CONFIRMED
```

Podemos tener conceptualmente:

```java
OrderStatus.CONFIRMED
```

y punto.

Si después quieres dejar espacio arquitectónico para un `PaymentGateway`, se puede, pero **no lo implementaría ahora salvo que aporte a H3**.

---

# 7. Decisión: despacho también será sólo estado

La descripción dice que después pasa a despacho.

No necesitamos construir logística.

Basta con que una orden pueda conceptualmente pasar por:

```text
CONFIRMED
→ DISPATCHED
```

pero incluso `DISPATCHED` puede quedar fuera del flujo principal si no existe historia de usuario para administración de despacho.

La compra termina exitosamente cuando:

```text
Order = CONFIRMED
```

y el frontend muestra la confirmación.

---

# 8. Decisión: autenticación necesaria sólo donde agrega valor

El catálogo será público:

```http
GET /products
GET /products/{id}
GET /categories
```

Agregar productos al carro anónimo tampoco requiere backend.

Para operar carrito persistente y comprar:

```text
login requerido
```

Así el flujo de usuario queda natural:

```text
Browse anonymously
        ↓
Add to local cart
        ↓
Checkout
        ↓
Not authenticated?
        ↓
Login/Register
        ↓
Merge carts
        ↓
Checkout
```

Esto satisface limpiamente la historia 4.

---

# 9. Decisión: JWT sigue

El backend ya tiene JWT trabajado y no hay una razón pragmática para reemplazarlo.

Por tanto:

```text
POST /auth/login
        ↓
JWT
        ↓
frontend API client
        ↓
Authorization: Bearer ...
```

No reabriría ahora la discusión JWT vs session cookie.

Podremos revisar **dónde almacenar el token** cuando lleguemos a integración, pero no bloquearía el diseño del dominio por eso.

---

# 10. Decisión: Product tiene imagen, pero no implementamos almacenamiento de archivos

Ésta requiere acotar bien la historia:

> agregar y eliminar productos del catálogo, incluyendo una imagen ilustrativa.

Lo más sencillo y suficiente es que:

```text
Product
 └─ imageUrl
```

El administrador proporciona una URL de imagen.

No implementaría ahora:

```text
multipart upload
filesystem
S3
Cloudinary
image resizing
CDN
```

salvo que la rúbrica lo exija.

El backend persiste:

```text
imageUrl
```

y el frontend la muestra.

Esto cumple funcionalmente el requisito con un costo muy inferior.

---

# 11. Decisión: administración sólo backend/API + UI mínima

Tenemos tres operaciones administrativas:

```text
Create Product
Update Product
Delete Product
```

y modificación incluye:

```text
name
description
price
category
stock
imageUrl
```

Yo mantendría endpoints REST convencionales:

```http
POST   /api/v1/products
PUT    /api/v1/products/{id}
DELETE /api/v1/products/{id}
```

protegidos por:

```text
ROLE_ADMIN
```

El frontend necesitará una UI administrativa sencilla, no un panel sofisticado.

---

# 12. El dominio mínimo de Product

No llenaría esto de Value Objects.

Propongo:

```text
Product
 ├─ ProductId
 ├─ name
 ├─ description
 ├─ Money price
 ├─ CategoryId
 ├─ stock
 └─ imageUrl
```

Value Objects que sí veo justificados:

```text
Money
Quantity
```

Posiblemente:

```text
ProductId
```

porque Sisacad demuestra el patrón y nos ayuda con H3.

No veo necesidad inmediata de:

```text
ProductName
ProductDescription
ImageUrl
CategoryName
```

como Value Objects independientes.

---

# 13. Cart

Modelo mínimo:

```text
Cart
 ├─ UserId
 └─ CartItem*
       ├─ ProductId
       └─ Quantity
```

Reglas:

```text
Quantity > 0

add same product
→ increase quantity

remove product
→ item disappears

quantity 0
→ remove item
```

No copiaría precio dentro de `CartItem` como fuente de verdad.

El precio válido se obtiene del catálogo al hacer checkout.

---

# 14. Order

Aquí sí conviene guardar snapshot.

```text
Order
 ├─ OrderId
 ├─ UserId
 ├─ ShippingAddress
 ├─ status
 ├─ total
 └─ OrderItem*
       ├─ ProductId
       ├─ productName
       ├─ unitPrice
       └─ quantity
```

¿Por qué copiar nombre y precio?

Porque si mañana el administrador cambia:

```text
Tazón Linux
$7.990
```

a:

```text
Tazón Linux Deluxe
$12.990
```

una compra histórica tiene que seguir diciendo que se compró:

```text
Tazón Linux
$7.990
```

Esto sí es una decisión de dominio que vale la pena hacer correctamente.

---

# 15. Contrato funcional mínimo

Con las historias actuales, yo reduciría la API objetivo aproximadamente a esto:

### Catálogo

```http
GET /api/v1/products
GET /api/v1/products/{id}
GET /api/v1/categories
```

### Autenticación

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### Carrito autenticado

```http
GET    /api/v1/cart
POST   /api/v1/cart/items
PUT    /api/v1/cart/items/{productId}
DELETE /api/v1/cart/items/{productId}
```

y una operación específica para la transición anónimo → autenticado:

```http
POST /api/v1/cart/merge
```

Payload:

```json
{
  "items": [
    {
      "productId": 42,
      "quantity": 2
    }
  ]
}
```

### Compra

Yo favorecería:

```http
POST /api/v1/orders
```

en vez de crear un recurso `/checkout`.

Semánticamente el resultado del checkout es una orden.

Request:

```json
{
  "shippingAddress": {
    "street": "...",
    "city": "...",
    "postalCode": "..."
  }
}
```

Los productos **no necesitan venir otra vez desde el cliente** si ya tenemos carrito servidor.

Backend:

```text
current authenticated user
        ↓
current cart
        ↓
PlaceOrder
```

Response:

```json
{
  "id": 123,
  "status": "CONFIRMED",
  "total": 39970
}
```

### Administración

```http
POST   /api/v1/products
PUT    /api/v1/products/{id}
DELETE /api/v1/products/{id}
```

Eso es suficiente.

---

# 16. Esto define nuestro verdadero flujo E2E

```text
VISITANTE
   │
   ├─ browse catalog
   │
   ├─ add product
   │
   ▼
localStorage cart
   │
   ▼
checkout
   │
not authenticated
   │
   ▼
login/register
   │
   ▼
POST /cart/merge
   │
   ▼
backend cart
   │
   ▼
POST /orders
   │
   ├─ validate stock
   ├─ create order
   ├─ decrement inventory
   └─ clear cart
   │
   ▼
Order CONFIRMED
   │
   ▼
UI purchase confirmation
```

Eso cubre prácticamente todas tus historias de cliente.

Y paralelamente:

```text
ADMIN
  │
  ▼
login
  │
  ├─ POST Product
  ├─ PUT Product
  └─ DELETE Product
          │
          ▼
      PostgreSQL
          │
          ▼
       Catalog
```

---

# 17. Ahora sí podemos definir los slices

Yo usaría sólo cuatro:

### Slice 1 — Catalog

```text
Product
Category
Stock
Admin CRUD
```

### Slice 2 — Cart

```text
anonymous local cart
authenticated server cart
merge
```

### Slice 3 — Ordering

```text
checkout
order
stock deduction
confirmation
```

### Slice 4 — Identity

```text
register
login
JWT
roles
```

No agregaría más dominios.

---

# 18. Orden realista de implementación

Aquí sería pragmático y **no empezaría por Clean Architecture completa**.

### Etapa 1 — Contrato y catálogo

Primero lograr:

```text
Frontend
   ↓
GET /products
   ↓
Backend
```

Eliminar el catálogo mock como fuente principal.

Luego:

```text
GET /categories
```

Esto nos entrega la primera integración real rápidamente.

---

### Etapa 2 — Login

Conectar:

```text
frontend login
     ↓
POST /auth/login
     ↓
JWT
```

Todavía sin carrito servidor.

---

### Etapa 3 — Carrito

Conservar el carrito local existente.

Implementar/conectar backend cart.

Después:

```text
login
 ↓
merge local cart
 ↓
server cart
```

Una vez autenticado:

```text
server owns cart
```

---

### Etapa 4 — Compra

Implementar la historia más importante:

```text
POST /orders
```

con transacción:

```text
stock validation
+
stock deduction
+
order creation
+
cart clear
```

Y frontend:

```text
submitting
→ success
→ error
```

que ya está bien construido gracias a H2.

---

### Etapa 5 — Administración

Conectar o construir UI mínima para:

```text
create
edit
delete
```

de Product.

---

### Etapa 6 — Refactor H1/H3

Sólo cuando los principales flujos estén claros:

```text
Catalog
Cart
Ordering
Identity
```

refactorizamos internamente siguiendo el patrón Sisacad:

```text
Domain
Application
Ports
Adapters
```

pero **slice por slice**.

Empezaría por Catalog.

---

### Etapa 7 — endurecimiento

Finalmente:

```text
ArchUnit
coverage
OpenAPI final
contract tests
E2E
```

y ejecutamos nuevamente la rúbrica completa.

---

# 19. Una corrección importante respecto de nuestra estrategia anterior

Ahora que tenemos requisitos concretos, **no creo que debamos diseñar primero toda la Clean Architecture final y luego conectar**.

Tampoco deberíamos simplemente conectar todo tal como está.

El punto intermedio razonable es:

```text
definir comportamiento
        ↓
definir contrato mínimo
        ↓
integrar happy path
        ↓
refactor arquitectónico controlado
        ↓
endurecer
```

Así obtenemos feedback real temprano sin convertir el legacy actual en arquitectura definitiva.

---

# 20. Decisiones que considero cerradas

Salvo que aparezca un requisito que las contradiga, yo daría por cerradas estas:

| Tema                    | Decisión                      |
| ----------------------- | ----------------------------- |
| Catálogo                | Backend es fuente de verdad   |
| Stock                   | Backend es fuente de verdad   |
| Carrito anónimo         | `localStorage`                |
| Carrito autenticado     | Backend                       |
| Login con carrito local | Merge hacia backend           |
| Reserva de stock        | No                            |
| Descuento de stock      | Al confirmar Order            |
| Checkout                | Crea `Order`                  |
| Transacción checkout    | Atómica                       |
| Pago                    | Simulado                      |
| Despacho                | Estado, no subsistema         |
| Auth                    | JWT existente                 |
| Imagen                  | URL persistida                |
| Product admin           | CRUD REST                     |
| Contrato                | OpenAPI                       |
| Tipos frontend          | Adaptados/generados desde API |
| Arquitectura backend    | Patrón Sisacad H1/H3          |
| Estrategia refactor     | Vertical slices               |
| Primer slice            | Catalog                       |
| Big-bang rewrite        | No                            |

Con esto ya tenemos suficiente definición para **dejar la discusión conceptual y empezar la planificación técnica concreta**. El próximo paso útil es hacer una matriz de **“estado actual frontend / estado actual backend / estado objetivo / cambio necesario”** para Catalog, Cart, Ordering e Identity, y de ahí sacar tareas implementables en orden.
