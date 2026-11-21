# Demo runbook: periodic execution check and demo reset (Bohdan's part of #28)

Scope: only the checklist item "Reset of the demo Case without deleting other Cases; short check of periodic execution" of the shared issue #28 (E7). Fixtures, scoring, blockchain scenario, film and slides belong to other owners. Decisions: `.agents/context/backend/decisions/task-28/`.

The demo is the team-confirmed **controlled synthetic demo**; checking real public sources is a separate documented check (E7-D1). Mentor acceptance of this arrangement is still open.

## Status of the parts

| Part | Status |
|---|---|
| Labelled shortened scheduler interval (`local` profile) | done |
| Automated check: a due term creates a new saved Analysis with trigger `SCHEDULED`; disabling stops further ones | done (`DemoSchedulerCheckSpec`, controlled clock, stubbed modules) |
| Live check in the running application | possible since backend-sync-1 (flow wired, DS1-001), see below |
| Reset of the demo Case | **not implemented**, waiting for the owners, see below |

## Prerequisites

- Docker (MongoDB) and JDK 25. Backend with MongoDB: `./scripts/run-backend.sh`, or `MONGODB_URI=mongodb://localhost:27017/db ./gradlew bootRun` in `backend/`.
- The shortened interval is set in the `local` profile (`application-local.yaml`, `SPRING_PROFILES_ACTIVE=local`, the default of `./scripts/run-backend.sh`). It sets `app.scheduler.tick=PT5S` (the scheduler looks for due terms every 5 seconds instead of 30) and `app.scheduler.demo-enabled=true` (allows the preset `DEMO_EVERY_MINUTE`). The application logs a WARN "DEMO mode active ..." at startup whenever `app.scheduler.demo-enabled=true` and every schedule of that preset is returned with `demo: true`. Never use the `local` profile in production; `prod` keeps the production defaults (30 s, demo cadence refused).

## Repeatable verification (works now)

```bash
cd backend
./gradlew integrationTest --tests '*DemoSchedulerCheckSpec' --tests '*ScheduledRunnerSpec' --tests '*LocalDemoConfigSpec'
```

`DemoSchedulerCheckSpec` uses a controllable clock (no waiting) and shows:
1. enabling `DEMO_EVERY_MINUTE` gives the next full minute as the next execution, labelled `demo`;
2. at each due minute a new Analysis is saved with trigger `SCHEDULED`; the earlier one is unchanged and the ids differ;
3. after the schedule is disabled, later ticks create no Analysis and the next execution is empty.

The modules behind the flow (search, candidates, exclusions, scoring) are **stubs** in this spec. It proves the scheduling and recording flow; it is not evidence that a live source or the real scoring ran.

## Live check in the running application

Run the backend with the `local` profile, then:

```bash
CASE=$(curl -s -X POST localhost:8080/api/1/cases -H 'Content-Type: application/json' -d '{"nickname":"zenek_fx"}' | jq -r .id)
curl -X PUT localhost:8080/api/1/cases/$CASE/schedule -H 'Content-Type: application/json' -d '{"enabled":true,"cadence":"DEMO_EVERY_MINUTE"}'
curl localhost:8080/api/1/cases/$CASE/schedule        # nextExecutionAt, lastAttempt
curl localhost:8080/api/1/cases/$CASE/analyses        # a new Analysis with trigger SCHEDULED every minute
curl -X PUT localhost:8080/api/1/cases/$CASE/schedule -H 'Content-Type: application/json' -d '{"enabled":false}'
curl localhost:8080/api/1/cases/$CASE/analyses        # no new Analysis after disabling
```

**Former blocker (resolved by backend-sync-1):** the shared flow is now the `AnalysisRerunService` bean of `config/AnalysisConfig`, fed by the real Case store, the synthetic `demo-v1` search, candidate preparation, exclusions and the provisional scoring (`provisional-1`, placeholder for #9). Each scheduled Analysis of the demo person ends `PARTIAL`, because the source `synthetic-broken` is always unavailable by design. Set `EXCLUSION_ONCHAIN_CHECK_ENABLED=false` if no address may leave the application (D22-005); the ETH candidates are then unclassified and flagged.

## Reset of the demo Case (not implemented)

There is no demo Case yet: no Case store and no fixtures exist, and nothing defines which data belongs to the demo. Proposed boundary (D28-002, not approved by the fixture owner or the E1 owner): one demo Case id; reset deletes only the Analyses and the schedule of that id, never another Case, and leaves Mentions alone (shared, not linked to a Case). A follow-up subtask, blocked by #5 and #37, covers the implementation as an endpoint that works only in demo mode.