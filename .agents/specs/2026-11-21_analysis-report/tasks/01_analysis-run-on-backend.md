# 01 — Starting the Analysis on the real backend

**Spec:** [../spec.md](../spec.md) · **Blocked by:** nothing · **Status:** ready-for-agent

**What to build:** The Analyst fills in the New case form and clicks "Rozpocznij analizę". The app creates the Case and starts its Analysis on the backend, the full-screen loader stays while the app polls the Analysis every 1.5 seconds, and when the Analysis ends (`COMPLETED`, `PARTIAL` or `FAILED`) Case details opens at `/cases/{caseId}?analysis={analysisId}`. The same run hook can start an Analysis for an existing Case, so task 02 only places the "Rozpocznij nową analizę" button. The mock goes away.

## Decisions that apply

- Q24: the run code is cross-feature and lives in `lib/analysis-run/`, not in `features/`. New case and Case details import it.
- Run function: New case = `createCase`, then `startAnalysis`; existing Case = `startAnalysis` only. Both return `{ caseId, analysisId }`. Only the generated client is used.
- Seam S3: a pure function from an Analysis status (or none yet) to the next interval: 1500 ms for `RUNNING` and for no data yet, `false` (stop) for `COMPLETED`, `PARTIAL`, `FAILED`. Passed to TanStack Query `refetchInterval`. Task 02 reuses it for the running report (Q16).
- Run hook: mutation, then poll `getAnalysis` until the status ends, then navigate. A `FAILED` Analysis also navigates; Case details shows it.
- Loader: moved from `features/new-case/` unchanged (overlay, sentence "Trwa analiza. Nie zamykaj tej strony.", `beforeunload` warning).
- Q15: no time limit. A network error while polling closes the loader and shows a dialog with "Rozumiem"; after it, Case details opens with the `analysis` parameter. A failed start (including 409, one Analysis already running) shows `toUserMessage(error)` with "Coś poszło nie tak" and "Spróbuj ponownie"; 400 field errors go next to their fields, as today.
- Q21: no Case counter in the page shell header.

## Files

Paths relative to `frontend/apps/web/`.

| File | Role |
|---|---|
| `lib/analysis-run/run-analysis.ts` | Run functions: `runNewCaseAnalysis(identifiers)` and `runCaseAnalysis(caseId)`, both `Promise<{ caseId, analysisId }>`; built on `createCase` / `startAnalysis` from `lib/api/generated/case-controller/case-controller.ts` |
| `lib/analysis-run/poll-interval.ts` | S3 rule, pure, no React import |
| `lib/analysis-run/poll-interval.test.ts` | Vitest cases for S3 |
| `lib/analysis-run/use-analysis-run.ts` | Run hook: `useMutation` around a run function, `useGetAnalysis(analysisId, { query: { enabled, refetchInterval } })` from `lib/api/generated/analysis-controller/analysis-controller.ts`, `router.push` on end |
| `lib/analysis-run/analysis-loader.tsx` | Moved from `features/new-case/analysis-loader.tsx`, no change in behaviour |
| `lib/analysis-run/polling-error-dialog.tsx` | Dialog after a network error while polling, button "Rozumiem" (copy: see Notes) |
| `features/new-case/new-case-form.tsx` | Change: uses the run hook instead of its own mutation; keeps field-error mapping and the failed-start dialog |
| `features/new-case/start-analysis.ts` | Deleted |
| `features/new-case/analysis-loader.tsx` | Deleted (moved) |
| `components/page-shell.tsx` | Change: no counter is rendered today; remove the comment that promises one, so nothing hints at it |

Interface offered to Case details (proposal, the lead confirms): one hook per entry, or one hook with two `start` functions, giving `start(caseId)`, `isRunning` (loader open), `warnBeforeUnload`, `startError` (for the "Coś poszło nie tak" dialog) and `pollingFailed` (for the "Rozumiem" dialog). The hook renders nothing; the caller renders the loader and the dialogs.

## Before coding

- Run `./scripts/api-sync.sh` from the repo root (Docker) and confirm there is no drift in `openapi.json` and `lib/api/generated/`. Commit any drift separately first.
- Read the TanStack Query docs for `refetchInterval` (function form, what it receives in v5) and `useMutation` in the installed version: `@tanstack/react-query` 5.107.1 (from `frontend/package-lock.json`).
- Check the default `retry` of a query in that version: it delays when a polling error is seen. Decide with the lead whether polling uses `retry: false` or the default.
- Read the local Next.js docs for `useRouter` and confirm that `/cases/${caseId}?analysis=${analysisId}` passes `typedRoutes: true` (`next.config`) without a cast; the current form already pushes this string.
- Note that generated functions return the envelope `{ data, status, headers }`: the ids are `response.data.id`.

## Acceptance criteria

- [ ] `poll-interval.test.ts` passes: 1500 for `RUNNING`; 1500 for no data yet (`undefined`); `false` for `COMPLETED`, `PARTIAL`, `FAILED`.
- [ ] A valid submit sends `POST /api/1/cases`, then `POST /api/1/cases/{caseId}/analyses`, then `GET /api/1/analyses/{analysisId}` about every 1.5 s while the status is `RUNNING` (visible in the network panel).
- [ ] The loader stays until the status leaves `RUNNING`; there is no time limit.
- [ ] When the Analysis ends, the address becomes `/cases/{caseId}?analysis={analysisId}` with the real ids, without an extra click; polling stops.
- [ ] A network error while polling closes the loader and shows a dialog with "Rozumiem"; after it, Case details opens with the `analysis` parameter (check by stopping the backend while the loader is shown).
- [ ] A failed start shows "Coś poszło nie tak", the text of `toUserMessage(error)` and "Spróbuj ponownie"; the typed values stay. A 400 puts field errors next to their fields, errors without a field above the form.
- [ ] The run hook exposes a start for an existing `caseId` (interface above); a 409 on it goes to the failed-start path.
- [ ] Loader behaviour is unchanged: blocks mouse and keyboard, ignores Escape, `beforeunload` warning only while the Analysis runs.
- [ ] `features/new-case/start-analysis.ts` is gone; no hand-written `fetch`, no hand-written response types.
- [ ] The page shell header shows no Case counter.
- [ ] `npm run check` and `npm run build` pass from `frontend/`.
- [ ] Verified in the browser against `./scripts/run-backend.sh`: the whole path from `/` to the real report URL, the tab-close warning, a clean console.

## Notes

- Open question: the title and sentence of the polling-error dialog are not in the spec or `views.md` (only the button "Rozumiem" is). Ask the lead before writing the copy.
- Open question: is an `ApiError` while polling (e.g. 404, 500) treated like a network error (dialog "Rozumiem", then Case details) or like a failed start? The spec names only a network error.
- Open question: if `createCase` succeeds and `startAnalysis` fails, "Spróbuj ponownie" would create a second Case. Should the retry start an Analysis for the Case already created? The spec does not say.
- Open question: should `StartAnalysisErrorDialog` move from `features/new-case/new-case-dialogs.tsx` to `lib/analysis-run/`, so Case details (task 02) can reuse it without importing from another feature (ADR-0009)?
- Ids come from the backend; nothing is invented. The 409 cannot happen from New case (a fresh Case), so it is exercised only from Case details in task 02, or reported at the gate as read, not as run.
- Components, the hook, the loader and navigation are not tested by Vitest (ADR-0010); they are verified in the browser.

## Resolved by the lead (2026-11-21)

The open questions of this task were answered in Q28–Q32; see the table "Decisions after the task review" in [../spec.md](../spec.md). Those answers win over the open questions and proposals above.
