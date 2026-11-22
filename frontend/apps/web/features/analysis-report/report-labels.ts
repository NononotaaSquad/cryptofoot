import type {
  AddressActivityResponseCoverageLimitationsItem,
  AnalysisResponse,
  AnalysisResponseAssessmentMode,
  AnalysisResponseStatus,
  AnalysisResponseTrigger,
  CashOutSignalResponseKind,
  DetectedAddressResponseValidationFailure,
  ExcludedAddressResponseReason,
  PremiseFactsResponseAuthorIsPerson,
  PremiseFactsResponseIdentifierTypesItem,
  PremiseResponseAssessedBy,
  PremiseResponseDismissalReason,
  PremiseResponseKind,
  PremiseResponseStatus,
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
    MODEL_ASSESSMENT_UNAVAILABLE:
      "Model nie ocenił części lub wszystkich przesłanek; te przesłanki oceniono samymi regułami.",
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

// Known codes of `confidence-1` to `confidence-3` and saved `provisional-1`
// Analyses. `withSources` marks rules whose line names the Mention sources in
// brackets when the Attribution has no Premises (an older Analysis).
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
  OWN_DECLARATION: { label: "Własna deklaracja adresu", withSources: true },
  SALE_OR_SETTLEMENT: {
    label: "Ogłoszenie sprzedaży lub rozliczenie",
    withSources: true,
  },
  DISCUSSION: { label: "Adres w dyskusji", withSources: true },
  MASS_COMMENT: { label: "Wpis konkursowy lub promocyjny", withSources: true },
  NO_PERSON_LINK: { label: "Brak powiązania z osobą", withSources: false },
  DECLARED_BY_ANOTHER_PERSON: {
    label: "Adres podany przez inną osobę",
    withSources: false,
  },
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
  WITHDRAWN_ADDRESS: {
    label: "Adres wycofany przez autora",
    withSources: false,
  },
  FOR_REVIEW: { label: "Przesłanki do weryfikacji", withSources: false },
  WARNING_CONTEXT: { label: "Adres w ostrzeżeniu", withSources: false },
  WARNING_DISMISSED: {
    label: "Ostrzeżenie przed adresem, przesłanka odrzucona",
    withSources: false,
  },
  CONFLICTING_EMAIL: { label: "E-mail innej osoby", withSources: false },
  CLASSIFICATION_UNAVAILABLE: {
    label: "Klasyfikacja niedostępna",
    withSources: false,
  },
  CLASSIFICATION_CAP: {
    label: "Ograniczenie z powodu niedostępnej klasyfikacji",
    withSources: false,
  },
  MODEL_ASSISTED: { label: "Ocena z pomocą modelu", withSources: false },
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

export type PremiseStatusIcon =
  "counted" | "forReview" | "dismissed" | "unknown";

const premiseStatuses: Record<
  PremiseResponseStatus,
  { icon: PremiseStatusIcon; label: string }
> = {
  COUNTED: { icon: "counted", label: "Zaliczona" },
  FOR_REVIEW: { icon: "forReview", label: "Do weryfikacji" },
  DISMISSED: { icon: "dismissed", label: "Odrzucona" },
};

/** Word and icon key of a Premise status; an unknown code is shown as is. */
export function premiseStatus(code: string): {
  icon: PremiseStatusIcon;
  label: string;
} {
  return isKnownCode(premiseStatuses, code)
    ? premiseStatuses[code]
    : { icon: "unknown", label: code };
}

const premiseReasonLabels: Record<
  NonNullable<PremiseResponseDismissalReason>,
  string
> = {
  WARNING: "Ostrzeżenie przed tym adresem",
  DECLARED_BY_ANOTHER_PERSON: "Adres podany przez inną osobę",
  NO_PERSON_LINK: "Brak powiązania z badaną osobą",
  CONFLICTING_EMAIL: "E-mail innej osoby",
};

export function premiseReasonLabel(code: string): string {
  return isKnownCode(premiseReasonLabels, code)
    ? premiseReasonLabels[code]
    : code;
}

const premiseKindLabels: Record<NonNullable<PremiseResponseKind>, string> = {
  OWN_PROFILE: "Profil badanej osoby",
  OWN_DECLARATION: "Własna deklaracja",
  SALE_OR_SETTLEMENT: "Sprzedaż lub rozliczenie",
  DISCUSSION: "Dyskusja",
  MASS_COMMENT: "Wpis masowy",
};

export function premiseKindLabel(code: string): string {
  return isKnownCode(premiseKindLabels, code) ? premiseKindLabels[code] : code;
}

const assessedByWords: Record<PremiseResponseAssessedBy, string> = {
  RULE: "reguła",
  MODEL: "model",
};

/** "Oceniono: reguła" / "Oceniono: model". */
export function assessedByLabel(code: string): string {
  return `Oceniono: ${isKnownCode(assessedByWords, code) ? assessedByWords[code] : code}`;
}

const authorIsPersonLabels: Record<PremiseFactsResponseAuthorIsPerson, string> =
  {
    YES: "tak",
    NO: "nie",
    UNKNOWN: "nieznany",
  };

export function premiseAuthorIsPersonLabel(code: string): string {
  return isKnownCode(authorIsPersonLabels, code)
    ? authorIsPersonLabels[code]
    : code;
}

const identifierTypeLabels: Record<
  PremiseFactsResponseIdentifierTypesItem,
  string
> = {
  NICKNAME: "nick",
  LOGIN: "login",
  EMAIL: "e-mail",
  MESSENGER_ID: "identyfikator komunikatora",
  PROFILE_URL: "adres profilu",
  WALLET_ADDRESS: "adres portfela",
  FULL_NAME: "imię i nazwisko",
};

export function identifierTypeLabel(code: string): string {
  return isKnownCode(identifierTypeLabels, code)
    ? identifierTypeLabels[code]
    : code;
}

export const premiseLabels = {
  heading: (count: number) => `Przesłanki (${String(count)})`,
  title: (number: number) => `Przesłanka ${String(number)}`,
  /** "przesłanka 1: GitHub", inside the brackets of a Punktacja line. */
  reference: (number: number, sourceName: string) =>
    sourceName === ""
      ? `przesłanka ${String(number)}`
      : `przesłanka ${String(number)}: ${sourceName}`,
  withdrawn: "Adres wycofany przez autora",
} as const;

export const premiseFactLabels = {
  authorIsPerson: "Autor to badana osoba",
  underProfile: "Strona pod profilem osoby",
  identifierTypes: "Identyfikatory osoby w tekście",
  identifierDistance: "Odległość identyfikatora od adresu (znaki)",
  conflictingEmail: "E-mail innej osoby w tekście",
  addressCount: "Liczba adresów we wzmiance",
  host: "Serwis",
} as const;

export const premiseAnswerLabels = {
  kind: "Rodzaj wpisu",
  declaration: "Deklaracja",
  warning: "Ostrzeżenie",
} as const;

export const premiseAnswerWords = {
  yes: "tak",
  no: "nie",
  undetermined: "niepewne",
} as const;

const assessmentModeLabels: Record<
  NonNullable<AnalysisResponseAssessmentMode>,
  string
> = {
  WITH_MODEL: "z modelem",
  RULES_ONLY: "tylko reguły",
};

export function assessmentModeLabel(code: string): string {
  return isKnownCode(assessmentModeLabels, code)
    ? assessmentModeLabels[code]
    : code;
}

export const summaryVersionLabels = {
  algorithmVersion: "Wersja algorytmu",
  assessmentMode: "Tryb oceny",
  modelVersion: "Wersja modelu",
  questionSetVersion: "Wersja zestawu pytań",
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

const cashOutSignalLabels: Record<CashOutSignalResponseKind, string> = {
  EXCHANGE: "Przelew na giełdę kryptowalut",
  P2P: "Przelew na platformę P2P",
  PAYMENT_GATEWAY: "Przelew do bramki płatniczej",
  CRYPTO_CARD: "Przelew do usługi karty kryptowalutowej",
  STABLECOIN_SWAP: "Zamiana na stablecoin",
};

export function cashOutSignalLabel(code: string): string {
  return isKnownCode(cashOutSignalLabels, code)
    ? cashOutSignalLabels[code]
    : code;
}

const activityLimitationSentences: Record<
  AddressActivityResponseCoverageLimitationsItem,
  string
> = {
  RECENT_TRANSACTIONS_ONLY:
    "Odczytano tylko najnowsze transakcje, więc starsze sygnały mogą nie być widoczne.",
  LABELLED_COUNTERPARTIES_ONLY:
    "Sygnały są wykrywane tylko dla serwisów ze znanych list. Brak sygnału nie oznacza, że środków nie wymieniono na waluty tradycyjne.",
  INTERNAL_TRANSFERS_NOT_COUNTED:
    "Sumy nie obejmują przelewów wewnętrznych z kontraktów.",
  TRANSACTIONS_UNAVAILABLE:
    "Nie udało się odczytać listy transakcji, więc nie sprawdzono sygnałów wyjścia do walut tradycyjnych.",
};

/** One sentence per coverage limitation; an unknown code is still shown. */
export function activityLimitationSentence(code: string): string {
  return isKnownCode(activityLimitationSentences, code)
    ? activityLimitationSentences[code]
    : `Dane o aktywności mogą być niepełne (kod: ${code}).`;
}

export const activityLabels = {
  noData:
    "Nie udało się pobrać danych o aktywności tego adresu. Brak danych nie oznacza braku aktywności.",
  notChecked:
    "Aktywności tego adresu nie sprawdzano w tej analizie, ponieważ nic nie łączy go z badaną osobą.",
  cashOut: "Możliwe wyjście do walut tradycyjnych.",
  unknownVolume: "Nieznany",
  unknownCount: "Nieznana",
  synthetic: " (dane testowe)",
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

/**
 * "21.11.2026, 14:32 · Ręczna · Zakończona": one Analysis as the history
 * select names it, so other places can name it the same way.
 */
export function analysisLabel(analysis: AnalysisResponse): string {
  return [
    analysis.startedAt === undefined ? "–" : formatDateTime(analysis.startedAt),
    analysis.trigger === undefined
      ? null
      : analysisTriggerLabels[analysis.trigger],
    analysis.status === undefined
      ? null
      : analysisStatusLabels[analysis.status],
  ]
    .filter((part) => part !== null)
    .join(" · ");
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

/**
 * "93%" from 0.925: a whole number, rounded half up (P28). The inner rounding
 * removes binary noise first (0.285 * 100 is 28.499…).
 */
export function formatPercent(probability: number): string {
  return `${String(Math.round(Math.round(probability * 10000) / 100))}%`;
}
