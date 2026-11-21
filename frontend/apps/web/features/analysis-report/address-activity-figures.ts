import type {
  AddressActivityEventResponse,
  AddressActivityResponse,
} from "@/lib/api/generated/model";

import {
  activityEventLabel,
  activityLabels,
  formatAmount,
  formatDateTime,
  formatMonth,
} from "./report-labels";

export interface AddressActivityEventView {
  key: string;
  /** "Otrzymano", "Wysłano", "Zamiana", "Sygnał wyjścia". */
  typeLabel: string;
  /** "21.11.2026, 14:32", or "" when the event has no time. */
  occurredAt: string;
  /** "2,41 BTC", or `null` when the amount is unknown. */
  amount: string | null;
  /** "Możliwe wyjście do walut tradycyjnych." for a cash-out signal. */
  cashOutNote: string | null;
}

/**
 * Seam S2: what the "Aktywność adresu" block shows. `available: false` means
 * no data, which is not the same as no activity.
 */
export type AddressActivityFigures =
  | { available: false; message: string }
  | {
      available: true;
      /** "2,41 BTC", or "Nieznany" when a total is unknown. */
      volume: string;
      transactionCount: number;
      /** "03.2025 – 11.2026", one month alone, or `null` without events. */
      period: string | null;
      /** Ascending by time. */
      events: AddressActivityEventView[];
      coverageLimitation: string | null;
    };

/**
 * Computes the figures from the raw activity. When the backend supplies them,
 * only the body of this function changes.
 */
export function toAddressActivityFigures(
  activity: AddressActivityResponse | null | undefined
): AddressActivityFigures {
  if (activity == null) {
    return { available: false, message: activityLabels.noData };
  }

  const unit = activity.unit ?? null;
  const events = [...(activity.events ?? [])].sort(
    (a, b) => eventTime(a) - eventTime(b)
  );
  const times = events
    .map((event) => event.occurredAt)
    .filter((time): time is string => time !== undefined);
  const first = times[0];
  const last = times[times.length - 1];

  return {
    available: true,
    // A null total is unknown, not zero: the sum would understate the volume.
    volume:
      activity.totalReceived == null || activity.totalSent == null
        ? activityLabels.unknownVolume
        : formatAmount(activity.totalReceived + activity.totalSent, unit),
    transactionCount: events.length,
    period:
      first === undefined || last === undefined
        ? null
        : monthRange(formatMonth(first), formatMonth(last)),
    events: events.map((event, index) => eventView(event, index, unit)),
    coverageLimitation: activity.coverageLimitation ?? null,
  };
}

// Events without a time go last; the contract marks the field optional only.
function eventTime(event: AddressActivityEventResponse): number {
  return event.occurredAt === undefined
    ? Number.POSITIVE_INFINITY
    : Date.parse(event.occurredAt);
}

function monthRange(first: string, last: string): string {
  return first === last ? first : `${first} – ${last}`;
}

function eventView(
  event: AddressActivityEventResponse,
  index: number,
  activityUnit: string | null
): AddressActivityEventView {
  return {
    key: event.transactionId ?? String(index),
    typeLabel: event.type === undefined ? "" : activityEventLabel(event.type),
    occurredAt:
      event.occurredAt === undefined ? "" : formatDateTime(event.occurredAt),
    amount:
      event.amount == null
        ? null
        : formatAmount(event.amount, event.unit ?? activityUnit),
    cashOutNote:
      event.type === "CASH_OUT_SIGNAL" ? activityLabels.cashOut : null,
  };
}
