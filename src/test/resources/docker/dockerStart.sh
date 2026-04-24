#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

# Clean up any existing banking-api containers
echo "Cleaning up old containers..."
docker ps -aq --filter "name=banking-api" | xargs -r docker rm -f 2>/dev/null || true

docker compose up -d --wait postgres
echo "Postgres is ready on localhost:5432"