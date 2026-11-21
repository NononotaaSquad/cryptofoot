# 01 — Read the Schedule and show the state row

**Spec:** [../spec.md](../spec.md) · **Blocked by:** none · **Status:** ready-for-agent

**What to build:** The Analyst opens `/cases/{caseId}` of a Case with an enabled Schedule and sees, between the Case header and the select "Wyświetlana analiza", a full-width state row with the Frequency and the next term, for example "Harmonogram: codziennie o 06:00 · następna analiza: 22.11.2026, 06:00". When the last attempt of the Schedule was `FAILED`, `SKIPPED_OVERLAP` or `SKIPPED_DOWNTIME`, the same row is a warning that says so. A Schedule on the demo Frequency carries the label "Tryb demonstracyjny". A disabled Schedule shows no row, and a Schedule that cannot be read shows no row and does not break the rest of the view. This task only reads: no clock button, no popover, no saving, no polling (tasks 02 and 03).

## Decisions that apply

- The Schedule is Case scope and is read and rendered by the Case details view (ADR-0009). `"use client"` stays in the feature view; `page.tsx` is not changed (ADR-0008).
- The Schedule is read with the generated hook `useGetSchedule(caseId)` from `lib/api/generated/case-controller/case-controller` (`GET /api/1/cases/{caseId}/schedule`). The body is in `data.data` and has the type `ScheduleResponse`. No hand-written `fetch` or response type.
- `toScheduleView(schedule)` in `schedule-view-model.ts` is the one public entry of the Schedule view model (spec "Seam 1"). In this task it returns only the state row part. **The return type must be an object with a `stateRow` field (not the bare state row), because task 02 adds more fields (options, clock button look, popover lines) to the same object and task 03 adds a second argument (the demo switch).** Helper functions are not exported.
- The state row view model has: a kind (`none`, `info` or `warning`), the text, and whether the "Tryb demonstracyjny" label is shown. Components do not read API types; `schedule-state-row.tsx` gets only this view model.
- State row rules:
  - Schedule disabled → kind `none`, also when its last attempt was a skip or a failure (**P4, proposal the lead may change**).
  - Schedule enabled and the last attempt is absent or `STARTED` → kind `info`, text "Harmonogram: {Frequency text} · następna analiza: {date}" (user story 15). A `STARTED` attempt whose Analysis later failed still gives `info`; the failed Analysis is visible in the select as "Nieudana" (spec "Specific interactions").
  - Schedule enabled and the last attempt is `FAILED`, `SKIPPED_OVERLAP` or `SKIPPED_DOWNTIME` → kind `warning`; the warning replaces the plain row in the same place (Q6, Q15), and keeps the next term (user story 23).
- Frequency texts (**P2, proposal the lead may change**): `DAILY` "codziennie o 06:00", `EVERY_6_HOURS` "co 6 godzin", `WEEKLY` "co tydzień w poniedziałek o 06:00", `DEMO_EVERY_MINUTE` "co minutę".
- Warning texts, each followed by "Następna analiza: {date}." (**P3, proposal the lead may change**); `{date}` in the first sentence is `lastAttempt.attemptedAt`:
  - `FAILED`: "Nie udało się uruchomić zaplanowanej analizy {date}."
  - `SKIPPED_OVERLAP`: "Pominięto zaplanowaną analizę {date}, ponieważ inna analiza tej sprawy była w toku."
  - `SKIPPED_DOWNTIME`: "Pominięto zaplanowaną analizę {date}, ponieważ aplikacja wtedy nie działała."
- "Tryb demonstracyjny" is shown when the answer has `demo: true` (Q5). It follows the `demo` field of the answer, not the Frequency code and not the frontend demo switch (the switch is task 03).
- Dates are formatted with `Intl.DateTimeFormat` `pl-PL` in the zone of `ScheduleResponse.timezone`, not in the zone of the browser (Q12, spec "Time zone"). Format as in the report: "22.11.2026, 06:00".
- Look of the row (Q9, Q15, Pencil frames "… – harmonogram włączony" and "… – harmonogram, nieudane uruchomienie"; the frames win on layout, sizes and copy): full width, 4 px left border, icon, text. The warning has a warning tint, a warning icon and bold text. The state is carried by the icon and the words, never by color alone (user story 25).
- When the Schedule cannot be read, there is no state row and the Case header, the select and the report work as before (**P6, proposal the lead may change**; only its "no state row" part belongs to this task, the popover part is task 02). The Schedule query must not be added to the loading or error condition of the Case details view.
- Texts live in `schedule-labels.ts` as code → Polish text maps, like `features/analysis-report/report-labels.ts`.

