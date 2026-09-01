#!/bin/sh
# Verifies the local toolchain and .env file before boot/build/checkout attempts.
# Usage: bash scripts/check-env.sh  (or sh scripts/check-env.sh)
# Exits 0 if everything required is present, 1 otherwise.

set -u
fail=0
warn=0

ok()   { printf '  [OK]   %s\n' "$1"; }
bad()  { printf '  [FAIL] %s\n' "$1"; fail=1; }
warnf() { printf '  [WARN] %s\n' "$1"; warn=1; }

section() { printf '\n%s\n' "$1"; }

section "Java"
if command -v java >/dev/null 2>&1; then
  ver="$(java -version 2>&1 | head -n1)"
  major="$(java -version 2>&1 | head -n1 | sed -E 's/.*"([0-9]+).*/\1/')"
  if [ "${major:-0}" -ge 21 ] 2>/dev/null; then
    ok "java found: $ver"
  else
    bad "java found ($ver) but this project targets Java 25 (21+ should compile; verify explicitly)"
  fi
else
  bad "java not found on PATH. Install a JDK 21+ (Temurin recommended: https://adoptium.net)"
fi

section "Maven Wrapper"
if [ -f "./mvnw" ]; then
  ok "./mvnw present"
else
  bad "./mvnw not found — run this script from the repository root"
fi

section "Docker"
if command -v docker >/dev/null 2>&1; then
  if docker info >/dev/null 2>&1; then
    ok "docker daemon is running"
  else
    bad "docker CLI found but the daemon is not reachable. Start Docker Desktop (or the docker service) and retry."
  fi
else
  bad "docker not found on PATH. Install Docker Desktop: https://www.docker.com/products/docker-desktop/"
fi

if command -v docker >/dev/null 2>&1; then
  if docker compose version >/dev/null 2>&1; then
    ok "docker compose plugin available"
  else
    bad "docker compose (v2 plugin) not available. Update Docker Desktop / install the compose plugin."
  fi
fi

section "OpenSSL (only needed to generate APP_JWT_SECRET)"
if command -v openssl >/dev/null 2>&1; then
  ok "openssl found: $(openssl version)"
else
  warnf "openssl not found on PATH. Only needed once, to generate APP_JWT_SECRET."
  warnf "  Debian/Ubuntu: sudo apt install openssl"
  warnf "  RHEL/Fedora:   sudo yum install openssl"
  warnf "  macOS:         brew install openssl"
  warnf "  Or generate the secret any other way: 32+ random bytes, base64-encoded."
fi

section "Port 8080"
if command -v ss >/dev/null 2>&1 && ss -ltn 2>/dev/null | grep -q ':8080 '; then
  warnf "port 8080 is already in use. Stop whatever is listening, or set SERVER_PORT in .env."
elif command -v lsof >/dev/null 2>&1 && lsof -i :8080 >/dev/null 2>&1; then
  warnf "port 8080 is already in use. Stop whatever is listening, or set SERVER_PORT in .env."
else
  ok "port 8080 looks free (or could not be checked on this system)"
fi

section ".env file"
if [ -f ".env" ]; then
  ok ".env exists"
  if grep -v '^[[:space:]]*#' .env 2>/dev/null | grep -q '__CHANGE_ME__'; then
    bad ".env still contains __CHANGE_ME__ placeholders — replace them with real local values"
  else
    ok "no __CHANGE_ME__ placeholders left in .env"
  fi
  secret_val=$(grep -E '^APP_JWT_SECRET=' .env 2>/dev/null | cut -d= -f2- | tr -d '[:space:]')
  secret_len=$(printf '%s' "$secret_val" | wc -c)
  case "$secret_val" in
    *CHANGE_ME*) bad "APP_JWT_SECRET is still the placeholder value — generate a real one (see below)" ;;
    *) if [ "${secret_len:-0}" -ge 32 ]; then
         ok "APP_JWT_SECRET looks present and long enough ($secret_len chars)"
       else
         bad "APP_JWT_SECRET is missing or looks too short (found $secret_len chars, want 32+)"
       fi ;;
  esac
else
  bad ".env not found. Run: cp .env.example .env   (then fill in the placeholders)"
fi

printf '\n'
if [ "$fail" -eq 1 ]; then
  printf 'Some required checks failed — see [FAIL] lines above.\n'
  exit 1
elif [ "$warn" -eq 1 ]; then
  printf 'All required checks passed, with warnings above.\n'
  exit 0
else
  printf 'All checks passed.\n'
  exit 0
fi
