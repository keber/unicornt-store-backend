# Multi-environment deploy — dev / qa / prod

Working plan for promoting `unicornt-store-backend` (and the matching
`unicornt-store-frontend`) through three branch-gated environments, each on its
own live URL, all sharing one VPS behind the existing nginx + SSL setup.

**Status:** in progress. D1–D8 settled (§2). §3, P0–P4 done. **P5, P6 and P7 all
done 2026-09-04 — dev and qa are both fully live on both repos**:
`unicornt-dev.keber.dev` / `api-unicornt-dev.keber.dev` and
`unicornt-qa.keber.cl` / `api-unicornt-qa.keber.cl`, every pipeline green
end-to-end, independently verified. `dev_PR_required`/`qa_PR_required` fixed
(see P7 notes — backend had no required checks at all; frontend required a
check name that doesn't exist on that repo) and **enabled** on both repos —
direct pushes to `dev`/`qa` now require a PR on both repos.

**Now: P8**, reframed 2026-09-05 after preparing it surfaced two facts the
original checklist did not account for — the prod storefront is still the
pre-integration static app, and the backend promotion is independent of it.
P8.0 (preflight) is done; the prod box is ready and the old deploy path is
confirmed retired. Read §4 "P8" in full before touching anything.

Owner legend: 🤖 Claude does it in the repo · 🧑 you do it (GitHub settings, VPS,
DNS, Supabase) · 👥 together (review / merge / watch a deploy).

---

## 1. Goal

| Env  | Git branch | Frontend URL                     | Backend API URL                 | App port (host) | DB                                   |
|------|-----------|-----------------------------------|--------------------------------|-----------------|--------------------------------------|
| dev  | `dev`     | `unicornt-dev.keber.dev`          | `api-unicornt-dev.keber.dev`   | 8081            | Postgres container on VPS (`pgdata_dev`) |
| qa   | `qa`      | `unicornt-qa.keber.cl`            | `api-unicornt-qa.keber.cl`     | 8082            | Postgres container on VPS (`pgdata_qa`)  |
| prod | `main`    | `unicornt-store.keber.cl` (GitHub Pages) | `api-unicornt-store.keber.cl` | 8088     | Supabase (pooled Postgres)          |

API hosts are flat names (`api-unicornt-<env>.<tld>`), not subdomains of the
frontend host — each is its own DNS `A` record and its own TLS cert (no wildcard
shortcut). Routing is host-based only; nginx does **not** rewrite an `/api` path
(D1).

Branch flow (enforced, see P1):

```
any feature branch ──PR──▶ dev ──PR──▶ qa ──PR──▶ main
        (deploy dev)   (deploy qa)   (deploy prod, manual approval)
main / qa ──PR──▶ dev        back-merge of hotfixes, gate does not block these
```

Every environment: no direct pushes, PR-only, required status checks, no
force-push, no branch deletion, linear history.

---

## 2. Decisions — settled

| # | Decision | Resolution |
|---|----------|-----------|
| D1 | API routing | **Host-based, per env.** Dedicated API host (`api-unicornt-<env>.<tld>`); nginx does not do `/api` path rewriting even though it could for dev/qa. Redundant with the frontend host, accepted for uniformity. |
| D2 | Spring profile files in the image | **Keep `application-{dev,qa,prod}.yml` gitignored** (an approval criterion names `application-prod.yml`). Only the `*.yml.example` templates are committed. The three real files are generated from their templates **during the Docker build** so all three ship in the jar and `SPRING_PROFILES_ACTIVE` selects at runtime (§7.7). Local non-Docker runs generate them with `scripts/gen-profiles.sh`. |
| D3 | `qa` profile behaviour | `ddl-auto: validate`, `show-sql: false`, **Swagger UI enabled**. Reflected in `application-qa.yml.example`. |
| D4 | Required PR approvals | **0 required approvals**; rely on required checks + no-force-push + linear history. Revisit if a collaborator joins. |
| D5 | Image tags | `build-and-push` pushes `:sha-<short>` **and** a moving channel tag `:dev` / `:qa` / `:prod` (+ `:latest` for prod). **Correction 2026-09-05:** the *deploy* does not pin the immutable tag. The scripts installed on the box ignore what CI sends and `docker compose pull` whatever `IMAGE_TAG` the env's `.env` holds — which is the channel tag (`IMAGE_TAG=dev` / `qa` / `prod`). `main.yml` documents this correctly; the repo's `deploy/deploy.sh` and §7.2 described the unbuilt pinning design. Consequence for rollback: see §6. |
| D6 | Registry | **Docker Hub** (`$DOCKERHUB_USERNAME/unicornt-store`), unchanged. |
| D7 | Promotion merge style | **Squash merge** for every promotion PR (`dev → qa`, `qa → main`), both repos. `main`'s history then reads as one commit per promotion regardless of how much churn rode along. Settled 2026-09-05; the frontend already did this (PR #27), the backend used a merge commit (PR #7). |
| D8 | Working notes vs documentation | `docs/configuration.md`, `docs/deployment.md`, `docs/development.md` describe the deployed system and are promoted normally. `docs/multi-env-deploy/`, `docs/final-delivery/`, `docs/archive-refactor-hito4/` are **working notes**. For the rest of this cycle they ride along with code — **never open a docs-only promotion PR**; the friction was never "notes on `main`", it was three PRs to move a paragraph. At the archive point (delivery + multi-env rollout both complete) they move off the promotion path for good: a `notes` branch edited through a worktree, folded back in as one curated archive PR. Settled 2026-09-05. |

---

## 3. Secrets hygiene — do this first (🧑)

- [X] **Rotate the Supabase database password.** It is currently in cleartext in
      `.env.dev.DB`, `.env.prod.DB`, and `../.env.prod`, and has been read into a
      Claude session. Generate a new one in the Supabase dashboard.
- [X] **Delete `.env.dev.DB`.** It points dev at the *prod* Supabase DB, which
      contradicts D-topology (dev gets a local container). Keep only
      `.env.prod.DB` (prod → Supabase) or fold it into the server-side `.env`.
- [X] Regenerate `APP_JWT_SECRET` for each env (the values now in `.env.dev`,
      `.env.qa`, `.env.prod` sat in a synced folder). One fresh secret per env.
- [X] Confirm `.gitignore` still covers `.env`, `.env.*` (it does) — none of the
      real env files are tracked. `git status` should not list any `.env*`.

---

## 4. Phases

### P0 — Confirm decisions & branches (👥)

- [x] Sign off on D1–D6 (settled — see §2).
- [x] 🧑 `qa` branch created in **backend** (from `main`; `dev → qa` merge will
      align it).
- [x] 🧑 `qa` branch created in **frontend** (same — will be aligned by a
      `dev → qa` merge).
- [x] 🧑 Reconcile stale branches later — not blocking.

### P1 — Branch protection, both repos (🧑 in GitHub UI / `gh`, 🤖 writes the gate workflow)

- [x] 🤖 `.github/workflows/gate-pr-source.yml` written (backend) — fails a PR
      into `main` whose head is not `qa`, and a PR into `qa` whose head is not
      `dev`. Final version in §7.1. Lands on `dev` with the P2 PR.
- [ ] 🤖 Add the same `gate-pr-source.yml` to the **frontend** repo.
- [x] 🧑 Backend rulesets for `main` / `qa` / `dev` created, **currently
      disabled** — re-enable when P2 merges so the checks exist.
- [ ] 🧑 When re-enabling, confirm each ruleset:
  - Require a pull request before merging (0 approvals — D4).
  - Required status checks: `Enforce promotion path` (gate-pr-source job name)
    + `Run Tests`. "Require branches up to date before merging."
  - Block force pushes. Restrict deletions. Require linear history.
- [ ] 🧑 Verify the `Run Tests` trigger covers PRs into `qa`: `main.yml`'s
      `pull_request` trigger is currently `branches: [main]` only — P2 widens it
      to `[main, qa]` so the check actually runs on `dev → qa` PRs.
- [ ] 🧑 Frontend rulesets for `main` / `qa` / `dev`: same, checks
      `quality` (from `ci.yml`) + `Enforce promotion path`.
- [ ] 👥 Smoke-test once enabled: throwaway PR `feature/x → main` must be
      blocked by the gate; `dev → qa` must pass it.

### P2 — Backend code changes (🤖) — **done, on branch `ci/multi-env-pipeline`, PR to `dev` pending**

- [x] `application-qa.yml.example` — rewritten per D3 (validate + no show-sql + Swagger on).
- [x] `.gitignore` — added `application-qa.yml` line (D2).
- [x] `Dockerfile` — build stage generates `application-{dev,qa,prod}.yml` from
      the `*.yml.example` templates (§7.7).
- [x] `scripts/gen-profiles.sh` — same for local non-Docker runs; referenced
      from `docs/development.md`.
- [x] `.dockerignore` — new; keeps `target/`, `.env*`, `node_modules/`, `.git/`
      etc. out of the build context.
- [x] `.github/workflows/main.yml` rewritten:
  - `pull_request` trigger widened to `[main, qa]`.
  - `publish-reports` — Sonar/Codecov still run everywhere; only the **Pages
    publish steps** are gated to `main`.
  - `resolve-target` (new) — maps `dev`/`qa`/`main` push → `channel`
    `dev`/`qa`/`prod` + `sha_tag=sha-<7>` + tag list.
  - `build-and-push` — builds via the Dockerfile only (no runner-side `mvn
    package`); pushes `:sha-<7>` + `:<channel>` (+ `:latest` for prod).
  - `deploy` — `environment: <channel>`, `concurrency: deploy-<channel>`; SSH
    `"deploy sha-<7>"`; **smoke folded in as a final step** (kept it one job so
    the prod approval gate fires once, not twice).
- [x] `.github/workflows/gate-pr-source.yml` — carried in on this branch.
- [x] `deploy/deploy.sh` — one script, sits in each `/opt/unicornt/<env>/`, acts
      on its own dir; validates the `sha-` tag from `$SSH_ORIGINAL_COMMAND`.
- [x] `deploy/{dev,qa,prod}/docker-compose.yml` + `.env.example` — dev/qa carry a
      Postgres container (no published port), prod is app-only (Supabase).
- [x] `docs/{deployment,configuration,development}.md` updated.

### P3 — VPS infrastructure (🧑 on the box; 🤖 supplies configs)

- [X] 🧑 DNS `A` records → VPS IP: `unicornt-dev.keber.dev`,
      `api-unicornt-dev.keber.dev`, `unicornt-qa.keber.cl`,
      `api-unicornt-qa.keber.cl`, `api-unicornt-store.keber.cl`.
      (`unicornt-store.keber.cl` stays pointed at GitHub Pages.)
- [x] 🧑 Create server dirs:
      `/opt/unicornt/{dev,qa,prod}/` each with `compose.yml` (from
      `deploy/<env>/`) + `.env` (secrets, D3 profile, `IMAGE_TAG=` line).
- [x] 🧑 Bring up the dev + qa Postgres containers, confirm named volumes
      `pgdata_dev` / `pgdata_qa` exist and survive `docker compose down`.
- [x] 🧑 nginx server blocks (§7.6) — one per hostname:
  - `api-unicornt-dev.keber.dev` → `proxy_pass http://127.0.0.1:8081`
  - `unicornt-dev.keber.dev` → `root /opt/easyengine/sites/unicornt-dev.keber.dev/app/htdocs` (static)
  - `api-unicornt-qa.keber.cl` → `127.0.0.1:8082`
  - `unicornt-qa.keber.cl` → `root /opt/easyengine/sites/unicornt-qa.keber.cl/app/htdocs` (static)
  - `api-unicornt-store.keber.cl` → `127.0.0.1:8088`
- [x] 🧑 `certbot --nginx` for the five VPS-served names (a `keber.dev` wildcard
      cert, if you have one, may already cover the two `.keber.dev` names).
- [X] 🧑 Create **three** deploy SSH keys, one per env; each `authorized_keys`
      entry uses `command="/usr/local/sbin/deploy-unicornt-<env>",no-port-forwarding,...`
      pointing at that env's `deploy.sh` (§7.2).
- [ ] 🧑 `mkdir -p /var/www/unicornt-dev /var/www/unicornt-qa`; give the
      frontend deploy key write access (rsync target).

**Verified 2026-09-04 via `deploy/vps-check.sh` + manual dump.** Actual infra:
EasyEngine (`nginx-proxy` container + per-site nginx container, not host nginx) —
that's fine, it terminates TLS for all 3 API hosts already. SSH is **better than
planned**: three users `deploy-{dev,qa,prod}`, each key `restrict,command="sudo
-n /usr/local/sbin/deploy-unicornt-<env>"` — properly locked, one script each,
no argument passed through (CI's `deploy <channel> <sha>` string is ignored;
harmless). `main.yml`'s deploy step and `deploy/vps-check.sh` were updated to
match. Remaining before the dev cutover (P5):

- [x] 🧑 dev `.env`: `IMAGE_TAG=latest` → `IMAGE_TAG=dev`, `chmod 600`. **qa
      still pending** — same edit, `IMAGE_TAG=qa`, before P7.
- [ ] 🧑 qa needs the same two fixes dev needed (see P5's "three bugs" note):
      reset `unicornt-qa-db` (stale pre-refactor schema →
      `ON CONFLICT (slug)` crash) and add `networks: [default,
      unicornt-qa-network]` under `qa/compose.yml`'s `app:` service (currently
      has no `networks:` key at all, same gap as dev had). Do both before or
      at the start of P7 — qa's first real deploy will 502/crash-loop
      otherwise, exactly like dev did.
- [ ] 🧑 `/opt/unicornt/qa/compose.yml`: the `networks:` **key name** is
      `unicornt-dev-network` (copy-paste from dev) instead of
      `unicornt-qa-network`. The external `name:` value is already correct
      (`api-unicornt-qa.keber.cl`) — only the label is wrong. Purely cosmetic,
      fix whenever convenient.
- [ ] 🧑 `/opt/unicornt/prod/.env` is missing `DOCKERHUB_USERNAME`,
      `SPRING_DATASOURCE_URL` (+ confirm `APP_CORS_ALLOWED_ORIGINS`,
      `APP_JWT_SECRET`) — not urgent, prod is P8.
- [x] 🧑 Repo secrets `DOCKERHUB_USERNAME` (`keberflores`) + `DOCKERHUB_TOKEN` set.

### P4 — GitHub Environments & secrets (🧑)

Backend repo → Settings → Environments: `dev`, `qa`, `prod`.

- [ ] Per environment secrets: `DEPLOY_HOST`, `DEPLOY_PORT`, `DEPLOY_USER`,
      `DEPLOY_SSH_KEY` (that env's private key).
- [   ] Per environment variables: `API_BASE_URL` (for the smoke job),
      `FRONTEND_ORIGIN` (for the CORS assertion).
- [x] `prod` environment: **required reviewer = you**; deployment branch rule =
      `main` only. `qa` → `qa` only. `dev` → `dev` only.
- [x] Leave `DOCKERHUB_USERNAME/TOKEN`, `SONAR_TOKEN`, `CODECOV_TOKEN`,
      `GIST_*` as repo-level secrets.
- [ ] Frontend repo Environments `dev` / `qa` / `prod`: `VPS_HOST`, `VPS_USER`,
      `VPS_SSH_KEY` (dev/qa only), and variable `VITE_API_BASE_URL`:
      `https://api-unicornt-dev.keber.dev` (dev) ·
      `https://api-unicornt-qa.keber.cl` (qa) ·
      `https://api-unicornt-store.keber.cl` (prod, used by the Pages build).

### P5 — Backend dev cutover (👥) — **DONE 2026-09-04**

- [x] 👥 Merged PR #6 into `dev` (`c2979c9`).
- [x] 👥 Watched the `dev` run: `test → resolve-target → build-and-push →
      deploy(dev) → smoke` — all green (run 33881738384, rerun after fixes).
- [x] 🧑 Verified on the box + externally: `https://api-unicornt-dev.keber.dev/api/v1/products`
      → 200, `Access-Control-Allow-Origin: https://unicornt-dev.keber.dev`.
- [x] Smoke job green in CI too (not just manually).

Three real bugs surfaced and fixed getting here (all backend-code-independent —
infra/CI wiring only):
1. **`resolve-target`'s `tags` output came out empty** — GitHub Actions drops a
   step *output* that contains a secret value (`DOCKERHUB_USERNAME`), silently.
   Fixed by interpolating the secret directly in `build-and-push`'s `with:`
   block instead of routing it through `needs.*.outputs` (`7f613fe`).
2. **`unicornt-dev-db` had a stale pre-refactor schema** — `product_types`
   existed without the `slug UNIQUE` constraint `V1__init.sql` declares (the
   table predated today, so `CREATE TABLE IF NOT EXISTS` skipped it), so
   `V2__seed_reference_data.sql`'s `ON CONFLICT (slug)` crashed the app on
   every boot. Fixed by `docker compose down -v && up -d db` to reinitialize
   from the correct migrations. **qa's db is the same age — expect the same
   crash there; reset it before/at P7.**
3. **`app` service had no `networks:` in `compose.yml`** — it only landed on
   the implicit Compose default network (needed to reach `db`), never on the
   external EasyEngine site network (`api-unicornt-dev.keber.dev`) that the
   proxy container (`proxy_pass http://unicornt-dev-app:8080`) requires to
   reach it by name → permanent 502. Fixed by adding
   `networks: [default, unicornt-dev-network]` under `app:`. **qa's
   compose.yml has the identical gap (network name `api-unicornt-qa.keber.cl`)
   — patch it before/at P7, or its first deploy 502s the same way.**

### P6 — Frontend dev cutover — **DONE 2026-09-04**

- [x] 🤖 `static.yml` (prod/Pages): bakes `VITE_API_BASE_URL=https://api-unicornt-store.keber.cl`
      into the build; verifies it landed in `dist/assets/*.js`. (`e2e-live.yml`
      needed no change — it tests the already-deployed site, it doesn't build.)
- [x] 🤖 `deploy-vps.yml`: push to `dev`/`qa` → lint+test+build
      (`VITE_API_BASE_URL` from `vars.API_HOST`) → bundle check → publish
      `dist/` to `deploy/<channel>` branch (git, not rsync — see redesign
      note above) → SSH-trigger the VPS pull → smoke check.
- [x] 🤖 `gate-pr-source.yml` added to frontend (mirrors backend).
- [x] 🤖 `ci.yml` / `unit-report.yml` / `e2e.yml`: added `dev`/`qa` to
      `pull_request` triggers (additive, `refactor`/`etapas/**` untouched).
- [x] Frontend PR [#26](https://github.com/keber/unicornt-store-frontend/pull/26) merged into `dev`.
- [x] 🧑 VPS + Environment setup done (see runbook + fixes below).
- [x] 👥 `deploy-vps.yml` green end to end (build → publish → SSH trigger →
      smoke). Verified independently: `https://unicornt-dev.keber.dev` → 200,
      `api-unicornt-dev.keber.dev` baked into `auth.service-*.js`.

**Bugs found and fixed while wiring this up** (all infra, no app code):

1. **Self-deadlock in `deploy-vps.yml`'s concurrency groups** — workflow-level
   and job-level `concurrency:` both resolved to the identical string
   (`deploy-vps-dev`); GitHub Actions detects that as a deadlock and cancels
   the job before it ever gets a runner (0ms duration, no steps). Fixed by
   dropping the redundant workflow-level group.
2. **Three parallel, inconsistent attempts at the deploy user** existed on the
   VPS by the time we wired secrets: `deploy-frontend-dev` (created by my
   literal runbook copy-paste, correct git-pull script, but never got an
   `authorized_keys`), `deploy-front-{dev,qa,prod}` (created independently
   earlier, `dev`'s key already installed), whose forced command pointed at
   `deploy-unicornt-frontend-dev` — **a script that was a literal copy of the
   backend's `deploy-unicornt-dev`** (`docker compose pull` against
   `/opt/unicornt/dev/compose.yml`, the *backend's* compose file — would have
   silently redeployed the backend instead of the frontend). Converged on
   `deploy-front-<env>` + corrected script content; deleted the redundant
   `deploy-frontend-dev` account. **`deploy-unicornt-frontend-{qa,prod}` have
   the same backend-copy bug — fix before wiring qa/prod (P7/P8).**
3. **`deploy-front-dev`'s `~/.ssh` had mode `drw-------` (no execute bit)** —
   directories need `x` to be traversable at all; this alone silently breaks
   key auth regardless of which key is installed. Fixed by recreating the
   directory with `mkdir -m 700`.
4. Along the way: a **stray trailing space in the `HOST` variable** (frontend
   `dev` env, pre-existing) would have broken the smoke-check URL — fixed.

**P6 runbook — VPS + Environment setup (🧑, kept for qa/prod reference):**

**Redesigned 2026-09-04** — no rsync. `deploy-vps.yml` now publishes `dist/` as
a normal (non-squashed) commit to a `deploy/dev` / `deploy/qa` branch in the
frontend repo (public, so anonymous `git fetch` works), then SSH-triggers a
forced command on the box that does `git fetch && git reset --hard` — the
exact pattern already proven on `keber.cl`, and the same SSH-trigger shape as
the backend's `deploy-unicornt-<env>`. Confirmed htdocs roots:

```
unicornt-dev.keber.dev -> /opt/easyengine/sites/unicornt-dev.keber.dev/app/htdocs
unicornt-qa.keber.cl   -> /opt/easyengine/sites/unicornt-qa.keber.cl/app/htdocs
```

> **Naming actually used** (dev is live with this — use the same for qa/prod):
> deploy user `deploy-front-<env>` (not `deploy-frontend-<env>`), server script
> `/usr/local/sbin/deploy-unicornt-frontend-<env>` (matches the backend's
> `deploy-unicornt-<env>` family name-wise, but git-pull content, **not** the
> docker-compose content it was copy-pasted from originally — verify qa/prod's
> script content before wiring sudoers to them, see the P6 "bugs found" note
> above).

GitHub Environments (`dev`, `qa` — already exist, empty): add secrets
`DEPLOY_HOST`, `DEPLOY_PORT`, `DEPLOY_USER` (`deploy-front-dev` /
`deploy-front-qa`), `DEPLOY_SSH_KEY`; variable `API_HOST`
(`api-unicornt-dev.keber.dev` / `api-unicornt-qa.keber.cl` — `HOST`, the
frontend's own host, already exists).

**a. Turn each htdocs root into a git working copy tracking its `deploy/<env>`
branch.** `deploy/dev` doesn't exist in the repo yet — it's created by the
first `deploy-vps.yml` run, so do that first (push the frontend PR's branch or
merge it, or `workflow_dispatch` it once), *then* set up the box:

```sh
# confirm the branch exists first: git ls-remote https://github.com/keber/unicornt-store-frontend.git deploy/dev
cd /opt/easyengine/sites/unicornt-dev.keber.dev/app/htdocs
ls -la                          # look before wiping — see what EasyEngine put here
git init
git remote add origin https://github.com/keber/unicornt-store-frontend.git
git fetch origin deploy/dev
git reset --hard origin/deploy/dev
```

Repeat for qa (`deploy/qa`). No credentials needed — public repo, plain HTTPS.

**b. Block `.git/` from being served** — a git working copy as webroot leaks
its whole history over HTTP if nginx doesn't hide dotfiles. `keber.cl` already
does this exact pattern successfully, so it has the answer already — check its
nginx config and replicate:

```sh
grep -rn "\.git" /opt/easyengine/sites/keber.cl/config/nginx/ 2>/dev/null
docker exec keber-clkeberdev-nginx-1 grep -rn "\.git\|location.*\\\\." /usr/local/openresty/nginx/conf/ 2>/dev/null
```

If found, copy the equivalent rule into the dev/qa sites' nginx config
(container name will follow the `<site-slug>-nginx-1` pattern seen earlier,
e.g. `unicornt-devkeberdev-nginx-1`). Verify: `curl -sI
https://unicornt-dev.keber.dev/.git/HEAD` must **not** return 200.

**c. Deploy user + forced command per env** (same shape as the backend's
`deploy-{dev,qa,prod}`):

```sh
sudo useradd -m -s /bin/sh deploy-frontend-dev
sudo -u deploy-frontend-dev mkdir -p ~deploy-frontend-dev/.ssh
echo 'restrict,command="/usr/bin/sudo -n /usr/local/sbin/deploy-frontend-dev" ssh-ed25519 AAAA... github-actions:unicornt-store-frontend-dev' \
  | sudo -u deploy-frontend-dev tee ~deploy-frontend-dev/.ssh/authorized_keys
sudo chmod 700 ~deploy-frontend-dev/.ssh
sudo chmod 600 ~deploy-frontend-dev/.ssh/authorized_keys
```

`/usr/local/sbin/deploy-frontend-dev` (mirrors `deploy-unicornt-dev`,
including the `flock`):

```sh
#!/usr/bin/env bash
set -Eeuo pipefail
readonly REPO_DIR="/opt/easyengine/sites/unicornt-dev.keber.dev/app/htdocs"
readonly BRANCH="deploy/dev"
readonly LOCK_FILE="/run/lock/unicornt-frontend-dev-deploy.lock"
exec 9>"${LOCK_FILE}"
/usr/bin/flock -n 9 || { echo "ERROR: ya existe otro despliegue en ejecución."; exit 1; }
cd "${REPO_DIR}"
/usr/bin/git fetch origin "${BRANCH}"
/usr/bin/git reset --hard "origin/${BRANCH}"
echo "Estado final:"
/usr/bin/git log -1 --oneline
```

`sudo chmod +x` it, then the matching sudoers line (append to
`/etc/sudoers.d/unicornt-deploy`, or a new file — `visudo -c` after either
way):

```
deploy-frontend-dev ALL=(root) NOPASSWD: /usr/local/sbin/deploy-frontend-dev
```

Repeat (b) and (c) for qa. Local test before wiring CI (same shape as the
backend's dry run):

```sh
sudo -u deploy-frontend-dev sudo -n /usr/local/sbin/deploy-frontend-dev
```

### P7 — Promote to qa — **DONE 2026-09-04, both repos**

- [x] 👥 PR `dev → qa` in both repos ([backend #7](https://github.com/keber/unicornt-store-backend/pull/7),
      [frontend #27](https://github.com/keber/unicornt-store-frontend/pull/27)) — gate passed, merged.
- [x] Backend `qa` run: fully green first try (dev's prep work paid off) —
      verified `https://api-unicornt-qa.keber.cl/api/v1/products` → 200,
      correct CORS.
- [x] Frontend `qa`: VPS setup (script content fix, `.ssh` created with the
      right mode from the start this time, sudoers, fresh keypair) went clean
      on the first attempt — no rework needed, unlike dev. `deploy-vps.yml`
      green end to end. Verified `https://unicornt-qa.keber.cl` → 200,
      `api-unicornt-qa.keber.cl` baked into the bundle.
- [x] 👥 Verified `unicornt-qa.keber.cl` end to end.
- [x] Along the way: found + closed a stray frontend PR #25 (`dev → main`
      directly, predating the branch-gating work) — `gate-pr-source` +
      `main_PR_required` correctly blocked it from merging. `qa` already has
      everything it carried via #27; the real `qa → main` PR comes at P8.
- [x] 👥 Fixed and enabled `dev_PR_required`/`qa_PR_required` on both repos
      (were disabled; backend's had no required checks at all, frontend's
      required a check name — `Run Tests` — that doesn't exist on that repo,
      which would have permanently deadlocked frontend `main` merges). Now:
      backend requires `Run Tests` (+ `Enforce promotion path` on qa/main);
      frontend requires `quality` (+ `Enforce promotion path` on qa/main).
      `SonarCloud Code Analysis` deliberately left non-required (unreliable).

### P8 — Promote to prod (👥)

**Reframed 2026-09-05.** The original checklist assumed prod was one more
cutover like dev and qa. Two facts found while preparing it say otherwise, and
they change the order of operations:

1. **The live prod storefront is still the pre-integration static app.** The
   bundle served from `unicornt-store.keber.cl` reads `data/products.json` and
   never calls an API. Frontend `qa → main` is therefore not a CI-only change:
   it is ~80 files that swap prod onto the backend-integrated app, and `qa`'s
   `static.yml` hard-bakes `https://api-unicornt-store.keber.cl` with a step
   that fails the build if the URL is absent from the bundle. **Merging the
   frontend before the prod API is live and seeded takes the prod storefront
   down.**
2. **Backend `qa → main` is independent and low-risk** — 21 files, all CI /
   deploy / config / docs. `src/` is untouched apart from two renamed
   `.example` profiles. The backend can be promoted on its own.

So: **backend prod first, proven end to end, and only then the frontend.**

The risk that matters is not downtime on a portfolio site — it is discovering a
prod-only failure *after* the public site already depends on it, when rollback
is "re-run an old Pages job". Everything below is ordered to make each
prod-only unknown fail early and cheaply. There are exactly four, and dev/qa
exercised none of them: Supabase egress + TLS from the VPS, Supabase schema
state, write-path behaviour against a *pooled managed* Postgres, and CORS from
the real prod origin.

#### P8.0 — Preflight (🧑 on the box) — **DONE 2026-09-05**

- [x] `deploy/vps-check.sh` run: every prod check `[OK]` except the three
      expected "no CI deploy yet" warnings. The NOPASSWD warnings on all three
      envs are a limitation of the check itself — dev and qa deploy fine.
- [x] `chmod 600 /opt/unicornt/prod/.env` — it was `644`, i.e. the Supabase
      password and `APP_JWT_SECRET` were world-readable on a shared box.
- [x] `/usr/local/sbin/deploy-unicornt-prod` read in full against its dev/qa
      siblings (LESSONS #10). It is a correct prod script — `flock`, acts on
      `/opt/unicornt/prod`, `compose config --quiet && pull && up -d`. No
      copy-paste bug. It does **not** pin an image tag; see the D5 correction
      in §2 and §7.2.
- [x] Old single-target deploy path retired: the repo-level `DEPLOY_*` secrets
      it needed no longer exist (they live in Environments now), the merge
      replaces `main.yml` wholesale, and `docker ps -a` shows no container left
      from it. **Nothing further to retire.**
- [x] `prod` GitHub Environment verified complete: `HOST`,
      `WEB_ORIGIN=https://unicornt-store.keber.cl`, `INTERNAL_PORT=8088`,
      `COMPOSE_PROJECT`, `ENVIRONMENT_NAME`, all four `DEPLOY_*` secrets,
      required reviewer `keber`, branch policy `main`.

#### P8.1 — Supabase (🧑)

- [ ] Prove egress, TLS, credentials and schema state in one shot, from the
      VPS. Note the JDBC URL is **not** a libpq URL — `psql` rejects it
      verbatim; the form that works is in §9.4.
- [ ] Take a snapshot / confirm PITR is on **before** any schema work.
- [ ] Confirm or create the `unicornt_store` schema.
- [ ] Settle `sslmode`: the Supabase pooler requires TLS, so the JDBC URL wants
      `&sslmode=require` unless the driver is already negotiating it.

#### P8.2 — Prod schema management (🧑)

- [ ] Run `V1..V3` manually against Supabase, once.
- [ ] Then set `SQL_INIT_MODE=never` in `/opt/unicornt/prod/.env`. Boot-time
      schema mutation against a pooled managed database is the wrong default;
      the template's `SQL_INIT_MODE=always` is corrected as a chore. **There is
      no `JPA_DDL_AUTO` env var** — earlier drafts of this plan said to set one;
      `application-prod.yml` hardcodes `ddl-auto: validate`, which is already
      what prod wants. P9 (Flyway) replaces this properly.
- [ ] **Decide what prod's catalog actually is** — same seed as qa, or real
      data. A green API over an empty catalog is a broken-looking store, and
      this decision has never been written down.

#### P8.3 — Backend cutover (👥)

- [ ] PR `qa → main`, **squash merge** (D7). Gate requires head = `qa`;
      required checks are `Run Tests` + `Enforce promotion path`.
- [ ] Approve the `prod` environment gate in the Actions UI (expected — D5-era
      required reviewer, not a bug to work around).
- [ ] Watch `deploy(prod)` → `127.0.0.1:8088`; the smoke step asserts
      `https://api-unicornt-store.keber.cl/api/v1/products` 200 + CORS =
      `https://unicornt-store.keber.cl`.
- [ ] **Write-path contract check against prod** — register → login → add to
      cart → create order, via `docs/bruno/unicornt-store/`. This is the step
      that actually exercises Supabase (transactions, sequences, constraints);
      `GET /products` proves almost nothing about a managed pooled DB.

#### P8.4 — Frontend cutover (👥) — only once P8.3 is green *and seeded*

- [ ] Demote the stale `e2e` suite from required first — see P8.5.
- [ ] Record the current Pages run id, so rollback is a known button rather
      than a search.
- [ ] Optional but recommended rehearsal: temporarily add
      `http://localhost:4173` to prod's `APP_CORS_ALLOWED_ORIGINS`, build the
      frontend locally with
      `VITE_API_BASE_URL=https://api-unicornt-store.keber.cl`, `npm run
      preview`, click through the real app against the real prod stack — then
      revert the CORS entry **and verify it is gone**. This is the only check
      that proves the integrated frontend works against Supabase before the
      public site depends on it. (The heavier alternative — a canary host on
      the idle `deploy-front-prod` machinery — buys the same signal for a DNS
      record and a cert, and is not worth it here.)
- [ ] PR `qa → main` on the frontend, **squash merge**. Required checks:
      `quality` + `Enforce promotion path`.
- [ ] Confirm `static.yml` bakes the prod API host and the Pages site reaches
      it.

#### P8.5 — E2E suite (🧑, blocking P8.4)

The Playwright suite in `keber/QA-UnicorntStore-refactor` was written to
validate the static→Vite refactor. The frontend has since changed
substantially, so its selectors are stale: most specs fail by timeout, retry,
and time out again — which is why a run takes the full 30-minute cap. Keeping
it as a required check on `main` is the LESSONS #3 failure mode with extra
steps: a check that cannot pass, gating the branch.

- [ ] Edit the frontend's `rule_e2e_statuscheck_main` ruleset to drop **only**
      its `required_status_checks` rule. Do not delete the ruleset — it also
      carries `deletion` and `non_fast_forward` protections.
- [ ] Leave `e2e.yml` running as informational, so the maintenance work has a
      signal to chase.
- [ ] Decide what to do about `e2e-live.yml`: it fires on `workflow_run`
      after **every** successful Pages deploy from `main`, so it will spend 30
      minutes going red immediately after the cutover. Recommend gating it off
      until the suite is maintained — a red run at the moment of final
      delivery reads badly and says nothing new.
- [ ] Suite maintenance is its own workstream, not a P8 blocker.

### P9 — Hardening (later, not blocking)

- [ ] Add `org.springframework.boot:spring-boot-flyway`; set
      `FLYWAY_ENABLED=true`, `SQL_INIT_MODE=never`; convert `V1..V3` to Flyway
      migrations. Prod stops mutating schema on boot.
- [ ] `deploy.resources` limits in each compose file so dev/qa load cannot
      starve prod on the shared box.
- [ ] Nightly `pg_dump` of the dev/qa volumes; rely on Supabase PITR for prod.
- [ ] Uptime check / alert on the three `/api/v1/products` endpoints.
- [ ] Prune dangling images on a schedule. The earlier claim that
      `docker image prune` is "already in `deploy.sh`" was wrong — the script
      actually installed on the box does not prune, so every deploy leaves the
      previous image behind on a shared VPS. A weekly cron should cover
      dangling images, volumes and build cache.
- [ ] Restrict `publish-reports` + gist badge to `main` (done in P2) — confirm
      no per-branch report noise remains.

---

## 5. Who does what — summary

| Area | 🤖 Claude | 🧑 You |
|------|-----------|--------|
| Workflows (`main.yml`, `gate-pr-source.yml`, frontend `static.yml`, `deploy-vps.yml`) | ✅ write / edit | review + merge PRs |
| `application-qa.yml`, compose templates, `deploy.sh`, nginx snippets, smoke script, docs | ✅ write | copy to server |
| GitHub rulesets, Environments, secrets, required reviewer | — | ✅ |
| DNS records, certbot, `/opt/unicornt/*`, DB containers, SSH deploy keys, `/var/www/*` | — | ✅ |
| Supabase project, schema, backup, credential rotation | — | ✅ |
| Cutover runs, verification | 👥 | 👥 |

---

## 6. Rollback

- **Bad backend deploy:** `ssh deploy-<env>@host` isn't interactive. Because the
  deploy follows the *moving channel tag* (D5 correction), re-running the
  previous good workflow run rolls back by rebuilding that SHA and moving the
  channel tag back onto it — it works, but it is a rebuild, not a repoint. The
  fast repoint is on the box: `sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=sha-OLD/'
  /opt/unicornt/<env>/.env && docker compose up -d`, which works because
  `build-and-push` also pushes every `:sha-<short>`. Remember to put
  `IMAGE_TAG` back to the channel tag afterwards, or the next CI deploy will
  appear to do nothing.
- **Bad frontend deploy (dev/qa):** re-run the previous frontend run; rsync
  overwrites `/var/www/unicornt-<env>`.
- **Bad prod frontend (Pages):** re-run the previous `static.yml` run (single
  orphan commit on `gh-pages`). Record that run's id **before** the P8.4
  cutover — after it, the pre-integration static build is no longer on `main`
  and finding the right run under time pressure is the slow part.
- **Branch protection mistake:** rulesets are non-destructive; edit/disable in
  Settings.
- **Supabase schema damage:** restore from the P8 snapshot / PITR.

---

## 7. Concrete artifacts (drafts — finalised when we build them)

### 7.1 `.github/workflows/gate-pr-source.yml` — written (backend)

Job name is **`Enforce promotion path`** — that is the string to add as a
required status check. Runs only for PRs into `main` / `qa`; PRs into `dev`
never trigger it, so feature branches and `main`/`qa` back-merges are unaffected.
File is on disk in the backend repo; commit it with the P2 PR. Copy verbatim to
the frontend repo.

### 7.2 `/usr/local/sbin/deploy-unicornt-<env>` (forced command, one per env)

**Corrected 2026-09-05 to match what is actually installed on the box.** The
earlier draft here (and `deploy/deploy.sh` in this repo) described a script
that reads an immutable `sha-` tag out of `SSH_ORIGINAL_COMMAND` and pins it
into `.env`. That is not what runs. The installed script ignores the client's
command entirely and pulls whatever `IMAGE_TAG` the env's `.env` already holds
— the moving channel tag. See the D5 correction in §2.

```bash
#!/usr/bin/env bash
set -Eeuo pipefail

readonly APP_DIR="/opt/unicornt/prod"        # <- per-env dir; the key can touch nothing else
readonly COMPOSE_FILE="${APP_DIR}/compose.yml"
readonly LOCK_FILE="/run/lock/unicornt-prod-deploy.lock"

exec 9>"${LOCK_FILE}"
/usr/bin/flock -n 9 || { echo "ERROR: ya existe otro despliegue en ejecución."; exit 1; }

[[ -f "${COMPOSE_FILE}" ]] || { echo "ERROR: no existe ${COMPOSE_FILE}."; exit 1; }

cd "${APP_DIR}"
/usr/bin/docker compose config --quiet
/usr/bin/docker compose pull
/usr/bin/docker compose up -d --remove-orphans
/usr/bin/docker compose ps
```

`authorized_keys` pins it as `restrict,command="sudo -n
/usr/local/sbin/deploy-unicornt-<env>" ssh-ed25519 AAAA...`, so the trailing
arguments CI sends (`"deploy <channel> sha-<7>"`) are a breadcrumb for the CI
and auth logs only — the server ignores them (LESSONS #8).

### 7.3 Smoke check (CI job step, per env)

```sh
set -eu
BASE="$API_BASE_URL"; ORIGIN="$FRONTEND_ORIGIN"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/products")
[ "$code" = 200 ] || { echo "products list $code"; exit 1; }
acao=$(curl -s -D - -o /dev/null -H "Origin: $ORIGIN" "$BASE/api/v1/products" \
        | tr -d '\r' | awk -F': ' 'tolower($1)=="access-control-allow-origin"{print $2}')
[ "$acao" = "$ORIGIN" ] || { echo "CORS ACAO='$acao' want '$ORIGIN'"; exit 1; }
echo "smoke OK"
```

### 7.4 Target `main.yml` job graph

```
on:
  push:            { branches: ['**'] }        # test on everything
  pull_request:    { branches: [main, qa] }
  workflow_dispatch:

jobs:
  test:                                        # all branches + PRs (unchanged)
  publish-reports:  needs: test                # Sonar/Codecov always; Pages publish only on main
  resolve-target:   needs: test                # only push on dev|qa|main; outputs channel + sha_tag + tags
  build-and-push:   needs: resolve-target       # build via Dockerfile; push :sha-<7> :<channel> (+ :latest on prod)
  deploy:           needs: [resolve-target, build-and-push]
                    environment: ${{ needs.resolve-target.outputs.channel }}   # dev|qa|prod
                    concurrency: deploy-${{ needs.resolve-target.outputs.channel }}
                    # step 1: ssh forced-command "deploy $SHA_TAG"  (host/port/user/key from the Environment)
                    # step 2: smoke — curl API 200 + assert CORS header (§7.3), as a step
                    #         not a separate job, so the prod approval gate fires once
```

`resolve-target` / `build-and-push` / `deploy` only run for `push` events on
`dev` / `qa` / `main`; every other push and all PRs stop after `test` (+ Sonar).

### 7.5 `deploy/dev/docker-compose.yml` (dev & qa; prod variant drops `db`)

```yaml
name: unicornt-dev
services:
  app:
    image: ${DOCKERHUB_USERNAME}/unicornt-store:${IMAGE_TAG}
    container_name: unicornt-dev-app
    env_file: .env
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
    ports: ["127.0.0.1:8081:8080"]            # qa: 8082 ; prod: 8088
    depends_on: { db: { condition: service_healthy } }
    restart: unless-stopped
  db:                                          # omit entirely in deploy/prod/
    image: postgres:16-alpine
    container_name: unicornt-dev-db
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes: [pgdata_dev:/var/lib/postgresql/data]   # qa: pgdata_qa
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 5s
      timeout: 3s
      retries: 5
    restart: unless-stopped
volumes:
  pgdata_dev:
```

Note: no host port published for `db` (was `5432:5432` in the current file) — keep
databases off the public interface; the app reaches `db` over the compose network.

### 7.6 nginx server block (API host)

```nginx
server {
  listen 443 ssl;
  server_name api-unicornt-dev.keber.dev;           # qa: api-unicornt-qa.keber.cl ; prod: api-unicornt-store.keber.cl
  # ssl_certificate ... (certbot-managed)
  location / {
    proxy_pass http://127.0.0.1:8081;               # qa: 8082 ; prod: 8088
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}
```

Static frontend host (`unicornt-dev.keber.dev`, `unicornt-qa.keber.cl`):
`root /var/www/unicornt-dev;` + `try_files $uri $uri/ /index.html;` (adjust for
the multipage entries: `index.html product.html login.html register.html
admin.html`).

### 7.7 Profile-file generation from templates (D2)

Committed: `application-{dev,qa,prod}.yml.example`. Gitignored: the three real
`application-{dev,qa,prod}.yml`. They are produced from the templates so
`SPRING_PROFILES_ACTIVE` can pick one at runtime.

`Dockerfile` (build stage, after `COPY src/ src/`):

```dockerfile
# Profile configs are gitignored (approval criterion). Derive all three from the
# committed *.example templates so the jar carries every profile.
RUN cd src/main/resources \
 && for p in dev qa prod; do cp "application-$p.yml.example" "application-$p.yml"; done
```

`scripts/gen-profiles.sh` (local non-Docker runs):

```sh
#!/bin/sh
set -eu
cd "$(dirname "$0")/../src/main/resources"
for p in dev qa prod; do
  [ -f "application-$p.yml" ] || cp "application-$p.yml.example" "application-$p.yml"
done
echo "profile files ready: application-{dev,qa,prod}.yml"
```

`.gitignore` — add the third line next to the existing two:

```
application-dev.yml
application-prod.yml
application-qa.yml
```

---

## 8. Open questions / notes

- The frontend's multipage build (`index/product/login/register/admin`) means
  the static host needs explicit `try_files` per page, not a blanket SPA
  fallback. Confirm the exact nginx `location` rules with the frontend routing.
- Frontend CI currently triggers on `main` / `refactor` / `etapas/**`. P6 should
  also retarget those triggers to `dev` / `qa` / `main` and prune the old ones.
- Cert scope: `unicornt-store.keber.cl` (apex) stays on GitHub Pages; the VPS
  serves `unicornt-qa.keber.cl`, `api-unicornt-qa.keber.cl` and
  `api-unicornt-store.keber.cl` — certbot issues those independently, no
  conflict with the Pages cert for the apex.
- `.keber.dev` is HSTS-preloaded — both dev hostnames are HTTPS-only from the
  first request; certbot must succeed before anything can talk to them.
- Consider GHCR later only if Docker Hub pull-rate limits bite the VPS.

---

## 9. Runbook — P3 (VPS), P4 (GitHub Environments), P8 (Supabase)

Do **P3 and P4 before merging the P2 PR into `dev`**: the merge is a push to
`dev`, which fires `deploy(dev)`; if environment `dev` has no secrets or the box
is not ready, that job fails (test + build still pass).

### 9.1 P3 — one-time VPS setup (🧑, as a sudo user on the box)

> **Superseded by what's actually on the box** — kept as the original sketch.
> Real infra: EasyEngine (containerized nginx-proxy, not host nginx/certbot);
> three `deploy-{dev,qa,prod}` users each with a `restrict,command="sudo -n
> /usr/local/sbin/deploy-unicornt-<env>"` key (not a shared `deployer`);
> `compose.yml` not `docker-compose.yml`. See the P3 status note (§4) for what's
> actually done and what's left. (d) and (e) below no longer apply as written.

**a. DNS** — at your DNS provider, `A` records → the VPS public IP:

```
unicornt-dev.keber.dev
api-unicornt-dev.keber.dev
unicornt-qa.keber.cl
api-unicornt-qa.keber.cl
api-unicornt-store.keber.cl
```

Leave `unicornt-store.keber.cl` pointing at GitHub Pages. Verify:
`dig +short api-unicornt-dev.keber.dev` → your IP.

**b. Directories & compose files**

```sh
sudo mkdir -p /opt/unicornt/{dev,qa,prod} /var/www/unicornt-dev /var/www/unicornt-qa
# from a checkout of this branch, per env:
sudo cp deploy/dev/docker-compose.yml   /opt/unicornt/dev/
sudo cp deploy/qa/docker-compose.yml    /opt/unicornt/qa/
sudo cp deploy/prod/docker-compose.yml  /opt/unicornt/prod/
# NOTE (2026-09-05): P3 was actually built differently — the deploy script
# ended up at /usr/local/sbin/deploy-unicornt-<env>, run through sudo, one file
# per env, NOT inside /opt/unicornt/<env>/. The lines below are kept as the
# historical record of what this runbook said; §7.2 has what is installed.
# sudo cp deploy/deploy.sh /opt/unicornt/dev/  && ... && sudo chmod +x ...
# real .env per env, from the templates — fill every __CHANGE_ME__:
sudo cp deploy/dev/.env.example   /opt/unicornt/dev/.env
sudo cp deploy/qa/.env.example    /opt/unicornt/qa/.env
sudo cp deploy/prod/.env.example  /opt/unicornt/prod/.env
sudo chmod 600 /opt/unicornt/*/.env
```

In each `.env`: set `DOCKERHUB_USERNAME`, a fresh `APP_JWT_SECRET`
(`openssl rand -base64 32`), the Postgres password (dev/qa) or Supabase creds
(prod). `IMAGE_TAG` is rewritten by `deploy.sh`; the seeded `latest` is only for
a manual first `docker compose up`.

**c. Bring the databases up** (dev/qa)

```sh
cd /opt/unicornt/dev && sudo docker compose up -d db && sudo docker compose ps
cd /opt/unicornt/qa  && sudo docker compose up -d db && sudo docker compose ps
sudo docker volume ls | grep pgdata      # pgdata_dev, pgdata_qa present
```

**d. Deploy user + three forced-command keys**

```sh
sudo useradd -m -s /bin/sh deployer 2>/dev/null || true
sudo mkdir -p /home/deployer/.ssh && sudo touch /home/deployer/.ssh/authorized_keys
sudo chmod 700 /home/deployer/.ssh && sudo chmod 600 /home/deployer/.ssh/authorized_keys
sudo chown -R deployer:deployer /home/deployer/.ssh
sudo usermod -aG docker deployer          # needs docker without sudo
```

On your workstation, generate one keypair per env (no passphrase):

```sh
ssh-keygen -t ed25519 -f ci-deploy-dev  -N '' -C ci-deploy-dev
ssh-keygen -t ed25519 -f ci-deploy-qa   -N '' -C ci-deploy-qa
ssh-keygen -t ed25519 -f ci-deploy-prod -N '' -C ci-deploy-prod
```

Append the **public** keys to `/home/deployer/.ssh/authorized_keys`, each pinned
to its env's script:

```
command="/opt/unicornt/dev/deploy.sh",no-port-forwarding,no-agent-forwarding,no-pty,no-X11-forwarding ssh-ed25519 AAAA...dev  ci-deploy-dev
command="/opt/unicornt/qa/deploy.sh",no-port-forwarding,no-agent-forwarding,no-pty,no-X11-forwarding ssh-ed25519 AAAA...qa   ci-deploy-qa
command="/opt/unicornt/prod/deploy.sh",no-port-forwarding,no-agent-forwarding,no-pty,no-X11-forwarding ssh-ed25519 AAAA...prod ci-deploy-prod
```

Local test (should pull + start, or fail cleanly on a bad tag):

```sh
ssh -i ci-deploy-dev -p <port> deployer@<host> "deploy sha-0000000"   # refused: bad tag -> good
```

The **private** keys go into the GitHub Environments (9.2), then delete them
locally.

**e. nginx** — five server blocks (templates in §7.6). API blocks
`proxy_pass` to `127.0.0.1:{8081,8082,8088}`; static blocks `root
/var/www/unicornt-{dev,qa}` with the multipage `try_files`. Then:

```sh
sudo certbot --nginx -d unicornt-dev.keber.dev -d api-unicornt-dev.keber.dev \
  -d unicornt-qa.keber.cl -d api-unicornt-qa.keber.cl -d api-unicornt-store.keber.cl
sudo nginx -t && sudo systemctl reload nginx
```

**f. First manual app bring-up** (optional sanity check, dev)

```sh
cd /opt/unicornt/dev
sudo sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=latest/' .env      # any pushed tag
sudo docker compose up -d && sudo docker compose logs -f app
curl -fsS https://api-unicornt-dev.keber.dev/api/v1/products | head -c 200
```

(There will be no image to pull until the first `dev` build runs — skip (f)
until after the PR merges if Docker Hub has nothing yet.)

### 9.2 P4 — GitHub Environments (🧑, backend repo → Settings)

**Repo-level** (Settings → Secrets and variables → Actions) — needed by
`resolve-target` / `build-and-push`, which have no `environment:`:

| Kind | Name | Value |
|------|------|-------|
| secret | `DOCKERHUB_USERNAME` | your Docker Hub username |
| secret | `DOCKERHUB_TOKEN` | a Docker Hub access token with write scope |

**Per environment** `dev` / `qa` / `prod` (Settings → Environments → *env*):

| Kind | Name | dev | qa | prod | status |
|------|------|-----|----|----|--------|
| secret | `DEPLOY_HOST` | VPS host | VPS host | VPS host | ✅ set |
| secret | `DEPLOY_PORT` | SSH port | " | " | ✅ set |
| secret | `DEPLOY_USER` | `deployer` | `deployer` | `deployer` | ✅ set |
| secret | `DEPLOY_SSH_KEY` | `ci-deploy-dev` private key | `ci-deploy-qa` | `ci-deploy-prod` | ✅ set |
| var | `HOST` | `api-unicornt-dev.keber.dev` | `api-unicornt-qa.keber.cl` | `api-unicornt-store.keber.cl` | ✅ set |
| var | `WEB_ORIGIN` | `https://unicornt-dev.keber.dev` | `https://unicornt-qa.keber.cl` | `https://unicornt-store.keber.cl` | ❌ **add** |
| var | `INTERNAL_PORT` | `8081` | `8082` | `8088` | ✅ set (informational) |
| var | `COMPOSE_PROJECT` | `unicornt-dev` | `unicornt-qa` | `unicornt-prod` | ✅ set (informational) |
| var | `ENVIRONMENT_NAME` | `dev` | `qa` | `prod` | ✅ set (informational) |

The workflow reads `secrets.DEPLOY_*`, `vars.HOST` (→ `https://<HOST>`) and
`vars.WEB_ORIGIN` (CORS assertion). `INTERNAL_PORT` / `COMPOSE_PROJECT` /
`ENVIRONMENT_NAME` are not consumed by CI but are handy on the box — keep them.

Protection (Settings → Environments → *env* → protection rules):

- `prod`: **Required reviewers** = you. **Deployment branches and tags** →
  *Selected* → add rule `main`.
- `qa`: Deployment branches → *Selected* → `qa`.
- `dev`: Deployment branches → *Selected* → `dev`.

`gh` one-liners for what's missing:

```sh
gh secret set DOCKERHUB_USERNAME --body '<user>'
gh secret set DOCKERHUB_TOKEN    --body '<token>'
gh variable set WEB_ORIGIN --env dev  --body 'https://unicornt-dev.keber.dev'
gh variable set WEB_ORIGIN --env qa   --body 'https://unicornt-qa.keber.cl'
gh variable set WEB_ORIGIN --env prod --body 'https://unicornt-store.keber.cl'
# prod reviewer + branch policy (replace <your-user-id>):
gh api -X PUT repos/keber/unicornt-store-backend/environments/prod -f 'deployment_branch_policy[protected_branches]=false' -f 'deployment_branch_policy[custom_branch_policies]=true'
gh api -X POST repos/keber/unicornt-store-backend/environments/prod/deployment-branch-policies -f name=main
```

### 9.3 Then

Merge the P2 PR → `dev` → watch Actions: `test → resolve-target →
build-and-push → deploy dev` (smoke step at the end). Re-enable the `dev` ruleset
first if you want the PR to exercise the checks.

### 9.4 P8 — Supabase preflight from the VPS (🧑)

A JDBC URL is not a libpq connection string. Passing
`$SPRING_DATASOURCE_URL` straight to `psql` fails with
`invalid connection option "jdbc:postgresql://..."` — the `jdbc:` prefix and
the `currentSchema=` parameter are both JDBC-only. Build a libpq *conninfo*
from the parts instead, which also sidesteps URL-encoding a password that may
contain reserved characters.

Run on the box. This keeps the password out of `argv` and out of the shell
history — it is read inside the container from the mounted `.env`, never
interpolated into the command line:

```sh
docker run --rm -v /opt/unicornt/prod/.env:/tmp/e:ro postgres:16 sh -c '
  set -a; . /tmp/e; set +a
  hostport=$(printf %s "$SPRING_DATASOURCE_URL" | sed -E "s|^jdbc:postgresql://([^/]+)/.*|\1|")
  export PGPASSWORD="$SPRING_DATASOURCE_PASSWORD"
  psql "host=${hostport%%:*} port=${hostport##*:} dbname=postgres \
        user=$SPRING_DATASOURCE_USERNAME sslmode=require" \
    -c "select version();" \
    -c "\dn" \
    -c "select current_setting(\"ssl.library\", true);"
'
```

One command, four answers: it connects at all (egress to the Supabase pooler
is not firewalled), TLS negotiates under `sslmode=require`, the credentials in
the prod `.env` are the rotated ones and still valid, and `\dn` says whether
the `unicornt_store` schema already exists.

Reading it:

- **`could not translate host name`** → DNS or egress, not credentials.
- **`connection timed out`** → outbound 5432 is blocked; check the VPS
  firewall before touching Supabase.
- **`password authentication failed`** → the rotation in §3 did not make it
  into `/opt/unicornt/prod/.env`. Note the Supabase *pooler* username is
  `postgres.<project-ref>`, not plain `postgres` — a frequent cause of this
  exact error.
- **`server does not support SSL`** → do **not** fall back to `sslmode=disable`
  against a managed pooler; re-check the host is the pooler endpoint.
- **`\dn` without `unicornt_store`** → create it before the first deploy, or
  the app starts against a schema that is not there.

If it connects but the schema is missing, snapshot first (P8.1), then create
the schema and run `V1..V3` (P8.2) — in that order.
