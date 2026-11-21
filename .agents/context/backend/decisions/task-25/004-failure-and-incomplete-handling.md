# D25-004: Failed, partial and unfinished Analyses

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** requirement from the brief (failure stays in history) + own proposal (guards)
- **Source:** issue #25 criterion 5; E6.1 ("Failure is retained in history ... not a successful Analysis with zero findings"); D24-002, D24-004
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
Failures and partial results must be stored as such. The recorder is called by the orchestration (#24); it can be misused (finish twice, finish unknown id) and a process can die between `begin` and `finish`.

## Options considered
1. Recorder silently ignores invalid calls.
2. Recorder fails loudly on invalid calls; stale `RUNNING` stays visible.
3. Recorder (or a job) closes stale `RUNNING` Analyses as `FAILED` after a timeout.

## Decision (proposed)
Option 2. `FAILED` and `PARTIAL` results are stored with their `failures`, `limitations` and the output of earlier stages; status and `finishedAt` are set. `finish` with status `RUNNING`, for an unknown id, or for an already finished Analysis throws `IllegalStateException` (becomes 500 via the global handler) and changes nothing. A stale `RUNNING` record is not closed automatically (no timeout defined; belongs to E6.4/E6-D3).

## Consequences and risks
- A crash leaves a permanent `RUNNING` Analysis that a Case history shows as running.
- An exception from `finish` after a successful run loses that run's result; the orchestrator does not retry.
- Failure details are fixed codes (no exception text, may contain personal data).

## Verification
`AnalysisHistorySpec`: failed Analysis stored with failure code and limitations; double `finish` rejected, first result preserved.

## Related
- Files: `service/AnalysisRecorderService.kt`. Issues: #24, #25. Decisions: D24-002, D25-001.

## Reconsider when
A timeout/recovery policy for stale `RUNNING` is decided (E6-D3/E6-D4), or retries are added.
