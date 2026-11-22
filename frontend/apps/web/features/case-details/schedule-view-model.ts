import type {
  ScheduleResponse,
  UpdateScheduleRequest,
} from "@/lib/api/generated/model";

import {
  cadenceTexts,
  customCadenceText,
  intervalUnitLabels,
  lastRunLine,
  nextAnalysisEnding,
  nextAnalysisLine,
  nextAnalysisSegment,
  OFF_OPTION_VALUE,
  offOptionLabel,
  optionLabels,
  regularCadences,
  stateRowText,
  warningSentences,
  type IntervalUnit,
  type ScheduleCadence,
  type ScheduleOptionValue,
  type ScheduleOutcome,
} from "./schedule-labels";

export type { ScheduleOptionValue } from "./schedule-labels";

const DEFAULT_ZONE = "Europe/Warsaw";

export interface ScheduleOption {
  value: ScheduleOptionValue;
  label: string;
  selected: boolean;
}

export interface ScheduleStateRow {
  kind: "none" | "info" | "warning";
  text: string;
  // The "Tryb demonstracyjny" label; follows `demo` of the answer.
  demo: boolean;
}

export interface ScheduleLastAttemptLine {
  text: string;
  // Only a started attempt has an Analysis to open.
  analysisId: string | null;
}

export interface ScheduleView {
  stateRow: ScheduleStateRow;
  options: ScheduleOption[];
  // Look of the clock button.
  enabled: boolean;
  nextLine: string | null;
  lastAttemptLine: ScheduleLastAttemptLine | null;
  // Starting values of the custom interval fields.
  customInterval: { value: string; unit: IntervalUnit };
}

export interface ScheduleViewSettings {
  // The frontend demo switch: always list the demo Frequency.
  demoEnabled: boolean;
}

function isCadence(value: unknown): value is ScheduleCadence {
  return (
    value === "DAILY" ||
    value === "EVERY_6_HOURS" ||
    value === "WEEKLY" ||
    value === "DEMO_EVERY_MINUTE" ||
    value === "CUSTOM_INTERVAL"
  );
}

function isIntervalUnit(value: unknown): value is IntervalUnit {
  return value === "MINUTES" || value === "HOURS" || value === "DAYS";
}

// Same limits as the backend (1 minute to 365 days), which stays the authority.
const MAX_INTERVAL_VALUE: Record<IntervalUnit, number> = {
  MINUTES: 525_600,
  HOURS: 8_760,
  DAYS: 365,
};

export type CustomIntervalCheck =
  | { ok: true; intervalValue: number; intervalUnit: IntervalUnit }
  | { ok: false; message: string };

/** Checks the text of the number field before it is sent. */
export function checkCustomInterval(
  text: string,
  unit: IntervalUnit
): CustomIntervalCheck {
  const trimmed = text.trim();
  if (trimmed === "") {
    return { ok: false, message: "Podaj liczbę większą od zera." };
  }
  if (!/^\d+$/.test(trimmed)) {
    return { ok: false, message: "Podaj dodatnią liczbę całkowitą." };
  }
  const value = Number(trimmed);
  if (value < 1) {
    return {
      ok: false,
      message: "Interwał musi wynosić co najmniej 1 minutę.",
    };
  }
  if (value > MAX_INTERVAL_VALUE[unit]) {
    return {
      ok: false,
      message:
        unit === "DAYS"
          ? "Interwał może wynosić najwyżej 365 dni."
          : `Interwał może wynosić najwyżej ${String(MAX_INTERVAL_VALUE[unit])} ${intervalUnitLabels[unit]} (365 dni).`,
    };
  }
  return { ok: true, intervalValue: value, intervalUnit: unit };
}

export function isScheduleOptionValue(
  value: unknown
): value is ScheduleOptionValue {
  return value === OFF_OPTION_VALUE || isCadence(value);
}

function isOutcome(value: unknown): value is ScheduleOutcome {
  return (
    value === "STARTED" ||
    value === "SKIPPED_OVERLAP" ||
    value === "SKIPPED_DOWNTIME" ||
    value === "FAILED"
  );
}

function makeFormatter(timeZone: string): Intl.DateTimeFormat {
  const options: Intl.DateTimeFormatOptions = {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  };
  try {
    return new Intl.DateTimeFormat("pl-PL", { ...options, timeZone });
  } catch {
    // An unknown zone name: fall back to the zone of the backend.
    return new Intl.DateTimeFormat("pl-PL", {
      ...options,
      timeZone: DEFAULT_ZONE,
    });
  }
}

