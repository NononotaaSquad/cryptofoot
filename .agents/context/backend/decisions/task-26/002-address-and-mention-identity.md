# D26-002: Address and Mention identity

- **Status:** proposed (dependency on the address-validation and Mention owners)
- **Date:** 2026-11-21
- **Type:** own proposal; requirement from E6.3 (match by network and canonical address)
- **Source:** issue #26 criteria 2 and 4; E6.3; `backend/decisions/0002-address-candidate-validation.md` (`CanonicalAddress`); issue #16 (stable Mention identity)
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
E6.3 matches Attributions by network and the canonical address "supplied by the address-validation domain", and wants unchanged copies of a Mention not counted as new support. Stored Analyses (#25) keep `Candidate(network: String, address: String, mentionIds)`. Mention ids are only known to be stable if E2 delivers that (#16 is open).

## Options considered
1. Re-validate/normalise addresses in the comparison.
2. Match on the exact stored (network, address) and rely on the producing module.
3. Match on address text only.

## Decision (proposed)
Option 2. Key = (`network`, `address`) exactly as stored. The comparison does no validation or normalisation of its own; the adapter that feeds the orchestrator must store the canonical form from `AddressCandidateService`/`CanonicalAddress` (BTC/ETH names). The same text on another network is a different address. An unknown network name in storage fails loudly (500), not silently. Mentions are never compared or counted: each change carries `earlierMentionIds` and `laterMentionIds` (both sides' evidence), and the category is decided from Confidence Level and exclusion state only, so an extra copy of a Mention cannot create a change or independent support.

## Consequences and risks
- If an adapter stores non-canonical addresses, the same address appears as NEW plus NOT_OBSERVED.
- Whether Mention ids stay stable across Analyses is unverified (E2).
- The response gives ids, not Mention content; content is read from each saved Analysis (D25-002).

## Verification
`AnalysisComparerSpec`: same text on BTC vs ETH; extra Mention copy with the same level gives no change; evidence ids of both sides.

## Related
- Files: `service/AnalysisComparer.kt`. Issues: #16, #21, #26. Decisions: D25-002, D26-001.

## Reconsider when
The address-validation owner exposes canonicalisation at storage time as a guarantee, or E2 defines Mention identity.
