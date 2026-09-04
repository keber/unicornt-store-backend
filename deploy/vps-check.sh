#!/bin/sh
# Read-only preflight for the multi-env VPS setup (P3 of docs/multi-env-deploy).
# Run it on the server as the sudo user:  sh vps-check.sh
# It changes nothing; it reports [OK] / [WARN] / [FAIL] per check.

BASE_DIR="${BASE_DIR:-/opt/unicornt}"
DEPLOY_USER="${DEPLOY_USER:-deployer}"
ENVS="dev qa prod"

# env -> "internal_port api_host web_host"
meta() {
  case "$1" in
    dev)  echo "8081 api-unicornt-dev.keber.dev unicornt-dev.keber.dev" ;;
    qa)   echo "8082 api-unicornt-qa.keber.cl unicornt-qa.keber.cl" ;;
    prod) echo "8088 api-unicornt-store.keber.cl unicornt-store.keber.cl" ;;
  esac
}

ok()   { printf '  [OK]   %s\n' "$1"; }
warn() { printf '  [WARN] %s\n' "$1"; }
bad()  { printf '  [FAIL] %s\n' "$1"; }
hdr()  { printf '\n=== %s ===\n' "$1"; }

have() { command -v "$1" >/dev/null 2>&1; }

MYIP="$(curl -fsS -4 ifconfig.me 2>/dev/null || curl -fsS -4 https://api.ipify.org 2>/dev/null || echo '?')"

hdr "Host / toolchain"
printf '  host public IPv4: %s\n' "$MYIP"
if have docker; then
  ok "docker $(docker --version 2>/dev/null | awk '{print $3}' | tr -d ,)"
  docker compose version >/dev/null 2>&1 && ok "docker compose v2 present" || bad "docker compose v2 missing"
  docker info >/dev/null 2>&1 && ok "docker daemon reachable" || bad "docker daemon not reachable for this user"
else
  bad "docker not on PATH"
fi
have nginx && ok "nginx $(nginx -v 2>&1 | sed 's|.*/||')" || warn "nginx not on PATH (ok if it runs in a container)"
have certbot && ok "certbot present" || warn "certbot not on PATH"

hdr "Deploy user: $DEPLOY_USER"
if id "$DEPLOY_USER" >/dev/null 2>&1; then
  ok "user exists"
  id -nG "$DEPLOY_USER" 2>/dev/null | tr ' ' '\n' | grep -qx docker \
    && ok "in 'docker' group" || bad "NOT in 'docker' group (deploy.sh will fail to talk to docker)"
  AK="$(getent passwd "$DEPLOY_USER" | cut -d: -f6)/.ssh/authorized_keys"
  if sudo test -f "$AK" 2>/dev/null || [ -f "$AK" ]; then
    n=$(sudo grep -c 'command=' "$AK" 2>/dev/null || grep -c 'command=' "$AK" 2>/dev/null || echo 0)
    [ "$n" -ge 3 ] && ok "authorized_keys has $n forced-command entries" \
                   || warn "authorized_keys has $n forced-command entries (expected 3)"
    for e in $ENVS; do
      if sudo grep -q "command=\"$BASE_DIR/$e/deploy.sh\"" "$AK" 2>/dev/null || grep -q "command=\"$BASE_DIR/$e/deploy.sh\"" "$AK" 2>/dev/null; then
        ok "  key pinned to $BASE_DIR/$e/deploy.sh"
      else
        bad "  no key pinned to $BASE_DIR/$e/deploy.sh"
      fi
    done
  else
    bad "no authorized_keys at $AK"
  fi
else
  bad "user '$DEPLOY_USER' does not exist"
fi

