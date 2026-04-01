# Unicorn't Store

Plataforma e-commerce con catálogo de productos, autenticación de usuarios y panel de administración.  
Desplegada con Docker sobre VPS + PostgreSQL en Supabase.

**Demo:** https://unicornt-store.keber.dev  
**Repositorio:** https://github.com/keber/unicornt-store-springboot

---

## Stack

| Capa | Tecnología |
|------|------------|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Web | Spring MVC |
| Vistas | Thymeleaf 3 |
| Seguridad | Spring Security 7 (roles ADMIN / CLIENT, BCrypt) |
| Persistencia | Spring JdbcTemplate (CRUD) · Spring Data JPA (usuarios/roles) |
| Build | Maven 3.x · JAR ejecutable |
| Contenedores | Docker · Docker Compose |
| Perfiles | `dev` (MySQL local) · `prod` (PostgreSQL / Supabase) |
| BD soportadas | MySQL 8+ · PostgreSQL 15+ |
| Tests | JUnit 5 · Mockito · MockMvc · H2 (in-memory) |
| UI | Bootstrap 5.3.8 · Font Awesome 6.5.1 |

---

## Funcionalidades

- **Registro e inicio de sesión** — Formularios públicos con validación. Contraseñas hasheadas con BCrypt.
- **Catálogo de productos** — Vista de grilla con búsqueda por nombre, filtro por categoría y paginación.
- **Panel de administración** — CRUD completo de productos (crear, editar, eliminar) con vista previa de imagen.
- **Roles y autorización** — `ADMIN` gestiona productos; `CLIENT` navega el catálogo. Redirección automática según rol.
- **Seed automático** — Usuarios de prueba creados al iniciar (`app.seed.enabled` para desactivar en producción).
- **Perfiles de entorno** — `dev` (MySQL local) y `prod` (PostgreSQL / Supabase) con un solo cambio de variable.
- **Despliegue con Docker** — Imagen ligera (Alpine), Docker Compose con variables desde `.env`.

---

## Inicio rápido

```bash
# 1. Compilar
mvn clean package -DskipTests

# 2. Configurar variables
cp .env-template .env
# Editar .env con los datos de conexión a BD

# 3. Levantar con Docker
docker compose --env-file .env up --build -d

# 4. Abrir en el navegador
# http://localhost:8080
```

Para ejecución local sin Docker, ver [docs/deployment.md](docs/deployment.md#ejecución-local-sin-docker).

---

## Rutas principales

| Método | URL | Acceso | Descripción |
|--------|-----|--------|-------------|
| `GET` | `/` | Autenticado | Redirige según rol (ADMIN → `/admin/products`, CLIENT → `/catalog`) |
| `GET` | `/login` | Público | Formulario de inicio de sesión |
| `GET/POST` | `/register` | Público | Registro de nuevo usuario (rol CLIENT) |
| `GET` | `/catalog` | Autenticado | Catálogo de productos (solo lectura) |
| `GET` | `/admin/products` | ADMIN | Listado de productos (búsqueda + filtro) |
| `GET` | `/admin/products/new` | ADMIN | Formulario de creación |
| `GET` | `/admin/products/edit?id={id}` | ADMIN | Formulario de edición |
| `POST` | `/admin/products` | ADMIN | Crear producto |
| `POST` | `/admin/products/update` | ADMIN | Actualizar producto |
| `POST` | `/admin/products/delete` | ADMIN | Eliminar producto |

---

## Tests

```bash
mvn clean test
```

| Clase | Tipo | Tests |
|-------|------|-------|
| `UserServiceTest` | Unitario (Mockito) | 4 |
| `SecurityIntegrationTest` | Integración (MockMvc + H2) | 11 |

---

## Documentación

| Documento | Contenido |
|-----------|-----------|
| [docs/configuration.md](docs/configuration.md) | Requisitos previos, variables de entorno, perfiles Spring, datasource, JPA, Thymeleaf |
| [docs/deployment.md](docs/deployment.md) | Compilación, ejecución local, Docker y Docker Compose |
| [docs/security.md](docs/security.md) | Roles, usuarios de prueba, flujo de autenticación, registro |
| [docs/development.md](docs/development.md) | Estructura del proyecto, tests |

---

## Proyectos relacionados

| Repositorio | Descripción |
|-------------|-------------|
| [unicornt-store-frontend](https://github.com/keber/unicornt-store-frontend) | Catálogo público (HTML/CSS/JS) |
| [ecommerce-db-m3](https://github.com/keber/ecommerce-db-m3) | Scripts SQL (schema + seed) para MySQL y PostgreSQL |
