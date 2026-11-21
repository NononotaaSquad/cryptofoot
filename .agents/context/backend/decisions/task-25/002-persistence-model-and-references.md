# D25-002: Persistence model and references

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** own proposal; temporary assumptions where other owners have not decided
- **Source:** issue #25 criterion 2; E6.1 ("references to its Mentions, results and exclusions"); E6 "Dependencies" (retention of captured content to be agreed with E2); E5-D3; D24-003, D24-004
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
The Analysis must keep identifiers used, source coverage, algorithm version and references to Mentions, Attributions and Excluded Addresses. On master there is no Mention store (E2), no Case store (E1) and no model for Attributions; the only agreed shape is the orchestration result of #24 (`AnalysisResult`).

## Options considered
1. New persistence model with separate collections for Mentions, Attributions, Exclusions.
2. Embed the #24 `AnalysisResult` (and `CaseIdentifier`) as is in the Analysis document.
3. Store only ids and wait for E2/E3 stores.

## Decision (proposed)
Option 2, to avoid a competing model. The record embeds: identifiers actually used (`CaseIdentifier` list), `networks`, and the `AnalysisResult` (sources with status/synthetic/checkedAt, Mentions with source, URL, collection time and context, rejected candidates, excluded candidates, unclassified candidates, Attributions with rules and `classificationStatus`, failures, limitations, algorithm version). Attributions and exclusions reference Mentions by Mention id (`mentionIds`). **Temporary assumption:** Mention content is snapshotted inside the Analysis until E2 decides retention (E5-D3), so a reopened Analysis does not depend on later source changes.
`caseId` is a plain reference; no Case is validated because no Case store exists.

## Consequences and risks
- Stored shape equals the #24 contract types; changing them changes stored data (no migrations).
- Mention content is duplicated across Analyses of one Case (size, 16 MB document limit is far away for the demo).
- `Candidate.network` and `RejectedCandidate.reason` are still strings in the #24 contract while the accepted ADR 0002 uses `Network` and `AddressValidationFailure`. The report (E5) must map them; the adapters feeding the orchestrator must emit `BTC`/`ETH`. To agree with the owners of #21/#24.
- `caseId` of a non-existent Case is accepted.

## Verification
`AnalysisHistorySpec` reads stored `result` (Attributions with Mention ids, failures, limitations).

## Related
- Files: `model/AnalysisRecord.kt`, `analysis/AnalysisContracts.kt`. Issues: #16, #21, #24, #25. Decisions: D24-003, D25-001, D25-005.

## Reconsider when
E2 provides retained Mention storage (replace embedded Mentions with references), E1 provides Cases, or the #24 contract changes.
