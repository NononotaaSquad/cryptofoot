# D26-003: Comparability limitations and the meaning of "not observed"

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** requirement from E6.3 + own proposal for the rules; contract addition
- **Source:** issue #26 criterion 5; E6.3 ("Not observed does not mean no longer belongs ... states that apparent absence cannot be verified"); `domain-model.md`; contract enum `ComparabilityLimitation`
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
E6.3 requires stating limitations when identifiers, source/network scope, algorithm version or completeness differ, and not presenting an unverifiable absence as fact. The merged enum lacks a network limitation, and the change DTO has nowhere to say that an absence is unverifiable.

## Options considered
1. Report `NOT_OBSERVED` only when the later Analysis fully covers the same scope, otherwise a fifth category.
2. Always report `NOT_OBSERVED` plus a per-change flag.
3. Rely on the top-level limitation list only.

## Decision (proposed)
Option 2, with additions to the contract:
- `ComparabilityLimitation` gains `NETWORKS_DIFFER`. Limitations: `IDENTIFIERS_DIFFER` (sets of type+value), `SOURCE_COVERAGE_DIFFERS` (source name + status), `NETWORKS_DIFFER`, `ALGORITHM_VERSION_DIFFERS` (null counts as a value), `PARTIAL_ANALYSIS` (either Analysis not `COMPLETED`). The full `AnalysisResponse` of both sides (identifiers, sources, networks, version, status) is returned, so the differences are visible, not only flagged.
- `AttributionChangeResponse` gains `absenceVerifiable` (only for `NOT_OBSERVED`, else null): true only if the later Analysis is `COMPLETED`, covers the address's network, and every source that supported the earlier Attribution was `CHECKED` again. Otherwise false: the absence is not evidence that the address left the person.
- A `FAILED` Analysis can be compared (all its earlier Attributions appear as unverifiable `NOT_OBSERVED`); a `RUNNING` one cannot (409).
- Not verifiable in the other direction: `NEW` may be an address an earlier partial Analysis could not see; this is covered only by `PARTIAL_ANALYSIS`/`SOURCE_COVERAGE_DIFFERS`.

## Consequences and risks
- A comparison against a failed Analysis is noisy but honest.
- Source names must be stable between Analyses; a renamed source reads as a coverage difference.
- The `absenceVerifiable` rule is mine; the team may want a stricter or looser one.

## Verification
`AnalysisComparerSpec` (table of verifiable cases, every limitation), `AnalysisComparisonSpec` (unavailable source flagged).

## Related
- Files: `service/AnalysisComparer.kt`, `model/Enums.kt`, `dto/Responses.kt`. Issues: #26, #25. Decisions: D25-005, D26-001.

## Reconsider when
The team confirms E6-D2, or the E4 blockchain coverage becomes part of comparability.
