# Spec: Analysis report on Case details

**Date:** 2026-11-21 · **Branch:** to be created by the lead from `master` (proposed `feature/FRONTEND-analysis-report`) · **Status:** decided by the lead, ready to implement

This spec records the decisions of the grilling of 2026-11-21 (rounds Q1–Q27) for the report of an Analysis on Case details (PL: Szczegóły sprawy), wired end to end to the backend merged in #53 and #55. It follows the `to-spec` template and is saved as a file instead of a tracker issue, on the lead's request. The work is split into three tasks in [tasks/](tasks/).

Sources this spec is built on, and which win when they differ from it:

- `.agents/context/frontend/views.md`, section "Report view against the real contract": behaviour and UI copy decided in this grilling (wins over the older Implementation Decisions of the same file).
- `.agents/context/frontend/views.md`, Implementation Decisions "Case details view": the rest of the behaviour and copy.
- Pencil frames "[MAIN] Szczegóły sprawy" and the five "[STAN] Szczegóły sprawy – …" frames in `.agents/context/frontend/2026-11-21_kas-olcys-views_v01.pen`: layout, sizes, copy (wins on visual detail; read through the Pencil MCP, the file is encrypted). The new states of this spec are not drawn (decision Q12).
- `frontend/apps/web/lib/api/openapi.json`: the contract. Never edited by hand.
- `.agents/rules/frontend.md` and the frontend ADRs (especially 0007, 0008, 0009, 0010): how the code is written (win on code rules).
- `.agents/context/domain/BUSINESS-CONTEXT.md`: the glossary in force (Rejected Address and Report Limitation added on 2026-11-21).
- `.agents/context/domain/hacknation-2026-task/MF - Pełna Treść Zadania Konkursowego.md`: the challenge text; §6 mandatory scenario and the judging criteria.

## Problem Statement

The Analyst (PL: Analityk) can fill in the New case form, but the Analysis behind "Rozpocznij analizę" is a mock and Case details is an empty stub. The Analyst never sees what the application found: which addresses are Attributions (PL: Powiązania), how sure they are, where the Mentions (PL: Wzmianki) come from, which addresses were rejected or excluded, and whether the report is complete. Without this the mandatory scenario of the challenge (§6: input, OSINT search, address detection and verification, Confidence Level, rejection of false attributions, blockchain activity, sources and result) cannot be shown.

## Solution

"Rozpocznij analizę" creates the Case and starts its Analysis on the real backend; a blocking loader waits until the Analysis ends, then Case details opens with its report. Case details shows, in one column:

- the Case header: label, identifiers, "Oznacz jako przejrzaną" (kept in the browser) and "Rozpocznij nową analizę";
- the select "Wyświetlana analiza" with the history of Analyses;
- the report summary: heading with the date, status, start and end times, checked sources with check time, "Dane testowe" and "niedostępne" labels, networks, and the Report Limitations;
- the Attributions, sorted by Confidence Level, each expandable into Punktacja, Uzasadnienie, Wzmianki, Weryfikacja adresu and Aktywność adresu;
- the Excluded Addresses (PL: Adresy wykluczone), each expandable into its Mentions;
- the Rejected Addresses (PL: Adresy odrzucone).

A failed Analysis shows the summary and a grey block instead of the Attributions; a partial one shows the full report with a warning; a running one shows "W toku" and refreshes itself when it ends. The Case list stays an empty page in this step.

## User Stories

### Starting and waiting

1. As an Analyst, I want "Rozpocznij analizę" to create the Case and start its Analysis on the server, so that the report shows real results instead of a demo.
2. As an Analyst, I want the full-screen loader to stay until the Analysis has really ended, so that I do not land on an empty report.
3. As an Analyst, I want the loader to check the state of the Analysis by itself every 1.5 seconds, so that I do not have to refresh the page.
4. As an Analyst, I want to land on Case details with the finished Analysis selected right after the loader disappears, so that I see the result without an extra click.
5. As an Analyst, I want "Rozpocznij nową analizę" on Case details to start another Analysis of the same Case with the same loader, so that I can check for new findings.
6. As an Analyst, I want the new Analysis to be selected when it ends, so that I see the newest result first.
7. As an Analyst, I want a clear pop-up when the connection fails while I wait, followed by Case details, so that I am not stuck behind the loader.
8. As an Analyst, I want a clear pop-up when the Analysis cannot be started (e.g. one is already running), so that I know why nothing happened.
9. As an Analyst, I want the browser to warn me before I close the tab while the loader is shown, so that I do not leave by accident.

