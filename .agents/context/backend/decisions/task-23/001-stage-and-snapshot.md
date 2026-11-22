# D23-001: Address Activity is an Analysis stage, stored with the Attribution

- **Status:** accepted (owner)
- **Date:** 2026-11-21
- **Type:** owner decision (mentor question 2, "in what scope", still open)
- **Source:** issue #23; E5.4 (reopening never reruns); D25-001 (immutable snapshot); task section 3 (results saved with the time of collection and metadata needed to reproduce the Analysis)
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., grilling 2026-11-21. Not approved by mentors.

## Problem and context
Address Activity must be shown inside the report of a saved Analysis. Chain data changes over time, so data fetched later than the Analysis would not belong to it.

## Options considered
1. A fifth stage of every Analysis, after scoring; the outcome is stored in the Analysis snapshot.
2. On request: a button per Attribution and a separate endpoint that fetches on demand.

## Decision
Option 1. A new port `AddressActivityCollector.collect(attributions): Map<Candidate, AddressActivityOutcome>` (stage `ADDRESS_ACTIVITY`) runs after `ConfidenceScoring`. The orchestrator stores each outcome on its `ScoredAttribution.addressActivity` (status + data); an Attribution missing from the map was not checked. Excluded Addresses never get Address Activity (they are not Attributions). Every activity records its source, whether it is synthetic, and `checkedAt`, the time it was read.

## Consequences and risks
- Reopening a report returns the activity as it was read; it is never fetched again (E5.4, D25-001).
- An Analysis takes longer when live explorers are on (a few HTTP calls per checked Attribution, 5 s timeout each).
- Option 2 conflicted with the snapshot policy: activity would be stored apart from the Analysis and dated later.
- Analyses stored before E4 read back with `addressActivity = null`, reported as `NOT_CHECKED`.

## Verification
`AnalysisOrchestratorSpec` "Address Activity is collected for the scored Attributions and stored with each of them"; `AnalysisFlowSpec` (demo report); `AnalysisReportSpec` (stored activity round-trips through MongoDB).

## Related
- Files: `analysis/AnalysisContracts.kt`, `analysis/AnalysisOrchestrator.kt`, `config/AnalysisConfig.kt`. Decisions: D23-002, D23-005, D24-003, D25-001.

## Reconsider when
The mentors want activity on request only, or a refresh of activity for an old Analysis (that would be a new Analysis, not an update).
