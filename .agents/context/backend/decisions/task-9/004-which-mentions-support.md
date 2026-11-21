# D9-004: Which Mentions support an address

- **Status:** accepted
- **Date:** 2026-11-21
- **Type:** own proposal chosen by the user; enforces D50-006 on the E3 side
- **Source:** issue #9 ("an address is not attributed merely because it occurs in a Mention"); E7.3 adverse cases (warning about another person, same nickname used by an unrelated person); D50-006 (the wallet entered by the Analyst is input, not evidence); E1 and DS1-004 (a full name never suffices)
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., grilling session 2026-11-21

## Problem and context
Every Mention of a candidate contains its address, but not every Mention says the address is the person's. Without a filter, a warning ("oszust podaje adres ..."), a namesake's post or a page found only through the Analyst's own wallet would raise the Confidence Level.

## Options considered
For an address with no support: a 0% Attribution with an explanation; 10% (noise band); no Attribution at all (breaks "every candidate gets a Confidence Level" and the port). For the warning: no support; a fixed penalty; a cap at 39. For the namesake: conflicting e-mail + note; a cap for nickname-only links; a note only. For the full name: never a link; a link when next to the wallet.

## Decision
A Mention **supports** an address only when all of these hold:

1. **Person link:** its URL or snippet contains a non-wallet narrowing identifier of the Case: nickname, login, e-mail, messenger ID or profile address. The **full name never counts** (E1). The **wallet never counts** (D50-006): a Mention found only through the wallet links the address to nobody.
2. **No warning:** the sentence that holds the address has no word starting with `oszust`, `scam`, `podszyw`, `ostrzeg`, `ostrzeż`, `fałszyw`, `fake`, `phishing`, `wyłudz`, `nie wpłacaj`, `nie wysyłaj`, `nie używa`, `nie używaj`. Only that sentence is checked, so a warning about another address in the same snippet does not affect this one. Rule `WARNING_CONTEXT` (0) is recorded when any Mention of the address is a warning.
3. **No conflicting e-mail:** when the Case has an e-mail, a snippet that contains e-mail addresses but none of the Case's is about another person. Rule `CONFLICTING_EMAIL` (0) is recorded.

Then:
- No supporting Mention -> Attribution with **0%** and rule `NO_PERSON_LINK`.
- The candidate equal to the Case's wallet gets rule `ENTERED_BY_ANALYST` (0); its level rests only on supporting Mentions.
- When every supporting Mention links only by nickname or login, rule `NICKNAME_ONLY` (0) warns that the nickname may belong to someone else. It does not lower the score: the appendix accepts a P2P ad linked by the same nickname at 60–79.

## Consequences and risks
- A wallet-only Case yields Attributions at 0%: honest, but the report must not present them as leads (E5).
- A snippet naming a forum admin's e-mail next to the person's address is treated as conflicting when the Case has an e-mail and the person's own e-mail is absent; the rule is visible to the Analyst.
- The warning list is Polish and English only.

## Verification
`ConfidenceScoringServiceSpec`: warning gives 0%; a warning about another address in the same snippet leaves this one at 40; conflicting e-mail (only another e-mail; the person's own e-mail present too; Case without e-mail); wallet-only Mention gives no support and marks the wallet as input; the wallet scores only from nickname Mentions; the full name never links. `AnalysisFlowSpec`: a wallet-only demo Case gives 0% for both addresses.

## Related
- Files: `service/ConfidenceScoringService.kt`. Decisions: D50-006, DS1-004, D9-002. Gaps closed: DS1-007 G3.

## Reconsider when
E5 decides how to show 0% Attributions, or the team accepts the full name as a link next to another weak identifier.
