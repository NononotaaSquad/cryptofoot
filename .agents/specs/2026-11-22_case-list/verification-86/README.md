# Verification record, #86 (search, groups, empty states, refresh, counter)

Edge (playwright-core) against a local backend on a dedicated Mongo database (`list86`, dropped and seeded by the script) and `npm run dev`. All checks below passed in the last run.

Screenshots: `A-no-cases.png` (state A), `B-collapsed.png`, `C-expanded.png`, `D-no-matches.png`, `badge.png` (header counter), `cyclic-new.png`, `refresh-error.png`, `initial-error.png`.

- **A, no Cases:** no search field, "Nie masz jeszcze żadnej sprawy.", link to `/`, no stats.
- **B/C:** stats "Liczba spraw: 13 / Nowe wyniki: 7"; group titles carry the matching counts; Reviewed collapsed by default, toggle expands and collapses it; New ordered by known highest level descending with unknown (RUNNING, FAILED, no leads) last.
- **D, no match:** exact message and hint, phrase and field stay, no stats; "Wyczyść" restores the list and puts focus back in the field. Markup (`<img ...>`) renders as text; `.*` is literal.
- **Search:** trim and case-insensitive; full wallet; matching-only counts; Reviewed opens by itself on a match and clearing restores the choice made before the search (closed, and open when it was open).
- **Keyboard:** Tab leaves the field; Enter and Space on the Reviewed toggle.
- **Header counter (unfiltered):** on the New case page it equals the number of New Cases; an explicit mark on Case details lowers it.
- **Cyclic result:** a new finished Analysis (started through the API while a search phrase was typed and the field focused) returns the Case to New Results within one poll; phrase and focus kept.
- **Polling:** 2 requests to `GET /cases` in 25 s while visible; 0 in 25 s with the tab hidden; returning refetches. No report or analysis request is made from the list.
- **Errors:** a failed background refresh shows a notice, keeps the rows, the phrase and focus, and "Spróbuj ponownie" clears it; a failed first load shows the shared error UI (not the empty-database panel) and retry recovers.
- No console errors other than the injected network failures.

Not covered: Case Number search (#56 has not landed; the field is searched when the API returns it). The RUNNING-keeps-group rule is covered by Vitest (a RUNNING Analysis with no finished one has no way to be held open in the browser with the synthetic source).