### Case header and history

10. As an Analyst, I want to see the label of the Case (full name or, without one, the first narrowing identifier) on top, so that I know whose report this is.
11. As an Analyst, I want to see the first two identifiers with their kind and "+N więcej" to expand the rest, so that I see the scope of the Case without noise.
12. As an Analyst, I want the Case Number to be missing rather than invented while the server has none, so that I never rely on a fake number.
13. As an Analyst, I want to mark the Case with "Oznacz jako przejrzaną", so that I note that I am done with it.
14. As an Analyst, I want to undo the mark with "Cofnij oznaczenie", so that I can correct a mark made too early.
15. As an Analyst, I want the mark to be kept when I reload the page, so that it does not disappear by itself.
16. As an Analyst, I want a select "Wyświetlana analiza" with one option per Analysis (date and time, trigger, status), so that I can open an earlier report.
17. As an Analyst, I want the newest Analysis selected when the address has no `analysis` parameter, so that I see the current result.
18. As an Analyst, I want the selected Analysis to be in the page address, so that I can reopen the same report later.
19. As an Analyst, I want the hint "Wybierz wcześniejszą analizę, aby zobaczyć jej raport. Liczba analiz: N.", so that I know the history exists.
20. As an Analyst, I want the header link "← Lista spraw" without a counter, so that the header shows nothing that cannot be counted yet.

### Report summary

21. As an Analyst, I want the heading "Raport analizy z {date}", so that I know which run I am reading.
22. As an Analyst, I want the status of the Analysis as a badge with a word ("W toku", "Zakończona", "Częściowa", "Nieudana") and the start and end times, so that I know if the result is complete.
23. As an Analyst, I want to see the trigger "Ręczna" or "Zaplanowana", so that I know who started the run.
24. As an Analyst, I want "Sprawdzone źródła (N z M)" with "· sprawdzono o HH:MM" next to each source, so that I know when each piece of data was collected.
25. As an Analyst, I want an unavailable source marked "· niedostępne" as a warning, so that I know the report misses it.
26. As an Analyst, I want each source built on synthetic data marked "Dane testowe", so that I never take test data for real evidence.
27. As an Analyst, I want to see the networks that were checked, so that I know the coverage.
28. As an Analyst, I want every Report Limitation as one plain Polish sentence in a warning list, so that I know why the report may be incomplete.
29. As an Analyst, I want a partial Analysis to show its full report with a warning in the summary, so that I can still use what was found.
30. As an Analyst, I want a failed Analysis to show the summary and a grey block "Analiza nie powiodła się" with its Report Limitations instead of the Attributions, so that I know the check did not happen.
31. As an Analyst, I want a running Analysis to show "W toku" and the message "Analiza jeszcze trwa", and to refresh itself when it ends, so that I do not have to reload.

### Attributions

