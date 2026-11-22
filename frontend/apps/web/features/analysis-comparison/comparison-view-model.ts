import {
  analysisLabel,
  excludedReasonLabel,
} from "@/features/analysis-report/report-labels";
import type {
  AnalysisComparisonResponse,
  AnalysisComparisonResponseComparabilityLimitationsItem,
  AnalysisResponse,
  AttributionChangeResponse,
  AttributionChangeResponseChange,
} from "@/lib/api/generated/model";

/**
 * The Analysis the displayed one is compared with: the newest Analysis of the
 * Case that started strictly before it and ended with results (`COMPLETED` or
 * `PARTIAL`). A `FAILED` one is skipped: against it every Attribution would
 * read as new. `null` when there is none (the first Analysis) or the
 * displayed id is not in the history.
 */
export function previousAnalysisWithResults(
  history: readonly AnalysisResponse[],
  displayedId: string
): AnalysisResponse | null {
  const displayedStart = startOf(
    history.find((analysis) => analysis.id === displayedId)
  );
  if (displayedStart === null) return null;

  let previous: AnalysisResponse | null = null;
  for (const analysis of history) {
    const start = startOf(analysis);
    if (
      analysis.id === undefined ||
      start === null ||
      start >= displayedStart ||
      (analysis.status !== "COMPLETED" && analysis.status !== "PARTIAL")
    ) {
      continue;
    }
    const previousStart = startOf(previous ?? undefined);
    if (previousStart === null || start > previousStart) previous = analysis;
  }
  return previous;
}

function startOf(analysis: AnalysisResponse | undefined): number | null {
  if (analysis?.startedAt === undefined) return null;
  const time = Date.parse(analysis.startedAt);
  return Number.isNaN(time) ? null : time;
}

export type ChangeKind = AttributionChangeResponseChange | "UNKNOWN";

export interface ChangeView {
  key: string;
  address: string;
  network: string;
  kind: ChangeKind;
  /** "Nowe powiązanie" */
  label: string;
  /** "Stopień pewności: wcześniej 55%, teraz 80%." */
  detail: string;
  /** Only for a "not observed" Attribution. */
  absenceNote: { text: string; verifiable: boolean } | null;
}

export interface ComparisonView {
  /** "Zmiany od poprzedniej analizy (N)" */
  heading: string;
  /** "Porównanie z analizą: 21.11.2026, 14:32 · Ręczna · Zakończona." */
  comparedWith: string;
  earlierAnalysisId: string | null;
  limitations: string[];
  changes: ChangeView[];
  /** Shown in place of the list when nothing changed. */
  emptyMessage: string | null;
}

const changeLabels: Record<AttributionChangeResponseChange, string> = {
  NEW: "Nowe powiązanie",
  CONFIDENCE_LEVEL_CHANGED: "Zmieniony stopień pewności",
  EXCLUSION_CHANGED: "Zmienione wykluczenie",
  NOT_OBSERVED: "Nie znaleziono w tej analizie",
};

// What needs the Analyst's attention first: new leads, then changes to known
// ones, then the ones that were not seen again.
const changeOrder: Record<ChangeKind, number> = {
  NEW: 0,
  CONFIDENCE_LEVEL_CHANGED: 1,
  EXCLUSION_CHANGED: 2,
  NOT_OBSERVED: 3,
  UNKNOWN: 4,
};

const comparisonLimitationSentences: Record<
  AnalysisComparisonResponseComparabilityLimitationsItem,
  string
> = {
  IDENTIFIERS_DIFFER:
    "Analizy użyły różnych identyfikatorów osoby, więc mogły znaleźć inne wzmianki.",
  SOURCE_COVERAGE_DIFFERS:
    "Analizy sprawdziły różne źródła albo część źródeł była niedostępna, więc różnice mogą wynikać z dostępności źródeł, a nie ze zmian u osoby.",
  NETWORKS_DIFFER:
    "Analizy objęły różne sieci, więc część adresów mogła nie zostać sprawdzona w obu.",
  ALGORITHM_VERSION_DIFFERS:
    "Stopień pewności obliczono w analizach według różnych reguł, więc jego zmiana może wynikać z reguł, a nie z nowych wzmianek.",
  PARTIAL_ANALYSIS:
    "Co najmniej jedna z analiz zakończyła się częściowo, więc porównanie może być niepełne.",
};

/** One sentence per comparability limitation; an unknown code is still shown. */
export function comparisonLimitationSentence(code: string): string {
  return isKnownCode(comparisonLimitationSentences, code)
    ? comparisonLimitationSentences[code]
    : `Porównanie może być niepełne (kod: ${code}).`;
}

