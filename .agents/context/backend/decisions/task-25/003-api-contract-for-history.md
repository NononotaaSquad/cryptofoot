# D25-003: API contract for Case history and one saved Analysis

- **Status:** proposed (changes the merged REST contract)
- **Date:** 2026-11-21
- **Type:** own proposal; contract change
- **Source:** issue #25 criterion 4; `backend/decisions/0001-epic-api-contract.md`; E6.1; E5.4
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet. Frontend owner (Marcin) must be told: generated types changed.

## Problem and context
Contract 0001 defines `GET /cases/{caseId}/analyses` and `GET /analyses/{analysisId}` as stubs returning `AnalysisResponse`. `AnalysisResponse.algorithmVersion` is non-null, yet a `RUNNING` Analysis, or one that failed before scoring, has no algorithm version. The response also has no field for network coverage (E6.1, E5.1).

## Options considered
1. Keep `algorithmVersion` non-null with a placeholder such as "unknown" (fabricated value).
2. Make it nullable.
3. Add a separate summary DTO.

## Decision (proposed)
Implement the two existing endpoints with the existing DTO and change it additively/for honesty: `algorithmVersion` is nullable (null = no scoring ran) and `networks: List<Network>` is added. `GET /analyses/{id}` of an unknown id is 404 with a Polish message. `GET /cases/{caseId}/analyses` returns `[]` for a Case without Analyses or an unknown Case (no Case store to check). `attributionCount`/`excludedAddressCount` stay null while `RUNNING`. `GET .../mentions` and `.../report` remain `TODO` stubs: they need E2/E5 mapping (`DetectedAddressResponse.valid`, justification) and are not part of #25. Neither endpoint reruns or alters an Analysis. `api-sync` was run and `lib/api/` regenerated.

## Consequences and risks
- Frontend code (none exists yet on master) must treat `algorithmVersion` as nullable.
- The history of an unknown Case cannot be told from an empty history until E1 exists.
- `sources[].mentionCount` is derived from the stored Mentions by source name.

## Verification
`AnalysisHistorySpec` (HTTP reads), `OpenApiContractSpec`, `npm run check`.

## Related
- Files: `controller/CaseController.kt`, `controller/AnalysisController.kt`, `dto/Responses.kt`, `frontend/apps/web/lib/api/`. Issues: #25, E5.4 frontend issue. Decisions: D25-001, D25-002.

## Reconsider when
E1 adds Cases (add 404 for unknown Case), or the frontend/E5 owners want a different shape.
