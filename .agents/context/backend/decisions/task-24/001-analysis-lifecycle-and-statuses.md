# D24-001: Analysis lifecycle and status names

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** requirement from the brief (names) + own proposal (transitions)
- **Source:** issue #24, acceptance criterion 2 (running, completed, partial, failed); `epics/E6-returning-to-a-case.md` E6.1; `domain/BUSINESS-CONTEXT.md` ("Analysis")
- **Proposed by:** AI agent (Claude), working for the assignee of #24
- **Approved by:** nobody yet

## Problem and context
An Analysis needs a visible outcome. #24 and E6.1 name four states but E6.1 says the exact names "must be aligned with the domain model", which was an empty template. Transitions and the meaning of `partial` are not defined anywhere.

## Options considered
1. Four states exactly as in #24 (`RUNNING`, `COMPLETED`, `PARTIAL`, `FAILED`), trigger kept as a separate field.
2. Add `CANCELLED`/`SKIPPED` states for the scheduler overlap policy (E6.4).

## Decision (proposed)
Option 1, reusing `AnalysisStatus` and `AnalysisTrigger` from `model/Enums.kt` (added by Maksym U. in the epic API contract, `backend/decisions/0001`) instead of defining own enums. `RUNNING` is set when the Analysis starts. It moves once, to one terminal state: `COMPLETED` (every stage ran, nothing unavailable), `PARTIAL` (usable output exists but part of the coverage is missing), `FAILED` (no usable Attributions can be produced). Terminal states never change. Trigger (`MANUAL`, `SCHEDULED`) is separate. A skipped scheduled attempt (E6-D3) is out of this decision.

## Consequences and risks
- Names are added to `domain-model.md` and `glossary.md`; the whole team must accept them.
- `PARTIAL` vs `FAILED` boundary is defined in D24-002.
- Risk: #25 may want extra states (e.g. skipped); then supersede this decision.

## Verification
`AnalysisOrchestratorSpec` covers each terminal state.

## Related
- Files: `backend/.../analysis/AnalysisContracts.kt`, `backend/.../model/Enums.kt`, `backend/decisions/0001-epic-api-contract.md`, `.agents/context/domain-model.md`, `.agents/context/glossary.md`
- Issues: #24, #25, E6-D3. Decisions: D24-002.

## Reconsider when
#25 or the scheduler (E6.4) needs more states, or the team renames the states.
