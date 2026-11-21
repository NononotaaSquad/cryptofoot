# D23-011: Every Address Activity stores its rules version and threshold

- **Status:** accepted
- **Date:** 2026-11-21
- **Type:** owner decision after the requirements review of PR #65
- **Source:** task section 3, acceptance test: results saved "wraz z … metadanymi potrzebnymi do późniejszego odtworzenia i zweryfikowania analizy"; cheat sheet KROK 4 (rules explainable and reproducible); precedent D9-008 (algorithm version `confidence-1`)
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., 2026-11-21 ("do 2 and 3")

## Problem and context
An Address Activity stored the source, the time it was read and the transaction references, but not the rules that turned transfers into signals. After a change to the threshold (configurable) or to the rules, an old result could not be explained or reproduced.

## Options considered
1. Store a rules version and the threshold in use with each Address Activity.
2. Store the raw transfers as well, so a result can be recomputed.
3. Store nothing more; rely on git history and the configuration.

## Decision
Option 1.
- `AddressActivity.rulesVersion` = `AddressActivityEvaluator.RULES_VERSION` (`activity-1`), and `AddressActivity.stablecoinSwapThreshold` = the threshold in use. Both are returned in `AddressActivityResponse`.
- `activity-1` covers the signal rules of D23-006, the tag mapping of D23-010, the USD stablecoin contracts and the totals rules of D23-004. Any change to them is a new version.
- Like `confidence-1`, the version is reproduction metadata. The frontend does not show it to the Analyst; it is in the API and the stored snapshot.

## Consequences and risks
- Raw transfers are not stored (option 2). A result can be explained but not recomputed from the snapshot alone; the transaction references let anyone re-read the chain.
- Address Activity stored before versioning (earlier builds of this branch, local databases only) reads back with `rulesVersion` and `stablecoinSwapThreshold` = `null` (unknown), like a Justification stored before `confidence-1`. Without that default one such document made the Case list fail with 500 (seen during verification on 2026-11-21).
- E6 comparison does not yet flag a different activity rules version (see D23-009 A5).

## Verification
`AddressActivityEvaluatorSpec` ("the rules version and the threshold in use are stored with the activity"); `AnalysisReportSpec` ("an Address Activity stored before the rules were versioned still reads back …"); `SyntheticActivitySourceSpec`; `AnalysisReportSpec` (exact JSON with `rulesVersion` and `stablecoinSwapThreshold`); `AnalysisFlowSpec`; E2E "Address Activity: the demo addresses …".

## Related
- Files: `analysis/AnalysisContracts.kt`, `service/AddressActivityEvaluator.kt`, `dto/Responses.kt`, `service/AnalysisReportService.kt`, `frontend/apps/web/lib/api/`. Decisions: D9-008, D23-004, D23-006, D23-008, D23-010.

## Reconsider when
Auditors need recomputation from the snapshot (store the raw transfers), or E6 should compare activity across versions.
