# Decision register: task #27 (Periodic execution of Analyses)

Lives under the existing backend decision directory (`../0001-epic-api-contract.md`). IDs are `D27-NNN`. Related: `../task-24/`, `../task-25/`, `../task-26/`.

Each decision is one file. "Proposed" means the author's proposal; neither this record nor silence is an approval. "Accepted" here means the user (task owner) chose the option in the working session of 2026-11-21; the team decision E6-D3 is still open in the epic. Superseded or rejected decisions are kept and linked.

| ID | Topic | Status | Approval source | Supersedes / superseded by |
|---|---|---|---|---|
| [D27-001](001-default-cadence.md) | Default cadence: daily 06:00 | accepted | User answer 2026-11-21 (E6-D3 open) | none |
| [D27-002](002-timezone.md) | Time zone Europe/Warsaw | accepted | User answer 2026-11-21 | none |
| [D27-003](003-configurability.md) | Predefined options per Case | accepted | User answer 2026-11-21 | none |
| [D27-004](004-preset-list.md) | List of predefined cadences | proposed | none | none |
| [D27-005](005-overlap-policy.md) | Skip and record overlapping terms | accepted | User answer 2026-11-21 | none |
| [D27-006](006-downtime-policy.md) | No catch-up after downtime (grace window proposed) | accepted | User answer 2026-11-21 (grace window not approved) | none |
| [D27-007](007-attempt-visibility-and-contract.md) | Visible attempts and schedule API | proposed | none (frontend owner must be told) | none |
| [D27-008](008-demo-interval.md) | Labelled demo interval | proposed | none | one-minute restriction superseded by [D27-010](010-custom-interval.md) |
| [D27-009](009-runtime-and-wiring.md) | Scheduler runtime and wiring | proposed | none | superseded in part by [DS1-001](../sync-1/001-analysis-flow-wiring.md) || [D27-010](010-custom-interval.md) | Analyst-defined interval (e.g. every 1 minute) | proposed | request of the task owner (#78); team E6-D3 open | supersedes the one-minute restriction of D27-008 and the fixed list of D27-003/004 |
