# Views and user paths

State after the frontend grilling sessions and the Pencil design rounds of 2026-11-21 (New case, Case list and Case details are drawn). Terms follow the glossary in `../domain/BUSINESS-CONTEXT.md`; Polish UI labels are given in parentheses and are used as written (sentence case). This file describes what the Analyst sees and what happens on each action. Visual design (colors, components, typography) lives in [kas-ui-ux/](kas-ui-ux/README.md); this file only refers to it. The Pencil frames named in this file are in `2026-11-21_kas-olcys-views_v01.pen`, next to this file.

## Problem Statement

The Analyst (PL: Analityk), a KAS official without IT knowledge, needs to enter what they know about an investigated person and get a report on that person's cryptocurrency addresses. Later they need to return to the same person, see earlier results and run the search again. The interface must stay quiet: one clear starting point, no noise, no technical terms.

## Solution

The application has three views:

1. **New case** (PL: Nowa sprawa): the start page. A clean form for the person's identifiers and one button, "Rozpocznij analizę".
2. **Case list** (PL: Lista spraw): a separate view for finding a Case. Cases with New Results (PL: Nowe wyniki) are on top; Reviewed Cases (PL: Przejrzane sprawy) are in a collapsed section below.
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
| Case Number (PL: Numer sprawy) | Excluded Addresses (PL: Adresy wykluczone) |
| New Results / Reviewed state and its button | |
| schedule (P3) | |

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
2a. Dropped on 2026-11-21: one value per field, see Implementation Decisions (One value per field).
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
15a. As an Analyst, I want one search field that filters the list as I type by Case Number, full name and every identifier, so that I find a Case without noise.
16. As an Analyst, I want each Case on the list labelled with the full name or, without one, the first narrowing identifier, with the Case Number and a short summary of identifiers next to it, so that I can recognise the person.
17. As an Analyst, I want to see the status and date of the last Analysis in each row, so that I know what state the Case is in.
17a. As an Analyst, I want to see the highest Confidence Level and the number of Attributions of the last Analysis in each row, so that I know how strong the lead is without opening the Case.
18. As an Analyst, I want Cases with New Results in a section on top, ordered from the highest Confidence Level, so that I see the strongest new leads first.
18a. As an Analyst, I want Cases with a highest Confidence Level of 80% or more to stand out, so that I do not miss a very likely Attribution.
18b. As an Analyst, I want Reviewed Cases in a section that is collapsed by default, so that they do not make noise.
18c. As an Analyst, I want a clear message when I have no Cases yet or when the search finds nothing, so that I know what to do next.

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
29. As an Analyst, I want a button "Oznacz jako przejrzaną" on Case details, so that the Case leaves New Results only when I decide I am done with it.
30. As an Analyst, I want to undo that mark, so that I can bring a Case back to New Results when I marked it too early.

## Implementation Decisions

