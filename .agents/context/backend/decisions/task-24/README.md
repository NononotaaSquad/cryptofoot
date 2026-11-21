# Decision register: task #24 (Analysis orchestration)

Lives under the existing backend decision directory (`../0001-epic-api-contract.md` is the REST contract this work aligns with). IDs are `D24-NNN` to avoid clashing with the numbered ADRs of other tasks.

Each decision is one file. "Proposed" means the author's proposal; neither this record nor silence is an approval. Superseded or rejected decisions are kept and linked.

| ID | Topic | Status | Approval source | Supersedes / superseded by |
|---|---|---|---|---|
| [D24-001](001-analysis-lifecycle-and-statuses.md) | Analysis lifecycle and status names | proposed | none | none |
| [D24-002](002-analysis-failure-policy.md) | Failure policy: no results vs failure | superseded in part | none | superseded (unclassified row) by D24-006 |
| [D24-003](003-module-contracts.md) | Contracts for search, candidates, exclusions, scoring | proposed | none (owners: Maksym Z., Maksym U., Marcin) | none |
| [D24-004](004-persistence-handoff.md) | Handing the result to storage (#25) | proposed | none (owner of #25; E6-D4 open) | none |
| [D24-005](005-scope-no-wiring-no-http.md) | Scope: no production wiring, no HTTP endpoint | proposed | none | superseded in part by [DS1-001](../sync-1/001-analysis-flow-wiring.md) |
| [D24-006](006-unclassified-candidates-are-scored-and-flagged.md) | Unclassified candidates are scored and flagged | proposed | none (contract is Maksym U.'s accepted ADR 0001; E3 owners must confirm) | supersedes part of D24-002 |