32. As an Analyst, I want the note "Powiązania są tropami do dalszej weryfikacji, a nie dowodami." above the Attributions, so that I treat them as leads.
33. As an Analyst, I want the Attributions "Powiązania (N)" sorted by Confidence Level, highest first, all collapsed on entry, so that I see the strongest leads first.
34. As an Analyst, I want each collapsed row to show the address, the network, the Confidence Level (percent, band label, bar) and the number of Mentions, so that I can compare leads at a glance.
35. As an Analyst, I want the band label to follow the challenge (80–100% "Bardzo wysokie prawdopodobieństwo", 60–79% "Prawdopodobny", 40–59% "Średni", 10–39% "Niski / szum"), so that the percent has a meaning.
36. As an Analyst, I want a row whose address was not checked against exchanges and contracts to say "Nie sprawdzono, czy adres należy do serwisu (np. giełdy).", so that I do not over-trust it.
37. As an Analyst, I want a row whose address is the one entered in the Case to say "Adres podany przy uruchomieniu analizy", so that I do not take my own input for a finding.
38. As an Analyst, I want to expand a row, so that I see why it was created.
39. As an Analyst, I want the block "Punktacja" (collapsed, "Rozwiń" / "Zwiń") with one line per scoring rule and its points and the bold line "Stopień pewności", so that I can recompute the result.
40. As an Analyst, I want an unknown scoring rule shown with its code and points rather than hidden, so that the sum always matches.
41. As an Analyst, I want the block "Uzasadnienie" with plain sentences built from the applied rules, without numbers, so that I understand the reason in words. *(Since Q46 the sentences are the backend `justification` text.)*
42. As an Analyst, I want the block "Wzmianki (N)" with source name, link, collection time and the quoted fragment, so that I can open and check each source.
43. As an Analyst, I want the block "Weryfikacja adresu" saying the format is valid, the network, and the result of the exchange and contract check, so that I see the address was verified.
44. As an Analyst, I want the block "Aktywność adresu w sieci {network}" with total volume, number of transactions, period and dated events, so that I see what the address did.
45. As an Analyst, I want a cash-out event to carry the line "Możliwe wyjście do walut tradycyjnych.", so that I notice a possible exit to fiat.
46. As an Analyst, I want a coverage limitation of the activity data shown under the block, so that I know the figures may be incomplete.
47. As an Analyst, I want "Nie udało się pobrać danych o aktywności tego adresu. Brak danych nie oznacza braku aktywności." when there is no activity data, so that I do not read missing data as no activity.
48. As an Analyst, I want "Powiązania (0)" with "Nie znaleziono powiązań w sprawdzonych źródłach." and "Brak powiązań nie oznacza, że osoba nie ma adresów kryptowalut." when there are none, so that I read the empty result correctly.

### Excluded and Rejected Addresses

49. As an Analyst, I want "Adresy wykluczone (N)" below the Attributions, collapsed by default, so that exclusions do not mix with leads.
50. As an Analyst, I want each Excluded Address with its network, a plain reason ("Adres giełdy kryptowalut", "Adres programu w sieci (smart kontrakt)", "Adres miksera", "Adres zbiórki publicznej") and the classification source, so that I know why it was excluded.
51. As an Analyst, I want to expand an Excluded Address into its Mentions (source name and link), so that I can see where it appeared.
52. As an Analyst, I want "Adresy odrzucone (N)", collapsed by default, with each Rejected Address, its reason ("Błędny format", "Błędna suma kontrolna", "Nieobsługiwana sieć", "Niezgodna sieć") and its source, so that I see that text which only looks like an address was not used.
53. As an Analyst, I want every section to show its count, also "(0)", with a short message, so that I know the section was checked and found nothing.

### General

54. As an Analyst, I want every failed call to the server to show "Coś poszło nie tak" with one sentence and "Spróbuj ponownie", so that errors look the same everywhere.
55. As an Analyst, I want loading states while the report is fetched, so that I know the page is working.
56. As an Analyst, I want to operate everything by keyboard and not rely on color alone, so that the view meets WCAG 2.1 AA.

## Implementation Decisions

### Modules and folders (Q23–Q27)

Three new areas, following ADR-0009 and the "Case scope | Analysis scope" table of `views.md`:

- **Analysis run** (cross-feature, in `lib/`, Q24): starting and waiting. Used by New case and Case details.
  - *Run function:* for New case, create the Case and start its Analysis; for Case details, start an Analysis of an existing Case. Both return the Case id and the Analysis id. Only the generated client is used.
  - *Poll interval rule (seam S3):* a pure function from an Analysis status (or none yet) to the next interval: 1500 ms while `RUNNING`, stop otherwise. Passed to TanStack Query `refetchInterval`. The same rule drives the loader and the running report.
  - *Run hook:* mutation, then polling of the Analysis until it ends, then navigation to `/cases/{caseId}?analysis={analysisId}`.
  - *Loader:* moved from the New case feature unchanged (overlay, sentence, `beforeunload` warning).
- **Case details** feature (Case scope): the client view with the four states (loading, error, empty, data), the Case header, the Reviewed store, the Analysis select.
- **Analysis report** feature (Analysis scope): the view model, the labels, the activity figures, the report components.

The New case feature loses its mock and calls the Analysis run hook. The route file for Case details stays a thin Server Component that passes the Case id and the optional `analysis` parameter to the Case details view. Only the Case details feature imports from the Analysis report feature.

### Report view model (seam S1, Q26)

