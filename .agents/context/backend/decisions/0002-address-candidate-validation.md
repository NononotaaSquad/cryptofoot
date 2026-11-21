# 0002. Address candidate validation, canonical form and grouping (input to the Confidence Level algorithm)

**Status:** Accepted by the team · 2026-11-21. The input shape is **proposed** and still needs confirmation from the owner of "E3: Confidence Level algorithm". Issue #21.

## Context

E2 detects strings that look like cryptocurrency addresses in Mentions. E3 must not score an address "merely because it looks like one" (E7.1), must keep malformed candidates visibly out of Attributions, and must group all Mentions of the same address. E6.3 matches Attributions across Analyses by network + canonical address. ADR 0001 left `network` as a free string until the supported networks were decided.

## Decision

**Networks.** Only `Network { BTC, ETH }` (mainnet), the two networks the brief names (task section 3). `network` in every response DTO is now this enum instead of `String`.
- BTC: Base58Check P2PKH (version `0x00`) and P2SH (`0x05`), double SHA-256 checksum; segwit `bc1` with bech32 for witness v0 (20/32-byte program) and bech32m for v1+ (BIP-173/350).
- ETH: `0x` + 40 hex. All-lowercase or all-uppercase has no checksum and is accepted. Mixed case must pass EIP-55 (Keccak-256).
- BTC testnet (`tb1`, `bcrt1`, versions `0x6f`/`0xc4`) and every other format (TRON, LTC, …) → `UNSUPPORTED_NETWORK`. Any valid `0x` address is `ETH`; other EVM chains are out of scope.

**Canonical form** `CanonicalAddress(network, address)`: bech32 and ETH lowercased, Base58 kept as is (case-sensitive). Its equality is the key for grouping within an Analysis and for matching across Analyses (E6.3).

**Validation failures** `AddressValidationFailure { MALFORMED, CHECKSUM_MISMATCH, UNSUPPORTED_NETWORK, NETWORK_MISMATCH }`. The network is inferred from the format; when E2 passes a `networkHint` that contradicts it → `NETWORK_MISMATCH`. Visible to the Analyst through `DetectedAddressResponse.valid = false` + the new `validationFailure`.

**Input from E2:** `RawAddressCandidate(mentionId, rawAddress, networkHint?)`. This is our own minimal type, so E3 does not depend on E2's Mention document; E2 maps its detections into it.

**Output for the Confidence Level algorithm (proposal):**
```kotlin
AddressCandidates(
    candidates: List<AddressCandidate(canonicalAddress, mentionIds)>, // only valid addresses
    rejected: List<RejectedAddressCandidate(mentionId, rawAddress, failure)>,
)
```
- `AddressCandidateService.prepare(rawCandidates)` is pure and stateless; nothing is persisted here (persisting Attributions belongs to scoring/report).
- Deterministic: candidates are sorted by (network, address), and `mentionIds` are deduplicated and sorted, so input order never changes the result ("same Mentions → same Confidence Level").
- Rejected candidates are a separate list and type, so they cannot become Attributions.
- Whether a copied Mention counts as independent support (E6.3, E7.3) is the algorithm's decision, not this step's.

**Implementation.** Base58 and bech32 are hand-written in `util/AddressCodecs.kt`, tested against BIP-173/350 and EIP-55 vectors. Only `org.bouncycastle:bcprov-jdk18on` is added, for Keccak-256 (the JDK's SHA3-256 uses different padding). bitcoinj/web3j were rejected as heavy for three functions.

## Consequences

- Frontend types: `network` is the union `'BTC' | 'ETH'`. `DetectedAddressResponse` gains `validationFailure`.
- Adding a network means a new enum value, a validator branch, test vectors and an api-sync.
- Open: confirm the `AddressCandidates` shape with the Confidence Level owner. E2 must emit `RawAddressCandidate`s and map `AddressValidation` into `DetectedAddressResponse`.
