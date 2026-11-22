import { describe, expect, it } from "vitest";

import type { ReviewedMark } from "@/features/case-details/reviewed-mark";
import type {
  AnalysisResponse,
  AnalysisResponseStatus,
} from "@/lib/api/generated/model";

import {
  buildCaseList,
  countNewResults,
  isNewResults,
  matchesQuery,
  reviewedOpen,
  type ReviewedMarks,
} from "./case-list-model";
import type { CaseListItem } from "./case-row-view-model";

const wallet = "0x71C7656EC7ab88b098defB751B7401B5f6d8976F";

function analysis(
  id: string,
  status: AnalysisResponseStatus,
  startedAt: string,
  extra: Partial<AnalysisResponse> = {}
): AnalysisResponse {
  return {
    id,
    status,
    startedAt,
    finishedAt: status === "RUNNING" ? null : startedAt,
    ...extra,
  };
}

function done(
  id: string,
  startedAt: string,
  level: number | null,
  count = 1
): AnalysisResponse {
  return analysis(id, "COMPLETED", startedAt, {
    highestConfidenceLevel: level,
    attributionCount: count,
  });
}

// A Case whose latest Analysis is terminal: both fields name the same one.
function finished(
  id: string,
  latest: AnalysisResponse,
  extra: Partial<CaseListItem> = {}
): CaseListItem {
  return {
    id,
    identifiers: { nickname: id },
    createdAt: "2026-11-01T10:00:00Z",
    latestAnalysis: latest,
    latestFinishedAnalysis: latest,
    ...extra,
  };
}

function markOf(id: string, startedAt: string): ReviewedMark {
  return { id, startedAt, reviewedAt: "2026-11-22T00:00:00Z" };
}

const noMarks: ReviewedMarks = new Map();

describe("matchesQuery", () => {
  const item: CaseListItem = {
    id: "c1",
    caseNumber: "2026/0006",
    identifiers: {
      fullName: "Jan Kowalski",
      nickname: "Krypto_Janek",
      email: "Jan.Kowalski@example.com",
      profileUrl: "https://forum.example.com/u/jk",
      walletAddress: wallet,
    },
    createdAt: "2026-11-01T10:00:00Z",
  };

  it("matches every identifier, the full name, the full wallet and the number", () => {
    for (const query of [
      "krypto_janek",
      "jan kowalski",
      "jan.kowalski@example.com",
      "forum.example.com/u/jk",
      wallet,
      wallet.toLowerCase(),
      "2026/0006",
      "0006",
    ]) {
      expect(matchesQuery(item, query), query).toBe(true);
    }
  });

  it("trims, ignores case, and treats an empty query as no search", () => {
    expect(matchesQuery(item, "  KOWALSKI  ")).toBe(true);
    expect(matchesQuery(item, "")).toBe(true);
    expect(matchesQuery(item, "   ")).toBe(true);
  });

  it("is a literal substring: no fuzzy match, no pattern", () => {
    expect(matchesQuery(item, "kowalsky")).toBe(false);
    expect(matchesQuery(item, ".*")).toBe(false);
    expect(matchesQuery(item, "kowal.ki")).toBe(false);
    expect(matchesQuery(item, "[a-z]+")).toBe(false);
    expect(matchesQuery(item, "<b>")).toBe(false);
  });

  it("does not match the shortened display of a wallet", () => {
    expect(matchesQuery(item, "0x71C7…976F")).toBe(false);
  });

  it("does not search a Case Number the API does not return", () => {
    expect(matchesQuery({ ...item, caseNumber: undefined }, "2026/0006")).toBe(
      false
    );
  });
});

