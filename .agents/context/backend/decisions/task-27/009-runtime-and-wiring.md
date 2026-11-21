# D27-009: Scheduler runtime and wiring

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** own proposal; scope limit; missing wiring from #24/#26
- **Source:** issue #27 ("one shared flow"); `backend/decisions/task-24/005-scope-no-wiring-no-http.md`; `task-26/005-manual-rerun.md`; E6-D4 ("who provides persistence, execution status and periodic triggering")
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
Scheduled and manual runs must use one flow. That flow (`AnalysisRerunService` over the orchestrator) is not a Spring bean because the module adapters and the Case store do not exist.

## Options considered
A scheduler that builds its own flow; a scheduler with its own fake adapters; a scheduler that uses the shared flow when it exists.

## Decision (proposed)
`ScheduledRunner` is a `@Scheduled` component (tick `app.scheduler.tick`, default 30 s) that takes the shared flow from an `ObjectProvider<AnalysisRerunService>` and calls `rerun(caseId, SCHEDULED)`. It contains no search, orchestration or scoring. While no such bean exists it logs one warning and does nothing, and it never touches stored schedules. A `Clock` bean is provided for tests. Single application instance is assumed; the optimistic claim only protects against accidental double runs.

## Consequences and risks
- **Unmet:** no scheduled Analysis runs in the application until someone registers an `AnalysisRerunService` bean (needs E1 Cases, E2/E3 adapters). Enabling a schedule today only stores it.
- Stale `RUNNING` records from a crash block later terms as overlaps (D25-004).
- Several instances are not supported.

## Verification
`ScheduledRunnerSpec` runs the real recorder and flow with stubbed modules (trigger `SCHEDULED` saved) and checks the idle case.

## Related
- Files: `service/ScheduledRunner.kt`, `service/AnalysisRerunService.kt` (new optional `trigger`), `config/SchedulerConfig.kt`. Issues: #24, #26, #27. Decisions: D24-005, D26-005, D27-005, D27-006.

## Reconsider when
The Analysis flow is wired, or the deployment uses more than one instance.