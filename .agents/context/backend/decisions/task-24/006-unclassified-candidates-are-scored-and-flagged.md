# D24-006: Unclassified candidates are scored and flagged

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** adaptation to a team API contract; changes an own earlier proposal
- **Source:** `backend/decisions/0001-epic-api-contract.md`; `dto/Responses.kt` (`AttributionResponse.classificationStatus`), `model/Enums.kt` (`ClassificationStatus`, `ReportLimitation.CLASSIFICATION_UNAVAILABLE`); issue #22 ("unavailable classification is distinguished from a confirmed exclusion"); E5.3, E7.3
- **Proposed by:** AI agent (Claude), after the contract was merged by Maksym U. (PR #31)
- **Approved by:** nobody yet. The contract ADR is marked Accepted by its author, but its owner is not the owner of #24 and it does not describe scoring behaviour.
- **Supersedes:** part of [D24-002](002-analysis-failure-policy.md) (row "A candidate with unavailable classification")

## Problem and context
D24-002 proposed that a candidate whose classification is unavailable is not scored. The merged contract gives every Attribution a `classificationStatus` (`NOT_EXCLUDED` or `UNAVAILABLE`), which only makes sense if such candidates can appear as Attributions.

## Options considered
1. Keep D24-002: do not score unclassified candidates (contract field `UNAVAILABLE` would never be used).
2. Score them and flag the Attribution `UNAVAILABLE`; Analysis is `PARTIAL` with limitation `CLASSIFICATION_UNAVAILABLE`.

## Decision (proposed)
Option 2. The orchestrator passes accepted and unclassified candidates to scoring together and sets `classificationStatus = UNAVAILABLE` on Attributions of unclassified candidates. It does not compute or change the score. The unavailable classification is shown as a limitation, not as a confirmed exclusion and not as a fabricated result.

## Consequences and risks
- An exchange or contract address that could not be checked may appear as a scored lead, flagged as unverified. This weakens the protection against false leads (judged at 25% in the brief). Mitigation to agree with E3 owners: scoring may cap such candidates, or the report may hide them by default.
- Whether a flagged Attribution is shown to the Analyst is an E5 decision.

## Verification
`AnalysisOrchestratorSpec`: "an unavailable classification is flagged on the scored Attribution and makes the analysis partial".

## Related
- Files: `AnalysisOrchestrator.kt`, `AnalysisContracts.kt`. Issues: #22, #24, #9. Decisions: D24-002, D24-003.

## Reconsider when
Maksym U. (#22) or Marcin (#9) prefers not to score unclassified candidates, in which case the contract field must change instead.
