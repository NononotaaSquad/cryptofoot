# D23-010: Off-ramp kinds of live cash-out signals come from the dataset tags

- **Status:** accepted
- **Date:** 2026-11-21
- **Type:** owner decision after the requirements review of PR #65
- **Source:** task section 3, required criterion "Fiat Cash-Out": interactions with P2P services, payment gateways and crypto-fiat card services; gap D23-009 A2
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., 2026-11-21 ("do 2 and 3")

## Problem and context
D23-006 named the kinds `P2P`, `PAYMENT_GATEWAY` and `CRYPTO_CARD`, but live data produced only `EXCHANGE`. The bundled Etherscan dataset does tag such services: `fiat-gateway` (28 addresses, e.g. Bity.com), `remitano` (a P2P platform, 3) and `wirex` (a crypto card issuer, 2). They were all in `EtherscanTagMapping.exchangeTags`, so each became an Excluded Address with reason `EXCHANGE`, and a transfer to one was a signal of kind `EXCHANGE`. The tags were kept only inside the label name text.

## Options considered
1. Keep the raw tags on `ExclusionLabel` and map them to a kind in `EtherscanTagMapping`.
2. Parse the tags back out of the label name.
3. Add new label lists for P2P platforms, gateways and card issuers.

## Decision
Option 1.
- `ExclusionLabel` gets `tags` (sorted dataset tags; empty for an own-list entry).
- `EtherscanTagMapping.offRampKindFor(tags)` uses a fixed precedence: `remitano` gives `P2P`, `wirex` gives `CRYPTO_CARD`, `fiat-gateway` gives `PAYMENT_GATEWAY`, and anything else stays `EXCHANGE`.
- `AddressActivityEvaluator` uses it for every counterparty labelled `EXCHANGE`. Exclusion (E3) does not change: these addresses stay Excluded Addresses with reason `EXCHANGE`.
- The mapping is part of the Address Activity rules `activity-1` (D23-011).

## Consequences and risks
- An exchange wallet that Etherscan also tags `fiat-gateway` (e.g. "Bitfinex 1 [bitfinex, fiat-gateway, otc]") becomes `PAYMENT_GATEWAY`. That is still an off-ramp, and the label text shows the full tag list.
- Coverage is what the 2023 dataset tags: BTC has no such tags, and no list names P2P escrow addresses (A2 stays partly open).
- Some tagged entries are token contracts or deployers (e.g. "Wirex: Deployer"); a transfer to one is rare and still points to the service.

## Verification
`ExclusionLabelDirectorySpec` ("an exchange tag names the off-ramp kind …", "the off-ramp tags are exchange tags …", the bundled Remitano 2, Wirex: Deployer, Bity.com and Kraken 4 entries); `AddressActivityEvaluatorSpec` ("the dataset tags of a listed exchange name the off-ramp kind …").

## Related
- Files: `model/ExclusionLabels.kt`, `service/ExclusionLabelDirectory.kt` (`EtherscanTagMapping`), `service/AddressActivityEvaluator.kt`. Decisions: D22-002, D22-006, D23-006, D23-011.

## Reconsider when
KAS supplies lists of P2P platforms, kantory or card issuers (an own off-ramp list with a kind per entry), or the dataset is refreshed.
