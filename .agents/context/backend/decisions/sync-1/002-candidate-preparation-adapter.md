# DS1-002: Adapter from E2 Mentions to the E3 candidate input

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** own proposal; completes an accepted ADR
- **Source:** ADR 0002 ("E2 must emit `RawAddressCandidate`s and map `AddressValidation` into `DetectedAddressResponse`"); D24-003 (`CandidatePreparation` port); TODOs in `AddressCandidates.kt` and `AddressCandidateService.kt`
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet (E2 owner Maksym Z., E3 owner Maksym U.)

## Problem and context
The orchestrator calls `CandidatePreparation.prepare(List<FoundMention>) -> PreparedCandidates` (string network, string reason). E3 (#21) built `AddressCandidateService.prepare(List<RawAddressCandidate>) -> AddressCandidates` (enum `Network`, enum failure). Nothing joined them.

## Options considered
1. Make `AddressCandidateService` implement the port (two `prepare(List<...>)` overloads clash on the JVM).
2. A thin adapter `CandidatePreparationService` that maps types only.

## Decision (proposed)
Option 2. Every detected string of every Mention becomes `RawAddressCandidate(mentionId, rawAddress)` without a network hint (the hint comes from the same format the validator reads, so it can never disagree); the result maps to `Candidate(network.name, canonical address, mentionIds)` and `RejectedCandidate(mentionId, rawAddress, failure.name)`. Grouping, sorting and validation stay in `AddressCandidateService`. The resolved TODO comments were removed.

## Consequences and risks
- The stored `Candidate.network` stays a string (D25-002); the report maps it back with `Network.valueOf`.
- Rejected candidates are stored in the Analysis but are not part of `ReportResponse`; the Analyst sees them through the Mentions endpoint (`valid = false`, `validationFailure`), see DS1-005.

## Verification
`CandidatePreparationServiceSpec` (grouping across Mentions, canonical form, rejections with reason, order independence); `AnalysisFlowSpec` (end to end).

## Related
- Files: `service/CandidatePreparationService.kt`. Decisions: ADR 0002, D24-003, D25-002. Diagram: `06-candidate-preparation.puml`.

## Reconsider when
The owners change `Candidate.network` / `RejectedCandidate.reason` to enums.
