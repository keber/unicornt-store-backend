# Multi-environment deploy — dev / qa / prod

Working plan for promoting `unicornt-store-backend` (and the matching
`unicornt-store-frontend`) through three branch-gated environments, each on its
own live URL, all sharing one VPS behind the existing nginx + SSL setup.

**Status:** in progress. D1–D6 settled (§2). §3 secrets hygiene done. P0–P1 done
bar re-enabling rulesets. **P2 code done on branch `ci/multi-env-pipeline`,
awaiting PR → `dev`.** Next: P3 (VPS) + P4 (Environments) — both 🧑, runbook below.

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
- [x] `deploy/deploy.sh` — one script, sits in each `/srv/unicornt/<env>/`, acts
      on its own dir; validates the `sha-` tag from `$SSH_ORIGINAL_COMMAND`.
- [x] `deploy/{dev,qa,prod}/docker-compose.yml` + `.env.example` — dev/qa carry a
      Postgres container (no published port), prod is app-only (Supabase).
- [x] `docs/{deployment,configuration,development}.md` updated.

### P3 — VPS infrastructure (🧑 on the box; 🤖 supplies configs)

- [ ] 🧑 DNS `A` records → VPS IP: `unicornt-dev.keber.dev`,
      `api-unicornt-dev.keber.dev`, `unicornt-qa.keber.cl`,
      `api-unicornt-qa.keber.cl`, `api-unicornt-store.keber.cl`.
      (`unicornt-store.keber.cl` stays pointed at GitHub Pages.)
- [ ] 🧑 Create server dirs:
      `/srv/unicornt/{dev,qa,prod}/` each with `docker-compose.yml` (from
      `deploy/<env>/`) + `.env` (secrets, D3 profile, `IMAGE_TAG=` line).
- [ ] 🧑 Bring up the dev + qa Postgres containers, confirm named volumes
      `pgdata_dev` / `pgdata_qa` exist and survive `docker compose down`.
- [ ] 🧑 nginx server blocks (§7.6) — one per hostname:
  - `api-unicornt-dev.keber.dev` → `proxy_pass http://127.0.0.1:8081`
  - `unicornt-dev.keber.dev` → `root /var/www/unicornt-dev` (static)
  - `api-unicornt-qa.keber.cl` → `127.0.0.1:8082`
  - `unicornt-qa.keber.cl` → `root /var/www/unicornt-qa` (static)
  - `api-unicornt-store.keber.cl` → `127.0.0.1:8088`
- [ ] 🧑 `certbot --nginx` for the five VPS-served names (a `keber.dev` wildcard
      cert, if you have one, may already cover the two `.keber.dev` names).
- [ ] 🧑 Create **three** deploy SSH keys, one per env; each `authorized_keys`
      entry uses `command="/srv/unicornt/<env>/deploy.sh",no-port-forwarding,...`
      pointing at that env's `deploy.sh` (§7.2).
- [ ] 🧑 `mkdir -p /var/www/unicornt-dev /var/www/unicornt-qa`; give the
      frontend deploy key write access (rsync target).

### P4 — GitHub Environments & secrets (🧑)

Backend repo → Settings → Environments: `dev`, `qa`, `prod`.

