# D9-008: Algorithm version `confidence-1` and frozen demo-v1 scores

- **Status:** accepted
- **Date:** 2026-11-21
- **Type:** answer to E7-D3 for the current fixture
- **Source:** issue #9 open decision "freeze the algorithm version and exact expected scores for the demo (E7-D3)"; E7.1 ("exact expected scores under a named algorithm version")
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., grilling session 2026-11-21 (keep demo-v1)

## Problem and context
E7 needs exact expected scores under a named version. A new fixture with adverse cases (giveaway, warning, namesake, copy) was offered and declined: demo-v1 stays, and the adverse cases are proven in unit specs.

## Decision
- `ScoringResult.algorithmVersion = "confidence-1"` (`ConfidenceScoringService.ALGORITHM_VERSION`). Any change to a value, keyword list, precedence, sentence or support rule is a new version, so E6 comparison flags `ALGORITHM_VERSION_DIFFERS`.
- `ProvisionalConfidenceScoring` (`provisional-1`) is deleted; DS1-003 is superseded.
- Expected scores with demo-v1 (`synthetic` source; contract `0xde0b…` excluded as `SMART_CONTRACT` when the bytecode check runs):

| Case identifiers | `1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa` (BTC) | `0x5aaeb605…beaed` (ETH) | `bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4` (BTC) |
|---|---|---|---|
| nickname `zenek_fx` | 75: `SALE_OR_SETTLEMENT`, `INDEPENDENT_SOURCES`, `NICKNAME_ONLY` | 75 (same rules) | 60: `SALE_OR_SETTLEMENT`, `NICKNAME_ONLY` |
| nickname + profile address `https://x.example/zenek_fx` (also the full demo-v1 person) | 95: `OWN_PROFILE`, `INDEPENDENT_SOURCES` | 95 (same rules) | 60: `SALE_OR_SETTLEMENT`, `NICKNAME_ONLY` |
| wallet `0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed` only | 0: `NO_PERSON_LINK` | 0: `ENTERED_BY_ANALYST`, `NO_PERSON_LINK` | not found |

Verified in the running app on 2026-11-21 (profile `local`, MongoDB in Docker, `EXCLUSION_ONCHAIN_CHECK_ENABLED=false`): the same levels and rules, plus, because no bytecode check runs, `CLASSIFICATION_UNAVAILABLE` on both ETH addresses and the contract `0xde0b…` shown as an unclassified 60% Attribution (`SALE_OR_SETTLEMENT`, `NICKNAME_ONLY`), which is gap G2 of DS1-007. A stored Analysis without the `justification` field (before `confidence-1`) reads back with an empty text.

## Consequences and risks
- **Known gap (E7):** demo-v1 has no low-score Attribution and none of the E7.3 adverse cases except the excluded contract; the film cannot show them. A fixture `demo-v2` (owner E2 T4 / E7) can add them without changing the algorithm.
- DS1-007 G1 (malformed address never detected) and G2 (exchange label) are unchanged.

## Verification
`AnalysisFlowSpec`: nickname Case 75 / 75 / 60 with rules and text; nickname + profile address 95 / 95 / 60; wallet-only Case 0 / 0; `algorithmVersion == 'confidence-1'`.

## Related
- Files: `service/ConfidenceScoringService.kt`, `resources/fixtures/demo-v1.json`. Decisions: DS1-003, DS1-007, E7-D3.

## Reconsider when
A new fixture version is added (extend this table) or any rule changes (new version, new table).
