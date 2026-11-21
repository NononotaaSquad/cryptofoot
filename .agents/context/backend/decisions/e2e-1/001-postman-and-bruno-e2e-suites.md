# DE1-001: Postman and Bruno E2E suites of the backend API

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** own proposal, on request ("e2e backend tests are for Postman and Bruno, in `.e2e/backend`")
- **Source:** `product.md` demo scenario and invariants, `domain-model.md`, the Spock integration specs in `backend/src/integrationTest`
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
The Spock integration specs test the backend in-process with Testcontainers and a stubbed on-chain service. Nothing checks a deployed backend (Docker, Cloud Run) over HTTP, and the team uses Postman and Bruno to explore the API.

## Decision (proposed)
- Two collections with the same 56 requests, in the same order and with the same assertions: `.e2e/backend/postman/` (collection v2.1 + `local` environment) and `.e2e/backend/bruno/` (`.bru` files + `local` environment). The only variable is `baseUrl`.
- One ordered run tells the demo story: health, Case validation, Case creation and reads, a manual Analysis of the synthetic demo person with its report and Mentions, a rerun with history and comparison, the wallet-as-input and no-results edge cases, the schedule, API errors and CORS. Ids are passed between requests as runtime variables (`caseId`, `analysisId`, ...).
- An Analysis runs asynchronously (202). `Wait for ...` requests poll `GET /api/1/analyses/{id}` in their pre-request script until it leaves `RUNNING` (Postman `pm.sendRequest` with timers, Bruno `axios` with `bru.sleep`; at most 60 s).
- Assertions are exact where the backend is deterministic (validation messages, BTC Attributions, rules and Justification, Mentions, schedule times computed without `Intl`). Where the result depends on the public on-chain services, the suite asserts the invariant instead: each ETH candidate is exactly one of an Excluded Address `SMART_CONTRACT`, an `UNAVAILABLE` Attribution (with `CLASSIFICATION_UNAVAILABLE`) or a `NOT_EXCLUDED` Attribution, each with exact details. Comparison changes are only checked for BTC.
- Finding: on mainnet the demo person's ETH address `0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed` has contract code, so a backend with internet access excludes it as `SMART_CONTRACT`. The demo then shows only the two BTC Attributions. `AnalysisFlowSpec` stubs it as a private account. Whether the fixture should use an address without code is for the owner of the demo fixture (E7.1).
- Timing races (409 while running) and the `local`-only demo cadence stay in the Spock specs.
- `./scripts/e2e-backend.sh [postman|bruno|all]` runs the suites with `npx` and pinned CLI versions (`newman@6.2.3`, `@usebruno/cli@4.3.0`); nothing is added to the repo's dependencies.

## Verification
Both suites passed (134 assertions each) against the backend image of this branch in Docker, both with the on-chain check enabled (both ETH addresses excluded) and with it disabled (`EXCLUSION_ONCHAIN_CHECK_ENABLED=false`, both `UNAVAILABLE`). Those runs used a local runner that executes the collections' own scripts with `pm` / `bru` shims and chai, not Newman or the Bruno CLI, which could not be downloaded at the time. Both suites list the same requests in the same order.

## Related
- Files: `.e2e/backend/`, `scripts/e2e-backend.sh`. Specs mirrored: `CaseCreationSpec`, `AnalysisFlowSpec`, `AnalysisReportSpec`, `AnalysisComparisonSpec`, `ScheduleApiSpec`, `RestExceptionHandlerSpec`, `HealthSpec`.

## Reconsider when
The API changes (both collections must follow it), the suites should run in CI against a deployed backend, Cases get a delete endpoint (clean up after a run), or the synthetic `demo-v1` fixture changes (the expected Confidence Levels follow it).
