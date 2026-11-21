# D28-004: How periodic execution is verified, and what blocks the live check

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** own proposal; limitation of the demonstration
- **Source:** issue #28; E7.2 ("a recorded or mocked output is never presented as evidence that a live integration succeeded"); E6.4; D27-009
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
The live check needs the shared Analysis flow as a bean, which depends on Cases (E1), the synthetic source (E2 T4), and module adapters.

## Options considered
Wire fake adapters in the demo profile; verify only with an automated spec; verify with a spec and document the live steps as blocked.

## Decision (proposed)
Third option. `DemoSchedulerCheckSpec` drives the real scheduler, recorder and flow with a controllable clock and stubbed modules and asserts: a new Analysis with trigger `SCHEDULED` per due minute (earlier ones unchanged) and no more Analyses after the schedule is disabled. The runbook lists the live curl steps and the exact blocker. No fake adapter is added to the application, so nothing mocked can be shown as a live run.

## Consequences and risks
- The film cannot show a live scheduled Analysis until the dependencies exist.
- The spec proves scheduling and recording, not the content of the Analysis.

## Verification
`DemoSchedulerCheckSpec`, `ScheduledRunnerSpec`.

## Related
- Files: `.agents/context/backend/demo-scheduler-check.md`. Issues: #5, #27, #28, #37. Decisions: D27-009, D28-001, D28-005.

## Reconsider when
The Analysis flow is wired; replace the blocker by the live check.