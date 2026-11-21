# D27-008: Clearly labelled demo interval

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** requirement from E6.4/E7.2 ("any shortened test interval is clearly labelled") + own proposal for the mechanism
- **Source:** issue #27 criterion 5; `epics/E7-demo-scenario.md` E7.2; E6.4
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
A film cannot wait for a daily run, but a shortened interval must not be mistaken for the production cadence.

## Options considered
A global flag that speeds up every preset; a separate preset enabled only in demo mode; a test-only clock.

## Decision (proposed)
Preset `SchedulePreset.DEMO_EVERY_MINUTE` (next full minute). It can be enabled only when `app.scheduler.demo-enabled=true` (default false); otherwise 400. Every response carries `demo: true` for it and the preset name itself contains `DEMO`, so the UI can label it. Tests use a controllable clock instead of waiting.

## Consequences and risks
- The demo preset exists in the production enum and OpenAPI; it is inert unless the flag is set.
- The grace window (2 minutes) is longer than the demo interval; demo runs do not skip as downtime unless the app stalls for minutes.

## Verification
`ScheduleDemoSpec` (accepted only with the flag, labelled), `ScheduleApiSpec` (refused by default), `ScheduleCalculatorSpec`.

## Related
- Files: `config/SchedulerConfig.kt`, `service/ScheduleService.kt`. Issues: #27, E7. Decisions: D27-004.

## Reconsider when
The film script needs a different demonstration of periodic execution.