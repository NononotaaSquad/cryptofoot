# D27-006: Terms missed during downtime are not made up

- **Status:** accepted (policy by the task owner, not confirmed by the team); the grace window is a proposed detail
- **Date:** 2026-11-21
- **Type:** decision chosen by the user; it equals the proposal in E6.4; the mechanism is a temporary own assumption
- **Source:** issue #27 criterion 4; E6.4 ("Missed intervals during downtime are not backfilled automatically"); E6-D3
- **Proposed by:** AI agent (Claude) offered the options; E6.4 proposed the policy
- **Approved by:** Answer of the user (assignee of #27) to the scheduler questions in the working session, 2026-11-21. E6-D3 in `epics/E6-returning-to-a-case.md` is still marked open and no team confirmation is recorded (policy). Not approved: the 2 minute grace window and the recorded `SKIPPED_DOWNTIME` outcome

## Problem and context
Scheduling is active only while the application runs; terms can pass while it is down, and a naive check ("next execution is in the past") would start a late run.

## Options considered
No backfill; one run after start; one run per missed term.

## Decision
A term is run only if it is at most the grace window (`app.scheduler.grace`, default 2 minutes) late. Otherwise nothing is run, the next execution is computed from now (a term in the future), and the attempt is recorded as `SKIPPED_DOWNTIME`. At most one decision is made per tick; multiple missed terms collapse into one record.

## Consequences and risks
- After a long outage the Analyst sees one `SKIPPED_DOWNTIME` entry, not one per missed term.
- A slow tick (longer than the grace window) is treated as downtime.
- No availability guarantee is implied (E6.4).

## Verification
`ScheduledRunnerSpec` ("terms missed during downtime are not run or made up").

## Related
- Files: `service/ScheduledRunner.kt`, `config/SchedulerConfig.kt`. Issues: #27. Decisions: D27-007.

## Reconsider when
The team decides on catch-up, or the tick interval approaches the grace window.