import { describe, expect, it } from "vitest";

import {
  type AnalysisResponse,
  AnalysisResponseStatus,
  AnalysisResponseTrigger,
  type AttributionResponse,
  type PersonIdentifiersResponse,
  type ReportResponse,
  ReportResponseLimitationsItem,
  type SourceCoverageResponse,
} from "@/lib/api/generated/model";

import { toReportView } from "./report-view-model";

// 13:32 UTC is 14:32 in Warsaw (CET) on 2026-11-21.
const STARTED_AT = "2026-11-21T13:32:00Z";
const FINISHED_AT = "2026-11-21T13:34:00Z";

function source(values: Partial<SourceCoverageResponse> = {}) {
  return {
    sourceName: "GitHub",
    status: "CHECKED",
    synthetic: false,
    mentionCount: 1,
    checkedAt: "2026-11-21T13:33:00Z",
    ...values,
  } satisfies SourceCoverageResponse;
}

function analysis(values: Partial<AnalysisResponse> = {}) {
  return {
    id: "analysis-1",
    caseId: "case-1",
    trigger: "MANUAL",
    status: "COMPLETED",
    startedAt: STARTED_AT,
    finishedAt: FINISHED_AT,
    sources: [source()],
    networks: ["BTC", "ETH"],
    ...values,
  } satisfies AnalysisResponse;
}

function attribution(values: Partial<AttributionResponse> = {}) {
  return {
    address: "bc1qexampleaddress",
    network: "BTC",
    confidenceLevel: 55,
    classificationStatus: "NOT_EXCLUDED",
    justification: [],
    scoringRules: [],
    addressActivity: null,
    ...values,
  } satisfies AttributionResponse;
}

function report(values: Partial<ReportResponse> = {}): ReportResponse {
  return {
    analysis: analysis(),
    attributions: [attribution()],
    excludedAddresses: [],
    limitations: [],
    ...values,
  };
}

const caseIdentifiers: PersonIdentifiersResponse = { nickname: "krypto_janek" };

function view(values: Partial<ReportResponse> = {}) {
  return toReportView(report(values), [], caseIdentifiers);
}

function summaryOf(analysisValues: Partial<AnalysisResponse>) {
  return view({ analysis: analysis(analysisValues) }).summary;
}

