# DS1-004: Canonical wallet match, full name not a relevance key, bech32 detected in any case

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** fix of integration gaps between E1 (#5, #50), E2 T2/T3 and ADR 0002
- **Source:** D50-002 (known gap: "until E2 matches by canonical form, searching by an ETH wallet finds nothing"); E1.1 and the Case invariant ("a full name is additional data and never suffices", "so that I do not get results about random people"); ADR 0002 (bech32 lowercased; BIP-173 allows all-uppercase)
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet (E2 owner Maksym Z.)

## Problem and context
1. The Case stores the canonical wallet (ETH lowercased), but `SearchService` matched address identifiers by exact text, so an ETH wallet found nothing in sources that use the EIP-55 spelling (probe in D50-002).
2. `SearchService` treated the full name like any identifier: a Mention that only named "Zenon Fikcyjny" was kept, which is exactly the noise the narrowing-identifier rule is meant to prevent.
3. The detector matched `bc1` only in lower case, while the validator accepts and canonicalises all-uppercase bech32. An uppercase address in a source was never seen.

## Decision (proposed)
1. An address-like identifier matches a detected address when both validate to the same `CanonicalAddress`; if either is invalid, the old exact-text rule applies (an invalid value never matches a different address). `SearchService` gets `AddressValidator`.
2. `FULL_NAME` is skipped in the relevance filter. It stays among the identifiers used by the Analysis, available to scoring (#9) as supporting data.
3. The bech32 alternative of the detector is case-insensitive. Detection still does not validate: a mixed-case candidate is detected and then rejected by the validator as `MALFORMED`.

## Consequences and risks
- A Case with only a full name cannot exist (E1), so no Analysis loses all its results; a Case whose Mentions mention only the full name now has none, by intent.
- Mention identity is unchanged; earlier stored Mentions are reused.

## Verification
`SearchServiceSpec` (canonical wallet in EIP-55 and uppercase bech32, invalid address-like identifier, full name alone vs with a nickname); `AddressCandidateDetectorSpec` (uppercase and mixed-case bech32); `AnalysisFlowSpec` (wallet-only Case finds the two demo Mentions, full name alone finds nothing).

## Related
- Files: `analysis/SearchService.kt`, `analysis/AddressCandidateDetector.kt`. Decisions: D50-002, D50-006, ADR 0002. Diagram: `05-mention-search.puml`.

## Reconsider when
The E2 owner defines a different relevance policy (e.g. full name plus another weak identifier).
