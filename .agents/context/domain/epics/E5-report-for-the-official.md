# E5. Report for the official

**Status:** in-application report with an expandable list confirmed by the team on 2026-11-21, based on mentor feedback; remaining details below are open. **Module:** M4. Official's view.

Business references: [BUSINESS-SPEC.md](../BUSINESS-SPEC.md), [BUSINESS-CONTEXT.md](../BUSINESS-CONTEXT.md).
Challenge reference: [task](../hacknation-2026-task/MF%20-%20Pełna%20Treść%20Zadania%20Konkursowego.md), sections 3, 6, 7 and 8.

## Goal

The Analyst can decide which Attributions require further verification and check the facts and calculations behind each result without manually assembling the sources.

## Required outcome

The task requires understandable results, source provenance, reproducible scoring, selected blockchain indicators and a clear separation of facts, calculations and conclusions requiring verification. Confirmed presentation: a report inside the application with an expandable list. Exact row contents and grouping remain to be designed; the previous table proposal is not a fixed requirement.

## User stories and acceptance criteria

### E5.1. Read the Analysis summary

As an Analyst, I want to see the result and coverage of one Analysis, so that I know what was checked and how complete the result is.

- The report identifies the Case and Analysis, the input identifiers used, collection times and the scope of sources and blockchain networks checked.
- Attributions are sorted by Confidence Level descending, with address and network displayed together.
- Each Attribution has an expandable list item. Expanding it exposes its sources, scoring details and available Address Activity; collapsing it returns to the compact summary.
- Zero Attributions, unavailable sources, unavailable blockchain data and an incomplete Analysis have distinct messages. Missing data is not displayed as zero activity or proof of no connection.
- The report states that Attributions are leads for verification and that a cash-out indicator does not establish an actual fiat withdrawal or a tax violation.

### E5.2. Verify one Attribution

As an Analyst, I want to inspect the sources and calculation behind a Confidence Level, so that I can verify the result.

- Each Attribution exposes the relevant Mentions: source, URL or resource identifier, collection timestamp and the captured context or a reference to retained evidence.
- Source facts, calculated values and conclusions requiring verification are visibly distinguished.
- Applied scoring rules, their contributions and the algorithm version are available. The report presents the calculation supplied by E3; it does not invent rules or recalculate a different score.
- The report does not infer address ownership merely from its occurrence in a Mention.
- Selected Address Activity supplied by E4 is shown chronologically, with transaction references, network, timestamps, amounts and units where available. Coverage limitations are stated.

### E5.3. Inspect exclusions

As an Analyst, I want to inspect rejected Attributions without mixing them with accepted leads, so that I can verify the protection against incorrect assignments.

- Excluded Addresses are available separately, with the reason and supporting source or classification result.
- An unavailable classification is distinguished from a confirmed exclusion.
- Proposed grouping within the expandable report: a separate collapsed section with an exclusion count. The exact grouping remains open; the expandable in-application report itself is confirmed.

### E5.4. Reopen the report in the application

As an Analyst, I want to reopen the report for a saved Analysis in the application, so that I can return to the same result.

- The reopened report identifies the saved Analysis and retains sources, scoring rules, exclusions, limitations and the distinction between facts and conclusions.
- Opening a saved report does not silently rerun the Analysis or replace its results with newer data.
- Print, PDF export and transfer outside the application are outside the confirmed scope. They can be considered later if explicitly requested. The competition's PDF slide submission is a separate E7 deliverable.

## Dependencies and boundaries

- E1 supplies the Case and identifiers; E2 supplies Mentions, retained context and source coverage; E3 supplies Attributions, exclusions and scoring details; E4 supplies Address Activity; E6 supplies the saved Analysis.
- Missing upstream fields are explicit integration gaps. This epic does not redefine E3's algorithm, E4's indicators or evidence retention policy.
- No graph editor, AI narrative, report emailing, or external sharing service is included in the proposed minimum.

## Decisions to confirm

| ID | Question and proposed resolution | Owner | Needed before |
|---|---|---|---|
| E5-D1 | CONFIRMED: report in the application with an expandable list, based on mentor feedback relayed by the team on 2026-11-21. Exact grouping remains open. | Team | Recorded |
| E5-D2 | PDF/print export is outside the confirmed scope; no export implementation is required by this epic at present. | Team | Revisit only if requested |
| E5-D3 | Can E2/E3/E4 supply retained context, scoring contributions, classification provenance and transaction references? | Owners of E2/E3/E4 | Integration contract |
| E5-D4 | Which external services are approved for report data? | Mentor / organiser | Any such integration |

## Documentation reconciliation

The proposal to display scoring rules and contributions conflicts with the current narrow definition of Justification in BUSINESS-CONTEXT.md and BUSINESS-SPEC.md. The team must reconcile those definitions and E3 before implementation. Displaying structured rules does not require AI-generated prose.
