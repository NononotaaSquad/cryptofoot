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
  type PremiseAnswersResponse,
  type PremiseResponse,
  PremiseResponseDismissalReason,
  PremiseResponseKind,
  PremiseResponseStatus,
  PremiseResponseUndeterminedItem,
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
      ReportResponseLimitationsItem.MODEL_ASSESSMENT_UNAVAILABLE,
      "Model nie ocenił części lub wszystkich przesłanek; te przesłanki oceniono samymi regułami.",
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
      // confidence-2 and confidence-3
      "OWN_DECLARATION",
      "DECLARED_BY_ANOTHER_PERSON",
      "WITHDRAWN_ADDRESS",
      "FOR_REVIEW",
      "WARNING_DISMISSED",
      "CLASSIFICATION_CAP",
      "MODEL_ASSISTED",
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

function premise(values: Partial<PremiseResponse> = {}) {
  return {
    id: "premise-1",
    mentionId: "mention-1",
    sourceName: "forum.example",
    url: "https://forum.example/t/1",
    author: "krypto_janek",
    collectedAt: "2026-11-21T13:33:00Z",
    text: "mój adres to bc1qexampleaddress, dzięki",
    addressStart: 13,
    addressLength: 18,
    status: "COUNTED",
    kind: "DISCUSSION",
    points: 40,
    dismissalReason: null,
    assessedBy: "RULE",
    facts: {},
    answers: null,
    undetermined: [],
    withdrawn: false,
    ...values,
  } satisfies PremiseResponse;
}

function answers(values: Partial<PremiseAnswersResponse> = {}) {
  return {
    kind: {
      ownDeclaration: 0.81,
      saleOrSettlement: 0.1,
      discussion: 0.06,
      massComment: 0.03,
    },
    declaration: { yes: 0.925, no: 0.075 },
    warning: { yes: 0.04, no: 0.96 },
    requestId: "req-1",
    ...values,
  } satisfies PremiseAnswersResponse;
}

function onlyPremise(values: Partial<PremiseResponse> = {}) {
  const [first] = onlyAttribution({ premises: [premise(values)] }).premises;
  if (first === undefined) throw new Error("expected one Premise");
  return first;
}

// The backend may send a code newer than the generated client.
const NEW_STATUS = JSON.parse('"NEW_STATUS"') as PremiseResponseStatus; // `as`: simulates an untyped response body
const NEW_REASON = JSON.parse('"NEW_REASON"') as PremiseResponseDismissalReason; // `as`: simulates an untyped response body

