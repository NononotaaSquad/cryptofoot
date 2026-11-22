# Spec: Schedule on Case details

**Date:** 2026-11-21 · **Branch:** to be created by the lead from `master` (proposed `feature/FRONTEND-case-schedule`) · **Status:** decisions Q1–Q18 made by the lead; the test seams and the proposals P1–P8 below wait for the lead's review

This spec records the decisions of the grilling of 2026-11-21 (rounds Q1–Q18) for the Schedule (PL: Harmonogram) of a Case on Case details (PL: Szczegóły sprawy), wired to the backend of task #27. It follows the `to-spec` template and is saved as a file instead of a tracker issue, on the lead's request.

Sources this spec is built on, and which win when they differ from it:

- Pencil frames in `.agents/context/frontend/2026-11-21_kas-olcys-views_v01.pen` (read through the Pencil MCP, the file is encrypted): "[STAN] Szczegóły sprawy – harmonogram wyłączony", "… – harmonogram włączony", "… – harmonogram, popover otwarty", "… – harmonogram, nieudane uruchomienie". They win on layout, sizes and copy.
- `frontend/apps/web/lib/api/openapi.json`: the contract (`getSchedule`, `updateSchedule`, `ScheduleResponse`, `UpdateScheduleRequest`, `ScheduleAttemptResponse`). Never edited by hand.
- `.agents/context/domain/epics/E6-returning-to-a-case.md`, story E6.4: the requirement.
- `.agents/context/backend/decisions/task-27/` (D27-001 to D27-008) and `sync-1/006-schedule-requires-case.md`: what the backend does.
- `.agents/context/domain/BUSINESS-CONTEXT.md`: the glossary in force (Schedule, Frequency and Scheduled Analysis added on 2026-11-21).
- `.agents/rules/frontend.md` and the frontend ADRs (0007, 0008, 0009, 0010): how the code is written (win on code rules).
- `.agents/specs/2026-11-21_analysis-report/spec.md`: the Case details view this spec extends.

## Problem Statement

The challenge requires Analyses to run periodically (§2 "cykliczne wykonywanie analiz"; deferred as D1 in `views.md`). The backend can already do it: a Case has a Schedule that can be enabled with a Frequency, and the server starts Scheduled Analyses by itself. The Analyst (PL: Analityk) cannot use any of it. Case details has no control to enable the Schedule, shows no next term, and does not tell the Analyst when a planned Analysis was skipped or could not start. The only trace is the word "Zaplanowana" in the select of Analyses.

## Solution

Next to "Rozpocznij nową analizę" in the Case header there is a small square button with a clock. It opens a popover "Harmonogram" under the button, where the Analyst picks one option, "Wyłączony" or a Frequency, and confirms with "Zapisz".

When the Schedule is enabled, a full-width state row between the Case header and the select "Wyświetlana analiza" shows the Frequency and the next term without any click. When the last planned Analysis was skipped or could not start, the same row turns into a warning that says so. The clock button looks different when the Schedule is enabled.

While the Schedule is enabled, the page checks the server in the background. A Scheduled Analysis appears in the select "Wyświetlana analiza" by itself; the report the Analyst is reading does not change.

## User Stories

### Finding and opening the Schedule

1. As an Analyst, I want a clock button next to "Rozpocznij nową analizę", so that I can find the Schedule where I start Analyses.
2. As an Analyst, I want the clock button to have the name "Harmonogram" as a tooltip and for screen readers, so that I know what an icon-only button does.
3. As an Analyst, I want the clock button to look different when the Schedule is enabled (tinted background and a check badge, not color alone), so that I see the state at a glance.
4. As an Analyst, I want the clock button to open a popover directly under it, so that I set the Schedule without leaving the Case.
5. As an Analyst, I want the popover to say "Analiza tej sprawy uruchomi się sama w wybranych terminach.", so that I understand what the Schedule does.
6. As an Analyst, I do not want the clock button on a Case without any Analysis, so that the empty Case shows one clear action.

### Setting the Schedule

7. As an Analyst, I want one group "Częstotliwość" with the options "Wyłączony", "Codziennie o 06:00", "Co 6 godzin (00:00, 06:00, 12:00, 18:00)" and "Co tydzień (poniedziałek, 06:00)", so that enabling and choosing the Frequency is one decision.
8. As an Analyst, I want the current setting to be selected when the popover opens, so that I see what is in force.
9. As an Analyst, I want a "Zapisz" button, so that the Schedule changes only when I confirm it.
10. As an Analyst, I want the popover to close after a successful save and the page to show the new state, so that I see the result without an extra message.
11. As an Analyst, I want the popover to stay open with a plain message when the save fails, so that I can try again without choosing anew.
12. As an Analyst, I want "Anuluj", the Escape key and a click outside to close the popover without saving, so that I can leave without changing anything.
13. As an Analyst, I want the hint "Godziny według czasu polskiego.", so that I know which clock the terms follow.
14. As an Analyst, I want to turn the Schedule off by choosing "Wyłączony" and "Zapisz", so that the Case is no longer checked by itself.

