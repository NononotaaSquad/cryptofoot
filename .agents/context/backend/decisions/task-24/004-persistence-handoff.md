# D24-004: Handing the result to storage (#25 boundary)

- **Status:** proposed (not agreed with the #25 owner)
- **Date:** 2026-11-21
- **Type:** own proposal; E6-D4 is an open team decision
- **Source:** issue #24 ("Open decisions: who provides persistence, execution status and periodic triggering (E6-D4)"); #25 acceptance criteria; `epics/E6-returning-to-a-case.md` E6.1, E6-D4
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
#25 owns saving every Analysis: identifier, start/end times, trigger, outcome, identifiers used, coverage, algorithm version, references to Mentions, Attributions and Excluded Addresses, plus reading history. #24 decides the outcome. A visible `RUNNING` state requires a stored Analysis before the work starts.

## Options considered
1. Orchestrator writes to MongoDB itself (duplicates #25, breaks the split).
2. Orchestrator calls a port: `begin(request) -> analysisId` (stores `RUNNING`), later `finish(analysisId, AnalysisResult)`.
3. Orchestrator returns the result and the caller stores it (no visible `RUNNING`).

## Decision (proposed)
Option 2 (`AnalysisRecorder`), exposed as `start(request) -> analysisId` (stores `RUNNING`) and `execute(analysisId, request)` (runs the modules, then `finish`); `run` does both. The split exists because the contract answers `POST /cases/{caseId}/analyses` with 202 and a `RUNNING` Analysis that the client polls: the caller starts, returns the id, then runs `execute` asynchronously. #25 owns the entity, repository, timestamps, immutability of history, and the read API. #24 owns status derivation and the content of `AnalysisResult`. If a stage throws, `finish` is still called with `FAILED`. If `begin` or `finish` throws, the exception propagates (nothing is hidden), and a stuck `RUNNING` Analysis is possible; handling stale `RUNNING` records is left to #25/E6.4.

## Consequences and risks
- #25 must implement `AnalysisRecorder`; until then the orchestrator cannot run in the application.
- The `409 when one is already running` rule of the contract (overlap) is not enforced here; it belongs to the caller/#25 (E6-D3).
- Execution trigger (who calls `run`, scheduling, overlap policy) is not decided here (E6-D3, E6-D4).

## Verification
`AnalysisOrchestratorSpec` asserts what is passed to `finish` for every outcome.

## Related
- Files: `AnalysisContracts.kt` (`AnalysisRecorder`, `AnalysisResult`). Issues: #24, #25. Decisions: D24-001, D24-003, D24-005.

## Reconsider when
#25 chooses a different persistence shape, or E6-D4 assigns execution to another component.
