# DJ1-001: A decision model takes part in the Confidence Level; its answers are not reused between Analyses

- **Status:** accepted (design; no code yet)
- **Date:** 2026-11-22
- **Type:** architecture decision; changes earlier team decisions
- **Source:** grilling session of 2026-11-22 (Q1–Q21); challenge text KROK 2, KROK 4, 5.6, 5.7, section 8; `../../jev-stories.md`
- **Proposed by:** AI agent (Claude)
- **Approved by:** Marcin (owner of the algorithm, #9)

## Problem and context
Algorithm `confidence-1` reads the text of a Mention with fixed keyword lists. This gives false positives (another user's comment on the profile of the investigated person scores 80) and false negatives (a change of address worded like a warning scores 0). Judging gives 25% to the quality of the Confidence Level and the rejection of false positives. Until now the team rule was: scoring is a pure function free of AI, "same Mentions -> same Confidence Level" (DS1-003, D9-001 to D9-008).

## Options considered
1. Keep rules only and grow the keyword lists.
2. A chat model that returns a category, a quote and a grade.
3. A decision model (TypeSafe "Jev") that answers typed questions with probabilities; code turns the answers into points.

For the answers of the model:
- a. Save an answer once per Premise and reuse it in later Analyses.
- b. Ask again in every Analysis and save the answers in the record of that Analysis.

## Decision
Option 3 with b.

- **Boundary:** code answers "whose?" (address detection and validation, Excluded Addresses, the link to the investigated person, independence of sources, arithmetic of points). The model answers "what does this text say about the address?" (kind of post, declaration of ownership, warning). The model never outputs the Confidence Level and never creates a link to the person that code did not find.
- **Privacy:** the model gets the text window of a Premise and neutral facts computed by code. It never gets the full name, e-mail or messenger ID of the investigated person.
- **Thresholds and points stay in code.** The Justification is still built from templates and is not written by a model.
- **No reuse:** every Analysis asks the model again. The answers, the pinned model version and the version of the question set are saved in the record of that Analysis.
- **Off by default:** the model is switched on by configuration together with an API key. When it is off or does not answer, the keyword rules are used and the report gets a Report Limitation.

## Consequences and risks
- The computation of points stays a pure function of the saved Premise assessments, so one saved Analysis can always be recomputed. The old rule "same Mentions -> same Confidence Level" no longer holds between two Analyses: the maker reports that a repeated request changes the decision in about 4% of cases (not re-checked).
- The comparison of Analyses must mark such a difference as "model assessment changed, no new Mentions", or the Analyst will read it as a change on the internet.
- Option a would have kept the old rule. It was rejected by the lead.
- The model is closed and hosted by one provider outside the target environment. Challenge 5.7 forbids sending data to services that are not approved; approval by the mentors is an open item of the lead. The port and the configurable base URL and model keep a local server of the same protocol possible (5.6); thresholds must be tuned again after such a change.
- A new external dependency and, possibly, an upgrade of Spring Boot.

## Verification
The golden set built from `../../jev-stories.md` is scored by `confidence-1`, by the fixed rules (`confidence-2`) and by the rules with the model (`confidence-3`); the three results are compared.

## Related
- Spec: `.agents/specs/2026-11-22_confidence-premises-jev/spec.md`. Stories: `../../jev-stories.md`. Glossary: Premise, Justification, Scoring breakdown in `.agents/context/domain/BUSINESS-CONTEXT.md`.
- Changes: DS1-003 ("scoring stays a pure function … same Mentions -> same Confidence Level"), D9-003 (source kind inference by keywords becomes the fallback), D9-006 (Justification wording in the glossary).

## Reconsider when
- The mentors do not approve the external service: keep the model off and show `confidence-2`.
- The golden set shows no gain of `confidence-3` over `confidence-2`.
- The run-to-run changes confuse the Analyst in the comparison of Analyses: go back to option a.
