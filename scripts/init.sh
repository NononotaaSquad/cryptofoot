#!/bin/bash

set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <project-name> [description]"
  echo "Example: $0 \"Recipe Finder\" \"Find recipes from what is in your fridge\""
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! grep -q '{{PROJECT_NAME}}' README.md; then
  echo "Project is already initialized."
  exit 1
fi

NAME="$1"
DESCRIPTION="${2:-$1}"

APP=$(echo "$NAME" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/-/g; s/^-+//' | cut -c1-30 | sed -E 's/-+$//')
[[ "$APP" =~ ^[a-z] ]] || APP="app-$APP"

escape() {
  printf '%s' "$1" | sed -e 's/[\\|&]/\\&/g'
}

replace() {
  local file="$1"
  shift
  sed "$@" "$file" > "$file.tmp"
  mv "$file.tmp" "$file"
}

echo "Filling README placeholders..."
replace README.md \
  -e "s|{{PROJECT_NAME}}/|$(escape "$(basename "$ROOT_DIR")")/|g" \
  -e "s|{{PROJECT_NAME}}|$(escape "$NAME")|g" \
  -e "s|{{PROJECT_DESCRIPTION}}|$(escape "$DESCRIPTION")|g" \
  -e "s|{{BACKEND_PORT}}|8080|g" \
  -e "s|{{FRONTEND_PORT}}|3000|g" \
  -e "s|{{PACKAGE_PATH}}|squad/nononotaa/backend|g"

echo "Setting APP_NAME=$APP in docker-compose.yaml..."
replace backend/docker-compose.yaml -e "s|APP_NAME: .*|APP_NAME: $APP|"

echo "Enabling shared git hooks from .githooks/..."
git config core.hooksPath .githooks

echo "Done. Next step:"
echo "  git add -A && git commit -m \"chore: initialize $APP from template\""
