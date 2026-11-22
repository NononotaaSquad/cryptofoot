import { describe, expect, it } from "vitest";

import type { ReviewedMark } from "@/features/case-details/reviewed-mark";

import { buildCaseList, type ReviewedMarks } from "./case-list-model";
import {
  clampPage,
  defaultPageSize,
  firstPages,
  hasPages,
  isPageSize,
  offersPageSize,
  pageCaseList,
  pageCount,
  pageLabel,
  paginate,
  pageSizes,
  pagesMoved,
  rangeLabel,
  searchChanged,
} from "./case-list-pagination";
import type { CaseListItem } from "./case-row-view-model";

const pad = (n: number) => String(n).padStart(3, "0");
const at = (n: number) => new Date(Date.UTC(2026, 10, 20, 0, n)).toISOString();

// Case n has one finished Analysis started at minute n with level n.
function caseOf(n: number): CaseListItem {
  const latest = {
    id: `a${pad(n)}`,
    status: "COMPLETED" as const,
    startedAt: at(n),
    finishedAt: at(n),
    highestConfidenceLevel: n,
    attributionCount: 1,
  };
  return {
    id: `c${pad(n)}`,
    identifiers: { nickname: `osoba_${pad(n)}` },
    createdAt: at(0),
    latestAnalysis: latest,
    latestFinishedAnalysis: latest,
  };
}

function markOf(n: number): ReviewedMark {
  return { id: `a${pad(n)}`, startedAt: at(n), reviewedAt: at(200) };
}

const range = (from: number, to: number) =>
  Array.from({ length: to - from + 1 }, (_, index) => from + index);

// 101 Cases, as the API might return them (Case 1 first): 1-41 have New
// Results, 42-101 are Reviewed. Both group orders are the reverse of the
// API order, so a page cut before sorting would show other Cases.
const answer = range(1, 101).map(caseOf);
const reviewedMarks: ReviewedMarks = new Map(
  range(42, 101).map((n) => [`c${pad(n)}`, markOf(n)])
);
const ids = (rows: readonly { caseId: string }[]) =>
  rows.map((row) => row.caseId);
const cases = (...numbers: number[]) => numbers.map((n) => `c${pad(n)}`);

describe("pageCount and clampPage", () => {
  it("counts no page for no rows, so '1 z 0' cannot appear", () => {
    expect(pageCount(0, 20)).toBe(0);
    expect(clampPage(3, 0, 20)).toBe(0);
  });

  it.each([
    [0, 0],
    [1, 1],
    [20, 1],
    [21, 2],
    [40, 2],
    [41, 3],
  ])("%i rows make %i pages of 20", (rows, pages) => {
    expect(pageCount(rows, 20)).toBe(pages);
  });

  it("keeps an index past the end on the last page and nonsense on the first", () => {
    expect(clampPage(5, 41, 20)).toBe(2);
    expect(clampPage(2, 40, 20)).toBe(1);
    expect(clampPage(-1, 41, 20)).toBe(0);
    expect(clampPage(Number.NaN, 41, 20)).toBe(0);
    expect(clampPage(Number.POSITIVE_INFINITY, 41, 20)).toBe(0);
    expect(clampPage(1.7, 41, 20)).toBe(1);
  });
});