describe("isNewResults", () => {
  const a = done("a", "2026-11-21T10:00:00Z", 50);
  const b = done("b", "2026-11-21T11:00:00Z", 60);

  it("is new without a mark, reviewed once the mark covers the finished one", () => {
    expect(isNewResults(finished("c", a), noMarks)).toBe(true);
    const marks: ReviewedMarks = new Map([
      ["c", markOf("a", a.startedAt ?? "")],
    ]);
    expect(isNewResults(finished("c", a), marks)).toBe(false);
  });

  it("a finished B after reviewed A is new again", () => {
    const marks: ReviewedMarks = new Map([
      ["c", markOf("a", a.startedAt ?? "")],
    ]);
    expect(isNewResults(finished("c", b), marks)).toBe(true);
  });

  it("a FAILED finish is a new result", () => {
    const failed = analysis("f", "FAILED", "2026-11-21T12:00:00Z");
    expect(isNewResults(finished("c", failed), noMarks)).toBe(true);
  });

  it("a new RUNNING alone changes nothing: the finished one decides", () => {
    const running = analysis("r", "RUNNING", "2026-11-21T13:00:00Z");
    const marks: ReviewedMarks = new Map([
      ["c", markOf("a", a.startedAt ?? "")],
    ]);
    const item = finished("c", a, { latestAnalysis: running });
    expect(isNewResults(item, marks)).toBe(false);
    expect(isNewResults(item, noMarks)).toBe(true);
  });

  it("a Case without a finished Analysis is not new", () => {
    const running = analysis("r", "RUNNING", "2026-11-21T13:00:00Z");
    expect(
      isNewResults(
        finished("c", running, { latestFinishedAnalysis: null }),
        noMarks
      )
    ).toBe(false);
    expect(
      isNewResults(
        finished("c", a, {
          latestAnalysis: null,
          latestFinishedAnalysis: null,
        }),
        noMarks
      )
    ).toBe(false);
  });

  it("a mark of an older Analysis does not cover a newer one", () => {
    const marks: ReviewedMarks = new Map([
      ["c", markOf("a", a.startedAt ?? "")],
    ]);
    expect(isNewResults(finished("c", b), marks)).toBe(true);
  });
});

describe("countNewResults", () => {
  it("counts Cases, not Analyses, and ignores the search", () => {
    const items = [
      finished("x", done("x1", "2026-11-21T10:00:00Z", 10)),
      finished("y", done("y1", "2026-11-21T10:00:00Z", 10)),
      finished("z", done("z1", "2026-11-21T10:00:00Z", 10)),
    ];
    const marks: ReviewedMarks = new Map([
      ["z", markOf("z1", "2026-11-21T10:00:00Z")],
    ]);
    expect(countNewResults(items, marks)).toBe(2);
  });
});

