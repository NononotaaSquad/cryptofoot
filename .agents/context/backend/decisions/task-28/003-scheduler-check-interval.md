# D28-003: Labelled shortened scheduler check interval (`demo` profile)

- **Status:** superseded by [D28-005](005-demo-config-in-local-profile.md)
- **Date:** 2026-11-21
- **Type:** requirement from E6.4/E7.2 ("any shortened test interval is clearly labelled") + own proposal for the mechanism
- **Source:** `epics/E7-demo-scenario.md` E7.2; E6.4; issue #28; D27-008
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
The demo cannot wait a day. D27-008 added the labelled cadence `DEMO_EVERY_MINUTE`; the scheduler also checks due terms only every 30 seconds.

## Options considered
Shorten the production defaults; a dedicated profile; flags set by hand.

## Decision (proposed)
Spring profile `demo` (`application-demo.yaml`): `app.scheduler.tick=PT5S` and `app.scheduler.demo-enabled=true`. It is labelled in the file, in a WARN log at startup (`DemoModeNotice`) and, per schedule, by `demo: true`. Production defaults are unchanged (tick 30 s, demo cadence refused).

## Consequences and risks
- The profile must never be active in production.
- The 5 s tick plus the 1 minute cadence give a visible run about every minute.

## Verification
`DemoProfileSpec` (tick `PT5S`, demo cadence enabled, notice bean present).

## Related
- Files: `application-demo.yaml`, `config/DemoModeNotice.kt`. Issues: #27, #28. Decisions: D27-008.

## Reconsider when
The film script needs another demonstration of periodic execution.