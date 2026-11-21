# D28-002: Proposed boundary of the demo data (for the later reset)

- **Status:** proposed (not agreed with the fixture owner or the E1 owner)
- **Date:** 2026-11-21
- **Type:** own proposal; temporary assumption
- **Source:** issue #28; E7.4; current data model (collections `analyses` and `schedules` keyed by `caseId`, `mentions` global with a content `identity`)
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody. Needed: Maksym Z. (fixtures, #37) and Marcin (Case, #5). The user preferred the trigger described below ("endpoint only in demo mode") but deferred the whole reset (D28-001).

## Problem and context
Data of the demo Case lives in several collections. Mentions have no `caseId` and are deduplicated by content identity across Analyses, so they cannot be removed per Case without affecting other Cases.

## Options considered
1. Delete by one fixed demo Case id: its Analyses and schedule (and, later, its Case record).
2. Also delete Mentions from the synthetic source.
3. Delete everything created after a marker time.

## Decision (proposed)
Option 1. The demo Case has one fixed id (`demo-v1`, from the fixture version, set in `app.demo.case-id`). Reset removes only records with that `caseId` and recreates the Case from the fixtures; it never touches another Case, never drops a collection, and does not remove Mentions. Repeating the reset gives the same state (idempotent). Trigger (the user's preference): `POST /api/1/demo/reset`, available only when `app.demo.enabled=true` (default off, otherwise 404), which adds to the API contract (api-sync).

## Consequences and risks
- Synthetic Mentions accumulate; they are identical on each fixture run (same identity), so this is harmless for repeatability but must be confirmed by the E2 owner.
- The id must not collide with a real user Case; the id is reserved for the demo.
- Needs the Case store (E1) and fixtures (E2 T4).

## Verification (to do with the subtask)
Specs: other Cases untouched, repeated reset gives the same state, endpoint refused outside demo mode.

## Related
- Issues: #5, #28, #37 and the reset subtask. Decisions: D28-001, D28-004.

## Reconsider when
The fixture or Case owners propose another boundary, or Mentions get a Case link.