- **Routes:** three addresses. New case is the start page, `/`. The Case list is `/cases`. Case details has the Case identifier in the path and the selected Analysis identifier in the query, in the form `/cases/{caseId}?analysis={analysisId}`. Without the query the newest Analysis is shown.
- **Page width:** views are drawn at 1920 px. The content stays in a centred column of 1200 px; the header and the footer use the same column.
- **Header:** a white bar with a thin divider. On the left: the KAS sign (the sign alone, without the caption "Krajowa Administracja Skarbowa") and next to it the text "Projekt OŁCyŚ" (20 px, bold, navy). No godło. On the right: one link. On New case it is "Lista spraw →" with the Case counter; on the Case list it is "← Nowa sprawa" without the counter. Permission to use the KAS sign is still an open question in `kas-ui-ux/`.
- **Case counter:** a small red circle with a number next to the "Lista spraw →" link. It shows the number of Cases with New Results.
- **New Results and Reviewed:** a Case has New Results when its newest Analysis has ended and the Analyst has not marked the Case as Reviewed. This holds for manual and scheduled Analyses and also for a failed Analysis, so that the Analyst learns the check did not happen. The Analyst marks a Case with the button "Oznacz jako przejrzaną" on Case details and can undo the mark. Opening the report alone changes nothing. A Reviewed Case gets New Results again when its next Analysis ends.
- **Case Number:** every Case has a Case Number shown to the Analyst, in the form year and sequence number, e.g. `2026/0006`. The word "identyfikator" is used only for data of the investigated person.
- **Case list view** (Pencil frame "[MAIN] Lista spraw"): a separate view, not a side panel. From the top: the title "Lista spraw" with one sentence, the search field, the short statistics "Liczba spraw: N" and "Nowe wyniki: N", then two sections.
  - **Section "Nowe wyniki (N)":** always expanded. Ordered by the highest Confidence Level of the last Analysis, highest first; equal values are ordered by the date of the last Analysis, newest first. The section is not shown when no Case has New Results.
  - **Section "Przejrzane sprawy (N)":** collapsed by default to one bar with the text action "Rozwiń" (expanded: "Zwiń"; frame "[STAN] Lista spraw – przejrzane rozwinięte"). Ordered by the date of the last Analysis, newest first.
  - **Row:** two lines on the left and three aligned columns on the right; the whole row opens Case details with the newest Analysis. Line 1: the label (full name or, without one, the first narrowing identifier; 20 px; bold in New Results) and the Case Number in grey. Line 2: up to two identifiers with the name of their kind in grey ("Nick: krypto_janek · E-mail: …") and "+N więcej". Columns: "Status", "Najwyższy stopień pewności", "Ostatnia analiza". A chevron ends the row.
  - **Column headers:** one small grey row ("Sprawa" and the three column names) above the rows of each section.
  - **Column "Najwyższy stopień pewności":** the highest Confidence Level of the last Analysis in bold (e.g. "95%") and the number of Attributions in grey ("3 powiązania"). Without Attributions: "Brak powiązań". For a failed Analysis or one in progress: "–".
  - **Highlight:** a row whose highest Confidence Level is 80% or more gets a 4 px blue bar on the left and a light blue background. The threshold is the "bardzo wysokie prawdopodobieństwo" band of the challenge cheat sheet (step 2). The percent is always shown as text, so the information is not carried by color alone.
  - **Status badge:** an icon and a word on a light background. The words "Zakończona", "Częściowa", "Nieudana" and "W toku" are working labels until V2 is answered.
  - **Analysis in progress:** the row shows "W toku", "–" in the Confidence Level column and the start time as the date. The row opens the last finished Analysis. The Case does not move to New Results until the Analysis ends.
- **Search on the Case list:** one field "Szukaj sprawy" with the hint "Wpisz numer sprawy, imię i nazwisko, nick, e-mail lub inny identyfikator." It filters as the Analyst types, in the browser, without a "Szukaj" button, by Case Number, full name and all identifiers. Results stay in the two sections with counts of matches only; a section without matches is not shown; the Reviewed section expands by itself when it has a match.
- **Empty states of the Case list:** a grey block in place of the list.
  - No Cases (frame "[STAN] Lista spraw – brak spraw"): "Nie masz jeszcze żadnej sprawy." with the link "← Rozpocznij od nowej sprawy"; the search field is not shown.
  - No matches (frame "[STAN] Lista spraw – brak wyników wyszukiwania"): "Nie znaleziono sprawy dla „…”." and "Sprawdź pisownię lub wpisz inny identyfikator." with the text action "Wyczyść".