- One public entry: `toReportView(report, mentions, caseIdentifiers)` → a view model that components render without reading API types.
- Inside it, one function per section (summary, Attribution, Excluded Address, Rejected Address); they are not exported and are tested only through `toReportView`.
- It decides: status and trigger labels; band of the Confidence Level; order of the Attributions (Confidence Level descending, then address for stability); Punktacja lines and their sum; Justification sentences from rule codes (*superseded by Q46: the Justification is the backend `justification` text, passed through*); the unchecked-classification warning (`classificationStatus: UNAVAILABLE`); the input-address flag (address equals the Case `walletAddress`, compared in the canonical form the backend stores); the "Weryfikacja adresu" lines (from the Mention's `detectedAddresses` entry for this address and from `classificationStatus`); Report Limitation sentences; reason labels for Excluded and Rejected Addresses; the source lines (check time, "Dane testowe", "niedostępne"); which top-level state to show (running, failed, partial, completed, no Attributions).
- Rejected Addresses come from `GET /analyses/{analysisId}/mentions`: every `detectedAddresses` entry with `valid: false`, de-duplicated by address and reason, with the source name of its Mention.

### Labels (Q27)

- One file with every code → Polish text map used by the report: Analysis status, trigger, Confidence Level bands, Excluded reasons, Rejected reasons, Report Limitations, scoring rule codes (Punktacja label and Justification sentence). *Since Q45–Q46: Punktacja label only.*
- Status: `RUNNING` "W toku", `COMPLETED` "Zakończona", `PARTIAL` "Częściowa", `FAILED` "Nieudana". Trigger: `MANUAL` "Ręczna", `SCHEDULED` "Zaplanowana" (closes V2).
- Scoring rules known today (`provisional-1`): `MENTIONED_WITH_IDENTIFIER` and `INDEPENDENT_SOURCES`. An unknown code is shown with the raw code and its points in Punktacja and adds no Justification sentence. *Superseded by Q45: the codes are the 13 codes of `confidence-1`; an unknown code still gets its raw code and points in Punktacja.*
- Report Limitations: `SOURCE_UNAVAILABLE`, `BLOCKCHAIN_DATA_UNAVAILABLE`, `CLASSIFICATION_UNAVAILABLE`, `ANALYSIS_INCOMPLETE`, one sentence each; an unknown code is shown as a generic sentence with the code.
- The exact wording of the Justification sentences (*superseded by Q46: written by the backend, D9-006*) and the Report Limitation sentences is written in task 03 and task 02 and reviewed by the lead; it follows the glossary (no numbers in the Justification, no technical terms).

### Address Activity figures (seam S2, Q17, Q25)

- A type `AddressActivityFigures` (volume, transaction count, period, source, events, coverage limitations) and one function `toAddressActivityFigures(status, activity)`.
- Adapted to the backend contract of #23 (D23-008) when it was merged: volume = `totalReceived + totalSent` in `unit`; transaction count from the backend (`transactionCount`, "Nieznana" when null); period from `firstActivityAt` and `lastActivityAt` ("do MM.RRRR" when only the last is known); source = source name, "(dane testowe)" for synthetic data and the time it was read.
- A `null` activity gives "no data" (`UNAVAILABLE`) or "not checked" (`NOT_CHECKED`, an Attribution at 0%); a `null` total or count is unknown, not zero.
- Events are the cash-out signals, ascending by time, each with a plain label per kind (exchange, P2P, payment gateway, crypto card, stablecoin swap), the amount, the counterparty, the transaction reference (a link only for http(s)) and the line "Możliwe wyjście do walut tradycyjnych.". Each coverage limitation code is one warning sentence under the block.

### Reviewed store (Q4)

- The Reviewed mark is kept in `localStorage`, one key per Case id, read and written through one small module; no backend call. Known limit: per browser.
- Not tested by Vitest; verified in the browser.

### API contract used

- `POST /api/1/cases` (201 `CaseResponse`), `POST /api/1/cases/{caseId}/analyses` (202 `AnalysisResponse`, `RUNNING`; 409 when one is running), `GET /api/1/analyses/{analysisId}` (polling), `GET /api/1/cases/{caseId}` (header), `GET /api/1/cases/{caseId}/analyses` (select), `GET /api/1/analyses/{analysisId}/report`, `GET /api/1/analyses/{analysisId}/mentions`.
- No contract change is needed for this spec. Run `./scripts/api-sync.sh` before coding to confirm there is no drift.

### Specific interactions

- **Loader errors (Q15):** no time limit. A network error while polling closes the loader and shows a dialog with "Rozumiem"; after it, Case details opens (with the `analysis` parameter when the id is known). A failed start shows `toUserMessage(error)` with "Coś poszło nie tak" and "Spróbuj ponownie".
- **Running report (Q16):** when the selected Analysis is `RUNNING`, the report polls with the same rule in the background and refetches the report when it ends; the view is not blocked.
- **Case Number (Q3):** not shown; issue #56.
- **Counter (Q21):** removed from the page shell header.
- **Excluded Addresses (Q20):** rows expand to their Mentions without the collection time.

## Decisions after the task review (Q28–Q44, 2026-11-21)

Answers of the lead to the open questions raised while the task files were written. They win over anything older in this spec and in the task files.

| # | Task | Decision |
|---|---|---|
| Q28 | 01 | Polling connection error dialog: title "Utracono połączenie", sentence "Analiza może nadal trwać. Sprawdź jej wynik w szczegółach sprawy.", button "Rozumiem"; then Case details opens. |
| Q29 | 01 | An `ApiError` while polling (e.g. 404, 500) shows the same dialog as Q28, not `toUserMessage`. Deliberate exception to the error rule of `frontend.md`: polling only observes, the Analysis may still be running on the server. Note it in a code comment. |
| Q30 | 01 | When `createCase` succeeded and `startAnalysis` failed, the hook keeps the `caseId`; "Spróbuj ponownie" calls only `startAnalysis`, never a second `createCase`. |
| Q31 | 01 | The start error dialogs move to `lib/analysis-run/` and are used by New case and Case details. |
| Q32 | 01 | The hook interface proposed in task 01 is accepted (`start(caseId)`, `isRunning`, `warnBeforeUnload`, `startError`, `pollingFailed`); names may be refined during implementation. |
| Q33 | 02 | The identifier kind labels move from the New case feature to `lib/` and are used by New case and Case details. |
| Q34 | 02 | The "first narrowing identifier" follows the form order: nickname, login, e-mail, messenger ID, profile address, wallet address. |
| Q35 | 02 | A Case without any Analysis shows "Ta sprawa nie ma jeszcze analizy." and the button "Rozpocznij nową analizę". |
| Q36 | 02 | An `analysis` parameter that is not in the history of the Case shows "Nie znaleziono tej analizy w sprawie." with the link "Pokaż najnowszą analizę"; no other report is shown in its place. |
| Q37 | 02 | `PARTIAL` shows the fixed sentence "Analiza zakończyła się częściowo. Raport może być niepełny." with the Report Limitations below it. |
| Q38 | 03 | A Punktacja line is the rule label followed by the source names of the Attribution's Mentions in brackets, e.g. "… (GitHub, Forum kryptowalut)". |
| Q39 | 03 | Every Confidence Level below 40% is "Niski / szum". |
| Q40 | 03 | A Rejected Address found in several sources lists all source names, comma-separated. |
| Q41 | 03 | Every section shows its count, also "(0)", with a message: Attributions use the designed empty state; Excluded Addresses "Nie znaleziono adresów wykluczonych."; Rejected Addresses "Nie znaleziono adresów odrzuconych." Replaces "hidden when empty". |
| Q42 | 03 | When no `detectedAddresses` entry matches the address, "Weryfikacja adresu" shows "Brak danych o sprawdzeniu formatu adresu."; the other lines stay. |
| Q43 | 03 | Excluded Addresses keep the table; each row gets a chevron and its Mentions expand below the row at full width. |
| Q44 | 03 | Formatting with `Intl` `pl-PL`: date and time "21.11.2026, 14:32"; amounts with a decimal comma, up to 8 decimals without trailing zeros, unit after the amount ("2,41 BTC"); period "03.2025 – 11.2026". |

## Decisions after merging confidence-1, #58 (Q45–Q48, 2026-11-21)

Master brought the backend algorithm `confidence-1` (#58, D9-006). The lead decided as below. These answers win over anything older in this spec and in the task files.

| # | Task | Decision |
|---|---|---|
| Q45 | 03 | The report shows the scoring breakdown. Punktacja stays on Case details as in Q38: rule label with the source names in brackets, points, bold "Stopień pewności". Every one of the 13 `confidence-1` rule codes has a Polish label in `features/analysis-report/report-labels.ts`. Replaces the `provisional-1` codes. D9-006 is amended to match. |
| Q46 | 03 | Uzasadnienie shows the backend `AttributionResponse.justification` text as returned. If the text is absent or blank, the block still shows "Brak zapisanego uzasadnienia dla tej analizy". The frontend has no Justification sentences of its own per rule. For saved `provisional-1` Analyses, Punktacja retains the label "Adres przy identyfikatorze osoby" for `MENTIONED_WITH_IDENTIFIER`. |
| Q47 | 03 | The Mentions of an Attribution and of an Excluded Address are read from `mentions`. The field `justification` is now the text, not the Mention list. |
| Q48 | 01 | New case keeps the Analysis run of `lib/analysis-run/`. The duplicate `features/new-case/analysis-flow.ts` from #59 is removed. |

## Testing Decisions

- **A good test here** checks external behaviour of pure logic: API-shaped data in, view model out. No test looks at components, hooks, markup or private helpers.
- **S1 `toReportView`** (main seam): status and trigger labels; band edges (39/40, 59/60, 79/80, 100); sort order; Punktacja lines and sum, including an unknown rule code; Justification sentences per known rule and none for unknown (*superseded by Q46: the view model passes the `justification` text through*); `UNAVAILABLE` warning; input-address flag on and off; "Weryfikacja adresu" lines; source lines (`synthetic`, `UNAVAILABLE`, `checkedAt` null); Report Limitation sentences including an unknown code; Excluded reason labels; Rejected Addresses from Mentions (only `valid: false`, de-duplicated, each reason label); top-level state for `RUNNING`, `FAILED`, `PARTIAL`, `COMPLETED` with and without Attributions.
- **S2 `toAddressActivityFigures`:** `null` activity; volume with both totals, with one `null`; transaction count; period from unsorted events; single event; no events; sort order.
- **S3 poll interval rule:** 1500 for `RUNNING` and for no data yet; stop for `COMPLETED`, `PARTIAL`, `FAILED`.
- **Not tested by Vitest** (ADR-0010): components, the loader, the Reviewed store, navigation. Verified in the running app with a clean console.
- **Fixtures:** typed with the generated model types, built by small factory functions inside the test files; no hand-written response types.
- **Prior art:** `features/new-case/new-case-rules.test.ts`, `lib/api/error-message.test.ts`, `lib/api/api-error.test.ts`.
- **Quality gate:** `npm run check` and `npm run build` from `frontend/`.

## Out of Scope

- The Case list (`/cases` stays an empty page) and the Case counter.
- The Case Number (issue #56).
- Reviewed state on the server (V11).
- Comparison of two Analyses (E6.3, D2) and schedule controls (E6.4, D1).
- Labelling blocks as source facts, calculation results and conclusions (D3, deferred by the lead).
- A time limit for the loader.
- New Pencil frames for the new states (Q12).
- Print, PDF export (D11).
- Any backend change, including the scoring bands (Q13, see Further Notes).

## Further Notes

- **Scoring bands (Q13):** *Superseded by #58: `confidence-1` uses bases 80, 60, 40, 10 and +15 (D9-002), so every band can appear; demo-v1 scores are in D9-008.* `provisional-1` gives only 40% or 55%, so the bands 60–79% and 80–100% never appear with today's backend. The frontend shows what the backend returns; the problem is described for the team in #9. The demo will look different from the Pencil frames (95%) until #9 changes.
- **Address Activity (G7):** closed by #23: the demo addresses carry synthetic activity; see D23-009 for the demo ETH address that the bytecode check excludes.
- **Input address (G3):** the frontend flag is a stopgap; a backend flag is a note for #9. The glossary says a Wallet address never makes an Attribution by itself, and the backend does not enforce it yet.
- **Rejected Address demo (G1):** the demo-v1 fixture produces no Rejected Address; the section will be empty until the fixture changes.
- **Parallel work:** tasks 01 and 02 can run in parallel; task 03 builds on the report view and view model of task 02.
