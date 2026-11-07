#!/usr/bin/env bash
# Custom-format pg_dump of the application database.
# A failed dump is deleted so a partial file cannot be restored by mistake.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ ! -f .env ]]; then
  echo "Missing .env. Copy .env.example to .env first." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

mkdir -p backups
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
outfile="backups/taskflow-${stamp}.dump"

echo "Creating ${outfile}"
if ! docker exec taskflow-postgres pg_dump -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -Fc > "${outfile}"; then
  echo "Backup failed. Removing ${outfile}." >&2
  rm -f "${outfile}"
  exit 1
fi

if [[ ! -s "${outfile}" ]]; then
  echo "Backup file is empty. Removing ${outfile}." >&2
  rm -f "${outfile}"
  exit 1
fi

echo "Backup complete: ${outfile}"
