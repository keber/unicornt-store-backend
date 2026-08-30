# Security

The application uses **Spring Security** with form-based authentication and role-based authorization.

## Roles

| Role | Access |
|-----|--------|
| `ADMIN` | Admin panel (`/admin/**`) + Catalog (`/catalog`) |
| `CLIENT` | Public catalog (`/catalog`) |

---

## Test users (automatic seed)

On application startup, they are created automatically if they do not exist:

| Email | Password | Role |
|-------|------------|-----|
| `admin@unicornt.cl` | `admin123` | ADMIN |
| `cliente@unicornt.cl` | `cliente123` | CLIENT |

> **Production:** The seed is controlled by the `app.seed.enabled` property. It defaults to `true` (it runs). To disable it in production, set the environment variable `APP_SEED_ENABLED=false` or add `app.seed.enabled=false` to `application.properties`. This prevents users with known passwords from existing on the server.

---

## Public pages

`/login` and `/register` are accessible without authentication.

---

## Authentication flow

1. The user accesses any protected route → redirected to `/login`.
2. Spring Security validates the credentials against the database (BCrypt).
3. Based on the role, `CustomAuthSuccessHandler` redirects:
   - **ADMIN** → `/admin/products`
   - **CLIENT** → `/catalog`
4. The navbar shows different options based on the role (`sec:authorize`).

---

## Registration

- Any visitor can register at `/register`.
- New users automatically receive the `CLIENT` role.
- Passwords are stored hashed with BCrypt.
