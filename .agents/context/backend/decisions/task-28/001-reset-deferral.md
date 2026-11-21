# D28-001: The demo reset is deferred until the owners define the boundary

- **Status:** accepted (by the task owner)
- **Date:** 2026-11-21
- **Type:** decision chosen by the user; scope limit
- **Source:** issue #28 checklist item of Bohdan; `epics/E7-demo-scenario.md` E7.4 ("how to reset this demonstration Case without deleting unrelated Cases"); the task instructions (agree the boundary with the fixture owner before implementing the reset)
- **Proposed by:** AI agent (Claude) offered three options; the user chose
- **Approved by:** the user (task owner), in the working session of 2026-11-21

## Problem and context
The reset must delete only synthetic demo data. There is no Case store (E1 #5, open) and no fixtures (E2 T4 #37, open), so nothing defines what the demo Case is.

## Options considered
Implement the reset for one fixed Case id now; also delete synthetic Mentions; wait for the owners and do only the scheduler check, runbook and demo profile.

## Decision
Wait. Implement only the scheduler check, the labelled interval and the runbook. The reset is recorded as a dependency and as a separate subtask blocked by #5 and #37, to be done later by the task owner.

## Consequences and risks
- The checklist item of Bohdan in #28 is **not complete**: the periodic check is done, the reset is not.
- E7.4 asks for a reset instruction; the runbook only states the blocker and the proposed boundary.

## Verification
The runbook states the status of every part; no reset code exists in the repository.

## Related
- Files: `.agents/context/backend/demo-scheduler-check.md`. Issues: #5, #28, #37. Decisions: D28-002.

## Reconsider when
E1 and the fixtures exist and the boundary of D28-002 is confirmed.