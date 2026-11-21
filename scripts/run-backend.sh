#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "Starting MongoDB and backend (profile: local) in Docker..."
docker compose -f "$ROOT_DIR/backend/docker-compose.yaml" up --build
