# D9-005: Independent sources differ in origin and in the sentence that holds the address

- **Status:** accepted
- **Date:** 2026-11-21
- **Type:** own proposal chosen by the user; refines DS1-003
- **Source:** cheat sheet KROK 2 ("independent = sources of different origin; several occurrences within one service are no extra confirmation"); E7.3 ("copied Mention on another source: no automatic independent-source bonus")
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., grilling session 2026-11-21

## Problem and context
DS1-003 treated two Mentions as copies only when their whole snippets were equal. A snippet is a 200-character window around the identifier, so a copied post on another site rarely gives the same window and the copy still earned the bonus.

## Options considered
1. Same address sentence (normalised).
2. Near-duplicate snippets (word overlap of at least 80%).
3. Exact snippet (as in DS1-003).

## Decision
Option 1. Among the **supporting** Mentions (D9-004):
- **Origin** = URL host, lower-cased, without `www.` (fallback: the whole URL).
- **Address sentence** = the sentence of the snippet that holds the address (split after `.`, `!`, `?` followed by whitespace, and at line breaks), compared case-, diacritic-, punctuation- and whitespace-insensitively.
- `INDEPENDENT_SOURCES` (+15) when at least one pair has a different origin **and** a different address sentence; its Justification lists the hosts of such pairs.
- Otherwise, when a pair of different origin exists but all such pairs share the sentence: `COPIED_TEXT` (0).
- Otherwise, when there are several supporting Mentions on one origin: `SAME_SERVICE` (0).

## Consequences and risks
- A person reusing the same short line on two services (e.g. "BTC: <address>") is treated as a copy: conservative, never inflates the level.
- Subdomains are different origins (`forum.example` vs `blog.forum.example`); acceptable for the fixed, short source list.

## Verification
`ConfidenceScoringServiceSpec`: three origins give +15 once; no bonus for two Mentions on one service, for `www.` and bare host, and for the same sentence copied into a different snippet on another service.

## Related
- Files: `service/ConfidenceScoringService.kt`. Decisions: DS1-003, D9-002, D9-004.

## Reconsider when
A real source returns full page text (longer snippets), or copies with small edits must be caught.
