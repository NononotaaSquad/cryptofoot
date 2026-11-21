Closes #

## What

<!-- What changes, in one or two sentences. -->

## Why

<!-- The problem or demo step this serves. -->

## How

<!-- Approach and key files. -->

## Reversibility

<!-- Easy to undo? yes/no and how. Flag anything hard to reverse: data model, API contract, shared packages/ui. -->

## Checklist

- [ ] `npm run check` passes (frontend) / `./gradlew spotlessCheck test` passes (backend)
- [ ] `./scripts/api-sync.sh` re-run and `frontend/apps/web/lib/api/` committed if the backend contract changed
- [ ] Visual change verified in the browser (or: no visual change)
