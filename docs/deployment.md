# Deployment

## Build and packaging

```bash
./mvnw -DskipTests clean package
```

The executable JAR is generated at `target/app.jar` (`<finalName>app</finalName>`
in `pom.xml`, so the Dockerfile does not depend on the project version).

## Local execution (without Docker)

Requires a running PostgreSQL instance and the environment variables from
[configuration.md](configuration.md#environment-variables). Neither `java -jar`
nor `./mvnw` reads `.env` automatically — load it into the shell first (see the
root [README](../README.md#option-b--app-on-the-host-database-in-a-container)
for the bash and PowerShell snippets), then:

```bash
java -jar target/app.jar
```

Or directly with Maven:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The application is available at `http://localhost:8080`.

## Deployment with Docker Compose

### 1. Create the `.env` file

```bash
cp .env.example .env
# edit .env with real values, including a generated APP_JWT_SECRET
```

### 2. Start the stack

```bash
docker compose up -d --build
```

This starts two services:

- `db` — `postgres:16-alpine`, with a named volume (`postgres_data`) so data
  survives a container restart, and a `pg_isready` healthcheck.
- `app` — built from the local multi-stage `Dockerfile`, waits for `db` to report
  healthy before starting.

The application is available at `http://localhost:8080`.

### 3. Logs and shutdown

```bash
docker compose logs -f
docker compose down        # keeps the volume
docker compose down -v     # also deletes the persisted database
```

## Dockerfile

Multi-stage build: the first stage compiles `target/app.jar` with the Maven
wrapper on `eclipse-temurin:25-jdk`; the second stage copies only the jar into
`eclipse-temurin:25-jre` and runs it as a non-root user (`unicornt`). See the
[repository Dockerfile](../Dockerfile) for the exact steps.

## docker-compose.yml

See the [repository docker-compose.yml](../docker-compose.yml). Both services
read every credential from `.env`; nothing is hardcoded.