// The backend may send a code newer than the generated client; it is shown as
// is, never hidden.
function isKnownCode<T extends object>(
  map: T,
  code: string
): code is Extract<keyof T, string> {
  return Object.prototype.hasOwnProperty.call(map, code);
}

export const comparisonLabels = {
  empty: "Nie ma zmian w powiązaniach ani w adresach wykluczonych.",
  limitationsHeading: "Ograniczenia porównania",
  showEarlier: "Pokaż poprzednią analizę",
  absenceVerified:
    "Źródła, w których wcześniej znaleziono ten adres, sprawdzono ponownie. Nie oznacza to, że adres przestał należeć do osoby.",
  absenceUnverified:
    "Nie da się tego potwierdzić: ta analiza nie sprawdziła ponownie wszystkich źródeł, w których adres wcześniej znaleziono. Nie oznacza to, że adres przestał należeć do osoby.",
} as const;

export function toComparisonView(
  comparison: AnalysisComparisonResponse
): ComparisonView {
  const changes = (comparison.changes ?? [])
    .map(toChangeView)
    // Array sort is stable: the backend order (network, address) stays
    // within each kind.
    .sort((a, b) => changeOrder[a.kind] - changeOrder[b.kind]);
  const earlier = comparison.earlierAnalysis;

  return {
    heading: `Zmiany od poprzedniej analizy (${String(changes.length)})`,
    comparedWith:
      earlier === undefined
        ? "Porównanie z poprzednią analizą."
        : `Porównanie z analizą: ${analysisLabel(earlier)}.`,
    earlierAnalysisId: earlier?.id ?? null,
    limitations: (comparison.comparabilityLimitations ?? []).map(
      comparisonLimitationSentence
    ),
    changes,
    emptyMessage: changes.length === 0 ? comparisonLabels.empty : null,
  };
}

function toChangeView(change: AttributionChangeResponse): ChangeView {
  const network = change.network ?? "";
  const address = change.address ?? "";
  const kind: ChangeKind =
    change.change !== undefined && isKnownCode(changeLabels, change.change)
      ? change.change
      : "UNKNOWN";

  return {
    key: `${network}:${address}`,
    address,
    network,
    kind,
    label: kind === "UNKNOWN" ? (change.change ?? "") : changeLabels[kind],
    detail: changeDetail(kind, change),
    absenceNote:
      kind === "NOT_OBSERVED"
        ? change.absenceVerifiable === true
          ? { text: comparisonLabels.absenceVerified, verifiable: true }
          : { text: comparisonLabels.absenceUnverified, verifiable: false }
        : null,
  };
}

// Both values are stated as they are; a changed score is not described as a
// change in the person's behaviour (E6.3).
function changeDetail(
  kind: ChangeKind,
  change: AttributionChangeResponse
): string {
  switch (kind) {
    case "NEW":
      return `Stopień pewności: ${percent(change.laterConfidenceLevel)}.`;
    case "NOT_OBSERVED":
      return `Stopień pewności w poprzedniej analizie: ${percent(change.earlierConfidenceLevel)}.`;
    case "CONFIDENCE_LEVEL_CHANGED":
      return `Stopień pewności: wcześniej ${percent(change.earlierConfidenceLevel)}, teraz ${percent(change.laterConfidenceLevel)}.`;
    case "EXCLUSION_CHANGED":
    case "UNKNOWN": {
      const earlier = side(
        change.earlierConfidenceLevel,
        change.earlierExclusionReason
      );
      const later = side(
        change.laterConfidenceLevel,
        change.laterExclusionReason
      );
      return `Wcześniej: ${earlier}. Teraz: ${later}.`;
    }
  }
}

/** "powiązanie, stopień pewności 55%" or "wykluczony jako adres giełdy kryptowalut". */
function side(
  confidenceLevel: number | null | undefined,
  exclusionReason: string | null | undefined
): string {
  if (typeof confidenceLevel === "number") {
    return `powiązanie, stopień pewności ${percent(confidenceLevel)}`;
  }
  if (typeof exclusionReason === "string") {
    const label = excludedReasonLabel(exclusionReason);
    return label === exclusionReason
      ? `wykluczony (powód: ${exclusionReason})`
      : `wykluczony jako ${label.charAt(0).toLowerCase()}${label.slice(1)}`;
  }
  return "brak";
}

function percent(level: number | null | undefined): string {
  return typeof level === "number" ? `${String(level)}%` : "–";
}
