# D23-009: Gaps found while implementing E4, left to their owners

- **Status:** open
- **Date:** 2026-11-21
- **Type:** findings; no decision taken for the owners
- **Proposed by:** AI agent (Claude)

| # | Gap | Effect | Owner / where to decide |
|---|---|---|---|
| A1 | The demo-v1 ETH address `0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed` (an EIP-55 test vector) is a contract on mainnet (old Solidity bytecode `0x6060…`, not an EIP-7702 delegation): the default bytecode check (`ethereum-rpc.publicnode.com`) excludes it as `SMART_CONTRACT`. | In the default demo the ETH Attribution and its two cash-out signals (stablecoin swap, exchange) are not shown; only BTC activity and the P2P signal are. With `EXCLUSION_ONCHAIN_CHECK_ENABLED=false` they appear, but the contract `0xde0b…` then becomes an unclassified Attribution with unavailable activity. **Proposed replacement** for a fixture demo-v2: see below. | E7 fixture owner, E3 (#22), see also #62 |
| A2 | **Partly closed by D23-010:** live data now names P2P (`remitano`), crypto card (`wirex`) and payment gateway (`fiat-gateway`) counterparties from the dataset tags. There is still no curated list of P2P escrow addresses, kantory or BTC services. | Most real P2P and BTC off-ramps are still not recognised (`LABELLED_COUNTERPARTIES_ONLY`). | E4 owner, KAS lists |
| A3 | Live explorers read one page (mempool up to 50 transactions, Blockscout 50 + 50). | Older signals are missed on busy addresses (`RECENT_TRANSACTIONS_ONLY`). | E4 owner |
| A4 | ETH coin totals ignore internal transfers from contracts. | Totals may be lower than the real volume (`INTERNAL_TRANSFERS_NOT_COUNTED`). | E4 owner |
| A5 | E6.3 comparison does not compare Address Activity (new signals between two Analyses). | A new cash-out signal is not listed as a change. | E6 owner (#26) |
| A6 | The frontend report (#64) planned `events` with `CASH_OUT_SIGNAL` and derived figures (seam S2); the contract changed (D23-008). | **Closed in the merge of #64 into #65:** `toAddressActivityFigures(status, activity)` maps the new contract, the block shows the source, transaction references, counterparties and one sentence per coverage limitation. | Frontend report owner (review) |
| A8 | Real-source check of 2026-11-22 (`ADDRESS_ACTIVITY_LIVE_ENABLED=true`): the response fields of mempool.space and Blockscout match the parsers, but for a very busy address (the contract `0xde0b…`, 8.4 M token transfers) Blockscout answers `token-transfers` in 20–30 s, beyond the 5 s timeout. | The whole address is `UNAVAILABLE` (honest, never zero), although counters and transactions answered in under 1 s. Options: keep the coin data with a limitation "token transfers not read", or raise `app.address-activity.timeout`. | E4 owner |
| A7 | The rules version `activity-1` and the threshold are stored (D23-011), but the raw transfers are not. | A result can be explained, not recomputed from the snapshot alone. | E4 owner |

## Proposed demo ETH address (A1)

`0xE73A66581205C9585215196C89c5648dea1420c6` (canonical `0xe73a66581205c9585215196c89c5648dea1420c6`).

- Derived as the last 20 bytes of keccak-256 of the public seed `olcys/demo-v2/eth/zenek_fx`. It is not derived from a public key, so nobody holds a private key for it: it can never send a transaction, deploy code or be anyone's wallet. A fictional person can be linked to it without pointing at a real one.
- Checked on 2026-11-21 through `ethereum-rpc.publicnode.com` (the default bytecode check): `eth_getCode` = `0x`, balance 0, nonce 0. It is on neither label list. The EIP-55 checksum was computed with a keccak-256 implementation verified against the vectors `0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed` and `0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359`.
- What a demo-v2 would change (not done here, E7 fixture owner): the two Mentions in `fixtures/demo-v1.json` that publish the ETH address, the key of the ETH entry in `fixtures/demo-v1-activity.json`, the D9-008 score table (95/75 expected unchanged: same Mentions), and the specs and E2E requests that use `0x5aAeb…` (also the wallet-as-input case of D50). The E2E "excluded or scored" branch for this address would then always be "scored".
- Risk: anyone can still send funds to it. With live explorers on, real history could appear, but the fixture wins for demo addresses (D23-003).
