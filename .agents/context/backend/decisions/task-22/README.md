# Decision register: task #22 (Classify Excluded Addresses with a reason)

Lives under the existing backend decision directory. IDs are `D22-NNN`. Related: `../task-24/` (ports, D24-003; unclassified candidates, D24-006), `../0002-address-candidate-validation.md` (canonical form).

"Accepted" means the user (task owner, Maksym U.) chose the option in the grilling session of 2026-11-21. Mentor question 4 (approved external services) is still open; D22-005 is built so it can be switched off.

| ID | Topic | Status | Approval source | Supersedes / superseded by |
|---|---|---|---|---|
| [D22-001](001-classification-pipeline.md) | Classification order and the three outcomes | accepted | User answers 2026-11-21 | none |
| [D22-002](002-label-lists.md) | Label lists: imported Etherscan dataset + own list | accepted | User answers 2026-11-21 | none |
| [D22-003](003-keyless-bytecode-check.md) | Keyless bytecode check: public JSON-RPC, Blockchair fallback | accepted | User answers 2026-11-21 | none |
| [D22-004](004-eip-7702-delegated-accounts.md) | EIP-7702 delegated accounts are private accounts | accepted | User answer 2026-11-21 (deviation from the brief's literal rule) | none |
| [D22-005](005-external-call-approval.md) | External call on by default with a kill switch | accepted | User answer 2026-11-21 (mentor question 4 open) | none |
| [D22-006](006-tag-mapping.md) | Which dataset tags exclude, and which never do | accepted | User answer 2026-11-21 (fundraiser scope) | none |
| [D22-007](007-wiring-and-source-text.md) | Wiring, classification source text, Mentions kept | proposed | none | none |
