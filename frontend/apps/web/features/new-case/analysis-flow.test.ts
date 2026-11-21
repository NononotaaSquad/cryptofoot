import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ApiError } from "@/lib/api/client";
import { getAnalysis } from "@/lib/api/generated/analysis-controller/analysis-controller";
import {
  createCase,
  startAnalysis,
} from "@/lib/api/generated/case-controller/case-controller";
import type { AnalysisResponseStatus } from "@/lib/api/generated/model";

import {
  type CreatedCase,
  POLL_INTERVAL_MS,
  StartAnalysisError,
  runAnalysis,
} from "./analysis-flow";
import type { NewCaseIdentifiers } from "./new-case-rules";

vi.mock("@/lib/api/generated/case-controller/case-controller", () => ({
  createCase: vi.fn(),
  startAnalysis: vi.fn(),
}));
vi.mock("@/lib/api/generated/analysis-controller/analysis-controller", () => ({
  getAnalysis: vi.fn(),
}));

const identifiers: NewCaseIdentifiers = {
  nickname: "satoshi",
  login: null,
  email: null,
  messengerId: null,
  profileUrl: null,
  walletAddress: null,
  fullName: null,
};

// The flow reads only `data`; `never` fits every generated envelope type,
// which spares building full response bodies in each test.
function envelope(data: unknown, status: number) {
  return { data, status, headers: new Headers() } as never;
}

function analysisWith(status: AnalysisResponseStatus) {
  return envelope({ id: "analysis-1", caseId: "case-1", status }, 200);
}

function fieldError(field: string | null, message: string) {
  return new ApiError({
    httpStatus: 400,
    statusName: "400 BAD_REQUEST",
    message: "Niepoprawne dane",
    fieldErrors: [{ field, message }],
  });
}

// Lets the flow reach its next await without moving the clock.
async function settle() {
  await vi.advanceTimersByTimeAsync(0);
}

