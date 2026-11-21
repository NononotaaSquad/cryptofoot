# D28-005: Demo scheduler settings live in the `local` profile

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** change requested by the task owner; supersedes D28-003
- **Source:** the user's request in the working session of 2026-11-21 ("add it to local instead of demo properties"); E6.4/E7.2 (labelled shortened interval)
- **Proposed by:** the user (idea), AI agent (Claude) (implementation)
- **Approved by:** the idea comes from the task owner; the resulting change is awaiting the owner's approval at the approval gate
- **Supersedes:** [D28-003](003-scheduler-check-interval.md)

## Reason for the change
D28-003 added a separate `demo` profile. The owner prefers fewer moving parts: `application-local.yaml` was empty and the `local` profile is already what local runs use.

## Options considered
A separate `demo` profile (D28-003); the settings in `application-local.yaml`; defaults changed for all profiles.

## Decision (proposed)
`application-local.yaml` sets `app.scheduler.tick=PT5S` and `app.scheduler.demo-enabled=true` (commented as not for production). `application.yaml` and `application-prod.yaml` keep the production defaults (tick 30 s, demo cadence refused). The label no longer depends on a profile name: `DemoModeNotice` logs a WARN at startup whenever `app.scheduler.demo-enabled=true`, and every schedule with the demo cadence is returned with `demo: true`.

## Consequences and risks
- Every local run (including `./scripts/run-backend.sh`, which uses `local`) now checks the scheduler every 5 seconds and accepts `DEMO_EVERY_MINUTE`; there is no way to run "local" without it except overriding the properties.
- The `local` profile must never be used in production; `prod` is a separate profile.
- Any profile or environment variable that sets `APP_SCHEDULER_DEMO_ENABLED=true` also enables the notice and the demo cadence.

## Verification
`LocalDemoConfigSpec` (profile `local`: tick `PT5S`, demo cadence enabled, notice bean present); `ScheduleApiSpec` (the demo cadence is refused with the default configuration).

## Related
- Files: `application-local.yaml`, `config/DemoModeNotice.kt`. Issues: #27, #28. Decisions: D27-008, D28-003.

## Reconsider when
The local profile should keep the production interval, or the film needs another mechanism.