describe("buildCaseList", () => {
  it("filters first: counts and groups describe the matching Cases only", () => {
    const items = [
      finished("anna", done("a1", "2026-11-21T10:00:00Z", 60), {
        identifiers: { fullName: "Anna Nowak" },
      }),
      finished("jan", done("j1", "2026-11-21T10:00:00Z", 90), {
        identifiers: { fullName: "Jan Kowalski" },
      }),
      finished("jan2", done("j2", "2026-11-21T10:00:00Z", 40), {
        identifiers: { fullName: "Jan Zielinski" },
      }),
    ];
    const marks: ReviewedMarks = new Map([
      ["jan2", markOf("j2", "2026-11-21T10:00:00Z")],
    ]);
    const model = buildCaseList(items, " jan ", marks);
    expect(model.matching).toBe(2);
    expect(model.newRows.map((r) => r.caseId)).toEqual(["jan"]);
    expect(model.reviewedRows.map((r) => r.caseId)).toEqual(["jan2"]);
  });

  it("orders New Results by known level desc, unknown last, then date, then id", () => {
    const at = (h: number) => `2026-11-21T${String(h).padStart(2, "0")}:00:00Z`;
    const running = analysis("r", "RUNNING", at(20));
    const items = [
      finished("low", done("l", at(9), 40)),
      finished("high-old", done("h1", at(8), 95)),
      finished("high-new", done("h2", at(10), 95)),
      finished("failed", analysis("f", "FAILED", at(21))),
      // New (finished A unreviewed) while a RUNNING one is latest: its older
      // 99% must not lift it above the others.
      finished("running", done("old", at(1), 99), {
        latestAnalysis: running,
      }),
      finished("zero", done("z", at(11), 0, 2)),
    ];
    const model = buildCaseList(items, "", noMarks);
    expect(model.newRows.map((r) => r.caseId)).toEqual([
      "high-new",
      "high-old",
      "low",
      "zero",
      // unknown: by last activity (RUNNING start 20:00 before FAILED 21:00)
      "failed",
      "running",
    ]);
  });

  it("breaks a full tie by id, stably", () => {
    const same = "2026-11-21T10:00:00Z";
    const items = [
      finished("a", done("a1", same, 50)),
      finished("c", done("c1", same, 50)),
      finished("b", done("b1", same, 50)),
    ];
    const first = buildCaseList(items, "", noMarks).newRows.map(
      (r) => r.caseId
    );
    const second = buildCaseList([...items].reverse(), "", noMarks).newRows.map(
      (r) => r.caseId
    );
    expect(first).toEqual(["c", "b", "a"]);
    expect(second).toEqual(first);
  });

  it("orders Reviewed by last activity, createdAt for a Case without Analysis", () => {
    const marks: ReviewedMarks = new Map([
      ["old", markOf("o", "2026-11-10T10:00:00Z")],
      ["new", markOf("n", "2026-11-20T10:00:00Z")],
    ]);
    const items = [
      finished("old", done("o", "2026-11-10T10:00:00Z", 50)),
      finished("new", done("n", "2026-11-20T10:00:00Z", 50)),
      {
        id: "none",
        identifiers: { nickname: "none" },
        createdAt: "2026-11-15T10:00:00Z",
        latestAnalysis: null,
        latestFinishedAnalysis: null,
      },
    ];
    const model = buildCaseList(items, "", marks);
    expect(model.newRows).toEqual([]);
    expect(model.reviewedRows.map((r) => r.caseId)).toEqual([
      "new",
      "none",
      "old",
    ]);
  });

  it("puts a Case without a finished Analysis in the non-new group", () => {
    const running = analysis("r", "RUNNING", "2026-11-21T13:00:00Z");
    const model = buildCaseList(
      [
        {
          id: "only-running",
          identifiers: { nickname: "x" },
          createdAt: "2026-11-01T10:00:00Z",
          latestAnalysis: running,
          latestFinishedAnalysis: null,
        },
      ],
      "",
      noMarks
    );
    expect(model.newRows).toEqual([]);
    expect(model.reviewedRows).toHaveLength(1);
  });

  it("returns an empty model for no Cases and for no match", () => {
    expect(buildCaseList([], "", noMarks)).toEqual({
      matching: 0,
      newRows: [],
      reviewedRows: [],
    });
    expect(
      buildCaseList(
        [finished("a", done("a1", "2026-11-21T10:00:00Z", 1))],
        "zzz",
        noMarks
      ).matching
    ).toBe(0);
  });
});

describe("reviewedOpen", () => {
  const base = {
    searching: false,
    hasMatches: false,
    choice: false,
    searchOverride: null,
  };

  it("follows the Analyst's choice outside a search", () => {
    expect(reviewedOpen(base)).toBe(false);
    expect(reviewedOpen({ ...base, choice: true })).toBe(true);
  });

  it("opens by itself when a search matches the Reviewed group", () => {
    expect(reviewedOpen({ ...base, searching: true, hasMatches: true })).toBe(
      true
    );
  });

  it("without a match keeps the choice, and the Analyst's own toggle wins", () => {
    expect(reviewedOpen({ ...base, searching: true, choice: true })).toBe(true);
    expect(
      reviewedOpen({
        ...base,
        searching: true,
        hasMatches: true,
        searchOverride: false,
      })
    ).toBe(false);
  });

  it("restores the choice made before the search once it ends", () => {
    const before = { ...base, choice: false };
    expect(reviewedOpen({ ...before, searching: true, hasMatches: true })).toBe(
      true
    );
    expect(reviewedOpen(before)).toBe(false);
  });
});
