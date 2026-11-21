import type {
  AnalysisResponse,
  AnalysisResponseStatus,
  MentionResponse,
  PersonIdentifiersResponse,
  ReportResponse,
  SourceCoverageResponse,
} from "@/lib/api/generated/model";

import {
  analysisStatusLabels,
  analysisTriggerLabels,
  formatDateTime,
  formatTime,
  reportLimitationSentence,
  reportNotices,
  sourceLabels,
} from "./report-labels";

/**
 * Which top-level block the report shows.
 * - `running`: summary and "Analiza jeszcze trwa"; no sections yet.
 * - `failed`: summary and the grey block in place of the Attributions.
 * - `partial`: full report with the partial notice in the summary.
 * - `completed`: full report with at least one Attribution.
 * - `noAttributions`: completed, but no Attribution (task 03 renders the
 *   "Powiązania (0)" empty state).
 */
export type ReportState =
  "running" | "failed" | "partial" | "completed" | "noAttributions";

export interface ReportSourceLine {
  name: string;
  /** "· sprawdzono o 14:32", or `null` when the source has no check time. */
  checkedAtText: string | null;
  /** "Dane testowe" for a source built on synthetic data. */
  testDataLabel: string | null;
  /** "· niedostępne", shown as a warning. */
  unavailableText: string | null;
}

export interface ReportSummaryView {
  /** "Raport analizy z 21.11.2026, 14:32" */
  heading: string;
  /** `key` picks the icon; `label` is the word (not color alone). */
  status: { key: AnalysisResponseStatus; label: string };
  triggerLabel: string;
  startedAt: string;
  /** `null` while the Analysis runs. */
  finishedAt: string | null;
  /** "Sprawdzone źródła (N z M)" */
  sourcesHeading: string;
  sources: ReportSourceLine[];
  networks: string[];
  /** One sentence per Report Limitation. */
  reportLimitations: string[];
  /** Running, partial or failed sentence; `null` for a completed Analysis. */
  notice: string | null;
}

/**
 * What the report components render; they never read API types. Task 03
 * extends this with the Attributions, Excluded and Rejected sections.
 */
export interface ReportView {
  state: ReportState;
  summary: ReportSummaryView;
}

/**
 * Seam S1: the one public entry from API data to the report view model.
 * `mentions` and `caseIdentifiers` feed the sections of task 03 (Rejected
 * Addresses, the input-address flag); the summary does not need them.
 */
export function toReportView(
  report: ReportResponse,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- read by the sections of task 03; part of the fixed S1 signature
  mentions: MentionResponse[],
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- read by the sections of task 03; part of the fixed S1 signature
  caseIdentifiers: PersonIdentifiersResponse
): ReportView {
  const analysis = report.analysis ?? {};
  const status = analysisStatus(analysis);
  const attributionCount = report.attributions?.length ?? 0;

  return {
    state: reportState(status, attributionCount),
    summary: summaryView(analysis, status, report.limitations ?? []),
  };
}

// A report without a status is treated as failed: the view must never claim
// a result it cannot show. The backend always sends it (contract fields are
// optional only because springdoc marks them so).
function analysisStatus(analysis: AnalysisResponse): AnalysisResponseStatus {
  return analysis.status ?? "FAILED";
}

function reportState(
  status: AnalysisResponseStatus,
  attributionCount: number
): ReportState {
  switch (status) {
    case "RUNNING":
      return "running";
    case "FAILED":
      return "failed";
    case "PARTIAL":
      return "partial";
    case "COMPLETED":
      return attributionCount > 0 ? "completed" : "noAttributions";
  }
}

function summaryView(
  analysis: AnalysisResponse,
  status: AnalysisResponseStatus,
  limitationCodes: string[]
): ReportSummaryView {
  const sources = analysis.sources ?? [];
  const checkedCount = sources.filter((s) => s.status === "CHECKED").length;
  const startedAt =
    analysis.startedAt === undefined ? "" : formatDateTime(analysis.startedAt);

  return {
    heading: `Raport analizy z ${startedAt}`,
    status: { key: status, label: analysisStatusLabels[status] },
    triggerLabel:
      analysis.trigger === undefined
        ? ""
        : analysisTriggerLabels[analysis.trigger],
    startedAt,
    finishedAt:
      analysis.finishedAt == null ? null : formatDateTime(analysis.finishedAt),
    sourcesHeading: `Sprawdzone źródła (${String(checkedCount)} z ${String(sources.length)})`,
    sources: sources.map(sourceLine),
    networks: analysis.networks ?? [],
    reportLimitations: limitationCodes.map(reportLimitationSentence),
    notice: statusNotice(status),
  };
}

function sourceLine(source: SourceCoverageResponse): ReportSourceLine {
  return {
    name: source.sourceName ?? "",
    checkedAtText:
      source.checkedAt == null
        ? null
        : sourceLabels.checkedAt(formatTime(source.checkedAt)),
    testDataLabel: source.synthetic === true ? sourceLabels.testData : null,
    unavailableText:
      source.status === "UNAVAILABLE" ? sourceLabels.unavailable : null,
  };
}

function statusNotice(status: AnalysisResponseStatus): string | null {
  switch (status) {
    case "RUNNING":
      return reportNotices.running;
    case "PARTIAL":
      return reportNotices.partial;
    case "FAILED":
      return reportNotices.failed;
    case "COMPLETED":
      return null;
  }
}