## Files

Paths relative to `frontend/apps/web/`.

| File | Role |
|---|---|
| `features/case-details/schedule-labels.ts` | New. Code → Polish maps: Frequency texts (P2), outcome warning texts (P3), "Tryb demonstracyjny", the "Harmonogram: …" and "Następna analiza: …" templates |
| `features/case-details/schedule-view-model.ts` | New, pure. `toScheduleView(schedule)` returns an object with `stateRow` (kind, text, demo label); formats dates in the zone of the answer |
| `features/case-details/schedule-view-model.test.ts` | New. Vitest for `toScheduleView` (state row cases of spec "Seam 1") |
| `features/case-details/schedule-state-row.tsx` | New. Renders the state row view model; renders nothing for kind `none` |
| `features/case-details/case-details-view.tsx` | Change: calls `useGetSchedule(caseId)`, maps the answer with `toScheduleView`, renders `ScheduleStateRow` between `CaseHeader` and `AnalysisSelect` |

Generated hook (from `lib/api/generated/case-controller/case-controller.ts`): `useGetSchedule`. Generated types (from `lib/api/generated/model`): `ScheduleResponse`, `ScheduleAttemptResponse`, `ScheduleResponseCadence`, `ScheduleAttemptResponseOutcome`. Bodies are in `data.data`.

Not changed by this task: `case-header.tsx` (the clock button is task 02), `lib/env.ts` (task 03), `packages/ui`.

## Before coding

- Run `./scripts/api-sync.sh` to confirm no contract drift.
- Read `ScheduleResponse` and `ScheduleAttemptResponse` in `lib/api/openapi.json` and the generated model files. Every field is optional in the generated types, `cadence`, `nextExecutionAt`, `lastAttempt` and `analysisId` can be `null`; the view model must handle each of them without `!` and without `as`.
- Read `features/analysis-report/report-view-model.ts`, `report-view-model.test.ts` and `report-labels.ts` as prior art for a pure view model, its factories and label maps. Note that `formatDateTime` there has the zone `Europe/Warsaw` fixed in code, so it cannot be used as it is for the zone of the answer (see Notes).
- Read the docs of the installed TanStack Query version for what a failed query returns while and after its retries, so that a failed Schedule read gives no state row and no error screen.
- Read the MDN docs of `Intl.DateTimeFormat` for the `timeZone` option and for what happens with an unknown zone name.
- Open the Pencil frames named in the spec through the Pencil MCP (the file is encrypted) for the sizes, tints and icons of both rows. Check which components of `packages/ui` (for example `alert.tsx`) match the frame before writing markup; read the component file, do not assume its props.
- Read `.e2e/backend/bruno/07-schedule/` for the `PUT` bodies used in the browser verification.

## Acceptance criteria