### Seeing the state of the Schedule

15. As an Analyst, I want a row "Harmonogram: codziennie o 06:00 · następna analiza: 22.11.2026, 06:00" under the Case header when the Schedule is enabled, so that I see the Frequency and the next term without a click.
16. As an Analyst, I want no such row when the Schedule is disabled, so that the page stays quiet.
17. As an Analyst, I want the popover to show "Następna analiza: {date}", so that I can check the next term while I change the setting.
18. As an Analyst, I want the popover to show "Ostatnie uruchomienie: {date}" with the link "Pokaż analizę", so that I can open the report of the last Scheduled Analysis.
19. As an Analyst, I want all dates and hours of the Schedule shown in Polish time, so that "Codziennie o 06:00" and the next term agree even when my computer uses another time zone.

### Skipped and failed terms

20. As an Analyst, I want the state row to become a warning "Nie udało się uruchomić zaplanowanej analizy {date}." when the last planned Analysis could not start, so that I do not assume the Case was checked.
21. As an Analyst, I want the state row to say that a term was skipped because another Analysis of this Case was running, so that I know why there is no new report.
22. As an Analyst, I want the state row to say that a term was skipped because the application was not running at that time, so that I know the gap is not a result.
23. As an Analyst, I want the warning to keep the next term, so that I know when the next attempt is.
24. As an Analyst, I want the same message in the popover, so that it is not hidden when the popover covers the row.
25. As an Analyst, I want the warning to use an icon and words, not color alone, so that I can read it in any condition.

### Scheduled Analyses in the history

26. As an Analyst, I want a Scheduled Analysis to appear in the select "Wyświetlana analiza" by itself while the page is open, so that I do not have to reload.
27. As an Analyst, I want the report I am reading to stay on screen when a Scheduled Analysis appears, so that I do not lose my place.
28. As an Analyst, I want the next term and the last attempt to refresh by themselves, so that the state row is never stale.
29. As an Analyst, I want a Scheduled Analysis to run without the blocking loader, so that it does not interrupt my work.

### Demonstration

30. As a person presenting the demo, I want an extra option "Co minutę (tryb demonstracyjny)" that is visible only when the frontend is started in demo mode, so that the film shows a Scheduled Analysis without waiting a day.
31. As an Analyst, I want a Schedule that runs on the demo Frequency to be labelled "Tryb demonstracyjny" in the state row, so that nobody takes it for the normal Frequency.
32. As a person presenting the demo, I want the page to check the server more often while the demo Frequency is on, so that the new Analysis shows up within seconds.

### General

33. As an Analyst, I want to operate the clock button, the options and both actions by keyboard, with focus returning to the clock button when the popover closes, so that the view meets WCAG 2.1 AA.
34. As an Analyst, I want the rest of Case details to work when the Schedule cannot be read, so that a Schedule problem does not hide the report.

## Implementation Decisions

### Decisions of the lead (Q1–Q18)

| # | Decision |
|---|---|
| Q1 | Scope of this spec: the Schedule only. The Case list is a separate spec. |
| Q2 | Glossary: Schedule (PL: Harmonogram), Frequency (PL: Częstotliwość), Scheduled Analysis (PL: Analiza zaplanowana). |
| Q3, Q14 | A square clock button next to "Rozpocznij nową analizę" opens a popover anchored under it, right-aligned to it, page not dimmed. |
| Q4 | One radio group with "Wyłączony" and the Frequencies, plus an explicit "Zapisz". No separate on/off switch. |
| Q5 | The demo Frequency is shown only when a frontend environment variable enables it. A Schedule answering `demo: true` is labelled "Tryb demonstracyjny". |
| Q6 | All four outcomes of the last attempt are shown. Skips and the failure are never hidden. |
| Q7 | Background polling while the Schedule is enabled. A new Analysis appears in the select; the displayed report does not change. |
| Q8, Q18 | Frames drawn in Pencil before code; the options not chosen were removed from the file. |
| Q9 | The state row is visible without a click when the Schedule is enabled; none when disabled. It uses the closed look of the former option C (full-width row, 4 px left border, icon, text). |
| Q10 | After "Zapisz" the popover closes and the state row shows the new state; no "Zapisano" message. On an error the popover stays open with the message above the actions. |
| Q11 | Polling every 30 s while the Schedule is enabled; every 5 s while the answer has `demo: true`. |
| Q12 | Hours of the Schedule are always in Polish time, with the hint "Godziny według czasu polskiego." |
| Q13 | No clock button on a Case without any Analysis. |
| Q15 | A skip or a failure replaces the state row in the same place (warning tint, warning icon, bold text). There is no second message elsewhere. |
| Q16 | The open popover may cover the right part of the state row and of the select; it repeats the same data. |
| Q17 | "Anuluj", Escape and a click outside close the popover; an unsaved choice is dropped without a question. |