describe("toReportView Premises", () => {
  it.each([
    [PremiseResponseStatus.COUNTED, "counted", "Zaliczona"],
    [PremiseResponseStatus.FOR_REVIEW, "forReview", "Do weryfikacji"],
    [PremiseResponseStatus.DISMISSED, "dismissed", "Odrzucona"],
    [NEW_STATUS, "unknown", "NEW_STATUS"],
  ])(
    "gives the status %s the icon %s and the word %s",
    (status, icon, label) => {
      expect(onlyPremise({ status }).status).toEqual({ icon, label });
    }
  );

  it.each([
    [PremiseResponseDismissalReason.WARNING, "Ostrzeżenie przed tym adresem"],
    [
      PremiseResponseDismissalReason.DECLARED_BY_ANOTHER_PERSON,
      "Adres podany przez inną osobę",
    ],
    [
      PremiseResponseDismissalReason.NO_PERSON_LINK,
      "Brak powiązania z badaną osobą",
    ],
    [PremiseResponseDismissalReason.CONFLICTING_EMAIL, "E-mail innej osoby"],
    [NEW_REASON, "NEW_REASON"],
    [null, null],
  ])("labels the dismissal reason %s as %s", (dismissalReason, label) => {
    expect(onlyPremise({ dismissalReason }).reason).toBe(label);
  });

  it.each([
    [PremiseResponseKind.OWN_PROFILE, 80, "Profil badanej osoby", "80"],
    [PremiseResponseKind.OWN_DECLARATION, 80, "Własna deklaracja", "80"],
    [
      PremiseResponseKind.SALE_OR_SETTLEMENT,
      60,
      "Sprzedaż lub rozliczenie",
      "60",
    ],
    [PremiseResponseKind.DISCUSSION, 40, "Dyskusja", "40"],
    [PremiseResponseKind.MASS_COMMENT, 10, "Wpis masowy", "10"],
    [null, 0, null, "0"],
  ])("labels the kind %s with %i points", (kind, points, label, pointsText) => {
    const result = onlyPremise({ kind, points });

    expect(result.kind).toBe(label);
    expect(result.points).toBe(pointsText);
  });

  it("orders Zaliczona, Do weryfikacji, Odrzucona, keeps the API order in a group and counts all", () => {
    const result = onlyAttribution({
      premises: [
        premise({ id: "d", status: "DISMISSED" }),
        premise({ id: "c1", status: "COUNTED" }),
        premise({ id: "r", status: "FOR_REVIEW" }),
        premise({ id: "c2", status: "COUNTED" }),
      ],
    });

    expect(result.premisesHeading).toBe("Przesłanki (4)");
    expect(result.premises.map((p) => [p.key, p.title])).toEqual([
      ["c1", "Przesłanka 1"],
      ["c2", "Przesłanka 2"],
      ["r", "Przesłanka 3"],
      ["d", "Przesłanka 4"],
    ]);
  });

  it.each([
    ["at the start", "bc1qabc to mój", 0, 7, "", "bc1qabc", " to mój"],
    ["in the middle", "mój bc1qabc adres", 4, 7, "mój ", "bc1qabc", " adres"],
    ["at the end", "mój adres bc1qabc", 10, 7, "mój adres ", "bc1qabc", ""],
    // The emoji is two UTF-16 code units, as in the backend's indices.
    ["after an emoji", "💰 bc1qabc ok", 3, 7, "💰 ", "bc1qabc", " ok"],
    ["outside the window", "mój bc1qabc", 8, 7, "mój bc1qabc", null, ""],
    [
      "without a position",
      "mój bc1qabc",
      undefined,
      undefined,
      "mój bc1qabc",
      null,
      "",
    ],
  ])(
    "splits the window for an address %s",
    (_name, text, addressStart, addressLength, before, address, after) => {
      expect(onlyPremise({ text, addressStart, addressLength }).window).toEqual(
        { before, address, after }
      );
    }
  );

  it("writes one line per code fact, each assessed by the rules, and none for an absent fact", () => {
    const assessedBy = "Oceniono: reguła";
    const result = onlyPremise({
      facts: {
        authorIsPerson: "UNKNOWN",
        underProfile: true,
        identifierTypes: ["NICKNAME", "EMAIL"],
        identifierDistance: 42,
        conflictingEmail: false,
        addressCount: 2,
        host: "forum.example",
      },
    });

    expect(result.facts).toEqual([
      { label: "Autor to badana osoba", value: "nieznany", assessedBy },
      { label: "Strona pod profilem osoby", value: "tak", assessedBy },
      {
        label: "Identyfikatory osoby w tekście",
        value: "nick, e-mail",
        assessedBy,
      },
      {
        label: "Odległość identyfikatora od adresu (znaki)",
        value: "42",
        assessedBy,
      },
      { label: "E-mail innej osoby w tekście", value: "nie", assessedBy },
      { label: "Liczba adresów we wzmiance", value: "2", assessedBy },
      { label: "Serwis", value: "forum.example", assessedBy },
    ]);
    expect(
      onlyPremise({
        facts: { authorIsPerson: "YES", identifierDistance: null },
      }).facts
    ).toEqual([{ label: "Autor to badana osoba", value: "tak", assessedBy }]);
  });

  it("has the Mention's source, link, author and collection time, and no link for a non-http address", () => {
    expect(onlyPremise()).toMatchObject({
      sourceName: "forum.example",
      url: "https://forum.example/t/1",
      urlText: "https://forum.example/t/1",
      author: "krypto_janek",
      collectedAt: "21.11.2026, 14:33",
    });
    expect(
      onlyPremise({ url: "javascript:alert(1)", author: null })
    ).toMatchObject({
      url: null,
      urlText: "javascript:alert(1)",
      author: null,
    });
  });

  it("names in a Punktacja line only the Premises of its premiseIds and shows the cap with a minus", () => {
    const result = onlyAttribution({
      mentions: [mention({ sourceName: "other.example" })],
      premises: [
        premise({
          id: "p-dismissed",
          status: "DISMISSED",
          sourceName: "x.example",
        }),
        premise({ id: "p-counted", sourceName: "GitHub" }),
      ],
      scoringRules: [
        {
          rule: "OWN_PROFILE",
          contribution: 80,
          premiseIds: ["p-counted", "p-gone"],
        },
        {
          rule: "WARNING_DISMISSED",
          contribution: 0,
          premiseIds: ["p-dismissed"],
        },
        { rule: "CLASSIFICATION_CAP", contribution: -21, premiseIds: [] },
      ],
    });

    expect(result.scoringLines).toEqual([
      { label: "Adres na profilu osoby (przesłanka 1: GitHub)", points: "80" },
      {
        label:
          "Ostrzeżenie przed adresem, przesłanka odrzucona (przesłanka 2: x.example)",
        points: "0",
      },
      {
        label: "Ograniczenie z powodu niedostępnej klasyfikacji",
        points: "-21",
      },
    ]);
  });

  it.each([[undefined], [[]]])(
    "has no Premise block and keeps Punktacja as before for premises %j",
    (premises) => {
      const result = onlyAttribution({
        premises,
        mentions: [mention()],
        scoringRules: [
          { rule: "DISCUSSION", contribution: 40, premiseIds: [] },
        ],
      });

      expect(result.premises).toEqual([]);
      expect(result.scoringLines).toEqual([
        { label: "Adres w dyskusji (forum.example)", points: "40" },
      ]);
    }
  );

  it.each([
    [0.3, 0.7, false, "nie (30%)"],
    [0.5, 0.5, true, "niepewne (50%)"],
    [0.7, 0.3, false, "tak (70%)"],
    [0.925, 0.075, false, "tak (93%)"],
    // The word follows the backend's flag, not the percentage.
    [0.695, 0.305, true, "niepewne (70%)"],
  ])(
    "writes the declaration answer %f / %f (undetermined: %s) as %s",
    (yes, no, flagged, text) => {
      const result = onlyPremise({
        assessedBy: "MODEL",
        answers: answers({ declaration: { yes, no } }),
        undetermined: flagged
          ? [PremiseResponseUndeterminedItem.DECLARATION]
          : [],
      });

      expect(result.answers[1]).toEqual({
        label: "Deklaracja",
        value: text,
        assessedBy: "Oceniono: model",
      });
    }
  );

  it("writes the kind answer as the top option or niepewne, and every raw number in the technical details", () => {
    const result = onlyPremise({ assessedBy: "MODEL", answers: answers() });

    expect(result.kindAssessedBy).toBe("Oceniono: model");
    expect(result.answers.map((a) => `${a.label}: ${a.value}`)).toEqual([
      "Rodzaj wpisu: Własna deklaracja (81%)",
      "Deklaracja: tak (93%)",
      "Ostrzeżenie: nie (4%)",
    ]);
    expect(result.technicalDetails).toEqual([
      { label: "Rodzaj wpisu, Własna deklaracja", value: "0.81" },
      { label: "Rodzaj wpisu, Sprzedaż lub rozliczenie", value: "0.1" },
      { label: "Rodzaj wpisu, Dyskusja", value: "0.06" },
      { label: "Rodzaj wpisu, Wpis masowy", value: "0.03" },
      { label: "Deklaracja, tak", value: "0.93" },
      { label: "Deklaracja, nie", value: "0.08" },
      { label: "Ostrzeżenie, tak", value: "0.04" },
      { label: "Ostrzeżenie, nie", value: "0.96" },
    ]);
    expect(
      onlyPremise({ answers: answers(), undetermined: ["KIND"] }).answers[0]
        ?.value
    ).toBe("niepewne (81%)");
  });

  it("has no answer lines and no technical details when the model did not answer", () => {
    const result = onlyPremise({ assessedBy: "RULE", answers: null });

    expect(result.kindAssessedBy).toBe("Oceniono: reguła");
    expect(result.answers).toEqual([]);
    expect(result.technicalDetails).toBe(null);
  });

  it("keeps Zaliczona and adds the note for a withdrawn address", () => {
    expect(onlyPremise({ withdrawn: true })).toMatchObject({
      status: { label: "Zaliczona" },
      withdrawnNote: "Adres wycofany przez autora",
    });
    expect(onlyPremise().withdrawnNote).toBe(null);
  });
});

describe("toReportView summary versions", () => {
  it("shows the algorithm version, the mode and the model versions that are set", () => {
    expect(
      summaryOf({
        algorithmVersion: "confidence-3",
        assessmentMode: "WITH_MODEL",
        modelVersion: "jev-1",
        questionSetVersion: "questions-1",
      }).versionLines
    ).toEqual([
      { label: "Wersja algorytmu", value: "confidence-3" },
      { label: "Tryb oceny", value: "z modelem" },
      { label: "Wersja modelu", value: "jev-1" },
      { label: "Wersja zestawu pytań", value: "questions-1" },
    ]);
    expect(
      summaryOf({
        algorithmVersion: "confidence-2",
        assessmentMode: "RULES_ONLY",
        modelVersion: null,
      }).versionLines
    ).toEqual([
      { label: "Wersja algorytmu", value: "confidence-2" },
      { label: "Tryb oceny", value: "tylko reguły" },
    ]);
    expect(summaryOf({ algorithmVersion: null }).versionLines).toEqual([]);
  });
});
