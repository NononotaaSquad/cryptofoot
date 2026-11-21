# DS1-001: Wiring the Analysis flow, the start endpoint and the scheduler switch

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** own proposal; follow-up announced by D24-005, D26-005 and D27-009 ("reconsider when adapters exist")
- **Source:** ADR 0001 (`POST /cases/{caseId}/analyses` -> 202, 409 while running); E2.1, E6.2, E6.4; D24-004, D24-005, D26-005, D27-009
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
Every module existed on `master`, but none ran in the application: `AnalysisOrchestrator` and `AnalysisRerunService` were plain classes, `POST /cases/{caseId}/analyses` was a `TODO`, and `ScheduledRunner` idled forever ("Scheduler idle: the Analysis flow is not wired"). The missing pieces were a `CandidatePreparation` adapter (DS1-002) and a `ConfidenceScoring` implementation (DS1-003).

## Options considered
1. Annotate the orchestrator and the rerun service with `@Service`.
2. A `@Configuration` that builds them from the existing port beans, keeping the classes framework-free (as the owners intended).

## Decision (proposed)
Option 2, `config/AnalysisConfig`:
- `analysisOrchestrator` from the beans `SearchService` (MentionSearch), `CandidatePreparationService`, the `ExclusionClassifier` of `ExclusionConfig`, `confidenceScoring` and `AnalysisRecorderService`; logs which implementation backs each port.
- `analysisRerunService` with `CaseService` as `CaseIdentifierSource` and a virtual-thread executor owned by the configuration (not a context bean, so Spring Boot's `applicationTaskExecutor` is untouched); shut down on context close.
- `CaseController.startAnalysis` calls `rerun(caseId)` (trigger `MANUAL`): 404 unknown Case, 409 while one is running, else 202 with the `RUNNING` Analysis; execution continues asynchronously and the client polls `GET /analyses/{id}`.
- `ScheduledRunner` now finds the bean and runs due terms through the same flow. New switch `app.scheduler.enabled` (default `true`) removes the runner bean; integration specs set it to `false` in `BaseSpec`, because a background tick on the system clock races the specs that drive `ScheduledRunner` with a controlled clock.

## Consequences and risks
- The application runs real Analyses: the synthetic sources (`synthetic`, `synthetic-broken`) are active by default, so every Analysis in the default configuration is `PARTIAL` (one source always unavailable, by design of E2 T4).
- With `app.exclusion.onchain-check-enabled=true` (the default) ETH candidates not on a label list are sent to the keyless public services (D22-005). Turn it off with `EXCLUSION_ONCHAIN_CHECK_ENABLED=false` until the mentors approve them.
- The overlap check stays check-then-insert (D26-005) and a crash still leaves `RUNNING` (D25-004).
- No REST contract change: the endpoint signature existed; `OpenApiContractSpec` passes without `api-sync`.

## Verification
`AnalysisFlowSpec` (HTTP, real beans, demo-v1 fixture, local on-chain stub): 202 + polling to `PARTIAL`, 409 while running, 404 for an unknown Case, rerun keeps the earlier result. `ScheduledAnalysisWiringSpec`: the bean exists, the runner bean is off in specs, a due term starts a real `SCHEDULED` Analysis.

## Related
- Files: `config/AnalysisConfig.kt`, `controller/CaseController.kt`, `service/ScheduledRunner.kt`, `integrationTest/.../BaseSpec.groovy`. Decisions: D24-005, D26-005, D27-009, DS1-002, DS1-003. Diagrams: `03-analysis-start.puml`, `04-analysis-pipeline.puml`, `12-periodic-execution.puml`.

## Reconsider when
More than one application instance runs (overlap and claims are single-instance), or the owners want a bounded executor.
