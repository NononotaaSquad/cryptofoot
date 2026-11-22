import { describe, expect, it } from "vitest";

import type {
  AnalysisResponse,
  AnalysisResponseStatus,
} from "@/lib/api/generated/model";

import {
  leadCountText,
  shortenWallet,
  toCaseRow,
  toCaseRows,
  toRowConfidence,
  toRowDate,
  toRowHref,
  toRowStatus,
  type CaseListItem,
} from "./case-row-view-model";

function analysis(
  status: AnalysisResponseStatus,
  extra: Partial<AnalysisResponse> = {}
): AnalysisResponse {
  return {
    id: `an-${status}`,
    status,
    startedAt: "2026-11-21T13:00:00Z",
    finishedAt: status === "RUNNING" ? null : "2026-11-21T13:32:00Z",
    ...extra,
  };
}

const wallet = "0x71C7656EC7ab88b098defB751B7401B5f6d8976F";

function item(extra: Partial<CaseListItem> = {}): CaseListItem {
  return {
    id: "case-1",
    identifiers: { nickname: "krypto_janek" },
    createdAt: "2026-11-20T10:00:00Z",
    latestAnalysis: null,
    latestFinishedAnalysis: null,
    ...extra,
  };
}

describe("toRowStatus", () => {
  it.each([
    ["COMPLETED", "Zakończona"],
    ["PARTIAL", "Częściowa"],
    ["RUNNING", "W toku"],
    ["FAILED", "Nieudana"],
  ] as const)("%s reads %s", (status, label) => {
    expect(toRowStatus(analysis(status))).toEqual({ key: status, label });
  });

  it("is 'Bez analizy' without an Analysis", () => {
    expect(toRowStatus(null)).toEqual({ key: "NONE", label: "Bez analizy" });
    expect(toRowStatus(undefined).key).toBe("NONE");
  });
});

describe("toRowConfidence", () => {
  it("shows the level and the number of leads of a finished result", () => {
    expect(
      toRowConfidence(
        analysis("COMPLETED", {
          highestConfidenceLevel: 95,
          attributionCount: 3,
        })
      )
    ).toEqual({
      kind: "level",
      text: "95%",
      count: "3 powiązania",
      highlight: true,
    });
    expect(
      toRowConfidence(
        analysis("PARTIAL", { highestConfidenceLevel: 62, attributionCount: 1 })
      )
    ).toMatchObject({ text: "62%", count: "1 powiązanie", highlight: false });
  });

  it("highlights from 80, not at 79", () => {
    const at = (level: number) =>
      toRowConfidence(
        analysis("COMPLETED", {
          highestConfidenceLevel: level,
          attributionCount: 2,
        })
      );
    expect(at(79)).toMatchObject({ highlight: false });
    expect(at(80)).toMatchObject({ highlight: true });
  });

  it("keeps 0% with leads a value, never 'no data' and never highlighted", () => {
    expect(
      toRowConfidence(
        analysis("COMPLETED", {
          highestConfidenceLevel: 0,
          attributionCount: 2,
        })
      )
    ).toEqual({
      kind: "level",
      text: "0%",
      count: "2 powiązania",
      highlight: false,
    });
  });

  it("says 'Brak powiązań' for a finished result without leads", () => {
    expect(
      toRowConfidence(
        analysis("COMPLETED", {
          highestConfidenceLevel: null,
          attributionCount: 0,
        })
      )
    ).toEqual({ kind: "noLeads", text: "Brak powiązań" });
  });

  it("is unknown, not zero, when the maximum or the count is not stated", () => {
    expect(
      toRowConfidence(
        analysis("COMPLETED", {
          highestConfidenceLevel: null,
          attributionCount: 4,
        })
      )
    ).toEqual({ kind: "none", text: "–" });
    expect(toRowConfidence(analysis("COMPLETED"))).toEqual({
      kind: "none",
      text: "–",
    });
    expect(
      toRowConfidence(analysis("COMPLETED", { highestConfidenceLevel: 70 }))
    ).toEqual({ kind: "level", text: "70%", count: null, highlight: false });
  });

  it("shows nothing for RUNNING, FAILED or no Analysis, even with saved figures", () => {
    for (const status of ["RUNNING", "FAILED"] as const) {
      expect(
        toRowConfidence(
          analysis(status, { highestConfidenceLevel: 99, attributionCount: 5 })
        )
      ).toEqual({ kind: "none", text: "–" });
    }
    expect(toRowConfidence(null)).toEqual({ kind: "none", text: "–" });
  });
});

describe("leadCountText", () => {
  it.each([
    [1, "1 powiązanie"],
    [2, "2 powiązania"],
    [4, "4 powiązania"],
    [5, "5 powiązań"],
    [12, "12 powiązań"],
    [22, "22 powiązania"],
  ])("%i -> %s", (count, text) => {
    expect(leadCountText(count)).toBe(text);
  });
});

