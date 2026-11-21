# D26-001: Comparison categories

- **Status:** proposed (E6-D2 is not confirmed by the team)
- **Date:** 2026-11-21
- **Type:** own proposal; the categories are the proposal of `epics/E6-returning-to-a-case.md` E6.3
- **Source:** issue #26 criterion 3; E6.3; E6-D2; `BUSINESS-SPEC.md` open team question 9 ("Not confirmed"); `backend/decisions/0001-epic-api-contract.md` (enum `AttributionChangeType`, contract-ahead-of-implementation, not an approval of the rules)
- **Proposed by:** AI agent (Claude), for the assignee of #26
- **Approved by:** nobody yet. Needed: team and E2/E3 owners (E6-D2)

## Problem and context
E6.3 proposes four categories but marks them unconfirmed. The enum already exists in the merged REST contract, so implementing it fixes the rules in practice.

## Options considered
1. Report every difference in Mentions and scores.
2. The four proposed categories, decided only from Confidence Level and exclusion state.
3. Wait for confirmation.

## Decision (proposed)
Option 2. Per (network, address), comparing the earlier and later Analysis:

| Earlier | Later | Result |
|---|---|---|
| Attribution | absent | `NOT_OBSERVED` |
| absent | Attribution | `NEW` |
| Attribution | Attribution, other Confidence Level | `CONFIDENCE_LEVEL_CHANGED` |
| Attribution | Excluded Address | `EXCLUSION_CHANGED` (later reason set) |
| Excluded Address | Attribution | `EXCLUSION_CHANGED` (earlier reason set) |
| Excluded Address | Excluded Address, other reason | `EXCLUSION_CHANGED` |

Nothing is reported for an unchanged address, an unchanged exclusion, or an exclusion seen on one side only (not an Attribution change). A different `classificationStatus` (`NOT_EXCLUDED` vs `UNAVAILABLE`) alone is not a category. Changes are sorted by network, then address.

## Consequences and risks
- A changed score is not described as changed behaviour; only both values are shown.
- Whether a newly appearing or disappearing exclusion should be reported, and whether `classificationStatus` changes count, are open points for the team.
- If the team picks other categories, the enum and tests change.

## Verification
`AnalysisComparerSpec` (every row), `AnalysisComparisonSpec`.

## Related
- Files: `service/AnalysisComparer.kt`, `model/Enums.kt`. Issues: #26, #24, #25. Decisions: D26-002, D26-003.

## Reconsider when
E6-D2 is decided, or E3 changes how exclusions or Confidence Levels are produced.
