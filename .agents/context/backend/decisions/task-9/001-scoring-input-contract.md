# D9-001: Input contract of the Confidence Level algorithm

- **Status:** accepted
- **Date:** 2026-11-21
- **Type:** port change; agreement between "E3: Input for the Confidence Level algorithm" and #9 (issue #9 criterion 1: "agree the shape of the input before coding")
- **Source:** issue #9; D24-003 (ports); D24-006 (unclassified candidates are scored); DS1-003 (port received Mentions); D50-006 (wallet is input, not evidence)
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U. (owner of the input task and implementer of #9), grilling session 2026-11-21

## Problem and context
`ConfidenceScoring.score(candidates, mentions)` (DS1-003) gave the algorithm no Case identifiers and no way to tell accepted from unclassified candidates. The agreed rules need both: the own-profile rule compares URLs with the profile address and the nickname, D50-006 needs to know the Analyst's wallet, and D9-007 adds a sentence for an unclassified candidate.

## Options considered
1. Add parameters one by one: `score(candidates, mentions, identifiers, unclassified)`.
2. One input value object.

## Decision
Option 2, in `analysis/AnalysisContracts.kt`:

```kotlin
data class ScoringInput(accepted, unclassified, mentions, identifiers)
interface ConfidenceScoring { fun score(input: ScoringInput): ScoringResult }
```

- `accepted` and `unclassified` come from `ExclusionClassifier` unchanged (canonical address, network, all `mentionIds`); Excluded Addresses never reach scoring.
- `mentions` are the Mentions of the search; `identifiers` are the identifiers used by the Analysis (`AnalysisRequest.identifiers`, wallet in canonical form).
- Output: one `ScoredAttribution` per candidate, in the order accepted then unclassified, with `confidenceLevel`, `appliedRules` (rule + contribution), `justification` (D9-006); `ScoringResult.algorithmVersion`.
- The orchestrator still sets `classificationStatus = UNAVAILABLE` (D24-006). The algorithm stays a pure function: no database, no clock, no external call.

## Consequences and risks
- Groovy specs that build `ScoredAttribution` positionally pass the Justification text as the 4th argument.
- The algorithm does not search, validate or classify addresses (issue scope); it only reads what the input carries.

## Verification
`AnalysisOrchestratorSpec` (the orchestrator passes accepted, unclassified, Mentions and identifiers), `ExcludedAddressPipelineSpec`, `ExclusionOnchainCheckSpec`, `ConfidenceScoringServiceSpec`.

## Related
- Files: `analysis/AnalysisContracts.kt`, `analysis/AnalysisOrchestrator.kt`. Decisions: D24-003, D24-006, DS1-003, D50-006.

## Reconsider when
A rule needs data the Mention does not carry (e.g. a page type declared by the source, option rejected in D9-003).