- [ ] Per environment secrets: `DEPLOY_HOST`, `DEPLOY_PORT`, `DEPLOY_USER`,
      `DEPLOY_SSH_KEY` (that env's private key).
- [ ] Per environment variables: `API_BASE_URL` (for the smoke job),
      `FRONTEND_ORIGIN` (for the CORS assertion).
- [ ] `prod` environment: **required reviewer = you**; deployment branch rule =
      `main` only. `qa` → `qa` only. `dev` → `dev` only.
- [ ] Leave `DOCKERHUB_USERNAME/TOKEN`, `SONAR_TOKEN`, `CODECOV_TOKEN`,
      `GIST_*` as repo-level secrets.
- [ ] Frontend repo Environments `dev` / `qa` / `prod`: `VPS_HOST`, `VPS_USER`,
      `VPS_SSH_KEY` (dev/qa only), and variable `VITE_API_BASE_URL`:
      `https://api-unicornt-dev.keber.dev` (dev) ·
      `https://api-unicornt-qa.keber.cl` (qa) ·
      `https://api-unicornt-store.keber.cl` (prod, used by the Pages build).

### P5 — Backend dev cutover (👥)

- [ ] 👥 Merge the P2 PR into `dev`.
- [ ] 👥 Watch the `dev` run: `test → build-and-push → deploy(dev) → smoke`.
- [ ] 🧑 On the box: `docker compose -p unicornt-dev ps`, check logs, hit
      `https://api-unicornt-dev.keber.dev/api/v1/products` → 200.
- [ ] Smoke job green (API 200 + `Access-Control-Allow-Origin` matches
      `https://unicornt-dev.keber.dev`).

### P6 — Frontend dev cutover (🤖 workflow, 👥 merge)

- [ ] 🤖 Frontend: add `VITE_API_BASE_URL` to the Pages build in `static.yml`
      (prod value) and to the live-E2E build.
- [ ] 🤖 Frontend: new `deploy-vps.yml` — on push to `dev` build with the dev
      `VITE_API_BASE_URL` and rsync `dist/` → `/var/www/unicornt-dev`; on push
      to `qa` → `/var/www/unicornt-qa`.
- [ ] 🤖 Frontend post-build check: `grep` the expected base URL in
      `dist/assets/*.js`; fail if absent.
- [ ] 👥 Merge to frontend `dev`; confirm `https://unicornt-dev.keber.dev`
      loads and talks to `api-unicornt-dev.keber.dev` (network tab, no CORS
      error).

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
- [ ] 🧑 Fill `/srv/unicornt/prod/.env` with Supabase creds + fresh
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
| DNS records, certbot, `/srv/unicornt/*`, DB containers, SSH deploy keys, `/var/www/*` | — | ✅ |
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

### 7.2 `/srv/unicornt/<env>/deploy.sh` (forced command, one per env)

```sh
#!/bin/sh
set -eu
# authorized_keys: command="/srv/unicornt/dev/deploy.sh",no-port-forwarding,no-agent-forwarding,no-pty ssh-ed25519 AAAA...
TAG="${SSH_ORIGINAL_COMMAND##* }"          # last whitespace-separated token
case "$TAG" in
  sha-[0-9a-f]*) : ;;
  *) echo "refusing tag '$TAG' (want sha-<hex>)" >&2; exit 1 ;;
esac
cd /srv/unicornt/dev                        # <- per-env dir; the key can touch nothing else
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
sudo mkdir -p /srv/unicornt/{dev,qa,prod} /var/www/unicornt-dev /var/www/unicornt-qa
# from a checkout of this branch, per env:
sudo cp deploy/dev/docker-compose.yml   /srv/unicornt/dev/
sudo cp deploy/qa/docker-compose.yml    /srv/unicornt/qa/
sudo cp deploy/prod/docker-compose.yml  /srv/unicornt/prod/
sudo cp deploy/deploy.sh /srv/unicornt/dev/  && sudo cp deploy/deploy.sh /srv/unicornt/qa/  && sudo cp deploy/deploy.sh /srv/unicornt/prod/
sudo chmod +x /srv/unicornt/*/deploy.sh
# real .env per env, from the templates — fill every __CHANGE_ME__:
sudo cp deploy/dev/.env.example   /srv/unicornt/dev/.env
sudo cp deploy/qa/.env.example    /srv/unicornt/qa/.env
sudo cp deploy/prod/.env.example  /srv/unicornt/prod/.env
sudo chmod 600 /srv/unicornt/*/.env
```

In each `.env`: set `DOCKERHUB_USERNAME`, a fresh `APP_JWT_SECRET`
(`openssl rand -base64 32`), the Postgres password (dev/qa) or Supabase creds
(prod). `IMAGE_TAG` is rewritten by `deploy.sh`; the seeded `latest` is only for
a manual first `docker compose up`.

**c. Bring the databases up** (dev/qa)

```sh
cd /srv/unicornt/dev && sudo docker compose up -d db && sudo docker compose ps
cd /srv/unicornt/qa  && sudo docker compose up -d db && sudo docker compose ps
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
command="/srv/unicornt/dev/deploy.sh",no-port-forwarding,no-agent-forwarding,no-pty,no-X11-forwarding ssh-ed25519 AAAA...dev  ci-deploy-dev
command="/srv/unicornt/qa/deploy.sh",no-port-forwarding,no-agent-forwarding,no-pty,no-X11-forwarding ssh-ed25519 AAAA...qa   ci-deploy-qa
command="/srv/unicornt/prod/deploy.sh",no-port-forwarding,no-agent-forwarding,no-pty,no-X11-forwarding ssh-ed25519 AAAA...prod ci-deploy-prod
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
cd /srv/unicornt/dev
sudo sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=latest/' .env      # any pushed tag
sudo docker compose up -d && sudo docker compose logs -f app
curl -fsS https://api-unicornt-dev.keber.dev/api/v1/products | head -c 200
```

(There will be no image to pull until the first `dev` build runs — skip (f)
until after the PR merges if Docker Hub has nothing yet.)

### 9.2 P4 — GitHub Environments (🧑, backend repo → Settings → Environments)

Create `dev`, `qa`, `prod`. For each:

| Kind | Name | dev | qa | prod |
|------|------|-----|----|----|
| secret | `DEPLOY_HOST` | VPS host | VPS host | VPS host |
| secret | `DEPLOY_PORT` | SSH port | " | " |
| secret | `DEPLOY_USER` | `deployer` | `deployer` | `deployer` |
| secret | `DEPLOY_SSH_KEY` | `ci-deploy-dev` private key | `ci-deploy-qa` | `ci-deploy-prod` |
| var | `API_BASE_URL` | `https://api-unicornt-dev.keber.dev` | `https://api-unicornt-qa.keber.cl` | `https://api-unicornt-store.keber.cl` |
| var | `FRONTEND_ORIGIN` | `https://unicornt-dev.keber.dev` | `https://unicornt-qa.keber.cl` | `https://unicornt-store.keber.cl` |

- `prod`: add **Required reviewers** = you; **Deployment branches** = selected →
  `main`. `qa` → selected → `qa`. `dev` → selected → `dev`.
- `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` stay **repo-level** secrets (already
  set) — `build-and-push` has no `environment:`.
- After copying each private key into GitHub, shred the local files.

### 9.3 Then

Merge the P2 PR → `dev` → watch Actions: `test → resolve-target →
build-and-push → deploy dev` (with the smoke step). Re-enable the `dev` ruleset
first if you want the PR to exercise the checks.
