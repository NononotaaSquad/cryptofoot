// Pure rules of the Reviewed mark (issue #84). The mark of a Case is a
// versioned watermark: the exact Analysis the Analyst marked and its order
// (startedAt, then id - the backend order). Everything newer is New Results.

export interface AnalysisOrder {
  id: string;
  startedAt: string;
}

export interface ReviewedMark extends AnalysisOrder {
  reviewedAt: string;
}

const markVersion = 1;

// Newer first -> positive. Same order as the backend: startedAt, then id.
export function compareAnalysisOrder(
  a: AnalysisOrder,
  b: AnalysisOrder
): number {
  const byTime = Date.parse(a.startedAt) - Date.parse(b.startedAt);
  if (byTime !== 0 && !Number.isNaN(byTime)) return byTime;
  if (a.id === b.id) return 0;
  return a.id > b.id ? 1 : -1;
}

export function serializeMark(mark: ReviewedMark): string {
  return JSON.stringify({ v: markVersion, ...mark });
}

// A legacy value is a bare ISO timestamp: it names no Analysis, so it is not a
// mark. Malformed or unknown versions are not marks either.
export function parseMark(raw: string | null): ReviewedMark | null {
  if (raw === null) return null;
  let value: unknown;
  try {
    value = JSON.parse(raw);
  } catch {
    return null;
  }
  if (typeof value !== "object" || value === null) return null;
  const record = value as Record<string, unknown>;
  const { v, id, startedAt, reviewedAt } = record;
  if (
    v !== markVersion ||
    typeof id !== "string" ||
    id === "" ||
    typeof startedAt !== "string" ||
    Number.isNaN(Date.parse(startedAt)) ||
    typeof reviewedAt !== "string"
  ) {
    return null;
  }
  return { id, startedAt, reviewedAt };
}

// Marking an Analysis never moves an existing newer watermark back; null
// means the stored mark already covers it and nothing is written.
export function markAfterReview(
  current: ReviewedMark | null,
  target: AnalysisOrder,
  reviewedAt: string
): ReviewedMark | null {
  if (current !== null && compareAnalysisOrder(target, current) <= 0) {
    return null;
  }
  return { id: target.id, startedAt: target.startedAt, reviewedAt };
}

// An unmarked, legacy or malformed Case reads as not reviewed.
export function isCovered(
  mark: ReviewedMark | null,
  analysis: AnalysisOrder
): boolean {
  return mark !== null && compareAnalysisOrder(analysis, mark) <= 0;
}

// New Results: the latest finished Analysis (FAILED included) is newer than
// the watermark. A Case without a finished Analysis has none; a RUNNING
// Analysis alone changes nothing.
export function hasNewResults(
  latestFinished: AnalysisOrder | null | undefined,
  mark: ReviewedMark | null
): boolean {
  if (latestFinished === null || latestFinished === undefined) return false;
  return !isCovered(mark, latestFinished);
}

export function toAnalysisOrder(analysis: {
  id?: string;
  startedAt?: string;
}): AnalysisOrder | null {
  if (analysis.id === undefined || analysis.startedAt === undefined) {
    return null;
  }
  return { id: analysis.id, startedAt: analysis.startedAt };
}
