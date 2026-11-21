import type { AnalysisResponseStatus } from "@/lib/api/generated/model";

export const POLL_INTERVAL_MS = 1500;

/**
 * Seam S3: how long to wait before asking for the Analysis again, or `false`
 * to stop. Polling goes on while the Analysis runs and while no data has
 * arrived yet (`undefined`); it stops once the Analysis has ended. Shaped for
 * TanStack Query `refetchInterval`; the loader and the running report share it.
 */
export function nextPollInterval(
  status: AnalysisResponseStatus | undefined
): number | false {
  switch (status) {
    case undefined:
    case "RUNNING":
      return POLL_INTERVAL_MS;
    case "COMPLETED":
    case "PARTIAL":
    case "FAILED":
      return false;
  }
}
