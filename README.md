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
| Persistencia | Spring JdbcTemplate (CRUD) · Spring Data JPA (repositorios) |
| Build | Maven 3.x · WAR |
| Servidor | Apache Tomcat 10.1+ (externo) |
| BD soportadas | MySQL 8+ · PostgreSQL 15+ |
| UI | Bootstrap 5.3.8 · Font Awesome 6.5.1 |

---

## Requisitos previos

- JDK 21+
- Maven 3.8+
- Apache Tomcat 10.1+
- MySQL 8+

---

## Variables de entorno

Las credenciales **nunca se almacenan en el código fuente**. La aplicación usa la nomenclatura estándar de Spring Boot para que el datasource se configure automáticamente a partir de las variables del sistema operativo.

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | JDBC URL completa | `jdbc:mysql://localhost:3306/unicornt_store?useSSL=false&serverTimezone=America/Santiago&characterEncoding=UTF-8&useUnicode=true` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos | `unicornt-store-admin` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña del usuario | `********` |

> Spring Boot mapea automáticamente `SPRING_DATASOURCE_URL` → `spring.datasource.url`, etc. No se requiere ninguna configuración extra.

### Definir variables en Windows (sesión actual)

```powershell
$env:SPRING_DATASOURCE_URL      = "jdbc:mysql://localhost:3306/unicornt_store?useSSL=false&serverTimezone=America/Santiago&characterEncoding=UTF-8&useUnicode=true"
$env:SPRING_DATASOURCE_USERNAME = "tu_usuario"
$env:SPRING_DATASOURCE_PASSWORD = "tu_password"
```

### Definir variables en Windows (persistente para el usuario)

```powershell
[System.Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_URL",      "jdbc:mysql://...", "User")
[System.Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_USERNAME", "tu_usuario",       "User")
[System.Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_PASSWORD", "tu_password",      "User")
```

> **Nota:** Las variables de scope `User` no son heredadas automáticamente por procesos iniciados antes de que se definieran (p. ej. una terminal ya abierta). Si ves el error `claims to not accept jdbcUrl`, ejecuta los comandos de sesión actual en la terminal desde la que lanzas Maven o Tomcat.

---

## Base de datos

Los scripts SQL se encuentran en el repositorio [ecommerce-db-m3](https://github.com/keber/ecommerce-db-m3).

```bash
mysql -u root -p < ecommerce-db-m3/mysql/sql/schema.sql
mysql -u root -p unicornt_store < ecommerce-db-m3/mysql/sql/seed.sql
```

---

## Compilación y empaquetado

```bash
mvn clean package -DskipTests
```

El WAR generado se encuentra en:

```
target/unicornt-store-admin.war
```

---

## Despliegue en Tomcat

Copia el WAR al directorio `webapps` de Tomcat. Tomcat lo desplegará automáticamente:

```bash
cp target/unicornt-store-admin.war $CATALINA_HOME/webapps/
```

Las variables de entorno (`SPRING_DATASOURCE_*`) deben estar disponibles para el proceso de Tomcat antes de iniciarlo.

---

## Rutas principales

| Método | URL | Descripción |
|--------|-----|-------------|
| `GET` | `/` | Redirige a `/admin/products` |
| `GET` | `/admin/products` | Listado de productos (con búsqueda y filtro por categoría) |
| `GET` | `/admin/products/new` | Formulario de creación |
| `GET` | `/admin/products/edit?id={id}` | Formulario de edición |
| `POST` | `/admin/products` | Crear producto |
| `POST` | `/admin/products/update` | Actualizar producto |
| `POST` | `/admin/products/delete` | Eliminar producto |

---

## Estructura del proyecto

```
unicornt-store-springboot/
├── pom.xml                              # Spring Boot parent, packaging WAR
├── src/
│   └── main/
│       ├── java/com/unicornt/store/
│       │   ├── StoreApplication.java    # Punto de entrada + SpringBootServletInitializer
│       │   ├── model/
│       │   │   ├── Product.java         # @Entity
│       │   │   ├── Category.java        # @Entity
│       │   │   └── ProductType.java     # @Entity
│       │   ├── mapper/
│       │   │   ├── ProductRowMapper.java
│       │   │   ├── CategoryRowMapper.java
│       │   │   └── ProductTypeRowMapper.java
│       │   ├── dao/                     # Acceso a datos con JdbcTemplate
│       │   │   ├── ProductDAO.java
│       │   │   ├── CategoryDAO.java
│       │   │   └── ProductTypeDAO.java
│       │   ├── repository/              # Spring Data JPA
│       │   │   ├── ProductRepository.java
│       │   │   ├── CategoryRepository.java
│       │   │   └── ProductTypeRepository.java
│       │   ├── service/
│       │   │   ├── ProductService.java
│       │   │   └── ProductServiceImpl.java
│       │   └── controller/
│       │       ├── AdminProductController.java
│       │       └── HomeController.java
│       ├── resources/
│       │   ├── application.properties   # Datasource vía SPRING_DATASOURCE_*
│       │   ├── templates/               # Thymeleaf
│       │   │   ├── layout/
│       │   │   │   ├── header.html      # Fragmentos th:fragment="head|navbar"
│       │   │   │   └── footer.html      # Fragmento th:fragment="footer" + Bootstrap JS
│       │   │   └── admin/
│       │   │       ├── product-list.html
│       │   │       └── product-form.html
│       │   └── static/
│       │       └── assets/css/
│       │           └── admin.css
│       └── webapp/
│           └── (vacío — sin JSPs ni web.xml)
└── target/
    └── unicornt-store-admin.war
```

---

## Proyectos relacionados

| Repositorio | Descripción |
|-------------|-------------|
| [unicornt-store-frontend](https://github.com/keber/unicornt-store-frontend) | Catálogo público (HTML/CSS/JS) |
| [ecommerce-db-m3](https://github.com/keber/ecommerce-db-m3) | Scripts SQL (schema + seed) para MySQL y PostgreSQL |
