# E6. Returning to a Case

**Status:** manual and periodic execution confirmed by the team on 2026-11-21; cadence and execution policies remain open. **Module:** M4. Official's view.

Business references: [BUSINESS-SPEC.md](../BUSINESS-SPEC.md), [BUSINESS-CONTEXT.md](../BUSINESS-CONTEXT.md).
Challenge reference: [task](../hacknation-2026-task/MF%20-%20Pełna%20Treść%20Zadania%20Konkursowego.md), sections 2 and 3.

## Goal

The Analyst can return to a Case, inspect earlier Analyses and distinguish changes in findings from changes in source availability or scoring.

## Required outcome

Periodic execution, saving successive results and comparing results are stated requirements. They are not optional extras. The task separately lists detecting replaced profile addresses as an optional example of temporal analysis. Manual reruns alone do not satisfy periodic execution.

## User stories and acceptance criteria

### E6.1. Save and reopen an Analysis

As an Analyst, I want the history of a Case, so that I can inspect the result as it was obtained.

- Each execution has a separate Analysis identifier, start/end times where applicable, trigger (manual or scheduled) and a visible outcome: running, completed, partial or failed. Exact lifecycle names must be aligned with the domain model.
- The Analysis retains the identifiers actually used, source/network coverage and algorithm version, with references to its Mentions, results and exclusions.
- A later execution does not overwrite an earlier Analysis. Its report can be reopened through E5.
- Failure is retained in history and is not represented as a successful Analysis with zero findings.

### E6.2. Repeat an Analysis manually

As an Analyst, I want to rerun the Analysis for the same Case, so that I can check for new findings.

- A manual rerun creates a new Analysis and makes its status visible.
- Earlier results remain available while the new execution is running or has failed.
- The proposed minimum uses the Case's current identifiers and records them in the new Analysis. Identifier differences are visible during comparison.

### E6.3. Compare two Analyses

As an Analyst, I want to compare two Analyses of one Case, so that I can identify what needs verification.

- The compared Analyses and their timestamps are explicit. Attributions are matched by network and the canonical address supplied by the address-validation domain.
- The proposed minimum distinguishes: newly observed Attribution, Attribution not observed in the later Analysis, changed Confidence Level, and changed exclusion classification. Evidence references for both sides remain accessible.
- Repeated copies of an unchanged Mention do not alone count as new independent support. E2/E3 must supply stable identity/provenance rules for Mentions.
- "Not observed" does not mean "no longer belongs to the person". If source coverage differs or an Analysis is partial, the comparison states that the apparent absence cannot be verified.
- Different identifiers, source/network scope or algorithm versions are shown as comparability limitations. A changed score is not automatically described as changed behaviour.
- Automatic detection of an address being replaced on a profile is outside this proposed minimum.

### E6.4. Execute periodically

As an Analyst, I want scheduled Analyses, so that a Case can be checked repeatedly without manual action.

- A schedule can be enabled and disabled for a Case. Its cadence, timezone and next execution time are visible.
- Each scheduled execution creates a saved Analysis that can be inspected and compared through the same flow as a manual execution.
- Schedule failures are visible; the UI does not report a successful execution when none occurred.
- Manual execution by the Analyst and periodic execution are both confirmed requirements. Frequency, execution time, timezone and whether the cadence is fixed or Analyst-configurable remain open; a daily cadence has not been approved. Scheduling is active while the application and its execution service are running; no availability guarantee is implied.
- Proposed overlap policy: do not start a second Analysis for a Case already being processed; record that the scheduled attempt was skipped. Missed intervals during downtime are not backfilled automatically. Both policies await confirmation.

## Delivery order

1. Saved Analyses and history.
2. Manual rerun and comparison.
3. Minimal periodic execution.

This order supports incremental delivery; it does not remove periodic execution from the required target scope. If it is deferred, record the unmet requirement explicitly.

## Dependencies and boundaries

- E1 supplies stable Case identity; E2/E3/E4 supply results and provenance; E5 displays saved results.
- This epic preserves references to evidence. The form and retention of captured source content must be agreed with E2.
- Proposed minimum excludes notifications, arbitrary recurrence rules, automatic identity merging and automatic conclusions about ownership changes.

## Decisions to confirm

| ID | Question and proposed resolution | Owner | Needed before |
|---|---|---|---|
| E6-D1 | CONFIRMED: the Analyst can start an Analysis manually and Analyses also execute periodically. Confirmed on 2026-11-21; this does not approve a daily cadence or the remaining comparison details. | Team | Recorded |
| E6-D2 | Use the proposed comparison categories and network/address identity? | Team and E2/E3 owners | Comparison implementation |
| E6-D3 | Which frequency, time/timezone and fixed or configurable cadence? Which overlap and downtime policies? Proposed policies: explicit timezone, skip overlap, no backfill. | Team | Scheduler implementation |
| E6-D4 | Who provides persistence, execution status and periodic triggering? | Frontend/backend owners | Integration planning |

## Documentation reconciliation

BUSINESS-SPEC.md currently marks all of E6 as an extra. This contradicts the required capabilities in the task and must be reconciled when the team adopts this scope. State names and relationships must also be added to domain-model.md before implementation.
