# Unicorn't Store — Admin

Panel de administración web para el ecommerce **Unicorn't Store**.  
Permite gestionar el catálogo de productos mediante operaciones CRUD.

Repositorio: **https://github.com/keber/unicornt-store-springboot**

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

## Base de datos

Los scripts SQL se encuentran en el repositorio [ecommerce-db-m3](https://github.com/keber/ecommerce-db-m3).

```bash
mysql -u root -p < ecommerce-db-m3/mysql/sql/schema.sql
mysql -u root -p unicornt_store < ecommerce-db-m3/mysql/sql/seed.sql
```

---

## Configuración

El archivo `src/main/resources/application.properties` centraliza la configuración de la aplicación.

### Datasource

El datasource **no** contiene credenciales en código fuente. Spring Boot resuelve las propiedades automáticamente a partir de las variables de entorno `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` (ver sección [Variables de entorno](#variables-de-entorno)).

Las propiedades de conexión específicas de cada motor se definen en los perfiles (ver [Perfiles Spring](#perfiles-spring)).

### Perfiles Spring

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

### JPA

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

- `ddl-auto=update` permite que Hibernate cree y actualice automáticamente las tablas de seguridad (`users`, `roles`, `users_roles`) sin necesidad de scripts DDL adicionales.
- Las tablas de productos, categorías y tipos se crean mediante los scripts SQL del repositorio [ecommerce-db-m3](https://github.com/keber/ecommerce-db-m3).

### Vistas (Thymeleaf)

```properties
spring.thymeleaf.cache=false
```

- **Motor de plantillas:** Thymeleaf 3, integrado vía `spring-boot-starter-thymeleaf`.
- **Ubicación de templates:** `src/main/resources/templates/` (convención por defecto de Spring Boot, no requiere configuración explícita).
- **Fragmentos reutilizables:** `layout/header.html` y `layout/footer.html` se insertan en cada página con `th:replace`.
- **Seguridad en vistas:** La dependencia `thymeleaf-extras-springsecurity6` habilita atributos como `sec:authorize="hasRole('ADMIN')"` y `sec:authentication="name"` para mostrar u ocultar elementos según el rol del usuario autenticado.
- **Cache desactivado** en desarrollo para ver cambios sin reiniciar. En producción se recomienda `spring.thymeleaf.cache=true`.

---

## Compilación y empaquetado

```bash
mvn clean package -DskipTests
```

El JAR ejecutable se genera en:

```
target/unicornt-store.jar
```

### Ejecución local (sin Docker)

Requiere las variables de entorno definidas previamente:

```bash
java -jar target/unicornt-store.jar
```

O directamente con Maven:

```bash
mvn spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`.

---

## Despliegue con Docker

### 1. Crear el archivo `.env`

```bash
cp .env-template .env
# Editar .env con los valores reales de conexión a BD
```

Ejemplo de `.env` para desarrollo local (MySQL en el host):

```env
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/unicornt_store?useSSL=false&serverTimezone=America/Santiago&characterEncoding=UTF-8&useUnicode=true
SPRING_DATASOURCE_USERNAME=tu_usuario
SPRING_DATASOURCE_PASSWORD=tu_password
```

> **Nota:** Desde Docker, `localhost` apunta al contenedor, no al host. Usa `host.docker.internal` para conectarte a servicios del host (MySQL, PostgreSQL).

### 2. Compilar y levantar

```bash
mvn clean package -DskipTests
docker compose --env-file .env up --build -d
```

La aplicación estará disponible en `http://localhost:8080`.

### 3. Ver logs

```bash
docker compose logs -f
```

### 4. Detener

```bash
docker compose down
```

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
ARG JAR_FILE=target/unicornt-store.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

### docker-compose.yml

```yaml
services:
  unicornt-store:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}
      SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
    restart: always
```

Las variables se inyectan desde el archivo `.env` y se pasan al contenedor como variables de entorno.

---

## Seguridad

La aplicación usa **Spring Security** con autenticación por formulario y autorización basada en roles.

### Roles

| Rol | Acceso |
|-----|--------|
| `ADMIN` | Panel de administración (`/admin/**`) + Catálogo (`/catalog`) |
| `CLIENT` | Catálogo público (`/catalog`) |

### Usuarios de prueba (seed automático)

Al iniciar la aplicación, se crean automáticamente si no existen:

| Email | Contraseña | Rol |
|-------|------------|-----|
| `admin@unicornt.cl` | `admin123` | ADMIN |
| `cliente@unicornt.cl` | `cliente123` | CLIENT |

> **Producción:** El seed está controlado por la propiedad `app.seed.enabled`. Por defecto es `true` (se ejecuta). Para desactivarlo en producción, define la variable de entorno `APP_SEED_ENABLED=false` o agrega `app.seed.enabled=false` en `application.properties`. Esto evita que existan usuarios con contraseñas conocidas en el servidor.

### Páginas públicas

`/login` y `/register` son accesibles sin autenticación.

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

## Estructura del proyecto

```
unicornt-store-springboot/
├── pom.xml                              # Spring Boot parent, packaging JAR
├── Dockerfile                           # Imagen Docker (eclipse-temurin:21)
├── docker-compose.yml                   # Orquestación con Docker Compose
├── .env-template                        # Plantilla de variables de entorno
├── src/
│   └── main/
│       ├── java/com/unicornt/store/
│       │   ├── StoreApplication.java    # Punto de entrada + seed de datos
│       │   ├── config/
│       │   │   ├── SecurityConfig.java   # Filtros, BCrypt, rutas protegidas
│       │   │   └── CustomAuthSuccessHandler.java
│       │   ├── model/
│       │   │   ├── Product.java         # @Entity
│       │   │   ├── Category.java        # @Entity
│       │   │   ├── ProductType.java     # @Entity
│       │   │   ├── User.java            # @Entity (autenticación)
│       │   │   └── Role.java            # @Entity (ROLE_ADMIN, ROLE_CLIENT)
│       │   ├── mapper/
│       │   │   ├── ProductRowMapper.java
│       │   │   ├── CategoryRowMapper.java
│       │   │   └── ProductTypeRowMapper.java
│       │   ├── dao/                     # Acceso a datos con JdbcTemplate
│       │   │   ├── ProductDAO.java
│       │   │   ├── CategoryDAO.java
│       │   │   └── ProductTypeDAO.java
│       │   ├── repository/              # Spring Data JPA
│       │   │   ├── UserRepository.java
│       │   │   ├── RoleRepository.java
│       │   │   ├── ProductRepository.java
│       │   │   ├── CategoryRepository.java
│       │   │   └── ProductTypeRepository.java
│       │   ├── dto/
│       │   │   └── RegisterRequest.java
│       │   ├── service/
│       │   │   ├── ProductService.java
│       │   │   ├── ProductServiceImpl.java
│       │   │   ├── UserService.java
│       │   │   ├── UserServiceImpl.java
│       │   │   └── CustomUserDetailsService.java
│       │   └── controller/
│       │       ├── AdminProductController.java  # @PreAuthorize(ADMIN)
│       │       ├── CatalogController.java       # Catálogo público
│       │       ├── AuthController.java          # Login + Registro
│       │       ├── CustomErrorController.java
│       │       └── HomeController.java
│       ├── resources/
│       │   ├── application.properties           # Config base (JPA, Thymeleaf)
│       │   ├── application-dev.properties       # Perfil dev (MySQL local)
│       │   ├── application-prod.properties      # Perfil prod (PostgreSQL/Supabase)
│       │   ├── templates/               # Thymeleaf
│       │   │   ├── layout/
│       │   │   │   ├── header.html      # Navbar con sec:authorize
│       │   │   │   └── footer.html      # Footer con sec:authorize
│       │   │   ├── login.html
│       │   │   ├── register.html
│       │   │   ├── error/
│       │   │   │   └── access-denied.html
│       │   │   ├── catalog/
│       │   │   │   └── product-list.html
│       │   │   └── admin/
│       │   │       ├── product-list.html
│       │   │       └── product-form.html
│       │   └── static/
│       │       └── assets/css/
│       │           └── admin.css
│   └── test/
│       ├── java/com/unicornt/store/
│       │   ├── service/
│       │   │   └── UserServiceTest.java         # Tests unitarios (Mockito)
│       │   └── controller/
│       │       └── SecurityIntegrationTest.java  # Tests de integración (MockMvc)
│       └── resources/
│           └── application.properties            # H2 in-memory para tests
└── target/
    └── unicornt-store.jar
```

---

## Tests

```bash
mvn clean test
```

| Clase | Tipo | Tests | Cobertura |
|-------|------|-------|-----------|
| `UserServiceTest` | Unitario (Mockito) | 4 | Registro, rol no encontrado, email exists |
| `SecurityIntegrationTest` | Integración (MockMvc + H2) | 11 | Acceso público, roles CLIENT/ADMIN, registro, validaciones |

Los tests de integración usan **H2 en memoria** y no requieren MySQL.

> **Nota:** Los tests usan `@TestPropertySource` para forzar la conexión a H2. Esto es necesario porque las variables de entorno `SPRING_DATASOURCE_*` (usadas en producción/desarrollo) tienen prioridad sobre `application.properties` de test. Sin `@TestPropertySource`, si tienes esas variables definidas en tu terminal, los tests intentarían conectarse a MySQL en vez de H2.

---

## Proyectos relacionados

| Repositorio | Descripción |
|-------------|-------------|
| [unicornt-store-frontend](https://github.com/keber/unicornt-store-frontend) | Catálogo público (HTML/CSS/JS) |
| [ecommerce-db-m3](https://github.com/keber/ecommerce-db-m3) | Scripts SQL (schema + seed) para MySQL y PostgreSQL |
