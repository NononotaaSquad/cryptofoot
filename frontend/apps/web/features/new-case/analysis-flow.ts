import { getAnalysis } from "@/lib/api/generated/analysis-controller/analysis-controller";
import {
  createCase,
  startAnalysis,
} from "@/lib/api/generated/case-controller/case-controller";
import { AnalysisResponseStatus } from "@/lib/api/generated/model";

import { narrowingIdentifierKinds } from "./identifier-fields";
import type { NewCaseIdentifiers } from "./new-case-rules";

export const POLL_INTERVAL_MS = 1500;

export interface StartedAnalysis {
  caseId: string;
  analysisId: string;
}

/** A Case the backend has already created, kept so a retry does not create a second one. */
export interface CreatedCase {
  caseId: string;
  identifiers: NewCaseIdentifiers;
}

/**
 * Failure before an Analysis exists (create the Case or start the Analysis).
 * `cause` is what the client threw (`ApiError` or a network error);
 * `createdCase` is set when the Case was created and only starting failed.
 */
export class StartAnalysisError extends Error {
  override readonly name = "StartAnalysisError";

  constructor(
    override readonly cause: unknown,
    readonly createdCase: CreatedCase | null
  ) {
    super("Starting the Analysis failed", { cause });
  }
}

export type AnalysisFlowResult =
  | { result: "ended"; analysis: StartedAnalysis }
  | { result: "pollingFailed"; analysis: StartedAnalysis; error: unknown };

function requireId(id: string | undefined, what: string): string {
  if (id === undefined)
    throw new Error(`The backend returned ${what} without an id`);
  return id;
}

function sameIdentifiers(a: NewCaseIdentifiers, b: NewCaseIdentifiers) {
  return [...narrowingIdentifierKinds, "fullName" as const].every(
    (kind) => a[kind] === b[kind]
  );
}

function wait(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(resolve, ms);
    signal.addEventListener(
      "abort",
      () => {
        clearTimeout(timer);
        reject(new DOMException("Polling was aborted", "AbortError"));
      },
      { once: true }
    );
  });
}

/**
 * The New Case flow: create the Case (or reuse `reusableCase` when it holds
 * the same identifiers), start its Analysis, then poll it every 1.5 s while
 * it is RUNNING, with no time limit.
 *
 * - Steps 1-2 fail with `StartAnalysisError`.
 * - A failed poll resolves with `pollingFailed`: the Analysis exists, so the
 *   caller still goes to the Case.
 * - Aborting `signal` (the form unmounts) stops polling and rejects with the
 *   abort reason.
 */
export async function runAnalysis(
  identifiers: NewCaseIdentifiers,
  reusableCase: CreatedCase | null,
  signal: AbortSignal
): Promise<AnalysisFlowResult> {
  let createdCase =
    reusableCase !== null &&
    sameIdentifiers(reusableCase.identifiers, identifiers)
      ? reusableCase
      : null;

  let analysis: StartedAnalysis;
  let status: string | undefined;
  try {
    if (createdCase === null) {
      const created = await createCase(identifiers);
      createdCase = {
        caseId: requireId(created.data.id, "a Case"),
        identifiers,
      };
    }
    const started = await startAnalysis(createdCase.caseId);
    analysis = {
      caseId: createdCase.caseId,
      analysisId: requireId(started.data.id, "an Analysis"),
    };
    status = started.data.status;
  } catch (error) {
    throw new StartAnalysisError(error, createdCase);
  }

  try {
    while (status === AnalysisResponseStatus.RUNNING) {
      await wait(POLL_INTERVAL_MS, signal);
      const polled = await getAnalysis(analysis.analysisId, { signal });
      status = polled.data.status;
    }
  } catch (error) {
    if (signal.aborted) throw error;
    return { result: "pollingFailed", analysis, error };
  }
  return { result: "ended", analysis };
}