// "22.11.2026, 06:00" in the zone of the answer, not in the zone of the browser.
function formatInstant(
  instant: string | null | undefined,
  formatter: Intl.DateTimeFormat
): string | null {
  if (instant === null || instant === undefined) return null;
  const time = Date.parse(instant);
  return Number.isNaN(time) ? null : formatter.format(new Date(time));
}

function buildOptions(
  selected: ScheduleOptionValue,
  listDemo: boolean
): ScheduleOption[] {
  const values: ScheduleOptionValue[] = [
    OFF_OPTION_VALUE,
    ...regularCadences,
    ...(listDemo ? (["DEMO_EVERY_MINUTE"] as const) : []),
  ];
  return values.map((value) => ({
    value,
    label: value === OFF_OPTION_VALUE ? offOptionLabel : optionLabels[value],
    selected: value === selected,
  }));
}

const noRow: ScheduleStateRow = { kind: "none", text: "", demo: false };

/**
 * Seam 1: everything the Schedule components render, from the answer of the
 * server (`undefined` while it is not read) and the frontend demo switch.
 */
export function toScheduleView(
  schedule: ScheduleResponse | undefined,
  { demoEnabled }: ScheduleViewSettings = { demoEnabled: false }
): ScheduleView {
  const cadence = isCadence(schedule?.cadence) ? schedule.cadence : null;
  const enabled = schedule?.enabled === true && cadence !== null;
  const selected: ScheduleOptionValue = enabled ? cadence : OFF_OPTION_VALUE;
  const options = buildOptions(
    selected,
    demoEnabled || (enabled && cadence === "DEMO_EVERY_MINUTE")
  );

  const formatter = makeFormatter(schedule?.timezone ?? DEFAULT_ZONE);
  const nextDate = formatInstant(schedule?.nextExecutionAt, formatter);
  const next =
    nextDate === null
      ? null
      : `${nextDate} (${formatter.resolvedOptions().timeZone})`;
  const intervalValue = schedule?.intervalValue;
  const intervalUnit = isIntervalUnit(schedule?.intervalUnit)
    ? schedule.intervalUnit
    : null;
  const hasInterval =
    typeof intervalValue === "number" &&
    Number.isInteger(intervalValue) &&
    intervalValue > 0 &&
    intervalUnit !== null;
  const cadenceText =
    cadence === "CUSTOM_INTERVAL" && hasInterval
      ? customCadenceText(intervalValue, intervalUnit)
      : cadence === null
        ? ""
        : cadenceTexts[cadence];
  const attempt = schedule?.lastAttempt ?? null;
  const outcome = isOutcome(attempt?.outcome) ? attempt.outcome : null;
  const attemptedAt = formatInstant(attempt?.attemptedAt, formatter);

  const warning =
    outcome !== null && outcome !== "STARTED" && attemptedAt !== null
      ? warningSentences[outcome](attemptedAt)
      : null;

  let lastAttemptLine: ScheduleLastAttemptLine | null = null;
  if (warning !== null) {
    lastAttemptLine = { text: warning, analysisId: null };
  } else if (outcome === "STARTED" && attemptedAt !== null) {
    lastAttemptLine = {
      text: lastRunLine(attemptedAt),
      analysisId: attempt?.analysisId ?? null,
    };
  }

  let stateRow = noRow;
  if (enabled) {
    const demo = schedule.demo === true;
    stateRow =
      warning !== null
        ? {
            kind: "warning",
            text:
              next === null
                ? warning
                : `${warning} ${nextAnalysisEnding(next)}`,
            demo,
          }
        : {
            kind: "info",
            text:
              next === null
                ? stateRowText(cadenceText)
                : `${stateRowText(cadenceText)} · ${nextAnalysisSegment(next)}`,
            demo,
          };
  }

  return {
    stateRow,
    options,
    enabled,
    nextLine: next === null ? null : nextAnalysisLine(next),
    lastAttemptLine,
    customInterval: {
      value: hasInterval ? String(intervalValue) : "",
      unit: intervalUnit ?? "MINUTES",
    },
  };
}

/** The request that saves the chosen option; a custom interval carries its checked length. */
export function toUpdateScheduleRequest(
  value: ScheduleOptionValue,
  custom?: { intervalValue: number; intervalUnit: IntervalUnit }
): UpdateScheduleRequest {
  if (value === OFF_OPTION_VALUE) return { enabled: false };
  if (value === "CUSTOM_INTERVAL" && custom !== undefined) {
    return { enabled: true, cadence: value, ...custom };
  }
  return { enabled: true, cadence: value };
}
