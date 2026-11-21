import type { NewCaseIdentifiers } from "./new-case-rules";

// Provisional shape (open question V8 of views.md); it must not leave this
// feature folder. The generated client replaces it.
export interface StartedAnalysis {
  caseId: string;
  analysisId: string;
}

const MOCK_DELAY_MS = 2500;

// Opaque ids shaped like MongoDB ObjectIds: 24 hexadecimal characters.
function mockObjectId(): string {
  return Array.from(crypto.getRandomValues(new Uint8Array(12)), (byte) =>
    byte.toString(16).padStart(2, "0")
  ).join("");
}

function wait(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

/**
 * MOCK, replaced by the generated client (`npm run api:gen`) once the New case
 * screen is wired to the backend. No network call; it resolves after a fixed
 * delay and never fails.
 *
 * The real flow is already in the contract on master:
 * 1. `createCase`: POST /api/1/cases -> 201 `CaseResponse`, its `id` is the caseId.
 * 2. `startAnalysis`: POST /api/1/cases/{caseId}/analyses -> 202
 *    `AnalysisResponse` with status RUNNING; 409 when one is already running.
 * 3. Poll `getAnalysis`: GET /api/1/analyses/{analysisId} until the status is
 *    COMPLETED, PARTIAL or FAILED.
 *
 * Typed as a function of the identifiers so the call site already passes them;
 * the mock itself does not read them.
 */
export const startAnalysis: (
  identifiers: NewCaseIdentifiers
) => Promise<StartedAnalysis> = async () => {
  await wait(MOCK_DELAY_MS);
  return { caseId: mockObjectId(), analysisId: mockObjectId() };
};
