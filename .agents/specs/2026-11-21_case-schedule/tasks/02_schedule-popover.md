# 02 — Clock button and the popover "Harmonogram"

**Spec:** [../spec.md](../spec.md) · **Blocked by:** 01 (uses its read of the Schedule, `toScheduleView`, `schedule-labels.ts` and the state row) · **Status:** ready-for-agent

**What to build:** On Case details the Analyst sees a square clock button right of "Rozpocznij nową analizę". It opens the popover "Harmonogram" under the button. There the Analyst picks "Wyłączony" or a Frequency and confirms with "Zapisz". After a successful save the popover closes and the state row of task 01 shows the new state. The popover also shows the next term and the last attempt. Background polling and the demo switch are task 03.

## Decisions that apply

- Clock button: square, outline, right of "Rozpocznij nową analizę" in the Case header (Q3, Q14). It has no visible text, so the name "Harmonogram" is its `aria-label` and its tooltip. When the Schedule is enabled the button has a tinted background and a check badge, so the state is not carried by color alone.
- No clock button on a Case without any Analysis (Q13). The Case header already hides its actions there (`showActions={false}`); the clock button goes into the same block.
- Popover: anchored under the clock button, right-aligned to it, page not dimmed (Q3, Q14). It may cover the right part of the state row and of the select (Q16).
- Popover content, top to bottom, copy exactly as here:
  - title "Harmonogram";
  - sentence "Analiza tej sprawy uruchomi się sama w wybranych terminach.";
  - one radio group "Częstotliwość" with "Wyłączony", "Codziennie o 06:00", "Co 6 godzin (00:00, 06:00, 12:00, 18:00)", "Co tydzień (poniedziałek, 06:00)" (Q4). No separate on/off switch;
  - hint "Godziny według czasu polskiego." (Q12);
  - "Następna analiza: {date}", only when the Schedule has a next term;
  - the last attempt line (see below);
  - actions "Zapisz" and "Anuluj".
- The option in force is selected when the popover opens. A Schedule that is not enabled selects "Wyłączony", whatever `cadence` the server returns (see Notes, "Disabled Schedule keeps its cadence").
- Last attempt line (Q6):
  - `STARTED`: "Ostatnie uruchomienie: {date}" and the link "Pokaż analizę" to `/cases/{caseId}?analysis={analysisId}`. The link closes the popover. Only this outcome has an Analysis id.
  - `SKIPPED_OVERLAP`, `SKIPPED_DOWNTIME`, `FAILED`: the same message as the state row of task 01 (texts of P3, **proposal, the lead may change it**). Take the text from `schedule-labels.ts` of task 01; do not write it a second time.
  - No attempt yet: no line (P8, **proposal, the lead may change it**).
- Demo option: not in this task, with one exception. When the server answers with an enabled Schedule on `DEMO_EVERY_MINUTE`, the option "Co minutę (tryb demonstracyjny)" is listed and selected, so the group always shows what is in force (P5, **proposal, the lead may change it**).
- Options → request: "Wyłączony" saves `enabled: false`; every other option saves `enabled: true` with its `cadence`.
- Save (Q10): `PUT` through the generated mutation. On success the answer replaces the cached Schedule (no second read) and the popover closes; no "Zapisano" message. On an error the popover stays open, the choice is kept, and `toUserMessage(error)` is shown above the actions. Never `error.message`.
- While the save is in flight "Zapisz" shows a pending state and a second click is ignored (P7, **proposal, the lead may change it**).
- "Anuluj", Escape and a click outside close the popover without saving. The unsaved choice is dropped without a question (Q17): the next opening shows the option in force again.
- Focus returns to the clock button when the popover closes, in every way of closing. Everything works by keyboard (WCAG 2.1 AA).
- The Schedule cannot be read: the clock button stays, and the popover shows `toUserMessage(error)` with "Spróbuj ponownie" in place of the options (P6, **proposal, the lead may change it**). The rest of Case details keeps working.
- Dates use the formatter of task 01 (`Intl` `pl-PL` in the zone of `ScheduleResponse.timezone`, "22.11.2026, 06:00"). Do not write a second formatter.
- Components do not read API types. They render what `toScheduleView` returns (ADR-0009, spec "Modules"). `"use client"` stays in the feature files (ADR-0008).
- Visual source, read only through the Pencil MCP (the `.pen` file is encrypted): "[STAN] Szczegóły sprawy – harmonogram, popover otwarty", "[STAN] Szczegóły sprawy – harmonogram wyłączony", "[STAN] Szczegóły sprawy – harmonogram włączony". The frames win on layout, sizes and copy.

## Files

Paths relative to `frontend/apps/web/`.

