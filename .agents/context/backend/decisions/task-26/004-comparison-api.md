# D26-004: Comparison API and its errors

- **Status:** proposed (changes the merged REST contract additively)
- **Date:** 2026-11-21
- **Type:** own proposal; contract change
- **Source:** issue #26 criteria 2, 5; `backend/decisions/0001-epic-api-contract.md` (`GET /cases/{caseId}/comparison?earlierAnalysisId&laterAnalysisId`); E6.3
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet. The frontend owner (E6.2/E6.3 frontend issue) must be told: generated types changed.

## Problem and context
The endpoint exists as a stub. The contract has no evidence references or absence flag, and error cases are undefined.

## Decision (proposed)
Implement the existing endpoint. Additive fields on `AttributionChangeResponse`: `earlierMentionIds`, `laterMentionIds`, `absenceVerifiable` (D26-002, D26-003); enum value `NETWORKS_DIFFER`. Errors (Polish messages): unknown analysis 404; analyses not both of the path Case 400 ("Porównać można tylko analizy tej samej sprawy."); a running Analysis 409; earlier started after later 400. Reads only; nothing is rerun or modified. `api-sync` was run and `lib/api/` regenerated.

## Consequences and risks
- The Case is not verified to exist beyond its Analyses (no Case store, E1).
- Analyses are labelled "earlier"/"later" by the caller; a reversed pair is rejected rather than swapped.

## Verification
`AnalysisComparisonSpec` (HTTP: result, different Cases, unknown, running, reversed order, read-only), `OpenApiContractSpec`, `npm run check`.

## Related
- Files: `controller/CaseController.kt`, `service/AnalysisComparisonService.kt`, `frontend/apps/web/lib/api/`. Issues: #26, #27. Decisions: D26-001, D26-003.

## Reconsider when
Frontend or E5 owners need a different shape.
