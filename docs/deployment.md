# Despliegue

## Compilación y empaquetado

```bash
mvn clean package -DskipTests
```

El JAR ejecutable se genera en:

```
target/unicornt-store.jar
```

---

## Ejecución local (sin Docker)

Requiere las variables de entorno definidas en [Configuración](configuration.md#variables-de-entorno):

```bash
java -jar target/unicornt-store.jar
```

O directamente con Maven:

```bash
mvn spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`.

---

## Despliegue con Docker

### 1. Crear el archivo `.env`

```bash
cp .env-template .env
# Editar .env con los valores reales de conexión a BD
```

Ejemplo de `.env` para desarrollo local (MySQL en el host):

```env
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/unicornt_store?useSSL=false&serverTimezone=America/Santiago&characterEncoding=UTF-8&useUnicode=true
SPRING_DATASOURCE_USERNAME=tu_usuario
SPRING_DATASOURCE_PASSWORD=tu_password
```

> **Nota:** Desde Docker, `localhost` apunta al contenedor, no al host. Usa `host.docker.internal` para conectarte a servicios del host (MySQL, PostgreSQL).

### 2. Compilar y levantar

```bash
mvn clean package -DskipTests
docker compose --env-file .env up --build -d
```

La aplicación estará disponible en `http://localhost:8080`.

### 3. Ver logs

```bash
docker compose logs -f
```

### 4. Detener

```bash
docker compose down
```

---

## Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
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

Las variables se inyectan desde el archivo `.env` y se pasan al contenedor como variables de entorno.