- **Case details view** (Pencil frame "[MAIN] Szczegóły sprawy"): one column, from the top: the Case header, the select of the displayed Analysis, the report of that Analysis (summary, Attributions, Excluded Addresses).
  - **Header link:** "← Lista spraw" with the Case counter.
  - **Case header:** the label (full name or first narrowing identifier) with the Case Number in grey; below it the first two identifiers and "+N więcej", which expands the rest. On the right: the outline button "Oznacz jako przejrzaną" and, at the right edge, the primary button "Rozpocznij nową analizę". There is no "Nowe wyniki" badge on this view.
  - **Reviewed Case** (frame "[STAN] Szczegóły sprawy – sprawa przejrzana"): the outline button is replaced by a check icon with the plain text "Sprawa przejrzana" and the text action "Cofnij oznaczenie". No badge.
  - **History of Analyses:** a select "Wyświetlana analiza" with one option per Analysis ("21.11.2026, 14:32 · Ręczna · Zakończona") and the hint "Wybierz wcześniejszą analizę, aby zobaczyć jej raport. Liczba analiz: N." There is no separate history list or table and no bar that warns about an earlier Analysis being displayed; the report heading carries the date.
  - **Report summary:** the heading "Raport analizy z {date}", the status badge with start and end times, "Sprawdzone źródła (N z M)" with the check time of each source, and "Sieci". Always visible.
  - **Note:** "Powiązania są tropami do dalszej weryfikacji, a nie dowodami." above the Attributions; not shown when there are none.
  - **Attributions "Powiązania (N)":** a list sorted by Confidence Level, all rows collapsed on entry. A collapsed row is one line: address with the network below it, the Confidence Level, the number of Mentions, a chevron.
  - **Confidence Level indicator:** the percent in bold, the band label and a single-colour bar. Bands follow the challenge text: 80–100% "Bardzo wysokie prawdopodobieństwo", 60–79% "Prawdopodobny", 40–59% "Średni", 10–39% "Niski / szum".
  - **Expanded Attribution** (frame "[STAN] Szczegóły sprawy – powiązanie rozwinięte"): three blocks stacked in one column, never side by side: "Uzasadnienie" (the Justification text), "Wzmianki (N)" (source name, link, collection time, quoted fragment) and "Aktywność adresu w sieci {network}" (volume, number of transactions, period, dated events). A cash-out signal is a plain line under its event: "Możliwe wyjście do walut tradycyjnych." The row has no badges, no small category labels and no algorithm version.
  - **Scoring "Punktacja":** the first block of an expanded Attribution, directly under the row with the Confidence Level. Collapsed by default to one line with the text action "Rozwiń" (expanded: "Zwiń"; frame "[STAN] Szczegóły sprawy – punktacja rozwinięta"). Expanded, it lists one line per scoring component: a short name with the data that triggered it on the left ("Adres na własnym profilu osoby (GitHub)") and the points it added on the right ("80", "+15"). The last line, in bold, is "Stopień pewności" with the result ("95%"). Added on 2026-11-21 after the review against the challenge text (cheat sheet, step 4: "reguły punktacji").
  - **Justification:** shown only after the row is expanded. A short text from a template of the applied scoring rules, without numbers (see the glossary). The template wording is open (V13).
  - **No Address Activity data:** "Nie udało się pobrać danych o aktywności tego adresu. Brak danych nie oznacza braku aktywności."
  - **No Attributions** (frame "[STAN] Szczegóły sprawy – brak powiązań"): the heading "Powiązania (0)" and a grey block with "Nie znaleziono powiązań w sprawdzonych źródłach." and "Brak powiązań nie oznacza, że osoba nie ma adresów kryptowalut."
  - **Excluded Addresses "Adresy wykluczone (N)":** below the Attributions, collapsed by default with "Rozwiń". Expanded (frame "[STAN] Szczegóły sprawy – adresy wykluczone rozwinięte"): the sentence "Te adresy pojawiły się w źródłach, ale należą do serwisów, z których korzysta wiele osób. Nie są powiązaniami." and a table with the columns "Adres i sieć", "Powód wykluczenia", "Źródło klasyfikacji". No percent; rows do not expand.
  - **Not drawn:** the failed and the partial Analysis.
- **Design tokens:** the Pencil file holds the gov.pl palette as variables, plus light tints added for the list: `primary-tint`, `success-tint`, `success-text`, `warning-tint`, `warning-text`, `error-tint`. Their contrast was chosen by eye and is not measured yet.
- **Footer:** one line, "Wszelkie prawa zastrzeżone. Krajowa Administracja Skarbowa, projekt OŁCyŚ." No links.
- **Font:** Open Sans, as in the gov.pl styleguide.
- **Identifier fields:** the public cryptocurrency address is an input identifier, as in the challenge text (section 2). It counts as a narrowing identifier.
- **One value per field:** every identifier field takes exactly one value, as the backend contract `CreateCaseRequest` does (one `String` per field). A comma is not a separator; the value is only trimmed. Decided on 2026-11-21, replacing "several values separated by commas".
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

