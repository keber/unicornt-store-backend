# Configuration

## Prerequisites

**Local execution (without Docker):**

- JDK 25+
- Maven 3.8+
- MySQL 8+ or PostgreSQL 15+

**Execution with Docker:**

- Docker and Docker Compose
- MySQL 8+ or PostgreSQL 15+ (may run on the host or on an external service)

---

## Environment variables

Credentials are **never stored in the source code**. The application uses the standard Spring Boot naming convention so that the datasource is configured automatically from the operating system variables.

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` (MySQL) or `prod` (PostgreSQL) |
| `SPRING_DATASOURCE_URL` | Full JDBC URL | `jdbc:mysql://localhost:3306/unicornt_store?...` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `unicornt-store-admin` |
| `SPRING_DATASOURCE_PASSWORD` | User password | `********` |

> Spring Boot automatically maps `SPRING_DATASOURCE_URL` → `spring.datasource.url`, etc. No extra configuration is required.

### `.env-template` file

The repository includes an `.env-template` file with the structure of the required variables. To use it:

```bash
cp .env-template .env
# Edit .env with the real values
```

The `.env` file is in `.gitignore` and is **never pushed to the repository**. Docker Compose reads it automatically with `--env-file .env`.

---

## Spring profiles

The application uses profiles to separate configuration per environment:

| Profile | File | DB | Use |
|--------|---------|----|---------|
| `dev` | `application-dev.properties` | Local MySQL | Development |
| `prod` | `application-prod.properties` | PostgreSQL (Supabase) | Production |

The active profile is set with the `SPRING_PROFILES_ACTIVE` variable:

```bash
# Development (MySQL)
SPRING_PROFILES_ACTIVE=dev

# Production (PostgreSQL / Supabase)
SPRING_PROFILES_ACTIVE=prod
```

The `prod` profile includes additional configuration for Supabase:

```properties
spring.datasource.hikari.connection-init-sql=SET search_path TO unicornt_store, public
spring.jpa.properties.hibernate.default_schema=unicornt_store
```

---

## Datasource

The datasource does **not** contain credentials in the source code. Spring Boot resolves the properties automatically from the environment variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` (see the [Environment variables](#environment-variables) section).

The engine-specific connection properties are defined in the profiles (see [Spring profiles](#spring-profiles)).

---

## JPA

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

- `ddl-auto=update` lets Hibernate automatically create and update the security tables (`users`, `roles`, `users_roles`) without needing additional DDL scripts.
- The product, category and type tables are created via the SQL scripts in the [ecommerce-db-m3](https://github.com/keber/ecommerce-db-m3) repository.

---

## Views (Thymeleaf)

```properties
spring.thymeleaf.cache=false
```

- **Template engine:** Thymeleaf 3, integrated via `spring-boot-starter-thymeleaf`.
- **Template location:** `src/main/resources/templates/` (Spring Boot default convention, no explicit configuration needed).
- **Reusable fragments:** `layout/header.html` and `layout/footer.html` are inserted into every page with `th:replace`.
- **Security in views:** The `thymeleaf-extras-springsecurity6` dependency enables attributes such as `sec:authorize="hasRole('ADMIN')"` and `sec:authentication="name"` to show or hide elements based on the authenticated user's role.
- **Cache disabled** in development to see changes without restarting. In production, `spring.thymeleaf.cache=true` is recommended.

---

## Database

The SQL scripts live in the [ecommerce-db-m3](https://github.com/keber/ecommerce-db-m3) repository.

```bash
mysql -u root -p < ecommerce-db-m3/mysql/sql/schema.sql
mysql -u root -p unicornt_store < ecommerce-db-m3/mysql/sql/seed.sql
```
