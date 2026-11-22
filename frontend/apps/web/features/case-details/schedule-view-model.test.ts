import { describe, expect, it } from "vitest";

import type {
  ScheduleAttemptResponse,
  ScheduleResponse,
} from "@/lib/api/generated/model";

import {
  checkCustomInterval,
  isScheduleOptionValue,
  toScheduleView,
  toUpdateScheduleRequest,
} from "./schedule-view-model";

const NEXT = "2026-11-22T05:00:00Z"; // 06:00 in Warsaw (CET)
const NEXT_TEXT = "22.11.2026, 06:00";
const NEXT_ZONED = `${NEXT_TEXT} (Europe/Warsaw)`;

function schedule(overrides: ScheduleResponse = {}): ScheduleResponse {
  return {
    enabled: true,
    cadence: "DAILY",
    demo: false,
    timezone: "Europe/Warsaw",
    nextExecutionAt: NEXT,
    lastAttempt: null,
    ...overrides,
  };
}

function attempt(
  overrides: ScheduleAttemptResponse = {}
): ScheduleAttemptResponse {
  return {
    attemptedAt: "2026-11-21T05:00:00Z",
    outcome: "STARTED",
    analysisId: "a-1",
    ...overrides,
  };
}

const off = { demoEnabled: false };
const demo = { demoEnabled: true };

function selected(view: ReturnType<typeof toScheduleView>): string[] {
  return view.options.filter((o) => o.selected).map((o) => o.value);
}

describe("toScheduleView: state row", () => {
  it("has no row for a disabled Schedule, also after a failed attempt (P4)", () => {
    const plain = toScheduleView(schedule({ enabled: false }), off);
    const failed = toScheduleView(
      schedule({ enabled: false, lastAttempt: attempt({ outcome: "FAILED" }) }),
      off
    );
    expect(plain.stateRow.kind).toBe("none");
    expect(failed.stateRow.kind).toBe("none");
  });

  it("has no row while the Schedule is not read", () => {
    expect(toScheduleView(undefined, off).stateRow.kind).toBe("none");
  });

  it.each([
    ["DAILY", "codziennie o 06:00"],
    ["EVERY_6_HOURS", "co 6 godzin"],
    ["WEEKLY", "co tydzień w poniedziałek o 06:00"],
    ["DEMO_EVERY_MINUTE", "co minutę"],
  ] as const)("shows the Frequency %s and the next term", (cadence, text) => {
    const row = toScheduleView(schedule({ cadence }), off).stateRow;
    expect(row.kind).toBe("info");
    expect(row.text).toBe(
      `Harmonogram: ${text} · następna analiza: ${NEXT_ZONED}`
    );
  });

  it("labels the demo Schedule by the `demo` flag of the answer, not by the switch", () => {
    expect(toScheduleView(schedule({ demo: true }), off).stateRow.demo).toBe(
      true
    );
    expect(toScheduleView(schedule({ demo: false }), demo).stateRow.demo).toBe(
      false
    );
  });

  it("keeps a started attempt plain", () => {
    const row = toScheduleView(
      schedule({ lastAttempt: attempt({ outcome: "STARTED" }) }),
      off
    ).stateRow;
    expect(row.kind).toBe("info");
  });

  it.each([
    [
      "FAILED",
      "Nie udało się uruchomić zaplanowanej analizy 21.11.2026, 06:00.",
    ],
    [
      "SKIPPED_OVERLAP",
      "Pominięto zaplanowaną analizę 21.11.2026, 06:00, ponieważ inna analiza tej sprawy była w toku.",
    ],
    [
      "SKIPPED_DOWNTIME",
      "Pominięto zaplanowaną analizę 21.11.2026, 06:00, ponieważ aplikacja wtedy nie działała.",
    ],
  ] as const)(
    "turns %s into a warning that keeps the next term",
    (outcome, sentence) => {
      const row = toScheduleView(
        schedule({ lastAttempt: attempt({ outcome, analysisId: null }) }),
        off
      ).stateRow;
      expect(row.kind).toBe("warning");
      expect(row.text).toBe(`${sentence} Następna analiza: ${NEXT_ZONED}.`);
    }
  );

  it("treats an absent last attempt like none", () => {
    const row = toScheduleView(
      schedule({ lastAttempt: undefined }),
      off
    ).stateRow;
    expect(row.kind).toBe("info");
  });

  it("drops the next term part without an invalid text when there is none", () => {
    const plain = toScheduleView(schedule({ nextExecutionAt: null }), off);
    const warning = toScheduleView(
      schedule({
        nextExecutionAt: null,
        lastAttempt: attempt({ outcome: "FAILED" }),
      }),
      off
    );
    expect(plain.stateRow.text).toBe("Harmonogram: codziennie o 06:00");
    expect(warning.stateRow.text).toBe(
      "Nie udało się uruchomić zaplanowanej analizy 21.11.2026, 06:00."
    );
    expect(plain.nextLine).toBeNull();
    expect(plain.stateRow.text).not.toContain("Invalid");
  });

  it("shows Polish time near midnight UTC, whatever the zone of the machine", () => {
    const row = toScheduleView(
      schedule({ nextExecutionAt: "2026-11-21T23:30:00Z" }),
      off
    ).stateRow;
    expect(row.text).toContain("22.11.2026, 00:30");
  });

  it("uses the zone of the answer and falls back to Warsaw for an unknown one", () => {
    const utc = toScheduleView(schedule({ timezone: "UTC" }), off).stateRow;
    const unknown = toScheduleView(
      schedule({ timezone: "Nowhere/Land" }),
      off
    ).stateRow;
    const missing = toScheduleView(
      schedule({ timezone: undefined }),
      off
    ).stateRow;
    expect(utc.text).toContain("22.11.2026, 05:00");
    expect(unknown.text).toContain(NEXT_TEXT);
    expect(missing.text).toContain(NEXT_TEXT);
  });

  it("treats an enabled Schedule without a Frequency as none", () => {
    expect(toScheduleView(schedule({ cadence: null }), off).stateRow.kind).toBe(
      "none"
    );
  });
});