| File | Role |
|---|---|
| `features/case-details/schedule-view-model.ts` | Change (created by 01): extend `toScheduleView(schedule)` with the list of options (value, label, selected), the clock button look (enabled or not), and the popover lines (next term; last attempt line with the optional Analysis id). Add `toUpdateScheduleRequest(optionValue)`. Keep the signature open for task 03's second argument (the demo switch); do not add it here |
| `features/case-details/schedule-view-model.test.ts` | Change (created by 01): Vitest cases of this slice, see Acceptance criteria |
| `features/case-details/schedule-labels.ts` | Change (created by 01): add the option labels, including "Co minutę (tryb demonstracyjny)" |
| `features/case-details/schedule-button.tsx` | New, client: clock button with tooltip and `aria-label` "Harmonogram", enabled look, owns the open state of the popover |
| `features/case-details/schedule-popover.tsx` | New, client: popover content, the chosen option, save with its pending and error states, the read error state |
| `features/case-details/case-header.tsx` | Change: render the clock button right of `StartAnalysisButton`, inside the `showActions` block |
| `features/case-details/case-details-view.tsx` | Change only if needed to hand the Schedule read of task 01 to the Case header (see Notes, "Where the Schedule is read") |

Generated code (from `lib/api/generated/case-controller/case-controller.ts`, checked in the repo on 2026-11-21): `useGetSchedule(caseId)`, `useUpdateSchedule()` with variables `{ caseId, data }` where `data` is `UpdateScheduleRequest`, and `getGetScheduleQueryKey(caseId)`. Bodies are in `data.data`. Model types from `lib/api/generated/model`: `ScheduleResponse`, `ScheduleAttemptResponse`, `UpdateScheduleRequest`, and the `…Cadence` / `…Outcome` enums. All fields of `ScheduleResponse` are optional in the generated type; handle `undefined`.

Shared UI used (`frontend/packages/ui/src/components/`, all wrap Base UI `@base-ui/react`, not Radix): `popover.tsx` (`Popover`, `PopoverTrigger`, `PopoverContent`, `PopoverHeader`, `PopoverTitle`, `PopoverDescription`), `radio-group.tsx` (`RadioGroup`, `RadioGroupItem`), `tooltip.tsx` (`Tooltip`, `TooltipTrigger`, `TooltipContent`, `TooltipProvider`), `button.tsx`. No new UI primitive is written and `packages/ui` is not changed.

## Before coding

- Run `./scripts/api-sync.sh` to confirm no contract drift.
- Read the docs of the installed Base UI version (`@base-ui/react`, `^1.9.0` in `packages/ui/package.json`; check the installed one) for:
  - Popover: controlled open state, closing on Escape and on a click outside, where focus goes on close, how to close it from a link inside;
  - Radio and Radio Group: controlled value, the type of the value given to the change handler, how a visible label is tied to each radio and to the group, arrow-key behaviour;
  - Tooltip: whether a provider is required (none is mounted in `apps/web` today), and how to put a tooltip and a popover on the same button with `render`.
- Read the docs of the installed TanStack Query version (`@tanstack/react-query`, `^5.107.1`) for mutations (pending state, error, reset of the error) and for updating the cache from a mutation answer. Check that the cached value of `useGetSchedule` and the answer of `useUpdateSchedule` have the same envelope shape before writing one into the other.
- Check in the backend (`backend/src/main/kotlin/squad/nononotaa/backend/service/ScheduleService.kt`, `update`) that `PUT` with `enabled: false` and no `cadence` is accepted, and what `cadence` and `demo` the answer has for a disabled Schedule.
- Check that the icon `clock` exists in the installed `lucide-react` and under which export name. The Pencil frames use `clock-4` only because the Pencil icon set has no lucide `clock`.
- Read `popover.tsx`, `radio-group.tsx` and `button.tsx` for their defaults: `PopoverContent` centers by default and has a fixed width and padding; the radio is smaller than the 24 px circle of the frames; `button.tsx` has no square size as high as size `xl` of "Rozpocznij nową analizę". Set these with props and `className` in the feature files.

## Acceptance criteria

