# 02 — Case details skeleton and the report summary

**Spec:** [../spec.md](../spec.md) · **Blocked by:** none (runs in parallel with 01; consumes its run hook and poll interval rule, see Notes) · **Status:** ready-for-agent

**What to build:** The Analyst opens `/cases/{caseId}` (optionally `?analysis={analysisId}`) and sees the Case header, the select "Wyświetlana analiza" and the summary of the report of the selected Analysis: heading with the date, status, start and end, checked sources, networks and Report Limitations. A failed, partial or running Analysis is shown as such. The Attributions, Excluded and Rejected sections are left as an empty slot for task 03.

## Decisions that apply

- Thin `page.tsx` (Server Component) passes `caseId` and the optional `analysis` param to the Case details view; `"use client"` lives in the feature view (ADR-0008). Only `features/case-details/` imports from `features/analysis-report/` (ADR-0009).
- Case details view renders four states: loading, error, empty (Case without Analyses), data. Every failed call shows "Coś poszło nie tak", `toUserMessage(error)` and "Spróbuj ponownie".
- Case header: label = full name, or without one the first narrowing identifier; the first two identifiers with their kind ("Nick: krypto_janek") and "+N więcej", which expands the rest. No Case Number (Q3, issue #56). Header link "← Lista spraw" without the counter (Q21).
- Reviewed: "Oznacz jako przejrzaną" (outline button) or a check icon with "Sprawa przejrzana" and the text action "Cofnij oznaczenie". Stored in `localStorage`, one key per Case id, through one small module; no backend call (Q4).
- Select "Wyświetlana analiza": one option per Analysis ("21.11.2026, 14:32 · Ręczna · Zakończona"), hint "Wybierz wcześniejszą analizę, aby zobaczyć jej raport. Liczba analiz: N." The selection is the `?analysis=` param; without it the newest Analysis is shown.
- `toReportView(report, mentions, caseIdentifiers)` is the one public entry (seam S1). This task implements only the summary part and the top-level state (running, failed, partial, completed, no Attributions); section functions are not exported.
- Labels: status `RUNNING` "W toku", `COMPLETED` "Zakończona", `PARTIAL` "Częściowa", `FAILED` "Nieudana"; trigger `MANUAL` "Ręczna", `SCHEDULED` "Zaplanowana". An unknown Report Limitation code gives a generic sentence with the code.
- Summary: "Raport analizy z {date}", status badge (icon + word, not color alone), start and end, "Sprawdzone źródła (N z M)" with "· sprawdzono o HH:MM" per source, "Dane testowe" when `synthetic: true`, "· niedostępne" as a warning when `status: UNAVAILABLE`, "Sieci", Report Limitations as a warning list.
- `PARTIAL`: full report plus a warning in the summary. `FAILED`: summary plus a grey block "Analiza nie powiodła się" with the Report Limitations in place of the Attributions. `RUNNING`: badge "W toku" and "Analiza jeszcze trwa"; the report polls in the background with the poll interval rule and refetches when the Analysis ends, without blocking (Q16).

## Files

Paths relative to `frontend/apps/web/`.

| File | Role |
|---|---|
| `app/cases/[caseId]/page.tsx` | Change: thin Server Component, awaits `params` and `searchParams`, renders `CaseDetailsView` |
| `features/case-details/case-details-view.tsx` | Client view: `useGetCase`, `useListAnalyses`; four states; resolves the selected Analysis; renders header, select and `ReportView` |
| `features/case-details/case-header.tsx` | Label, identifiers with "+N więcej", Reviewed control, "Rozpocznij nową analizę" |
| `features/case-details/reviewed-store.ts` | `localStorage` read/write per Case id |
| `features/case-details/analysis-select.tsx` | Select "Wyświetlana analiza" with hint; writes `?analysis=` |
| `features/analysis-report/report-labels.ts` | Code → Polish maps: status, trigger, Report Limitation sentences, source labels |
| `features/analysis-report/report-view-model.ts` | `toReportView`; summary part and top-level state only |
| `features/analysis-report/report-view-model.test.ts` | Vitest for `toReportView` (summary and state) |
| `features/analysis-report/report-view.tsx` | Client: `useGetReport` + `useListMentions`, background polling while `RUNNING`, summary + slot for task 03 |
| `features/analysis-report/report-summary.tsx` | Renders the summary view model |

Generated hooks (from `lib/api/generated/`): `useGetCase`, `useListAnalyses` (case-controller), `useGetReport`, `useListMentions`, `useGetAnalysis` (analysis-controller). Bodies are in `data.data`.

## Before coding

- Run `./scripts/api-sync.sh` to confirm no contract drift.
- Read the local Next.js docs for async `params`/`searchParams` and for updating a search param from a Client Component (`useRouter` / `useSearchParams`) with typed routes.
- Read the TanStack Query 5.107.1 docs for `refetchInterval` as a function and for invalidating the report query when the status changes.
- Read the Base UI 1.9.0 Select docs (`onValueChange` may receive `null`).
- Check the order of `listAnalyses` in the backend; do not assume newest first, sort by `startedAt` if needed.

## Acceptance criteria

- [ ] `toReportView` tests: status labels for all four codes; trigger labels for both codes.
- [ ] Tests: source lines for `synthetic: true`, `status: UNAVAILABLE` and `checkedAt: null`; "N z M" counts checked sources against all sources.
- [ ] Tests: one sentence per known Report Limitation code; an unknown code gives the generic sentence with the code.
- [ ] Tests: top-level state for `RUNNING`, `FAILED`, `PARTIAL`, `COMPLETED` with Attributions and `COMPLETED` without Attributions.
- [ ] Fixtures are typed with the generated model types and built by small factories in the test file.
- [ ] Without `?analysis=` the newest Analysis is shown; choosing another option changes the address and the report; reloading keeps it.
- [ ] The header shows the label, two identifiers with their kind and "+N więcej" that expands the rest; no Case Number; no counter.
- [ ] "Oznacz jako przejrzaną" switches to "Sprawa przejrzana" + "Cofnij oznaczenie"; the state survives a reload and is per Case.
- [ ] A `FAILED` Analysis shows the summary and the grey block; `PARTIAL` shows the warning; `RUNNING` shows "Analiza jeszcze trwa" and refreshes by itself when it ends.
- [ ] Loading and error states are visible; an unknown `caseId` shows the error state with "Spróbuj ponownie".
- [ ] Everything works by keyboard; the status is not carried by color alone.
- [ ] `npm run check` and `npm run build` pass from `frontend/`.
- [ ] Verified in the browser against `./scripts/run-backend.sh` with the demo data: a completed report, the select with two Analyses, the Reviewed toggle across a reload, a clean console.

## Notes

- **Dependency on 01:** "Rozpocznij nową analizę" and the background polling use task 01's run hook and poll interval rule in `lib/analysis-run/`. If 01 is not merged, build everything else first, wire the button last, and for polling import the rule from 01's branch once merged; do not copy it into this feature.
- **Slot for 03:** `report-view.tsx` leaves one marked place below the summary where task 03 adds the Attributions, Excluded and Rejected sections. The "no Attributions" empty state ("Powiązania (0)") is rendered by task 03; this task only exposes the state.
- **Report Limitation sentences (proposals, to be reviewed by the lead):**
  - `SOURCE_UNAVAILABLE`: "Co najmniej jedno źródło było niedostępne, więc raport może nie zawierać wszystkich wzmianek."
  - `BLOCKCHAIN_DATA_UNAVAILABLE`: "Nie udało się pobrać danych o aktywności adresów w sieci, więc ta część raportu może być pusta lub niepełna."
  - `CLASSIFICATION_UNAVAILABLE`: "Nie udało się sprawdzić, czy adresy należą do serwisów (np. giełd), więc część powiązań może dotyczyć takich adresów."
  - `ANALYSIS_INCOMPLETE`: "Analiza nie została ukończona, więc raport może być niepełny."
  - Unknown code: "Raport może być niepełny (kod: {code})."
- **Open questions for the lead:**
  - Identifier kind labels ("Nick", "Adres e-mail", …) live in `features/new-case/identifier-fields.ts`; reusing them across features means moving them to `lib/`. Move, or duplicate in `case-details`?
  - Which order defines "first narrowing identifier" for the label? Proposal: the New case form order.
  - Empty state when a Case has no Analysis yet: wording is not decided.
  - An `?analysis=` id that is not in the Case history: show the error state or fall back to the newest?
  - Does `PARTIAL` get its own warning sentence besides the Report Limitations, and what is it?

## Resolved by the lead (2026-11-21)

The open questions of this task were answered in Q33–Q37; see the table "Decisions after the task review" in [../spec.md](../spec.md). Those answers win over the open questions and proposals above.
