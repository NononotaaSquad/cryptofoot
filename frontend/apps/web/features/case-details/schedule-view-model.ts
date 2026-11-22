import type {
  ScheduleResponse,
  UpdateScheduleRequest,
} from "@/lib/api/generated/model";

import {
  cadenceTexts,
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
    value === "DEMO_EVERY_MINUTE"
  );
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
  const next = formatInstant(schedule?.nextExecutionAt, formatter);
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
                ? stateRowText(cadenceTexts[cadence])
                : `${stateRowText(cadenceTexts[cadence])} · ${nextAnalysisSegment(next)}`,
            demo,
          };
  }

  return {
    stateRow,
    options,
    enabled,
    nextLine: next === null ? null : nextAnalysisLine(next),
    lastAttemptLine,
  };
}

/** The request that saves the chosen option. */
export function toUpdateScheduleRequest(
  value: ScheduleOptionValue
): UpdateScheduleRequest {
  return value === OFF_OPTION_VALUE
    ? { enabled: false }
    : { enabled: true, cadence: value };
}
