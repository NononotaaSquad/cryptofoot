# D24-005: Scope: orchestration logic only, no production wiring and no HTTP endpoint

- **Status:** proposed (deviation from the issue wording)
- **Date:** 2026-11-21
- **Type:** own proposal; scope limit; deviation from documentation
- **Source:** issue #24 criterion 2 ("An Analysis can be started for a Case"); `rules.md` (no ignoring patterns, test mocks are not integration); ADR-003 package structure
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
`master` has no Case (E1), Mention (E2), candidate/scoring (E3) or Analysis storage (#25) code. A Spring bean needing these ports fails the context (and the full-app integration specs and `OpenApiContractSpec`). Fake production beans would pretend that integration exists.

## Options considered
1. `@Service` orchestrator plus controller `POST .../analyses`, with stub adapters.
2. `@Service` orchestrator only, ports required.
3. Plain Kotlin class with ports; no bean, no controller, no stubs; wiring added when adapters exist.

## Decision (proposed)
Option 3. The orchestrator is a plain class in the new feature package `squad.nononotaa.backend.analysis`, not a Spring stereotype. No endpoint is added or changed: `CaseController.startAnalysis` and `AnalysisController` already exist on master as `TODO` stubs from the epic API contract (`backend/decisions/0001`); they are left untouched because they need Case storage (E1) and the recorder (#25). The API contract and `openapi.json` are unchanged. The feature package departs from the layer folders of ADR-003 because the ports, types and orchestrator are one unit whose names must not clash with types owners are adding in `model/` and `service/`.

## Consequences and risks
- **Unmet in this PR:** starting an Analysis from the UI/API and the real end-to-end flow. The criterion "can be started for a Case" is met at service level only.
- Follow-up when adapters exist: replace the `startAnalysis` stub with `start` + asynchronous `execute`, a `@Configuration` or `@Service` wiring, a start endpoint (then run `./scripts/api-sync.sh`), and the scheduler.
- Specs use mocks; they do not show a working production integration.

## Verification
`./gradlew test`; app startup and `OpenApiContractSpec` stay unaffected.

## Related
- Files: `analysis/AnalysisOrchestrator.kt`. Issues: #24, #25, #16, #21, #22, #9. Decisions: D24-003, D24-004.

## Reconsider when
The first real module adapters or #25's recorder land on `master`.
