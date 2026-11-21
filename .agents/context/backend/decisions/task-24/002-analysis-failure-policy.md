# D24-002: Failure policy: no results vs failure

- **Status:** superseded by [D24-006](006-unclassified-candidates-are-scored-and-flagged.md) (only the row about unavailable classification; the rest stays proposed)
- **Date:** 2026-11-21
- **Type:** requirement from the brief (never store a failure as zero findings) + own proposal (the mapping)
- **Source:** issue #24 criterion 3; E6.1 ("Failure is retained in history"); E2 story 3; E7.3 ("Unavailable source or classification service: partial/unknown, no fabricated zero")
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
A module failure must not look like a successful Analysis with zero findings, and "no Mentions" must be distinguishable from "could not check". The documents say this but do not define which failure yields `PARTIAL` and which `FAILED`.

## Options considered
1. Any failure gives `FAILED`.
2. Failure of a module that blocks Attributions gives `FAILED`; a gap in coverage gives `PARTIAL`.
3. Continue past every failure and always return `PARTIAL`.

## Decision (proposed)
Option 2:

| Situation | Status | Recorded failure |
|---|---|---|
| Every stage ran, nothing unavailable (also with zero Mentions) | COMPLETED | none |
| Some sources unavailable, search returned the rest | PARTIAL | `SOURCE_UNAVAILABLE:<source>` |
| A candidate with unavailable classification | PARTIAL, candidate **not scored** | `CLASSIFICATION_UNAVAILABLE:<address>` |
| All sources unavailable, or no source checked | FAILED | `ALL_SOURCES_UNAVAILABLE` / `NO_SOURCE_CHECKED` |
| Search, candidate preparation, exclusion or scoring throws | FAILED | `MODULE_ERROR` |

Later stages are skipped after a `FAILED` stage. Output of earlier stages is still handed to the recorder. Exception messages are not stored (they may contain personal data); only the stage and a fixed code. Scoring an unclassified candidate was rejected here; see D24-006 for the reversal after the API contract.

## Consequences and risks
- Zero Mentions from healthy sources is `COMPLETED`: "no results" is told apart from failure by the status plus the source coverage.
- A scoring failure gives `FAILED` even though Mentions exist; the Analyst loses the partial view unless E5 shows the stored Mentions.
- Retry and timeout policy are not defined.

## Verification
`AnalysisOrchestratorSpec`: correct flow, partial (source, classification), total failure (all sources, scoring, search, preparation, exclusion), zero Mentions.

## Related
- Files: `AnalysisOrchestrator.kt`, `AnalysisOrchestratorSpec.groovy`. Issues: #24, #16, #22, #9. Decisions: D24-001.

## Reconsider when
Module owners define their own failure signals, the team wants retries, or mentors want a different treatment of zero Mentions.
