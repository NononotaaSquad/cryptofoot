# D27-003: Configurability: predefined options per Case

- **Status:** accepted (by the task owner; not confirmed by the team)
- **Date:** 2026-11-21
- **Type:** decision chosen by the user among offered options
- **Source:** issue #27; E6.4; E6-D3 ("fixed or Analyst-configurable cadence"); E6 "Dependencies and boundaries" (no arbitrary recurrence rules)
- **Proposed by:** AI agent (Claude) offered the options; the user chose
- **Approved by:** Answer of the user (assignee of #27) to the scheduler questions in the working session, 2026-11-21. E6-D3 in `epics/E6-returning-to-a-case.md` is still marked open and no team confirmation is recorded

## Problem and context
The cadence can be fixed for the whole application or chosen by the Analyst.

## Options considered
A fixed value from configuration; predefined options chosen per Case; arbitrary cron rules.

## Decision
The Analyst chooses, per Case, one of a short list of predefined cadences (`UpdateScheduleRequest.cadence`). No arbitrary rules. Enabling without a choice uses the stored cadence or the default (D27-001). Which presets exist is D27-004.

## Consequences and risks
- The contract grows by one request field and an enum (additive, `api-sync` run).
- Changing the cadence of an enabled schedule recomputes its next execution; repeating the same cadence keeps it.

## Verification
`ScheduleApiSpec` (changing and repeating the cadence, unknown cadence is 400).

## Related
- Files: `dto/Requests.kt`, `model/Enums.kt` (`SchedulePreset`). Issues: #27. Decisions: D27-001, D27-004.

## Reconsider when
Mentors or the team want free-form schedules or a single fixed cadence.