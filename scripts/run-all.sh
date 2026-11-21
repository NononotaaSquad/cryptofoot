#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE=(docker compose -f "$ROOT_DIR/backend/docker-compose.yaml")

echo "Starting MongoDB and backend (profile: local) in Docker, waiting until healthy..."
"${COMPOSE[@]}" up --build -d --wait

echo "Starting frontend..."
cd "$ROOT_DIR/frontend"
npm run dev &
FRONTEND_PID=$!

"${COMPOSE[@]}" logs -f backend &
LOGS_PID=$!

echo "Press Ctrl+C to stop everything."
trap 'kill $FRONTEND_PID $LOGS_PID 2>/dev/null; "${COMPOSE[@]}" stop; exit' SIGINT SIGTERM

wait $FRONTEND_PID
