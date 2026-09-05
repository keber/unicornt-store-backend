#!/bin/sh
# Read-only preflight for the multi-env VPS setup (P3 of docs/multi-env-deploy).
# Run on the server as root (or a sudo user):  sh vps-check.sh
# Changes nothing. Prints [OK] / [WARN] / [FAIL] per check.
#
# Assumes:
#  - per-env dir  /opt/unicornt/<env>/  with compose.yml (or docker-compose.yml) + .env
#  - per-env user deploy-<env> whose key forces sudo /usr/local/sbin/deploy-unicornt-<env>
#  - reverse proxy handled outside (EasyEngine / nginx-proxy container) — not checked here

BASE_DIR="${BASE_DIR:-/opt/unicornt}"
ENVS="dev qa prod"

meta() { # env -> "internal_port api_host"
  case "$1" in
    dev)  echo "8081 api-unicornt-dev.keber.dev" ;;
    qa)   echo "8082 api-unicornt-qa.keber.cl" ;;
    prod) echo "8088 api-unicornt-store.keber.cl" ;;
  esac
}

ok()   { printf '  [OK]   %s\n' "$1"; }
warn() { printf '  [WARN] %s\n' "$1"; }
bad()  { printf '  [FAIL] %s\n' "$1"; }
hdr()  { printf '\n=== %s ===\n' "$1"; }
have() { command -v "$1" >/dev/null 2>&1; }
SUDO=""; [ "$(id -u)" -ne 0 ] && have sudo && SUDO="sudo"

MYIP="$(curl -fsS -4 ifconfig.me 2>/dev/null || curl -fsS -4 https://api.ipify.org 2>/dev/null || echo '?')"

hdr "Host / toolchain"
printf '  host public IPv4: %s\n' "$MYIP"
if have docker; then
  ok "docker $(docker --version 2>/dev/null | awk '{print $3}' | tr -d ,)"
  docker compose version >/dev/null 2>&1 && ok "docker compose v2" || bad "docker compose v2 missing"
  docker info >/dev/null 2>&1 && ok "docker daemon reachable" || bad "docker daemon not reachable"
else
  bad "docker not on PATH"
fi

hdr "Deploy users + forced-command keys"
for e in $ENVS; do
  u="deploy-$e"
  if id "$u" >/dev/null 2>&1; then
    ok "user $u exists"
    home="$(getent passwd "$u" | cut -d: -f6)"
    ak="$home/.ssh/authorized_keys"
    if $SUDO test -f "$ak"; then
      line="$($SUDO grep -m1 'ssh-' "$ak" 2>/dev/null)"
      echo "$line" | grep -q "command=\"[^\"]*deploy-unicornt-$e\"" \
        && ok "  key forces .../deploy-unicornt-$e" \
        || bad "  key does NOT force deploy-unicornt-$e (got: $(echo "$line" | sed 's/ssh-ed25519.*//'))"
      echo "$line" | grep -q 'restrict' && ok "  key is 'restrict'" || warn "  key not 'restrict' (add it)"
    else
      bad "  no authorized_keys at $ak"
    fi
    # passwordless sudo for exactly that script?
    if $SUDO -l -U "$u" 2>/dev/null | grep -q "NOPASSWD.*deploy-unicornt-$e"; then
      ok "  $u may run deploy-unicornt-$e via sudo NOPASSWD"
    else
      warn "  could not confirm NOPASSWD sudo for $u (sudo -l -U $u)"
    fi
  else
    bad "user $u does not exist"
  fi
  [ -x "/usr/local/sbin/deploy-unicornt-$e" ] && ok "script /usr/local/sbin/deploy-unicornt-$e present+exec" \
                                              || bad "script /usr/local/sbin/deploy-unicornt-$e missing/not exec"
done

