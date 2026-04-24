#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

# Stop compose services and remove volumes
docker compose down -v

# Clean up any remaining banking-api containers
docker ps -aq --filter "name=banking-api" | xargs -r docker rm -f 2>/dev/null || true

echo "Stopped"