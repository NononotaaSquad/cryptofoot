# D27-002: Time zone: Europe/Warsaw, not configurable

- **Status:** accepted (by the task owner; not confirmed by the team)
- **Date:** 2026-11-21
- **Type:** decision chosen by the user among offered options
- **Source:** issue #27; E6.4 ("explicit timezone"); E6-D3
- **Proposed by:** AI agent (Claude) offered the options; the user chose
- **Approved by:** Answer of the user (assignee of #27) to the scheduler questions in the working session, 2026-11-21. E6-D3 in `epics/E6-returning-to-a-case.md` is still marked open and no team confirmation is recorded

## Problem and context
A wall-clock time ("06:00") needs an explicit zone; daylight saving shifts it relative to UTC.

## Options considered
Europe/Warsaw; UTC; configurable per Case.

## Decision
All schedule times are interpreted in `Europe/Warsaw` (constant in `ScheduleCalculator`), shown in every `ScheduleResponse.timezone`. Instants are stored and returned in UTC.

## Consequences and risks
- Local 06:00 stays 06:00 across daylight-saving changes (05:00Z in winter, 04:00Z in summer); intervals of the 6-hour preset are 5 or 7 hours on change days.
- A deployment outside Poland keeps this zone; changing it needs a new decision.

## Verification
`ScheduleCalculatorSpec` (winter, summer, spring and autumn changes).

## Related
- Files: `service/ScheduleCalculator.kt`. Issues: #27. Decisions: D27-001.

## Reconsider when
Users outside the Warsaw zone appear, or per-Case zones are requested.