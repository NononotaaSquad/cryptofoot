import { describe, expect, it } from "vitest";

import type {
  AnalysisComparisonResponse,
  AnalysisResponse,
  AttributionChangeResponse,
} from "@/lib/api/generated/model";

import {
  comparisonLabels,
  comparisonLimitationSentence,
  previousAnalysisWithResults,
  toComparisonView,
} from "./comparison-view-model";

function analysis(values: AnalysisResponse = {}): AnalysisResponse {
  return {
    id: "a-1",
    caseId: "case-1",
    trigger: "MANUAL",
    status: "COMPLETED",
    // 13:32 UTC is 14:32 in Warsaw (CET).
    startedAt: "2026-11-21T13:32:00Z",
    ...values,
  };
}

function change(
  values: AttributionChangeResponse = {}
): AttributionChangeResponse {
  return {
    address: "bc1qexample",
    network: "BTC",
    change: "NEW",
    earlierConfidenceLevel: null,
    laterConfidenceLevel: 80,
    earlierExclusionReason: null,
    laterExclusionReason: null,
    earlierMentionIds: [],
    laterMentionIds: ["m-1"],
    absenceVerifiable: null,
    ...values,
  };
}

function comparison(
  values: AnalysisComparisonResponse = {}
): AnalysisComparisonResponse {
  return {
    earlierAnalysis: analysis({ id: "a-1" }),
    laterAnalysis: analysis({ id: "a-2", startedAt: "2026-11-22T05:00:00Z" }),
    changes: [],
    comparabilityLimitations: [],
    ...values,
  };
}

describe("previousAnalysisWithResults", () => {
  const first = analysis({ id: "a-1", startedAt: "2026-11-20T10:00:00Z" });
  const partial = analysis({
    id: "a-2",
    status: "PARTIAL",
    startedAt: "2026-11-21T10:00:00Z",
  });
  const failed = analysis({
    id: "a-3",
    status: "FAILED",
    startedAt: "2026-11-21T12:00:00Z",
  });
  const scheduled = analysis({
    id: "a-4",
    trigger: "SCHEDULED",
    startedAt: "2026-11-22T05:00:00Z",
  });
  // Newest first, as Case details passes it.
  const history = [scheduled, failed, partial, first];

  it("gives the newest earlier Analysis with results, skipping a failed one", () => {
    expect(previousAnalysisWithResults(history, "a-4")?.id).toBe("a-2");
  });

  it("does not depend on the order of the history", () => {
    expect(
      previousAnalysisWithResults([first, scheduled, partial, failed], "a-4")
        ?.id
    ).toBe("a-2");
  });

  it("accepts a partial Analysis as the earlier one", () => {
    expect(previousAnalysisWithResults(history, "a-2")?.id).toBe("a-1");
    // The status of the displayed Analysis does not matter here: the report
    // does not show the comparison for a failed one.
    expect(previousAnalysisWithResults(history, "a-3")?.id).toBe("a-2");
  });

  it("gives nothing for the first Analysis of the Case", () => {
    expect(previousAnalysisWithResults(history, "a-1")).toBeNull();
  });

  it("gives nothing for an id outside the history", () => {
    expect(previousAnalysisWithResults(history, "unknown")).toBeNull();
  });

  it("skips a running Analysis and one that started at the same time", () => {
    const running = analysis({
      id: "r",
      status: "RUNNING",
      startedAt: "2026-11-21T11:00:00Z",
    });
    const sameStart = analysis({
      id: "same",
      startedAt: "2026-11-22T05:00:00Z",
    });
    expect(
      previousAnalysisWithResults([scheduled, sameStart, running, first], "a-4")
        ?.id
    ).toBe("a-1");
  });
});