describe("runAnalysis", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.mocked(createCase).mockResolvedValue(envelope({ id: "case-1" }, 201));
    vi.mocked(startAnalysis).mockResolvedValue(
      envelope({ id: "analysis-1", caseId: "case-1", status: "RUNNING" }, 202)
    );
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.resetAllMocks();
  });

  it("creates the Case, starts its Analysis and ends when it completes", async () => {
    vi.mocked(getAnalysis).mockResolvedValue(analysisWith("COMPLETED"));

    const flow = runAnalysis(identifiers, null, new AbortController().signal);
    await vi.advanceTimersByTimeAsync(POLL_INTERVAL_MS);

    await expect(flow).resolves.toEqual({
      result: "ended",
      analysis: { caseId: "case-1", analysisId: "analysis-1" },
    });
    expect(createCase).toHaveBeenCalledWith(identifiers);
    expect(startAnalysis).toHaveBeenCalledWith("case-1");
  });

  it("polls every 1.5 s while RUNNING and ends on PARTIAL or FAILED", async () => {
    for (const terminal of ["PARTIAL", "FAILED"] as const) {
      vi.mocked(getAnalysis)
        .mockReset()
        .mockResolvedValueOnce(analysisWith("RUNNING"))
        .mockResolvedValueOnce(analysisWith("RUNNING"))
        .mockResolvedValueOnce(analysisWith(terminal));

      let settled = false;
      const flow = runAnalysis(
        identifiers,
        null,
        new AbortController().signal
      ).finally(() => {
        settled = true;
      });

      await settle();
      expect(getAnalysis).not.toHaveBeenCalled();
      await vi.advanceTimersByTimeAsync(POLL_INTERVAL_MS);
      expect(getAnalysis).toHaveBeenCalledTimes(1);
      await vi.advanceTimersByTimeAsync(POLL_INTERVAL_MS);
      expect(getAnalysis).toHaveBeenCalledTimes(2);
      expect(settled).toBe(false);
      await vi.advanceTimersByTimeAsync(POLL_INTERVAL_MS);

      await expect(flow).resolves.toMatchObject({ result: "ended" });
      expect(getAnalysis).toHaveBeenCalledTimes(3);
      expect(getAnalysis).toHaveBeenCalledWith("analysis-1", {
        signal: expect.any(AbortSignal) as AbortSignal,
      });
    }
  });

  it("fails with the 400 field errors and no Case when the Case is rejected", async () => {
    const rejection = fieldError("email", "Podaj poprawny adres e-mail.");
    vi.mocked(createCase).mockRejectedValue(rejection);

    const error: unknown = await runAnalysis(
      identifiers,
      null,
      new AbortController().signal
    ).catch((reason: unknown) => reason);

    expect(error).toBeInstanceOf(StartAnalysisError);
    expect(error).toMatchObject({ cause: rejection, createdCase: null });
    expect(startAnalysis).not.toHaveBeenCalled();
  });

  it("keeps the created Case when starting fails and reuses it on retry", async () => {
    vi.mocked(startAnalysis).mockRejectedValueOnce(new TypeError("offline"));
    vi.mocked(getAnalysis).mockResolvedValue(analysisWith("COMPLETED"));

    const first: unknown = await runAnalysis(
      identifiers,
      null,
      new AbortController().signal
    ).catch((reason: unknown) => reason);
    expect(first).toBeInstanceOf(StartAnalysisError);
    const createdCase =
      first instanceof StartAnalysisError ? first.createdCase : null;
    expect(createdCase).toEqual({ caseId: "case-1", identifiers });

    const retry = runAnalysis(
      identifiers,
      createdCase,
      new AbortController().signal
    );
    await vi.advanceTimersByTimeAsync(POLL_INTERVAL_MS);

    await expect(retry).resolves.toMatchObject({ result: "ended" });
    expect(createCase).toHaveBeenCalledTimes(1);
    expect(startAnalysis).toHaveBeenCalledTimes(2);
    expect(startAnalysis).toHaveBeenLastCalledWith("case-1");
  });

  it("creates a new Case when the identifiers changed since the failed start", async () => {
    vi.mocked(getAnalysis).mockResolvedValue(analysisWith("COMPLETED"));
    const earlier: CreatedCase = {
      caseId: "case-0",
      identifiers: { ...identifiers, nickname: "other" },
    };

    const flow = runAnalysis(
      identifiers,
      earlier,
      new AbortController().signal
    );
    await vi.advanceTimersByTimeAsync(POLL_INTERVAL_MS);

    await expect(flow).resolves.toMatchObject({ result: "ended" });
    expect(createCase).toHaveBeenCalledTimes(1);
    expect(startAnalysis).toHaveBeenCalledWith("case-1");
  });

  it("resolves with pollingFailed and the Analysis when a poll fails", async () => {
    const pollError = new TypeError("offline");
    vi.mocked(getAnalysis)
      .mockResolvedValueOnce(analysisWith("RUNNING"))
      .mockRejectedValueOnce(pollError);

    const flow = runAnalysis(identifiers, null, new AbortController().signal);
    await vi.advanceTimersByTimeAsync(2 * POLL_INTERVAL_MS);

    await expect(flow).resolves.toEqual({
      result: "pollingFailed",
      analysis: { caseId: "case-1", analysisId: "analysis-1" },
      error: pollError,
    });
  });

  it("stops polling when aborted", async () => {
    vi.mocked(getAnalysis).mockResolvedValue(analysisWith("RUNNING"));
    const controller = new AbortController();

    const flow = runAnalysis(identifiers, null, controller.signal);
    const outcome = flow.catch((reason: unknown) => reason);
    await vi.advanceTimersByTimeAsync(POLL_INTERVAL_MS);
    controller.abort();

    expect(await outcome).toBeInstanceOf(DOMException);
    await vi.advanceTimersByTimeAsync(10 * POLL_INTERVAL_MS);
    expect(getAnalysis).toHaveBeenCalledTimes(1);
  });
});
