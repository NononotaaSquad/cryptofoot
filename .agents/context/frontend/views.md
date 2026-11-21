# Views and user paths

State after the frontend grilling session of 2026-11-21. Terms follow the glossary in `../domain/BUSINESS-CONTEXT.md`; Polish UI labels are given in parentheses and are used as written (sentence case). This file describes what the Analyst sees and what happens on each action. Visual design (colors, components, typography) lives in [kas-ui-ux/](kas-ui-ux/README.md); this file only refers to it.

## Problem Statement

The Analyst (PL: Analityk), a KAS official without IT knowledge, needs to enter what they know about an investigated person and get a report on that person's cryptocurrency addresses. Later they need to return to the same person, see earlier results and run the search again. The interface must stay quiet: one clear starting point, no noise, no technical terms.

## Solution

The application has three views:

1. **New case** (PL: Nowa sprawa): the start page. A clean form for the person's identifiers and one button, "Rozpocznij analizę".
2. **Case list** (PL: Lista spraw): a collapsible side panel, collapsed by default, for browsing existing Cases.
3. **Case details** (PL: Szczegóły sprawy): the Case scope on top (identifiers, history of Analyses, "Rozpocznij nową analizę") and, below it, the report of the selected Analysis.

### Model behind the views

- A **Case** (PL: Sprawa) is one investigated person and their identifiers. A Case **cannot be edited** after it is created.
- A Case has **many Analyses** (PL: Analiza). Each Analysis is saved separately; a later one never overwrites an earlier one.
- The **report belongs to an Analysis**, not to the Case. Two Analyses of one Case can give different results.

| Case scope | Analysis scope |
|---|---|
| identifiers of the person | status of the Analysis |
| history of Analyses | checked sources and collection times |
| "Rozpocznij nową analizę" button | Attributions (PL: Powiązania) with Confidence Level (PL: Stopień pewności) and Mentions (PL: Wzmianki) |
| schedule (P3) | Excluded Addresses (PL: Adresy wykluczone) |

### Main user path

```
New case ──"Rozpocznij analizę"──► blocking loader ──► Case details (report of this Analysis)
   ▲                                                        │
   │                                       "Rozpocznij nową analizę" ──► blocking loader ──► Case details (new Analysis)
   │                                                        │
   └──────────── Case list (collapsible side panel, available from every view) ◄──┘
```

1. The Analyst opens the application and sees **New case**, with the Case list collapsed.
2. They fill in at least one narrowing identifier and click "Rozpocznij analizę".
3. The Case is created automatically and its first Analysis starts. A blocking loader is shown until the Analysis ends.
4. The Analyst lands on **Case details** with the report of that Analysis, without an extra click.
5. Later they open the Case from the **Case list**, pick an earlier Analysis from the history or start a new one.

## User Stories

### New case

1. As an Analyst, I want to see a clean form when I open the application, so that I can start work at once.
2. As an Analyst, I want to enter from one field up to the full set of identifiers (nickname, login, e-mail, messenger ID, profile address, full name), so that the Analysis is as accurate as possible.
3. As an Analyst, I want the form to show which fields are narrowing identifiers and which are additional data (full name), so that I know what I must enter.
4. As an Analyst, I want one button, "Rozpocznij analizę", that creates the Case and starts the Analysis, so that I do not have to create the Case as a separate step.
5. As an Analyst, I want the button to always be active, so that clicking it tells me what is missing instead of silently doing nothing.
6. As an Analyst, I want a pop-up that says exactly what is missing when I entered no narrowing identifier, so that I can fix it at once.
7. As an Analyst, I want a pop-up that says a full name alone is not enough, so that I understand why the Analysis did not start.
8. As an Analyst, I want a pop-up "Taka sprawa już istnieje" with a way to open the existing Case when a Case with any of my narrowing identifiers already exists, so that the same person is not investigated in two Cases.

### Running an Analysis

9. As an Analyst, I want a full-screen loader with the message that I must wait and must not close the page, so that I know the Analysis is running and that leaving can break it.
10. As an Analyst, I want the browser to warn me when I try to close the tab during an Analysis, so that I do not close it by accident.
11. As an Analyst, I want to see the report of the finished Analysis right after the loader disappears, so that I do not have to look for it.
12. As an Analyst, I want an Analysis that I left by closing the tab to still finish and appear in the history of the Case, so that the work is not lost (to be confirmed by the Analysis owner, see Further Notes).
13. As an Analyst, I want a scheduled Analysis to appear in the history of the Case with its status, without blocking my work, so that periodic checks do not interrupt me.

### Case list

14. As an Analyst, I want the Case list as a side panel that is collapsed by default, so that it does not add noise when I start.
15. As an Analyst, I want to expand the Case list from every view, so that I can switch to another Case at any time.
16. As an Analyst, I want each Case on the list labelled with the full name or, without one, the first narrowing identifier, so that I can recognise the person.
17. As an Analyst, I want to see the date and status of the last Analysis under each Case, so that I know which Cases have new results.
18. As an Analyst, I want the newest Case at the top, so that I find my current work first.

### Case details

