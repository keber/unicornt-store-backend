# Security

The API is stateless: every request is authenticated with a JWT bearer token,
there is no session and no cookie. Passwords are hashed with BCrypt.

## Roles

| Role | Access |
|------|--------|
| `ROLE_ADMIN` | Everything `ROLE_USER` can do, plus creating/updating/deleting products and creating categories |
| `ROLE_USER` | Browsing the catalog, and managing their own cart, addresses and orders |

`hasRole("ADMIN")` is used in `@PreAuthorize`/request matchers; Spring Security
strips and re-adds the `ROLE_` prefix internally, matching a
`SimpleGrantedAuthority("ROLE_ADMIN")` authority carried by the JWT `roles` claim.

## Authentication flow

1. `POST /api/v1/auth/register` — creates an account with `ROLE_USER`, password
   hashed with BCrypt. A duplicate email returns `409 RESOURCE_CONFLICT`.
2. `POST /api/v1/auth/login` — authenticates against the stored credentials and
   returns `{ "token": "...", "expiresIn": 3600000 }`. Wrong credentials return
   `401 UNAUTHENTICATED`.
3. Every subsequent request carries `Authorization: Bearer <token>`. A missing or
   invalid token on a protected route returns `401 UNAUTHENTICATED`; a valid
   token without the required role returns `403 ACCESS_DENIED`. Both use the
   same `ErrorResponse` shape as every other error in the API.
4. `GET /api/v1/auth/me` returns the identity Spring Security resolved from the
   token.

## Request matchers and method security

`infrastructure/security/SecurityConfig` defines coarse, path-based rules:

- `/api/v1/auth/**`, `/swagger-ui/**`, `/api-docs/**` — public.
- `GET /api/v1/products/**`, `GET /api/v1/categories/**` — public.
- Any write on `/api/v1/products/**` or `/api/v1/categories/**` — `ROLE_ADMIN`.
- Everything else (`/api/v1/cart/**`, `/api/v1/orders/**`, `/api/v1/addresses/**`) —
  any authenticated user.

Resource ownership (a user can only see their own cart, addresses and orders) is
not a path-shaped rule: it is enforced in the service layer, which resolves the
caller's identity from `Authentication.getName()` and never from a path or body
parameter, and raises `ResourceNotFoundException` (`404`, never `403`) when a
resource id does not belong to the caller.

## Accounts

The application ships no accounts and no credentials. Reference data — the
`ROLE_USER` and `ROLE_ADMIN` rows, product types, categories and sample
products — is seeded by the versioned migration `V2__seed_reference_data.sql`.

Regular accounts are created through `POST /api/v1/auth/register`, which always
grants `ROLE_USER`. There is no public way to obtain `ROLE_ADMIN`.

### Bootstrap admin

The first administrator is created by `StoreApplication.bootstrapAdmin`, a
`CommandLineRunner` that exists only when `app.bootstrap-admin.email`
(`APP_BOOTSTRAP_ADMIN_EMAIL`) is set. A default deployment defines neither
variable and creates no account.

| `APP_BOOTSTRAP_ADMIN_EMAIL` | `APP_BOOTSTRAP_ADMIN_PASSWORD` | Result |
|---|---|---|
| unset | — | no account created |
| set | unset / blank | account created; a strong random password is generated and logged **once** on startup for the operator to record and rotate |
| set | set | account created with the supplied password; nothing is logged |

The runner is idempotent: if the address already exists it does nothing, so it
is safe to leave the variables in place across restarts. Once the admin exists,
unset `APP_BOOTSTRAP_ADMIN_PASSWORD` (or remove both variables) so no credential
lingers in the environment.

## Secrets

Every credential — database, JWT signing key, CORS origins — comes from an
environment variable (see [configuration.md](configuration.md)). Generate a
signing key with:

```bash
openssl rand -base64 32
```
