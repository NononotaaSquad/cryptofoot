# D9-007: An unclassified candidate gets a sentence, not a cap

- **Status:** accepted
- **Date:** 2026-11-21
- **Type:** answer to the mitigation left open by D24-006
- **Source:** D24-006 ("scoring may cap such candidates, or the report may hide them by default"); judging criterion "Confidence Level quality and limiting false attributions" (25%)
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., grilling session 2026-11-21

## Problem and context
A candidate whose exclusion check was unavailable may be an exchange or a contract. The orchestrator scores it and flags `classificationStatus = UNAVAILABLE`; the Analysis is `PARTIAL`.

## Options considered
1. A 0-point rule with an explaining sentence, level unchanged.
2. The same sentence and a cap at 59.
3. Nothing (only the orchestrator flag).

## Decision
Option 1. `ScoringInput.unclassified` candidates get rule `CLASSIFICATION_UNAVAILABLE` (0) and the sentence "Nie udało się sprawdzić, czy adres należy do giełdy, kontraktu lub innej usługi." The level is computed exactly as for an accepted candidate.

## Consequences and risks
- An unchecked exchange address published on the person's profile can still show 80%+; the flag, the sentence and the `PARTIAL` status carry the warning. A cap remains possible in a later algorithm version.

## Verification
`ConfidenceScoringServiceSpec`: an unclassified candidate keeps the level of an accepted one and ends with the sentence; accepted Attributions come first.

## Related
- Files: `service/ConfidenceScoringService.kt`. Decisions: D24-006, D9-001.

## Reconsider when
The on-chain check is switched off for the demo (all ETH candidates unclassified) or the mentors ask for stronger protection.
