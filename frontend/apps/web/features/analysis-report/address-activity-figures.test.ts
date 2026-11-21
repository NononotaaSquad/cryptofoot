import { describe, expect, it } from "vitest";

import type {
  AddressActivityEventResponse,
  AddressActivityResponse,
} from "@/lib/api/generated/model";

import { toAddressActivityFigures } from "./address-activity-figures";

function event(values: Partial<AddressActivityEventResponse> = {}) {
  return {
    type: "RECEIVED",
    transactionId: "tx-1",
    occurredAt: "2025-03-10T10:00:00Z",
    amount: 1.2,
    unit: "BTC",
    ...values,
  } satisfies AddressActivityEventResponse;
}

function activity(values: Partial<AddressActivityResponse> = {}) {
  return {
    totalReceived: 1.2,
    totalSent: 1.21,
    unit: "BTC",
    events: [event()],
    coverageLimitation: null,
    ...values,
  } satisfies AddressActivityResponse;
}

function available(values: Partial<AddressActivityResponse> = {}) {
  const figures = toAddressActivityFigures(activity(values));
  if (!figures.available) throw new Error("expected available figures");
  return figures;
}

describe("toAddressActivityFigures", () => {
  it("gives the no-data message for a null activity", () => {
    expect(toAddressActivityFigures(null)).toEqual({
      available: false,
      message:
        "Nie udało się pobrać danych o aktywności tego adresu. Brak danych nie oznacza braku aktywności.",
    });
  });

  it("adds both totals into the volume with a decimal comma and the unit", () => {
    expect(available().volume).toBe("2,41 BTC");
  });

  it("drops trailing zeros and keeps at most 8 decimals", () => {
    expect(available({ totalReceived: 0.123456789, totalSent: 0 }).volume).toBe(
      "0,12345679 BTC"
    );
    expect(available({ totalReceived: 2, totalSent: 0.5 }).volume).toBe(
      "2,5 BTC"
    );
  });

  it("treats a null total as unknown, not zero", () => {
    expect(available({ totalSent: null }).volume).toBe("Nieznany");
    expect(available({ totalReceived: null }).volume).toBe("Nieznany");
  });

  it("counts the transactions", () => {
    expect(
      available({
        events: [
          event({ transactionId: "a" }),
          event({ transactionId: "b" }),
          event({ transactionId: "c" }),
        ],
      }).transactionCount
    ).toBe(3);
  });

  it("takes the period from the earliest to the latest of unsorted events", () => {
    expect(
      available({
        events: [
          event({ transactionId: "b", occurredAt: "2026-11-02T10:00:00Z" }),
          event({ transactionId: "a", occurredAt: "2025-03-10T10:00:00Z" }),
          event({ transactionId: "c", occurredAt: "2025-07-01T10:00:00Z" }),
        ],
      }).period
    ).toBe("03.2025 – 11.2026");
  });

  it("gives one month as the period of a single event", () => {
    const figures = available({ events: [event()] });

    expect(figures.period).toBe("03.2025");
    expect(figures.transactionCount).toBe(1);
  });

  it("has no period and no events without events", () => {
    const figures = available({ events: [] });

    expect(figures.period).toBe(null);
    expect(figures.events).toEqual([]);
    expect(figures.transactionCount).toBe(0);
  });

  it("sorts the events ascending by time", () => {
    const figures = available({
      events: [
        event({ transactionId: "late", occurredAt: "2026-11-21T13:32:00Z" }),
        event({ transactionId: "early", occurredAt: "2025-03-10T10:00:00Z" }),
      ],
    });

    expect(figures.events.map((e) => e.key)).toEqual(["early", "late"]);
    expect(figures.events[1]?.occurredAt).toBe("21.11.2026, 14:32");
  });

  it("labels every event type and marks a cash-out signal", () => {
    const figures = available({
      events: [
        event({ transactionId: "1", type: "RECEIVED" }),
        event({ transactionId: "2", type: "SENT" }),
        event({ transactionId: "3", type: "SWAP" }),
        event({ transactionId: "4", type: "CASH_OUT_SIGNAL", amount: null }),
      ],
    });

    expect(
      figures.events.map((e) => [e.typeLabel, e.amount, e.cashOutNote])
    ).toEqual([
      ["Otrzymano", "1,2 BTC", null],
      ["Wysłano", "1,2 BTC", null],
      ["Zamiana", "1,2 BTC", null],
      ["Sygnał wyjścia", null, "Możliwe wyjście do walut tradycyjnych."],
    ]);
  });

  it("passes the coverage limitation through as a warning", () => {
    expect(
      available({ coverageLimitation: "Dane obejmują tylko 2025 rok." })
        .coverageLimitation
    ).toBe("Dane obejmują tylko 2025 rok.");
  });
});
