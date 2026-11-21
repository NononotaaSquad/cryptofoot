# D23-008: Report contract for Address Activity

- **Status:** accepted (contract change needs frontend awareness)
- **Date:** 2026-11-21
- **Type:** consequence of D23-001 to D23-006
- **Source:** ADR 0001 (placeholder `addressActivity`, "E4 on hold"); frontend spec on `feature/FRONTEND-analysis-report` (seam S2 `toAddressActivityFigures`, user stories 44–47)
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U. (scope "backend + contract"), grilling 2026-11-21

## Problem and context
The placeholder DTOs (`AddressActivityEventResponse` with `RECEIVED/SENT/SWAP/CASH_OUT_SIGNAL`, a free-text `coverageLimitation`) did not fit the decisions: only signals are listed, missing data has to be told apart from data that was not checked, and provenance is required.

## Decision
`GET /api/1/analyses/{analysisId}/report`, per Attribution:

```
addressActivityStatus: AVAILABLE | UNAVAILABLE | NOT_CHECKED
addressActivity: AddressActivityResponse | null      // non-null only when AVAILABLE
  sourceName, synthetic, checkedAt, rulesVersion?, stablecoinSwapThreshold?, unit   // "activity-1"; null only before versioning (D23-011)
  totalReceived?, totalSent?, transactionCount?, firstActivityAt?, lastActivityAt?   // null = unknown, never 0
  cashOutSignals: [CashOutSignalResponse]            // time order
    kind: EXCHANGE | P2P | PAYMENT_GATEWAY | CRYPTO_CARD | STABLECOIN_SWAP
    transactionId, transactionUrl?, occurredAt, amount, unit, counterpartyAddress?, counterpartyLabel?
  coverageLimitations: [RECENT_TRANSACTIONS_ONLY | LABELLED_COUNTERPARTIES_ONLY | INTERNAL_TRANSFERS_NOT_COUNTED]
```

- Removed: `AddressActivityEventResponse`, enum `AddressActivityEventType`, `AddressActivityResponse.events` and `coverageLimitation: String`.
- `ReportLimitation.BLOCKCHAIN_DATA_UNAVAILABLE` is now produced (D23-005).
- Limitation and signal codes are mapped to Polish sentences in the frontend, like `ReportLimitation`. `counterpartyLabel` is provenance text ("<list>: <name>"), like `classificationSource`.
- `./scripts/api-sync.sh` was run; `frontend/apps/web/lib/api/` is regenerated.

## Consequences and risks
- The frontend report (seam S2, merged in #64) was adapted in the merge: it maps `cashOutSignals` instead of `events`, takes the transaction count and the period from the backend, and reads `addressActivity == null` together with `addressActivityStatus` (`UNAVAILABLE` shows "Nie udało się pobrać danych o aktywności tego adresu. Brak danych nie oznacza braku aktywności.", `NOT_CHECKED` says the activity was not checked).
- Amounts are JSON numbers (`BigDecimal`), exact on the backend; 18-decimal ETH values lose precision only in JavaScript display.

## Verification
`AnalysisReportSpec` "stored Address Activity is reported with its totals, cash-out signals, provenance and limitations …" (exact JSON, MongoDB round trip); `OpenApiContractSpec`; `npm run check`.

## Related
- Files: `dto/Responses.kt`, `model/Enums.kt`, `service/AnalysisReportService.kt`, `frontend/apps/web/lib/api/`. Decisions: ADR 0001, D23-002, D23-004.

## Reconsider when
The frontend needs the swapped amount (what was sold for the stablecoin) or the full transfer list.
