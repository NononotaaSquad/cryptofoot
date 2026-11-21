# ADR-0005: Address Validation and Canonical Form

**Status:** Accepted

**Context:**
Address candidates detected in Mentions (E2) must not become Attributions merely because they look like an address (#21, E7). The same address can be written differently across Mentions and Analyses (letter case, surrounding whitespace), and E6.3 matches Attributions by network and canonical address. The brief names Bitcoin and Ethereum as the networks to cover.

**Decision:**
- Support two networks: `BITCOIN` and `ETHEREUM`. Anything else is rejected as `UNKNOWN_FORMAT`.
- A candidate is first classified by shape, then validated with the network's checksum:
  - Bitcoin Base58Check (`1…` P2PKH, `3…` P2SH): double SHA-256 checksum, 21-byte payload, version byte `0x00` or `0x05`.
  - Bitcoin SegWit (`bc1…`): BIP-173 Bech32 for witness version 0 (program 20 or 32 bytes), BIP-350 Bech32m for versions 1–16 (program 2–40 bytes); mixed case is invalid.
  - Ethereum (`0x` + 40 hex): if the hex mixes upper and lower case, it must match the EIP-55 checksum; all-lowercase or all-uppercase has no checksum to check and is accepted.
- A candidate whose shape matches a network but fails validation is `INVALID_ADDRESS` with that network.
- Canonical form is `CanonicalAddress(network, address)`: Bech32 and Ethereum addresses lowercased, Base58 kept as written (case-sensitive), surrounding whitespace trimmed.
- Codecs (Base58Check, Bech32/Bech32m, Keccak-256) live in `util/AddressCodecs.kt` instead of a library dependency. The JDK has no Keccak-256 (its SHA3-256 pads differently), and the only cached BouncyCastle version (1.78.1) has a known CVE; one hash did not justify a crypto library.

**Consequences:**
- **Positive:** No new dependency; validation is checksum-based and deterministic; canonical form gives E6.3 a stable match key.
- **Negative:** Hand-written crypto code to maintain. It is covered by the official BIP-173, BIP-350 and EIP-55 test vectors in `AddressCandidateServiceSpec`. Other EVM chains share Ethereum's address format and are currently classified as `ETHEREUM`.
- **Follow-up:** Adding a network means adding a `Network` value, a shape and a validator in `AddressCandidateService`.
