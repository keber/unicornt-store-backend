# Unicorn't Store

E-commerce platform with a product catalog, user authentication and an admin panel.
Deployed with Docker on a VPS + PostgreSQL on Supabase.

**Demo:** https://unicornt-store.keber.dev
**Repository:** https://github.com/keber/unicornt-store-springboot

---

## Stack

| Layer | Technology |
|------|------------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.8 |
| Web | Spring MVC |
| Views | Thymeleaf 3 |
| Security | Spring Security 7 (ADMIN / CLIENT roles, BCrypt) |
| Persistence | Spring JdbcTemplate (CRUD) · Spring Data JPA (users/roles) |
| Build | Maven 3.x · executable JAR |
| Containers | Docker · Docker Compose |
| Profiles | `dev` (local MySQL) · `prod` (PostgreSQL / Supabase) |
| Supported databases | MySQL 8+ · PostgreSQL 15+ |
| Tests | JUnit 5 · Mockito · MockMvc · H2 (in-memory) |
| UI | Bootstrap 5.3.8 · Font Awesome 6.5.1 |

---

## Features

- **Sign-up and sign-in** — Public forms with validation. Passwords hashed with BCrypt.
- **Product catalog** — Grid view with search by name, filter by category and pagination.
- **Admin panel** — Full product CRUD (create, edit, delete) with image preview.
- **Roles and authorization** — `ADMIN` manages products; `CLIENT` browses the catalog. Automatic redirect based on role.
- **Automatic seed** — Test users created on startup (`app.seed.enabled` to disable in production).
- **Environment profiles** — `dev` (local MySQL) and `prod` (PostgreSQL / Supabase) with a single variable change.
- **Docker deployment** — Lightweight image (Alpine), Docker Compose with variables from `.env`.

---

## Quick start

```bash
# 1. Build
mvn clean package -DskipTests

# 2. Configure variables
cp .env-template .env
# Edit .env with the database connection details

# 3. Start with Docker
docker compose --env-file .env up --build -d

# 4. Open in the browser
# http://localhost:8080
```

For local execution without Docker, see [docs/deployment.md](docs/deployment.md#local-execution-without-docker).

---

## Main routes

| Method | URL | Access | Description |
|--------|-----|--------|-------------|
| `GET` | `/` | Authenticated | Redirects based on role (ADMIN → `/admin/products`, CLIENT → `/catalog`) |
| `GET` | `/login` | Public | Sign-in form |
| `GET/POST` | `/register` | Public | New user registration (CLIENT role) |
| `GET` | `/catalog` | Authenticated | Product catalog (read-only) |
| `GET` | `/admin/products` | ADMIN | Product list (search + filter) |
| `GET` | `/admin/products/new` | ADMIN | Create form |
| `GET` | `/admin/products/edit?id={id}` | ADMIN | Edit form |
| `POST` | `/admin/products` | ADMIN | Create product |
| `POST` | `/admin/products/update` | ADMIN | Update product |
| `POST` | `/admin/products/delete` | ADMIN | Delete product |

---

## Tests

```bash
mvn clean test
```

| Class | Type | Tests |
|-------|------|-------|
| `UserServiceTest` | Unit (Mockito) | 4 |
| `SecurityIntegrationTest` | Integration (MockMvc + H2) | 11 |

---

## Documentation

| Document | Contents |
|-----------|-----------|
| [docs/configuration.md](docs/configuration.md) | Prerequisites, environment variables, Spring profiles, datasource, JPA, Thymeleaf |
| [docs/deployment.md](docs/deployment.md) | Build, local execution, Docker and Docker Compose |
| [docs/security.md](docs/security.md) | Roles, test users, authentication flow, registration |
| [docs/development.md](docs/development.md) | Project structure, tests |

---

## Related projects

| Repository | Description |
|-------------|-------------|
| [unicornt-store-frontend](https://github.com/keber/unicornt-store-frontend) | Public catalog (HTML/CSS/JS) |
| [ecommerce-db-m3](https://github.com/keber/ecommerce-db-m3) | SQL scripts (schema + seed) for MySQL and PostgreSQL |
