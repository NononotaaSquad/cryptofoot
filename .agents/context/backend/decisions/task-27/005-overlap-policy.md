# D27-005: Overlapping terms are skipped and recorded

- **Status:** accepted (by the task owner; not confirmed by the team)
- **Date:** 2026-11-21
- **Type:** decision chosen by the user; it equals the proposal in E6.4
- **Source:** issue #27 criterion 4; E6.4 ("Proposed overlap policy ... awaits confirmation"); E6-D3
- **Proposed by:** AI agent (Claude) offered the options; E6.4 proposed the policy
- **Approved by:** Answer of the user (assignee of #27) to the scheduler questions in the working session, 2026-11-21. E6-D3 in `epics/E6-returning-to-a-case.md` is still marked open and no team confirmation is recorded

## Problem and context
A term may come while an Analysis of the same Case still runs.

## Options considered
Skip and record; queue; allow parallel.

## Decision
No second Analysis is started. The attempt is recorded as `SKIPPED_OVERLAP` in the schedule's last attempt and the next execution advances to the next term. The check is the same as for a manual rerun (`ConflictException` from the shared flow).

## Consequences and risks
- The shared check is check-then-insert, not atomic (D26-005); a manual and a scheduled start at the same instant can both run.
- Only the last attempt is kept, so a run of skipped terms is visible as the latest one only.

## Verification
`ScheduledRunnerSpec` ("an overlapping term is skipped, recorded and starts no second Analysis").

## Related
- Files: `service/ScheduledRunner.kt`. Issues: #26, #27. Decisions: D26-005, D27-007.

## Reconsider when
An atomic start guard exists, or the team wants queueing.