describe("paginate", () => {
  // Walks every page and returns what was shown, page by page.
  function walk(total: number, size: number) {
    const rows = range(1, total);
    const first = paginate(rows, 0, size);
    return Array.from({ length: Math.max(first.pageCount, 1) }, (_, page) =>
      paginate(rows, page, size)
    );
  }

  it.each(
    [0, 1, 20, 21, 40, 41].flatMap((total) =>
      pageSizes.map((size) => [total, size] as const)
    )
  )(
    "%i rows at %i per page: every row exactly once, true ranges",
    (total, size) => {
      const pages = walk(total, size);
      const shown = pages.flatMap((page) => page.rows);
      expect(shown).toEqual(range(1, total));
      pages.forEach((page, index) => {
        expect(page.page).toBe(index);
        expect(page.total).toBe(total);
        expect(page.rows.length).toBeLessThanOrEqual(size);
        if (total === 0) {
          expect(page).toMatchObject({
            rows: [],
            first: 0,
            last: 0,
            pageCount: 0,
          });
        } else {
          expect(page.first).toBe(index * size + 1);
          expect(page.last).toBe(index * size + page.rows.length);
        }
      });
    }
  );

  it("21-40-41 at 20 per page: ranges and labels at both ends", () => {
    const rows = range(1, 41);
    const first = paginate(rows, 0, 20);
    const middle = paginate(rows, 1, 20);
    const last = paginate(rows, 2, 20);
    expect([rangeLabel(first), pageLabel(first)]).toEqual([
      "Wyświetlono 1–20 z 41",
      "Strona 1 z 3",
    ]);
    expect(rangeLabel(middle)).toBe("Wyświetlono 21–40 z 41");
    expect([rangeLabel(last), pageLabel(last)]).toEqual([
      "Wyświetlono 41–41 z 41",
      "Strona 3 z 3",
    ]);
    expect(last.rows).toEqual([41]);
  });

  it("offers a navigator only for more than one page", () => {
    expect(hasPages(paginate([], 0, 20))).toBe(false);
    expect(hasPages(paginate(range(1, 1), 0, 20))).toBe(false);
    expect(hasPages(paginate(range(1, 20), 0, 20))).toBe(false);
    expect(hasPages(paginate(range(1, 21), 0, 20))).toBe(true);
  });

  it("does not change the rows it was given", () => {
    const rows = range(1, 41);
    paginate(rows, 1, 20);
    expect(rows).toEqual(range(1, 41));
  });
});

describe("page sizes", () => {
  it("is 20 by default, with 50 and 100 to choose", () => {
    expect(defaultPageSize).toBe(20);
    expect(pageSizes).toEqual([20, 50, 100]);
    expect(isPageSize(50)).toBe(true);
    expect(isPageSize(30)).toBe(false);
    expect(isPageSize("20")).toBe(false);
  });

  it("offers the choice only when a group has more rows than 20", () => {
    expect(
      offersPageSize(buildCaseList(answer.slice(0, 20), "", new Map()))
    ).toBe(false);
    expect(
      offersPageSize(buildCaseList(answer.slice(0, 21), "", new Map()))
    ).toBe(true);
    // 20 New + 20 Reviewed: each group fits on one page.
    const marks: ReviewedMarks = new Map(
      range(21, 40).map((n) => [`c${pad(n)}`, markOf(n)])
    );
    expect(offersPageSize(buildCaseList(answer.slice(0, 40), "", marks))).toBe(
      false
    );
  });
});

