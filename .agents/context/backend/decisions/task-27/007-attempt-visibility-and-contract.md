# D27-007: Visible attempts, failures and the schedule API

- **Status:** proposed (changes the merged REST contract)
- **Date:** 2026-11-21
- **Type:** own proposal; contract change
- **Source:** issue #27 criteria 1, 2, 3; E6.4 ("Schedule failures are visible; the UI does not report a successful execution when none occurred"); `backend/decisions/0001-epic-api-contract.md`
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet. The E6.4 frontend owner must be told: generated types changed.

## Problem and context
The contract has `GET/PUT /cases/{caseId}/schedule` as stubs, `ScheduleResponse(enabled, cadence: String?, timezone: String?, nextExecutionAt, lastAttempt)` and outcomes `STARTED`, `SKIPPED_OVERLAP`, `FAILED`.

## Options considered
A history of attempts per Case; only the last attempt (as in the contract).

## Decision (proposed)
Implement both endpoints. Collection `schedules` (one document per Case, id = caseId, optimistic `@Version`). The record stores `enabled`, `cadence`, `nextExecutionAt` and `lastAttempt` (time, outcome, Analysis id). Outcomes: `STARTED` (with the id of the saved Analysis, trigger `SCHEDULED`), `SKIPPED_OVERLAP`, new `SKIPPED_DOWNTIME`, `FAILED` (the start of the Analysis threw, for example an unknown Case). Contract changes: `ScheduleResponse.cadence` is `SchedulePreset?`, `timezone` is non-null, new `demo: Boolean`; `UpdateScheduleRequest.cadence` (optional); enum value `SKIPPED_DOWNTIME`. A Case without a schedule reads as disabled with the zone. `PUT` with the demo preset while demo mode is off is 400 (Polish message). A term claims the schedule first (saves the next execution), so a second runner cannot start the same term.

## Consequences and risks
- Only the last attempt is kept; older attempts survive only in logs and as saved Analyses.
- A `FAILED` attempt means the start failed; an Analysis that ran and ended `FAILED` is a normal `STARTED` attempt with a failed Analysis in history (D24-002).
- The Case is not verified to exist when its schedule is set (no Case store, E1).

## Verification
`ScheduleApiSpec`, `ScheduleDemoSpec`, `ScheduledRunnerSpec` (outcomes, failure visible through the API, other Cases not blocked), `OpenApiContractSpec`, `npm run check`.

## Related
- Files: `controller/CaseController.kt`, `service/ScheduleService.kt`, `service/ScheduledRunner.kt`, `model/ScheduleRecord.kt`, `dto/`. Issues: #25, #27. Decisions: D25-001, D27-003, D27-005, D27-006.

## Reconsider when
The frontend needs an attempt history, or E1 provides Cases.