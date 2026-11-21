# D50-006: The wallet entered by the Analyst is input, not evidence

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** own proposal; requirement of the issue and of the business rules; contract for the E3 and E5 owners
- **Source:** issue #50 ("protection against wrong attribution"); `BUSINESS-SPEC.md` M3a; `epics/E3-scoring-attributions.md`; `epics/E5-report-for-the-official.md` (facts, calculations, conclusions); `task-24/006-unclassified-candidates-are-scored-and-flagged.md`; `task-22` (classification flow)
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet. Needed: Maksym U. and Marcin (E3), the E5 owners

## Problem and context
Three things must never be mixed: the address given by the Analyst, an address found in a public source, and an Attribution confirmed by evidence. Adding the field must not make the address belong to the person.

## Options considered
Add a flag on every candidate and Attribution; recognise input-derived data by comparing with the identifiers of the Analysis; do nothing.

## Decision (proposed)
- The wallet is stored only as a Case identifier and, per Analysis, in the identifiers used (`IdentifierType.WALLET_ADDRESS`). It is never stored as a Mention and creates no Mention, candidate or Attribution.
- **Contract for E3:** the wallet gives no Confidence Level and no independent-source bonus; a Confidence Level for the person and that address rests only on Mentions. The Mentions found by searching for the wallet contain the wallet by construction, so they are not independent evidence that the person owns it; the scoring rules must account for it (not implemented here).
- **Contract for E5:** an Attribution or Excluded Address whose canonical address equals the wallet of the Analysis (`AnalysisResponse.identifiers.walletAddress`) is shown as input-derived, separate from the facts found in sources.
- **Classification:** an exchange, mixer or contract address entered as a wallet is accepted as input (no classification at Case creation, no external call with the Analyst's input) but goes through the existing `ExclusionClassifier` when it appears as a candidate, and is never accepted as private because of the input. An unavailable classification is not confirmation that the address is private (D24-006, D22-001).

## Consequences and risks
- Nothing in this task enforces the E3 and E5 rules; they depend on the owners (scoring #9 is not merged, the report is not implemented).
- A Case with only a wallet can yield Mentions but no honest Confidence Level for the person until E3 defines how.

## Verification
`CaseCreationSpec`: the wallet creates no Mention; an exchange address entered as the wallet is accepted as input but classified as `EXCHANGE` and not accepted by the real `ExclusionClassifier`; the wallet appears in the identifiers of a saved Analysis.

## Related
- Files: `service/CaseService.kt`, `analysis/AnalysisContracts.kt`. Issues: #9, #22, #50. Decisions: D50-001, D50-002, D24-006.

## Reconsider when
E3 defines scoring for input-derived addresses, or E5 designs the report.