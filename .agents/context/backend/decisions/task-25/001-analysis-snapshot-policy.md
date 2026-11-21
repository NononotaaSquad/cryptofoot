# D25-001: Analysis snapshot policy (one immutable record per Analysis)

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** requirement from the brief (history is never overwritten) + own proposal (how)
- **Source:** issue #25 criteria 1, 3, 5; `epics/E6-returning-to-a-case.md` E6.1, E6.2; `epics/E5-report-for-the-official.md` E5.4; task brief section 3 ("saves results of successive analyses")
- **Proposed by:** AI agent (Claude), for the assignee of #25
- **Approved by:** nobody yet

## Problem and context
A later Analysis must not overwrite an earlier one, a saved result must not change when opened, and failed/partial Analyses must stay in history.

## Options considered
1. One mutable "latest result" document per Case (overwrites history).
2. One document per Analysis, inserted at start, updated once at the end, never changed afterwards.
3. Event log of every state change per Analysis.

## Decision (proposed)
Option 2. Collection `analyses`, one `AnalysisRecord` per Analysis with its own Mongo id and the `caseId`. `begin` inserts it as `RUNNING` (insert, so an existing id can never be overwritten). `finish` moves it once to `COMPLETED`/`PARTIAL`/`FAILED` and stores the whole result. A record that is not `RUNNING` is never written again (`finish` throws `IllegalStateException`); `@Version` guards concurrent writers. Reads never trigger or modify anything. History is `startedAt` descending (id as tiebreaker).

## Consequences and risks
- Reopening a result returns exactly what was stored (E5.4); successive Analyses are independent records.
- No update/delete API exists; deleting a Case or Analysis is not designed.
- A crash between `begin` and `finish` leaves a `RUNNING` record forever (see D25-004).

## Verification
`AnalysisHistorySpec`: independent ids, earlier result unchanged after a later Analysis (byte-equal JSON), history order, double `finish` rejected.

## Related
- Files: `model/AnalysisRecord.kt`, `repository/AnalysisRepository.kt`, `service/AnalysisRecorderService.kt`, `service/AnalysisHistoryService.kt`. Issues: #24, #25. Decisions: D24-001, D24-004, D25-002, D25-004.

## Reconsider when
Retention/deletion rules appear, or E6.4 needs scheduler state stored with the record.
