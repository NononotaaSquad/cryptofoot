# D23-013: A slow transaction list keeps the totals instead of making the activity unavailable

- **Status:** accepted
- **Date:** 2026-11-22
- **Type:** bug fix found on production
- **Source:** bug report of 2026-11-22: a Case with only a nickname got an Attribution but showed "Nie udało się pobrać danych o aktywności"; gap D23-009 A8
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., 2026-11-22 (bug report)

## Problem and context
Every explorer answer had to be complete: mempool.space needed both `/api/address/{a}` and `/api/address/{a}/txs`, and Blockscout needed counters, transactions and token transfers. On production the list calls often exceeded the 5 s timeout, while the first call answered in under a second:
- `1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2`: `/txs` is a 1 MB body, 1.2–5.5 s.
- `bc1qfa666…c44`: `/txs` took 14.3 s for an empty list.
- Blockscout token transfers took 20–30 s for a busy contract (A8).

The Cloud Run logs showed `HttpTimeoutException: request timed out`. The whole Address Activity then became `UNAVAILABLE` and the Analysis `PARTIAL`, although the totals and the transaction count had already arrived. The same address worked minutes later, only because the explorer had cached the list. So the problem was the timeout, not whether the Case was started from a nickname or a wallet.

## Options considered
1. Keep whatever answered: the totals and count are enough for `AVAILABLE`; a missing list is a coverage limitation.
2. Raise the timeout (for example to 30 s): slower Analyses, and the 14 s and 30 s cases still fail sometimes.
3. Keep all-or-nothing.

## Decision
Option 1.
- **mempool.space:** `/api/address/{a}` is required (exact totals, transaction count). If `/txs` does not answer, the activity keeps the totals and count, has no transfers and no signals, and states `TRANSACTIONS_UNAVAILABLE`.
- **Blockscout:** counters are required. Transactions and token transfers are fetched independently. If either does not answer, the ones that did are kept and `TRANSACTIONS_UNAVAILABLE` is stated. Coin totals stay unknown unless the whole history was read.
- `TRANSACTIONS_UNAVAILABLE` replaces `RECENT_TRANSACTIONS_ONLY` when the list could not be read at all. The frontend sentence is "Nie udało się odczytać listy transakcji, więc nie sprawdzono sygnałów wyjścia do walut tradycyjnych."
- Status `AVAILABLE`: such an activity no longer makes the Analysis `PARTIAL`. The limitation is shown in the activity block.
- The rules version stays `activity-1`, because the signal rules do not change; only what a source returns does.

## Consequences and risks
- For busy addresses the Analyst sees how much went through the address even when the signals could not be checked. "No signal" with `TRANSACTIONS_UNAVAILABLE` does not mean "no cash-out".
- A failure of the first call (`/api/address` or Blockscout counters) still gives `UNAVAILABLE`, never zero.

## Verification
`AddressActivityLiveSpec`: a `/txs` answer slower than the timeout keeps 236.43446701 BTC and 5448 transactions with `TRANSACTIONS_UNAVAILABLE`; Blockscout token transfers slower than the timeout keep the count and the coin transfers. `AddressActivityEvaluatorSpec`: the limitation replaces "recent page only". `address-activity-figures.test.ts`: the Polish sentence.

## Related
- Files: `service/AddressActivitySources.kt`, `service/AddressActivityEvaluator.kt`, `model/Enums.kt`, `features/analysis-report/report-labels.ts`. Decisions: D23-004, D23-005, D23-009 (A8), D23-011.
