import {
  analysisStatusLabels,
  formatDateTime,
} from "@/features/analysis-report/report-labels";
import type {
  AnalysisResponse,
  AnalysisResponseStatus,
  CaseResponse,
  PersonIdentifiersResponse,
} from "@/lib/api/generated/model";
import {
  caseLabel,
  identifierKindLabels,
  presentNarrowingIdentifiers,
  type NarrowingIdentifierKind,
} from "@/lib/identifiers/identifier-kinds";

// What one row of the Case list shows. Status, date, highest Confidence Level
// and the number of leads all describe `latestAnalysis`, never an older one;
// only the link may point at the latest finished Analysis (issue #85).

// The Case Number comes from #56 (`CaseResponse.caseNumber`). The generated
// type does not carry it yet; the intersection accepts the response before and
// after the client is regenerated. A missing number is hidden, never made up.
export type CaseListItem = CaseResponse & { caseNumber?: string | null };

export const highConfidenceThreshold = 80;

const none = "–";

export type RowStatusKey = AnalysisResponseStatus | "NONE";

export interface RowStatus {
  key: RowStatusKey;
  label: string;
}

export type RowConfidence =
  // "–": no result to show (no Analysis, RUNNING, FAILED) or unknown data.
  | { kind: "none"; text: string }
  // The Analysis finished and found no leads.
  | { kind: "noLeads"; text: string }
  | { kind: "level"; text: string; count: string | null; highlight: boolean };

export interface RowIdentifier {
  label: string;
  // Possibly shortened for the eye; `full` is what a screen reader reads.
  text: string;
  full: string;
}

export interface CaseRowView {
  caseId: string;
  title: string;
  titleFull: string;
  caseNumber: string | null;
  identifiers: RowIdentifier[];
  hiddenIdentifierCount: number;
  status: RowStatus;
  confidence: RowConfidence;
  date: string;
  href: string;
}

const leadPlural = new Intl.PluralRules("pl-PL");

/** "1 powiązanie", "3 powiązania", "5 powiązań". */
export function leadCountText(count: number): string {
  const word =
    leadPlural.select(count) === "one"
      ? "powiązanie"
      : leadPlural.select(count) === "few"
        ? "powiązania"
        : "powiązań";
  return `${String(count)} ${word}`;
}

const walletKeepHead = 6;
const walletKeepTail = 4;
const profileMaxLength = 36;

/** "0x71C7…976F": the ends of a long address, enough to recognise it. */
export function shortenWallet(address: string): string {
  return address.length <= walletKeepHead + walletKeepTail + 4
    ? address
    : `${address.slice(0, walletKeepHead)}…${address.slice(-walletKeepTail)}`;
}

function shortenProfile(url: string): string {
  const bare = url.replace(/^https?:\/\/(www\.)?/i, "");
  return bare.length <= profileMaxLength
    ? bare
    : `${bare.slice(0, profileMaxLength)}…`;
}

function shortenValue(kind: NarrowingIdentifierKind, value: string): string {
  if (kind === "walletAddress") return shortenWallet(value);
  if (kind === "profileUrl") return shortenProfile(value);
  return value;
}

export function toRowStatus(
  analysis: AnalysisResponse | null | undefined
): RowStatus {
  const status = analysis?.status;
  if (status === undefined) return { key: "NONE", label: "Bez analizy" };
  return { key: status, label: analysisStatusLabels[status] };
}

/**
 * Highest level and number of leads of ONE Analysis. Only a finished result
 * (COMPLETED or PARTIAL) has them: RUNNING has none yet and a FAILED one is
 * not a result to rank. 0% with leads is a value; no leads is its own text;
 * anything the backend did not state is unknown ("–"), never zero.
 */
export function toRowConfidence(
  analysis: AnalysisResponse | null | undefined
): RowConfidence {
  const status = analysis?.status;
  if (status !== "COMPLETED" && status !== "PARTIAL") {
    return { kind: "none", text: none };
  }
  const count = analysis?.attributionCount ?? null;
  const level = analysis?.highestConfidenceLevel ?? null;
  if (count === 0) return { kind: "noLeads", text: "Brak powiązań" };
  if (level === null) return { kind: "none", text: none };
  return {
    kind: "level",
    text: `${String(level)}%`,
    count: count === null ? null : leadCountText(count),
    highlight: level >= highConfidenceThreshold,
  };
}

/**
 * The moment a row shows and sorts by: a finished Analysis ended (start as a
 * fallback), a running one started. Null when there is none or it is unreadable.
 */
export function analysisInstant(
  analysis: AnalysisResponse | null | undefined
): string | null {
  if (analysis === null || analysis === undefined) return null;
  const instant =
    analysis.status === "RUNNING"
      ? analysis.startedAt
      : (analysis.finishedAt ?? analysis.startedAt);
  if (instant === undefined || Number.isNaN(Date.parse(instant))) return null;
  return instant;
}

export function toRowDate(analysis: AnalysisResponse | null | undefined) {
  const instant = analysisInstant(analysis);
  return instant === null ? none : formatDateTime(instant);
}

/**
 * The report a row opens. A RUNNING latest opens the latest finished one (the
 * running one has no report yet), or itself without one; a terminal latest
 * opens itself; no Analysis opens the Case, which shows its empty state.
 */
export function toRowHref(
  caseId: string,
  latest: AnalysisResponse | null | undefined,
  latestFinished: AnalysisResponse | null | undefined
): string {
  const base = `/cases/${encodeURIComponent(caseId)}`;
  const target =
    latest?.status === "RUNNING" ? (latestFinished ?? latest) : latest;
  return target?.id === undefined
    ? base
    : `${base}?analysis=${encodeURIComponent(target.id)}`;
}

const maxIdentifiers = 2;

function toRowIdentifiers(identifiers: PersonIdentifiersResponse) {
  const present = presentNarrowingIdentifiers(identifiers);
  return {
    shown: present
      .slice(0, maxIdentifiers)
      .map(({ kind, value }): RowIdentifier => ({
        label: identifierKindLabels[kind],
        text: shortenValue(kind, value),
        full: value,
      })),
    hidden: Math.max(0, present.length - maxIdentifiers),
  };
}

function toTitle(identifiers: PersonIdentifiersResponse) {
  const label = caseLabel(identifiers) ?? "Sprawa";
  const isWallet = identifiers.walletAddress?.trim() === label;
  return { full: label, text: isWallet ? shortenWallet(label) : label };
}

export function toCaseRow(item: CaseListItem): CaseRowView | null {
  if (item.id === undefined) return null;
  const identifiers = item.identifiers ?? {};
  const latest = item.latestAnalysis;
  const title = toTitle(identifiers);
  const { shown, hidden } = toRowIdentifiers(identifiers);
  const number = item.caseNumber?.trim() ?? "";
  return {
    caseId: item.id,
    title: title.text,
    titleFull: title.full,
    caseNumber: number === "" ? null : number,
    identifiers: shown,
    hiddenIdentifierCount: hidden,
    status: toRowStatus(latest),
    confidence: toRowConfidence(latest),
    date: toRowDate(latest),
    href: toRowHref(item.id, latest, item.latestFinishedAnalysis),
  };
}

/** Rows in the order received; an item without an id cannot be opened. */
export function toCaseRows(items: readonly CaseListItem[]): CaseRowView[] {
  return items.flatMap((item) => {
    const row = toCaseRow(item);
    return row === null ? [] : [row];
  });
}
