# D27-010: Analyst-defined interval ("every 1 minute") in the existing scheduler

- **Status:** proposed (change requested by the task owner; issue #78)
- **Date:** 2026-11-22
- **Type:** requirement from the owner's request; supersedes the restriction of D27-003 ("no arbitrary rules" for interval length) and of D27-008 (one-minute cadence only in demo mode)
- **Source:** issue #78 (follow-up of #27 and #15); E6.4; E6-D3
- **Proposed by:** AI agent (Claude)
- **Approved by:** the request to allow "every 1 minute" for the schedule comes from the task owner; the team has not confirmed E6-D3

## Problem and context
The Analyst could only pick fixed presets; "every minute" existed only as `DEMO_EVERY_MINUTE`, refused unless `app.scheduler.demo-enabled=true`, so a normal form could not set it.

## Decision (proposed)
- New preset `SchedulePreset.CUSTOM_INTERVAL` with `intervalValue` (integer) and `intervalUnit` (`MINUTES`, `HOURS`, `DAYS`) on `UpdateScheduleRequest`, `ScheduleResponse` and `ScheduleRecord`. It is not a demo preset and needs no flag. There is no second scheduler: `ScheduledRunner` serves it exactly like the other presets.
- Validation in `ScheduleService` (the backend is the authority; the frontend only mirrors it for fast feedback): whole numbers only (`1.5`, strings, overflow → 400), `value >= 1`, total length from 1 minute to 365 days (`525600 min`, `8760 h`, `365 d`), both fields required for `CUSTOM_INTERVAL`. Messages are Polish. A rejected request changes nothing.
- First term: `now + interval` at the moment of saving (not rounded to the clock, because "every 1 minute" is a gap, not a time of day). Calendar presets keep their rules (D27-001/002/004).
- Next term after a claim: `due + k * interval`, the smallest `k` that is after the current time. The grid stays anchored on the first term, so a late tick (tick `app.scheduler.tick`, default now `PT10S`, was `PT30S`) does not drift the later terms, and after downtime the next term is the next grid point in the future (no catch-up, D27-006).
- Changing the interval or the unit, or enabling a disabled schedule, recomputes the term from now. Repeating the same saved settings keeps the term. Disabling clears `nextExecutionAt` and keeps cadence and interval for the next enabling; switching to a preset drops the interval fields. `PUT {enabled:true}` without cadence and interval reuses the stored ones.
- Overlap (D27-005) is unchanged: while an Analysis of the Case is `RUNNING` the term is recorded as `SKIPPED_OVERLAP` and the grid moves on; nothing is queued. `AnalysisRerunService` now makes the "already running?" check and the start of the Analysis atomic per instance (`synchronized`), so a manual and a scheduled start in one instance cannot both pass the check. Across instances this is still check-then-insert (D26-005).
- Multi-instance claim of a term is unchanged: `ScheduleRecord.@Version`; only one instance saves the claim.
- Existing records need no migration: the new fields are nullable and absent in stored documents (tested with a raw legacy document). `DEMO_EVERY_MINUTE` stays for compatibility and is still gated by `demo-enabled`.

## Consequences and risks
- A one-minute interval multiplies requests to the sources (and to the live explorers). The UI says so; there is no rate limit per source beyond the existing ones.
- A stale `RUNNING` Analysis (backend killed mid-Analysis, D25-004) keeps blocking its Case: every term is recorded as `SKIPPED_OVERLAP` until it is resolved. Historical records are deliberately not rewritten here.
- The interval is counted from the save, and from the claimed term afterwards, not from the end of the previous Analysis.
- `ScheduleResponse` gained `intervalValue` and `intervalUnit`; `cadence` gained `CUSTOM_INTERVAL` (additive; `api-sync` run).

## Verification
`ScheduleCalculatorSpec` (first term, grid anchoring, late tick, early claim), `ScheduleApiSpec` (save and re-read, range edges, 0, negative, fraction, string, overflow, unit, missing fields, recompute, repeat, disable, switch to preset), `CustomIntervalSchedulerSpec` (two consecutive terms and disable, recompute, overlap without a queue, two instances at once, legacy record, fresh service after "restart"), `OpenApiContractSpec`. Live one-minute trial: `.agents/context/backend/demo-scheduler-check.md`.

## Related
- Files: `model/Enums.kt`, `model/ScheduleRecord.kt`, `service/ScheduleService.kt`, `service/ScheduleCalculator.kt`, `service/ScheduledRunner.kt`, `service/AnalysisRerunService.kt`, `util/IntegerOnly.kt`, `dto/`. Issues: #27, #15, #73, #78. Decisions: D27-003, D27-004, D27-005, D27-006, D27-008.

## Reconsider when
The team fixes the frequency policy (E6-D3), a source needs rate limiting, or stale `RUNNING` Analyses need recovery.
