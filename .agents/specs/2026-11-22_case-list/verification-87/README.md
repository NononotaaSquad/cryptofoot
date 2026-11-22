# Verification record, #87 (Case list: integration and the four states)

**Result: every scenario passes except the Case Number, which is blocked by #56.** No defect was found in the list code, so this record changes no application code. #87 stays open until #56 lands and number display and search are verified.

## What was run

| | |
| --- | --- |
| Run | **Local only**, 2026-11-22. Nothing was tried against production. |
| Code under test | `master` `07a01f8` (#83, #84, #85, #86 merged). #56 is open: `CaseResponse` has no `caseNumber`. |
| Backend | `./gradlew bootRun`, profile `local`, port 8087, `EXCLUSION_ONCHAIN_CHECK_ENABLED=false`, `SOURCES_SYNTHETIC_BROKEN_ENABLED=false` (as in production). Synthetic sources only, scheduler tick 5 s. |
| Database | Dedicated `list87` on the local Mongo 8. The shared `db` and the other databases were not touched. |
| Frontend | `next dev` 16.4.6 on port 3087, `NEXT_PUBLIC_API_URL=http://localhost:8087`, `NEXT_PUBLIC_DEMO_SCHEDULE=true`; TanStack Query 5.107.1. |
| Browser | Edge 158.0.4370.33, headless, `pl-PL`, `Europe/Warsaw`, driven by playwright-core from a scratch folder outside the repo (ADR-0010: nothing added to the repo). |
| Machine | Windows 11, Node 24.16.0, npm 11.13.0, OpenJDK 25.0.3, Docker 29.5.2. |

**Data.** Cases were created through `POST /cases`, and each Analysis was a real run started through `POST /cases/{id}/analyses`. To reproduce the mockups, status, dates and scores were then set in `list87`, copying a real scored Attribution (scenario S1). The held RUNNING Analysis and the legacy Analysis without a result were also inserted there. S11 (`oliwer_zmienny`) used only real manual and scheduled runs. Reviewed marks were set and removed through the real buttons on Case details.

## Quality gate (after the walkthrough, same SHA)

- `./gradlew check integrationTest`: pass (345 unit specs, 253 integration specs, 1 skipped, 0 failures).
- `./scripts/api-sync.sh`: no drift (`git status` clean after regenerating `openapi.json` and `lib/api/generated/`).
- `npm run check`: pass (typecheck, lint, format, 287 Vitest tests). `next build`: pass.

## Four mockups (1920 px viewport; the references are 5760 px = 1920 px at 3×)

| State | Screenshot | Matches | Deviations |
| --- | --- | --- | --- |
| A, no Cases | `A-no-cases-1920.png`, `A-no-cases-375.png` | Panel, both texts, `← Rozpocznij od nowej sprawy` → `/`; no search, no stats | – |
| B, Reviewed collapsed | `B-collapsed-1920.png` | 1200 px column, search + hint, `Liczba spraw: 7 / Nowe wyniki: 3`, columns, order 95 → 85 → 62, blue bar + tint only ≥80, bold New labels, status icons, `Przejrzane sprawy (4)` + `Rozwiń` | No Case Number (#56). Identifier labels and order follow the spec, not the image (see below). Kowalski's identifiers wrap onto a 2nd line. |
| C, Reviewed expanded | `C-expanded-1920.png` | Order W toku → wallet → Nieudana → 45%; `W toku –`, `Brak powiązań`, `Nieudana –`, dates in Warsaw time; no blue; regular labels | Same as B; the wallet is lowercase (the backend stores the canonical address). |
| D, no match | `D-no-matches-1920.png`, `D-375.png` | `Nie znaleziono sprawy dla „kowalsky”.`, hint, `Wyczyść`; phrase stays in the field | – |

Deviations in all states that follow the spec (no change needed):

- **Labels.** The list reuses `identifierKindLabels`, as the spec requires: `Adres e-mail` (image: `E-mail`), `Identyfikator z komunikatora` (`Komunikator`), `Adres portfela` (`Adres kryptowalutowy`), `Adres profilu` (`Profil`).
- **Order.** The spec order is nickname, login, email, messengerId, profileUrl, walletAddress. So Nowak shows `Login · Adres e-mail` (image: e-mail first), and Zieliński shows `Nick · Adres e-mail · +1 więcej` (image: profile first).
- **Type sizes.** Slightly smaller than in the images: row title 18 px, search field 480 px wide (image about 560 px).

## Acceptance scenarios of #87

| Scenario | Result | Evidence |
| --- | --- | --- |
| No Cases → A, working link to the New case page | **Pass** (mouse and keyboard; 375 px no overflow) | `A-*.png` |
| Several Cases → B/C; all 4 statuses and a Case without an Analysis; counter, **number** and order | **Pass except the number (blocked by #56)** | `B-*.png`, `C-*.png`, `values-1920.png` |
| Search by **number**, name and every identifier, full address; a Reviewed match expands the group | **Pass except the number (blocked by #56)**. Name, nick, login, e-mail (any case), messenger id, full profile URL, full BTC address hidden behind `+2 więcej`, full ETH address typed checksummed and stored lowercase, substring, trimming. `.*` is literal; markup renders as text. | `search-auto-expand.png` |
| `kowalsky` → D with the phrase; Clear restores the list, focus and previous expansion | **Pass** (collapsed and expanded choice; a collapse made during a search is honoured and then dropped) | `D-*.png` |
| Opening a report marks nothing; explicit mark and undo update the list and badge; reload and second tab agree | **Pass** (no `localStorage` write on open; the second tab follows through the `storage` event; reload keeps the mark; undo removes the key of that Case only) | `badge-new-case-page.png` |
| A open, newer B finishes before the click on A's button → B stays new; the older report does not mark B | **Pass** (real runs; the mark names A; opening A after marking B leaves the watermark at B) | `details-older-report-reviewed.png` |
| A Scheduled result reaches the list and the badge without a manual refresh and does not replace the report being read; RUNNING shows no old confidence | **Pass** (backend scheduler, 1-minute custom interval: the list moved S11 to New 3–7 s after the run ended, with the phrase and focus kept; badge +1; both details tabs received the new Analysis yet kept showing the one being read, with `?analysis=` and pinned without it. A RUNNING row shows `W toku –` in both groups, and a New Case keeps its group while running.) | `cyclic-new-scheduled.png`, `details-reading-B-after-C.png`, `new-running.png` |
| Zero Attributions, positive count with confidence 0, unavailable data are distinct | **Pass**: `Brak powiązań` / `0% 2 powiązania` (not blue) / `–`, announced as "brak danych do pokazania". 80% blue, 79% not. | `values-1920.png` |
| API error and retry, refresh error: no fake empty list, query and expansion kept | **Pass** (503 injected in the browser: first load shows `Coś poszło nie tak … (błąd 503)` + retry; a failed poll keeps rows, counts, phrase, expansion and focus, shows a notice, and retry clears it) | `initial-error.png`, `refresh-error.png` |
| No new console errors; no hand edit of the generated API; earlier reports unchanged | **Pass** (see console note; api-sync has no drift; the 12 stored Analyses hashed before the later scenarios were byte-identical afterwards) | – |

Also checked:

- **Keyboard and focus.** Tab goes from search to rows to `Rozwiń`. Enter and Space toggle the group. Enter opens a row. The focus ring is visible. `focus-first-row.png`
- **Long values.** Nothing is clipped and the page doesn't scroll sideways at 1920 or 375 px; long tokens wrap. `long-values-*.png`, `list-375.png`
- **Network.**
  - While visible, `GET /cases` runs every 10 s (3 calls in 31 s) with 14 Cases on screen.
  - The list makes 0 report, history or mention requests, and polling sends no POST.
  - A hidden tab makes 0 calls in 25 s (emulated through the Page Visibility API).
  - Returning to the tab refetches at once, keeping phrase and focus, and the interval resumes.

## Findings

- **Blocker, #56.** No Case Number in the API, so B/C show none and number search cannot work (`2026/0006` gives state D). The list already reads and searches `caseNumber` once it is returned. After #56: check the number in B/C at 1920 and 375 px, search by full and partial number, and old Cases without a number.
- **Pre-existing, app-wide, outside the list.** The app has no icon (`app/icon.*` or `favicon.ico`), so every page logs one `404` for `/favicon.ico`. It was the only console error besides the injected 503s.
- **Polish (not a blocker).** On a blue (≥80) row the focus ring (`ring-ring/50` on `bg-primary-tint`) is visible but has low contrast.

## Method notes

Three script assertions failed because of the harness, not the app; each was checked again:

1. A regex expected a space after `·`, but the gap is a CSS margin, so the visible text is right.
2. The desktop-hidden cell names (`md:sr-only`) were counted as clipped elements.
3. A synthetic `visibilitychange` was dispatched without `bubbles: true`, so TanStack Query, which listens on `window`, missed it. With a bubbling event the refetch on return passes.

In the first scheduled run, the schedule was switched off before the details pages' 30-s history poll, so they never received the new Analysis. The run was repeated with the schedule kept on until they did.

## Not covered

- Production (this run is local only).
- A real minimised or background window: the hidden tab was emulated through the Page Visibility API.
- A screen reader was not run; only the hidden texts were read from the page.
