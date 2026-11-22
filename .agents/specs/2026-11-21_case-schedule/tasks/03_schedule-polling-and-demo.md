# 03 — Background polling and the demo switch

**Spec:** [../spec.md](../spec.md) · **Blocked by:** 02 (which is blocked by 01) · **Status:** implemented in #15 (PR feat/15-case-schedule)

**What to build:** While the Schedule of a Case is enabled, Case details checks the server in the background. A Scheduled Analysis appears in the select "Wyświetlana analiza" by itself, the hint "Liczba analiz: N" follows, and the next term and the last attempt in the state row and in the popover refresh. The report the Analyst is reading and the `analysis` parameter of the address never change. A frontend demo switch (an environment variable) lists the option "Co minutę (tryb demonstracyjny)" in the popover, so that the film can show a Scheduled Analysis within a minute; while the Schedule answers `demo: true`, the page checks the server every 5 seconds instead of every 30.

## Decisions that apply

- **Polling (Q7, Q11):** only while the Schedule is enabled. Every 30 000 ms; every 5 000 ms while the answer has `demo: true`. No polling when the Schedule is disabled or when there is no answer yet.
- **Schedule poll interval rule** (new, pure, seam 2 of the spec): from the Schedule answer (`ScheduleResponse | undefined`) to `number | false`. Same shape as `nextPollInterval` in `lib/analysis-run/poll-interval.ts`, with the two intervals as named constants.
  - Order of the checks: disabled or no answer → `false` first; then `demo: true` → 5 000; otherwise 30 000. The backend computes `demo` from the saved Frequency (`ScheduleService.toResponse`), so a disabled Schedule can still answer `demo: true`; it must not poll.
  - Difference from the Analysis run rule: there, "no data yet" keeps polling; here it stops. The first read of the Schedule is the normal fetch of the query, not a poll.
- **Where the rule is applied:** in the Case details view, to two queries: the Schedule query (`useGetSchedule`, added by task 01) and the history of Analyses (`useListAnalyses`). Both intervals come from the Schedule answer. The history query cannot read the interval from its own data, so the view computes it from the Schedule data and passes it down.
- **What polling must not do:**
  - It never changes the `analysis` parameter. Without the parameter the view shows the newest Analysis (`history[0]`); a new Scheduled Analysis would then replace the displayed report. This conflicts with Q7 and user story 27. See "Open questions" before coding this part.
  - It never shows the blocking loader. The loader belongs to the manual start (`lib/analysis-run/`); this task does not touch it. A running Scheduled Analysis is one more option in the select ("… · Zaplanowana · W toku").
  - It never shows the page skeleton again: the skeleton depends on `isPending`, which is true only before the first answer. Keep it that way; do not switch to `isFetching`.
- **After a save (spec, "Specific interactions"):** task 02 puts the answer of `PUT` into the cached Schedule. The polling must follow at once: start after enabling, change to 5 s after choosing the demo Frequency, stop after "Wyłączony". No extra code is expected for this; verify it in the browser.
- **Demo switch (Q5, P1 — proposal, the lead may change the name and the values):** optional public variable `NEXT_PUBLIC_DEMO_SCHEDULE`. `true` = on, absent = off. Validated in `lib/env.ts` in the same schema as `NEXT_PUBLIC_API_URL`, read as a literal `process.env.NEXT_PUBLIC_DEMO_SCHEDULE` reference, and exposed as a boolean on `env`. Documented in `.env.example`.
- **View model stays pure:** `toScheduleView(schedule, { demoEnabled })` gets the switch as its second argument. It never reads `env` or `process.env`. Only the Case details view imports `env` and passes the value on.
- **Demo option:** with the switch on, "Co minutę (tryb demonstracyjny)" is always listed, after the three normal Frequencies. With the switch off it is listed only when the server answers with cadence `DEMO_EVERY_MINUTE` (P5 — proposal, the lead may change it; already built by task 02 and must keep working).
- **Label "Tryb demonstracyjny"** in the state row comes from `demo: true` in the answer (task 01), not from the switch. This task does not change it.
- **Two separate flags (spec, "Further Notes"):** the frontend switch and the backend flag `app.scheduler.demo-enabled` are independent. With the switch on and the backend flag off, saving the demo option answers 400 with a Polish message; the popover stays open and shows it through `toUserMessage(error)` above the actions (Q10, error path of task 02). Verify it; fix it in this task only if it does not work.
- Rules: only the generated client (ADR-0007); `"use client"` stays in the feature view (ADR-0008); the new file lives in `features/case-details/` (ADR-0009); Vitest only for pure logic, no component or hook tests (ADR-0010).

