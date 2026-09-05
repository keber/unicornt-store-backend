# Bruno collection — unicornt-store API

Open the folder in [Bruno](https://usebruno.com), pick an environment, run the
requests. `Auth/Login` stashes the JWT into the `token` environment variable
via a post-response script; every authenticated request reads `{{token}}`.

## Environments

| Env | `baseUrl` |
|---|---|
| `local` | `http://localhost:8080` |
| `qa` | `https://api-unicornt-qa.keber.cl` |
| `prod` | `https://api-unicornt-store.keber.cl` |

## The write-path contract check

`GET /api/v1/products` proves almost nothing about a database — it is a single
read. The check that matters after a deploy, and the one P8.3 of
[the multi-env plan](../../multi-env-deploy/PLAN.md) requires, is the write
path, in this order:

1. `Auth/Register` → 201
2. `Auth/Login` → 200, stores the token
3. `Auth/Me` → 200
4. `Cart/Add Item` → 201
5. `Cart/Get Cart` → 200
6. `Orders/Create Order` → 201
7. `Orders/Get Order` → 200, shipping address echoed back, cart now empty

Together those exercise identity sequences, a join-table insert
(`users_roles`), and a transaction spanning `orders` + `order_items` + the
cart clear. Against prod that is the only thing proving the Supabase session
pooler handles real transactions, not just selects.

**Running this against `prod` writes real rows** — a user, a cart, an order.
Change the email in `Auth/Register` and `Auth/Login` each time: re-registering
an existing address fails, and a shared address makes the rows impossible to
attribute later. Something like `smoke-<yyyymmdd>@example.com` works.

## Keeping it honest

Every request here is a hand-maintained copy of a contract defined in
`OrderDtos` / `CartDtos` / `AuthDtos`, so it drifts silently when those change
— a stale request fails with a 400 that looks like a server problem. Found
stale on 2026-09-05, all dating from before the V3 migration:

- `Cart/Add Item` and `Cart/Update Item Quantity` sent `qty`; the DTOs take
  `quantity`. `Update Item Quantity` also used `PATCH`, but the route is `PUT`.
- `Orders/Create Order` sent `addressId`; V3 replaced stored addresses with an
  inline `shippingAddress` object.
- An `Addresses/` folder called `/api/v1/addresses`, which no longer exists —
  no controller serves that path. Removed.
- `POST /api/v1/cart/merge` (anonymous cart folded in at login) had no request
  at all. Added.

When you change a DTO, change the matching `.bru` in the same commit.
`src/main/java/com/unicornt/store/infrastructure/web/dto/` is the source of
truth; the OpenAPI UI on dev and qa renders the current shape if in doubt.
