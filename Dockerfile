# ---------- Stage 1: build the fat jar ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

# The per-profile config files (application-{dev,qa,prod}.yml) are gitignored
# (an approval criterion names application-prod.yml). Generate them from the
# committed *.yml.example templates so every profile ships inside the jar and
# SPRING_PROFILES_ACTIVE can select one at runtime.
RUN cd src/main/resources \
 && for p in dev qa prod; do cp "application-$p.yml.example" "application-$p.yml"; done

# Normalize line endings before executing: a checkout with core.autocrlf=true
# (the common Windows default) rewrites mvnw's LF endings to CRLF, which
# breaks its "#!/bin/sh" shebang inside this Linux build stage.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q -DskipTests clean package

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
