---
name: api-contract
description: >
  Keep the backend REST contract, frontend/apps/web/lib/api/openapi.json and the generated Orval
  client in sync. Use before any frontend change that calls the backend, after any backend change
  to a controller, DTO, validation annotation or ErrorResponse, and whenever a merge or rebase
  conflicts in openapi.json, lib/api/generated/, dto/ or controller/.
---

# API contract

The backend code is the single source of truth. `frontend/apps/web/lib/api/openapi.json` is a
snapshot of springdoc's `/v3/api-docs`, and `frontend/apps/web/lib/api/generated/` is Orval output
from that snapshot. Neither is ever edited by hand.

CI enforces both links:
- backend job: `OpenApiContractSpec` fails when `openapi.json` differs from what the backend serves;
- frontend job: `npm run api:gen` must leave `lib/api/` unchanged.

## Sync command

```bash
./scripts/api-sync.sh   # from the repo root; needs Docker (Testcontainers MongoDB)
```

It rewrites `openapi.json` from the backend and runs `npm run api:gen`. Then run
`npm run check` in `frontend/` and fix the type errors it reveals.

## Before a frontend change that touches the backend

1. Run `./scripts/api-sync.sh`. If `git status` shows changes in `frontend/apps/web/lib/api/`,
   the snapshot was stale: commit the sync on its own (`chore(api): sync openapi client`) before
   your feature, so the feature diff stays readable.
2. Use only the hooks and types in `generated/`. If the endpoint or field you need does not
   exist, it is a backend change first (next section), never a hand-written type or `fetch`.

## After a backend contract change

A contract change is any change to an `@ApiController` method (path, verb, params, return type),
a DTO in `dto/Requests.kt` / `dto/Responses.kt` (fields, nullability, validation annotations), or
`exception/ErrorResponse.kt`.

1. Make the change and its Spock specs.
2. Run `./scripts/api-sync.sh` and `npm run check` in `frontend/`.
3. Commit the backend code, `openapi.json`, `generated/` and the frontend fixes together, in the
   same commit or PR. A PR that changes one side only is broken on CI.
4. Prefer additive changes (new optional field, new endpoint). For a rename or removal, update
   every frontend usage in the same PR; never leave the frontend on the old shape.
5. If `ErrorResponse` changes, also update `apiErrorBodySchema` in
   `frontend/apps/web/lib/api/api-error.ts` and its tests.

## Resolving merge conflicts

Never merge `openapi.json` or `generated/` by hand, and never pick "ours" or "theirs" for them:
either side alone drops the other branch's endpoints.

1. Resolve conflicts in the backend sources (`controller/`, `dto/`, `exception/`) first, keeping
   both branches' endpoints and fields.
2. Take either side of the generated files to clear the markers:
   `git checkout --theirs -- frontend/apps/web/lib/api/openapi.json frontend/apps/web/lib/api/generated`
3. Regenerate from the merged backend: `./scripts/api-sync.sh`.
4. Run `./gradlew spotlessApply integrationTest` in `backend/` and `npm run check` in `frontend/`,
   fix the frontend code that uses changed hooks, then `git add` and continue the merge.

Without Docker, start the merged backend instead (`./scripts/run-backend.sh`) and run
`curl -s http://localhost:8080/v3/api-docs -o frontend/apps/web/lib/api/openapi.json` followed by
`npm run api:gen` in `frontend/`. `OpenApiContractSpec` ignores key order and the `servers` entry.
