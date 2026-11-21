# D23-007: Demo fixture `demo-v1-activity.json` and its frozen expected values

- **Status:** accepted (owner)
- **Date:** 2026-11-21
- **Type:** answer to E7-D2 ("which network, indicator and transaction references E4 delivers") for demo-v1
- **Source:** E7.1 (documented synthetic data, expected values before the demo, synthetic distinct from live), E7.2 step 6; D9-008 (demo-v1 Attributions and scores)
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., grilling 2026-11-21

## Problem and context
The demo needs repeatable Address Activity for the three demo-v1 Attributions, showing every kind of result without a live service.

## Options considered
1. Three states: ETH with a stablecoin swap and an exchange transfer (plus one swap below the threshold), BTC `1A1z` with activity and no signal, BTC `bc1q` (the P2P ad) with a P2P transfer.
2. Only ETH with signals.
3. Option 1 plus one address without data (the demo Analysis would become `PARTIAL` because of E4).

## Decision
Option 1. `backend/src/main/resources/fixtures/demo-v1-activity.json` (version `demo-v1`, `synthetic: true`) holds raw transfers. The same `AddressActivityEvaluator` that handles live data derives totals and signals from them, so the demo exercises the real rules. Expected values (algorithm `confidence-1`, threshold 1000):

| Attribution | Totals | Period | Cash-out signals (time order) |
|---|---|---|---|
| ETH `0x5aaeb605…beaed` | in 10.2 ETH, out 8.1 ETH, 5 transactions | 2026-03-02 – 2026-09-30 | 2026-08-11 `STABLECOIN_SWAP` 21480 USDC; 2026-08-12 `EXCHANGE` 20000 USDC to Kraken 4 (Etherscan label). The 2026-09-30 swap of 260 USDC is below the threshold. |
| BTC `1A1zP1…DivfNa` | in 0.75 BTC, out 0.25 BTC, 4 transactions | 2026-01-15 – 2026-07-07 | none (activity is not a cash-out) |
| BTC `bc1qw508…f3t4` | in 1.2 BTC, out 1.1 BTC, 3 transactions | 2026-09-01 – 2026-10-05 | 2026-10-05 `P2P` 1.1 BTC to "P2P marketplace escrow (synthetic)" |

- Transaction ids are SHA-256 of `olcys/demo-v1/<key>`: well-formed but fictional. They get no explorer link.
- Counterparties are public test vectors and well-known sample addresses (EIP-55, BIP-13/84/173 examples), the Uniswap V2 router and USDC/WETH pair (labelled `dex`, never an off-ramp), and Kraken 4 (labelled `kraken` in the bundled Etherscan dataset). Only the P2P counterparty carries a fixture label.
- The fixture applies only to these three addresses, only while `sources.synthetic.enabled` is true, and wins over the live explorers.

## Consequences and risks
- The demo Analysis keeps its status. Every demo Attribution has data, so E4 adds no limitation.
- The ETH row is visible only while `0x5aaeb605…` is an Attribution. On mainnet it now holds code, so with the bytecode check on it is an Excluded Address and the two ETH signals do not appear (D23-009, gap A1).
- A new fixture version needs a new table here and in `SyntheticActivitySourceSpec`.

## Verification
`SyntheticActivitySourceSpec` (every value above, with the bundled label lists); `AnalysisFlowSpec` (the report over HTTP); E2E "Address Activity: the demo addresses carry synthetic data with cash-out signals in time order". Seen in the running app on 2026-11-21 (profile `local`, `EXCLUSION_ONCHAIN_CHECK_ENABLED=false`): all three rows as above, the contract `0xde0b…` `UNAVAILABLE`.

## Related
- Files: `resources/fixtures/demo-v1-activity.json`, `analysis/synthetic/SyntheticActivitySource.kt`, `analysis/synthetic/SyntheticFixture.kt`. Decisions: D9-008, E7-D2.

## Reconsider when
A demo-v2 fixture is added (E7 owner), or the demo ETH address changes.
