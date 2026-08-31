#!/usr/bin/env bash
# End-to-end acceptance check for the Unicornt Store REST API.
#
# Brings up PostgreSQL via Docker Compose, packages and starts the application
# in the dev profile, then asserts the core rubric behaviours: products list
# 200, create without a token 401, create with an ADMIN token 201, and
# Swagger UI reachable in dev. Exits non-zero on the first failed assertion.
#
# Usage: bash scripts/acceptance.sh
# Requires: docker, docker compose, a .env file (copy .env.example and fill it in),
#           and port 8082 free on the host (chosen to avoid colliding with an
#           already-running instance on 8080).

set -e

PORT="${ACCEPTANCE_PORT:-8082}"
BASE="http://localhost:${PORT}"
APP_PID=""

cleanup() {
  if [ -n "$APP_PID" ]; then
    kill "$APP_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo "== 1. Start PostgreSQL =="
docker compose up -d db

echo "== 2. Wait for the database to be healthy =="
for _ in $(seq 1 20); do
  status=$(docker inspect --format='{{.State.Health.Status}}' unicornt-postgres 2>/dev/null || echo "starting")
  [ "$status" = "healthy" ] && break
  sleep 3
done
if [ "$status" != "healthy" ]; then
  echo "PostgreSQL did not become healthy in time" >&2
  exit 1
fi

echo "== 3. Package the application =="
./mvnw -q -DskipTests clean package

echo "== 4. Start the application (dev profile, port ${PORT}) =="
: "${SPRING_DATASOURCE_URL:=jdbc:postgresql://localhost:5432/${POSTGRES_DB:-unicornt_db}}"
: "${SPRING_DATASOURCE_USERNAME:=${POSTGRES_USER:?set POSTGRES_USER, e.g. via .env}}"
: "${SPRING_DATASOURCE_PASSWORD:=${POSTGRES_PASSWORD:?set POSTGRES_PASSWORD, e.g. via .env}}"
: "${APP_JWT_SECRET:?set APP_JWT_SECRET, e.g. openssl rand -base64 32}"

SERVER_PORT="$PORT" \
SPRING_DATASOURCE_URL="$SPRING_DATASOURCE_URL" \
SPRING_DATASOURCE_USERNAME="$SPRING_DATASOURCE_USERNAME" \
SPRING_DATASOURCE_PASSWORD="$SPRING_DATASOURCE_PASSWORD" \
APP_JWT_SECRET="$APP_JWT_SECRET" \
APP_CORS_ALLOWED_ORIGINS="${APP_CORS_ALLOWED_ORIGINS:-http://localhost:5173}" \
SPRING_PROFILES_ACTIVE=dev \
java -jar target/app.jar > /tmp/acceptance-app.log 2>&1 &
APP_PID=$!

echo "== 5. Wait for startup =="
for _ in $(seq 1 30); do
  if grep -q "Started StoreApplication" /tmp/acceptance-app.log 2>/dev/null; then
    break
  fi
  sleep 2
done
if ! grep -q "Started StoreApplication" /tmp/acceptance-app.log 2>/dev/null; then
  echo "Application did not start in time; see /tmp/acceptance-app.log" >&2
  cat /tmp/acceptance-app.log >&2
  exit 1
fi

assert_status() {
  local description="$1" expected="$2" actual="$3"
  if [ "$actual" != "$expected" ]; then
    echo "FAIL: $description — expected $expected, got $actual" >&2
    exit 1
  fi
  echo "PASS: $description ($actual)"
}

echo "== 6. Products list is public =="
code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/v1/products")
assert_status "products list" 200 "$code"

echo "== 7. Creating a product without a token is rejected =="
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/v1/products" \
  -H "Content-Type: application/json" \
  -d '{"name":"Acceptance","price":100,"categoryId":1,"productTypeId":1}')
assert_status "create without token" 401 "$code"

echo "== 8. Obtain an ADMIN token =="
admin_response=$(curl -s -X POST "$BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@unicornt.cl","password":"admin123"}')
admin_token=$(echo "$admin_response" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [ -z "$admin_token" ]; then
  echo "FAIL: could not obtain an ADMIN token — response was: $admin_response" >&2
  exit 1
fi
echo "PASS: obtained ADMIN token"

echo "== 9. Creating a product as ADMIN succeeds =="
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/v1/products" \
  -H "Authorization: Bearer $admin_token" -H "Content-Type: application/json" \
  -d '{"name":"Acceptance Product","description":"created by scripts/acceptance.sh","imageBase":"acceptance","price":9990,"categoryId":1,"productTypeId":1,"active":true}')
assert_status "create as ADMIN" 201 "$code"

echo "== 10. Swagger UI is reachable in dev =="
code=$(curl -s -o /dev/null -w "%{http_code}" -L "$BASE/swagger-ui.html")
assert_status "swagger dev" 200 "$code"

echo
echo "All acceptance assertions passed."
