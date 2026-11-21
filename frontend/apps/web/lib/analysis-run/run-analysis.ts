import {
  createCase,
  startAnalysis,
} from "@/lib/api/generated/case-controller/case-controller";
import type { CreateCaseRequest } from "@/lib/api/generated/model";
import { narrowingIdentifierKinds } from "@/lib/identifiers/identifier-kinds";

export interface NewCaseRunInput {
  kind: "newCase";
  identifiers: CreateCaseRequest;
  caseId?: string;
}

export function toNewCaseRunInput(
  identifiers: CreateCaseRequest,
  previous?: NewCaseRunInput
): NewCaseRunInput {
  if (
    previous?.caseId !== undefined &&
    [...narrowingIdentifierKinds, "fullName" as const].every(
      (kind) =>
        (identifiers[kind] ?? null) === (previous.identifiers[kind] ?? null)
    )
  ) {
    return { kind: "newCase", identifiers, caseId: previous.caseId };
  }
  return { kind: "newCase", identifiers };
}

export interface StartedAnalysis {
  caseId: string;
  analysisId: string;
}

// The contract marks every response field optional, but the backend always
// returns the id of what it created. A missing id is a broken response, not a
// state to handle; the throw ends in the failed-start dialog.
function requireId(id: string | undefined, what: string): string {
  if (id === undefined) {
    throw new Error(`The backend returned the ${what} without an id`);
  }
  return id;
}

/** Starts an Analysis of an existing Case. A 409 means one is already running. */
export async function runCaseAnalysis(
  caseId: string
): Promise<StartedAnalysis> {
  const response = await startAnalysis(caseId);
  return {
    caseId,
    analysisId: requireId(response.data.id, "Analysis"),
  };
}

/**
 * Creates the Case, then starts its Analysis. `onCaseCreated` receives the id
 * as soon as the Case exists, so a retry after a failed start can start the
 * Analysis again without creating a second Case (Q30).
 */
export async function runNewCaseAnalysis(
  identifiers: CreateCaseRequest,
  onCaseCreated?: (caseId: string) => void
): Promise<StartedAnalysis> {
  const response = await createCase(identifiers);
  const caseId = requireId(response.data.id, "Case");
  onCaseCreated?.(caseId);
  return runCaseAnalysis(caseId);
}