describe("toReportView summary", () => {
  it.each([
    [AnalysisResponseStatus.RUNNING, "W toku"],
    [AnalysisResponseStatus.COMPLETED, "Zakończona"],
    [AnalysisResponseStatus.PARTIAL, "Częściowa"],
    [AnalysisResponseStatus.FAILED, "Nieudana"],
  ])("labels the status %s as %s", (status, label) => {
    expect(summaryOf({ status }).status).toEqual({ key: status, label });
  });

  it.each([
    [AnalysisResponseTrigger.MANUAL, "Ręczna"],
    [AnalysisResponseTrigger.SCHEDULED, "Zaplanowana"],
  ])("labels the trigger %s as %s", (trigger, label) => {
    expect(summaryOf({ trigger }).triggerLabel).toBe(label);
  });

  it("formats the heading, start and end in pl-PL", () => {
    const summary = summaryOf({});

    expect(summary.heading).toBe("Raport analizy z 21.11.2026, 14:32");
    expect(summary.startedAt).toBe("21.11.2026, 14:32");
    expect(summary.finishedAt).toBe("21.11.2026, 14:34");
  });

  it("has no end while the Analysis runs", () => {
    expect(summaryOf({ status: "RUNNING", finishedAt: null }).finishedAt).toBe(
      null
    );
  });

  it("gives a checked source its check time and no labels", () => {
    expect(summaryOf({ sources: [source()] }).sources).toEqual([
      {
        name: "GitHub",
        checkedAtText: "· sprawdzono o 14:33",
        testDataLabel: null,
        unavailableText: null,
      },
    ]);
  });

  it("labels a synthetic source as test data", () => {
    const [line] = summaryOf({
      sources: [source({ synthetic: true })],
    }).sources;

    expect(line?.testDataLabel).toBe("Dane testowe");
  });

  it("marks an unavailable source", () => {
    const [line] = summaryOf({
      sources: [source({ status: "UNAVAILABLE" })],
    }).sources;

    expect(line?.unavailableText).toBe("· niedostępne");
  });

  it("gives a source without a check time no check line", () => {
    const [line] = summaryOf({
      sources: [source({ checkedAt: null })],
    }).sources;

    expect(line?.checkedAtText).toBe(null);
  });

  it("counts checked sources against all sources", () => {
    const summary = summaryOf({
      sources: [
        source({ sourceName: "GitHub" }),
        source({ sourceName: "Forum", status: "UNAVAILABLE", checkedAt: null }),
        source({ sourceName: "Reddit" }),
      ],
    });

    expect(summary.sourcesHeading).toBe("Sprawdzone źródła (2 z 3)");
  });

  it("lists the networks", () => {
    expect(summaryOf({ networks: ["BTC", "ETH"] }).networks).toEqual([
      "BTC",
      "ETH",
    ]);
  });

  it.each([
    [
      ReportResponseLimitationsItem.SOURCE_UNAVAILABLE,
      "Co najmniej jedno źródło było niedostępne, więc raport może nie zawierać wszystkich wzmianek.",
    ],
    [
      ReportResponseLimitationsItem.BLOCKCHAIN_DATA_UNAVAILABLE,
      "Nie udało się pobrać danych o aktywności adresów w sieci, więc ta część raportu może być pusta lub niepełna.",
    ],
    [
      ReportResponseLimitationsItem.CLASSIFICATION_UNAVAILABLE,
      "Nie udało się sprawdzić, czy adresy należą do serwisów (np. giełd), więc część powiązań może dotyczyć takich adresów.",
    ],
    [
      ReportResponseLimitationsItem.ANALYSIS_INCOMPLETE,
      "Analiza nie została ukończona, więc raport może być niepełny.",
    ],
  ])("writes one sentence for the Report Limitation %s", (code, sentence) => {
    expect(view({ limitations: [code] }).summary.reportLimitations).toEqual([
      sentence,
    ]);
  });

  it("writes a generic sentence with the code for an unknown Report Limitation", () => {
    // The backend may send a code newer than the generated client.
    const unknown = JSON.parse(
      '["NEW_LIMITATION"]'
    ) as ReportResponse["limitations"]; // `as`: simulates an untyped response body

    expect(view({ limitations: unknown }).summary.reportLimitations).toEqual([
      "Raport może być niepełny (kod: NEW_LIMITATION).",
    ]);
  });
});

describe("toReportView state", () => {
  it("is running with the running notice", () => {
    const result = view({
      analysis: analysis({ status: "RUNNING", finishedAt: null }),
    });

    expect(result.state).toBe("running");
    expect(result.summary.notice).toBe("Analiza jeszcze trwa");
  });

  it("is failed with the failed notice", () => {
    const result = view({ analysis: analysis({ status: "FAILED" }) });

    expect(result.state).toBe("failed");
    expect(result.summary.notice).toBe("Analiza nie powiodła się");
  });

  it("is partial with the partial notice", () => {
    const result = view({ analysis: analysis({ status: "PARTIAL" }) });

    expect(result.state).toBe("partial");
    expect(result.summary.notice).toBe(
      "Analiza zakończyła się częściowo. Raport może być niepełny."
    );
  });

  it("is completed when the report has Attributions", () => {
    const result = view({ attributions: [attribution()] });

    expect(result.state).toBe("completed");
    expect(result.summary.notice).toBe(null);
  });

  it("is noAttributions when a completed report has none", () => {
    expect(view({ attributions: [] }).state).toBe("noAttributions");
  });
});