19. As an Analyst, I want to see the identifiers of the Case on top, so that I know which person the report is about.
20. As an Analyst, I want to see the history of Analyses of the Case, so that I can return to an earlier result.
21. As an Analyst, I want the newest Analysis selected by default, so that I see the current result first.
22. As an Analyst, I want to select another Analysis from the history and see its report, so that I can inspect the result as it was obtained.
23. As an Analyst, I want the selected Analysis to be part of the page address, so that I can reopen the same report later.
24. As an Analyst, I want a "Rozpocznij nową analizę" button, so that I can check for new findings while the earlier Analyses stay in the history.
25. As an Analyst, I want the report of the selected Analysis below the Case data, so that the Case and its results are on one screen.
26. As an Analyst, I want the report to show the status of the Analysis, the checked sources and the collection times, so that I know what was covered.
27. As an Analyst, I want the report to show Attributions as an expandable list sorted by Confidence Level, so that I see the strongest leads first (details in E5).
28. As an Analyst, I want Excluded Addresses in a separate section, so that they do not mix with the leads (details in E5.3).

## Implementation Decisions

- **Routes:** two addresses. New case is the start page. Case details has the Case identifier in the path and the selected Analysis identifier in the query, in the form `/cases/{caseId}?analysis={analysisId}`. Without the query the newest Analysis is shown. The Case list is part of the layout, not a route.
- **Case list panel:** collapsible, collapsed by default. Its look is decided after the KAS / Ministry of Finance design work.
- **One action creates the Case and starts the Analysis:** "Rozpocznij analizę" on New case. On Case details the button is "Rozpocznij nową analizę" and creates another Analysis of the same Case.
- **Minimum data rule:** at least one narrowing identifier (nickname, login, e-mail, messenger ID, profile address) is required.
  - A full name alone is too little data. When only the full name is filled in, the pop-up asks the Analyst to add at least one narrowing identifier.
  - When any narrowing identifier is filled in, nothing more is required; the full name stays optional.
  - When the form is empty, the pop-up lists the narrowing identifiers to choose from.
- **Cases are immutable:** no editing of identifiers. A Case with different data is a new Case, subject to the duplicate rule below.
- **Duplicate rule:** a new Case is a duplicate when it shares **at least one narrowing identifier** with an existing Case. Duplicates are not accepted; the Analyst gets the pop-up "Taka sprawa już istnieje" with a way to open the existing Case.
- **Validation messages:** shown in a pop-up (dialog), not only inline. The pattern follows the gov.pl dialog in `kas-ui-ux/`: a title, one sentence, one confirming button ("Rozumiem").
- **Manual Analysis blocks the view:** a full-screen loader ("Trwa analiza. Nie zamykaj tej strony.") without navigation until the Analysis ends, plus a browser warning on tab close. After it ends, Case details opens with this Analysis selected.
- **Scheduled Analysis does not block:** it runs on the server and shows up in the history with its status.
- **Analysis status names** (running, completed, partial, failed) come from the Analysis owner and are not fixed here.
- **Backend dependency:** until the Case and Analysis contracts are agreed with the backend owners, views run on mocks built on the agreed types; mocks are replaced by the generated client afterwards.
- **Language:** UI copy in Polish, addressing the Analyst as "Ty" with imperative verbs ("Rozpocznij", "Sprawdź"), as in the gov.pl materials. No technical terms.
- **Accessibility:** target WCAG 2.1 AA (contrast 4.5:1 for text, keyboard operation, information not carried by color alone).

## Testing Decisions

Deferred. Test seams are decided after the views are laid out in Pencil. The test scope of ADR-0010 applies.

## Out of Scope

- Editing a Case.
- Leaving the view during a manual Analysis.
- Merging two Cases or adding new identifiers to an existing Case.
- Visual design details (colors, typography, components): see `kas-ui-ux/`.
- Comparison of two Analyses (E6.3) and the schedule controls (E6.4): later phases, placed in the Case scope of Case details.
- Print, PDF export and passing the report outside the application (E5-D2).

## Further Notes

### Known limitation

Because Cases cannot be edited and duplicates are rejected, the Analyst cannot add a new identifier to a known person. Example: the e-mail matches an existing Case, but the nickname is new. The Analyst is sent to the existing Case, and the new nickname is not searched. To revisit if the team needs it.

### Open questions

| # | Question | Owner |
|---|---|---|
| V1 | Does a manual Analysis still finish and get saved on the server when the Analyst closes the tab? The frontend assumes yes. Asked in issue #24. | Bohdan (Analysis) |
| V2 | Exact status names of an Analysis and how "completed, no Mentions" is told apart from a failure. | Bohdan (Analysis) |
| V3 | Look of the collapsible Case list panel; gov.pl has only a fixed blue side menu. | Design work |
| V4 | Look of the blocking loader; the gov.pl styleguide lists a Loader component without a screenshot. | Design work |
| V5 | Look of status badges, the Confidence Level indicator, the history of Analyses and the Excluded Addresses section; not covered by `kas-ui-ux/`. | Design work |
| V6 | Normalisation for the duplicate rule (e.g. case of e-mail, spaces at the ends). | Case owner (Maksym) |