for e in $ENVS; do
  set -- $(meta "$e"); PORT="$1"; APIHOST="$2"
  D="$BASE_DIR/$e"
  hdr "env: $e  ($D, port $PORT, $APIHOST)"

  [ -d "$D" ] && ok "dir exists" || { bad "dir $D missing"; continue; }

  CF=""
  for c in compose.yml docker-compose.yml compose.yaml; do
    [ -f "$D/$c" ] && { CF="$D/$c"; break; }
  done
  [ -n "$CF" ] && ok "compose file: $(basename "$CF")" || bad "no compose.yml / docker-compose.yml in $D"

  if [ -f "$D/.env" ]; then
    perm=$(stat -c '%a' "$D/.env" 2>/dev/null || echo '?')
    [ "$perm" = "600" ] && ok ".env mode 600" || warn ".env mode $perm (want 600: chmod 600 $D/.env)"
    grep -q '__CHANGE_ME__' "$D/.env" 2>/dev/null && bad ".env has __CHANGE_ME__ placeholders" || ok ".env has no placeholders"
    tag=$(grep -E '^IMAGE_TAG=' "$D/.env" 2>/dev/null | cut -d= -f2)
    if [ "$tag" = "$e" ]; then ok ".env IMAGE_TAG=$tag"
    elif [ -n "$tag" ]; then warn ".env IMAGE_TAG=$tag (expected '$e' so channel deploys land here)"
    else warn ".env has no IMAGE_TAG line"; fi
    grep -q '^DOCKERHUB_USERNAME=' "$D/.env" 2>/dev/null && ok ".env has DOCKERHUB_USERNAME" || bad ".env missing DOCKERHUB_USERNAME"
    if [ "$e" = "prod" ]; then
      grep -q '^SPRING_DATASOURCE_URL=' "$D/.env" 2>/dev/null && ok ".env has SPRING_DATASOURCE_URL (Supabase)" || bad ".env missing SPRING_DATASOURCE_URL"
      grep -q '^APP_CORS_ALLOWED_ORIGINS=' "$D/.env" 2>/dev/null && ok ".env has APP_CORS_ALLOWED_ORIGINS" || bad ".env missing APP_CORS_ALLOWED_ORIGINS"
    else
      grep -q '^POSTGRES_PASSWORD=' "$D/.env" 2>/dev/null && ok ".env has POSTGRES_PASSWORD" || bad ".env missing POSTGRES_PASSWORD"
    fi
    grep -q '^APP_JWT_SECRET=' "$D/.env" 2>/dev/null && ok ".env has APP_JWT_SECRET" || bad ".env missing APP_JWT_SECRET"
  else
    bad ".env missing in $D"
  fi

  [ -n "$CF" ] && { ( cd "$D" && docker compose config -q >/dev/null 2>&1 ) \
    && ok "docker compose config valid" \
    || bad "docker compose config INVALID  (cd $D && docker compose config)"; }

  if [ "$e" != "prod" ]; then
    cid=$(docker ps -q -f "name=unicornt-$e-db" 2>/dev/null)
    if [ -n "$cid" ]; then
      h=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$cid" 2>/dev/null)
      [ "$h" = "healthy" ] && ok "db container healthy" || warn "db container up, health=$h"
      v=$(docker inspect -f '{{range .Mounts}}{{if eq .Type "volume"}}{{.Name}} {{end}}{{end}}' "$cid" 2>/dev/null)
      [ -n "$v" ] && ok "db persists to volume: $v" || warn "db container has no named volume mount (data not persistent?)"
    else
      warn "db container unicornt-$e-db not running"
    fi
  fi

  cid=$(docker ps -q -f "name=unicornt-$e-app" 2>/dev/null)
  if [ -n "$cid" ]; then
    img=$(docker inspect -f '{{.Config.Image}}' "$cid" 2>/dev/null)
    ok "app container up (image: $img)"
  else
    warn "app container unicornt-$e-app not running (expected until first CI deploy)"
  fi

  have ss && { ss -ltn 2>/dev/null | grep -q "127.0.0.1:$PORT " && ok "listening on 127.0.0.1:$PORT" || warn "nothing on 127.0.0.1:$PORT"; }

  ips=$(getent ahostsv4 "$APIHOST" 2>/dev/null | awk '{print $1}' | sort -u | tr '\n' ' ')
  if [ -n "$ips" ]; then
    echo " $ips " | grep -q " $MYIP " && ok "DNS $APIHOST -> $ips" || warn "DNS $APIHOST -> $ips (host is $MYIP)"
  else
    bad "DNS $APIHOST does not resolve"
  fi

  hc=$(curl -s -o /dev/null -w '%{http_code}' "https://$APIHOST/api/v1/products" 2>/dev/null || echo 000)
  loc=$(curl -s -o /dev/null -w '%{redirect_url}' "https://$APIHOST/api/v1/products" 2>/dev/null)
  case "$hc" in
    200) ok "GET https://$APIHOST/api/v1/products -> 200" ;;
    000) warn "https://$APIHOST unreachable (proxy/app not ready)" ;;
    30*) warn "GET .../api/v1/products -> $hc redirect to ${loc:-?}  (stale image if -> /login)" ;;
    *)   warn "GET .../api/v1/products -> $hc" ;;
  esac
done

hdr "Done"
echo "[WARN] is acceptable before the first CI deploy. Clear every [FAIL]."
