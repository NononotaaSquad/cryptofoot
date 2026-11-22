import { describe, expect, it } from "vitest";

import {
  compareAnalysisOrder,
  hasNewResults,
  isCovered,
  markAfterReview,
  parseMark,
  serializeMark,
  toAnalysisOrder,
  type AnalysisOrder,
  type ReviewedMark,
} from "./reviewed-mark";

const a: AnalysisOrder = { id: "a", startedAt: "2026-11-22T10:00:00Z" };
const b: AnalysisOrder = { id: "b", startedAt: "2026-11-22T10:05:00Z" };
const at = "2026-11-22T11:00:00Z";
const markOf = (analysis: AnalysisOrder): ReviewedMark => ({
  ...analysis,
  reviewedAt: at,
});

describe("compareAnalysisOrder", () => {
  it("orders by startedAt, then by id for the same instant", () => {
    expect(compareAnalysisOrder(b, a)).toBeGreaterThan(0);
    expect(compareAnalysisOrder(a, b)).toBeLessThan(0);
    const x = { id: "x1", startedAt: a.startedAt };
    const y = { id: "x2", startedAt: "2026-11-22T10:00:00.000Z" };
    expect(compareAnalysisOrder(y, x)).toBeGreaterThan(0);
    expect(compareAnalysisOrder(x, x)).toBe(0);
  });
});

describe("serializeMark / parseMark", () => {
  it("round-trips a mark", () => {
    expect(parseMark(serializeMark(markOf(a)))).toEqual(markOf(a));
  });

  it("reads no mark, a legacy timestamp, bad JSON and unknown shapes as none", () => {
    expect(parseMark(null)).toBeNull();
    expect(parseMark("2026-11-21T10:00:00.000Z")).toBeNull();
    expect(parseMark("{broken")).toBeNull();
    expect(parseMark("null")).toBeNull();
    expect(parseMark("42")).toBeNull();
    expect(parseMark(JSON.stringify({ v: 2, ...markOf(a) }))).toBeNull();
    expect(parseMark(JSON.stringify({ v: 1, id: "a" }))).toBeNull();
    expect(
      parseMark(JSON.stringify({ v: 1, ...markOf(a), startedAt: "nope" }))
    ).toBeNull();
    expect(
      parseMark(JSON.stringify({ v: 1, ...markOf(a), id: "" }))
    ).toBeNull();
  });
});

describe("markAfterReview", () => {
  it("marks the first Analysis and a newer one", () => {
    expect(markAfterReview(null, a, at)).toEqual(markOf(a));
    expect(markAfterReview(markOf(a), b, at)).toEqual(markOf(b));
  });

  it("never moves a newer watermark back, nor rewrites the same one", () => {
    expect(markAfterReview(markOf(b), a, at)).toBeNull();
    expect(markAfterReview(markOf(b), b, at)).toBeNull();
  });
});

describe("isCovered", () => {
  it("covers the marked Analysis and older ones, not newer, not without a mark", () => {
    expect(isCovered(markOf(b), a)).toBe(true);
    expect(isCovered(markOf(b), b)).toBe(true);
    expect(isCovered(markOf(a), b)).toBe(false);
    expect(isCovered(null, a)).toBe(false);
  });
});

describe("hasNewResults", () => {
  it("is false without a finished Analysis, also with a mark", () => {
    expect(hasNewResults(null, null)).toBe(false);
    expect(hasNewResults(undefined, markOf(a))).toBe(false);
  });

  it("is true for an unmarked, legacy or malformed Case", () => {
    expect(hasNewResults(a, null)).toBe(true);
    expect(hasNewResults(a, parseMark("2026-11-21T10:00:00.000Z"))).toBe(true);
  });

  it("A reviewed -> no new results; B finished -> new results again", () => {
    expect(hasNewResults(a, markOf(a))).toBe(false);
    expect(hasNewResults(b, markOf(a))).toBe(true);
  });

  it("a click for A while B finishes leaves B new", () => {
    const stored = markAfterReview(null, a, at);
    expect(hasNewResults(b, stored)).toBe(true);
  });

  it("marking an older report keeps the newer watermark", () => {
    const stored = markOf(b);
    const after = markAfterReview(stored, a, at) ?? stored;
    expect(hasNewResults(b, after)).toBe(false);
  });
});

describe("toAnalysisOrder", () => {
  it("needs an id and a start", () => {
    expect(toAnalysisOrder({ id: "a" })).toBeNull();
    expect(toAnalysisOrder({ startedAt: a.startedAt })).toBeNull();
    expect(toAnalysisOrder(a)).toEqual(a);
  });
});
