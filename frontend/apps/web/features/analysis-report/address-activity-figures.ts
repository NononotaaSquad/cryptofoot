import type {
  AddressActivityResponse,
  AttributionResponseAddressActivityStatus,
  CashOutSignalResponse,
} from "@/lib/api/generated/model";

import {
  activityLabels,
  activityLimitationSentence,
  cashOutSignalLabel,
  formatAmount,
  formatDateTime,
  formatMonth,
} from "./report-labels";

export interface AddressActivityEventView {
  key: string;
  /** "Przelew na giełdę kryptowalut", "Zamiana na stablecoin", … */
  typeLabel: string;
  /** "21.11.2026, 14:32", or "" when the event has no time. */
  occurredAt: string;
  /** "2,41 BTC", or `null` when the amount is unknown. */
  amount: string | null;
  /** The transaction id: the reference the Analyst can check. */
  transaction: string;
  /** `null` when the link is not http(s) (synthetic data has none). */
  transactionUrl: string | null;
  /** The labelled counterparty, else its address, else `null`. */
  counterparty: string | null;
  /** Every listed event is a cash-out signal (D23-004). */
  cashOutNote: string;
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
      /** "5", or "Nieznana" when the source does not know it. */
      transactionCount: string;
      /** "03.2025 – 11.2026", one month alone, "do 11.2026", or `null`. */
      period: string | null;
      /** "synthetic (dane testowe), pobrano 21.11.2026, 14:32". */
      source: string;
      /** Cash-out signals, ascending by time. */
      events: AddressActivityEventView[];
      /** One sentence per coverage limitation. */
      coverageLimitations: string[];
    };

/** Maps the figures supplied by the backend (task #23) to the block. */
export function toAddressActivityFigures(
  status: AttributionResponseAddressActivityStatus | undefined,
  activity: AddressActivityResponse | null | undefined
): AddressActivityFigures {
  if (activity == null) {
    return {
      available: false,
      message:
        status === "NOT_CHECKED"
          ? activityLabels.notChecked
          : activityLabels.noData,
    };
  }

  const unit = activity.unit ?? null;
  const events = [...(activity.cashOutSignals ?? [])].sort(
    (a, b) => eventTime(a) - eventTime(b)
  );

  return {
    available: true,
    // A null total is unknown, not zero: the sum would understate the volume.
    volume:
      activity.totalReceived == null || activity.totalSent == null
        ? activityLabels.unknownVolume
        : formatAmount(activity.totalReceived + activity.totalSent, unit),
    transactionCount:
      activity.transactionCount == null
        ? activityLabels.unknownCount
        : String(activity.transactionCount),
    period: period(activity.firstActivityAt, activity.lastActivityAt),
    source: sourceText(activity),
    events: events.map((event, index) => eventView(event, index, unit)),
    coverageLimitations: (activity.coverageLimitations ?? []).map(
      activityLimitationSentence
    ),
  };
}

// Events without a time go last; the contract marks the field optional only.
function eventTime(event: CashOutSignalResponse): number {
  return event.occurredAt === undefined
    ? Number.POSITIVE_INFINITY
    : Date.parse(event.occurredAt);
}

// Only the last activity is known when the source read its most recent page.
function period(
  first: string | null | undefined,
  last: string | null | undefined
): string | null {
  if (last == null) return null;
  const lastMonth = formatMonth(last);
  if (first == null) return `do ${lastMonth}`;
  const firstMonth = formatMonth(first);
  return firstMonth === lastMonth ? firstMonth : `${firstMonth} – ${lastMonth}`;
}

function sourceText(activity: AddressActivityResponse): string {
  const name = activity.sourceName ?? "";
  const synthetic = activity.synthetic === true ? activityLabels.synthetic : "";
  const checkedAt =
    activity.checkedAt === undefined
      ? ""
      : `, pobrano ${formatDateTime(activity.checkedAt)}`;
  return `${name}${synthetic}${checkedAt}`;
}

function eventView(
  event: CashOutSignalResponse,
  index: number,
  activityUnit: string | null
): AddressActivityEventView {
  const url = event.transactionUrl ?? "";
  return {
    key: `${event.transactionId ?? ""}-${String(index)}`,
    typeLabel: event.kind === undefined ? "" : cashOutSignalLabel(event.kind),
    occurredAt:
      event.occurredAt === undefined ? "" : formatDateTime(event.occurredAt),
    amount:
      event.amount == null
        ? null
        : formatAmount(event.amount, event.unit ?? activityUnit),
    transaction: event.transactionId ?? "",
    transactionUrl: /^https?:\/\//i.test(url) ? url : null,
    counterparty: event.counterpartyLabel ?? event.counterpartyAddress ?? null,
    cashOutNote: activityLabels.cashOut,
  };
}
