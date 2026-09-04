#!/bin/sh
set -eu

: "${MYSQL_PASSWORD:?MYSQL_PASSWORD must be set}"
: "${JWT_SECRET:?JWT_SECRET must be set}"

if [ "${#JWT_SECRET}" -lt 32 ]; then
  echo "JWT_SECRET must be at least 32 characters" >&2
  exit 1
fi

mkdir -p "${INSPECTION_STORAGE_UPLOAD_ROOT:-/app/data/upload}" \
         "${INSPECTION_STORAGE_RESULT_ROOT:-/app/data/results}"

exec "$@"
