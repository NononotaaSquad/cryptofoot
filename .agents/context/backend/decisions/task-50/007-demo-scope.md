# D50-007: The wallet search is not part of the demo

- **Status:** accepted (by the task owner as a team-side choice; mentors not asked)
- **Date:** 2026-11-21
- **Type:** decision chosen by the user among offered options; distinction between the team decision and the mentors' acceptance
- **Source:** issue #50 decision 5; `epics/E7-demo-scenario.md` (E7-D1: controlled synthetic demo; mentor acceptance open); `BUSINESS-SPEC.md` questions 10 and 11
- **Proposed by:** AI agent (Claude) offered the options; the user chose "not in the demo"
- **Approved by:** the user (task owner), in the working session of 2026-11-21 (answers to the five questions of issue #50); not confirmed by the team, by the owners of E2 and E3, or by the mentors

## Problem and context
The extended direction (a wallet as input) could enter the demo scenario.

## Decision
The demo stays with the confirmed path: a fictional person entered by identifiers and addresses found for that person. The entered-wallet search is not shown in the demo and no fixture or expected result is prepared for it. This is a decision of the task owner; the mentors were not asked and their acceptance is neither given nor sought.

## Consequences and risks
- The feature exists in the product but is not demonstrated; the jury may still ask about it, because the brief lists an address as an identifier.
- If it is added to the demo, the ETH matching gap (D50-002) must be closed first.

## Verification
No demo change in this task.

## Related
- Issues: #28, #50. Decisions: D50-002.

## Reconsider when
The team decides to show it, or the mentors comment on the postponed direction.