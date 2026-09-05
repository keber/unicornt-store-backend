# Operational lessons — multi-env deploy rollout

Patterns and gotchas found while wiring dev/qa/prod CI+deploy for
unicornt-store-backend and unicornt-store-frontend (2026-09-04/05). These
generalize beyond this project — worth checking against before repeating the
same debugging session elsewhere. Narrative/history of *this* rollout lives in
[PLAN.md](PLAN.md); this file is just the distilled, reusable part.

## GitHub Actions

1. **A step output containing a secret's value is silently dropped, not
   masked.** If a `run:` step does `echo "x=${{ secrets.FOO }}something" >>
   "$GITHUB_OUTPUT"`, Actions detects the secret substring and skips setting
   that output entirely (log line: `Skip output '...' since it may contain
   secret`) — no error, the output is just empty downstream. This is subtly
   different from log masking (`***`), which still lets the value flow. Fix:
   never route a secret-derived string through a job/step *output*; interpolate
   `${{ secrets.X }}` directly at the point of use (a `with:`/`env:` field),
   where it's masked in logs but still resolves correctly.

2. **Workflow-level and job-level `concurrency:` resolving to the identical
   group string is a self-deadlock**, not a no-op. GitHub cancels the job
   before it ever gets a runner ("Canceling since a deadlock was detected for
   concurrency group '...' between a top level workflow and '...'") — shows as
   0ms runner duration, zero steps executed, easy to misread as an environment
   -protection block or a scheduling failure. Fix: make the two groups
   different strings, or only set concurrency at one level.

3. **A required status check needs an exact, existing context name.** Creating
   a branch ruleset's `required_status_checks` rule with a context that no
   workflow in that repo ever posts (e.g. copied from a sibling repo with
   differently-named jobs) doesn't error at creation time — it just makes the
   branch permanently unmergeable the moment the ruleset is active, since the
   check can never be satisfied. This can sit live and undetected for a long
   time if nothing has tried to merge against that branch since the rule was
   set. Before enabling/trusting a ruleset, list its actual required contexts
   (`gh api repos/{o}/{r}/rulesets/{id}`) and diff them against real job names
   in the workflows that run on that branch.

4. **Check runs are keyed by commit SHA + context name, not by PR.** If the
   same commit is the head of two open PRs with different base branches (e.g.
   a stray `dev → main` PR sitting alongside a proper `dev → qa` one), a
   check's pass/fail recorded in one PR's context can be *displayed* on the
   other PR too, even though it's semantically about the wrong base. Don't
   trust the checks list alone to mean "this PR can merge" — check
   `mergeStateStatus`/`mergeable` on the PR itself, which reflects the actual
   ruleset evaluation.

5. **`gh secret set`/similar is treated as a sensitive action by permission
   classifiers** even for a repo you administer — expect it to prompt or be
   asked for confirmation each time; don't build automation that assumes it's
   silent.

14. **"Require branches to be up to date before merging" is wrong for a
    promotion train, and it fails closed.** The rule demands the head branch
    contain the base branch's tip — which fits `feature → trunk`, where the
    head is derived from the base. In a `dev → qa → main` train the
    dependency runs the other way: every promotion puts a new commit on the
    base that the head will never have, so from the *second* promotion onward
    every promotion PR is permanently "out of date". Worse, the usual escapes
    are typically closed by the rules you enabled alongside it — "Update
    branch" wants to push a merge commit (blocked by required linear history)
    or force-push a rebase (blocked by non-fast-forward), and a squash
    back-merge does not satisfy it because the check compares ancestry, not
    content. Enable it on the branch feature work targets; leave it off the
    promotion targets.

15. **A `PUT` on a GitHub ruleset is a full replace, not a patch.** Editing one
    flag means reading the live ruleset, stripping the read-only fields
    (`id`, `source`, `node_id`, `created_at`, `updated_at`, `_links`,
    `current_user_can_bypass`) and sending the whole `rules` array back. Send a
    partial body and the rules you omitted are silently gone — on a branch you
    thought was protected. Snapshot every ruleset to a file before touching
    any of them; the snapshot is also the rollback.

16. **`require_extra_approval_for_unattributed_changes` can deadlock a
    single-maintainer repo.** With `required_approving_review_count: 0` it
    looks free, but any commit GitHub cannot attribute to a user account (an
    Action that commits, an author email not linked to an account) silently
    demands one approval — and GitHub does not permit approving your own PR.
    With no bypass actors configured, the only way out is editing the ruleset
    in Settings. Worth knowing before it fires mid-release.

## SSH / server hardening

6. **A directory missing its execute bit breaks SSH key auth with no useful
   client- or server-side trace.** `~/.ssh` at `drw-------` (600, no `x`)
   instead of `700` silently prevents the directory from being traversed —
   sshd can't read `authorized_keys` inside it, and the failure looks
   identical to "wrong key" or "wrong user" (`Permission denied (publickey)`
   client-side; often *nothing* in `auth.log`/`journalctl -u ssh` for the
   attempt, because it never got far enough to log a `Failed publickey` line).
   When key auth fails for a reason that isn't showing up in server logs at
   all, check directory permissions before anything else — `mkdir -m 700`
   at creation time is cheaper than debugging this after the fact.

7. **`sudo -n` needs an explicit `NOPASSWD:` grant; there is no default.**
   "sudo: a password is required" from a forced SSH command means the
   sudoers rule is missing or doesn't match the exact command path — it does
   not mean the SSH key or user is wrong. Diagnose with `sudo -l -U <user>`
   (lists exactly what that user is allowed to run) before assuming the auth
   layer is broken.

8. **The SSH `restrict,command="..."` forced-command pattern is worth the
   setup cost.** A key pinned to `restrict,command="/path/to/one/script"` in
   `authorized_keys` can do exactly one thing regardless of what the client
   sends over the wire — CI's SSH invocation can carry a descriptive argument
   purely for log readability, safe in the knowledge the server ignores it.
   Cheap, effective privilege minimization for a deploy trigger.

9. **A git working copy used as a webroot must have `.git/` blocked by the
   webserver**, or the entire repo history (including anything ever committed,
   not just the current tree) is servable over plain HTTP. Verify with
   `curl -sI https://host/.git/HEAD` — must not be 200. If another site on the
   same box already does this pattern successfully, its config is the fastest
   answer, not a fresh design.

## Process / workflow

10. **A script "working" (exit 0, sensible-looking output) doesn't mean it's
    doing the right thing.** A deploy script copy-pasted from a sibling
    service and adapted in a hurry ran clean end-to-end (`docker compose pull
    && up -d`, valid compose file, correct exit code) while silently
    redeploying the *wrong* service (the backend's compose file, not the
    frontend's static assets) — because only the *filename* was adapted, not
    the body. When adapting a proven script for a new purpose, read the whole
    body against the new target before wiring it to anything live, not just
    confirm it runs.

11. **Verify each infra step locally before spending a CI round-trip on it.**
    A local `ssh -i key user@host "cmd"` or `sudo -u user sudo -n script`
    dry-run gives the same signal as a full GitHub Actions run in seconds
    instead of minutes, and isolates "server-side problem" from "GitHub-side
    problem" immediately. Every SSH-key/sudoers issue this rollout hit was
    diagnosed faster locally than by re-triggering CI and reading logs after
    the fact.

12. **When multiple partial setup attempts accumulate on a server** (e.g. a
    user created three variously-named/wired accounts for the same purpose
    across different sessions), the fast path is usually to pick one canonical
    name and delete the others outright, not to reconcile/rename in place —
    less state to reason about, and stray unused accounts with SSH keys
    installed are exactly the kind of thing that becomes a forgotten
    backdoor if left "just in case."

13. **A branch-protection ruleset blocking your own direct push is a feature,
    not friction** — if it fires on you mid-session (e.g. a docs-only commit
    rejected because the ruleset you just enabled now applies to you too),
    that's confirmation the protection works; route through a
    branch+PR+merge instead of finding a way around it.
