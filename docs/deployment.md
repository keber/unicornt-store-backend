# Deployment

## Build and packaging

```bash
mvn clean package -DskipTests
```

The executable JAR is generated at:

```
target/unicornt-store.jar
```

---

## Local execution (without Docker)

Requires the environment variables defined in [Configuration](configuration.md#environment-variables):

```bash
java -jar target/unicornt-store.jar
```

Or directly with Maven:

```bash
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`.

---

## Deployment with Docker

### 1. Create the `.env` file

```bash
cp .env-template .env
# Edit .env with the real DB connection values
```

Example `.env` for local development (MySQL on the host):

```env
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/unicornt_store?useSSL=false&serverTimezone=America/Santiago&characterEncoding=UTF-8&useUnicode=true
SPRING_DATASOURCE_USERNAME=your_user
SPRING_DATASOURCE_PASSWORD=your_password
```

> **Note:** From Docker, `localhost` points to the container, not the host. Use `host.docker.internal` to connect to host services (MySQL, PostgreSQL).

### 2. Build and start

```bash
mvn clean package -DskipTests
docker compose --env-file .env up --build -d
```

The application will be available at `http://localhost:8080`.

### 3. View logs

```bash
docker compose logs -f
```

### 4. Stop

```bash
docker compose down
```

---

## Dockerfile

```dockerfile
FROM eclipse-temurin:25-jdk-alpine
ARG JAR_FILE=target/unicornt-store.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

## docker-compose.yml

```yaml
services:
  unicornt-store:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}
      SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
    restart: always
```

The variables are injected from the `.env` file and passed to the container as environment variables.
