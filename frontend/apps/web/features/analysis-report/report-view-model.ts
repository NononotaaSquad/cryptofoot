import type {
  AnalysisResponse,
  AnalysisResponseStatus,
  AttributionResponse,
  ExcludedAddressResponse,
  MentionResponse,
  PersonIdentifiersResponse,
  ReportResponse,
  SourceCoverageResponse,
} from "@/lib/api/generated/model";

import {
  type AddressActivityFigures,
  toAddressActivityFigures,
} from "./address-activity-figures";
import {
  analysisStatusLabels,
  analysisTriggerLabels,
  attributionLabels,
  confidenceBandLabel,
  excludedReasonLabel,
  formatDateTime,
  formatTime,
  mentionCountText,
  rejectedReasonLabel,
  reportLimitationSentence,
  reportNotices,
  scoringRule,
  sourceLabels,
} from "./report-labels";

/**
 * Which top-level block the report shows.
 * - `running`: summary and "Analiza jeszcze trwa"; no sections yet.
 * - `failed`: summary and the grey block in place of the Attributions.
 * - `partial`: full report with the partial notice in the summary.
 * - `completed`: full report with at least one Attribution.
 * - `noAttributions`: completed, but no Attribution ("Powiązania (0)" empty
 *   state).
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

export interface MentionView {
  key: string;
  sourceName: string;
  /** `null` when the link is not http(s): it is then shown as plain text. */
  url: string | null;
  urlText: string;
  /** "21.11.2026, 14:32", or `null` without a collection time. */
  collectedAt: string | null;
  context: string | null;
}

export interface ScoringLine {
  /** "Adres przy identyfikatorze osoby (GitHub, Forum)", or the raw code. */
  label: string;
  /** "40" for the first line, "+15" for the next ones. */
  points: string;
}

export interface VerificationLine {
  text: string;
  /** `false` is a warning: icon and text, not colour alone. */
  ok: boolean;
}

export interface AttributionView {
  key: string;
  address: string;
  network: string;
  confidence: { percent: number; text: string; bandLabel: string };
  /** "2 wzmianki" */
  mentionCountText: string;
  /** "Adres podany przy uruchomieniu analizy" when it is the Case input. */
  inputAddressNote: string | null;
  /** The unchecked-classification warning of the collapsed row. */
  classificationWarning: string | null;
  scoringLines: ScoringLine[];
  /** "55%", the bold "Stopień pewności" line. */
  confidenceTotal: string;
  justification: string[];
  /** "Wzmianki (2)" */
  mentionsHeading: string;
  mentions: MentionView[];
  verification: VerificationLine[];
  /** "Aktywność adresu w sieci BTC" */
  activityHeading: string;
  activity: AddressActivityFigures;
}

export interface ExcludedAddressView {
  key: string;
  address: string;
  network: string;
  reasonLabel: string;
  classificationSource: string;
  mentions: MentionView[];
}

export interface RejectedAddressView {
  key: string;
  address: string;
  reasonLabel: string;
  /** Every source name, comma-separated (Q40). */
  sourceNames: string;
}

/** What the report components render; they never read API types. */
export interface ReportView {
  state: ReportState;
  summary: ReportSummaryView;
  /** "Powiązania (N)" */
  attributionsHeading: string;
  /** Confidence Level descending, then address. */
  attributions: AttributionView[];
  /** "Adresy wykluczone (N)" */
  excludedHeading: string;
  excludedAddresses: ExcludedAddressView[];
  /** "Adresy odrzucone (N)" */
  rejectedHeading: string;
  rejectedAddresses: RejectedAddressView[];
}

