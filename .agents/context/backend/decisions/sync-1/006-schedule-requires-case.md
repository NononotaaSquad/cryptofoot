# DS1-006: A schedule can only be set for an existing Case

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** own proposal; connects #27 with #5
- **Source:** D27-007; E6.4 ("schedule failures are visible; the UI does not report a successful execution when none occurred")
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet (owner of #27)

## Problem and context
#27 was built before Cases existed, so `PUT /cases/{caseId}/schedule` accepted any id. With the flow wired, a schedule of an unknown Case would produce a `FAILED` attempt at every term.

## Decision (proposed)
`ScheduleService.update` answers 404 "Nie znaleziono sprawy o podanym identyfikatorze." when the Case does not exist and stores nothing. `GET .../schedule` of an unknown Case still answers "disabled" (harmless read, no stored state). A Case deleted later (no delete exists) would still be recorded as a `FAILED` attempt.

## Verification
`ScheduleApiSpec` ("a schedule can only be set for an existing Case"); the other schedule specs now create a real Case or stub the check.

## Related
- Files: `service/ScheduleService.kt`. Decisions: D27-007, DS1-001. Diagram: `12-periodic-execution.puml`.

## Reconsider when
Cases can be deleted (the schedule must go with them), or `GET` should also answer 404.
