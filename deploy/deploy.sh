#!/usr/bin/env bash
# Transcript of the deploy script actually installed on the VPS, one per env:
#
#   /usr/local/sbin/deploy-unicornt-dev
#   /usr/local/sbin/deploy-unicornt-qa
#   /usr/local/sbin/deploy-unicornt-prod
#
# They are identical apart from APP_DIR / LOCK_FILE. Set both for the env you
# are installing, or keep this file as the reference copy it is.
#
# The env's deploy key is pinned to it as a forced command in authorized_keys:
#
#   restrict,command="sudo -n /usr/local/sbin/deploy-unicornt-dev" ssh-ed25519 AAAA... ci-deploy-dev
#
# plus a matching NOPASSWD sudoers rule for exactly that path. CI connects with
# `ssh ... "deploy <channel> sha-<7>"`; the server ignores every one of those
# arguments — they are a breadcrumb for the CI and auth logs only.
#
# NOTE ON IMAGE TAGS: this script deliberately does NOT read a tag out of
# SSH_ORIGINAL_COMMAND. It pulls whatever IMAGE_TAG the env's .env holds, which
# is the moving channel tag (dev / qa / prod) that `build-and-push` has just
# repointed. An earlier version of this file pinned an immutable sha- tag; that
# design was never installed. For a fast rollback, set IMAGE_TAG=sha-<7> in
# .env by hand and `docker compose up -d` — every sha- tag is still pushed.
# See docs/multi-env-deploy/PLAN.md §2 (D5) and §6.

set -Eeuo pipefail

readonly APP_DIR="/opt/unicornt/dev"
readonly COMPOSE_FILE="${APP_DIR}/compose.yml"
readonly LOCK_FILE="/run/lock/unicornt-dev-deploy.lock"

# Serialize: a second deploy while one is in flight fails fast instead of
# racing `docker compose up` against itself.
exec 9>"${LOCK_FILE}"

/usr/bin/flock -n 9 || {
  echo "ERROR: ya existe otro despliegue en ejecución."
  exit 1
}

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "ERROR: no existe ${COMPOSE_FILE}."
  exit 1
fi

cd "${APP_DIR}"

echo "Validando configuración..."
/usr/bin/docker compose config --quiet

echo "Descargando imágenes..."
/usr/bin/docker compose pull

echo "Actualizando servicios..."
/usr/bin/docker compose up -d --remove-orphans

echo "Estado final:"
/usr/bin/docker compose ps