describe("toScheduleView: options", () => {
  it("selects 'Wyłączony' for a disabled Schedule, whatever cadence is stored", () => {
    const view = toScheduleView(
      schedule({ enabled: false, cadence: "WEEKLY" }),
      off
    );
    expect(selected(view)).toEqual(["OFF"]);
  });

  it("selects 'Wyłączony' for a Schedule never saved", () => {
    const view = toScheduleView(
      { enabled: false, cadence: null, nextExecutionAt: null },
      off
    );
    expect(selected(view)).toEqual(["OFF"]);
    expect(toScheduleView(undefined, off).options[0]?.selected).toBe(true);
  });

  it.each(["DAILY", "EVERY_6_HOURS", "WEEKLY"] as const)(
    "selects exactly %s",
    (cadence) => {
      expect(selected(toScheduleView(schedule({ cadence }), off))).toEqual([
        cadence,
      ]);
    }
  );

  it("lists the regular options in order with the labels of the spec", () => {
    const view = toScheduleView(schedule({ enabled: false }), off);
    expect(view.options.map((o) => o.label)).toEqual([
      "Wyłączony",
      "Codziennie o 06:00",
      "Co 6 godzin (00:00, 06:00, 12:00, 18:00)",
      "Co tydzień (poniedziałek, 06:00)",
      "Własny interwał",
    ]);
  });

  it("lists the demo option only with the switch on", () => {
    const withSwitch = toScheduleView(schedule({ enabled: false }), demo);
    expect(withSwitch.options.map((o) => o.value)).toEqual([
      "OFF",
      "DAILY",
      "EVERY_6_HOURS",
      "WEEKLY",
      "CUSTOM_INTERVAL",
      "DEMO_EVERY_MINUTE",
    ]);
    expect(withSwitch.options.at(-1)).toMatchObject({
      label: "Co minutę (tryb demonstracyjny)",
      selected: false,
    });
    expect(
      toScheduleView(schedule({ enabled: false }), off).options.map(
        (o) => o.value
      )
    ).not.toContain("DEMO_EVERY_MINUTE");
  });

  it("lists and selects the demo option when the server runs it, switch off or on, once", () => {
    for (const settings of [off, demo]) {
      const view = toScheduleView(
        schedule({ cadence: "DEMO_EVERY_MINUTE", demo: true }),
        settings
      );
      expect(
        view.options.filter((o) => o.value === "DEMO_EVERY_MINUTE")
      ).toHaveLength(1);
      expect(selected(view)).toEqual(["DEMO_EVERY_MINUTE"]);
    }
  });

  it("does not list the demo option for a disabled Schedule once saved as demo, with the switch off", () => {
    const view = toScheduleView(
      schedule({ enabled: false, cadence: "DEMO_EVERY_MINUTE", demo: true }),
      off
    );
    expect(view.options.map((o) => o.value)).not.toContain("DEMO_EVERY_MINUTE");
  });

  it("looks enabled only for an enabled Schedule", () => {
    expect(toScheduleView(schedule(), off).enabled).toBe(true);
    expect(toScheduleView(schedule({ enabled: false }), off).enabled).toBe(
      false
    );
  });
});