### Modules

Following ADR-0009 and the "Case scope | Analysis scope" table of `views.md`, the Schedule is Case scope and is rendered by the Case details view.

- **Schedule view model** (new, pure, the main seam): one function from the Schedule answer of the server, the demo switch and nothing else, to everything the components render:
  - the list of options with their labels and which one is selected;
  - the look of the clock button (enabled or not);
  - the state row: none, information or warning, with its text and the "Tryb demonstracyjny" label;
  - the popover lines "Następna analiza" and the last attempt line, with the Analysis id for "Pokaż analizę" when there is one.

  A second small function turns a chosen option into the save request (`enabled` and `cadence`). Components do not read API types.
- **Schedule poll interval rule** (new, pure): from the Schedule answer (or none yet) to the next interval: 30 000 ms when enabled, 5 000 ms when `demo` is true, stop when disabled. The same shape as the existing poll interval rule of the Analysis run, and passed to TanStack Query `refetchInterval` in the same way.
- **Schedule labels**: Frequency and outcome texts live with the view model, as code → Polish text maps, like the report labels.
- **Schedule components** (new): the clock button with the popover, and the state row. They use the popover, radio group, tooltip and button of the shared UI package; no new UI primitive is written.
- **Case details view** (changed): reads the Schedule, renders the clock button in the Case header and the state row between the header and the select, and applies the poll interval rule to the Schedule and to the history of Analyses.
- **Environment** (changed): one new optional public variable for the demo switch, validated in the existing environment module with the other variable. Proposed name: `NEXT_PUBLIC_DEMO_SCHEDULE` (P1).

### API contract used

- `GET /api/1/cases/{caseId}/schedule` → `ScheduleResponse`: `enabled`, `cadence` (`DAILY`, `EVERY_6_HOURS`, `WEEKLY`, `DEMO_EVERY_MINUTE` or null), `demo`, `timezone`, `nextExecutionAt`, `lastAttempt` (`attemptedAt`, `outcome`, `analysisId`).
- `PUT /api/1/cases/{caseId}/schedule` with `UpdateScheduleRequest` (`enabled`, `cadence`) → `ScheduleResponse`. It answers 400 with a Polish message for the demo Frequency when the backend demo mode is off, and 404 for an unknown Case.
- `GET /api/1/cases/{caseId}/analyses`: already used by the select; now also refetched by the polling.
- No contract change is needed. Only the generated client is used.

### Specific interactions

- **Options → request:** "Wyłączony" saves `enabled: false`; every other option saves `enabled: true` with its `cadence`.
- **After a save:** the answer of `PUT` replaces the cached Schedule, so the state row and the polling follow at once without a second read.
- **"Pokaż analizę":** opens `/cases/{caseId}?analysis={analysisId}` and closes the popover. Shown only for the outcome `STARTED`, the only one with an Analysis id.
- **Polling:** only while the Schedule is enabled. It refreshes the Schedule and the history of Analyses. It never changes the `analysis` parameter of the address.
- **Time zone:** dates of the Schedule are formatted with `Intl` `pl-PL` in the zone returned in `ScheduleResponse.timezone`, not in the zone of the browser. Format as in the report: "22.11.2026, 06:00".
- **Outcome `STARTED` with a failed Analysis:** the attempt did start, so the state row stays plain; the failed Analysis is visible in the select with "Nieudana" (D27-007).
- **Manual start during a Scheduled Analysis:** the server answers 409 and the existing start error dialog is shown. Nothing new is built.

### Proposals waiting for the lead's review (P1–P8)

Not asked in the grilling; proposed so the implementation is not blocked. Each can be changed before coding.

