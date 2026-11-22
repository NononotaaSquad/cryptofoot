# ADR-0015: Pages of the Case list in the browser only

**Status:** Accepted (scope fixed by #104: frontend only, no backend or API change)

**Context:**
With many Cases the Case list (#85/#86) rendered every row of both sections on one very long page. Search, the two groups, their order, the counts and the header badge all work on the full `GET /api/1/cases` answer in the browser. A backend page would cut the set that the search runs on, and that endpoint and its contract must stay unchanged.

**Decision:**
- **Order of work:** full API answer (kept whole in the existing TanStack Query cache, same query key) → search over all Cases → New Results / Reviewed groups → each group's order → the current page of each group (`pageCaseList` in `features/case-list/case-list-pagination.ts`). The page is the last step and only limits the rows drawn.
- **No request per page:** the page number and the page size are local screen state of `CaseListView`, never in the request or the query key. Turning pages, changing the size and typing make no request; the existing 10-s poll still re-reads the whole list.
- **Two pages, one size:** "Nowe wyniki" and "Przejrzane sprawy" each have their own page, so one never moves the other. One size applies to both: 20 by default, or 50 or 100 ("Wierszy na stronę w każdej sekcji"). The choice is shown only when a group has more than 20 rows.
- **Navigator:** under each group with more than one page: "Wyświetlono A–B z N", "Poprzednia", "Strona X z Y", "Następna", in a `nav` named "Strony sekcji {group}". It reuses the shared `Pagination` structure but with buttons, not page links, because the page is not an address. No navigator for one page, an empty group or a collapsed Reviewed section; never "1 z 0".
- **Resets:** a new search phrase (one that can change the matches: trimming and letter case do not), "Wyczyść" and a new page size send both groups to page 1. A poll, a refetch or a Reviewed mark never resets the pages. When a group shrinks, its page is clamped to the last page that exists, and the clamped value is kept, so a later poll does not jump back.
- **Counts:** "Liczba spraw", "Nowe wyniki" and the group titles count all matching Cases, not the page. The header badge stays unfiltered and ignores pages.
- **Focus and scrolling:** the pressed button keeps the focus. At the first or last page it becomes `aria-disabled` (Base UI `focusableWhenDisabled`), so the focus is not dropped to the page body. After a mouse or touch press, the group's heading scrolls back into view when it was above the window. After Enter or Space the view does not jump to the heading; the focused button is scrolled to the middle only when the new page pushed it off the screen. The range has `aria-live="polite"`.

**Consequences:**
- **Positive:** the search still finds any Case, including one that was on a later page, and shows it on the first page of the matches. No API, backend or generated-client change. The rules are pure functions with Vitest tests (`case-list-pagination.test.ts`: 0/1/20/21/40/41 rows, ranges, clamp, the 101-Case order of operations).
- **Negative:** the browser still downloads and holds every Case; this limits rendering, not data. At a much larger scale the search would have to move to the backend together with the paging.
- **Neutral:** the page and size are not kept in the address or across reloads; a reload starts on page 1 with 20 rows.
