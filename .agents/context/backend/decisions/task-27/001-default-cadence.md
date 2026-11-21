# D27-001: Default cadence: once a day at 06:00

- **Status:** accepted (by the task owner; not confirmed by the team)
- **Date:** 2026-11-21
- **Type:** decision chosen by the user among offered options (a daily cadence was explicitly not approved before)
- **Source:** issue #27 ("Open decisions: frequency, time"); `epics/E6-returning-to-a-case.md` E6.4 ("a daily cadence has not been approved"), E6-D3; BUSINESS-SPEC question 12
- **Proposed by:** AI agent (Claude) offered the options; the user chose
- **Approved by:** Answer of the user (assignee of #27) to the scheduler questions in the working session, 2026-11-21. E6-D3 in `epics/E6-returning-to-a-case.md` is still marked open and no team confirmation is recorded

## Problem and context
The brief requires periodic execution (E6-D1 confirmed) but not how often or when.

## Options considered
Once a day 06:00; every 6 hours; once a week (Monday 06:00); every hour.

## Decision
Once a day at 06:00 is the default cadence when a schedule is enabled without choosing another one (see D27-004 for the other presets).

## Consequences and risks
- Daily is a coarse cadence for a lead-generation tool; the team may want a different default.
- Any change of default only affects schedules enabled afterwards; stored schedules keep their preset.

## Verification
`ScheduleCalculatorSpec` (next 06:00 incl. DST changes), `ScheduleApiSpec` ("enabling uses the daily 06:00 Warsaw default").

## Related
- Files: `service/ScheduleCalculator.kt`, `service/ScheduleService.kt`. Issues: #27. Decisions: D27-002, D27-004.

## Reconsider when
The team confirms E6-D3 with another cadence or mentors comment on the frequency.