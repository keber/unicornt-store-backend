# T6 — Deliverable and end-to-end verification

**Runs:** after T5. Sequential.
**Branch:** directly on `refactor/hito4`, or a short-lived `refactor/h4-deliverable`
**Covers:** plan stage 8
**Rubric:** the deliverable checklist

Read [../CONVENTIONS.md](../CONVENTIONS.md) first. Everything you write here is in
English — README, docs, CI, script comments.

---

## Tasks

1. **Rewrite `README.md`.** It must let someone with no prior knowledge bring the
   service up. Sections:
   - what the microservice is, and the stack: Java 25, Spring Boot 4.0.8, Spring
     Security with JWT, Spring Data JPA, PostgreSQL 16, Docker Compose,
     springdoc-openapi
   - architecture: the `domain` / `infrastructure` package tree
   - getting started:
     ```bash
     cp .env.example .env      # fill in the placeholders
     docker compose up -d      # PostgreSQL with a persistent volume
     ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
     ```
   - documentation URLs: `http://localhost:8080/swagger-ui.html` and
     `http://localhost:8080/api-docs`
   - the endpoint table with the role required for each
   - how to generate `APP_JWT_SECRET`: `openssl rand -base64 32`
   - a security note: Swagger is disabled outside `dev`, and credentials only ever
     come from the environment
   - a pointer to the Bruno collection

2. **Bruno collection in `docs/`.** Folders for Auth, Products, Cart, Orders and
   Addresses. `{{baseUrl}}` and `{{token}}` as environment variables, and a
   post-response script on login that stores the token. No real credential in the
   committed collection — placeholders only.

3. **`.github/workflows/`** — a CI workflow running `./mvnw -B verify` on push and
   pull request, on JDK 25 (`actions/setup-java` with `temurin` 25) and with the
   Maven cache enabled. The repository already has `.github/workflows/main.yml`;
   update it rather than adding a parallel workflow, and delete the stale
   `.github/modernize/**` artifacts left over from the Java upgrade run.

4. **Acceptance script** at `scripts/acceptance.sh`, the E2E sequence from plan
   stage 8: compose up, package, run in `dev`, obtain a token, then assert
   `products list 200`, `create without token 401`, `create as ADMIN 201`,
   `swagger dev 200`. `set -e` at the top so a failure actually fails.

5. **Secret sweep.**

   ```bash
   git log -p baseline-hito3..HEAD | grep -inE "password|secret|supabase|apikey" 
   grep -rniE "password|secret" --include=*.yml --include=*.yaml --include=*.xml \
     --include=*.md . --exclude-dir=.git --exclude-dir=target
   ```

   Every hit must be a placeholder, an environment-variable reference, or prose. If a
   real credential exists anywhere in the history reachable from this branch, stop and
   report it to the orchestrator — the remedy (`git filter-repo`, or a fresh repository
   from a squashed branch) is the orchestrator's decision, not yours.

6. **Repository tidy-up.** The reference projects `actividad_m6_l5/` and
   `demoApiRest/` and the file `REFACTOR-UNICORNT-HITO4.md.bak` are untracked working
   material. Confirm they are not staged, and add them to `.gitignore` if they are to
   stay in the working tree.

7. **Rubric checklist.** Walk the C1 / C2 / C3 / deliverable checklist at the end of
   [REFACTOR-UNICORNT-HITO4.md](../../../REFACTOR-UNICORNT-HITO4.md) and record, for
   each box, the command or URL that proves it. Write the result to
   `docs/refactor/handoffs/rubric-evidence.md`. State any unmet box as unmet — do not
   round up.

## Definition of Done

```bash
./mvnw -B verify              # green
bash scripts/acceptance.sh    # every assertion passes
```

The README instructions work verbatim on a clean clone, and
`docs/refactor/handoffs/rubric-evidence.md` accounts for every rubric box.

Suggested commits:

```
docs: rewrite the README for the REST microservice
docs: add the Bruno API collection
ci: run mvn verify on push and pull request
test: add the end-to-end acceptance script
```