describe("toRowDate", () => {
  it("uses the end of a finished Analysis, the start as a fallback", () => {
    expect(toRowDate(analysis("COMPLETED"))).toBe("21.11.2026, 14:32");
    expect(toRowDate(analysis("FAILED", { finishedAt: null }))).toBe(
      "21.11.2026, 14:00"
    );
  });

  it("uses the start of a RUNNING Analysis and a dash without one", () => {
    expect(toRowDate(analysis("RUNNING"))).toBe("21.11.2026, 14:00");
    expect(toRowDate(null)).toBe("–");
    expect(
      toRowDate(analysis("COMPLETED", { finishedAt: "x", startedAt: "y" }))
    ).toBe("–");
  });

  it("reads in Europe/Warsaw whatever the machine zone", () => {
    expect(
      toRowDate(analysis("COMPLETED", { finishedAt: "2026-07-01T10:05:00Z" }))
    ).toBe("01.07.2026, 12:05");
  });
});

describe("toRowHref", () => {
  const finished = analysis("COMPLETED", { id: "done" });
  const running = analysis("RUNNING", { id: "run" });

  it("opens the exact latest Analysis when it is terminal", () => {
    expect(toRowHref("c1", finished, finished)).toBe("/cases/c1?analysis=done");
    expect(toRowHref("c1", analysis("FAILED", { id: "bad" }), null)).toBe(
      "/cases/c1?analysis=bad"
    );
  });

  it("opens the finished one while the latest runs, else the running one", () => {
    expect(toRowHref("c1", running, finished)).toBe("/cases/c1?analysis=done");
    expect(toRowHref("c1", running, null)).toBe("/cases/c1?analysis=run");
  });

  it("opens the Case itself without an Analysis", () => {
    expect(toRowHref("c1", null, null)).toBe("/cases/c1");
  });

  it("escapes the ids", () => {
    expect(toRowHref("a/b", analysis("COMPLETED", { id: "x y" }), null)).toBe(
      "/cases/a%2Fb?analysis=x%20y"
    );
  });
});

describe("toCaseRow", () => {
  it("does not mix a running status with the earlier result", () => {
    const row = toCaseRow(
      item({
        latestAnalysis: analysis("RUNNING", {
          id: "run",
          highestConfidenceLevel: null,
          attributionCount: null,
        }),
        latestFinishedAnalysis: analysis("COMPLETED", {
          id: "done",
          highestConfidenceLevel: 95,
          attributionCount: 3,
        }),
      })
    );
    expect(row?.status.key).toBe("RUNNING");
    expect(row?.confidence).toEqual({ kind: "none", text: "–" });
    expect(row?.date).toBe("21.11.2026, 14:00");
    expect(row?.href).toBe("/cases/case-1?analysis=done");
  });

  it("a Case without an Analysis has three plain cells and opens the Case", () => {
    const row = toCaseRow(item());
    expect(row).toMatchObject({
      status: { key: "NONE", label: "Bez analizy" },
      confidence: { kind: "none", text: "–" },
      date: "–",
      href: "/cases/case-1",
    });
  });

  it("shows the label, two identifiers and a true '+N więcej'", () => {
    const row = toCaseRow(
      item({
        identifiers: {
          fullName: "Jan Kowalski",
          nickname: "krypto_janek",
          email: "jan.kowalski@example.com",
          login: "jk",
          walletAddress: wallet,
        },
      })
    );
    expect(row?.title).toBe("Jan Kowalski");
    expect(row?.identifiers.map((i) => [i.label, i.full])).toEqual([
      ["Nick", "krypto_janek"],
      ["Login", "jk"],
    ]);
    expect(row?.hiddenIdentifierCount).toBe(2);
  });

  it("without a full name the first identifier is the title and also listed", () => {
    const row = toCaseRow(
      item({ identifiers: { nickname: "btc_wilk", messengerId: "@btcwilk" } })
    );
    expect(row?.title).toBe("btc_wilk");
    expect(row?.identifiers.map((i) => i.full)).toEqual([
      "btc_wilk",
      "@btcwilk",
    ]);
    expect(row?.hiddenIdentifierCount).toBe(0);
  });

  it("shortens long addresses for the eye and keeps the full value", () => {
    const row = toCaseRow(item({ identifiers: { walletAddress: wallet } }));
    expect(row?.title).toBe("0x71C7…976F");
    expect(row?.titleFull).toBe(wallet);
    expect(row?.identifiers[0]).toMatchObject({
      text: "0x71C7…976F",
      full: wallet,
    });

    const profile = toCaseRow(
      item({
        identifiers: {
          profileUrl: `https://www.forum.example/u/${"a".repeat(60)}`,
        },
      })
    );
    expect(profile?.identifiers[0]?.text.endsWith("…")).toBe(true);
    expect(profile?.identifiers[0]?.text.startsWith("forum.example/u/")).toBe(
      true
    );
  });

  it("hides a missing Case Number and never invents one", () => {
    expect(toCaseRow(item())?.caseNumber).toBeNull();
    expect(toCaseRow(item({ caseNumber: "  " }))?.caseNumber).toBeNull();
    expect(toCaseRow(item({ caseNumber: "2026/0006" }))?.caseNumber).toBe(
      "2026/0006"
    );
  });

  it("skips an item without an id and keeps the received order", () => {
    const rows = toCaseRows([
      item({ id: "b" }),
      item({ id: undefined }),
      item({ id: "a" }),
    ]);
    expect(rows.map((r) => r.caseId)).toEqual(["b", "a"]);
  });
});

describe("shortenWallet", () => {
  it("leaves a short value alone", () => {
    expect(shortenWallet("0x1234")).toBe("0x1234");
  });
});
