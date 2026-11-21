# Views and user paths

State after the frontend grilling session and the first Pencil design round of 2026-11-21. Terms follow the glossary in `../domain/BUSINESS-CONTEXT.md`; Polish UI labels are given in parentheses and are used as written (sentence case). This file describes what the Analyst sees and what happens on each action. Visual design (colors, components, typography) lives in [kas-ui-ux/](kas-ui-ux/README.md); this file only refers to it.

## Problem Statement

The Analyst (PL: Analityk), a KAS official without IT knowledge, needs to enter what they know about an investigated person and get a report on that person's cryptocurrency addresses. Later they need to return to the same person, see earlier results and run the search again. The interface must stay quiet: one clear starting point, no noise, no technical terms.

## Solution

The application has three views:

1. **New case** (PL: Nowa sprawa): the start page. A clean form for the person's identifiers and one button, "Rozpocznij analizę".
2. **Case list** (PL: Lista spraw): a separate view for browsing many Cases at once and finding one of them.
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
   └──────────── Case list (separate view, reachable from the header of every view) ◄──┘
```

1. The Analyst opens the application and sees **New case**; the header has a link to the Case list.
2. They fill in at least one narrowing identifier and click "Rozpocznij analizę".
3. The Case is created automatically and its first Analysis starts. A blocking loader is shown until the Analysis ends.
4. The Analyst lands on **Case details** with the report of that Analysis, without an extra click.
5. Later they open the Case from the **Case list**, pick an earlier Analysis from the history or start a new one.

## User Stories

### New case

1. As an Analyst, I want to see a clean form when I open the application, so that I can start work at once.
2. As an Analyst, I want to enter from one field up to the full set of identifiers (nickname, login, e-mail, messenger ID, profile address, public cryptocurrency address, full name), so that the Analysis is as accurate as possible.
2a. As an Analyst, I want to enter several values of one kind in one field, separated by commas (e.g. two nicknames), so that I can pass everything I know about the person.
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

14. As an Analyst, I want the Case list as a separate view that shows many Cases at once, so that the start page stays clean and I can still browse all my Cases.
15. As an Analyst, I want to open the Case list from every view, so that I can switch to another Case at any time.
15a. As an Analyst, I want to search the Case list by the data that identify a person (details to be decided, see V3), so that I find a Case without noise.
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

- **Routes:** three addresses. New case is the start page. The Case list has its own address (path to be fixed with the view, see V3). Case details has the Case identifier in the path and the selected Analysis identifier in the query, in the form `/cases/{caseId}?analysis={analysisId}`. Without the query the newest Analysis is shown.
- **Case list view:** a separate view, not a side panel. It shows many Cases at once. When a Case has a full name, the row also shows more detailed data that the Analyst can search by, without noise. Which data and how the search works is to be decided (V3).
- **Header:** a white bar with a thin divider. On the left: the KAS sign (the sign alone, without the caption "Krajowa Administracja Skarbowa") and next to it the text "Projekt OŁCyŚ" (20 px, bold, navy). No godło. On the right: the link "Lista spraw". Permission to use the KAS sign is still an open question in `kas-ui-ux/`.
- **Case counter:** a small red circle with a number at the top-right corner of the "Lista spraw" link. It shows how many Cases still wait for the Analyst's review. Which Cases count is to be decided with the Case list (V3).
- **Footer:** one line, "Wszelkie prawa zastrzeżone. Krajowa Administracja Skarbowa, projekt OŁCyŚ." No links.
- **Font:** Open Sans, as in the gov.pl styleguide.
- **Identifier fields:** the public cryptocurrency address is an input identifier, as in the challenge text (section 2). It counts as a narrowing identifier.
- **Several values in one field:** every identifier kind that a person can have more than one of (e.g. nickname, e-mail, cryptocurrency address) accepts several values separated by commas.
- **No asterisk:** no single field is required, so fields are not marked with `*`. The rule "at least one" is stated once for the whole group of narrowing identifiers.
- **Form layout:** decided on the Pencil drafts (frame "[MAIN] Nowa sprawa"). The form is a left column with two groups: "Identyfikatory" (six narrowing identifiers in a grid of two columns and three rows) and, under a divider, "Dane dodatkowe" (full name, marked "(opcjonalnie)"). Every field has an "np. …" example inside and a one-sentence hint below. The actions are the primary button "Rozpocznij analizę" and the text action "Wyczyść" next to it. A grey column on the right, "Co się stanie, gdy rozpoczniesz analizę", lists four steps and a reminder of the minimum data rule. The biała lista pattern of a radio group that shows one field does not apply, because the Analyst fills in many fields at once.
- **One action creates the Case and starts the Analysis:** "Rozpocznij analizę" on New case. On Case details the button is "Rozpocznij nową analizę" and creates another Analysis of the same Case.
- **Minimum data rule:** at least one narrowing identifier (nickname, login, e-mail, messenger ID, profile address, public cryptocurrency address) is required.
  - A full name alone is too little data. When only the full name is filled in, the pop-up asks the Analyst to add at least one narrowing identifier.
  - When any narrowing identifier is filled in, nothing more is required; the full name stays optional.
  - When the form is empty, the pop-up lists the narrowing identifiers to choose from.
- **No duration in the copy:** the UI does not say how long an Analysis takes. The step in the guidance column reads "Analiza chwilę potrwa. Nie zamykaj w tym czasie strony." The demo has at most two minutes and runs on sample data.
- **Full name hint:** "Samo imię i nazwisko nie wystarczy, ponieważ prowadzi do zbyt ogólnych wyników."
- **Cases are immutable:** no editing of identifiers. A Case with different data is a new Case, subject to the duplicate rule below.
- **Duplicate rule:** a new Case is a duplicate when it shares **at least one narrowing identifier** with an existing Case. Duplicates are not accepted; the Analyst gets the pop-up "Taka sprawa już istnieje" with a way to open the existing Case.
- **Validation messages:** shown in a pop-up (dialog), not only inline. The pattern follows the gov.pl dialog in `kas-ui-ux/`: a title, one sentence, one confirming button ("Rozumiem").
- **Duplicate pop-up:** the exception to the one-button pattern. It has two actions: the primary button "Otwórz sprawę" and the text button "Zamknij".
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
| V3 | Case list view: its address, what a row shows (in particular the more detailed data for a Case with a full name) and what the Analyst can search by. Also: which Cases the counter in the header counts as waiting for review. | Design work |
| V4 | Look of the blocking loader; the gov.pl styleguide lists a Loader component without a screenshot. | Design work |
| V5 | Look of status badges, the Confidence Level indicator, the history of Analyses and the Excluded Addresses section; not covered by `kas-ui-ux/`. | Design work |
| V6 | Normalisation for the duplicate rule (e.g. case of e-mail, spaces at the ends). | Case owner (Maksym) |
| V7 | Closed on 2026-11-21: the layout of the New case form is described in Implementation Decisions (Form layout). | Design work |
| V8 | The Case contract must accept the public cryptocurrency address and several values per identifier kind; the glossary entry for Case in `BUSINESS-CONTEXT.md` does not list them yet. | Case owner (Maksym) |