## Files

Paths relative to `frontend/apps/web/`.

| File | Role |
|---|---|
| `features/case-details/schedule-poll-interval.ts` | New: the pure rule and its two interval constants |
| `features/case-details/schedule-poll-interval.test.ts` | New: Vitest for the rule |
| `features/case-details/schedule-view-model.ts` | Change: second argument `{ demoEnabled }`; the demo option listed when the switch is on |
| `features/case-details/schedule-view-model.test.ts` | Change: tests for the switch off and on, and for P5 |
| `features/case-details/case-details-view.tsx` | Change: `refetchInterval` on the Schedule query and on the history of Analyses; reads `env` and passes `demoEnabled` to `toScheduleView` |
| `lib/env.ts` | Change: `NEXT_PUBLIC_DEMO_SCHEDULE` in the schema and in the parsed object |
| `.env.example` | Change: the new variable with a comment |
| `../../turbo.json` (`frontend/turbo.json`) | Change: add the variable to `tasks.build.env` (see Notes) |

Generated hooks (from `lib/api/generated/case-controller/case-controller.ts`, verified): `useGetSchedule(caseId, options?)` and `useListAnalyses(caseId, options?)`. Query options go in `options.query`, as in `useGetAnalysis(analysisId, { query: { refetchInterval: … } })` in `features/analysis-report/report-view.tsx`. Bodies are in `data.data`. The Schedule type is `ScheduleResponse` from `@/lib/api/generated/model`; all its fields are optional (`enabled?`, `demo?`, `cadence?`), so the rule must treat a missing `enabled` as not enabled.

## Before coding

- Run `./scripts/api-sync.sh` to confirm no contract drift.
- Read tasks 01 and 02 and their merged code: where `useGetSchedule` is called, the real signature of `toScheduleView`, and how the popover shows a failed save.
- Read the docs of the installed TanStack Query (5.107.1 in `node_modules`) for `refetchInterval` as a number, as `false` and as a function, and for its behaviour in a background tab (`refetchIntervalInBackground`, `refetchOnWindowFocus`). Do not guess: `lib/analysis-run/use-analysis-run.ts` sets both options by hand, the report view sets neither.
- Read the local Next.js docs (16.4.6): `node_modules/next/dist/docs/01-app/02-guides/environment-variables.md`, for public environment variables. They are inlined only as literal `process.env.NEXT_PUBLIC_*` references; see the comment in `lib/env.ts`. Check also when a changed `.env.local` is picked up by `next dev`.
- Read the Zod docs of the installed version (4.x) for an optional variable that accepts only `true`.
- Read `app/providers.tsx`: the default `staleTime` is 60 s. Check in the docs how it works together with `refetchInterval`.

## Acceptance criteria

- [ ] Vitest, poll interval rule: no answer yet → `false`; disabled → `false`; enabled → 30 000; enabled with `demo: true` → 5 000.
- [ ] Vitest, poll interval rule: disabled with `demo: true` → `false`.
- [ ] Vitest, view model: with the switch off and a normal Frequency (or a disabled Schedule) the demo option is absent; with the switch on it is present and not selected.
- [ ] Vitest, view model: with the switch off and cadence `DEMO_EVERY_MINUTE` from the server the demo option is listed and selected (P5 still holds); the same with the switch on, listed once.
- [ ] Fixtures are typed with the generated model types (`ScheduleResponse`) and built by small factories in the test file.
- [ ] `toScheduleView` and the poll interval rule do not import `env` and do not read `process.env`.
- [ ] Without `NEXT_PUBLIC_DEMO_SCHEDULE` the app starts and builds as before and the demo option is not listed; a value other than `true` behaves as decided in "Open questions".
- [ ] `npm run check` and `npm run build` pass from `frontend/`.
- [ ] Verified in the browser against `./scripts/run-backend.sh` (profile `local`: the demo Frequency is allowed and terms are checked every 5 seconds) with `NEXT_PUBLIC_DEMO_SCHEDULE=true`:
  - [ ] the popover lists "Co minutę (tryb demonstracyjny)"; choose it and "Zapisz";
  - [ ] the state row appears with the label "Tryb demonstracyjny";
  - [ ] within about a minute, without a reload, a Scheduled Analysis ("Zaplanowana") appears in the select "Wyświetlana analiza" and "Liczba analiz: N" grows;
  - [ ] the displayed report and the address stay the same; no blocking loader and no page skeleton appear;
  - [ ] the next term in the state row moves forward by itself;
  - [ ] the network panel shows `GET …/schedule` and `GET …/analyses` about every 5 seconds;
  - [ ] choose "Wyłączony" and "Zapisz": the state row goes away and the network panel shows that both requests stop;
  - [ ] with a normal Frequency (for example "Codziennie o 06:00") the requests come about every 30 seconds;
  - [ ] the console is clean.
