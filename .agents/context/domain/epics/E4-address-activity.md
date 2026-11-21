# E4. Address Activity

**Status:** implemented for task #23 with owner decisions of 2026-11-21 ([D23-001 to D23-009](../../backend/decisions/task-23/README.md)); the mentor questions below are still open and may change them. · **Module:** M3b. Scoring: what the address did · [BUSINESS-SPEC.md](../BUSINESS-SPEC.md)

## Goal (draft)

Show whether real money stands behind the address from an Attribution and whether it was withdrawn to traditional currencies.

## User stories (draft, not accepted)

1. As an Analyst, I want to see how much money went through the address from an Attribution, so that I can judge the scale of the case.
2. As an Analyst, I want to see signals of withdrawal to traditional currencies in time order, so that I know whether and when the money left cryptocurrencies.

## Owner decisions (2026-11-21, not approved by the mentors)

- Scope and placement (D23-001, D23-004): a stage of every Analysis, stored with it; totals (received, sent, transactions, period) and cash-out signals in time order with transaction references, network, timestamps, amounts and units.
- Which Attributions (D23-002): every Attribution with a Confidence Level above 0.
- Significant value (D23-006): a transfer to an exchange, P2P platform, payment gateway or crypto card service at any amount; a swap into a USD stablecoin from 1000.
- Sources (D23-003): synthetic chain data for the demo; mempool.space and Blockscout (keyless, self-hostable) only when switched on.
- Missing data (D23-002, D23-005): shown as unavailable with a limitation, never as zero activity; the Analysis becomes partial.

## Questions for the mentors

- Should we show Address Activity, and in what scope?
- For which Attributions: all, above a Confidence Level threshold, or on request?
- What value counts as "significant" for withdrawal signals?

## Risk

Analysis of blockchain indicators is one of the 7 scored steps of the demo (Z p.5) and an acceptance test (Z p.3).
