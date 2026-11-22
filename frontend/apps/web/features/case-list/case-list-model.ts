import {
  hasNewResults,
  toAnalysisOrder,
  type ReviewedMark,
} from "@/features/case-details/reviewed-mark";

import {
  analysisInstant,
  toCaseRow,
  type CaseListItem,
  type CaseRowView,
} from "./case-row-view-model";

// Pure rules of the Case list (issue #86): search, the New Results / Reviewed
// groups, their order and counts. Cases are counted, never Analyses.

export type ReviewedMarks = ReadonlyMap<string, ReviewedMark>;

/** Every full value the Analyst can search by (not the shortened display). */
function searchableValues(item: CaseListItem): string[] {
  const { identifiers } = item;
  return [
    item.caseNumber,
    identifiers?.fullName,
    identifiers?.nickname,
    identifiers?.login,
    identifiers?.email,
    identifiers?.messengerId,
    identifiers?.profileUrl,
    identifiers?.walletAddress,
  ].flatMap((value) => (value === null || value === undefined ? [] : [value]));
}

/** The query as it is matched: trimmed; empty means "no search". */
export function normalizeQuery(query: string): string {
  return query.trim();
}

/**
 * Literal, case-insensitive substring over the Case Number, the full name and
 * every identifier. No fuzzy matching, and the query is never a pattern.
 */
export function matchesQuery(item: CaseListItem, query: string): boolean {
  const needle = normalizeQuery(query).toLowerCase();
  if (needle === "") return true;
  return searchableValues(item).some((value) =>
    value.toLowerCase().includes(needle)
  );
}

/**
 * New Results: the latest FINISHED Analysis (FAILED included) is newer than
 * the Reviewed mark. A RUNNING Analysis alone changes nothing, and a Case that
 * never finished one has none (it is never "reviewed" either).
 */
export function isNewResults(
  item: CaseListItem,
  marks: ReviewedMarks
): boolean {
  const finished = item.latestFinishedAnalysis;
  if (finished === null || finished === undefined || item.id === undefined) {
    return false;
  }
  return hasNewResults(toAnalysisOrder(finished), marks.get(item.id) ?? null);
}

/** The Cases with New Results, unfiltered: the number on the header link. */
export function countNewResults(
  items: readonly CaseListItem[],
  marks: ReviewedMarks
): number {
  return items.filter((item) => isNewResults(item, marks)).length;
}

function millis(instant: string | null | undefined): number {
  const value =
    instant === null || instant === undefined ? NaN : Date.parse(instant);
  return Number.isNaN(value) ? 0 : value;
}

// The date of the last Analysis, the creation of the Case without one.
function lastActivity(item: CaseListItem): number {
  const instant = analysisInstant(item.latestAnalysis);
  return instant === null ? millis(item.createdAt) : millis(instant);
}

// Known only for a finished result of the latest Analysis: a RUNNING or
// FAILED latest never borrows an older score.
function knownLevel(item: CaseListItem, row: CaseRowView): number | null {
  return row.confidence.kind === "level"
    ? (item.latestAnalysis?.highestConfidenceLevel ?? null)
    : null;
}

interface Entry {
  item: CaseListItem;
  row: CaseRowView;
}

const byIdDescending = (a: Entry, b: Entry) =>
  a.row.caseId === b.row.caseId ? 0 : a.row.caseId < b.row.caseId ? 1 : -1;

function compareNew(a: Entry, b: Entry): number {
  const levelA = knownLevel(a.item, a.row);
  const levelB = knownLevel(b.item, b.row);
  if (levelA !== levelB) {
    if (levelA === null) return 1;
    if (levelB === null) return -1;
    return levelB - levelA;
  }
  return lastActivity(b.item) - lastActivity(a.item) || byIdDescending(a, b);
}

function compareReviewed(a: Entry, b: Entry): number {
  return lastActivity(b.item) - lastActivity(a.item) || byIdDescending(a, b);
}

export interface CaseListModel {
  /** Cases matching the query: "Liczba spraw". */
  matching: number;
  newRows: CaseRowView[];
  reviewedRows: CaseRowView[];
}

/** Filters first, then groups; the counts are those of the matching Cases. */
export function buildCaseList(
  items: readonly CaseListItem[],
  query: string,
  marks: ReviewedMarks
): CaseListModel {
  const fresh: Entry[] = [];
  const reviewed: Entry[] = [];
  for (const item of items) {
    if (!matchesQuery(item, query)) continue;
    const row = toCaseRow(item);
    if (row === null) continue;
    (isNewResults(item, marks) ? fresh : reviewed).push({ item, row });
  }
  return {
    matching: fresh.length + reviewed.length,
    newRows: fresh.sort(compareNew).map((entry) => entry.row),
    reviewedRows: reviewed.sort(compareReviewed).map((entry) => entry.row),
  };
}

/**
 * Whether the Reviewed group is open. While searching, it opens by itself
 * when it has a match unless the Analyst chose otherwise during the search;
 * outside a search it is the Analyst's own choice, kept across searches.
 */
export function reviewedOpen(state: {
  searching: boolean;
  hasMatches: boolean;
  choice: boolean;
  searchOverride: boolean | null;
}): boolean {
  if (!state.searching) return state.choice;
  return state.searchOverride ?? (state.hasMatches ? true : state.choice);
}
