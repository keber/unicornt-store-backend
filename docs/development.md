# Development

## Project structure

```
unicornt-store-backend/
├── pom.xml                     # Spring Boot 4.0.8, Java 25, dependencies frozen after T0
├── Dockerfile                  # multi-stage build, eclipse-temurin:25
├── docker-compose.yml          # app + PostgreSQL 16, persistent volume
├── .env.example                # placeholder environment variables
├── mvnw / mvnw.cmd              # Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/unicornt/store/
│   │   │   ├── StoreApplication.java
│   │   │   ├── domain/
│   │   │   │   ├── service/         business use cases
│   │   │   │   └── exception/       ResourceNotFoundException, OutOfStockException, ...
│   │   │   └── infrastructure/
│   │   │       ├── persistence/entity/       @Entity classes
│   │   │       ├── persistence/repository/   Spring Data JpaRepository interfaces
│   │   │       ├── security/                 JWT filter, SecurityConfig
│   │   │       ├── config/                   OpenApiConfig
│   │   │       └── web/
│   │   │           ├── rest/        @RestController classes
│   │   │           ├── dto/         request/response records
│   │   │           ├── mapper/      entity <-> DTO translation
│   │   │           └── error/       ErrorResponse, GlobalExceptionHandler
│   │   └── resources/
│   │       ├── application.yml           base profile, secure by default
│   │       ├── application-dev.yml       schema auto-sync, Swagger enabled
│   │       ├── application-prod.yml      schema validated only, Swagger disabled
│   │       └── db/migration/             versioned SQL schema and seed data
│   └── test/
│       ├── java/com/unicornt/store/      unit tests, MockMvc slices, security tests
│       └── resources/application.properties   H2 datasource for tests
└── docs/
    ├── bruno/unicornt-store/    Bruno API collection
    └── refactor/                Milestone 4 refactor task files (not part of the deliverable)
```

`domain` has no dependency on Spring MVC, `jakarta.servlet` or `jakarta.persistence`
annotations — business rules do not know they run behind HTTP or JPA.

## Running the tests

```bash
./mvnw test       # unit tests and MockMvc slices, no external services required
./mvnw verify      # same, plus the packaging step CI runs
```

Tests run against in-memory H2 in PostgreSQL compatibility mode
(`src/test/resources/application.properties`), so they do not require Docker or a
running PostgreSQL instance. A few worker branches used Testcontainers-backed
tests during the refactor to validate JPQL and startup against a real
PostgreSQL container; `org.testcontainers:postgresql` is on the test classpath
for that purpose.

## Local development loop

```bash
cp .env.example .env
docker compose up -d db          # PostgreSQL only, app runs from the IDE/Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

`dev` keeps the schema in sync with `ddl-auto: update` and exposes Swagger UI at
`http://localhost:8080/swagger-ui.html`.