## Report view against the real contract (2026-11-21, after backend #53)

Decided by the lead in the grilling of 2026-11-21 against `frontend/apps/web/lib/api/openapi.json`. Where this section and Implementation Decisions differ, this section wins. The Case list (`/cases`) stays an empty page in this step.

- **Start and wait:** "Rozpocznij analizę" calls `POST /cases`, then `POST /cases/{caseId}/analyses` (202, status `RUNNING`). The blocking loader polls `GET /analyses/{analysisId}` every 1.5 s while the status is `RUNNING`, then opens `/cases/{caseId}?analysis={analysisId}`, which reads `GET /analyses/{analysisId}/report`. "Rozpocznij nową analizę" does the same without `POST /cases`.
- **Justification from rule codes:** the backend returns no Justification text (gap G4), only `scoringRules` (`rule`, `contribution`) and the Mentions in `justification`. The frontend maps each known rule code to a Polish sentence (Justification) and a Polish line label (Punktacja). An unknown code is shown as is, with its points. Known codes: `MENTIONED_WITH_IDENTIFIER` (base 40), `INDEPENDENT_SOURCES` (+15).
- **Case Number hidden:** not in `CaseResponse` yet; issue #56. The Case header shows the label without the number until then.
- **Reviewed state in the browser:** "Oznacz jako przejrzaną" and "Cofnij oznaczenie" stay; the state is stored in `localStorage` per Case until the backend holds it (V11). Known limit: it is per browser.
- **Address Activity unavailable:** `addressActivity` is always null today (gap G7); the block shows the existing message "Nie udało się pobrać danych o aktywności tego adresu. Brak danych nie oznacza braku aktywności."
- **Test data label:** each source in "Sprawdzone źródła" with `synthetic: true` gets the label "Dane testowe".
- **Unchecked classification:** an Attribution with `classificationStatus: UNAVAILABLE` shows, in its collapsed row, "Nie sprawdzono, czy adres należy do serwisu (np. giełdy)."
- **Report limitations:** each item of `limitations` is one Polish sentence in a warning list in the report summary.
- **Address verification (closes D10):** an expanded Attribution gets the block "Weryfikacja adresu": format valid, network, result of the exchange and contract check (from `detectedAddresses` and `classificationStatus`).
- **Failed and partial Analysis (closes V16 for the report):** `FAILED` shows the summary and, in place of the Attributions, a grey block "Analiza nie powiodła się" with the limitations. `PARTIAL` shows the full report with a warning in the summary.
- **No new Pencil frames:** the new states (loader, `FAILED`, `PARTIAL`, "Weryfikacja adresu", Address Activity unavailable, "Dane testowe", limitations) are built from existing patterns (grey block, warning, label) without drawing them first.
- **Status and trigger labels (closes V2):** `RUNNING` "W toku", `COMPLETED` "Zakończona", `PARTIAL` "Częściowa", `FAILED` "Nieudana"; `MANUAL` "Ręczna", `SCHEDULED` "Zaplanowana".
- **Loader errors:** no time limit. A network error while polling closes the loader and shows a dialog with "Rozumiem", then opens Case details.
- **Opening a running Analysis:** the summary with the badge "W toku" and the message "Analiza jeszcze trwa"; the report polls every 1.5 s in the background without blocking and refreshes itself when the Analysis ends.
- **Address entered by the Analyst (gap G3):** when an Attribution's address equals the Case `walletAddress`, the row shows "Adres podany przy uruchomieniu analizy". A backend flag is a note for #9.
- **Rejected addresses:** a collapsed section "Adresy odrzucone (N)" below Excluded Addresses, from `GET /analyses/{analysisId}/mentions`, entries with `valid: false`. Columns: address, reason (`MALFORMED` "Błędny format", `CHECKSUM_MISMATCH` "Błędna suma kontrolna", `UNSUPPORTED_NETWORK` "Nieobsługiwana sieć", `NETWORK_MISMATCH` "Niezgodna sieć"), source.
- **Excluded Address evidence (D7, part):** a row expands to its Mentions (source name and link, no collection time). Reason labels: `EXCHANGE` "Adres giełdy kryptowalut", `SMART_CONTRACT` "Adres programu w sieci (smart kontrakt)", `MIXER` "Adres miksera", `FUNDRAISER` "Adres zbiórki publicznej". `classificationSource` is shown as returned.
- **No Case counter:** the counter next to "Lista spraw" is removed in this step; it returns with the Case list.
- **Address Activity figures:** "Łączny wolumen" = `totalReceived + totalSent` in `unit`; "Liczba transakcji" = `events.length`; "Okres" = earliest to latest `occurredAt`; events sorted by `occurredAt`; `CASH_OUT_SIGNAL` is its own event with the line "Możliwe wyjście do walut tradycyjnych."; `coverageLimitation` is a warning under the block. These figures are computed in one separate frontend module with a seam, so the backend can supply them later without changing the view.
- **Scoring bands (Q13):** `provisional-1` gives only 40% or 55%; the gap is described for the team in #9. The frontend shows what the backend returns.
- **Copy decided after the task review (Q28–Q44, 2026-11-21):** full list in `.agents/specs/2026-11-21_analysis-report/spec.md`. Polling error: "Utracono połączenie" / "Analiza może nadal trwać. Sprawdź jej wynik w szczegółach sprawy." / "Rozumiem". No Analysis: "Ta sprawa nie ma jeszcze analizy.". Unknown Analysis in the address: "Nie znaleziono tej analizy w sprawie." with "Pokaż najnowszą analizę". Partial: "Analiza zakończyła się częściowo. Raport może być niepełny.". Empty sections show "(0)" and "Nie znaleziono adresów wykluczonych." / "Nie znaleziono adresów odrzuconych.". Missing format check: "Brak danych o sprawdzeniu formatu adresu.". Below 40% is "Niski / szum".
- **Source check time:** every source shows "· sprawdzono o HH:MM" from `checkedAt`; a source with `status: UNAVAILABLE` shows "· niedostępne" as a warning.

