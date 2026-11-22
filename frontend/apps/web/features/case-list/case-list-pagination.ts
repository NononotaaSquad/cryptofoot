import { normalizeQuery, type CaseListModel } from "./case-list-model";
import type { CaseRowView } from "./case-row-view-model";

// Local pages of the Case list (issue #104). They limit only how many rows are
// drawn: `GET /cases` still returns every Case, the query cache keeps the full
// answer, and search, groups and order run on all of it first
// (full answer -> search -> groups -> order -> page). The page number and the
// page size are screen state, never part of the API request or query key.

export const pageSizes = [20, 50, 100] as const;
export type PageSize = (typeof pageSizes)[number];
export const defaultPageSize: PageSize = 20;

export function isPageSize(value: unknown): value is PageSize {
  return pageSizes.some((size) => size === value);
}

/** Pages needed for `total` rows; 0 for none, so "1 z 0" can never appear. */
export function pageCount(total: number, pageSize: number): number {
  if (total <= 0) return 0;
  return Math.ceil(total / pageSize);
}

/**
 * The page (0-based) that exists for `total` rows: past the end it is the
 * last page, so a group that shrank never shows an empty page; with no rows
 * or a nonsense index it is the first.
 */
export function clampPage(
  page: number,
  total: number,
  pageSize: number
): number {
  const count = pageCount(total, pageSize);
  if (count === 0 || !Number.isFinite(page) || page < 0) return 0;
  return Math.min(Math.floor(page), count - 1);
}

export interface Page<T> {
  rows: T[];
  /** 0-based, already clamped. */
  page: number;
  pageCount: number;
  /** 1-based positions of the first and last row shown; 0 when empty. */
  first: number;
  last: number;
  /** All rows of the group: what the counts and the range refer to. */
  total: number;
}

export function paginate<T>(
  rows: readonly T[],
  page: number,
  pageSize: number
): Page<T> {
  const total = rows.length;
  const current = clampPage(page, total, pageSize);
  const start = current * pageSize;
  const shown = rows.slice(start, start + pageSize);
  return {
    rows: shown,
    page: current,
    pageCount: pageCount(total, pageSize),
    first: shown.length === 0 ? 0 : start + 1,
    last: start + shown.length,
    total,
  };
}

/** A navigator only when there is somewhere to go. */
export function hasPages(page: Page<unknown>): boolean {
  return page.pageCount > 1;
}

/** "Strona 2 z 3". */
export function pageLabel(page: Page<unknown>): string {
  return `Strona ${String(page.page + 1)} z ${String(page.pageCount)}`;
}

/** "Wyświetlono 21–40 z 41". */
export function rangeLabel(page: Page<unknown>): string {
  return `Wyświetlono ${String(page.first)}–${String(page.last)} z ${String(page.total)}`;
}

/** The current page of each group, kept apart so one never moves the other. */
export interface GroupPages {
  newResults: number;
  reviewed: number;
}

export const firstPages: GroupPages = { newResults: 0, reviewed: 0 };

export interface CaseListPages {
  newResults: Page<CaseRowView>;
  reviewed: Page<CaseRowView>;
}

/**
 * The last step: cuts each group of an already searched, grouped and sorted
 * model into its current page. The model's counts stay those of all matching
 * Cases.
 */
export function pageCaseList(
  model: CaseListModel,
  pages: GroupPages,
  pageSize: PageSize
): CaseListPages {
  return {
    newResults: paginate(model.newRows, pages.newResults, pageSize),
    reviewed: paginate(model.reviewedRows, pages.reviewed, pageSize),
  };
}

/** The clamped pages differ from the stored ones (a group shrank). */
export function pagesMoved(stored: GroupPages, paged: CaseListPages): boolean {
  return (
    stored.newResults !== paged.newResults.page ||
    stored.reviewed !== paged.reviewed.page
  );
}

/**
 * A page size changes something only when a group has more rows than the
 * smallest size; otherwise every size shows one page and the choice is hidden.
 */
export function offersPageSize(model: CaseListModel): boolean {
  return (
    Math.max(model.newRows.length, model.reviewedRows.length) > pageSizes[0]
  );
}

/**
 * Whether a new search phrase can change the matches, so the pages go back to
 * the first. Matching trims and ignores case, so neither does that.
 */
export function searchChanged(previous: string, next: string): boolean {
  return (
    normalizeQuery(previous).toLowerCase() !==
    normalizeQuery(next).toLowerCase()
  );
}
