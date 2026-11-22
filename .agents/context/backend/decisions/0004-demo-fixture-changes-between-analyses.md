# 0004. A demo scenario whose results change between Analyses of one Case

**Status:** Proposed (Maksym U., 2026-11-22) · Extends the synthetic fixture of ADR 0003 and D9-008

## Context

The comparison of two Analyses (E6.3, `GET /cases/{caseId}/comparison`, frontend ADR-0014) has nothing to show in the demo: the synthetic source returned the same fixed list on every call, so a rerun or a Scheduled Analysis of a demo Case always gave "no changes". `demo-scenarios.md` listed "two Analyses with a real change" as not covered. A Case cannot be created twice (duplicate rule), so a demo Case that is rehearsed once keeps its history on production.

## Options considered

1. **A one-way change** (the 2nd Analysis sees a newer web, later ones the same). Used up after one rehearsal: the film would show "no changes".
2. **Time-based visibility** (Mentions that appear some minutes after the Case was created). Depends on the clock and the film's timing; hard to freeze in specs.
3. **Two states that alternate per Analysis of the Case.** Deterministic, survives restarts and rehearsals: every rerun differs from the one before.

## Decision

Option 3.

- `AnalysisRequest.earlierAnalyses`: the number of Analyses of the Case started before this one, whatever their trigger or outcome. `AnalysisRerunService` counts it (`AnalysisRepository.countByCaseId`) under the same lock as the "already running" check, so manual and scheduled starts get it alike.
- `MentionSearch.search(identifiers, earlierAnalyses)` passes it to every source through `MentionSource.findMentions(identifiers, earlierAnalyses)`, a default method that real sources (GitHub, Bitcointalk) do not override: they answer with the web as it is.
- `SyntheticMentionSource` filters fixture Mentions by the optional `visibleIn`: `ODD_ANALYSES` (1st, 3rd, …) or `EVEN_ANALYSES` (2nd, 4th, …); without it a Mention is seen by every Analysis, so S1–S10 do not change.
- Scenario **S11** (`oliwer_zmienny`, category `CHANGE_OVER_TIME`), three BTC addresses (BTC only: an unlisted BTC address needs no network check, so the result is the same everywhere):
  - odd Analyses: savings address 40% (forum discussion), sale-ad address 60%;
  - even Analyses: savings address 95% (own profile + the forum post as an independent source), the ad is gone, a new own-profile address 80% with an outgoing transfer to an exchange.
  - Every rerun gives a new Attribution, a changed Confidence Level and one not observed. With the production setting (no `synthetic-broken`) both Analyses are `COMPLETED`, so the absence is shown as verifiable; locally it is partial and unverifiable.
- The addresses are keyless: the payload is the first 20 bytes of SHA-256 of `olcys/demo-v1/scenario/S11/<forum|sale|new>`, not a public-key hash. Transaction ids are SHA-256 of `olcys/demo-v1/s11/...` (D23-007). All three have synthetic Address Activity, so no Analysis turns partial for missing chain data.

## Consequences

- The scoring invariant still holds: the same Mentions always give the same Confidence Level. Only the set of Mentions the source returns depends on the position of the Analysis.
- The flip is fictional on purpose and labelled: the source is `synthetic` ("Dane testowe"). A third Analysis restores the first state, which a real web would rarely do.
- A failed or skipped-and-recorded Analysis still counts, so it shifts the parity.
- `MentionSearch` has one method with two parameters: test stubs use `search(_, _)`.

## Verification

`DemoScenariosSpec` (S11: three Analyses through HTTP, levels, cash-out signal, comparison both ways), `SyntheticMentionSourceSpec` (parity), `SearchServiceSpec` and `AnalysisOrchestratorSpec` (the position reaches the sources), `AnalysisRerunSpec` (count per Case, any trigger or outcome).
