import { describe, expect, it } from "vitest";

import { AnalysisResponseStatus } from "@/lib/api/generated/model";

import { nextPollInterval } from "./poll-interval";

describe("nextPollInterval", () => {
  it("polls every 1500 ms while the Analysis is running", () => {
    expect(nextPollInterval(AnalysisResponseStatus.RUNNING)).toBe(1500);
  });

  it("polls every 1500 ms while no data has arrived yet", () => {
    expect(nextPollInterval(undefined)).toBe(1500);
  });

  it.each([
    AnalysisResponseStatus.COMPLETED,
    AnalysisResponseStatus.PARTIAL,
    AnalysisResponseStatus.FAILED,
  ])("stops once the Analysis is %s", (status) => {
    expect(nextPollInterval(status)).toBe(false);
  });
});
