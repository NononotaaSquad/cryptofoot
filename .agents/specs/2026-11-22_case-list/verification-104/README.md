# Verification record, #104 (Case list pages in the browser only)

**Result: all checks pass.** Frontend only (ADR-0015): no backend, DTO, OpenAPI or generated-client change.

## What was run

| | |
| --- | --- |
| Run | **Local only**, 2026-11-22. |
| Code | Branch `feat/104-case-list-pagination` on `master` `97ee603`; the backend run from the same, unchanged tree. |
| Backend | `./gradlew bootRun`, profile `local`, port 8087, `EXCLUSION_ONCHAIN_CHECK_ENABLED=false`, `SOURCES_SYNTHETIC_BROKEN_ENABLED=false`. Scheduler tick 5 s. |
| Database | Dedicated `list104` on the local Mongo 8. The shared `db` was not touched. |
| Frontend | `next dev` 16.4.6 on port 3087, `NEXT_PUBLIC_API_URL=http://localhost:8087`; TanStack Query 5.107.1. |
| Browser | Edge 158.0.4370.33, headless, `pl-PL`, `Europe/Warsaw`, playwright-core from a scratch folder outside the repo (ADR-0010: nothing added). Desktop 1920×1080, small screen 375×812. |
| Machine | Windows 11, Node 24.16.0, npm 11.13.0, OpenJDK 25.0.3, Docker 29.5.2. |

**Data: 101 Cases through `POST /cases`.**

- 95 have a real Analysis run by the backend; levels and dates were then set in `list104` from a real scored Attribution.
- **New Results, 41 Cases:**
  - Cases 1–40 have levels 2 % to 80 %.
  - Case 41 is S11 `oliwer_zmienny`, with untouched real results.
  - "Osoba Testowa 001" has the lowest level, so it sorts last, on page 3.
- **Non-new group, 60 Cases:**
  - Cases 42–95 have browser marks (the v1 records the button writes, set at the start).
  - Cases 96–101 have no Analysis.

## Quality gate

- `npm run check`: pass (typecheck, lint, format, 327 Vitest tests; 40 new in `case-list-pagination.test.ts`).
- `npm run build` (with `NEXT_PUBLIC_API_URL`): pass.
- No file under `backend/`, `lib/api/openapi.json` or `lib/api/generated/` changed.

## Browser walkthrough (66 checks, all pass)

**Full set, local pages, no paging requests**

- `GET /api/1/cases` (no parameters) returns all 101 Cases. The rendered list draws 20 rows of New Results, with "Liczba spraw: 101 / Nowe wyniki: 41" and "Nowe wyniki (41)", "Przejrzane sprawy (60)". `p1-new-1920.png`, `stats-size-1920.png`
- Network: turning pages, changing the size and typing made no request. Every list request in both runs (4 and 35) was the plain `GET /api/1/cases` poll.
- **Key case:** "Osoba Testowa 001", alone on New page 3, is found by typing "testowa 001": 1 match, shown on page 1, no navigator, no request. `search-beyond-page-1920.png`

**Navigator and edges**

- New: "Wyświetlono 1–20 z 41 / Strona 1 z 3" (Poprzednia disabled), then 21–40, then "41–41 z 41 / Strona 3 z 3" (Następna disabled) with only Case 001. Page 2 starts with the 21st Case of the group order. `pager-new-1920.png`, `p3-new-1920.png`
- A disabled button does nothing and keeps the focus.
- Reviewed has its own pages (1–20 z 60). Moving it does not move New, and vice versa. Collapsed, it shows neither rows nor navigator; re-expanding keeps its page.
- The two navigators are named "Strony sekcji Nowe wyniki" and "Strony sekcji Przejrzane sprawy". `both-groups-1920.png`
- 50 per page: New shows all 41 with no navigator, Reviewed "1–50 z 60 / Strona 1 z 2". 100: no navigators. Each change returns both groups to page 1.

**Resets**

- "testowa 0" (98 matches, New 40, Reviewed 58 auto-expanded): both on page 1.
- Trailing spaces or other letter case keep the page; "osoba" (a new phrase) returns to page 1.
- "kowalsky" then "Wyczyść": page 1, focus in the field, Reviewed back to the choice made before the search.
- Emptying the field: page 1.

**Refetch, marks, cyclic result**

- Marking the only Case on New page 3 in a second tab gives "Nowe wyniki (40)" without reload. The page is clamped to "Strona 2 z 2 / Wyświetlono 21–40 z 40" with 20 rows, never an empty page. `clamp-after-mark.png`
- Undo gives "(41)" and the Analyst stays on page 2 ("Strona 2 z 3"), with no reset and no jump back to 3.
- **Real scheduled run (backend scheduler, 1-minute custom interval).** S11 was marked, so New had 40 and the list was on page 2 with focus on "Poprzednia". The scheduled Analysis finished and the list showed "Nowe wyniki (41)" 1–4 s later without a refresh. It stayed on "Strona 2 z 3 / Wyświetlono 21–40 z 41" with the focus kept. The badge on the details page went 40 → 41, and S11 is back on page 1 in level order. `cyclic-new-page1.png`
- The badge on the New case page shows 41, all New Results, regardless of pages and search.

**Errors**

- A failed background refresh (503 injected in the browser) shows the notice and keeps page 2, its 20 rows and the counts (101 / 41). "Spróbuj ponownie" clears it, still on page 2. `refresh-error-page2.png`
- A failed first load shows the shared error UI, no navigator and no empty panel; retry loads page 1 of 3.

**Keyboard, focus, small screen**

- A disabled "Poprzednia" stays focusable. Tab, then Enter or Space, pages forward and the focus stays on the button.
- The focused button stays on screen after Enter or Space, including when the page shrinks to 1 row or grows back to 20 (it is scrolled to the middle only when pushed off-screen). `focus-pager.png`
- After a mouse click at the bottom of a long page, the group heading comes back to the top of the view.
- The page size can be changed by keyboard (Enter, ArrowDown, Enter).
- 375 px: no horizontal scroll. "Strona X z Y" sits on its own line above the two buttons, which stay side by side, 40 px tall, inside the screen. `pager-375.png`
- Console: only the pre-existing app-wide `404` for `/favicon.ico` (see the #87 record) and the injected 503s.

## Method notes

Three harness problems were fixed and the affected checks run again:

1. The expected page count for "osoba" was wrong in the script: 100 matches, New 40, so 2 pages.
2. A headless background tab does not paint, so screenshots there time out. Tabs are brought to the front first.
3. After a first full run, data had moved on (S11 had a newer scheduled result), so the script marks S11's latest finished Analysis.

The first run also showed a real issue, fixed before the final runs: after a keyboard page change the view jumped to the heading and left the focused button off-screen. Keyboard activation now keeps the button visible; only a pointer press scrolls to the heading.

## Not covered

- Production.
- A real minimised window (polling while hidden was covered in #87).
- A screen reader was not run; the live region and names were read from the page.
