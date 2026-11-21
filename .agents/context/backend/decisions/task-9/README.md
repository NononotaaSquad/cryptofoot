# Decision register: task #9 (E3: Confidence Level algorithm)

Lives under the existing backend decision directory. IDs are `D9-NNN`. Related: `../task-24/` (ports D24-003, unclassified candidates D24-006), `../task-22/` (classification, the input owner's side), `../task-50/006-input-is-not-evidence.md` (wallet), `../sync-1/003-provisional-confidence-scoring.md` (the placeholder this task replaces), `../0001-epic-api-contract.md` (report contract).

"Accepted" means the user (Maksym U., who implements #9 and owns "E3: Input for the Confidence Level algorithm") chose the option in the grilling session of 2026-11-21. Requirements applied: issue #9 acceptance criteria; the frontend decision of 2026-11-21 (the Justification is a short template text again, no numbers, no breakdown shown; the last part was reversed by the frontend lead on 2026-11-21 after #58, see the D9-006 amendment); challenge cheat sheet KROK 2 (bands, +15 synergy, "several occurrences within one service are no extra confirmation"), KROK 4 (rules must be explainable and reproducible); E7.3 adverse cases.

| ID | Topic | Status | Approval source | Supersedes / superseded by |
|---|---|---|---|---|
| [D9-001](001-scoring-input-contract.md) | Input contract: `ScoringInput(accepted, unclassified, mentions, identifiers)` | accepted | User answers 2026-11-21 (input owner = implementer) | supersedes the port part of DS1-003 |
| [D9-002](002-bands-base-and-bonus.md) | Bands from the appendix (lower bound), strongest Mention sets the base, +15 once | accepted | User answers 2026-11-21 (team question 8) | supersedes the rules of DS1-003 |
| [D9-003](003-source-kind-inference.md) | Source kind inferred by scoring: own profile rule, keyword lists, precedence | accepted | User answers 2026-11-21 | none |
| [D9-004](004-which-mentions-support.md) | Which Mentions support an address: person link, wallet, full name, warnings, conflicting e-mail, 0% when none | accepted | User answers 2026-11-21 | enforces D50-006 for E3 |
| [D9-005](005-independent-sources-and-copies.md) | Independent sources: different origin and a different address sentence | accepted | User answers 2026-11-21 | refines DS1-003 (copy = same sentence, not same snippet) |
| [D9-006](006-justification-text-and-contract.md) | Justification text from rule templates; contract `justification: String`, `mentions` | accepted (contract change needs frontend awareness); amended 2026-11-21: the report shows the breakdown | User answers 2026-11-21; frontend decision 2026-11-21 | supersedes "Justification is a list of MentionResponse" in ADR 0001 |
| [D9-007](007-unclassified-candidates.md) | Unclassified candidate: sentence, no cap | accepted | User answer 2026-11-21 | refines D24-006 |
| [D9-008](008-algorithm-version-and-demo-scores.md) | Algorithm version `confidence-1`, demo-v1 kept, frozen expected scores (E7-D3) | accepted | User answers 2026-11-21 | answers E7-D3 for demo-v1 |

Code: `backend/src/main/kotlin/squad/nononotaa/backend/service/ConfidenceScoringService.kt` (`ScoringRule` enum + `ConfidenceScoringService`). Specs: `ConfidenceScoringServiceSpec` (unit), `AnalysisFlowSpec` and `AnalysisReportSpec` (HTTP). Diagram: `../../diagrams/08-confidence-scoring.puml`.
