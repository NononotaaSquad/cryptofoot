#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE=(docker compose -f "$ROOT_DIR/backend/docker-compose.yaml")

echo "Starting MongoDB and backend (profile: local) in Docker, waiting until healthy..."
if ! "${COMPOSE[@]}" up --build -d --wait; then
  echo "MongoDB or the backend did not become healthy, so the frontend was not started." >&2
  echo "Is port 8080 or 27017 taken by another process, e.g. a backend started from the IDE?" >&2
  echo "Backend logs: docker compose -f backend/docker-compose.yaml logs backend" >&2
  exit 1
fi

echo "Starting frontend..."
cd "$ROOT_DIR/frontend"
npm run dev &
FRONTEND_PID=$!

"${COMPOSE[@]}" logs -f backend &
LOGS_PID=$!

echo "Press Ctrl+C to stop everything."
trap 'kill $FRONTEND_PID $LOGS_PID 2>/dev/null; "${COMPOSE[@]}" stop; exit' SIGINT SIGTERM

wait $FRONTEND_PID
