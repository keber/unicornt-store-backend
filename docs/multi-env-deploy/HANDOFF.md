# Handoff — multi-env deploy, resume at P8 (promote to prod)

**Written:** 2026-09-05, end of the session that completed P0–P7.
**Read in this order:** this file → [PLAN.md](PLAN.md) §4 "P8" and §9 (runbooks) →
[LESSONS.md](LESSONS.md) if you hit something that smells like a repeat of a
past problem.

> **Update 2026-09-05.** P8 has since been reframed and its preflight
> completed — read [PLAN.md](PLAN.md) §4 "P8" as the current source of truth,
> not the checklist summary below. Specifically: item 2 here is stale (the prod
> `.env` is complete; it was world-readable at `644` and is now `600`), item 3's
> `SQL_INIT_MODE=always` is superseded, and items 6–7 are confirmed — the old
> single-target deploy path is fully retired, with nothing left on the box.
> Two facts the checklist below did not account for: the live prod storefront is
> still the pre-integration static app, and the `e2e` suite is knowingly stale
> and must be demoted from required before the frontend can merge.

## One-paragraph state of the world

Both repos (`unicornt-store-backend`, sibling checkout `unicornt-store-frontend`)
now deploy automatically on push to `dev`/`qa`/`main`, gated by a promotion-path
check (`gate-pr-source.yml`: main only from qa, qa only from dev) enforced by
branch rulesets. **`dev` and `qa` are live and verified on both repos.** `main`
(prod) has never been deployed through this pipeline — that's the whole job
here. Everything below is what you need to not re-discover things this session
already paid for.

## Current live state

| Env | Frontend | Backend API | Verified |
|---|---|---|---|
| dev | `https://unicornt-dev.keber.dev` | `https://api-unicornt-dev.keber.dev` | ✅ both, independently |
| qa | `https://unicornt-qa.keber.cl` | `https://api-unicornt-qa.keber.cl` | ✅ both, independently |
| prod | `https://unicornt-store.keber.cl` (**GitHub Pages**, unchanged, not part of this pipeline) | `https://api-unicornt-store.keber.cl` (nginx site exists, nothing deployed there yet) | ❌ not yet |

Branch rulesets: `main_PR_required` active on both repos (was already active,
had a bug — see LESSONS.md). `dev_PR_required` / `qa_PR_required` now also
**active** on both repos — direct pushes to `dev`/`qa` are blocked, everything
goes through a PR (including trivial docs commits — this bit the previous
session near the end, budget for it).

## Your task: P8 — promote to prod

Checklist lives in [PLAN.md §4 "P8"](PLAN.md) — don't duplicate it here, but
the state of each item as of this handoff:

1. **Supabase** — not yet touched this rollout. Need: create/confirm the
   `unicornt_store` schema exists, take a snapshot/backup first, decide on
   `&sslmode=require` in the JDBC URL. `../.env.prod` (parent dir, backend
   repo's *sibling*, not tracked) has a Supabase connection string from an
   earlier point — its password was flagged for rotation in an earlier
   session; confirm it was actually rotated before trusting it.
2. **Backend `/opt/unicornt/prod/.env`** — last checked (a while before this
   handoff, may be stale) it was missing `DOCKERHUB_USERNAME` and
   `SPRING_DATASOURCE_URL`. Re-verify with `deploy/vps-check.sh` (in this repo)
   before assuming anything.
3. **Schema management decision** — `SQL_INIT_MODE=always` re-runs the V1–V3
   scripts every boot; safe (idempotent) but not great for prod. P9 (Flyway)
   fixes this properly; at minimum run the scripts once manually against
   Supabase and set `JPA_DDL_AUTO=validate` before the first real deploy.
4. **`qa → main` PR must be opened fresh.** There was a stray `dev → main` PR
   (#25 on frontend) predating this whole rollout — it got closed, not
   reused, because `qa` already had everything it carried via the `dev → qa`
   promotion. Don't go looking for it; open a new one.
5. **Backend `prod` GitHub Environment already has a required reviewer**
   (`keber`) and branch policy (`main` only) configured — `deploy(prod)` will
   pause for manual approval in the Actions UI. That's intentional (D5-era
   decision), not a bug to work around.
6. **Frontend prod is GitHub Pages, not the VPS.** `deploy-vps.yml`'s push
   trigger is only `["dev", "qa"]` — it does not run on `main`. Frontend prod
   deploys via the pre-existing `static.yml` (unchanged, no `environment:`
   reference). **Don't try to wire `deploy-front-prod`/`deploy-vps.yml` for
   prod** — there's nothing to wire; that job already works and isn't part of
   this project.
7. **`deploy-front-prod` and `deploy-unicornt-frontend-prod` already exist on
   the VPS** (created alongside dev/qa, same session as the rest) but are
   **currently unused** (point 6). If a future need for a VPS-served frontend
   prod ever arises, `deploy-unicornt-frontend-prod`'s script content almost
   certainly has the same bug dev/qa's did before they were fixed — it's a
   literal copy of the *backend's* `docker compose pull` script, not a
   git-fetch script. Check before trusting it. Until then, safe to ignore or
   remove.
8. **Backend's `main_PR_required` and frontend's `main_PR_required`** were
   fixed this session (see LESSONS.md #6) — required checks are now
   `Run Tests`/`quality` + `Enforce promotion path`. A `qa → main` PR needs
   both green to be mergeable.

## Things nobody has checked yet for prod specifically

- Backend prod's Postgres connectivity from the VPS to Supabase (network
  egress, firewall, `sslmode`) — dev/qa never exercised this path (they use
  local containers).
- Whether `APP_CORS_ALLOWED_ORIGINS` for prod is exactly
  `https://unicornt-store.keber.cl` (var `WEB_ORIGIN` already carries this
  value in the backend `prod` Environment — confirm the server `.env` matches
  it, don't just trust the GitHub side).
- Whether the *old*, pre-this-rollout single-target deploy path (the original
  `main.yml` that pushed straight to prod on every `main` merge) is still
  wired to anything real on the box that could double-deploy or conflict.
  PLAN.md P8 has a checklist item to retire it — do that check even if it
  looks already retired.

## Operating notes for whoever (human or agent) picks this up

- **No agent in this project has ever had direct VPS shell access.** Every
  server-side command this session ran was pasted by the user after being
  given exact copy-paste blocks, then the output pasted back for verification.
  Continue that pattern — don't assume you can SSH in yourself.
- `gh` CLI is authenticated as `keber` and has write access to both repos
  (secrets, variables, rulesets, PRs — used extensively this session). Always
  pass `-R owner/repo` explicitly; there's no fixed default.
- Local checkouts: backend at the primary working directory, frontend at the
  sibling directory `../unicornt-store-frontend`. Both had a `dev` branch
  checked out at handoff time.
- Stray merged branches not yet deleted (harmless, clean up whenever):
  backend `ci/multi-env-pipeline`, `docs/p7-progress`, `docs/p7-done`;
  frontend `ci/dev-qa-deploy`.
- `docs/multi-env-deploy/debug.log` in this repo is unrelated Chromium/VS Code
  crash-reporter noise, not part of this project — leave it, don't reference
  it, don't feel obligated to clean it up.