for e in $ENVS; do
  set -- $(meta "$e"); PORT="$1"; APIHOST="$2"; WEBHOST="$3"
  D="$BASE_DIR/$e"
  hdr "env: $e  ($D, port $PORT)"

  [ -d "$D" ] && ok "dir exists" || { bad "dir $D missing"; continue; }

  [ -f "$D/docker-compose.yml" ] && ok "docker-compose.yml present" || bad "docker-compose.yml missing"
  if [ -f "$D/deploy.sh" ]; then
    [ -x "$D/deploy.sh" ] && ok "deploy.sh present + executable" || bad "deploy.sh present but NOT executable"
  else
    bad "deploy.sh missing"
  fi

  if [ -f "$D/.env" ]; then
    perm=$(stat -c '%a' "$D/.env" 2>/dev/null || echo '?')
    [ "$perm" = "600" ] && ok ".env present (mode 600)" || warn ".env present (mode $perm; want 600)"
    grep -q '__CHANGE_ME__' "$D/.env" 2>/dev/null && bad ".env still has __CHANGE_ME__ placeholders" || ok ".env has no placeholders"
    grep -q '^IMAGE_TAG=' "$D/.env" 2>/dev/null && ok ".env has IMAGE_TAG line" || warn ".env has no IMAGE_TAG line (deploy.sh will append one)"
    grep -q '^DOCKERHUB_USERNAME=' "$D/.env" 2>/dev/null && ok ".env has DOCKERHUB_USERNAME" || bad ".env missing DOCKERHUB_USERNAME (compose needs it for image ref)"
    if [ "$e" = "prod" ]; then
      grep -q '^SPRING_DATASOURCE_URL=' "$D/.env" 2>/dev/null && ok ".env has SPRING_DATASOURCE_URL (Supabase)" || bad ".env missing SPRING_DATASOURCE_URL"
    else
      grep -q '^POSTGRES_PASSWORD=' "$D/.env" 2>/dev/null && ok ".env has POSTGRES_PASSWORD" || bad ".env missing POSTGRES_PASSWORD"
    fi
  else
    bad ".env missing"
  fi

  if [ -f "$D/docker-compose.yml" ]; then
    ( cd "$D" && docker compose config -q >/dev/null 2>&1 ) && ok "docker compose config valid" || bad "docker compose config INVALID (cd $D && docker compose config)"
  fi

  # db container (dev/qa only)
  if [ "$e" != "prod" ]; then
    cid=$(docker ps -q -f "name=unicornt-$e-db" 2>/dev/null)
    if [ -n "$cid" ]; then
      health=$(docker inspect -f '{{.State.Health.Status}}' "$cid" 2>/dev/null || echo '?')
      [ "$health" = "healthy" ] && ok "db container running + healthy" || warn "db container running, health=$health"
    else
      warn "db container unicornt-$e-db not running (fine if you haven't started it yet)"
    fi
    docker volume ls -q 2>/dev/null | grep -qx "pgdata_$e" && ok "volume pgdata_$e exists" || warn "volume pgdata_$e not created yet"
  fi

  # app container
  cid=$(docker ps -q -f "name=unicornt-$e-app" 2>/dev/null)
  [ -n "$cid" ] && ok "app container running" || warn "app container unicornt-$e-app not running (expected until first deploy)"

  # local port
  if have ss; then
    ss -ltn 2>/dev/null | grep -q "127.0.0.1:$PORT " && ok "something listening on 127.0.0.1:$PORT" || warn "nothing on 127.0.0.1:$PORT yet"
  fi

  # nginx knows the hosts
  if have nginx; then
    conf=$(sudo nginx -T 2>/dev/null || nginx -T 2>/dev/null)
    echo "$conf" | grep -qE "server_name[^;]*\b$APIHOST\b" && ok "nginx has server_name $APIHOST" || bad "nginx has no server block for $APIHOST"
    if [ "$e" != "prod" ]; then
      echo "$conf" | grep -qE "server_name[^;]*\b$WEBHOST\b" && ok "nginx has server_name $WEBHOST" || warn "nginx has no server block for $WEBHOST (frontend, P6)"
    fi
  fi

  # TLS cert present
  if [ -d "/etc/letsencrypt/live" ]; then
    ls /etc/letsencrypt/live/ 2>/dev/null | grep -q "$APIHOST" && ok "letsencrypt cert dir for $APIHOST" || warn "no letsencrypt cert dir matching $APIHOST"
  fi

  # DNS resolves to this box
  ips=$(getent ahostsv4 "$APIHOST" 2>/dev/null | awk '{print $1}' | sort -u | tr '\n' ' ')
  if [ -n "$ips" ]; then
    echo " $ips " | grep -q " $MYIP " && ok "DNS $APIHOST -> $ips (matches host)" || warn "DNS $APIHOST -> $ips (host is $MYIP)"
  else
    bad "DNS $APIHOST does not resolve"
  fi

  # end-to-end HTTPS (only meaningful once the app is deployed)
  hc=$(curl -s -o /dev/null -w '%{http_code}' "https://$APIHOST/api/v1/products" 2>/dev/null || echo 000)
  case "$hc" in
    200) ok "GET https://$APIHOST/api/v1/products -> 200" ;;
    000) warn "https://$APIHOST unreachable (TLS/nginx/app not ready)" ;;
    *)   warn "GET https://$APIHOST/api/v1/products -> $hc" ;;
  esac
done

hdr "Done"
echo "Re-run after each fix. [WARN] is fine before the first deploy; [FAIL] must be cleared."
