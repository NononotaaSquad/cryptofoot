# D22-006: Which dataset tags exclude, and which never do

- **Status:** accepted (fundraiser scope chosen by the owner; the exchange tag list is the agent's review of the dataset)
- **Date:** 2026-11-21
- **Type:** owner decision + own review
- **Source:** glossary "Excluded Address" (exchange, smart contract, mixer, fundraiser); brief KROK 1.3 (CEX / custodial), KROK 1.5 (CASP / off-ramp), KROK 2 "0%" row (exchange, charity collection, mixer, smart contract)
- **Proposed by:** AI agent (Claude) · **Approved by:** Maksym U. for the fundraiser scope, grilling 2026-11-21

## Problem and context
The dataset has about 600 tags and no generic "exchange" tag; exchanges are tagged by name. Several tags mark **private people**, and excluding them would hide real leads: `ofac-sanctions-lists` (sanctioned individuals as well as Tornado Cash), `gitcoin-grants` (often a developer's own address), `maker-vault-owner`, `buidlguidl-builders`, `sybil-delegate`, `airdrop-hunter`. The `charity` tag is mostly charity *tokens*.

## Decision
Only explicit tags exclude (`EtherscanTagMapping`), with the precedence MIXER > EXCHANGE > FUNDRAISER:
- **MIXER:** `tornado-cash`, `typhoon-cash`, `ethereum-mixer`.
- **EXCHANGE:** named centralised exchanges (`binance`, `kraken`, `coinbase`, `okx`, `bitfinex`, `huobi`, `kucoin`, `upbit`, `bittrex`, …, full list in the code), exchange wallets (`hot-wallet`, `cold-wallet`), and custodial services, fiat gateways and OTC desks (`fiat-gateway`, `otc`, `nexo`, `celsius-network`, `blockfi`, `changenow`, `remitano`, `wirex`).
- **FUNDRAISER:** `donate` and `crowdfunding` only (owner choice). Gitcoin grants are **not** excluded. Token sales and charity tokens are contracts, so the bytecode check covers them.
- **SMART_CONTRACT** comes from no tag: DEX routers, pools and tokens are found by bytecode (D22-003), so this reason always rests on the chain itself.
- Every other tag, including all the person-like tags above, never excludes.

## Consequences and risks
- Exchange deposit addresses recorded only under the depositor's name (e.g. "Jump Trading: Coinbase Deposit", tag `fund`) are missed. They are EOAs, so bytecode does not catch them either.
- A sanctioned individual's own address stays a lead, which is intended.

## Verification
`ExclusionLabelDirectorySpec`: rows of the tag table; person tags → no exclusion; precedence; bundled Gitcoin, OFAC-only and DEX router addresses are not listed.
