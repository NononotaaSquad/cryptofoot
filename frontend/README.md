# Frontend

Next.js app (`apps/web`) and shared shadcn UI package (`packages/ui`) in a Turborepo. Rules for agents: `../.agents/rules/frontend.md`. Architecture: `../.agents/context/frontend/frontend-architecture.md`.

## Requirements

Node >= 22.18, npm.

## Commands

Run from `frontend/`:

```bash
npm install
npm run dev        # http://localhost:3000
npm run check      # typecheck, lint, format check, unit tests (same as CI)
npm run build
npm run format
npm run test
npm run api:gen    # regenerate apps/web/lib/api/generated/ from openapi.json
```

## Configuration

Copy `apps/web/.env.example` to `apps/web/.env.local`. `NEXT_PUBLIC_API_URL` is the backend URL the browser calls (dev-only default in `apps/web/lib/env.ts`; `next build` fails without it).

> **Warning:** `NEXT_PUBLIC_*` values are inlined at `next build`. Build the Docker image with the real public backend URL:
> `docker build --build-arg NEXT_PUBLIC_API_URL=https://<backend-url> -t frontend .`
> Changing the container's environment afterwards has no effect.

## Docker

Run from `frontend/`:

```bash
docker build --build-arg NEXT_PUBLIC_API_URL=http://localhost:8080 -t frontend .
docker run --rm -p 3000:3000 frontend   # http://localhost:3000
```

Or from the repository root:

```bash
docker build -f frontend/Dockerfile --build-arg NEXT_PUBLIC_API_URL=http://localhost:8080 -t frontend frontend
docker run --rm -p 3000:3000 frontend
```

## Updating the API client

`apps/web/lib/api/openapi.json` and `apps/web/lib/api/generated/` are committed. Refresh them after every backend API change, from the repo root (needs Docker):

```bash
./scripts/api-sync.sh
```

It writes `openapi.json` from the backend code (via the `OpenApiContractSpec` integration spec) and runs `npm run api:gen`. Without Docker, start the backend and run `curl -s http://localhost:8080/v3/api-docs -o apps/web/lib/api/openapi.json && npm run api:gen` here instead.

Fix the type errors that appear, then commit `openapi.json`, `generated/` and the screen that uses them together. CI fails when either is out of date. Never edit them by hand; resolve merge conflicts there by regenerating (see `.agents/skills/api-contract/SKILL.md`).

## Adding UI components

```bash
npx shadcn@latest add <name> -c apps/web
```

Components land in `packages/ui/src/components/`. Import them as `@workspace/ui/components/<name>`. Agree changes to `packages/ui` with the other frontend developer first.
