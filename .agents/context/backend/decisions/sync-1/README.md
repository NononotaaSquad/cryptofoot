# Decision register: backend sync 1 (connecting the finished backend tasks)

Branch `backend-sync-1`. Connects the separately merged tasks #5, #21/#33, #22, #24, #25, #26, #27, #28, #50 and E2 T1–T4 into one running Analysis flow. IDs are `DS1-NNN`. "Proposed" means the author's proposal; nothing here is an approval by the module owners, the team or the mentors. Diagrams of every flow: `../../diagrams/`.

| ID | Topic | Status | Approval source | Supersedes / superseded by |
|---|---|---|---|---|
| [DS1-001](001-analysis-flow-wiring.md) | Wiring the Analysis flow, start endpoint, scheduler switch | proposed | none | supersedes D24-005, D26-005 (scope parts), the "unmet" part of D27-009 |
| [DS1-002](002-candidate-preparation-adapter.md) | E2 Mentions -> E3 candidate input adapter | proposed | none (E2 owner Maksym Z., E3 owner Maksym U.) | completes ADR 0002 "E2 must emit RawAddressCandidates" |
| [DS1-003](003-provisional-confidence-scoring.md) | Provisional Confidence Level algorithm; scoring port receives Mentions | superseded | none (owner of #9: Marcin) | resolves the D24-003 open point "Mention content for scoring"; superseded by [task-9](../task-9/README.md) |
| [DS1-004](004-search-relevance-and-detection.md) | Canonical wallet match, full name not a relevance key, bech32 in any case | proposed | none (E2 owner) | closes the known gap of D50-002 |
| [DS1-005](005-read-endpoints.md) | Case list and details, Mentions and report endpoints | proposed | none (E5/E6 frontend owners) | implements the stubs of ADR 0001; keeps D25-003 for history |
| [DS1-006](006-schedule-requires-case.md) | A schedule can only be set for an existing Case | proposed | none (owner of #27) | refines D27-007 |
| [DS1-007](007-known-gaps.md) | Gaps found while connecting, left to their owners | open | none | none |
