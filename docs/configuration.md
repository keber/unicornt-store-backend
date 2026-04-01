# Configuración

## Requisitos previos

**Ejecución local (sin Docker):**

- JDK 21+
- Maven 3.8+
- MySQL 8+ o PostgreSQL 15+

**Ejecución con Docker:**

- Docker y Docker Compose
- MySQL 8+ o PostgreSQL 15+ (puede estar en el host o en un servicio externo)

---

## Variables de entorno

Las credenciales **nunca se almacenan en el código fuente**. La aplicación usa la nomenclatura estándar de Spring Boot para que el datasource se configure automáticamente a partir de las variables del sistema operativo.

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Perfil activo | `dev` (MySQL) o `prod` (PostgreSQL) |
| `SPRING_DATASOURCE_URL` | JDBC URL completa | `jdbc:mysql://localhost:3306/unicornt_store?...` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos | `unicornt-store-admin` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña del usuario | `********` |

> Spring Boot mapea automáticamente `SPRING_DATASOURCE_URL` → `spring.datasource.url`, etc. No se requiere ninguna configuración extra.

### Archivo `.env-template`

El repositorio incluye un archivo `.env-template` con la estructura de variables necesarias. Para usarlo:

```bash
cp .env-template .env
# Editar .env con los valores reales
```

El archivo `.env` está en `.gitignore` y **nunca se sube al repositorio**. Docker Compose lo lee automáticamente con `--env-file .env`.

---

## Perfiles Spring

La aplicación utiliza perfiles para separar la configuración por entorno:

| Perfil | Archivo | BD | Uso |
|--------|---------|----|---------|
| `dev` | `application-dev.properties` | MySQL local | Desarrollo |
| `prod` | `application-prod.properties` | PostgreSQL (Supabase) | Producción |

El perfil activo se define con la variable `SPRING_PROFILES_ACTIVE`:

```bash
# Desarrollo (MySQL)
SPRING_PROFILES_ACTIVE=dev

# Producción (PostgreSQL / Supabase)
SPRING_PROFILES_ACTIVE=prod
```

El perfil `prod` incluye configuración adicional para Supabase:

```properties
spring.datasource.hikari.connection-init-sql=SET search_path TO unicornt_store, public
spring.jpa.properties.hibernate.default_schema=unicornt_store
```

---

## Datasource

El datasource **no** contiene credenciales en código fuente. Spring Boot resuelve las propiedades automáticamente a partir de las variables de entorno `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` (ver sección [Variables de entorno](#variables-de-entorno)).

Las propiedades de conexión específicas de cada motor se definen en los perfiles (ver [Perfiles Spring](#perfiles-spring)).

---

## JPA

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

- `ddl-auto=update` permite que Hibernate cree y actualice automáticamente las tablas de seguridad (`users`, `roles`, `users_roles`) sin necesidad de scripts DDL adicionales.
- Las tablas de productos, categorías y tipos se crean mediante los scripts SQL del repositorio [ecommerce-db-m3](https://github.com/keber/ecommerce-db-m3).

---

## Vistas (Thymeleaf)

```properties
spring.thymeleaf.cache=false
```

- **Motor de plantillas:** Thymeleaf 3, integrado vía `spring-boot-starter-thymeleaf`.
- **Ubicación de templates:** `src/main/resources/templates/` (convención por defecto de Spring Boot, no requiere configuración explícita).
- **Fragmentos reutilizables:** `layout/header.html` y `layout/footer.html` se insertan en cada página con `th:replace`.
- **Seguridad en vistas:** La dependencia `thymeleaf-extras-springsecurity6` habilita atributos como `sec:authorize="hasRole('ADMIN')"` y `sec:authentication="name"` para mostrar u ocultar elementos según el rol del usuario autenticado.
- **Cache desactivado** en desarrollo para ver cambios sin reiniciar. En producción se recomienda `spring.thymeleaf.cache=true`.

---

## Base de datos

Los scripts SQL se encuentran en el repositorio [ecommerce-db-m3](https://github.com/keber/ecommerce-db-m3).

```bash
mysql -u root -p < ecommerce-db-m3/mysql/sql/schema.sql
mysql -u root -p unicornt_store < ecommerce-db-m3/mysql/sql/seed.sql
```