describe("toComparisonView", () => {
  it("names the earlier Analysis the way the history select does", () => {
    const view = toComparisonView(comparison());
    expect(view.comparedWith).toBe(
      "Porównanie z analizą: 21.11.2026, 14:32 · Ręczna · Zakończona."
    );
    expect(view.earlierAnalysisId).toBe("a-1");
  });

  it("says there are no changes instead of showing an empty list", () => {
    const view = toComparisonView(comparison());
    expect(view.heading).toBe("Zmiany od poprzedniej analizy (0)");
    expect(view.changes).toEqual([]);
    expect(view.emptyMessage).toBe(comparisonLabels.empty);
  });

  it("describes every kind of change with both values", () => {
    const view = toComparisonView(
      comparison({
        changes: [
          change({ address: "new", change: "NEW", laterConfidenceLevel: 95 }),
          change({
            address: "level",
            change: "CONFIDENCE_LEVEL_CHANGED",
            earlierConfidenceLevel: 55,
            laterConfidenceLevel: 80,
          }),
          change({
            address: "to-excluded",
            change: "EXCLUSION_CHANGED",
            earlierConfidenceLevel: 40,
            laterConfidenceLevel: null,
            laterExclusionReason: "EXCHANGE",
          }),
          change({
            address: "other-reason",
            change: "EXCLUSION_CHANGED",
            laterConfidenceLevel: null,
            earlierExclusionReason: "MIXER",
            laterExclusionReason: "SMART_CONTRACT",
          }),
          change({
            address: "gone",
            change: "NOT_OBSERVED",
            earlierConfidenceLevel: 60,
            laterConfidenceLevel: null,
          }),
        ],
      })
    );

    expect(view.heading).toBe("Zmiany od poprzedniej analizy (5)");
    expect(view.emptyMessage).toBeNull();
    expect(
      view.changes.map(({ address, label, detail }) => ({
        address,
        label,
        detail,
      }))
    ).toEqual([
      {
        address: "new",
        label: "Nowe powiązanie",
        detail: "Stopień pewności: 95%.",
      },
      {
        address: "level",
        label: "Zmieniony stopień pewności",
        detail: "Stopień pewności: wcześniej 55%, teraz 80%.",
      },
      {
        address: "to-excluded",
        label: "Zmienione wykluczenie",
        detail:
          "Wcześniej: powiązanie, stopień pewności 40%. Teraz: wykluczony jako adres giełdy kryptowalut.",
      },
      {
        address: "other-reason",
        label: "Zmienione wykluczenie",
        detail:
          "Wcześniej: wykluczony jako adres miksera. Teraz: wykluczony jako adres programu w sieci (smart kontrakt).",
      },
      {
        address: "gone",
        label: "Nie znaleziono w tej analizie",
        detail: "Stopień pewności w poprzedniej analizie: 60%.",
      },
    ]);
  });

  it("lists new leads first and not observed ones last, keeping the backend order within a kind", () => {
    const view = toComparisonView(
      comparison({
        changes: [
          change({ address: "btc-gone", change: "NOT_OBSERVED" }),
          change({ address: "btc-new", change: "NEW" }),
          change({
            address: "eth-level",
            network: "ETH",
            change: "CONFIDENCE_LEVEL_CHANGED",
          }),
          change({ address: "eth-new", network: "ETH", change: "NEW" }),
        ],
      })
    );
    expect(view.changes.map((item) => item.key)).toEqual([
      "BTC:btc-new",
      "ETH:eth-new",
      "ETH:eth-level",
      "BTC:btc-gone",
    ]);
  });

  it("never presents an absence as a fact, and says when it cannot be confirmed", () => {
    const view = toComparisonView(
      comparison({
        changes: [
          change({
            address: "checked",
            change: "NOT_OBSERVED",
            absenceVerifiable: true,
          }),
          change({
            address: "unchecked",
            change: "NOT_OBSERVED",
            absenceVerifiable: false,
          }),
          change({
            address: "no-flag",
            change: "NOT_OBSERVED",
            absenceVerifiable: null,
          }),
          change({ address: "new", change: "NEW" }),
        ],
      })
    );
    const notes = Object.fromEntries(
      view.changes.map((item) => [item.address, item.absenceNote])
    );
    expect(notes).toEqual({
      new: null,
      checked: { text: comparisonLabels.absenceVerified, verifiable: true },
      unchecked: {
        text: comparisonLabels.absenceUnverified,
        verifiable: false,
      },
      "no-flag": {
        text: comparisonLabels.absenceUnverified,
        verifiable: false,
      },
    });
    for (const note of [
      comparisonLabels.absenceVerified,
      comparisonLabels.absenceUnverified,
    ]) {
      expect(note).toContain(
        "Nie oznacza to, że adres przestał należeć do osoby."
      );
    }
  });

  it("states every comparability limitation as a sentence", () => {
    const view = toComparisonView(
      comparison({
        comparabilityLimitations: [
          "IDENTIFIERS_DIFFER",
          "SOURCE_COVERAGE_DIFFERS",
          "NETWORKS_DIFFER",
          "ALGORITHM_VERSION_DIFFERS",
          "PARTIAL_ANALYSIS",
        ],
      })
    );
    expect(view.limitations).toHaveLength(5);
    for (const sentence of view.limitations) {
      expect(sentence).toMatch(/^[A-ZŻŹĆĄŚĘŁÓŃ].+\.$/u);
      expect(sentence).not.toContain("kod:");
    }
  });

  it("shows a code newer than the client instead of hiding it", () => {
    expect(comparisonLimitationSentence("NEW_CODE")).toBe(
      "Porównanie może być niepełne (kod: NEW_CODE)."
    );
    const unknown = toComparisonView(
      comparison({
        changes: [
          // The generated type allows only the known values; the backend may
          // still send a newer one.
          change({ change: "MOVED" as AttributionChangeResponse["change"] }),
        ],
      })
    ).changes[0];
    expect(unknown?.kind).toBe("UNKNOWN");
    expect(unknown?.label).toBe("MOVED");
  });
});
