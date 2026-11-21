# D26-005: Manual rerun (service only, not wired)

- **Status:** proposed (scope limit and missing contract)
- **Date:** 2026-11-21
- **Type:** own proposal; missing contract with the E1 owner; deviation from the issue wording
- **Source:** issue #26 criterion 1; E6.2; `backend/decisions/task-24/005-scope-no-wiring-no-http.md`; D25-001; contract `POST /cases/{caseId}/analyses` (202, 409 when one is running)
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
A rerun must create a new Analysis from the Case's current identifiers and leave earlier results available during and after it. There is no Case store (E1) and the orchestrator ports for search, candidates, exclusions and scoring have no production implementations, so the endpoint cannot be wired without fakes.

## Options considered
1. Wire `startAnalysis` with stub adapters.
2. A plain service with a small port for current identifiers, tested with the real recorder and stubbed modules; endpoint left as a stub.

## Decision (proposed)
Option 2. `AnalysisRerunService.rerun(caseId)`: gets identifiers from `CaseIdentifierSource.currentIdentifiers(caseId)` (proposed port for the E1 owner; null means unknown Case, 404); refuses with 409 ("Analiza tej sprawy już trwa.") while a `RUNNING` Analysis of the Case exists; starts a `MANUAL` Analysis through the orchestrator (`start`, stored `RUNNING`) and runs `execute` on the given `Executor`; returns the `RUNNING` `AnalysisResponse`. Earlier results stay available because every Analysis is its own immutable record (D25-001); a failed rerun is stored as `FAILED`. Not a Spring bean; `CaseController.startAnalysis` stays a `TODO` stub.

## Consequences and risks
- **Unmet:** a working "rerun" endpoint and UI action until E1, the module adapters and wiring exist.
- The overlap check is check-then-insert, not atomic; two simultaneous reruns can both start. A partial unique index would fix it but would also forbid the several `RUNNING` records that #25 specs create.
- Errors from recording the final result are logged and leave the Analysis `RUNNING` (D25-004).

## Verification
`AnalysisRerunSpec` (real recorder and MongoDB): new manual Analysis with current identifiers, earlier result byte-identical while running and after completion and failure, 409 while running, 404 for an unknown Case.

## Related
- Files: `service/AnalysisRerunService.kt`, `repository/AnalysisRepository.kt`. Issues: #24, #25, #26, E1 backend issue. Decisions: D24-005, D25-001, D25-004.

## Reconsider when
E1 provides Cases, or the first real module adapters allow wiring.