- [ ] `toScheduleView` tests, selected option: a disabled Schedule selects "Wyłączony"; `DAILY`, `EVERY_6_HOURS` and `WEEKLY` each select their option; exactly one option is selected in every case.
- [ ] Tests, option list: without the demo Frequency the list has "Wyłączony" and the three Frequencies, with the labels of the spec, in this order; an enabled Schedule on `DEMO_EVERY_MINUTE` adds "Co minutę (tryb demonstracyjny)" and selects it (P5).
- [ ] Tests, clock button look: plain for a disabled Schedule, enabled for an enabled one.
- [ ] Tests, popover lines per outcome: `STARTED` gives "Ostatnie uruchomienie: {date}" with the Analysis id; `SKIPPED_OVERLAP`, `SKIPPED_DOWNTIME` and `FAILED` give the message of the state row and no Analysis id.
- [ ] Tests: no last attempt gives no last attempt line (P8); no next term gives no "Następna analiza" line; a next term gives "Następna analiza: {date}" in Polish time.
- [ ] `toUpdateScheduleRequest` tests: "Wyłączony" gives `enabled: false`; each of the three Frequencies and the demo option gives `enabled: true` with its `cadence`.
- [ ] Fixtures are typed with the generated model types and built by small factories in the test file. No test looks at components, hooks or markup (ADR-0010).
- [ ] The clock button is right of "Rozpocznij nową analizę", has the tooltip and the accessible name "Harmonogram", and is absent on a Case without any Analysis.
- [ ] The enabled look (tinted background and check badge) appears after enabling and goes away after disabling.
- [ ] The popover opens under the button, right-aligned, without dimming the page, with the copy of this task and the option in force selected.
- [ ] "Zapisz" saves; the popover closes; the state row of task 01 follows without a reload and without a second `GET` of the Schedule; there is no "Zapisano" message.
- [ ] A failed save keeps the popover open with the chosen option and shows `toUserMessage(error)` above the actions; a second try works.
- [ ] While the save is in flight "Zapisz" shows the pending state and a second click sends nothing (P7).
- [ ] "Anuluj", Escape and a click outside close the popover without a request; the next opening shows the option in force, not the dropped choice.
- [ ] "Pokaż analizę" opens `/cases/{caseId}?analysis={analysisId}` and closes the popover.
- [ ] A failed read of the Schedule shows the message and "Spróbuj ponownie" in the popover in place of the options; the report below still works (P6).
- [ ] Keyboard: Tab reaches the clock button; Enter and Space open the popover; the options are changed with the arrow keys; "Zapisz" and "Anuluj" are reachable and work; Escape closes. Focus is visible everywhere and returns to the clock button after every way of closing.
- [ ] The radio group has the visible label "Częstotliwość" and every option has a visible label tied to its radio.
- [ ] `npm run check` and `npm run build` pass from `frontend/`.
- [ ] Verified in the browser against `./scripts/run-backend.sh`, with a clean console: enable "Codziennie o 06:00" and see the state row of task 01 change; reload and see the state kept; open the popover and see the option selected and "Następna analiza"; disable and see the row gone; stop the backend, press "Zapisz" and see the message in the open popover.

## Notes

- **Dependency on 01:** this task extends the files task 01 creates. If 01 is not merged, wait; do not create a second view model or a second label file.
- **Room for 03:** task 03 adds the demo switch as the second argument of `toScheduleView` (the demo option always listed) and the polling. Build the option list so that one more condition can add the demo option; do not read any environment variable here.
- **Disabled Schedule keeps its cadence (found in the backend, 2026-11-21):** `ScheduleService.update` with `enabled: false` and no `cadence` keeps the stored `cadence` (or `DAILY` for a new record), and the answer returns it, with `demo: true` when it is the demo one. A Schedule never saved answers `cadence: null`. So the selected option must come from `enabled` first and from `cadence` second. For P5 this task reads "the server answers with the demo Frequency" as "enabled and on the demo Frequency": a disabled Schedule that once ran on the demo Frequency does not list the demo option. **Proposal, the lead may change it.**
- **Where the Schedule is read (proposal, the lead may change it):** task 01 reads the Schedule in `case-details-view.tsx`. The popover needs the answer, the read error and a retry. Either the view passes them to the Case header as props, or `schedule-button.tsx` calls `useGetSchedule(caseId)` itself. Proposal: pass them as props, so task 03 puts the polling in one place. Agree it with the result of task 01 before coding.
- **Message of a skip or a failure in the popover:** P3 ends every warning of the state row with "Następna analiza: {date}." The popover has its own "Następna analiza" line, so the proposal is to show the warning sentence without that ending. **Proposal, the lead may change it;** it needs the sentence and the ending to be separate in `schedule-labels.ts` of task 01.
- **Option values:** the view model gives each option a value that the components pass back to `toUpdateScheduleRequest`. Proposal: one value for "Wyłączony" plus the cadence codes of the generated enum. The components treat the value as opaque.
- **Icon:** the frames show `clock-4`; the code uses lucide `clock`.
- **Radio look:** the radio in the frames was drawn by hand; the code uses `RadioGroup` of the shared UI package, and the frames win only on size and spacing.
- **Demo option and the backend flag:** saving the demo option with the backend demo mode off answers 400 with a Polish message. It is shown like every other save error; nothing special is built.
- **Open questions for the lead:**
  - "Zapisz" with the option in force still selected: send the `PUT` anyway (the backend then keeps the next term), or only close the popover? Proposal: send it, one path.
  - Should a screen reader hear that the Schedule is enabled from the clock button (the check badge is visual)? The spec asks only for the name "Harmonogram". Proposal: keep the name, and rely on the state row of task 01 for the state.
  - The save error has no title in the spec. Proposal: only the sentence of `toUserMessage(error)` with `role="alert"`, without "Coś poszło nie tak", because the popover is small. The read error (P6) is the same sentence plus "Spróbuj ponownie".