describe("pageCaseList: full answer -> search -> groups -> order -> page", () => {
  it("counts all 101 Cases and shows 20 of each group, ordered over all of them", () => {
    const model = buildCaseList(answer, "", reviewedMarks);
    const paged = pageCaseList(model, firstPages, 20);
    expect(model.matching).toBe(101);
    expect(model.newRows).toHaveLength(41);
    expect(model.reviewedRows).toHaveLength(60);
    // The top 20 by level of all 41, not the first 20 of the API answer.
    expect(ids(paged.newResults.rows)).toEqual(
      cases(...range(22, 41).reverse())
    );
    // The 20 newest of all 60 Reviewed.
    expect(ids(paged.reviewed.rows)).toEqual(
      cases(...range(82, 101).reverse())
    );
    expect([paged.newResults.pageCount, paged.reviewed.pageCount]).toEqual([
      3, 3,
    ]);
  });

  it("walks each group page by page: every Case once, in the group order", () => {
    const model = buildCaseList(answer, "", reviewedMarks);
    const walk = (group: "newResults" | "reviewed", count: number) =>
      range(0, count - 1).flatMap((page) =>
        ids(
          pageCaseList(model, { ...firstPages, [group]: page }, 20)[group].rows
        )
      );
    expect(walk("newResults", 3)).toEqual(ids(model.newRows));
    expect(walk("reviewed", 3)).toEqual(ids(model.reviewedRows));
  });

  it("finds a Case that was beyond the first page and shows it on page 1", () => {
    // Case 1 is 41st of 41 New Results: on page 3 without a search.
    const before = pageCaseList(
      buildCaseList(answer, "", reviewedMarks),
      { newResults: 2, reviewed: 0 },
      20
    );
    expect(ids(before.newResults.rows)).toEqual(cases(1));

    const model = buildCaseList(answer, "OSOBA_001", reviewedMarks);
    const paged = pageCaseList(model, firstPages, 20);
    expect(model.matching).toBe(1);
    expect(ids(paged.newResults.rows)).toEqual(cases(1));
    expect(paged.newResults).toMatchObject({
      page: 0,
      pageCount: 1,
      first: 1,
      last: 1,
      total: 1,
    });
    expect(hasPages(paged.newResults)).toBe(false);
    expect(paged.reviewed.pageCount).toBe(0);
  });

  it("finds a Reviewed Case from the last page of its group", () => {
    // Case 42 is 60th of 60 Reviewed.
    const model = buildCaseList(answer, "osoba_042", reviewedMarks);
    const paged = pageCaseList(model, firstPages, 20);
    expect(ids(paged.reviewed.rows)).toEqual(cases(42));
    expect(model.newRows).toHaveLength(0);
  });

  it("pages the matches only: counts and pages follow the search, not the answer", () => {
    // "osoba_0" matches Cases 1-99: all 41 New and Reviewed 42-99.
    const model = buildCaseList(answer, "osoba_0", reviewedMarks);
    const paged = pageCaseList(model, firstPages, 50);
    expect([
      model.matching,
      model.newRows.length,
      model.reviewedRows.length,
    ]).toEqual([99, 41, 58]);
    expect(paged.newResults.pageCount).toBe(1);
    expect(paged.reviewed.pageCount).toBe(2);
    expect(paged.reviewed.rows[0]?.caseId).toBe("c099");
    expect(rangeLabel(paged.reviewed)).toBe("Wyświetlono 1–50 z 58");
  });

  it("keeps the two groups' pages apart", () => {
    const model = buildCaseList(answer, "", reviewedMarks);
    const paged = pageCaseList(model, { newResults: 2, reviewed: 0 }, 20);
    expect(ids(paged.newResults.rows)).toEqual(cases(1));
    expect(paged.reviewed.rows[0]?.caseId).toBe("c101");
    expect(
      pageCaseList(model, { newResults: 0, reviewed: 1 }, 20).newResults.rows[0]
        ?.caseId
    ).toBe("c041");
  });

  it("a shrinking group keeps the last page that exists, never an empty one", () => {
    const stored = { newResults: 2, reviewed: 0 };
    // Case 1 (alone on New page 3) is marked: 40 New Results remain.
    const marks: ReviewedMarks = new Map([
      ...reviewedMarks,
      ["c001", markOf(1)],
    ]);
    const paged = pageCaseList(buildCaseList(answer, "", marks), stored, 20);
    expect(paged.newResults.page).toBe(1);
    expect(paged.newResults.rows).toHaveLength(20);
    expect(rangeLabel(paged.newResults)).toBe("Wyświetlono 21–40 z 40");
    expect(pagesMoved(stored, paged)).toBe(true);
    // Unchanged data keeps the stored pages.
    expect(
      pagesMoved(
        stored,
        pageCaseList(buildCaseList(answer, "", reviewedMarks), stored, 20)
      )
    ).toBe(false);
  });

  it("a larger page size shows more of the same order", () => {
    const model = buildCaseList(answer, "", reviewedMarks);
    const at50 = pageCaseList(model, firstPages, 50);
    const at100 = pageCaseList(model, firstPages, 100);
    expect(at50.newResults.rows).toHaveLength(41);
    expect(at50.reviewed.pageCount).toBe(2);
    expect(at100.reviewed.rows).toHaveLength(60);
    expect(hasPages(at100.reviewed)).toBe(false);
  });
});

describe("searchChanged", () => {
  it("resets the pages only when the matches can change", () => {
    expect(searchChanged("", "k")).toBe(true);
    expect(searchChanged("kowal", "kowals")).toBe(true);
    expect(searchChanged("kowal", "kowal  ")).toBe(false);
    expect(searchChanged("kowal", "KOWAL")).toBe(false);
    expect(searchChanged("  ", "")).toBe(false);
  });
});
