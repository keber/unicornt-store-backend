# Desarrollo

## Estructura del proyecto

```
unicornt-store-springboot/
├── pom.xml                              # Spring Boot parent, packaging JAR
├── Dockerfile                           # Imagen Docker (eclipse-temurin:21)
├── docker-compose.yml                   # Orquestación con Docker Compose
├── .env-template                        # Plantilla de variables de entorno
├── src/
│   └── main/
│       ├── java/com/unicornt/store/
│       │   ├── StoreApplication.java    # Punto de entrada + seed de datos
│       │   ├── config/
│       │   │   ├── SecurityConfig.java   # Filtros, BCrypt, rutas protegidas
│       │   │   └── CustomAuthSuccessHandler.java
│       │   ├── model/
│       │   │   ├── Product.java         # @Entity
│       │   │   ├── Category.java        # @Entity
│       │   │   ├── ProductType.java     # @Entity
│       │   │   ├── User.java            # @Entity (autenticación)
│       │   │   └── Role.java            # @Entity (ROLE_ADMIN, ROLE_CLIENT)
│       │   ├── mapper/
│       │   │   ├── ProductRowMapper.java
│       │   │   ├── CategoryRowMapper.java
│       │   │   └── ProductTypeRowMapper.java
│       │   ├── dao/                     # Acceso a datos con JdbcTemplate
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
│       │       ├── CatalogController.java       # Catálogo público
│       │       ├── AuthController.java          # Login + Registro
│       │       ├── CustomErrorController.java
│       │       └── HomeController.java
│       ├── resources/
│       │   ├── application.properties           # Config base (JPA, Thymeleaf)
│       │   ├── application-dev.properties       # Perfil dev (MySQL local)
│       │   ├── application-prod.properties      # Perfil prod (PostgreSQL/Supabase)
│       │   ├── templates/               # Thymeleaf
│       │   │   ├── layout/
│       │   │   │   ├── header.html      # Navbar con sec:authorize
│       │   │   │   └── footer.html      # Footer con sec:authorize
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
│       │   │   └── UserServiceTest.java         # Tests unitarios (Mockito)
│       │   └── controller/
│       │       └── SecurityIntegrationTest.java  # Tests de integración (MockMvc)
│       └── resources/
│           └── application.properties            # H2 in-memory para tests
└── target/
    └── unicornt-store.jar
```

---

## Tests

```bash
mvn clean test
```

| Clase | Tipo | Tests | Cobertura |
|-------|------|-------|-----------|
| `UserServiceTest` | Unitario (Mockito) | 4 | Registro, rol no encontrado, email exists |
| `SecurityIntegrationTest` | Integración (MockMvc + H2) | 11 | Acceso público, roles CLIENT/ADMIN, registro, validaciones |

Los tests de integración usan **H2 en memoria** y no requieren MySQL.

> **Nota:** Los tests usan `@TestPropertySource` para forzar la conexión a H2. Esto es necesario porque las variables de entorno `SPRING_DATASOURCE_*` (usadas en producción/desarrollo) tienen prioridad sobre `application.properties` de test. Sin `@TestPropertySource`, si tienes esas variables definidas en tu terminal, los tests intentarían conectarse a MySQL en vez de H2.
