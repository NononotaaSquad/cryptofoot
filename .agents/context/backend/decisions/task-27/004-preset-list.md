# D27-004: The list of predefined cadences

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** own proposal (the user chose "predefined options per Case" but did not list them)
- **Source:** D27-003; the cadence options offered in the session
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
Per-Case presets need a concrete list.

## Options considered
Only daily; the options offered in the session (daily, every 6 hours, weekly); that list plus hourly.

## Decision (proposed)
`SchedulePreset`: `DAILY` (06:00), `EVERY_6_HOURS` (00:00, 06:00, 12:00, 18:00), `WEEKLY` (Monday 06:00), all in Europe/Warsaw, and `DEMO_EVERY_MINUTE` (D27-008). Hourly was left out to limit load on sources.

## Consequences and risks
- Adding or removing a preset changes the generated enum; stored schedules with a removed preset would no longer load.
- `scheduleResponse.cadence` changed from free text to this enum in the contract.

## Verification
`ScheduleCalculatorSpec` (every preset).

## Related
- Files: `model/Enums.kt`, `service/ScheduleCalculator.kt`, `frontend/apps/web/lib/api/`. Issues: #27, E6.4 frontend issue. Decisions: D27-001, D27-003, D27-008.

## Reconsider when
The team names the cadences it wants, or the frontend owner needs others.