#!/bin/sh
# Server-side deploy script. Install one copy per environment:
#   /opt/unicornt/dev/deploy.sh
#   /opt/unicornt/qa/deploy.sh
#   /opt/unicornt/prod/deploy.sh
# (identical file — it acts on whatever directory it sits in).
#
# Pin it as the forced command for that environment's deploy key in
# ~/.ssh/authorized_keys, e.g.:
#
#   command="/opt/unicornt/dev/deploy.sh",no-port-forwarding,no-agent-forwarding,no-pty,no-X11-forwarding ssh-ed25519 AAAA... ci-deploy-dev
#
# CI connects with:  ssh ... "deploy sha-abc1234"
# which arrives here as: SSH_ORIGINAL_COMMAND="deploy sha-abc1234"

set -eu

# Last whitespace-separated token of the requested command = the image tag.
TAG="${SSH_ORIGINAL_COMMAND##* }"
case "$TAG" in
  sha-[0-9a-f][0-9a-f]*) : ;;
  *)
    echo "refusing image tag '$TAG' (expected sha-<hex>)" >&2
    exit 1
    ;;
esac

# Act on this script's own directory; the key can touch nothing else.
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "no .env in $(pwd)" >&2
  exit 1
fi

# Pin the immutable tag so restarts are deterministic.
if grep -q '^IMAGE_TAG=' .env; then
  sed -i "s|^IMAGE_TAG=.*|IMAGE_TAG=${TAG}|" .env
else
  printf '\nIMAGE_TAG=%s\n' "${TAG}" >> .env
fi

docker compose pull
docker compose up -d --remove-orphans
docker image prune -f

echo "deployed ${TAG} in $(pwd)"
