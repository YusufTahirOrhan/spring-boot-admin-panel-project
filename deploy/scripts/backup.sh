#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-"${DEPLOY_DIR}/.env"}"
COMPOSE_FILE="${COMPOSE_FILE:-"${DEPLOY_DIR}/docker-compose.prod.yml"}"
BACKUP_ROOT="${BACKUP_ROOT:-"${DEPLOY_DIR}/backups"}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP_DIR="${BACKUP_ROOT}/${TIMESTAMP}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing env file: ${ENV_FILE}" >&2
  echo "Copy deploy/.env.example to deploy/.env and fill production secrets first." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

mkdir -p "${BACKUP_DIR}"

COMPOSE=(docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}")
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-optimaxx}"
UPLOAD_VOLUME="${UPLOAD_VOLUME:-"${PROJECT_NAME}_site_uploads"}"

"${COMPOSE[@]}" exec -T postgres pg_dump \
  -U "${POSTGRES_USER}" \
  -d "${POSTGRES_DB}" \
  -Fc > "${BACKUP_DIR}/postgres_${POSTGRES_DB}_${TIMESTAMP}.dump"

"${COMPOSE[@]}" exec -T clickhouse clickhouse-client \
  --user "${CLICKHOUSE_USERNAME}" \
  --password "${CLICKHOUSE_PASSWORD}" \
  --query "SELECT * FROM audit_events FORMAT JSONEachRow" \
  > "${BACKUP_DIR}/clickhouse_audit_events_${TIMESTAMP}.jsonl"

docker run --rm \
  -v "${UPLOAD_VOLUME}:/data:ro" \
  -v "${BACKUP_DIR}:/backup" \
  alpine:3.21 \
  tar -czf "/backup/site_uploads_${TIMESTAMP}.tar.gz" -C /data .

echo "Backups written to ${BACKUP_DIR}"