/** Seam S1: the one public entry from API data to the report view model. */
export function toReportView(
  report: ReportResponse,
  mentions: MentionResponse[],
  caseIdentifiers: PersonIdentifiersResponse
): ReportView {
  const analysis = report.analysis ?? {};
  const status = analysisStatus(analysis);
  const walletAddress = caseIdentifiers.walletAddress ?? null;
  const attributions = (report.attributions ?? [])
    .map((attribution) => attributionView(attribution, walletAddress))
    .sort(
      (a, b) =>
        b.confidence.percent - a.confidence.percent ||
        compareText(a.address, b.address)
    );
  const excludedAddresses = (report.excludedAddresses ?? []).map(
    excludedAddressView
  );
  const rejectedAddresses = rejectedAddressViews(mentions);

  return {
    state: reportState(status, attributions.length),
    summary: summaryView(analysis, status, report.limitations ?? []),
    attributionsHeading: `Powiązania (${String(attributions.length)})`,
    attributions,
    excludedHeading: `Adresy wykluczone (${String(excludedAddresses.length)})`,
    excludedAddresses,
    rejectedHeading: `Adresy odrzucone (${String(rejectedAddresses.length)})`,
    rejectedAddresses,
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

function attributionView(
  attribution: AttributionResponse,
  walletAddress: string | null
): AttributionView {
  const address = attribution.address ?? "";
  const network = attribution.network ?? "";
  const percent = attribution.confidenceLevel ?? 0;
  const mentions = attribution.justification ?? [];
  const rules = attribution.scoringRules ?? [];
  const sourceNames = unique(mentions.map((m) => m.sourceName ?? "")).join(
    ", "
  );
  // A missing status counts as unchecked: never claim a check the backend did
  // not report.
  const classified = attribution.classificationStatus === "NOT_EXCLUDED";
  const formatChecked = mentions
    .flatMap((m) => m.detectedAddresses ?? [])
    .some((d) => d.valid === true && sameAddress(d.address ?? "", address));

  return {
    key: `${network}:${address}`,
    address,
    network,
    confidence: {
      percent,
      text: `${String(percent)}%`,
      bandLabel: confidenceBandLabel(percent),
    },
    mentionCountText: mentionCountText(mentions.length),
    inputAddressNote:
      walletAddress !== null && sameAddress(walletAddress, address)
        ? attributionLabels.inputAddress
        : null,
    classificationWarning: classified
      ? null
      : attributionLabels.classificationUnavailable,
    scoringLines: rules.map((rule, index) => {
      const known = scoringRule(rule.rule ?? "");
      const contribution = rule.contribution ?? 0;
      return {
        // An unknown code gets no source names: the frontend cannot tell
        // which Mention triggered it.
        label:
          known === null
            ? (rule.rule ?? "")
            : withSources(known.label, sourceNames),
        points:
          index > 0 && contribution >= 0
            ? `+${String(contribution)}`
            : String(contribution),
      };
    }),
    confidenceTotal: `${String(percent)}%`,
    justification: rules.flatMap((rule) => {
      const known = scoringRule(rule.rule ?? "");
      return known === null ? [] : [known.justification];
    }),
    mentionsHeading: `Wzmianki (${String(mentions.length)})`,
    mentions: mentions.map(mentionView),
    verification: [
      formatChecked
        ? { text: attributionLabels.validFormat, ok: true }
        : { text: attributionLabels.noFormatCheck, ok: false },
      { text: attributionLabels.network(network), ok: true },
      classified
        ? { text: attributionLabels.notExcluded, ok: true }
        : { text: attributionLabels.classificationUnavailable, ok: false },
    ],
    activityHeading: `Aktywność adresu w sieci ${network}`,
    activity: toAddressActivityFigures(attribution.addressActivity),
  };
}

function withSources(label: string, sourceNames: string): string {
  return sourceNames === "" ? label : `${label} (${sourceNames})`;
}

function excludedAddressView(
  excluded: ExcludedAddressResponse
): ExcludedAddressView {
  const address = excluded.address ?? "";
  const network = excluded.network ?? "";
  return {
    key: `${network}:${address}`,
    address,
    network,
    reasonLabel:
      excluded.reason === undefined ? "" : excludedReasonLabel(excluded.reason),
    classificationSource: excluded.classificationSource ?? "",
    mentions: (excluded.justification ?? []).map(mentionView),
  };
}

// Every invalid detected address, one entry per address and reason, with the
// names of all sources it was found in (in the order of the Mentions).
function rejectedAddressViews(
  mentions: MentionResponse[]
): RejectedAddressView[] {
  const byKey = new Map<
    string,
    { address: string; reason: string; sources: string[] }
  >();
  for (const mention of mentions) {
    for (const detected of mention.detectedAddresses ?? []) {
      if (detected.valid !== false) continue;
      const address = detected.address ?? "";
      const reason = detected.validationFailure ?? "";
      const key = `${canonicalAddress(address)}|${reason}`;
      const entry = byKey.get(key) ?? { address, reason, sources: [] };
      entry.sources.push(mention.sourceName ?? "");
      byKey.set(key, entry);
    }
  }
  return [...byKey].map(([key, entry]) => ({
    key,
    address: entry.address,
    reasonLabel: rejectedReasonLabel(entry.reason),
    sourceNames: unique(entry.sources).join(", "),
  }));
}

function mentionView(mention: MentionResponse, index: number): MentionView {
  const url = mention.url ?? "";
  return {
    key: mention.id ?? String(index),
    sourceName: mention.sourceName ?? "",
    url: /^https?:\/\//i.test(url) ? url : null,
    urlText: url,
    collectedAt:
      mention.collectedAt === undefined
        ? null
        : formatDateTime(mention.collectedAt),
    context: mention.context ?? null,
  };
}

// The backend stores ETH hex and bech32 in lower case and Base58 as is, so
// lowering only the first two keeps Base58 case-sensitive.
function canonicalAddress(address: string): string {
  const trimmed = address.trim();
  return /^(0x|bc1)/i.test(trimmed) ? trimmed.toLowerCase() : trimmed;
}

function sameAddress(a: string, b: string): boolean {
  return canonicalAddress(a) === canonicalAddress(b);
}

function unique(values: string[]): string[] {
  return [...new Set(values.filter((value) => value !== ""))];
}

// Code-unit order: the same on every machine, unlike `localeCompare`.
function compareText(a: string, b: string): number {
  if (a === b) return 0;
  return a < b ? -1 : 1;
}
