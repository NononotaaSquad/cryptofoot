# Backend flow diagrams (PlantUML)

One diagram per backend aspect, drawn from the code on branch `backend-sync-1` (the wired Analysis flow, decisions `../decisions/sync-1/`). Names follow `glossary.md`. When a flow changes, update its diagram in the same PR.

| File | Aspect | Kind | Epics / decisions |
|---|---|---|---|
| [01-system-overview.puml](01-system-overview.puml) | Controllers, services, Analysis ports and their implementations, MongoDB collections, external services | component | ADR 0001, D24-003, DS1-001 |
| [02-case-creation.puml](02-case-creation.puml) | `POST /cases`: trimming, narrowing-identifier rule, wallet validation and canonical form | sequence | E1, D5-*, D50-* |
| [03-analysis-start.puml](03-analysis-start.puml) | `POST /cases/{id}/analyses`: 404/409/202, `RUNNING` record, asynchronous execution, polling | sequence | E2.1, E6.2, D24-004, D26-005, DS1-001 |
| [04-analysis-pipeline.puml](04-analysis-pipeline.puml) | Orchestrator stages and how the status and limitations are derived | activity | D24-001, D24-002, D24-006, D23-005 |
| [05-mention-search.puml](05-mention-search.puml) | Parallel sources, timeout, relevance filter, snippet, detection, Mention identity | sequence | E2, DS1-004 |
| [06-candidate-preparation.puml](06-candidate-preparation.puml) | Detected strings -> validated canonical candidates and rejected ones | activity | ADR 0002, DS1-002 |
| [07-exclusion-classification.puml](07-exclusion-classification.puml) | Label lists, bytecode check, accepted / Excluded Address / unclassified | activity | E3, D22-* |
| [08-confidence-scoring.puml](08-confidence-scoring.puml) | Confidence Level algorithm `confidence-1`: support, source kind, bonus, Justification text | activity | E3, D9-* |
| [09-analysis-lifecycle.puml](09-analysis-lifecycle.puml) | `RUNNING` -> `COMPLETED` / `PARTIAL` / `FAILED` | state | D24-001, D25-001 |
| [10-report-and-history.puml](10-report-and-history.puml) | Case list and details, history, Mentions and report reads | sequence | E5, E6.1, DS1-005 |
| [11-analysis-comparison.puml](11-analysis-comparison.puml) | Comparison categories, `absenceVerifiable`, comparability limitations | activity | E6.3, D26-* |
| [12-periodic-execution.puml](12-periodic-execution.puml) | Schedule update and the scheduler tick (claim, overlap, downtime) | sequence | E6.4, D27-*, DS1-006 |
| [13-data-model.puml](13-data-model.puml) | MongoDB documents and the embedded Analysis snapshot | class | D25-002 |
| [14-error-handling.puml](14-error-handling.puml) | Exception to HTTP status mapping | activity | ADR-004 |
| [15-address-activity.puml](15-address-activity.puml) | Address Activity: which Attributions, fixture first then live explorers, totals and cash-out signals | activity | E4, D23-* |

## Viewing

- IntelliJ IDEA: the PlantUML Integration plugin previews `.puml` files.
- VS Code: the "PlantUML" extension (`jebbs.plantuml`).
- CLI: `java -jar plantuml.jar -tsvg .agents/context/backend/diagrams/*.puml` (component and class diagrams need Graphviz, or add `!pragma layout smetana`).

All diagrams render without errors or deprecation warnings with PlantUML 1.2026.8 (the jar of the IntelliJ PlantUML plugin). Colour an activity with the current syntax `:text; <<#pink>>`; the older prefix `#pink:text;` is deprecated and breaks single-line activities.
