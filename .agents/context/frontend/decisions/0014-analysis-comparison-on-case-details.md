# ADR-0014: Comparison with the previous Analysis on Case details

**Status:** Proposed (E6-D2, the comparison categories, is still open for the team; the backend side is D26-001 to D26-004)

**Context:**
E6.3 requires comparing two Analyses of one Case, and E6.4 requires a Scheduled Analysis to be compared through the same flow as a manual one. The backend has served `GET /cases/{caseId}/comparison?earlierAnalysisId&laterAnalysisId` since #26, but Case details only let the Analyst switch between reports (views.md, deferred point D2). After "Rozpocznij nową analizę", or when a Scheduled Analysis arrives, the Analyst could not see what changed without reading two reports side by side.

**Decision:**
- **Which pair:** the displayed Analysis is compared with the newest Analysis of the Case that started strictly before it and ended `COMPLETED` or `PARTIAL` (`previousAnalysisWithResults`). A `FAILED` earlier Analysis is skipped, because against it every Attribution would read as new. The trigger is ignored, so manual and scheduled Analyses work the same way. There is no picker: to compare another pair, the Analyst selects another Analysis in "Wyświetlana analiza".
- **When:** only when the displayed Analysis has ended with results. While it runs, or when it failed, nothing is shown and nothing is fetched (the report renders the `comparison` slot only in its ended, non-failed branch). The first Analysis of a Case shows nothing.
- **Where:** a section "Zmiany od poprzedniej analizy (N)" in the report, between the summary and "Powiązania", always expanded, so it is what the Analyst sees after the loader ends.
- **Content:** "Porównanie z analizą: {date} · {trigger} · {status}." (the history select's own wording) with the link "Pokaż poprzednią analizę" to the earlier report, where its Mentions are. Then the comparability limitations as a warning ("Ograniczenia porównania", one sentence per code), then one row per change: address and network, an icon plus a label ("Nowe powiązanie", "Zmieniony stopień pewności", "Zmienione wykluczenie", "Nie znaleziono w tej analizie") and both values as plain text. Rows are ordered new, changed level, changed exclusion, not observed, keeping the backend order (network, address) within a kind. No changes: "Nie ma zmian w powiązaniach ani w adresach wykluczonych."
- **Not observed:** every such row says "Nie oznacza to, że adres przestał należeć do osoby." When `absenceVerifiable` is not `true`, it adds a warning that the absence cannot be confirmed. A changed score is stated as two numbers, never as a change in behaviour.
- **Caching:** both Analyses have ended and never change, so the comparison is cached for the session (`staleTime: Infinity`). A failed read shows the standard error block inside the section and never hides the report.

**Consequences:**
- **Positive:** E6.3 and the comparison part of E6.4 are visible without a new view or route. The labels and their order are pure logic with Vitest tests (`features/analysis-comparison/comparison-view-model.test.ts`).
- **Negative:** Only consecutive Analyses with results can be compared in the UI, not an arbitrary pair, although the API allows one. Evidence of both sides is reached through the two reports, not shown inside the row (the response carries only Mention ids).
- **Demo:** the synthetic fixture is static, so a rerun of a demo Case shows "Nie ma zmian…" plus the limitations. Showing a real change in the film needs fixture data that differs between runs.
