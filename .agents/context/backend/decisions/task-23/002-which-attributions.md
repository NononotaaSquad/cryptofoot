# D23-002: Only Attributions with a Confidence Level above 0 are checked

- **Status:** accepted (owner); mentor question 2 ("for which Attributions") still open
- **Date:** 2026-11-21
- **Type:** owner decision under an open mentor question
- **Source:** BUSINESS-SPEC question 2; main rule "little noise"; D9-004 (0% = no supporting Mention); task section 5.7 (data minimisation)
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., grilling 2026-11-21. Not approved by mentors.

## Problem and context
Which Attributions should get Address Activity: all of them, those above a threshold, or those the Analyst asks for?

## Options considered
1. Every Attribution with a Confidence Level above 0, including those whose classification was unavailable.
2. All Attributions, including 0%.
3. Confidence Level of 60 or more only.
4. On request (rejected in D23-001).

## Decision
Option 1. `AddressActivityService` checks `confidenceLevel > 0`. A 0% Attribution has no Mention that links it to the person (for example the Analyst's own wallet, D50-006): its activity would be noise, and fetching it would send an address out for nothing. Attributions flagged `classificationStatus = UNAVAILABLE` are checked; the flag already warns the Analyst.

The report shows `addressActivityStatus`:

| Status | When | `addressActivity` |
|---|---|---|
| `AVAILABLE` | a source answered | the data |
| `UNAVAILABLE` | checked, but no source answered (live explorers off, down, unusable answer, unsupported network) | `null`; "Brak danych nie oznacza braku aktywności" |
| `NOT_CHECKED` | Confidence Level 0, or an Analysis stored before E4 | `null` |

## Consequences and risks
- A 40% discussion lead gets activity too; the Confidence Level stays the main ordering.
- An unclassified address may in fact be an exchange or a contract, so its activity can be large. The classification flag and the Justification sentence warn about it.

## Verification
`AddressActivityServiceSpec` "only Attributions with a Confidence Level above 0 are checked"; `AnalysisFlowSpec` wallet-only Case (`NOT_CHECKED`); `AddressActivityLiveSpec` "an Attribution at 0% is not checked"; E2E "an Attribution at 0% is not checked for Address Activity".

## Related
- Files: `service/AddressActivityService.kt`, `service/AnalysisReportService.kt`. Decisions: D9-004, D24-006, D50-006.

## Reconsider when
Mentors answer question 2 (for example "only from 60%"). The rule is one line in `AddressActivityService.collect`.
