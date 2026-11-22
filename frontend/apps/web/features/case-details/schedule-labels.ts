// Polish texts of the Schedule, keyed by the codes of the backend contract
// (like features/analysis-report/report-labels.ts).

export type ScheduleCadence =
  "DAILY" | "EVERY_6_HOURS" | "WEEKLY" | "DEMO_EVERY_MINUTE";

export type ScheduleOutcome =
  "STARTED" | "SKIPPED_OVERLAP" | "SKIPPED_DOWNTIME" | "FAILED";

export const OFF_OPTION_VALUE = "OFF";

export type ScheduleOptionValue = typeof OFF_OPTION_VALUE | ScheduleCadence;

// Order of the options in the popover; the demo one is added by the view model.
export const regularCadences: readonly ScheduleCadence[] = [
  "DAILY",
  "EVERY_6_HOURS",
  "WEEKLY",
];

export const offOptionLabel = "Wyłączony";

export const optionLabels: Record<ScheduleCadence, string> = {
  DAILY: "Codziennie o 06:00",
  EVERY_6_HOURS: "Co 6 godzin (00:00, 06:00, 12:00, 18:00)",
  WEEKLY: "Co tydzień (poniedziałek, 06:00)",
  DEMO_EVERY_MINUTE: "Co minutę (tryb demonstracyjny)",
};

export const cadenceTexts: Record<ScheduleCadence, string> = {
  DAILY: "codziennie o 06:00",
  EVERY_6_HOURS: "co 6 godzin",
  WEEKLY: "co tydzień w poniedziałek o 06:00",
  DEMO_EVERY_MINUTE: "co minutę",
};

export const demoLabel = "Tryb demonstracyjny";

export function stateRowText(cadenceText: string): string {
  return `Harmonogram: ${cadenceText}`;
}

export function nextAnalysisSegment(date: string): string {
  return `następna analiza: ${date}`;
}

export function nextAnalysisLine(date: string): string {
  return `Następna analiza: ${date}`;
}

export function nextAnalysisEnding(date: string): string {
  return `Następna analiza: ${date}.`;
}

export function lastRunLine(date: string): string {
  return `Ostatnie uruchomienie: ${date}`;
}

// The sentence of a skipped or failed attempt, without the "next term" ending.
export const warningSentences: Record<
  Exclude<ScheduleOutcome, "STARTED">,
  (date: string) => string
> = {
  FAILED: (date) => `Nie udało się uruchomić zaplanowanej analizy ${date}.`,
  SKIPPED_OVERLAP: (date) =>
    `Pominięto zaplanowaną analizę ${date}, ponieważ inna analiza tej sprawy była w toku.`,
  SKIPPED_DOWNTIME: (date) =>
    `Pominięto zaplanowaną analizę ${date}, ponieważ aplikacja wtedy nie działała.`,
};
