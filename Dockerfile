# ---------- Stage 1: build the fat jar ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

RUN chmod +x mvnw && ./mvnw -B -q -DskipTests clean package

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Run as an unprivileged user.
RUN useradd --system --create-home --shell /usr/sbin/nologin unicornt
USER unicornt

# finalName is "app", so the packaged artifact is target/app.jar.
COPY --from=build /src/target/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
