# Decision register: task #25 (Save every Analysis and keep the Case history)

Lives under the existing backend decision directory (`../0001-epic-api-contract.md`, `../0002-address-candidate-validation.md`). IDs are `D25-NNN` to avoid clashing with the numbered ADRs. Task #24 decisions are in `../task-24/`.

Each decision is one file. "Proposed" means the author's proposal; neither this record nor silence is an approval. Superseded or rejected decisions are kept and linked.

| ID | Topic | Status | Approval source | Supersedes / superseded by |
|---|---|---|---|---|
| [D25-001](001-analysis-snapshot-policy.md) | Immutable record per Analysis | proposed | none | none |
| [D25-002](002-persistence-model-and-references.md) | Persistence model and references | proposed | none (owners of E2 #16, #21/#24) | none |
| [D25-003](003-api-contract-for-history.md) | API contract for history and one Analysis | proposed | none (frontend owner must be told) | none |
| [D25-004](004-failure-and-incomplete-handling.md) | Failed, partial and unfinished Analyses | proposed | none | none |
| [D25-005](005-network-coverage-assumption.md) | Network coverage = supported networks (temporary) | proposed | none | none |
