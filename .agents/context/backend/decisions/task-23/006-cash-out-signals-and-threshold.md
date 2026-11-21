# D23-006: Cash-out signals: off-ramp transfer at any amount, stablecoin swap from 1000

- **Status:** accepted (owner); mentor question 5 ("what value counts as significant") still open
- **Date:** 2026-11-21
- **Type:** owner decision under an open mentor question
- **Source:** task section 3, required criterion "Fiat Cash-Out": interactions with P2P services, payment gateways, crypto-fiat card services, or exchange of significant values into stablecoins; cheat sheet KROK 1 item 3 (interaction with an exchange is recorded) and item 5 (CASP / off-ramp); EU Regulation 2023/1113 (1000 EUR threshold for crypto transfers, used only as an anchor)
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., grilling 2026-11-21. Not approved by mentors.

## Problem and context
Which on-chain events are "signals of withdrawal to traditional currencies", and what is "significant"?

## Options considered
1. An outgoing transfer to a labelled off-ramp counts at any amount; a swap into a USD stablecoin counts from 1000 units.
2. One minimum amount per asset for every signal (for example 0.01 BTC, 0.1 ETH, 1000 USD stablecoin).
3. No threshold at all.

## Decision
Option 1 (`AddressActivityEvaluator`). Signal kinds (`CashOutSignalKind`):

| Kind | Rule |
|---|---|
| `EXCHANGE` | outgoing transfer (coin or token) to an address labelled `EXCHANGE` in the offline lists (Etherscan dataset and own list, D22-002), at any amount; the counterparty is the signal |
| `P2P`, `PAYMENT_GATEWAY`, `CRYPTO_CARD` | outgoing transfer to a counterparty with that label: an exchange label whose dataset tags name the service (`remitano`, `fiat-gateway`, `wirex`, D23-010), or a fixture label |
| `STABLECOIN_SWAP` | in one transaction the address sends something other than a stablecoin and receives USDT, USDC or DAI of at least `app.address-activity.stablecoin-swap-threshold` (default 1000) units |

- A list label wins over a fixture label. Incoming transfers, transfers to unlisted addresses and transfers to mixers are not cash-out signals.
- Stablecoins are matched by contract address (USDT `0xdac17f95…`, USDC `0xa0b86991…`, DAI `0x6b175474…`), never by symbol, so a fake token named "USDC" proves nothing. A swap between stablecoins and a stablecoin received without selling are not swaps.
- 1000 units of a USD stablecoin are read as 1000 USD without any price service (nothing else is sent out). The value echoes the 1000 EUR threshold for crypto transfers in EU Regulation 2023/1113; it is configurable.
- A signal is a lead, not proof: it does not establish an actual fiat withdrawal or a tax violation (E5.1). The frontend line is "Możliwe wyjście do walut tradycyjnych."

## Consequences and risks
- A dust transfer to an exchange is a signal; the counterparty matters more than the amount.
- Exchanges give each customer a deposit address that is rarely on public lists, especially on BTC, so most real exchange deposits are not recognised (`LABELLED_COUNTERPARTIES_ONLY`, D23-004).
- Swaps of other tokens or into EUR stablecoins are not signals yet.

## Verification
`AddressActivityEvaluatorSpec`: exchange at any amount with provenance; incoming, unlisted and mixer transfers are not signals; fixture label versus list label; swap at 999.99 / 1000 / 21480; look-alike token, stable-to-stable swap and a received stablecoin are not swaps; time order; no stablecoins on BTC.

## Related
- Files: `service/AddressActivityEvaluator.kt`, `config/AddressActivityConfig.kt`, `service/ExclusionLabelDirectory.kt`. Decisions: D22-002, D23-004.

## Reconsider when
Mentors answer question 5, KAS supplies lists of off-ramps (P2P platforms, kantory, card issuers), or EUR stablecoins become relevant.
