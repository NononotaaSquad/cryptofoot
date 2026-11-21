# DS1-005: Case list and details, Mentions and report endpoints

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** implementation of the ADR 0001 stubs; own proposal for the running case
- **Source:** ADR 0001 (`GET /cases`, `GET /cases/{caseId}`, `GET /analyses/{id}/mentions`, `GET /analyses/{id}/report`); E2.2, E2.3, E3.1, E5.1–E5.4, E6.1; D25-002 (stored snapshot); `DetectedAddressResponse` TODO ("never from the detection alone")
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet (frontend owners of #8, #10–#13)

## Decision (proposed)
- `GET /cases`: all Cases, newest first (`createdAt`, then id), each with `latestAnalysis` (newest by `startedAt`, any status, null when none). `GET /cases/{caseId}`: the same for one Case, 404 when unknown. One query per Case for the latest Analysis is accepted for the demo scale.
- `GET /analyses/{id}/mentions` and `/report` (`AnalysisReportService`) read only the stored Analysis; they never rerun it.
  - 404 for an unknown Analysis; **409** "Analiza jeszcze trwa. Wynik będzie dostępny po jej zakończeniu." while `RUNNING`, because an empty list would read as "no results" (E2.3).
  - Every detected address is validated with `AddressValidator` at read time: `network`, `valid`, `validationFailure`; `address` is the raw text as found (a source fact).
  - Report: Attributions sorted by Confidence Level desc, then network and address; `justification` = the stored Mentions of the candidate; `scoringRules` = applied rules; `classificationStatus` from the orchestrator; `addressActivity = null` (E4 on hold). Excluded Addresses separately with reason, classification source and their Mentions. `limitations` as stored. A `FAILED` Analysis gets a report with status `FAILED` and its limitations, never presented as zero findings by the backend.
- `GET /cases/{caseId}/analyses` keeps returning `[]` for an unknown Case (D25-003): specs and the recorder still accept analyses of any `caseId`.

## Consequences and risks
- No contract change (signatures existed), no `api-sync`.
- Rejected candidates are visible only through the Mentions endpoint. Address Activity has no limitation code yet (`BLOCKCHAIN_DATA_UNAVAILABLE` is unused while E4 is on hold).

## Verification
`AnalysisReportSpec` (sorting, justification, rules, flags, exclusions, validated addresses, failed and empty Analyses, 409/404, reopening changes nothing); `AnalysisFlowSpec` (real demo data, Case list/details).

## Related
- Files: `service/AnalysisReportService.kt`, `service/CaseService.kt`, `controller/*`, `repository/AnalysisRepository.kt`. Decisions: ADR 0001, D25-002, D25-003, D22-007. Diagram: `10-report-and-history.puml`.

## Reconsider when
E5 agrees the grouping and content of a list item, or E4 delivers Address Activity.
