#!/bin/sh
# Generates the gitignored per-profile config files from their committed
# *.yml.example templates:
#   src/main/resources/application-{dev,qa,prod}.yml
#
# Run this once after cloning if you start the app OUTSIDE Docker
# (./mvnw spring-boot:run or java -jar). The Docker build does the same step
# itself, so it is not needed for `docker compose up`.
#
# Safe to re-run: an existing file is left untouched, so local edits survive.

set -eu

cd "$(dirname "$0")/../src/main/resources"

for p in dev qa prod; do
  src="application-$p.yml.example"
  dst="application-$p.yml"
  if [ ! -f "$src" ]; then
    echo "missing template: $src" >&2
    exit 1
  fi
  if [ -f "$dst" ]; then
    echo "keep   $dst"
  else
    cp "$src" "$dst"
    echo "create $dst  (from $src)"
  fi
done
