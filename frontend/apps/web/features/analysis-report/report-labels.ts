import type {
  AnalysisResponseStatus,
  AnalysisResponseTrigger,
  ReportResponseLimitationsItem,
} from "@/lib/api/generated/model";

// Every code -> Polish text map of the report (Q27). Task 03 adds the
// Confidence Level bands, Excluded and Rejected reasons and scoring rules here.

export const analysisStatusLabels: Record<AnalysisResponseStatus, string> = {
  RUNNING: "W toku",
  COMPLETED: "Zakończona",
  PARTIAL: "Częściowa",
  FAILED: "Nieudana",
};

export const analysisTriggerLabels: Record<AnalysisResponseTrigger, string> = {
  MANUAL: "Ręczna",
  SCHEDULED: "Zaplanowana",
};

const reportLimitationSentences: Record<ReportResponseLimitationsItem, string> =
  {
    SOURCE_UNAVAILABLE:
      "Co najmniej jedno źródło było niedostępne, więc raport może nie zawierać wszystkich wzmianek.",
    BLOCKCHAIN_DATA_UNAVAILABLE:
      "Nie udało się pobrać danych o aktywności adresów w sieci, więc ta część raportu może być pusta lub niepełna.",
    CLASSIFICATION_UNAVAILABLE:
      "Nie udało się sprawdzić, czy adresy należą do serwisów (np. giełd), więc część powiązań może dotyczyć takich adresów.",
    ANALYSIS_INCOMPLETE:
      "Analiza nie została ukończona, więc raport może być niepełny.",
  };

/**
 * One sentence per Report Limitation. The parameter is a plain string: the
 * backend may send a code newer than the generated client, and it must still
 * be shown, not hidden.
 */
export function reportLimitationSentence(code: string): string {
  return isKnownReportLimitation(code)
    ? reportLimitationSentences[code]
    : `Raport może być niepełny (kod: ${code}).`;
}

function isKnownReportLimitation(
  code: string
): code is ReportResponseLimitationsItem {
  return Object.prototype.hasOwnProperty.call(reportLimitationSentences, code);
}

export const reportNotices = {
  running: "Analiza jeszcze trwa",
  partial: "Analiza zakończyła się częściowo. Raport może być niepełny.",
  failed: "Analiza nie powiodła się",
} as const;

export const sourceLabels = {
  testData: "Dane testowe",
  unavailable: "· niedostępne",
  checkedAt: (time: string) => `· sprawdzono o ${time}`,
} as const;

// Q44: `Intl` pl-PL. The zone is fixed to Europe/Warsaw so the report reads
// the same on every machine (and in tests); the Analysts work in Poland.
const dateTimeFormat = new Intl.DateTimeFormat("pl-PL", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  timeZone: "Europe/Warsaw",
});

const timeFormat = new Intl.DateTimeFormat("pl-PL", {
  hour: "2-digit",
  minute: "2-digit",
  timeZone: "Europe/Warsaw",
});

/** "21.11.2026, 14:32" from an ISO instant. */
export function formatDateTime(isoInstant: string): string {
  return dateTimeFormat.format(new Date(isoInstant));
}

/** "14:32" from an ISO instant. */
export function formatTime(isoInstant: string): string {
  return timeFormat.format(new Date(isoInstant));
}
