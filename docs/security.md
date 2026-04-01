# Seguridad

La aplicación usa **Spring Security** con autenticación por formulario y autorización basada en roles.

## Roles

| Rol | Acceso |
|-----|--------|
| `ADMIN` | Panel de administración (`/admin/**`) + Catálogo (`/catalog`) |
| `CLIENT` | Catálogo público (`/catalog`) |

---

## Usuarios de prueba (seed automático)

Al iniciar la aplicación, se crean automáticamente si no existen:

| Email | Contraseña | Rol |
|-------|------------|-----|
| `admin@unicornt.cl` | `admin123` | ADMIN |
| `cliente@unicornt.cl` | `cliente123` | CLIENT |

> **Producción:** El seed está controlado por la propiedad `app.seed.enabled`. Por defecto es `true` (se ejecuta). Para desactivarlo en producción, define la variable de entorno `APP_SEED_ENABLED=false` o agrega `app.seed.enabled=false` en `application.properties`. Esto evita que existan usuarios con contraseñas conocidas en el servidor.

---

## Páginas públicas

`/login` y `/register` son accesibles sin autenticación.

---

## Flujo de autenticación

1. El usuario accede a cualquier ruta protegida → redirigido a `/login`.
2. Spring Security valida credenciales contra la base de datos (BCrypt).
3. Según el rol, `CustomAuthSuccessHandler` redirige:
   - **ADMIN** → `/admin/products`
   - **CLIENT** → `/catalog`
4. El navbar muestra opciones distintas según el rol (`sec:authorize`).

---

## Registro

- Cualquier visitante puede registrarse en `/register`.
- Los nuevos usuarios reciben automáticamente el rol `CLIENT`.
- Las contraseñas se almacenan hasheadas con BCrypt.
