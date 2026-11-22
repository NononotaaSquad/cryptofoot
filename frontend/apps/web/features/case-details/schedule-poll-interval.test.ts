import { describe, expect, it } from "vitest";

import type { ScheduleResponse } from "@/lib/api/generated/model";

import {
  DEMO_SCHEDULE_POLL_INTERVAL_MS,
  SCHEDULE_POLL_INTERVAL_MS,
  schedulePollInterval,
} from "./schedule-poll-interval";

function schedule(overrides: ScheduleResponse): ScheduleResponse {
  return { enabled: false, demo: false, cadence: "DAILY", ...overrides };
}

describe("schedulePollInterval", () => {
  it("does not poll before the Schedule is read", () => {
    expect(schedulePollInterval(undefined)).toBe(false);
  });

  it("does not poll a disabled Schedule", () => {
    expect(schedulePollInterval(schedule({ enabled: false }))).toBe(false);
  });

  it("does not poll a disabled Schedule that was saved on the demo Frequency", () => {
    expect(
      schedulePollInterval(
        schedule({ enabled: false, demo: true, cadence: "DEMO_EVERY_MINUTE" })
      )
    ).toBe(false);
  });

  it("polls every 30 seconds while enabled", () => {
    expect(schedulePollInterval(schedule({ enabled: true }))).toBe(
      SCHEDULE_POLL_INTERVAL_MS
    );
    expect(SCHEDULE_POLL_INTERVAL_MS).toBe(30_000);
  });

  it("polls every 5 seconds while enabled on the demo Frequency", () => {
    expect(
      schedulePollInterval(
        schedule({ enabled: true, demo: true, cadence: "DEMO_EVERY_MINUTE" })
      )
    ).toBe(DEMO_SCHEDULE_POLL_INTERVAL_MS);
    expect(DEMO_SCHEDULE_POLL_INTERVAL_MS).toBe(5_000);
  });

  it("treats a missing enabled flag as disabled", () => {
    expect(schedulePollInterval({})).toBe(false);
  });
});
