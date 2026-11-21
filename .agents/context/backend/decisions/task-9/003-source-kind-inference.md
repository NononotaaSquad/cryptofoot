# D9-003: The algorithm infers the source kind from URL, snippet and Case identifiers

- **Status:** accepted
- **Date:** 2026-11-21
- **Type:** own proposal chosen by the user
- **Source:** cheat sheet KROK 2 (kinds of Web2 source); E7.3 ("same nickname used by an unrelated person: no automatic identity merge"); E2 boundary rule "the field agent does not judge"
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., grilling session 2026-11-21

## Problem and context
A Mention carries only source name, URL, collection time and a snippet of at most 200 characters. The bands of D9-002 depend on the kind of page the address was published on.

## Options considered
1. The algorithm infers the kind with fixed, versioned rules (no change to E2).
2. The source declares a page type on every Mention (new field in `Mention`, `RawMention` and the fixture; changes Maksym Z.'s module).
3. A configured host -> kind table.

## Decision
Option 1. Per Mention, in this precedence:

1. **`OWN_PROFILE`** if the Mention URL lies under the profile address of the Case (scheme, `www.`, case and trailing `/` ignored), **or** the first path segment of the URL equals the nickname or login (leading `@` ignored) **and** the URL or snippet also holds the Case's e-mail, messenger ID or full name. A page named after the nickname alone may belong to a namesake (E7.3), so it is not enough.
2. **`MASS_COMMENT`** if URL + snippet contain a word starting with: `giveaway`, `konkurs`, `rozdani`, `rozdajemy`, `airdrop`, `wygraj`, `promo`.
3. **`SALE_OR_SETTLEMENT`** if URL + snippet contain a word starting with: `ogłosz`, `sprzeda`, `kupi`, `ofert`, `p2p`, `płatnoś`, `zapłat`, `wpłat`, `przelew`, `rozlicz`, `faktur`.
4. **`DISCUSSION`** otherwise.

Matching is case- and diacritic-insensitive (`ł` -> `l`, combining marks removed) and anchored at the start of a word (`p2p` in `porady-p2p-od-zenka` matches; `promo` inside another word does not). The lists are part of algorithm version `confidence-1` (D9-008).

## Consequences and risks
- Keyword heuristics can misread a page (e.g. "nie sprzedaję" still reads as a sale). The kind is visible in `appliedRules` and in the Justification sentence, so the Analyst can check it against the Mention.
- A verified social account cannot be detected; only the Analyst's profile address and the nickname page with a second identifier reach 80.
- Changing a list or the precedence changes scores and requires a new algorithm version.

## Verification
`ConfidenceScoringServiceSpec`: kinds per keyword (with and without diacritics), mass comment beats sale, own profile by profile address, by nickname page + e-mail, by nickname page + full name, and not by the nickname page alone. `AnalysisFlowSpec`: with the profile address the demo post on `x.example/zenek_fx` scores 95.

## Related
- Files: `service/ConfidenceScoringService.kt`. Decisions: D9-002, D9-004, D9-008.

## Reconsider when
A real source adapter knows its page type for certain (GitHub profile, P2P marketplace): then option 2 for that source is more accurate than keywords.
