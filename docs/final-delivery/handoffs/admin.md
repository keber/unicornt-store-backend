# Handoff — P4 admin product (orchestrator-run)

**Repos / branches:** merged into `unicornt-store-frontend @ final-delivery`.
Backend **untouched** — product CRUD was already `hasRole('ADMIN')`-gated and tested in P1.
**Base:** frontend `d0b12e3`.
**Status:** complete, integrated, gate green.

## What landed (frontend only)

- `src/api/product.api.ts`: `createProductRequest`, `updateProductRequest`,
  `deleteProductRequest` over the shared `apiFetch` (bearer token attached automatically).
- `src/services/product.service.ts`: `createProduct`, `updateProduct`, `deleteProduct`
  wrapping the transport with `isProductDto` validation.
- `src/views/admin/adminProducts.view.ts`: admin-gated module — list, create/edit form
  (`name, description, imageBase, price, categoryId, productTypeId, stock, active`),
  delete. No session → sign-in gate. Non-admin session or a `403` from a write →
  actionable "necesitas una sesión de administrador" message, never a broken screen.
  `submitting / success / error` states, safe DOM (`textContent`, typed queries,
  `preventDefault`).
- `admin.html` + `src/pages/admin.main.ts` entry; `admin` added to `vite.config.ts`
  `rollupOptions.input`.
- Tests: `src/views/admin/adminProducts.view.test.ts` (gate, forbidden, list render,
  create posts the mapped payload + reloads, delete removes the row, 403 → message),
  `src/pages/admin.main.test.ts`.

## Backend

No change. Verified: `ProductRestController` `POST/PUT/DELETE` all carry
`@PreAuthorize("hasRole('ADMIN')")`; `SecurityConfig` has the
`POST/PUT/PATCH/DELETE /api/v1/products` → `hasRole("ADMIN")` matchers;
`SecurityChainTest` covers `POST /api/v1/products` → 401 (no token) / 403 (USER) /
201 (ADMIN); `CreateProductUseCaseTest`, `UpdateProductUseCaseTest`,
`DeleteProductUseCaseTest` cover the use cases (P1). No gap, no additions.

## Verification

| Check | Result |
|-------|--------|
| frontend `npm run build` | green |
| frontend `npm test` | green — see integration report |
| frontend `npm run lint` | green (0 `any`, 0 `!`) |
| admin E2E gate (live) | green — see transcript in the P4 integration report |

## Requests for the orchestrator

- **Router / entry:** `admin` input added to `vite.config.ts` and `admin.html` created —
  applied here (orchestrator owns wiring; done as part of integration).
- `SecurityConfig`: no matcher gap.
- `pom.xml` / `package.json`: untouched.
