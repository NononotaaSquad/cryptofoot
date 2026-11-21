# D25-005: Network coverage recorded as the supported networks

- **Status:** proposed (temporary assumption)
- **Date:** 2026-11-21
- **Type:** temporary assumption
- **Source:** issue #25 criterion 2 ("source/network coverage"); E6.1; E5.1; ADR 0002 (`Network { BTC, ETH }`)
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
E6.1 requires the networks covered by an Analysis. No module reports the networks it actually checked; address validation supports `BTC` and `ETH` (ADR 0002) and blockchain data (E4) is on hold.

## Options considered
1. Store nothing until modules report coverage.
2. Store `Network.entries` at the start of the Analysis.
3. Add a networks field to the #24 search/scoring contracts.

## Decision (proposed)
Option 2 as a temporary measure: `networks` = all `Network` values at the moment the Analysis starts, exposed as `AnalysisResponse.networks`. It states which networks the application could validate then, not which blockchains were queried.

## Consequences and risks
- It can overstate coverage (no blockchain was queried). E5/E6.3 must not present it as blockchain-data coverage.
- Adding a network later changes only new Analyses; old records keep their list (comparability limitation for E6.3).

## Verification
`AnalysisHistorySpec`: `networks` is `[BTC, ETH]` for a started Analysis.

## Related
- Files: `service/AnalysisRecorderService.kt`, `model/AnalysisRecord.kt`. Issues: #25, #26/#27 (comparison). Decisions: D25-002, D25-003.

## Reconsider when
Modules report actually checked networks, or E4 delivers blockchain data coverage.
