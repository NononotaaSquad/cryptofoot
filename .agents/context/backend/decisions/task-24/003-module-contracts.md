# D24-003: Contracts the orchestration calls (search, candidates, exclusions, scoring)

- **Status:** proposed (not agreed with the owners)
- **Date:** 2026-11-21
- **Type:** own proposal; temporary assumption until the owners answer
- **Source:** issue #24 criterion 1 ("First: agree ... the interfaces"); #16, #21, #22, #9
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody. Needed: Maksym Z. (search, #16), Maksym U. (candidates #21, exclusions #22), Marcin (scoring #9)

## Problem and context
The orchestration must call four modules whose owners have not published a contract. Search of agreements: issues #9, #16, #21, #22, #24, #25 have no comments, only assignments. The only related artefact is ADR-0006 (status "Proposed") on `origin/feature/E3-address-candidates` (`AddressCandidateService.prepare(List<MentionAddresses>) -> AddressCandidates(valid, rejected)`), also not agreed. `master` has no Mention, Candidate or scoring code.

## Options considered
1. Wait for all owners.
2. Reuse the types from the unmerged E3 branch (would depend on, and likely conflict with, unmerged work).
3. Define the orchestration's own ports in package `analysis` with neutral types, let each owner adapt, and keep the orchestrator unaware of module internals.

## Decision (proposed)
Option 3. Ports in `AnalysisContracts.kt`:

| Port | Owner | Signature |
|---|---|---|
| `MentionSearch` | #16 | `search(identifiers) -> SearchResult(mentions: id, sourceName, url, collectedAt, context, detected addresses; sources: sourceName, SourceStatus CHECKED/UNAVAILABLE, synthetic, checkedAt)`; an exception means the whole module failed |
| `CandidatePreparation` | #21 | `prepare(mentions) -> PreparedCandidates(valid: network + address + mentionIds, rejected: with reason)` |
| `ExclusionClassifier` | #22 | `classify(candidates) -> ExclusionResult(accepted, excluded: ExclusionReason + classification source, unclassified)` |
| `ConfidenceScoring` | #9 | `score(candidates) -> ScoringResult(attributions: confidence level 0-100 + applied rules with contributions, algorithmVersion)`; the orchestrator adds `ClassificationStatus` (see D24-006) |

The orchestrator only passes data between ports and derives the status. It does not read Mention content, validate addresses, classify or compute scores. The shape of `appliedRules` follows the #9 acceptance criteria; it is unresolved whether it conflicts with the glossary definition of Justification (see Open points).

## Consequences and risks
- Adapters are needed on both sides once the owners agree; nothing in production implements these ports yet.
- Differences in owner contracts (e.g. `Network` enum vs `String`, `MentionAddresses` naming) are absorbed by adapters.
- Open points for the owners: source-level availability from search; unavailable classification as a separate result; `appliedRules` vs Justification; whether Mention content is needed by scoring (independent-source bonus).

## Alignment with the REST contract (`backend/decisions/0001`, merged to master)

Field names and enums (`SourceStatus`, `ExclusionReason`, `ClassificationStatus`, `ReportLimitation`, scoring rule name + contribution) follow `dto/Responses.kt` and `model/Enums.kt` so #25 can map the result to `AnalysisResponse`, `MentionResponse` and `ReportResponse` without translation. Known mismatches to agree: `AnalysisResponse.algorithmVersion` is non-null but is null here when no scoring ran; `mentionCount` must be derived by #25 from Mentions; `DetectedAddressResponse.valid` must be derived by #25 from candidate preparation output (valid/rejected); `justification` as full `MentionResponse` is built by #25 from stored Mentions.

## Verification
Specs use mocks of the ports; they prove the orchestration logic only, not integration.

## Related
- Files: `AnalysisContracts.kt`. Issues: #9, #16, #21, #22, #24, #25. Decisions: D24-002, D24-004.

## Reconsider when
Any owner publishes or agrees a different contract. Then mark this decision superseded and link the new one.
