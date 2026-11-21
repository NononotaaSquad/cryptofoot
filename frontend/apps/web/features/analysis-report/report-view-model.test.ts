import { describe, expect, it } from "vitest";

import {
  type AnalysisResponse,
  AnalysisResponseStatus,
  AnalysisResponseTrigger,
  type AttributionResponse,
  type DetectedAddressResponse,
  DetectedAddressResponseValidationFailure,
  type ExcludedAddressResponse,
  ExcludedAddressResponseReason,
  type MentionResponse,
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
    mentions: [],
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

const BTC_ADDRESS = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa";
const ETH_ADDRESS = "0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed";
const ETH_ADDRESS_EIP55 = "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed";

function detected(values: Partial<DetectedAddressResponse> = {}) {
  return {
    address: BTC_ADDRESS,
    network: "BTC",
    valid: true,
    validationFailure: null,
    ...values,
  } satisfies DetectedAddressResponse;
}

function mention(values: Partial<MentionResponse> = {}) {
  return {
    id: "mention-1",
    sourceName: "forum.example",
    url: "https://forum.example/t/1",
    collectedAt: "2026-11-21T13:33:00Z",
    context: "mój adres to 1A1z…",
    detectedAddresses: [detected()],
    ...values,
  } satisfies MentionResponse;
}

function excluded(values: Partial<ExcludedAddressResponse> = {}) {
  return {
    address: "0xdac17f958d2ee523a2206206994597c13d831ec7",
    network: "ETH",
    reason: "SMART_CONTRACT",
    classificationSource: "Etherscan",
    mentions: [mention({ id: "m-x", sourceName: "x.example" })],
    ...values,
  } satisfies ExcludedAddressResponse;
}

function attributionsOf(
  attributions: AttributionResponse[],
  identifiers: PersonIdentifiersResponse = caseIdentifiers
) {
  return toReportView(report({ attributions }), [], identifiers).attributions;
}

function onlyAttribution(
  values: Partial<AttributionResponse>,
  identifiers?: PersonIdentifiersResponse
) {
  const [first] = attributionsOf([attribution(values)], identifiers);
  if (first === undefined) throw new Error("expected one Attribution");
  return first;
}

const knownRules = [
  { rule: "DISCUSSION", contribution: 40 },
  { rule: "INDEPENDENT_SOURCES", contribution: 15 },
];

describe("toReportView Attributions", () => {
  it.each([
    [100, "Bardzo wysokie prawdopodobieństwo"],
    [80, "Bardzo wysokie prawdopodobieństwo"],
    [79, "Prawdopodobny"],
    [60, "Prawdopodobny"],
    [59, "Średni"],
    [40, "Średni"],
    [39, "Niski / szum"],
    [10, "Niski / szum"],
    [0, "Niski / szum"],
  ])("labels the Confidence Level %i as %s", (confidenceLevel, bandLabel) => {
    expect(onlyAttribution({ confidenceLevel }).confidence).toEqual({
      percent: confidenceLevel,
      text: `${String(confidenceLevel)}%`,
      bandLabel,
    });
  });

  it("sorts by Confidence Level descending, then by address", () => {
    const sorted = attributionsOf([
      attribution({ address: "bc1qzzz", confidenceLevel: 40 }),
      attribution({ address: "bc1qbbb", confidenceLevel: 55 }),
      attribution({ address: "bc1qaaa", confidenceLevel: 55 }),
    ]);

    expect(sorted.map((a) => a.address)).toEqual([
      "bc1qaaa",
      "bc1qbbb",
      "bc1qzzz",
    ]);
  });

  it("writes one Punktacja line per rule with the Mentions' source names and the total", () => {
    const result = onlyAttribution({
      confidenceLevel: 55,
      scoringRules: knownRules,
      mentions: [
        mention({ id: "1", sourceName: "forum.example" }),
        mention({ id: "2", sourceName: "x.example" }),
        mention({ id: "3", sourceName: "forum.example" }),
      ],
    });

    expect(result.scoringLines).toEqual([
      {
        label: "Adres w dyskusji (forum.example, x.example)",
        points: "40",
      },
      {
        label:
          "Potwierdzenie w niezależnych źródłach (forum.example, x.example)",
        points: "+15",
      },
    ]);
    expect(result.confidenceTotal).toBe("55%");
  });

  it("shows an unknown rule code as is with its points", () => {
    const result = onlyAttribution({
      scoringRules: [...knownRules, { rule: "NEW_RULE", contribution: 5 }],
    });

    expect(result.scoringLines[2]).toEqual({ label: "NEW_RULE", points: "+5" });
  });

  it("names no sources and shows 0 without a sign for a rule that is not evidence", () => {
    const result = onlyAttribution({
      confidenceLevel: 80,
      scoringRules: [
        { rule: "OWN_PROFILE", contribution: 80 },
        { rule: "SAME_SERVICE", contribution: 0 },
        { rule: "CLASSIFICATION_UNAVAILABLE", contribution: 0 },
      ],
      mentions: [mention({ sourceName: "github.example" })],
    });

    expect(result.scoringLines).toEqual([
      { label: "Adres na profilu osoby (github.example)", points: "80" },
      { label: "Kolejna wzmianka z tego samego serwisu", points: "0" },
      { label: "Klasyfikacja niedostępna", points: "0" },
    ]);
  });

  it("labels every rule code of confidence-1", () => {
    const codes = [
      "ENTERED_BY_ANALYST",
      "OWN_PROFILE",
      "SALE_OR_SETTLEMENT",
      "DISCUSSION",
      "MASS_COMMENT",
      "NO_PERSON_LINK",
      "INDEPENDENT_SOURCES",
      "SAME_SERVICE",
      "COPIED_TEXT",
      "NICKNAME_ONLY",
      "WARNING_CONTEXT",
      "CONFLICTING_EMAIL",
      "CLASSIFICATION_UNAVAILABLE",
    ];
    const result = onlyAttribution({
      scoringRules: codes.map((rule) => ({ rule, contribution: 0 })),
    });

    for (const [index, line] of result.scoringLines.entries()) {
      expect(line.label).not.toBe(codes[index]);
    }
  });

  it("passes the backend Justification text through without trimming it", () => {
    const text =
      "  Adres pojawił się w dyskusji (p2p.example); taki ślad wymaga potwierdzenia drugim źródłem.\n";

    expect(onlyAttribution({ justification: text }).justification).toBe(text);
  });

  it("keeps legacy Punktacja and shows the fallback for an absent Justification", () => {
    const result = onlyAttribution({
      confidenceLevel: 40,
      scoringRules: [{ rule: "MENTIONED_WITH_IDENTIFIER", contribution: 40 }],
      mentions: [mention()],
    });

    expect(result.scoringLines).toEqual([
      {
        label: "Adres przy identyfikatorze osoby (forum.example)",
        points: "40",
      },
    ]);
    expect(result.confidenceTotal).toBe("40%");
    expect(result.justification).toBe(
      "Brak zapisanego uzasadnienia dla tej analizy"
    );
  });

  it.each([[undefined], [""], [" \t\n "]])(
    "shows the Justification fallback for %j",
    (justification) => {
      expect(onlyAttribution({ justification }).justification).toBe(
        "Brak zapisanego uzasadnienia dla tej analizy"
      );
    }
  );

  it("warns when the classification is UNAVAILABLE", () => {
    expect(
      onlyAttribution({ classificationStatus: "UNAVAILABLE" })
        .classificationWarning
    ).toBe("Nie sprawdzono, czy adres należy do serwisu (np. giełdy).");
  });

  it("does not warn when the address is NOT_EXCLUDED", () => {
    expect(
      onlyAttribution({ classificationStatus: "NOT_EXCLUDED" })
        .classificationWarning
    ).toBe(null);
  });

  it("flags the address entered in the Case", () => {
    expect(
      onlyAttribution({ address: BTC_ADDRESS }, { walletAddress: BTC_ADDRESS })
        .inputAddressNote
    ).toBe("Adres podany przy uruchomieniu analizy");
  });

  it("flags an ETH address that differs only in letter case", () => {
    expect(
      onlyAttribution(
        { address: ETH_ADDRESS, network: "ETH" },
        { walletAddress: ETH_ADDRESS_EIP55 }
      ).inputAddressNote
    ).toBe("Adres podany przy uruchomieniu analizy");
  });

  it("keeps a Base58 address case-sensitive and does not flag another address", () => {
    expect(
      onlyAttribution(
        { address: BTC_ADDRESS },
        { walletAddress: BTC_ADDRESS.toLowerCase() }
      ).inputAddressNote
    ).toBe(null);
    expect(
      onlyAttribution({ address: BTC_ADDRESS }, { nickname: "krypto_janek" })
        .inputAddressNote
    ).toBe(null);
  });

  it("verifies the address of a NOT_EXCLUDED Attribution", () => {
    const result = onlyAttribution({
      address: ETH_ADDRESS,
      network: "ETH",
      classificationStatus: "NOT_EXCLUDED",
      mentions: [
        mention({
          detectedAddresses: [
            detected({ address: ETH_ADDRESS_EIP55, network: "ETH" }),
          ],
        }),
      ],
    });

    expect(result.verification).toEqual([
      { text: "Poprawny format adresu", ok: true },
      { text: "Sieć: ETH", ok: true },
      {
        text: "Adres nie należy do znanego serwisu (giełdy, smart kontraktu, miksera, zbiórki).",
        ok: true,
      },
    ]);
  });

  it("verifies the address of an UNAVAILABLE Attribution", () => {
    const result = onlyAttribution({
      address: BTC_ADDRESS,
      classificationStatus: "UNAVAILABLE",
      mentions: [mention()],
    });

    expect(result.verification).toEqual([
      { text: "Poprawny format adresu", ok: true },
      { text: "Sieć: BTC", ok: true },
      {
        text: "Nie sprawdzono, czy adres należy do serwisu (np. giełdy).",
        ok: false,
      },
    ]);
  });

  it("says the format check is missing when no detected address matches", () => {
    const result = onlyAttribution({
      address: BTC_ADDRESS,
      mentions: [
        mention({ detectedAddresses: [detected({ address: "bc1qother" })] }),
      ],
    });

    expect(result.verification[0]).toEqual({
      text: "Brak danych o sprawdzeniu formatu adresu.",
      ok: false,
    });
    expect(result.verification).toHaveLength(3);
  });

  it("lists the Mentions with source, link, collection time and fragment", () => {
    const result = onlyAttribution({
      mentions: [mention(), mention({ id: "m-2", context: null })],
    });

    expect(result.mentionsHeading).toBe("Wzmianki (2)");
    expect(result.mentionCountText).toBe("2 wzmianki");
    expect(result.mentions[0]).toEqual({
      key: "mention-1",
      sourceName: "forum.example",
      url: "https://forum.example/t/1",
      urlText: "https://forum.example/t/1",
      collectedAt: "21.11.2026, 14:33",
      context: "mój adres to 1A1z…",
    });
    expect(result.mentions[1]?.context).toBe(null);
  });

  it("never makes a link of a non-http address", () => {
    const result = onlyAttribution({
      mentions: [mention({ url: "javascript:alert(1)" })],
    });

    expect(result.mentions[0]?.url).toBe(null);
  });

  it("names the network in the activity heading and has no activity data for null", () => {
    const result = onlyAttribution({ network: "ETH", addressActivity: null });

    expect(result.activityHeading).toBe("Aktywność adresu w sieci ETH");
    expect(result.activity.available).toBe(false);
  });

  it("gives the Powiązania (0) state without Attributions", () => {
    const result = view({ attributions: [] });

    expect(result.state).toBe("noAttributions");
    expect(result.attributionsHeading).toBe("Powiązania (0)");
    expect(result.attributions).toEqual([]);
  });

  it("counts the Attributions in the heading", () => {
    expect(
      view({
        attributions: [
          attribution({ address: "bc1qa" }),
          attribution({ address: "bc1qb" }),
        ],
      }).attributionsHeading
    ).toBe("Powiązania (2)");
  });
});

describe("toReportView Excluded Addresses", () => {
  it.each([
    [ExcludedAddressResponseReason.EXCHANGE, "Adres giełdy kryptowalut"],
    [
      ExcludedAddressResponseReason.SMART_CONTRACT,
      "Adres programu w sieci (smart kontrakt)",
    ],
    [ExcludedAddressResponseReason.MIXER, "Adres miksera"],
    [ExcludedAddressResponseReason.FUNDRAISER, "Adres zbiórki publicznej"],
  ])("labels the reason %s as %s, with its Mentions", (reason, reasonLabel) => {
    const result = view({ excludedAddresses: [excluded({ reason })] });

    expect(result.excludedHeading).toBe("Adresy wykluczone (1)");
    expect(result.excludedAddresses).toEqual([
      {
        key: "ETH:0xdac17f958d2ee523a2206206994597c13d831ec7",
        address: "0xdac17f958d2ee523a2206206994597c13d831ec7",
        network: "ETH",
        reasonLabel,
        classificationSource: "Etherscan",
        mentions: [
          expect.objectContaining({
            sourceName: "x.example",
            url: "https://forum.example/t/1",
          }),
        ],
      },
    ]);
  });

  it("has an empty section with a zero count", () => {
    expect(view({ excludedAddresses: [] }).excludedHeading).toBe(
      "Adresy wykluczone (0)"
    );
  });
});

describe("toReportView Rejected Addresses", () => {
  function rejectedOf(mentions: MentionResponse[]) {
    return toReportView(report(), mentions, caseIdentifiers);
  }

  it("takes only invalid detected addresses", () => {
    const result = rejectedOf([
      mention({
        detectedAddresses: [
          detected(),
          detected({
            address: "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfN0",
            valid: false,
            validationFailure: "CHECKSUM_MISMATCH",
          }),
        ],
      }),
    ]);

    expect(result.rejectedHeading).toBe("Adresy odrzucone (1)");
    expect(result.rejectedAddresses).toEqual([
      {
        key: "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfN0|CHECKSUM_MISMATCH",
        address: "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfN0",
        reasonLabel: "Błędna suma kontrolna",
        sourceNames: "forum.example",
      },
    ]);
  });

  it.each([
    [DetectedAddressResponseValidationFailure.MALFORMED, "Błędny format"],
    [
      DetectedAddressResponseValidationFailure.CHECKSUM_MISMATCH,
      "Błędna suma kontrolna",
    ],
    [
      DetectedAddressResponseValidationFailure.UNSUPPORTED_NETWORK,
      "Nieobsługiwana sieć",
    ],
    [
      DetectedAddressResponseValidationFailure.NETWORK_MISMATCH,
      "Niezgodna sieć",
    ],
  ])("labels the reason %s as %s", (validationFailure, reasonLabel) => {
    const [rejected] = rejectedOf([
      mention({
        detectedAddresses: [
          detected({ address: "bad", valid: false, validationFailure }),
        ],
      }),
    ]).rejectedAddresses;

    expect(rejected?.reasonLabel).toBe(reasonLabel);
  });

  it("de-duplicates by address and reason and lists every source name", () => {
    const bad = (validationFailure: DetectedAddressResponseValidationFailure) =>
      detected({ address: "0xBAD", valid: false, validationFailure });
    const result = rejectedOf([
      mention({
        id: "1",
        sourceName: "forum.example",
        detectedAddresses: [bad("MALFORMED")],
      }),
      mention({
        id: "2",
        sourceName: "x.example",
        detectedAddresses: [bad("MALFORMED")],
      }),
      mention({
        id: "3",
        sourceName: "forum.example",
        detectedAddresses: [bad("MALFORMED")],
      }),
      mention({
        id: "4",
        sourceName: "p2p.example",
        detectedAddresses: [bad("NETWORK_MISMATCH")],
      }),
    ]);

    expect(
      result.rejectedAddresses.map((r) => [r.reasonLabel, r.sourceNames])
    ).toEqual([
      ["Błędny format", "forum.example, x.example"],
      ["Niezgodna sieć", "p2p.example"],
    ]);
  });

  it("has an empty section with a zero count", () => {
    const result = rejectedOf([mention()]);

    expect(result.rejectedHeading).toBe("Adresy odrzucone (0)");
    expect(result.rejectedAddresses).toEqual([]);
  });
});
