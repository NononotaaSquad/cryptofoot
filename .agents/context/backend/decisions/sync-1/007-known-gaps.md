# DS1-007: Gaps found while connecting the tasks, left to their owners

- **Status:** open
- **Date:** 2026-11-21
- **Type:** findings; no decision taken for the owners
- **Proposed by:** AI agent (Claude)

Each row was observed on the wired flow with the demo-v1 fixture. None is changed in this sync because it belongs to another owner or needs a team decision.

| # | Gap | Effect | Owner / where to decide |
|---|---|---|---|
| G1 | The demo-v1 "malformed" address `1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfN0` contains `0` (not Base58) and lies beyond the 200-character snippet. It is never detected, so no rejected candidate appears. | E7.3 "malformed address candidate: validation failure visible" cannot be shown with demo-v1. A candidate with a valid alphabet and a bad checksum near the nickname would show it (e.g. `...DivfNb`). | E2 T4 / E7 fixture owner (new fixture version) |
| G2 | The fixture calls `0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae` an exchange hot wallet, but the bundled Etherscan dataset labels it `EthDev [ethereum-foundation]`, a tag not mapped to any reason. Only the bytecode check can exclude it (as `SMART_CONTRACT`, if it holds code on mainnet; not verified live in this sync). | With `EXCLUSION_ONCHAIN_CHECK_ENABLED=false` it becomes an unclassified Attribution flagged `UNAVAILABLE` and the demo shows no exclusion. A labelled exchange address (e.g. Kraken `0x267be1c1d684f78cb4f6a176c4911b741e4ffdc0`) would be excluded offline. | E7 fixture owner, E3 (#22) |
| G3 | The wallet entered by the Analyst is scored like any found address (D50-006 not enforced): a wallet-only Case can yield an Attribution of its own input. | Input may look like evidence. | **Closed for E3 by D9-004** (`ENTERED_BY_ANALYST`, wallet-only Mentions give no support); E5 still decides how to show it |
| G4 | The frontend decision of 2026-11-21 (comment on #9, V13/V14) wants a Justification text per Attribution; the contract has only Mentions and rules. | Contract change pending. | **Closed by D9-006** (`justification: String`, `mentions`; `api-sync` done) |
| G5 | Frontend open questions V9 (Case number `2026/0006`), V10 (highest Confidence Level and Attribution count per Case in the list), V11 (Reviewed state) are not in the contract. | Case list columns cannot be filled from the API. | Case owner, Analysis owner |
| G6 | A crash between `begin` and `finish` leaves `RUNNING` forever and blocks new starts and terms (D25-004). | Needs a timeout policy. | #25 / E6.4 |
| G7 | E4 Address Activity is on hold: `addressActivity` is always null, `BLOCKCHAIN_DATA_UNAVAILABLE` unused. | Required demo stage missing (BUSINESS-SPEC risks). | **Closed by task #23** ([D23-001 to D23-009](../task-23/README.md)); mentor questions 2, 4, 5 still open |
| G8 | The demo Case reset (#46) is still not implemented; the live scheduler check of `demo-scheduler-check.md` is now possible because the flow is wired. | Manual reset only. | #46 (Bohdan) |
