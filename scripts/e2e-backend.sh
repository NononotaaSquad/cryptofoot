#!/bin/bash
# Runs the backend E2E suites in .e2e/backend against a running backend (./scripts/run-backend.sh).
# Usage: ./scripts/e2e-backend.sh [postman|bruno|all]    BASE_URL defaults to http://localhost:8080

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
E2E_DIR="$(dirname "$SCRIPT_DIR")/.e2e/backend"
BASE_URL="${BASE_URL:-http://localhost:8080}"

run_postman() {
  echo "Postman collection (Newman) against $BASE_URL"
  npx --yes newman@6.2.3 run "$E2E_DIR/postman/cryptofoot-backend.postman_collection.json" \
    --environment "$E2E_DIR/postman/local.postman_environment.json" \
    --env-var "baseUrl=$BASE_URL"
}

run_bruno() {
  echo "Bruno collection against $BASE_URL"
  (cd "$E2E_DIR/bruno" && npx --yes @usebruno/cli@4.3.0 run --env local --env-var "baseUrl=$BASE_URL")
}

case "${1:-all}" in
  postman) run_postman ;;
  bruno) run_bruno ;;
  all) run_postman && run_bruno ;;
  *) echo "Usage: $0 [postman|bruno|all]" >&2; exit 1 ;;
esac
