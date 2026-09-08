# Bruno collection — unicornt-store API

Open the folder in [Bruno](https://usebruno.com), pick an environment, run the
requests. `Auth/Login` stashes the JWT into the `token` environment variable
via a post-response script; every authenticated request reads `{{token}}`.

## Environments

| Env | `baseUrl` |
|---|---|
| `local` | `http://localhost:8080` |
| `dev` | `https://api-unicornt-dev.keber.dev` |
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

**Running this against a real environment writes real rows** — a user, a cart,
an order. The address no longer has to be edited by hand: `collection.bru`
generates `smoke-<timestamp>-<salt>@example.com` once per run when `email` is
unset, so re-registering never collides and every run's rows are attributable.
Pass `--env-var email=you@example.com` to pin a specific address.

## In CI

The `api-write-path` job runs exactly this sequence against **dev** after every
dev deploy, which is what keeps the collection from rotting unnoticed:

```bash
cd docs/bruno/unicornt-store
npx --yes @usebruno/cli@4.1.0 run   "Auth/Register.bru" "Auth/Login.bru" "Auth/Me.bru"   "Cart/Add Item.bru" "Cart/Get Cart.bru"   "Orders/Create Order.bru" "Orders/Get Order.bru"   --env dev
```

The requests are named one by one rather than run as folders, because a folder
run follows `seq` and `Cart/Remove Item` (seq 4) would empty the cart before
`Orders` ever runs.

Two things to know about the checks themselves. Assertions are what make the
run meaningful — before this, every request was assertion-free, so `bru run`
reported success no matter what came back, and a 403 or an empty body passed
silently. And `Orders/Get Order` used to request `/orders/1`, an order
belonging to whoever registered first on that environment; it now reads the id
that `Create Order` stashes, which is the only order the run is entitled to.

**A run rewrites the environment file it used.** Bruno persists `setEnvVar` back
to `environments/<env>.bru`, so after running locally that file holds a real
JWT, the generated address and the last `orderId` — `git checkout` it rather
than committing it. This predates the CI work (the `Login` script has always
stashed `token` this way); there are simply more values now. In CI it does not
matter, the checkout is thrown away.

Each run consumes one unit of stock of product 1 on dev. Dev's catalog is the
`V2` seed and its data is disposable, so that is a re-seed when it matters, not
a leak.

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
