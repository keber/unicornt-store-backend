# Development

## Project structure

```
unicornt-store-springboot/
├── pom.xml                              # Spring Boot parent, JAR packaging
├── Dockerfile                           # Docker image (eclipse-temurin:25)
├── docker-compose.yml                   # Orchestration with Docker Compose
├── .env-template                        # Environment variable template
├── src/
│   └── main/
│       ├── java/com/unicornt/store/
│       │   ├── StoreApplication.java    # Entry point + data seed
│       │   ├── config/
│       │   │   ├── SecurityConfig.java   # Filters, BCrypt, protected routes
│       │   │   └── CustomAuthSuccessHandler.java
│       │   ├── model/
│       │   │   ├── Product.java         # @Entity
│       │   │   ├── Category.java        # @Entity
│       │   │   ├── ProductType.java     # @Entity
│       │   │   ├── User.java            # @Entity (authentication)
│       │   │   └── Role.java            # @Entity (ROLE_ADMIN, ROLE_CLIENT)
│       │   ├── mapper/
│       │   │   ├── ProductRowMapper.java
│       │   │   ├── CategoryRowMapper.java
│       │   │   └── ProductTypeRowMapper.java
│       │   ├── dao/                     # Data access with JdbcTemplate
│       │   │   ├── ProductDAO.java
│       │   │   ├── CategoryDAO.java
│       │   │   └── ProductTypeDAO.java
│       │   ├── repository/              # Spring Data JPA
│       │   │   ├── UserRepository.java
│       │   │   ├── RoleRepository.java
│       │   │   ├── ProductRepository.java
│       │   │   ├── CategoryRepository.java
│       │   │   └── ProductTypeRepository.java
│       │   ├── dto/
│       │   │   └── RegisterRequest.java
│       │   ├── service/
│       │   │   ├── ProductService.java
│       │   │   ├── ProductServiceImpl.java
│       │   │   ├── UserService.java
│       │   │   ├── UserServiceImpl.java
│       │   │   └── CustomUserDetailsService.java
│       │   └── controller/
│       │       ├── AdminProductController.java  # @PreAuthorize(ADMIN)
│       │       ├── CatalogController.java       # Public catalog
│       │       ├── AuthController.java          # Login + Registration
│       │       ├── CustomErrorController.java
│       │       └── HomeController.java
│       ├── resources/
│       │   ├── application.properties           # Base config (JPA, Thymeleaf)
│       │   ├── application-dev.properties       # dev profile (local MySQL)
│       │   ├── application-prod.properties      # prod profile (PostgreSQL/Supabase)
│       │   ├── templates/               # Thymeleaf
│       │   │   ├── layout/
│       │   │   │   ├── header.html      # Navbar with sec:authorize
│       │   │   │   └── footer.html      # Footer with sec:authorize
│       │   │   ├── login.html
│       │   │   ├── register.html
│       │   │   ├── error/
│       │   │   │   └── access-denied.html
│       │   │   ├── catalog/
│       │   │   │   └── product-list.html
│       │   │   └── admin/
│       │   │       ├── product-list.html
│       │   │       └── product-form.html
│       │   └── static/
│       │       └── assets/css/
│       │           └── admin.css
│   └── test/
│       ├── java/com/unicornt/store/
│       │   ├── service/
│       │   │   └── UserServiceTest.java         # Unit tests (Mockito)
│       │   └── controller/
│       │       └── SecurityIntegrationTest.java  # Integration tests (MockMvc)
│       └── resources/
│           └── application.properties            # In-memory H2 for tests
└── target/
    └── unicornt-store.jar
```

---

## Tests

```bash
mvn clean test
```

| Class | Type | Tests | Coverage |
|-------|------|-------|-----------|
| `UserServiceTest` | Unit (Mockito) | 4 | Registration, role not found, email exists |
| `SecurityIntegrationTest` | Integration (MockMvc + H2) | 11 | Public access, CLIENT/ADMIN roles, registration, validations |

The integration tests use **in-memory H2** and do not require MySQL.

> **Note:** The tests use `@TestPropertySource` to force the connection to H2. This is necessary because the environment variables `SPRING_DATASOURCE_*` (used in production/development) take precedence over the test `application.properties`. Without `@TestPropertySource`, if you have those variables defined in your terminal, the tests would try to connect to MySQL instead of H2.
