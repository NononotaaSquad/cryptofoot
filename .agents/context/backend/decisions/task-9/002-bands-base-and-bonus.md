# D9-002: Bands from the appendix, the strongest Mention sets the base, +15 once

- **Status:** accepted
- **Date:** 2026-11-21
- **Type:** answer to team question 8 ("which Confidence Level bands do we use")
- **Source:** challenge cheat sheet KROK 2 (bands 80–100, 60–79, 40–59, 10–39, 0; synergy rule +15, capped at 100); issue #9
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., grilling session 2026-11-21

## Problem and context
The cheat sheet gives bands per kind of Web2 source and a +15 bonus for two independent sources. The algorithm needs one exact value per kind so that the same input gives the same level.

## Options considered
1. Lower bound of each band: 80 / 60 / 40 / 10.
2. Midpoint: 90 / 70 / 50 / 25 (own profile + second source hits the 100 cap).
3. Own bands.

## Decision
Option 1.

| Rule | Contribution | When |
|---|---|---|
| `OWN_PROFILE` | 80 | strongest supporting Mention is on the person's own profile (D9-003) |
| `SALE_OR_SETTLEMENT` | 60 | sale ad, P2P offer, payment or settlement context |
| `DISCUSSION` | 40 | any other supporting Mention ("needs a second trace") |
| `MASS_COMMENT` | 10 | giveaway, contest, promotion, airdrop |
| `NO_PERSON_LINK` | 0 | no supporting Mention (D9-004) |
| `INDEPENDENT_SOURCES` | +15 | at least two supporting Mentions of different origin and different text (D9-005) |

- The base is the contribution of the **strongest kind** among the supporting Mentions; weaker Mentions do not add points. The bonus is applied once, however many sources agree.
- Confidence Level = sum of contributions, capped to 0..100 (highest reachable: 95).
- Rules with contribution 0 (`ENTERED_BY_ANALYST`, `SAME_SERVICE`, `COPIED_TEXT`, `NICKNAME_ONLY`, `WARNING_CONTEXT`, `CONFLICTING_EMAIL`, `CLASSIFICATION_UNAVAILABLE`) are still recorded in `appliedRules`: they explain why a level is not higher and produce their Justification sentence.
- 0% for an Excluded Address is decided by #22 before scoring; an Attribution at 0% means "no supporting Mention", not an exclusion.

## Consequences and risks
- The +15 is always visible as a step (e.g. 60 -> 75); the cap never triggers with these values.
- An Attribution at 0% and an Excluded Address (also 0%) can look alike; the Justification and the separate report section tell them apart. Hiding 0% Attributions is an E5 decision.

## Verification
`ConfidenceScoringServiceSpec`: one Mention per kind gives 80 / 60 / 40 / 10; three independent sources give 95 with one bonus.

## Related
- Files: `service/ConfidenceScoringService.kt` (`ScoringRule`). Decisions: D9-003, D9-004, D9-005.

## Reconsider when
The mentors or KAS give their own weights, or a kind needs a value inside its band (e.g. a verified social account).