## Testing Decisions

Deferred. Test seams are decided after the views are laid out in Pencil. The test scope of ADR-0010 applies.

## Out of Scope

- Editing a Case.
- Leaving the view during a manual Analysis.
- Merging two Cases or adding new identifiers to an existing Case.
- Visual design details (colors, typography, components): see `kas-ui-ux/`.
- Comparison of two Analyses (E6.3) and the schedule controls (E6.4): later phases, placed in the Case scope of Case details.
- Print, PDF export and passing the report outside the application (E5-D2).
- On Case details: the algorithm version. The scoring breakdown is in scope since 2026-11-21 (block "Punktacja").
- On the Case list: pagination, a sort control, filters by status and server-side search. The demo has a handful of Cases.

## Deferred against the challenge text

Result of the review of the Pencil views against `../domain/hacknation-2026-task/MF - Pełna Treść Zadania Konkursowego.md` on 2026-11-21. The lead decided to defer every point below on purpose; only the scoring breakdown was added. Each point names the part of the challenge text it falls short of, so that the team can defend it before the jury.

| # | Deferred | Challenge text | State in the views |
|---|---|---|---|
| D1 | Periodic Analyses | §2 "cykliczne wykonywanie analiz" | No schedule view; only the word "Ręczna" in the Analysis select (E6.4). |
| D2 | Comparing results and detecting a replaced address over time | §2 "porównywanie rezultatów"; §3 optional criterion | Analyses can be switched, but there is no view of differences (E6.3). |
| D3 | Marking source facts, calculation results and conclusions that need verification | §7; judging criterion 3 | One general sentence, "Powiązania są tropami…"; the blocks are not labelled this way. |
| D4 | Full name alone as input | §2 lists it as a sufficient identifier | The form rejects it (minimum data rule). |
| D5 | Category of an Attribution | §3 "kategoryzować znalezione powiązania" | Only the Confidence Level band; the source kind appears in the "Punktacja" line and the Justification, not as a category. |
| D6 | Cash-out signals | §3 Fiat Cash-Out | The signal is only on the transfer to an exchange, not on the swap to a stablecoin; no signal kind (P2P, payment gateway, card), no name or address of the counterparty; visible only after expanding the row. |
| D7 | Evidence for Excluded Addresses | §3; cheat sheet steps 1 and 3 | No Mention (where found, URL, time); "Źródło klasyfikacji" is general, without the API tag (e.g. `Kraken: Hot Wallet`); no smart contract or mixer example. |
| D8 | Metadata to reproduce an Analysis | Acceptance criteria | A Mention has URL, collection time and quote; search parameters, publication date of the post and the algorithm version are missing. |
| D9 | Chronological description of the flow of funds | §7 | Two dated outgoing events; no incoming transfers, counterparties or full timeline. |
| D10 | Verification of the address | §6 mandatory scenario | The report does not show that the address was verified (format, exists on the network, not a contract). |
| D11 | Complete OSINT report as output | §7 | The report exists only on screen; print and export are out of scope (E5-D2). |