- [ ] `toScheduleView` tests: a disabled Schedule gives kind `none`; a disabled Schedule with a `FAILED` last attempt also gives kind `none` (P4).
- [ ] Tests: for each Frequency (`DAILY`, `EVERY_6_HOURS`, `WEEKLY`, `DEMO_EVERY_MINUTE`) an enabled Schedule gives kind `info` with the text "Harmonogram: {Frequency text} · następna analiza: {date}".
- [ ] Tests: `demo: true` sets the "Tryb demonstracyjny" label; `demo: false` does not.
- [ ] Tests: each outcome of the last attempt: `STARTED` gives kind `info`; `FAILED`, `SKIPPED_OVERLAP` and `SKIPPED_DOWNTIME` each give kind `warning` with their own sentence, the date of the attempt and "Następna analiza: {date}.".
- [ ] Tests: no last attempt (`null` and absent) gives kind `info`.
- [ ] Tests: no next term (`nextExecutionAt: null`) on an enabled Schedule, for the plain row and for a warning: the text has no next term part and no empty or "Invalid Date" text (wording: see Notes).
- [ ] Tests: a date near midnight UTC is shown as the Polish date and hour, for example `2026-11-21T23:30:00Z` with `timezone: "Europe/Warsaw"` gives "22.11.2026, 00:30". The test passes whatever the zone of the machine that runs it.
- [ ] Fixtures are typed with the generated model types and built by small factories in the test file. No test looks at components, hooks or markup (ADR-0010).
- [ ] `toScheduleView` returns an object with the state row under its own field, so that task 02 can add fields without changing the callers of this task.
- [ ] On Case details the state row is between the Case header and the select "Wyświetlana analiza", full width; nothing is rendered there for a disabled Schedule, and the spacing of the view is the same as before.
- [ ] When `GET …/schedule` fails, there is no state row and the header, the select and the report still work; while the Schedule is loading the rest of the view is not held back.
- [ ] Everything on the page still works by keyboard; the row adds no focus stop of its own; the warning is carried by an icon and words, not by color alone; the icon is hidden from screen readers and the text is readable by them.
- [ ] `npm run check` and `npm run build` pass from `frontend/`.
- [ ] Verified in the browser against `./scripts/run-backend.sh`, with the Schedule set by `PUT /api/1/cases/{caseId}/schedule` (Bruno folder `.e2e/backend/bruno/07-schedule/`, the Postman collection, or curl), because the UI cannot save it before task 02:
  - disabled (default): no row;
  - `{"enabled": true}` (daily): the plain row with the next term at 06:00 Polish time, after a reload;
  - `{"enabled": true, "cadence": "WEEKLY"}`: the weekly text;
  - `{"enabled": true, "cadence": "DEMO_EVERY_MINUTE"}`: "co minutę" and the label "Tryb demonstracyjny";
  - one warning row, for example `SKIPPED_OVERLAP`: with the demo Frequency on, start a manual Analysis and reload after the next minute;
  - the backend stopped or the request blocked: no row, the rest of the page works;
  - a clean console in every state. Report which states were seen and which could not be produced.

## Notes

- **No refresh in this task:** the row shows what the server said at the last read; it changes after a reload. Task 03 adds the background polling. Do not add `refetchInterval` here.
- **Slots for 02 and 03:** task 02 adds the clock button to `case-header.tsx`, extends `toScheduleView` with more fields, and writes the answer of `PUT` into the cache of the same query; keep the query on the generated query key (no custom `queryKey`) so that this works. Task 02 also shows the warning sentence in the popover without the "Następna analiza: {date}." ending, so keep the warning sentence and that ending as separate entries in `schedule-labels.ts`. Task 03 adds the poll interval rule and the demo switch as the second argument of `toScheduleView`. Do not start any of them here.
- **Empty Case:** the state row is rendered only in the data state of the view (a Case with at least one Analysis), where the select exists. See the open question below.
- **Proposals of this task, to be reviewed by the lead (not in the spec):**
  - No next term on an enabled Schedule: the plain row is "Harmonogram: {Frequency text}" without the "· następna analiza: …" part; a warning ends after its first sentence.
  - `timezone` absent in the answer (the generated type allows it): use `Europe/Warsaw`, the zone of D27-002 and of the report.
  - `enabled: true` with `cadence` null or absent (should not happen): kind `none`.
  - Date formatting: write a small formatter with a zone argument inside `schedule-view-model.ts`; do not change `report-labels.ts` in this task.
- **Open questions for the lead:**
  - `SKIPPED_DOWNTIME` wording: by D27-006 the attempt is recorded when the application is running again, and several missed terms give one record. So `attemptedAt` is the moment the skip was noticed, not the missed term, and "… {date}, ponieważ aplikacja wtedy nie działała." may name a moment at which the application was running. Keep P3, or change the sentence (for example without the date)?
  - A Case without any Analysis but with a Schedule enabled through the API: Q13 removes the clock button there; the spec does not say whether the state row is shown. This task does not show it.
  - Where exactly does "Tryb demonstracyjny" sit in the row, and does it also appear on a warning row? This task follows the Pencil frame if it shows it; otherwise a badge at the end of the row, on both kinds.
  - Should the warning row be announced to screen readers when it appears (a live region)? Without polling it appears only on page load, so this task adds none; task 03 may need it.
