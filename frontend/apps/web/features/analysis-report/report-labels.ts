import type {
  AddressActivityEventResponseType,
  AnalysisResponseStatus,
  AnalysisResponseTrigger,
  DetectedAddressResponseValidationFailure,
  ExcludedAddressResponseReason,
  ReportResponseLimitationsItem,
} from "@/lib/api/generated/model";

// Every code -> Polish text map of the report (Q27). Lookups that take a plain
// string do so because the backend may send a code newer than the generated
// client; such a code is shown as is, never hidden.

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
  return isKnownCode(reportLimitationSentences, code)
    ? reportLimitationSentences[code]
    : `Raport może być niepełny (kod: ${code}).`;
}

function isKnownCode<T extends object>(
  map: T,
  code: string
): code is Extract<keyof T, string> {
  return Object.prototype.hasOwnProperty.call(map, code);
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

/** Q39: every level below 40% is "Niski / szum". */
export function confidenceBandLabel(level: number): string {
  if (level >= 80) return "Bardzo wysokie prawdopodobieństwo";
  if (level >= 60) return "Prawdopodobny";
  if (level >= 40) return "Średni";
  return "Niski / szum";
}

// Known codes of `confidence-1` and saved `provisional-1` Analyses.
// `withSources` marks rules whose line names the Mention sources in brackets.
const scoringRules = {
  MENTIONED_WITH_IDENTIFIER: {
    label: "Adres przy identyfikatorze osoby",
    withSources: true,
  },
  ENTERED_BY_ANALYST: {
    label: "Adres wpisany przez analityka",
    withSources: false,
  },
  OWN_PROFILE: { label: "Adres na profilu osoby", withSources: true },
  SALE_OR_SETTLEMENT: {
    label: "Ogłoszenie sprzedaży lub rozliczenie",
    withSources: true,
  },
  DISCUSSION: { label: "Adres w dyskusji", withSources: true },
  MASS_COMMENT: { label: "Wpis konkursowy lub promocyjny", withSources: true },
  NO_PERSON_LINK: { label: "Brak powiązania z osobą", withSources: false },
  INDEPENDENT_SOURCES: {
    label: "Potwierdzenie w niezależnych źródłach",
    withSources: true,
  },
  SAME_SERVICE: {
    label: "Kolejna wzmianka z tego samego serwisu",
    withSources: false,
  },
  COPIED_TEXT: { label: "Skopiowana treść", withSources: false },
  NICKNAME_ONLY: {
    label: "Powiązanie tylko przez nick lub login",
    withSources: false,
  },
  WARNING_CONTEXT: { label: "Adres w ostrzeżeniu", withSources: false },
  CONFLICTING_EMAIL: { label: "E-mail innej osoby", withSources: false },
  CLASSIFICATION_UNAVAILABLE: {
    label: "Klasyfikacja niedostępna",
    withSources: false,
  },
};

/** Punktacja label of a rule; `null` for an unknown code. */
export function scoringRule(
  code: string
): { label: string; withSources: boolean } | null {
  return isKnownCode(scoringRules, code) ? scoringRules[code] : null;
}

export const attributionLabels = {
  inputAddress: "Adres podany przy uruchomieniu analizy",
  missingJustification: "Brak zapisanego uzasadnienia dla tej analizy",
  classificationUnavailable:
    "Nie sprawdzono, czy adres należy do serwisu (np. giełdy).",
  notExcluded:
    "Adres nie należy do znanego serwisu (giełdy, smart kontraktu, miksera, zbiórki).",
  validFormat: "Poprawny format adresu",
  noFormatCheck: "Brak danych o sprawdzeniu formatu adresu.",
  network: (network: string) => `Sieć: ${network}`,
  confidenceTotal: "Stopień pewności",
} as const;

const mentionPlural = new Intl.PluralRules("pl-PL");
const mentionNouns: Partial<Record<Intl.LDMLPluralRule, string>> = {
  one: "wzmianka",
  few: "wzmianki",
  many: "wzmianek",
};

/** "1 wzmianka", "2 wzmianki", "5 wzmianek". */
export function mentionCountText(count: number): string {
  const noun = mentionNouns[mentionPlural.select(count)] ?? "wzmianki";
  return `${String(count)} ${noun}`;
}

const excludedReasonLabels: Record<ExcludedAddressResponseReason, string> = {
  EXCHANGE: "Adres giełdy kryptowalut",
  SMART_CONTRACT: "Adres programu w sieci (smart kontrakt)",
  MIXER: "Adres miksera",
  FUNDRAISER: "Adres zbiórki publicznej",
};

export function excludedReasonLabel(code: string): string {
  return isKnownCode(excludedReasonLabels, code)
    ? excludedReasonLabels[code]
    : code;
}

const rejectedReasonLabels: Record<
  NonNullable<DetectedAddressResponseValidationFailure>,
  string
> = {
  MALFORMED: "Błędny format",
  CHECKSUM_MISMATCH: "Błędna suma kontrolna",
  UNSUPPORTED_NETWORK: "Nieobsługiwana sieć",
  NETWORK_MISMATCH: "Niezgodna sieć",
};

export function rejectedReasonLabel(code: string): string {
  return isKnownCode(rejectedReasonLabels, code)
    ? rejectedReasonLabels[code]
    : code;
}

const activityEventLabels: Record<AddressActivityEventResponseType, string> = {
  RECEIVED: "Otrzymano",
  SENT: "Wysłano",
  SWAP: "Zamiana",
  CASH_OUT_SIGNAL: "Sygnał wyjścia",
};

export function activityEventLabel(code: string): string {
  return isKnownCode(activityEventLabels, code)
    ? activityEventLabels[code]
    : code;
}

export const activityLabels = {
  noData:
    "Nie udało się pobrać danych o aktywności tego adresu. Brak danych nie oznacza braku aktywności.",
  cashOut: "Możliwe wyjście do walut tradycyjnych.",
  unknownVolume: "Nieznany",
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

const monthFormat = new Intl.DateTimeFormat("pl-PL", {
  month: "2-digit",
  year: "numeric",
  timeZone: "Europe/Warsaw",
});

/** "03.2025" from an ISO instant. */
export function formatMonth(isoInstant: string): string {
  return monthFormat.format(new Date(isoInstant));
}

const amountFormat = new Intl.NumberFormat("pl-PL", {
  maximumFractionDigits: 8,
});

/** "2,41 BTC": decimal comma, up to 8 decimals, no trailing zeros. */
export function formatAmount(amount: number, unit: string | null): string {
  const text = amountFormat.format(amount);
  return unit === null ? text : `${text} ${unit}`;
}
