# D22-001: Classification order and the three outcomes

- **Status:** accepted
- **Date:** 2026-11-21
- **Type:** owner decision for issue #22
- **Source:** issue #22 criteria 1–3; brief KROK 3 (labels, then bytecode); `AnalysisContracts.kt` port `ExclusionClassifier` (D24-003); D24-006
- **Proposed by:** AI agent (Claude) · **Approved by:** Maksym U. (task owner), grilling 2026-11-21

## Problem and context
The port returns `ExclusionResult(accepted, excluded, unclassified)`. The orchestrator scores only `accepted + unclassified`, and flags `unclassified` as `ClassificationStatus.UNAVAILABLE` with the Analysis `PARTIAL` (D24-006). Every candidate must land in exactly one list, and an unavailable check must never look like a confirmed result.

## Decision
`ExclusionClassifierService` classifies each candidate in input order:

1. **Label lists** (offline, D22-002). A hit → `excluded` with the label's `ExclusionReason`. No external call is made.
2. **ETH only: bytecode** (D22-003). Sources are asked in order; the first usable answer decides: code → `excluded` as `SMART_CONTRACT`; no code, or an EIP-7702 delegation (D22-004) → `accepted`.
3. **No usable answer**, or the on-chain check is switched off (D22-005) → `unclassified`.

| Candidate | Result |
|---|---|
| On a label list | `excluded` (reason from the list) |
| ETH, not listed, code present | `excluded` `SMART_CONTRACT` |
| ETH, not listed, no code / delegated | `accepted` |
| ETH, not listed, all services unavailable or switched off | `unclassified` |
| BTC, not listed | `accepted` (the lists are always available, and BTC has no contract code) |
| Unknown network string | `unclassified` (logged WARN) |

## Consequences and risks
- An unchecked ETH address is still scored, but visibly flagged; it is never reported as "not excluded" (criterion 3).
- BTC protection is only as good as the own list (D22-002); the report (E5) should say so.
- The same input with the same external answers gives the same result; the input order is preserved.

## Verification
`ExclusionClassifierServiceSpec` (every row above); `ExcludedAddressPipelineSpec` (real classifier inside the orchestrator: Excluded Addresses never reach `ConfidenceScoring.score` and never come back as Attributions; the unchecked one is flagged `UNAVAILABLE`, Analysis `PARTIAL` with `CLASSIFICATION_UNAVAILABLE`).

## Reconsider when
A BTC label service is approved (then an unlisted BTC address may become `unclassified` while that service is down).
