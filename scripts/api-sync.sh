#!/bin/bash
# Refreshes frontend/apps/web/lib/api/openapi.json from the backend code and regenerates the
# Orval client from it. Needs Docker (the backend spec runs against Testcontainers MongoDB).

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Writing openapi.json from the backend..."
(cd "$ROOT_DIR/backend" && ./gradlew integrationTest --tests 'squad.nononotaa.backend.OpenApiContractSpec' -PupdateOpenApi --console=plain -q)

echo "Regenerating the frontend API client..."
(cd "$ROOT_DIR/frontend" && npm run api:gen)

echo "Done. Review and commit:"
git -C "$ROOT_DIR" status --short -- frontend/apps/web/lib/api