describe("toScheduleView: popover lines", () => {
  it("shows the next term and the last started run with its Analysis", () => {
    const view = toScheduleView(
      schedule({ lastAttempt: attempt({ analysisId: "a-7" }) }),
      off
    );
    expect(view.nextLine).toBe(`Następna analiza: ${NEXT_ZONED}`);
    expect(view.lastAttemptLine).toEqual({
      text: "Ostatnie uruchomienie: 21.11.2026, 06:00",
      analysisId: "a-7",
    });
  });

  it.each(["SKIPPED_OVERLAP", "SKIPPED_DOWNTIME", "FAILED"] as const)(
    "shows the message of %s without an Analysis to open",
    (outcome) => {
      const view = toScheduleView(
        schedule({ lastAttempt: attempt({ outcome, analysisId: null }) }),
        off
      );
      expect(view.lastAttemptLine?.analysisId).toBeNull();
      expect(view.lastAttemptLine?.text).toMatch(/^(Nie udało|Pominięto)/);
      expect(view.lastAttemptLine?.text).not.toContain("Następna analiza");
    }
  );

  it("has no last attempt line without an attempt", () => {
    expect(toScheduleView(schedule(), off).lastAttemptLine).toBeNull();
    expect(
      toScheduleView(schedule({ lastAttempt: undefined }), off).lastAttemptLine
    ).toBeNull();
  });

  it("keeps the last attempt visible when the Schedule is disabled", () => {
    const view = toScheduleView(
      schedule({
        enabled: false,
        nextExecutionAt: null,
        lastAttempt: attempt(),
      }),
      off
    );
    expect(view.lastAttemptLine?.analysisId).toBe("a-1");
    expect(view.nextLine).toBeNull();
  });
});

describe("toUpdateScheduleRequest", () => {
  it("disables the Schedule for 'Wyłączony'", () => {
    expect(toUpdateScheduleRequest("OFF")).toEqual({ enabled: false });
  });

  it.each(["DAILY", "EVERY_6_HOURS", "WEEKLY", "DEMO_EVERY_MINUTE"] as const)(
    "enables the Schedule on %s",
    (cadence) => {
      expect(toUpdateScheduleRequest(cadence)).toEqual({
        enabled: true,
        cadence,
      });
    }
  );
});

describe("isScheduleOptionValue", () => {
  it("accepts the option values and nothing else", () => {
    expect(isScheduleOptionValue("OFF")).toBe(true);
    expect(isScheduleOptionValue("WEEKLY")).toBe(true);
    expect(isScheduleOptionValue("MONTHLY")).toBe(false);
    expect(isScheduleOptionValue(null)).toBe(false);
  });
});

