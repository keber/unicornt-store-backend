# P4 — Admin product

**Worktree (backend):** `../unicornt-worktrees/fd-admin` (small — verification only)
**Worktree (frontend):** `../unicornt-frontend-worktrees/fd-admin`
**Branch:** `fd/admin`, cut from the tip of `final-delivery` after P3
**Plan:** [PLAN.md](../PLAN.md) §4 Phase 4
**Rubric:** dimension 1 (admin data cycle)
**Follow:** [../slice-recipe.md](../slice-recipe.md) frontend steps

Read [../CONVENTIONS.md](../CONVENTIONS.md). Backend product CRUD already exists from
P1; this phase is the admin UI plus the admin E2E gate. You own the frontend admin
module. On the backend you only add `@PreAuthorize` / a test if P1 left a gap — and
you record any `SecurityConfig` matcher need in the handoff, you do not edit
`SecurityConfig`.

---

## Scope

- Backend: confirm `POST/PUT/DELETE /api/v1/products` are `hasRole('ADMIN')` and
  covered by tests (`CreateProductUseCaseTest`, `UpdateProductUseCaseTest`,
  `DeleteProductUseCaseTest` from P1's migration). Add only what is missing.
- Frontend: a minimal admin product module — list, create/edit form
  (`name, description, price, categoryId, productTypeId, stock, imageUrl`), delete.
  No dashboard, no file upload — `imageUrl` is a text field.
- Auth: the admin views are gated behind an authenticated ADMIN session; a non-admin
  gets a clear "forbidden" message, not a broken screen.

## Frontend (recipe steps 6–8)

- Reuse `api/product.api.ts` from P1; add `createProduct`, `updateProduct`,
  `deleteProduct` via `apiFetch` (they carry the bearer token automatically).
- `views/admin/` (or the repo's convention) with safe DOM, `preventDefault`, the H2
  `submitting / success / error` states.
- Register the admin route via a handoff request (orchestrator owns the router).
- Vitest: create posts the mapped payload; delete removes the row from the list;
  a `403` surfaces an actionable message.

---

## Definition of Done

```bash
mvn -q -DskipTests compile && mvn -q test     # if the backend worktree was touched
npm run build && npm test && npm run lint
```

## Gate

Admin logs in → creates a product (with an `imageUrl`) → the row lands in PostgreSQL
→ the customer catalog shows the new product after a refresh. Then edit its price and
confirm the change persists; delete it and confirm it disappears. Capture the
transcript.

## Handoff

`docs/final-delivery/handoffs/admin.md`. Record: the admin route registration
request, any `SecurityConfig` matcher gap, and whether P1's product write tests
needed additions.
