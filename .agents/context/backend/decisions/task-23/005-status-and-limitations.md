# D23-005: Unavailable activity makes the Analysis PARTIAL, never FAILED

- **Status:** accepted (owner)
- **Date:** 2026-11-21
- **Type:** owner decision; extends D24-002
- **Source:** D24-001/D24-002 (`COMPLETED` means nothing was unavailable); D24-006 (unavailable classification gives `PARTIAL`); E5.1 (unavailable blockchain data has its own message)
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., grilling 2026-11-21

## Problem and context
What happens to the Analysis status when Address Activity could not be read for some Attribution?

## Options considered
1. `PARTIAL` plus the limitation `BLOCKCHAIN_DATA_UNAVAILABLE`.
2. Status unchanged; only the limitation and the per-Attribution status.

## Decision
Option 1.
- Each Attribution with an `UNAVAILABLE` outcome adds `StageFailure(ADDRESS_ACTIVITY, "ADDRESS_ACTIVITY_UNAVAILABLE:<address>")`, so the Analysis becomes `PARTIAL`, and the report gets `BLOCKCHAIN_DATA_UNAVAILABLE` (between `CLASSIFICATION_UNAVAILABLE` and `ANALYSIS_INCOMPLETE`).
- If the collector itself throws, the orchestrator records `StageFailure(ADDRESS_ACTIVITY, "MODULE_ERROR")` and marks every Attribution `UNAVAILABLE`. Address Activity is supplementary: the Attributions are kept and the Analysis is never `FAILED` because of E4.
- `NOT_CHECKED` (D23-002) is a rule, not a failure, and changes nothing.

## Consequences and risks
- With the live explorers off (the default), any Attribution outside the demo fixture makes a real Analysis `PARTIAL`. E6 comparison then reports `PARTIAL_ANALYSIS`, which is accurate.
- `ScheduledAnalysisWiringSpec` (bytecode check off) now expects `BLOCKCHAIN_DATA_UNAVAILABLE`: the unclassified contract `0xde0b…` is a 60% Attribution outside the fixture.

## Verification
`AnalysisOrchestratorSpec`: "unavailable Address Activity makes the Analysis partial with its limitation, never failed", "a failing Address Activity module …", "Address Activity is not collected without Attributions or after a failed scoring"; `AddressActivityLiveSpec` (demo Analysis with an unclassified contract); E2E "missing Address Activity is unavailable with its limitation, never zero activity".

## Related
- Files: `analysis/AnalysisOrchestrator.kt`, `model/Enums.kt` (`ReportLimitation.BLOCKCHAIN_DATA_UNAVAILABLE`, now in use). Decisions: D24-001, D24-002, D24-006.

## Reconsider when
The team decides that supplementary data must not affect the status (then option 2 is a two-line change in the orchestrator).
