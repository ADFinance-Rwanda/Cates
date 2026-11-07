#!/usr/bin/env bash
# Restore a custom-format dump produced by scripts/backup.sh.
# This replaces objects in the application database. Stop writers first if you can.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ ! -f .env ]]; then
  echo "Missing .env. Copy .env.example to .env first." >&2
  exit 1
fi

file="${1:-}"
if [[ -z "${file}" || ! -f "${file}" ]]; then
  echo "Usage: ./scripts/restore.sh backups/taskflow-YYYYMMDDTHHMMSSZ.dump" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

echo "Restoring ${file} into ${POSTGRES_DB}. If this fails, the database may be partial. Restore a known-good dump or recreate the volume."
docker cp "${file}" taskflow-postgres:/tmp/taskflow-restore.dump
docker exec taskflow-postgres pg_restore \
  -U "${POSTGRES_USER}" \
  -d "${POSTGRES_DB}" \
  --clean \
  --if-exists \
  --no-owner \
  --exit-on-error \
  /tmp/taskflow-restore.dump
docker exec taskflow-postgres rm -f /tmp/taskflow-restore.dump

echo "Restore finished. Row check:"
docker exec taskflow-postgres psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -c \
  "SELECT 'organizations' AS table_name, count(*) FROM organizations UNION ALL SELECT 'tasks', count(*) FROM tasks;"