## Further Notes

### Known limitation

Because Cases cannot be edited and duplicates are rejected, the Analyst cannot add a new identifier to a known person. Example: the e-mail matches an existing Case, but the nickname is new. The Analyst is sent to the existing Case, and the new nickname is not searched. To revisit if the team needs it.

### Open questions

| # | Question | Owner |
|---|---|---|
| V1 | Does a manual Analysis still finish and get saved on the server when the Analyst closes the tab? The frontend assumes yes. Asked in issue #24. | Bohdan (Analysis) |
| V2 | Exact status names of an Analysis and how "completed, no Mentions" is told apart from a failure. | Bohdan (Analysis) |
| V3 | Closed on 2026-11-21: the Case list is described in Implementation Decisions (Routes, Case counter, New Results and Reviewed, Case list view, Search, Empty states). | Design work |
| V4 | Look of the blocking loader; the gov.pl styleguide lists a Loader component without a screenshot. | Design work |
| V5 | Closed on 2026-11-21: the Confidence Level indicator, the history of Analyses and the Excluded Addresses section are described in Implementation Decisions (Case details view). | Design work |
| V6 | Normalisation for the duplicate rule (e.g. case of e-mail, spaces at the ends). | Case owner (Maksym) |
| V7 | Closed on 2026-11-21: the layout of the New case form is described in Implementation Decisions (Form layout). | Design work |
| V8 | Closed on 2026-11-21: the Case contract accepts the wallet address (`walletAddress`, PR #52); the form takes one value per field (Implementation Decisions, One value per field). | Case owner (Maksym) |
| V9 | The backend must assign a Case Number in the form year and sequence number (e.g. `2026/0006`) and return it with the Case. Issue #56. | Case owner (Maksym) |
| V10 | The Case list response must carry, per Case: the status and date of the last Analysis, the number of its Attributions and its highest Confidence Level. Without the last two the column "Najwyższy stopień pewności", the highlight and the order of New Results cannot work. | Case owner (Maksym), Bohdan (Analysis) |
| V11 | The Reviewed state must be saved on the server (the application has no login, so the browser cannot hold it), with an action to set it and to undo it, and it must reset when the next Analysis of the Case ends. Who owns it: Case or Analysis? | Maksym, Bohdan |
| V12 | Measure the contrast of the tint variables added in Pencil against WCAG 2.1 AA (4.5:1 for text). | Design work |
| V13 | Wording of the Justification template: one sentence per scoring rule, without numbers. The backend returns the ready text with each Attribution. Issue #9. | E3 (Marcin) |
| V14 | The Analysis response must carry, per Attribution: address, network, Confidence Level, the Justification text and its Mentions (source name, link, collection time, quoted fragment); per Excluded Address: address, network, reason, classification source. | Bohdan (Analysis), Maksym (E3) |
| V15 | Address Activity depends on E4 (issue #23, blocked). The block is drawn on sample data; its fields (volume, number of transactions, period, events, cash-out signal) are not agreed. | Maksym (E4) |
| V16 | Look of Case details for a failed and for a partial Analysis; not drawn. Depends on V2. | Design work |
