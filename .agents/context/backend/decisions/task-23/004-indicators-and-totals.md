# D23-004: Totals plus a chronological list of cash-out signals; unknown is never zero

- **Status:** accepted (owner)
- **Date:** 2026-11-21
- **Type:** owner decision (scope of mentor question 2)
- **Source:** issue #23 criteria 1–3; task section 1 (total volume, swaps, potential exit to fiat, exchange and P2P interactions), section 7 (chronological description of the flow of funds); E5.1 ("missing data is not displayed as zero activity"); E5.2 (chronological events with transaction references); main rule "little noise"
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., grilling 2026-11-21

## Problem and context
What does one Address Activity block contain, and how are partial explorer answers shown?

## Options considered
1. Totals plus signal events only.
2. Totals plus the latest N transactions with the signals marked.
3. Totals, signal events and the N largest plain transfers.

## Decision
Option 1.

- **Totals** ("how much money went through the address"): `totalReceived`, `totalSent` (in the network's coin, `unit` BTC or ETH), `transactionCount`, `firstActivityAt`, `lastActivityAt`.
- **Events:** only cash-out signals (D23-006), sorted by time, each with the transaction id, a link to the explorer when the data is live, the amount, its unit, and the counterparty with the label that made it a signal. Plain transfers are summed, not listed.
- **Unknown is never zero** (`AddressActivityEvaluator`): a total stated by the source is used as is (mempool `chain_stats`, Blockscout `transactions_count`). Otherwise it is summed from the transfers only when the source returned the whole history; with only the most recent page it stays `null`. The first activity is known only for a complete history; the last activity is the newest transfer read. A complete empty history is real zero activity.
- **Coverage limitations** per block (`coverageLimitations`):

| Code | When | Meaning for the Analyst |
|---|---|---|
| `LABELLED_COUNTERPARTIES_ONLY` | always | signals are found only for counterparties on the label lists; no signal does not mean no cash-out |
| `RECENT_TRANSACTIONS_ONLY` | the explorer returned only its most recent page | older signals may be missing; unknown totals stay empty |
| `INTERNAL_TRANSFERS_NOT_COUNTED` | ETH from Blockscout | coin totals count the address's own transactions, not internal transfers from contracts |

## Consequences and risks
- The Analyst sees no plain transfer list; the transaction count and the period give the scale.
- Live data reads one page (mempool: the most recent page of up to 50 transactions, confirmed ones used; Blockscout: 50 transactions and 50 token transfers). Paging is not implemented (D23-009).
- BTC totals come from mempool `funded_txo_sum` / `spent_txo_sum`; like every explorer they include change sent back to the address.

## Verification
`AddressActivityEvaluatorSpec` (complete history, empty history, partial page, stated totals, limitations); `AddressActivityLiveSpec` (mempool with 3 and 40 transactions, Blockscout).

## Related
- Files: `service/AddressActivityEvaluator.kt`, `service/AddressActivitySources.kt`. Decisions: D23-006, D23-008.

## Reconsider when
The Analyst asks for the transfer list, swaps between cryptocurrencies other than into stablecoins, or full history paging.