- [ ] Verified once with the switch off (variable removed, dev server restarted): the demo option is not listed for a disabled Schedule.
- [ ] Verified, if a backend without the demo flag can be started (not profile `local`): saving the demo option keeps the popover open with the backend's 400 message. If it cannot be started, say so in the report instead of skipping silently.

## Notes

- **Proposals this task relies on (the lead may change each before coding):** P1 (name and values of the demo switch), P5 (demo Frequency from the server with the switch off is still listed and selected). P6 is touched by the open question on a failed read below.
- **`turbo.json`:** `frontend/turbo.json` lists `NEXT_PUBLIC_API_URL` in `tasks.build.env`, and `lib/env.ts` already needs an `eslint-disable` for `turbo/no-undeclared-env-vars` on `NODE_ENV`. A new variable that is not listed will most likely fail lint, and Turborepo would reuse a cached build made with another value. Add the variable there instead of disabling the rule. This file is outside the list agreed for the three tasks; it is added here because the quality gate needs it.
- **Docker image:** `frontend/Dockerfile` passes only `NEXT_PUBLIC_API_URL` as a build argument, so an image is always built with the demo switch off. Not changed in this task; see "Open questions".
- **Error message of `lib/env.ts`:** it names only `NEXT_PUBLIC_API_URL`. If the new variable can fail validation, the message must name the variable that failed.
- **No new invalidation:** `report-view.tsx` already invalidates the history when the displayed Analysis ends. Polling adds to it; do not remove it.
- **Manual start during polling:** nothing new. A manual start during a Scheduled Analysis answers 409 and the existing start error dialog is shown (spec).
- **Open questions for the lead:**
  - **Newest Analysis without the `analysis` parameter.** Today the view shows `history[0]` when the address has no `analysis` parameter, which is the usual way to open a Case. When a Scheduled Analysis arrives, the displayed report would jump to it, against Q7 and user story 27. The spec does not say how to prevent it. Proposal: the view remembers the Analysis it showed first and keeps showing it until the Analyst chooses another option or the address changes; the address is not rewritten. Alternative: write the id into the address once (`router.replace`), which changes the address but only on opening. Needs a decision; this is the main gap of the slice.
  - **Background tab.** Q11 does not say whether polling goes on when the tab is hidden. Proposal: keep the library default and do not set `refetchIntervalInBackground` (confirm in the docs what the default does). For the film the tab is visible anyway.
  - **Failed read while polling.** `report-view.tsx` stops polling after an error. Here that would stop the polling for good after one failed request. Proposal: keep polling from the last good answer, so the page recovers by itself; P6 (popover with "Spróbuj ponownie") stays for a Schedule that was never read.
  - **Value other than `true`.** Proposal: `NEXT_PUBLIC_DEMO_SCHEDULE` accepts only `true`; absent or empty means off; any other value fails at start with a clear message, like the existing variable. Alternative: every other value means off.
  - **Docker build argument.** Should `frontend/Dockerfile` and `frontend/README.md` get `NEXT_PUBLIC_DEMO_SCHEDULE` as an optional build argument, for a demo run from the image? Proposal: not in this task.
  - **History interval in demo mode.** The rule gives one interval for both queries (5 s in demo mode). Is 5 s also right for the history of Analyses, or should only the Schedule be read that often? Proposal: one interval for both, as the spec says.
