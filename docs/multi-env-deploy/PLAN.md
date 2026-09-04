# Multi-environment deploy — dev / qa / prod

Working plan for promoting `unicornt-store-backend` (and the matching
`unicornt-store-frontend`) through three branch-gated environments, each on its
own live URL, all sharing one VPS behind the existing nginx + SSL setup.

**Status:** in progress. D1–D6 settled (§2). §3, P0–P4 done (rulesets still
disabled, re-enable when convenient). **P5 done 2026-09-04 —
`https://unicornt-dev.keber.dev` / `https://api-unicornt-dev.keber.dev` are
live, full pipeline green.** qa prep (stale DB reset + compose networks fix)
done. **P6 code done, PR [#26](https://github.com/keber/unicornt-store-frontend/pull/26)
open** — blocked on 🧑 VPS + frontend Environment setup (P6 runbook, §4) before
it can merge/run. Next after that: **P7** (promote both repos to qa).

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
| D5 | Image tags | Deploy pins immutable `:sha-<short>`; also push a moving channel tag `:dev` / `:qa` / `:prod`; keep `:latest` = prod. |
| D6 | Registry | **Docker Hub** (`$DOCKERHUB_USERNAME/unicornt-store`), unchanged. |

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

### P6 — Frontend dev cutover (🤖 workflow, 👥 merge) — **code done, PR open**

- [x] 🤖 `static.yml` (prod/Pages): bakes `VITE_API_BASE_URL=https://api-unicornt-store.keber.cl`
      into the build; verifies it landed in `dist/assets/*.js`. (`e2e-live.yml`
      needed no change — it tests the already-deployed site, it doesn't build.)
- [x] 🤖 New `deploy-vps.yml`: push to `dev`/`qa` → lint+test+build
      (`VITE_API_BASE_URL` from `vars.API_HOST`) → same bundle check → rsync
      `dist/` to the VPS → smoke check. `environment:`-scoped like the backend.
- [x] 🤖 `gate-pr-source.yml` added to frontend (mirrors backend).
- [x] 🤖 `ci.yml` / `unit-report.yml` / `e2e.yml`: added `dev`/`qa` to
      `pull_request` triggers (additive, `refactor`/`etapas/**` untouched) so
      PRs into dev/qa get checks.
- [x] Frontend PR: [#26](https://github.com/keber/unicornt-store-frontend/pull/26) → `dev`.
- [ ] 🧑 **VPS + Environment setup needed before `deploy-vps.yml` can run** — see
      the runbook addendum right below. Nothing here touches the live site
      until this is done and the PR merges.
- [ ] 👥 Merge PR #26; watch `deploy-vps.yml` on `dev`; confirm
      `https://unicornt-dev.keber.dev` loads and talks to
      `api-unicornt-dev.keber.dev` (network tab, no CORS error).

**P6 runbook — VPS + Environment setup (🧑, mirrors P3/P4 for the backend):**

Frontend `dev`/`qa`/`prod` Environments already exist (created earlier, empty).
Needed per env (`dev`, `qa`):

- Secrets: `DEPLOY_HOST`, `DEPLOY_PORT`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`.
- Variable: `API_HOST` (`api-unicornt-dev.keber.dev` / `api-unicornt-qa.keber.cl`).
  `HOST` (the frontend's own host) already exists from earlier setup.

On the VPS — confirm the two sites' htdocs roots first (don't guess):

```sh
docker inspect unicornt-devkeberdev-nginx-1 --format '{{json .Mounts}}' | python3 -m json.tool
docker inspect unicornt-qakebercl-nginx-1   --format '{{json .Mounts}}' | python3 -m json.tool
# or, if EasyEngine's own CLI is on PATH:
ee site info unicornt-dev.keber.dev
```

Then a restricted deploy user per env, same spirit as `deploy-{dev,qa,prod}` —
`rrsync` locked to that site's htdocs root instead of a forced script:

```sh
sudo useradd -m -s /usr/sbin/nologin deploy-frontend-dev
sudo -u deploy-frontend-dev mkdir -p ~deploy-frontend-dev/.ssh
# find rrsync (ships with the rsync package, path varies by distro):
find / -name rrsync -type f 2>/dev/null
# authorized_keys entry (path = the confirmed htdocs root, trailing slash matters):
echo 'command="/usr/bin/rrsync -delete <htdocs-root>/",restrict ssh-ed25519 AAAA... github-actions:unicornt-store-frontend-dev' \
  | sudo -u deploy-frontend-dev tee ~deploy-frontend-dev/.ssh/authorized_keys
sudo chmod 700 ~deploy-frontend-dev/.ssh
sudo chmod 600 ~deploy-frontend-dev/.ssh/authorized_keys
sudo chown -R deploy-frontend-dev:deploy-frontend-dev <htdocs-root>   # deploy user must own/write it
```

Repeat for qa (`deploy-frontend-qa`, its own key, its own htdocs root). Generate
the keypairs the same way as the backend's (`ssh-keygen -t ed25519 -f ...`), put
the private half in the matching frontend GitHub Environment, delete it locally.

Local test before wiring CI: `rsync -az --delete -e "ssh -i key -p <port>" ./some-test-dir/ deploy-frontend-dev@<host>:/` should land files in the htdocs root and nowhere else — try a path traversal (`../`) and confirm rrsync refuses it.

### P7 — Promote to qa (👥)

- [ ] 👥 PR `dev → qa` in **both** repos (gate requires head = `dev`).
- [ ] Backend `qa` run deploys to `:8082`; frontend `qa` run publishes to
      `/var/www/unicornt-qa`.
- [ ] 👥 Verify `unicornt-qa.keber.cl` end to end; run
      `scripts/acceptance.sh`-style checks against the qa API.

### P8 — Promote to prod (👥)

- [ ] 🧑 Supabase: create the `unicornt_store` schema; take a snapshot/backup;
      append `&sslmode=require` to the JDBC URL if the driver needs it.
- [ ] 🧑 Decide prod schema management: keep `SQL_INIT_MODE=always` with the
      idempotent `V1..V3` scripts **or** enable Flyway first (P9). At minimum,
      run the scripts once manually against Supabase and set
      `JPA_DDL_AUTO=validate`.
- [ ] 🧑 Fill `/opt/unicornt/prod/.env` with Supabase creds + fresh
      `APP_JWT_SECRET` + `APP_CORS_ALLOWED_ORIGINS=https://unicornt-store.keber.cl`.
- [ ] 👥 PR `qa → main` (gate requires head = `qa`). The `prod` environment
      pauses for your manual approval.
- [ ] 👥 Approve; watch `deploy(prod)` → `:8088`; smoke job asserts
      `https://api-unicornt-store.keber.cl/api/v1/products` 200 + CORS =
      `https://unicornt-store.keber.cl`.
- [ ] 👥 Confirm the Pages site (`unicornt-store.keber.cl`) still builds with
      the prod `VITE_API_BASE_URL` and reaches the new API host.
- [ ] 🧑 Retire the old single-target deploy path once prod is green.

### P9 — Hardening (later, not blocking)

- [ ] Add `org.springframework.boot:spring-boot-flyway`; set
      `FLYWAY_ENABLED=true`, `SQL_INIT_MODE=never`; convert `V1..V3` to Flyway
      migrations. Prod stops mutating schema on boot.
- [ ] `deploy.resources` limits in each compose file so dev/qa load cannot
      starve prod on the shared box.
- [ ] Nightly `pg_dump` of the dev/qa volumes; rely on Supabase PITR for prod.
- [ ] Uptime check / alert on the three `/api/v1/products` endpoints.
- [ ] `docker image prune` already in `deploy.sh`; add a weekly cron for
      dangling volumes/build cache.
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

- **Bad backend deploy:** `ssh deploy-<env>@host` isn't interactive; instead
  re-run the previous good workflow run, or `gh workflow run` with an older SHA,
  or on the box `sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=sha-OLD/' .env && docker
  compose up -d`.
- **Bad frontend deploy (dev/qa):** re-run the previous frontend run; rsync
  overwrites `/var/www/unicornt-<env>`.
- **Bad prod frontend (Pages):** re-run the previous `static.yml` run (single
  orphan commit on `gh-pages`).
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

### 7.2 `/opt/unicornt/<env>/deploy.sh` (forced command, one per env)

```sh
#!/bin/sh
set -eu
# authorized_keys: command="/opt/unicornt/dev/deploy.sh",no-port-forwarding,no-agent-forwarding,no-pty ssh-ed25519 AAAA...
TAG="${SSH_ORIGINAL_COMMAND##* }"          # last whitespace-separated token
case "$TAG" in
  sha-[0-9a-f]*) : ;;
  *) echo "refusing tag '$TAG' (want sha-<hex>)" >&2; exit 1 ;;
esac
cd /opt/unicornt/dev                        # <- per-env dir; the key can touch nothing else
sed -i "s|^IMAGE_TAG=.*|IMAGE_TAG=${TAG}|" .env
docker compose pull
docker compose up -d
docker image prune -f
```

CI calls: `ssh -i key -p $PORT $USER@$HOST "deploy sha-abc1234"`.

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

## 9. Runbook — P3 (VPS) & P4 (GitHub Environments)

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
sudo cp deploy/deploy.sh /opt/unicornt/dev/  && sudo cp deploy/deploy.sh /opt/unicornt/qa/  && sudo cp deploy/deploy.sh /opt/unicornt/prod/
sudo chmod +x /opt/unicornt/*/deploy.sh
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
