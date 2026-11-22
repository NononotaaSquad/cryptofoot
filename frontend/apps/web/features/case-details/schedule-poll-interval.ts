import type { ScheduleResponse } from "@/lib/api/generated/model";

export const SCHEDULE_POLL_INTERVAL_MS = 30_000;
export const DEMO_SCHEDULE_POLL_INTERVAL_MS = 5_000;

/**
 * Seam 2: how long to wait before asking the server again while a Schedule is
 * enabled, or `false` to stop. A disabled Schedule and an unread one do not
 * poll (the first read is the normal fetch of the query). The backend answers
 * `demo: true` for a saved demo Frequency even when disabled, so `enabled` is
 * checked first. Shaped for TanStack Query `refetchInterval`.
 */
export function schedulePollInterval(
  schedule: ScheduleResponse | undefined
): number | false {
  if (schedule?.enabled !== true) return false;
  return schedule.demo === true
    ? DEMO_SCHEDULE_POLL_INTERVAL_MS
    : SCHEDULE_POLL_INTERVAL_MS;
}