describe("custom interval", () => {
  const customSchedule = (
    intervalValue: number,
    intervalUnit: "MINUTES" | "HOURS" | "DAYS"
  ) => schedule({ cadence: "CUSTOM_INTERVAL", intervalValue, intervalUnit });

  it.each([
    [1, "MINUTES", "co 1 minutę"],
    [2, "MINUTES", "co 2 minuty"],
    [5, "MINUTES", "co 5 minut"],
    [12, "MINUTES", "co 12 minut"],
    [22, "MINUTES", "co 22 minuty"],
    [1, "HOURS", "co 1 godzinę"],
    [3, "HOURS", "co 3 godziny"],
    [1, "DAYS", "co 1 dzień"],
    [2, "DAYS", "co 2 dni"],
    [30, "DAYS", "co 30 dni"],
  ] as const)(
    "shows %i %s as '%s' with the next term and its zone",
    (value, unit, text) => {
      const view = toScheduleView(customSchedule(value, unit), off);
      expect(view.stateRow.text).toBe(
        `Harmonogram: ${text} · następna analiza: ${NEXT_ZONED}`
      );
      expect(selected(view)).toEqual(["CUSTOM_INTERVAL"]);
      expect(view.nextLine).toBe(`Następna analiza: ${NEXT_ZONED}`);
    }
  );

  it("does not label a custom interval as the demo mode", () => {
    expect(
      toScheduleView(customSchedule(1, "MINUTES"), off).stateRow.demo
    ).toBe(false);
  });

  it("offers the saved interval as the starting values, also while disabled", () => {
    const view = toScheduleView(
      { ...customSchedule(15, "HOURS"), enabled: false },
      off
    );
    expect(selected(view)).toEqual(["OFF"]);
    expect(view.customInterval).toEqual({ value: "15", unit: "HOURS" });
    expect(toScheduleView(undefined, off).customInterval).toEqual({
      value: "",
      unit: "MINUTES",
    });
  });

  it("does not show an invalid text for a custom schedule without a length", () => {
    const view = toScheduleView(schedule({ cadence: "CUSTOM_INTERVAL" }), off);
    expect(view.stateRow.text).toContain("Harmonogram: co własny interwał");
    expect(view.stateRow.text).not.toContain("undefined");
  });

  it("builds the save request with the checked length", () => {
    expect(
      toUpdateScheduleRequest("CUSTOM_INTERVAL", {
        intervalValue: 1,
        intervalUnit: "MINUTES",
      })
    ).toEqual({
      enabled: true,
      cadence: "CUSTOM_INTERVAL",
      intervalValue: 1,
      intervalUnit: "MINUTES",
    });
    expect(toUpdateScheduleRequest("DAILY")).toEqual({
      enabled: true,
      cadence: "DAILY",
    });
  });

  it.each([
    ["1", "MINUTES"],
    [" 90 ", "MINUTES"],
    ["525600", "MINUTES"],
    ["8760", "HOURS"],
    ["365", "DAYS"],
  ] as const)("accepts %j %s", (text, unit) => {
    expect(checkCustomInterval(text, unit)).toEqual({
      ok: true,
      intervalValue: Number(text.trim()),
      intervalUnit: unit,
    });
  });

  it.each([
    ["", "MINUTES"],
    ["   ", "MINUTES"],
    ["0", "MINUTES"],
    ["-5", "MINUTES"],
    ["1.5", "MINUTES"],
    ["1,5", "HOURS"],
    ["abc", "DAYS"],
    ["1e3", "MINUTES"],
    ["525601", "MINUTES"],
    ["8761", "HOURS"],
    ["366", "DAYS"],
    ["99999999999999999999", "DAYS"],
  ] as const)("rejects %j %s with a message", (text, unit) => {
    const result = checkCustomInterval(text, unit);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.message).toMatch(/\.$/);
  });
});