| # | Proposal |
|---|---|
| P1 | The demo switch is the environment variable `NEXT_PUBLIC_DEMO_SCHEDULE=true`; absent means off. |
| P2 | Frequency texts in the state row: "codziennie o 06:00", "co 6 godzin", "co tydzień w poniedziałek o 06:00", "co minutę". |
| P3 | Warning texts of the state row, each followed by "Następna analiza: {date}.": `FAILED` "Nie udało się uruchomić zaplanowanej analizy {date}."; `SKIPPED_OVERLAP` "Pominięto zaplanowaną analizę {date}, ponieważ inna analiza tej sprawy była w toku."; `SKIPPED_DOWNTIME` "Pominięto zaplanowaną analizę {date}, ponieważ aplikacja wtedy nie działała." |
| P4 | A disabled Schedule shows no state row even when its last attempt was a skip or a failure; the last attempt stays visible in the popover. |
| P5 | When the server answers with the demo Frequency while the demo switch of the frontend is off, the option "Co minutę (tryb demonstracyjny)" is still listed and selected, so that the group always shows what is in force. |
| P6 | When the Schedule cannot be read: the clock button stays, there is no state row, and the popover shows `toUserMessage(error)` with "Spróbuj ponownie" in place of the options. |
| P7 | While a save is in flight, "Zapisz" shows a pending state and a second click is ignored. |
| P8 | Without any attempt yet, the popover has no "Ostatnie uruchomienie" line. |

## Testing Decisions

- **A good test here** checks external behaviour of pure logic: an API-shaped Schedule in, a view model or an interval out. No test looks at components, hooks, markup or private helpers (ADR-0010).
- **Seam 1, Schedule view model:**
  - disabled Schedule: "Wyłączony" selected, plain clock button, no state row;
  - each Frequency: selected option and state row text;
  - `demo: true`: the "Tryb demonstracyjny" label;
  - demo switch off and on: the demo option absent and present; demo Frequency from the server with the switch off (P5);
  - each outcome of the last attempt: `STARTED` with the Analysis id for the link, `SKIPPED_OVERLAP`, `SKIPPED_DOWNTIME`, `FAILED` as warnings with the next term;
  - no last attempt; no next term;
  - a date near midnight UTC is shown as the Polish date and hour;
  - option → request for "Wyłączony" and for each Frequency.
- **Seam 2, Schedule poll interval rule:** no answer yet, disabled, enabled, enabled with `demo: true`.
- **Not tested by Vitest:** the popover, focus handling, the tooltip, the save and its error, the polling itself. Verified in the running app with a clean console.
- **Fixtures:** typed with the generated model types, built by small factory functions inside the test files.
- **Prior art:** the tests of the report view model, of the Address Activity figures and of the Analysis run poll interval rule.
- **Quality gate:** `npm run check` and `npm run build` from `frontend/`.
- **Verified in the browser** against `./scripts/run-backend.sh` (profile `local` allows the demo Frequency and checks terms every 5 seconds): enable the demo Frequency, see the state row and the label, see a Scheduled Analysis appear in the select within a minute without a reload while the displayed report stays, disable the Schedule, reload and see the state kept.

## Out of Scope

- The Case list and the Case counter; whether a Case has an enabled Schedule is not shown there.
- A history of attempts: the backend keeps only the last one (D27-007).
- ~~Frequencies entered by the Analyst~~: added by #78 (D27-010): the option "Własny interwał" with a number and a unit (minuty, godziny, dni), the explanation "interwał a godzina", the note that frequent Analyses increase requests to sources, the Zapisz/Zapisano/error states, and the zone shown next to the next term. Cron-like rules stay out of scope.
- Notifications about a finished Scheduled Analysis.
- Resetting the Reviewed mark when a Scheduled Analysis ends: the mark lives in the browser until V11.
- Comparison of two Analyses (E6.3, D2).
- Any backend change.

## Further Notes

- **Before coding:** run `./scripts/api-sync.sh` to confirm there is no contract drift; read the docs of the installed versions of TanStack Query (`refetchInterval` as a function, behaviour in a background tab), of the popover, radio group and tooltip primitives in the shared UI package, and the local Next.js docs for public environment variables. Check in the backend that `PUT` with `enabled: false` and no `cadence` is accepted.
- **Icon:** the Pencil frames use `clock-4` because the Pencil icon set has no lucide `clock`; the code uses lucide `clock`.
- **Radio look:** the radio in the frames was drawn by hand (24 px circle); the code uses the radio group of the shared UI package, and the frames win only on size and spacing.
- **Open team decision:** E6-D3 (Frequency list, default and policies) is still marked open in the epic; D27-004 and D27-008 are "proposed". A change of the list changes the generated enum and the labels of this spec.
- **Demo:** the backend flag `app.scheduler.demo-enabled` and the frontend demo switch are separate. With the frontend switch on and the backend flag off, saving the demo option shows the 400 message of the backend in the popover.
- **Deferred point D1** of `views.md` (periodic Analyses) is closed by this spec once implemented.
