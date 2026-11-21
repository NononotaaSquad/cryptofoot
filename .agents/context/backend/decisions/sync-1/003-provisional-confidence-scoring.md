# DS1-003: Provisional Confidence Level algorithm; the scoring port receives the Mentions

- **Status:** superseded by task #9 (`../task-9/`, D9-001 to D9-008): `ProvisionalConfidenceScoring` is deleted, the port takes `ScoringInput`, algorithm `confidence-1`
- **Date:** 2026-11-21
- **Type:** own proposal; temporary implementation of another owner's task; port change
- **Source:** issue #9 acceptance criteria; challenge cheat sheet KROK 2 (bands, +15 for independent sources, "several occurrences within one service are no extra confirmation"); E7.3 (copied Mention gives no bonus); D24-003 open point "whether Mention content is needed by scoring"
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody. Owner of the algorithm: Marcin (#9)

## Problem and context
Without a `ConfidenceScoring` bean the flow cannot run, the demo has no Attributions and E5/E6 cannot be integrated. #9 has no code yet. The port only received candidates (`score(candidates)`), so no rule based on sources could be written, although the cheat sheet's synergy rule needs the origin of each Mention.

## Options considered
1. Leave the flow unwired until #9 lands.
2. A fake that returns a constant level.
3. A small, honest, rule-based placeholder behind the same port, clearly versioned, that #9 replaces.

## Decision (proposed)
Option 3, plus a port change.
- **Port:** `ConfidenceScoring.score(candidates, mentions)`; the orchestrator passes the Mentions of the search. Scoring stays a pure function (no database), so "same Mentions -> same Confidence Level" is testable.
- **Algorithm `provisional-1`** (`ProvisionalConfidenceScoring`, built in `AnalysisConfig`):
  - `MENTIONED_WITH_IDENTIFIER` = 40: the address appears in a Mention that also names a narrowing identifier (the search keeps only such Mentions). Lower bound of the cheat-sheet band 40–59 ("needs a second trace"): without classifying the kind of source the algorithm never claims more.
  - `INDEPENDENT_SOURCES` = +15: at least two supporting Mentions with a different origin (URL host, `www.` ignored) and a different text (case and whitespace ignored). Several Mentions on one service, or a verbatim copy on another service, give no bonus.
  - Level = sum of contributions, capped to 0..100; rules and contributions are stored as `appliedRules`.

## Consequences and risks
- The demo shows 55% for an address confirmed on two services and 40% for a single Mention; the 80–100 and 60–79 bands (own profile, P2P ad) are out of reach until #9 classifies source kinds.
- Not handled (left to #9, see DS1-007): the wallet entered by the Analyst is not evidence (D50-006); an address quoted in a warning about another person (E7.3); the Justification text the frontend now expects (comment on #9 of 2026-11-21).
- #9 replaces the `confidenceScoring` bean in `AnalysisConfig` and keeps the port; the orchestrator and storage do not change.

## Verification
`ProvisionalConfidenceScoringSpec` (base, bonus, no bonus within one service or for a copied text, order independence, version); orchestrator specs assert the Mentions are passed; `AnalysisFlowSpec` checks the demo levels.

## Related
- Files: `service/ProvisionalConfidenceScoring.kt`, `analysis/AnalysisContracts.kt`, `analysis/AnalysisOrchestrator.kt`, `config/AnalysisConfig.kt`. Issues: #9. Decisions: D24-003, D24-006, D50-006. Diagram: `08-confidence-scoring.puml`.

## Reconsider when
#9 is implemented: delete `ProvisionalConfidenceScoring`, mark this decision superseded.
