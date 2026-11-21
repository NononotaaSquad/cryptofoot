import { describe, expect, it } from "vitest";

import type {
  AddressActivityResponse,
  CashOutSignalResponse,
} from "@/lib/api/generated/model";

import { toAddressActivityFigures } from "./address-activity-figures";

function signal(values: Partial<CashOutSignalResponse> = {}) {
  return {
    kind: "EXCHANGE",
    transactionId: "tx-1",
    transactionUrl: null,
    occurredAt: "2025-03-10T10:00:00Z",
    amount: 1.2,
    unit: "BTC",
    counterpartyAddress: "1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s",
    counterpartyLabel: "Own label list: Binance.com",
    ...values,
  } satisfies CashOutSignalResponse;
}

function activity(values: Partial<AddressActivityResponse> = {}) {
  return {
    sourceName: "synthetic",
    synthetic: true,
    checkedAt: "2026-11-21T13:32:00Z",
    unit: "BTC",
    totalReceived: 1.2,
    totalSent: 1.21,
    transactionCount: 3,
    firstActivityAt: "2025-03-10T10:00:00Z",
    lastActivityAt: "2026-11-02T10:00:00Z",
    cashOutSignals: [signal()],
    coverageLimitations: ["LABELLED_COUNTERPARTIES_ONLY"],
    ...values,
  } satisfies AddressActivityResponse;
}

function available(values: Partial<AddressActivityResponse> = {}) {
  const figures = toAddressActivityFigures("AVAILABLE", activity(values));
  if (!figures.available) throw new Error("expected available figures");
  return figures;
}

describe("toAddressActivityFigures", () => {
  it("gives the no-data message when the activity was unavailable", () => {
    expect(toAddressActivityFigures("UNAVAILABLE", null)).toEqual({
      available: false,
      message:
        "Nie udało się pobrać danych o aktywności tego adresu. Brak danych nie oznacza braku aktywności.",
    });
    expect(toAddressActivityFigures(undefined, null).available).toBe(false);
  });

  it("says the activity was not checked for an Attribution at 0%", () => {
    expect(toAddressActivityFigures("NOT_CHECKED", null)).toEqual({
      available: false,
      message:
        "Aktywności tego adresu nie sprawdzano w tej analizie, ponieważ nic nie łączy go z badaną osobą.",
    });
  });

  it("adds both totals into the volume with a decimal comma and the unit", () => {
    expect(available().volume).toBe("2,41 BTC");
    expect(
      available({ totalReceived: 10.2, totalSent: 8.1, unit: "ETH" }).volume
    ).toBe("18,3 ETH");
  });

  it("drops trailing zeros and keeps at most 8 decimals", () => {
    expect(available({ totalReceived: 0.123456789, totalSent: 0 }).volume).toBe(
      "0,12345679 BTC"
    );
  });

  it("treats a null total or count as unknown, not zero", () => {
    expect(available({ totalSent: null }).volume).toBe("Nieznany");
    expect(available({ totalReceived: null }).volume).toBe("Nieznany");
    expect(available({ transactionCount: null }).transactionCount).toBe(
      "Nieznana"
    );
  });

  it("takes the transaction count from the backend, not from the signals", () => {
    expect(
      available({ transactionCount: 40, cashOutSignals: [] }).transactionCount
    ).toBe("40");
  });

  it("gives the period from the first to the last activity, or up to the last one when the first is unknown", () => {
    expect(available().period).toBe("03.2025 – 11.2026");
    expect(
      available({
        firstActivityAt: "2025-03-01T10:00:00Z",
        lastActivityAt: "2025-03-20T10:00:00Z",
      }).period
    ).toBe("03.2025");
    expect(available({ firstActivityAt: null }).period).toBe("do 11.2026");
    expect(
      available({ firstActivityAt: null, lastActivityAt: null }).period
    ).toBe(null);
  });

  it("names the source, marks synthetic data and gives the time it was read", () => {
    expect(available().source).toBe(
      "synthetic (dane testowe), pobrano 21.11.2026, 14:32"
    );
    expect(
      available({ sourceName: "mempool.space", synthetic: false }).source
    ).toBe("mempool.space, pobrano 21.11.2026, 14:32");
  });

  it("lists the cash-out signals ascending by time, each with its reference, counterparty and note", () => {
    const figures = available({
      cashOutSignals: [
        signal({
          transactionId: "late",
          occurredAt: "2026-11-21T13:32:00Z",
          kind: "P2P",
          counterpartyLabel: null,
        }),
        signal({
          transactionId: "early",
          kind: "STABLECOIN_SWAP",
          amount: 21480,
          unit: "USDC",
          transactionUrl: "https://mempool.space/tx/early",
        }),
      ],
    });

    expect(
      figures.events.map((e) => [
        e.transaction,
        e.typeLabel,
        e.amount,
        e.occurredAt,
        e.transactionUrl,
        e.counterparty,
        e.cashOutNote,
      ])
    ).toEqual([
      [
        "early",
        "Zamiana na stablecoin",
        // pl-PL groups thousands with a no-break space
        "21 480 USDC",
        "10.03.2025, 11:00",
        "https://mempool.space/tx/early",
        "Own label list: Binance.com",
        "Możliwe wyjście do walut tradycyjnych.",
      ],
      [
        "late",
        "Przelew na platformę P2P",
        "1,2 BTC",
        "21.11.2026, 14:32",
        null,
        "1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s",
        "Możliwe wyjście do walut tradycyjnych.",
      ],
    ]);
  });

  it("labels every signal kind and shows an unknown kind as is", () => {
    const kinds = [
      "EXCHANGE",
      "P2P",
      "PAYMENT_GATEWAY",
      "CRYPTO_CARD",
      "STABLECOIN_SWAP",
    ] as const;
    const figures = available({
      cashOutSignals: kinds.map((kind) => signal({ kind })),
    });

    expect(figures.events.map((e) => e.typeLabel)).toEqual([
      "Przelew na giełdę kryptowalut",
      "Przelew na platformę P2P",
      "Przelew do bramki płatniczej",
      "Przelew do usługi karty kryptowalutowej",
      "Zamiana na stablecoin",
    ]);
  });

  it("never makes a link of a non-http transaction reference", () => {
    expect(
      available({
        cashOutSignals: [signal({ transactionUrl: "javascript:alert(1)" })],
      }).events[0]?.transactionUrl
    ).toBe(null);
  });

  it("gives one sentence per coverage limitation, also for an unknown code", () => {
    expect(
      available({
        coverageLimitations: [
          "RECENT_TRANSACTIONS_ONLY",
          "LABELLED_COUNTERPARTIES_ONLY",
          "INTERNAL_TRANSFERS_NOT_COUNTED",
        ],
      }).coverageLimitations
    ).toEqual([
      "Odczytano tylko najnowsze transakcje, więc starsze sygnały mogą nie być widoczne.",
      "Sygnały są wykrywane tylko dla serwisów ze znanych list. Brak sygnału nie oznacza, że środków nie wymieniono na waluty tradycyjne.",
      "Sumy nie obejmują przelewów wewnętrznych z kontraktów.",
    ]);
    expect(
      toAddressActivityFigures("AVAILABLE", {
        ...activity(),
        coverageLimitations: [],
      }).available
    ).toBe(true);
  });
});
