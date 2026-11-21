# D23-009: Gaps found while implementing E4, left to their owners

- **Status:** open
- **Date:** 2026-11-21
- **Type:** findings; no decision taken for the owners
- **Proposed by:** AI agent (Claude)

| # | Gap | Effect | Owner / where to decide |
|---|---|---|---|
| A1 | The demo-v1 ETH address `0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed` (an EIP-55 test vector) holds executable code on mainnet as of 2026-11-21: the default bytecode check (`ethereum-rpc.publicnode.com`) excludes it as `SMART_CONTRACT`. | In the default demo the ETH Attribution and its two cash-out signals (stablecoin swap, exchange) are not shown; only BTC activity and the P2P signal are. With `EXCLUSION_ONCHAIN_CHECK_ENABLED=false` they appear, but the contract `0xde0b…` then becomes an unclassified Attribution with unavailable activity. A fixture demo-v2 with an ETH address without code would fix it. | E7 fixture owner, E3 (#22), see also #62 |
| A2 | Live data recognises only `EXCHANGE` counterparties (offline lists). P2P platforms, payment gateways and crypto card issuers have no curated address list; only the synthetic fixture names them. | Live P2P/gateway/card signals are never produced (`LABELLED_COUNTERPARTIES_ONLY`). | E4 owner, KAS lists |
| A3 | Live explorers read one page (mempool 25 confirmed, Blockscout 50 + 50). | Older signals are missed on busy addresses (`RECENT_TRANSACTIONS_ONLY`). | E4 owner |
| A4 | ETH coin totals ignore internal transfers from contracts. | Totals may be lower than the real volume (`INTERNAL_TRANSFERS_NOT_COUNTED`). | E4 owner |
| A5 | E6.3 comparison does not compare Address Activity (new signals between two Analyses). | A new cash-out signal is not listed as a change. | E6 owner (#26) |
| A6 | The frontend report (#64) planned `events` with `CASH_OUT_SIGNAL` and derived figures (seam S2); the contract changed (D23-008). | **Closed in the merge of #64 into #65:** `toAddressActivityFigures(status, activity)` maps the new contract, the block shows the source, transaction references, counterparties and one sentence per coverage limitation. | Frontend report owner (review) |
