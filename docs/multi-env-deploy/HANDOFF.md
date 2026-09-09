# Handoff — multi-env deploy: all three environments are live

**Written:** 2026-09-07, replacing the 2026-09-05 handoff, which was written
before P8 and told its reader to resume at a phase that is now finished.

**Read in this order:** this file → [PLAN.md](PLAN.md) §4 for what is left →
[LESSONS.md](LESSONS.md) when something smells like a repeat of a problem
someone already paid for. LESSONS is the highest-value file per minute spent;
several entries describe failures that took hours to diagnose and seconds to
recognise the second time.

## One-paragraph state of the world

**The rollout is done.** Backend and frontend both deploy automatically on push
to `dev`/`qa`/`main`, gated by a promotion-path check and, on the backend, by a
deployment chain (`qa` needs a green `dev` deploy, `main` needs a green `qa`
one). Prod pauses for manual approval. `unicornt-store.keber.cl` serves the
backend-integrated storefront against `api-unicornt-store.keber.cl`, backed by
Supabase. Everything below is either hardening, or the handful of loose ends
listed under "What is left".

## Current live state

| Env | Frontend | Backend API | Docs |
|---|---|---|---|
| dev | `unicornt-dev.keber.dev` | `api-unicornt-dev.keber.dev` | `/swagger-ui.html` ✅ |
| qa | `unicornt-qa.keber.cl` | `api-unicornt-qa.keber.cl` | **broken — see below** |
| prod | `unicornt-store.keber.cl` (GitHub Pages) | `api-unicornt-store.keber.cl` | disabled by requirement |

Prod's database is Supabase. Its schema was rebuilt on 2026-09-05 from
`V1 → V3 → V2`; the previous milestone's data survives untouched in the
schema `unicornt_store_legacy`. Backups live in `/root/unicornt-backups/`
(mode 600). The OpenAPI spec is at **`/api-docs`**, not `/v3/api-docs` — dev
and qa override springdoc's default path, and the default returns the API's
own `ENDPOINT_NOT_FOUND`, which sends everyone to the wrong URL first.

## What is left

### Do this first — qa's Swagger UI is still broken

Dev was fixed; qa was not. The block to install is in
[`deploy/nginx/api-unicornt-qa.keber.cl.user.conf`](../../deploy/nginx/api-unicornt-qa.keber.cl.user.conf)
— append its `location ^~ /swagger-ui/` section to
`/opt/easyengine/sites/api-unicornt-qa.keber.cl/config/nginx/custom/user.conf`
and `ee site reload api-unicornt-qa.keber.cl`. Verify with
`curl -sI https://api-unicornt-qa.keber.cl/swagger-ui/swagger-ui.css` → 200. A
404 whose `Content-Type` is `text/html` means the block is not being hit; see
LESSONS #21.

### Three decisions nobody has made

1. **`e2e.yml` on the frontend** — still active, still runs the stale Playwright
   suite on every PR, still goes red after ~30 minutes. `e2e-live.yml` is
   already disabled and the `e2e` required check already removed, so this is the
   last piece. Disable it (`gh workflow disable e2e.yml -R
   keber/unicornt-store-frontend`) or accept permanent red. A check that is
   always red trains people to ignore the checks panel.
2. **Frontend merge methods contradict D7.** Backend rulesets allow `squash`
   only; the frontend still allows `merge` and `rebase` on dev/qa and `merge` on
   main. Either align it or amend D7 — right now the convention is enforced on
   one repo and aspirational on the other.
3. **`require_extra_approval_for_unattributed_changes` is armed on all six
   rulesets** with `required_approving_review_count: 0` and no bypass actors.
   Harmless today because commits are attributed, but any commit GitHub cannot
   attribute (an Action that commits, an unlinked author email) demands one
   approval that a solo maintainer cannot give — GitHub forbids self-approval.
   See LESSONS #16. Decide whether to disarm it or accept the trap knowingly.

### P9 hardening — ten items, none blocking

Listed with their reasoning in [PLAN.md](PLAN.md) §4 "P9". The two most
consequential: **Flyway**, so prod stops depending on hand-run SQL; and
**`permissions:` blocks** on both workflows — where the plan warns *not* to
accept the suggested fix for `main.yml`, because it proposes `contents: read`
while `publish-reports` needs `contents: write` to push to `gh-pages`.

The **E2E suite maintenance** is its own workstream. Re-enabling both `e2e.yml`
and `e2e-live.yml` is its final step, not a separate chore.

## Operating notes

- **No agent in this project has ever had VPS shell access.** Every server-side
  command was handed to the user as a copy-paste block and its output pasted
  back. Continue that. It works well, and it forces you to write commands that
  are readable and verifiable rather than clever.
- `gh` is authenticated as `keber` with admin on both repos. **Ruleset and
  secret writes are blocked by the permission classifier** — build the payload,
  hand over the `PUT`. Expect that, do not fight it (LESSONS #5).
- Local checkouts: backend at the working directory, frontend at the sibling
  `../unicornt-store-frontend`.
- **Branch divergence numbers lie.** `main...qa` reports qa "ahead 15" and the
  count grows with every promotion. That is squash-merge accounting — a squash
  never makes the source commits ancestors of the target — not un-promoted work.
  Compare *files* (`gh api .../compare/A...B --jq '.files[].filename'`) to see
  what is genuinely different. D9 exists because this interacts badly with
  "require branches up to date".
- Real un-promoted content does exist: backend `dev` carries the documentation
  and chore work from PRs #11–#20 that has not reached `qa` or `main`. Per D8 it
  rides along with the next code change rather than driving its own promotion.
- Stale merged branches, safe to delete whenever: `ci/multi-env-pipeline`,
  `docs/p7-progress`, `docs/p7-done`, `docs/p8-handoff`, `final-delivery`,
  `fix/S1186-*`, `fix/S5778-*`, `keber-patch-{1,2,3}`,
  `patch/remove-refactor-docs`, `refactor/hito4`, `test/coverage-boost`, and on
  the frontend `ci/dev-qa-deploy`.
- `docs/multi-env-deploy/debug.log` is unrelated Chromium crash-reporter noise.
  Leave it.

## The four traps that cost the most time

Full versions in [LESSONS.md](LESSONS.md); these are the ones most likely to
bite again in this project specifically.

1. **Diagnose by who answers, not by the status code** (#21). A 404 from the
   proxy and a 404 from the app look identical in a status line and completely
   different in the headers. Comparing them settles proxy-vs-app in one command.
   Two application-side theories were pursued before that was run.
2. **A repo artifact that describes a system drifts silently** (#10, and the
   Bruno collection). `deploy.sh`, the compose files and the Bruno requests all
   described things that were no longer true. Nothing failed, because nothing
   connected them to reality. Prefer artifacts that are executed by CI.
3. **A secret written through the web UI can silently not save** (#18). An MFA
   prompt on a phone went unanswered; the form looked submitted; the value never
   changed; a re-run "with the new secret" re-tested the old one. Verify
   `updated_at`, or pipe the file into `gh secret set`.
4. **A `.env` value containing `&` breaks every script that sources the file**
   (#20) while remaining valid for Docker Compose. A working container proves
   nothing about whether your scripts can read the same file.
