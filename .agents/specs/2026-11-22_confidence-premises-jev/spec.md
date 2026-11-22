# Spec: Premises and the decision model in the Confidence Level

**Date:** 2026-11-22 · **Branch:** to be created by the lead from `master` (proposed `feature/BACKEND-confidence-premises`, plus a separate time-boxed branch for the Spring Boot 4.2 attempt, see D12) · **Status:** decisions D1–D19 made by the lead in the grilling of 2026-11-22 (rounds Q1–Q21); the test seams and the proposals P1–P28 below wait for the lead's review

This spec records the decisions of the grilling of 2026-11-22 for a redesign of the Confidence Level (PL: Stopień pewności) algorithm. The unit of assessment becomes the Premise (PL: Przesłanka). Code fixes the known errors of the rules, and an optional decision model reads what the text says about an address. It follows the `to-spec` template and is saved as a file instead of a tracker issue, on the lead's request. The work is split into three tasks in `tasks/` (01, 02, 03), written from this spec.

Sources this spec is built on, and which win when they differ from it:

- `.agents/context/backend/jev-stories.md`: the agreed scenarios S1–S6 and the worked example P1–P3. They are the base of the golden set and win on expected outcomes. They do not win on thresholds: see Further Notes for one inconsistency in S4.
- `.agents/context/backend/decisions/jev-1/001-decision-model-in-confidence-level.md` (DJ1-001): the ADR for D19. It is already written and accepted. This spec only references it.
- `.agents/context/domain/BUSINESS-CONTEXT.md`: the glossary in force. Premise was added on 2026-11-22, and Justification and Scoring breakdown were reworded.
- `.agents/context/domain/hacknation-2026-task/MF - Pełna Treść Zadania Konkursowego.md`: the challenge text. KROK 2 (bands, +15), KROK 3, KROK 4 (AI only supporting; "źródła, przesłanki i reguły punktacji" must be shown), 5.6 (easy change of the AI model), 5.7 and section 9 (no data to services that are not approved), section 8 (25% for Confidence Level quality and rejection of false attributions).
- `.agents/context/backend/decisions/task-9/` (D9-001 to D9-008), `sync-1/003-provisional-confidence-scoring.md`, `sync-1/007-known-gaps.md`: the team decisions this spec changes (see "Team decisions changed by this spec").
- `C:\Users\marci\.agents\handoffs\cryptofoot-confidence-jev-scoring.md`: the earlier research. Its facts about Jev, OpenRouter and Spring AI were not re-checked. This spec marks them "unchecked (handoff)". Each must be checked against a live source before implementation. The open questions listed there (Q6, Q8, Q11–Q15) are replaced by D1–D19.
- `.agents/context/backend/backend-architecture.md`, `.agents/rules/rules.md`, `.agents/rules/frontend.md`: how the code is written. They win on code rules.
- Code read on 2026-11-22 to describe the current state: `ConfidenceScoringService.kt`, `analysis/AnalysisContracts.kt`, `analysis/SearchService.kt`, `analysis/AnalysisOrchestrator.kt`, `service/AnalysisComparer.kt`, `dto/Responses.kt`, `model/Mention.kt`, `model/AnalysisRecord.kt`, `resources/fixtures/demo-v1.json`; specs `ConfidenceScoringServiceSpec`, `AnalysisFlowSpec`, `StubChainServer`; frontend `features/analysis-report/` (`report-view-model.ts` and its test, `report-labels.ts`, `attribution-details.tsx`).

## Problem Statement

The Analyst (PL: Analityk) gets a Confidence Level for every Attribution. Today it comes from algorithm `confidence-1`, which reads a Mention with fixed keyword lists. The algorithm makes mistakes in both directions, and some of them always happen in the same situations:

- **False negatives.** A post by the investigated person that does not repeat their nickname scores 0. The search accepts the Mention because its author is the person, but scoring never reads the author (S1). A change of address worded like a warning ("Nie wysyłajcie już na stary adres, mój nowy to …") scores 0 because the sentence contains "nie wysyłaj" (S3).
- **False positives.** Every page under the person's profile scores 80 for "own profile", also another person's comment there (S2). The nickname is matched as a bare substring, so `max` matches "maximum". A mass comment on a second service earns the +15 bonus for independent sources. An address whose exchange check was unavailable has no cap, so an unlisted exchange address on the profile can show 80% or more.
- **Not explainable per address.** The text window is 200 characters around the first match of an identifier, and addresses are searched only inside it. An address further away is never found (known gap DS1-007 G1). The per-Mention-per-address assessment exists only inside the algorithm and is thrown away. A scoring rule does not say which Mention made it apply. The challenge, however, asks the application to show "źródła, przesłanki i reguły punktacji" (KROK 4).

Judging gives 25% to the quality of the Confidence Level and the rejection of false attributions (section 8). Keyword lists cannot tell a warning from a change of address, or the author's own address from a quoted one.

## Solution

Every cryptocurrency address in every Mention becomes a **Premise**: the address, a text window of about 1200 characters around it, the facts that code computed about it, and its assessment. A Mention with two addresses gives two Premises, and they may be assessed differently. Dismissed Premises stay in the report with their reason, so the Analyst sees what the algorithm rejected.

The work comes in three algorithm versions, each measured on the same golden set:

1. **Golden set:** 10–15 synthetic Mentions with a hand-assigned category and an expected Confidence Level range, built from `jev-stories.md`.
2. **`confidence-2`, rules only:** addresses are detected in the whole content of a page, and code computes the facts of each Premise. The known rule errors are fixed: the author counts, identifiers match whole words only, a profile page does not make another person's comment "own", a mass comment gives no bonus, and an unclassified address is capped at 59. Each scoring rule points to the Premises that made it apply.
3. **`confidence-3`, rules with a decision model:** for each Premise, the decision model TypeSafe "Jev" answers three typed questions: kind of post, declaration of ownership, warning. Code turns the answers into a status and points with fixed thresholds. The model never sees the full name, e-mail or messenger ID of the person, never decides whose address it is, and never outputs the Confidence Level. It is off by default. When it does not answer, the keyword rules take over and the report says so.

On Case details the Analyst sees, for each Attribution, its Premises with a status ("Zaliczona", "Odrzucona", "Do weryfikacji"), the reason of a dismissal, the highlighted address in the text, who assessed each part ("reguła" or "model"), and the model's answers as words with a percentage. The summary of the report shows the assessment mode. A new Report Limitation tells the Analyst when the model did not answer.

## User Stories

### Premises in the report

1. As an Analyst, I want each Attribution to list its Premises (one per Mention that contains the address), so that I see every place the Confidence Level rests on.
2. As an Analyst, I want each Premise to show its source name, link, author, collection date and the text around the address, so that I can check it without opening the page.
3. As an Analyst, I want the assessed address to be highlighted in the text of the Premise, so that I find it at once when the text contains several addresses.
4. As an Analyst, I want each Premise to have one status, "Zaliczona", "Odrzucona" or "Do weryfikacji", so that I know at a glance which places count.
5. As an Analyst, I want each Premise to show its kind of post and the points it gave, so that I can follow the Scoring breakdown back to the text.
6. As an Analyst, I want a dismissed Premise to stay visible with a reason from a short fixed list (warning, address declared by another person, no link to the person, e-mail of another person), so that I see that the algorithm rejected it and why.
7. As an Analyst, I want two addresses from the same post to be two separate Premises, so that a warning about an impostor's address does not lower or raise the score of the person's own address (worked example P1–P2).
8. As an Analyst, I want each line of the Scoring breakdown to name the Premises that made it apply, so that I can recompute the Confidence Level myself.
9. As an Analyst, I want the facts that code found about a Premise (author is the investigated person, page under the person's profile, which identifiers occur, distance between identifier and address, e-mail of another person, number of addresses in the post, service), so that I can check the reasoning without trusting a model.
10. As an Analyst, I want each element of a Premise labelled "reguła" or "model", so that I know which part a fixed rule decided and which part the model only suggested.

### Fewer false positives

11. As an Analyst, I want a comment by another person on the investigated person's profile not to count as the person's own address, so that I do not chase someone else's wallet (S2).
12. As an Analyst, I want a nickname to match only as a whole word, so that the nickname `max` is not found in "maximum".
13. As an Analyst, I want a mass comment (giveaway, contest) on a second service not to raise the Confidence Level, so that spam does not look like independent confirmation.
14. As an Analyst, I want an address whose exchange check was unavailable to stay below 60%, so that an unchecked exchange address never looks very likely.
15. As an Analyst, I want an address that the text presents as somebody else's to be dismissed with the reason "address declared by another person", so that a quoted address does not become a lead.
16. As an Analyst, I want an address that the text warns against to be dismissed with the reason "warning", so that an impostor's address does not become a lead (P2).

### Fewer false negatives

17. As an Analyst, I want a post written by the investigated person to link the address to them even when the post does not repeat their nickname, so that their own sale ad is not lost (S1).
18. As an Analyst, I want a change of address ("mój nowy to …") to count for the new address even though the post also says "do not send", so that the current address of the person is found (S3).
19. As an Analyst, I want an address far from the identifier in a long post to be detected, so that it is not missed only because it was outside a short window (DS1-007 G1).
20. As an Analyst, I want to be told when the text says an address used to belong to the person and is no longer used by them ("Adres wycofany przez autora"), so that I know it was once their address (D10).

### Uncertainty and manual verification

21. As an Analyst, I want a Premise on which the model was unsure to be counted as a discussion and marked "Do weryfikacji", so that an unsure reading never gives a high score and I know where to look (S4).
22. As an Analyst, I want a Premise where code found no link to the person but the text looks like a declaration to get 0 points and the mark "Do weryfikacji", so that the model never links an address to the person by itself (S5).
23. As an Analyst, I want the model's answers shown as "tak", "nie" or "niepewne" with the probability as a percentage, so that I understand them without technical knowledge.
24. As an Analyst, I want the raw numbers in a collapsed "szczegóły techniczne" section, so that they are available but do not clutter the report.

### Model on, off, or not answering

25. As an Analyst, I want the summary of the report to show the assessment mode ("z modelem" or "tylko reguły"), so that I know how this report was made.
26. As an Analyst, I want a Report Limitation when the model did not answer for some or all Premises, so that I know those Premises were assessed by rules only (S6).
27. As an Analyst, I want the application to work fully without the model, so that the report is never blocked by an external service.
28. As an Analyst, I want the Justification to stay a fixed template text, not written by a model, so that the same Premise assessments always give the same words.
29. As an Analyst, I want the comparison of two Analyses to mark a changed Confidence Level with no new Mentions as "model assessment changed, no new Mentions", so that I do not read it as a change on the internet (D7).

### Developer, juror and auditor

30. As a developer, I want a golden set of synthetic Mentions with expected Confidence Level ranges, so that every rule change is measured before it ships.
31. As a juror, I want to see the same golden set scored by `confidence-1`, `confidence-2` and `confidence-3`, so that the gain on false positives and false negatives is shown with numbers.
32. As an auditor, I want every Analysis to store the algorithm version, the pinned model version, the question-set version, the assessment mode and every raw model answer, so that a report can be explained and recomputed later.
33. As an auditor, I want the model to receive only the text window and neutral facts, never the full name, e-mail or messenger ID of the person, so that personal data from the Case does not leave the application.
34. As a developer, I want the model behind a port with the base URL and the model name in configuration, so that another server that speaks the same protocol (e.g. a local one) can be plugged in (5.6).
35. As a developer, I want the model switched off by default and switched on only with configuration and an API key from an environment variable, so that nothing is sent outside by accident (5.7).
36. As a person presenting the demo, I want the old and the new Confidence Level of the same golden-set Mentions side by side, so that the film shows the false positives and false negatives that were fixed.

## Implementation Decisions

### Decisions of the lead (D1–D20)

| # | Decision |
|---|---|
| D1 | The unit of assessment is the Premise: one cryptocurrency address in one Mention, stored with its assessment. A Mention with two addresses gives two Premises that may be assessed differently. Dismissed Premises stay visible to the Analyst. |
| D2 | Addresses are detected in the whole content of a source page. Each Premise stores a text window of about 1200 characters centred on the address. This closes DS1-007 G1. The Mention identity hash changes, so old Mentions will not deduplicate with new ones, and the API contract changes. |
| D3 | Code computes these facts for each Premise, deterministically and without a second model summarising text: (1) the author is the investigated person; (2) the URL is under the person's profile; (3) which identifier types occur in the text, matched with word boundaries; (4) distance in characters between identifier and address; (5) an e-mail of another person is in the text; (6) number of addresses in the Mention; (7) service (host) and source name. |
| D4 | Code answers "whose?": address detection and validation, Excluded Addresses, link to the person (identifier, author, profile), independence of sources, arithmetic of points. The model answers "what does this text say about the address?": kind of post, declaration of ownership, warning. The model never receives the full name, e-mail or messenger ID and is never asked "is this person X". It gets the text window and neutral facts. Keyword lists stay only as the fallback. |
| D5 | The model is TypeSafe "Jev", a decision model that takes a state plus typed questions and returns only numbers and labels, no text (unchecked (handoff)). It never outputs the Confidence Level. |
| D6 | False positive: a high Confidence Level although the text does not say the address belongs to the person (quote of someone else's address, warning, another person's comment on the person's profile). False negative: a low Confidence Level although the text clearly declares ownership. The model may move the kind of a Premise up or down. It never creates a link to the person that code did not find. When code sees no link and the model sees a declaration, the Premise gets 0 points and "Do weryfikacji". |
| D7 | Each Analysis asks the model again. Answers are saved in that Analysis' own record and are not reused (no cache). Two Analyses of the same Mentions may differ. The comparison marks this as "model assessment changed, no new Mentions". |
| D8 | One request per Premise with three questions, each answer saved separately: `rodzaj_wpisu` (choice: own declaration / sale or settlement / discussion / mass comment), `deklaracja` (yes/no: the author of the text presents the assessed address as their own; this is about the author of the text, not the investigated person), `ostrzezenie` (yes/no: the text tells readers not to use or trust the assessed address). |
| D9 | Starting rule table, to be tuned on the golden set. Thresholds in code: 0.7 or more is yes, 0.3 or less is no, between is undetermined. No link to the person gives 0. Warning yes and declaration not yes: Premise dismissed. Declaration yes and the author is not the investigated person: Premise dismissed ("address declared by another person"). Base = strongest kind: own profile or own declaration 80, sale/settlement 60, discussion 40, mass comment 10. Undetermined: counted as discussion with "Do weryfikacji". +15 only when two Premises from different services are both at least "discussion". An unclassified address is capped at 59. |
| D10 | Declaration yes AND warning yes AND the author is the investigated person (a withdrawn own address): the Analyst must be told that the address used to belong to the person and no longer does. Handling: P14. |
| D11 | Order: golden set first, then the deterministic fixes as `confidence-2`, then the model as `confidence-3`. Each is measured on the golden set. The old-vs-new comparison is demo material. |
| D12 | Calling the model: on a separate branch, try upgrading the backend to Spring Boot 4.2 and using the Spring AI System One module, time-boxed to one hour. Per the handoff, `spring-ai-starter-model-systemone` 2.1.0 needs Boot 4.2 (unchecked (handoff)). If the checks do not pass, write a thin own client on the JDK HTTP client, like the existing chain client. Either way the model sits behind an own port, with the base URL and the model name in configuration. Versions are checked against a live source before the attempt. The System One contract (OpenAPI) is read before the first request is sent. **Partly replaced by D20:** the time-boxed attempt is now the chosen path. |
| D13 | The model is off by default and is switched on by configuration together with an API key from an environment variable. Without a key the application works on rules alone. If the model does not answer, the keyword rules take over and a Report Limitation is added. The lead presents the model as the innovation for handling false positives and false negatives. Mentor approval of the external service (5.7) is the lead's open item. |
| D14 | Each Attribution in the API gets a list `premises`. Each Premise has: its Mention (source, URL, author, date, text window) and the position of the address in the window; the status; the kind of post and its points; the dismissal reason from a closed list (warning, address declared by another person, no link to the person, e-mail of another person); the code facts of D3; the model's answers to the three questions. The existing `mentions` field stays unchanged. Contract change, so `./scripts/api-sync.sh` is required. |
| D15 | Model answers on screen: "tak / nie / niepewne" with the probability as a percentage. Raw numbers also in the API and in a collapsed "szczegóły techniczne" section. |
| D16 | Every element of a Premise shows who assessed it: "reguła" or "model". |
| D17 | The Justification is still built from templates and never written by a model. The glossary sentence "the same Premise assessments always give the same text" holds (already in the glossary). Each rule in the Scoring breakdown points to the Premises that triggered it. |
| D18 | Saved and shown once per Analysis: algorithm version (exists), pinned model version, question-set version, assessment mode ("z modelem" / "tylko reguły"), and a new Report Limitation when the model did not answer for some or all Premises. |
| D19 | One ADR records (a) that a model takes part in the computation although an earlier team decision said scoring is free of AI, and (b) that answers are not reused between Analyses. It exists as DJ1-001. |
| D20 | (2026-11-22, after the research below.) The backend uses Spring AI 2.1.0 GA and its Decision Model API, so the solution stays extensible: the Premise assessment adapter depends on the provider-neutral `DecisionModel` interface, not on the System One classes. Jev on OpenRouter is the first provider (`spring-ai-starter-model-systemone`, `base-url` `https://openrouter.ai/api`). Another decision model (Ollama, OpenAI, another System One server) is a change of starter and configuration, not of the adapter. This needs Spring Boot 4.2.0. Open for the lead: Q-D20a, Q-D20b in the recommendation section. |

### Calling Jev through OpenRouter with Spring AI (recommendation, 2026-11-22)

Research done after the grilling. It checks part of the "unchecked (handoff)" list for D12. It does not change D12: the lead decides.

**Checked facts**

| Fact | Source |
|---|---|
| Jev is not a chat model and is not served on `/api/v1/chat/completions`. It answers typed questions (`noul`, `choice`, `score`, `box`) with probabilities and no text. | checked: [OpenRouter Decisions API](https://openrouter.ai/docs/guides/features/decisions), [Jev Documentation](https://openrouter.ai/docs/guides/community/jev) |
| Because of that, the Spring AI OpenAI starter (`spring-ai-starter-model-openai`, `spring.ai.openai.base-url`) does **not** fit Jev. It only fits chat models on OpenRouter. | checked: [Spring AI 2.1.0 OpenAI Chat](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html) |
| OpenRouter serves Jev on two endpoints: `POST /api/alpha/decisions` (own Decisions API) and `POST /api/v1/systemone` ("Compatible with the TypeSafe SDKs"). Auth: `Authorization: Bearer <OPENROUTER_API_KEY>`. No TypeSafe account is needed. | checked: [Decisions API](https://openrouter.ai/docs/guides/features/decisions), [Submit a System One request](https://openrouter.ai/docs/api/api-reference/systemone/submit-a-system-one-request) |
| A System One client switches to OpenRouter by changing its base URL to `https://openrouter.ai/api` and its key. The client appends `/v1/systemone`. | checked: [OpenRouter TypeSafe SDK guide](https://openrouter.ai/docs/guides/community/typesafe-sdk) |
| Model ids: `typesafe/jev-1.14` (current, has `box`), `typesafe/jev-1.13`, alias `~typesafe/jev-latest`. On `/api/v1/systemone`, bare ids (`jev-1.13`, `jev-latest`) map to the `typesafe/` namespace. The response echoes a dated id (for example `typesafe/jev-1.14-20261014`). | checked: [Jev Documentation](https://openrouter.ai/docs/guides/community/jev), [Submit a System One request](https://openrouter.ai/docs/api/api-reference/systemone/submit-a-system-one-request) |
| Answer shapes: `noul` → `{noul, confidence}` (`noul` = probability of yes, 0.0–1.0); `choice` → `{choice, probabilities, confidence}`; `refusal` or a missing key means "not answered", never "no". Question keys: use only `A-Z a-z 0-9 _ -`. `usage.cost` holds the exact USD charged; output tokens are free. | checked: [Decisions API](https://openrouter.ai/docs/guides/features/decisions) |
| Spring AI 2.1.0 GA (2026-11-20) has the System One client: `spring-ai-systemone` and the starter `org.springframework.ai:spring-ai-starter-model-systemone`, properties under `spring.ai.systemone`. "Spring AI 2.1 is built on Spring Boot 4.2, so upgrade both together." Spring AI TypeSafe is being archived in favour of the `DecisionModel` API. | checked: [Spring AI 2.1.0 GA blog](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-GA-available-now/) |
| The starter 2.1.0 is the only version on Maven Central. Its POM depends on `spring-boot-starter-restclient:4.2.0`. Spring Boot 4.2.0 GA is on Maven Central (uploaded 2026-11-19). | checked: [Maven Central starter 2.1.0 POM](https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-starter-model-systemone/2.1.0/spring-ai-starter-model-systemone-2.1.0.pom), [Maven Central spring-boot](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/) |
| Properties: `spring.ai.systemone.api-key`, `base-url` (default `https://api.typesafe.ai`, "Point it at Laya, llmman or another server of the protocol"), `timeout`, `retry.*` (default 2 retries), `decision.model` (default `jev-latest`). No path override; the path `/v1/systemone` is fixed. Java API: `SystemOneDecisionModel`, `DecisionPrompt`, `YesNoQuestion`, `ChoiceQuestion`, answers via `getYesNo(..)`, `getChoice(..)`, `probabilityOf(..)`. | checked, **but from the 2.1-SNAPSHOT page**: [Spring AI System One (2.1-SNAPSHOT)](https://docs.spring.io/spring-ai/reference/2.1-SNAPSHOT/integrations/models/systemone.html). The stable 2.1.0 page (`api/decision/systemone-decision.html`) was not read. |
| Spring AI 2.1.0 Decision Model API: one `DecisionModel` bean, `call(DecisionPrompt)` → `DecisionResponse`. Implementations: System One (TypeSafe, Laya, llmman, Ollama), Ollama, OpenAI. All support yes/no, choice and score questions; System One takes text only. Inputs: `DecisionInput.text(..)` or `DecisionInput.fields(Map)`. Answers: `getYesNo(name).getProbability()`, `getChoice(name).getValue()` / `getProbability(option)` / `getConfidence()`, `isRefused(name)`. Per-call `DecisionOptions.model(..)`. Observed as `gen_ai.client.operation` "decision". The pages do not show the interface package, a Spring Boot version or mock guidance. | checked: [Spring AI 2.1.0 Decision Model](https://docs.spring.io/spring-ai/reference/api/decision.html), [Spring AI 2.1.0 System One](https://docs.spring.io/spring-ai/reference/api/decision/systemone-decision.html) |

**Still unchecked**

- That `spring.ai.systemone.base-url=https://openrouter.ai/api` really reaches `https://openrouter.ai/api/v1/systemone`. It follows from the two docs above, but nobody has tested it.
- How `/api/v1/systemone` differs from `/api/alpha/decisions`, besides SDK compatibility.
- Whether `seed` or `temperature` exist, run-to-run change, Polish language quality, and whether `choice` favours the first option (all still "unchecked (handoff)").
- The full OpenAPI contract: the API reference points to `/openapi/openapi.yaml` (presumably `https://openrouter.ai/openapi/openapi.yaml`), which was not read. Read it before the first request (task 02).

**Recommendation**

1. **Decided as D20.** Spring AI 2.1.0 + `spring-ai-starter-model-systemone` on Spring Boot 4.2.0, both GA. The adapter codes against `DecisionModel`, `DecisionPrompt` and `DecisionResponse`; only configuration names System One.
2. Point it at OpenRouter by configuration only, not by code:
   ```yaml
   spring:
     ai:
       model:
         decision: none            # off by default (D13); `systemone` when switched on
       systemone:
         base-url: https://openrouter.ai/api
         api-key: ${OPENROUTER_API_KEY:}
         decision:
           model: typesafe/jev-1.14   # pinned, never the default jev-latest (D18)
   ```
   checked: property names and defaults are the same on the stable [Spring AI 2.1.0 System One page](https://docs.spring.io/spring-ai/reference/api/decision/systemone-decision.html). Any value of `spring.ai.model.decision` other than `systemone` disables the bean.
3. Do not use the OpenAI starter or `/api/alpha/decisions` for Jev. `alpha` means the contract may change, and the Spring AI client speaks `/v1/systemone`.
4. Keep the own port (`PremiseAssessment`, domain words) above `DecisionModel`. The port speaks Premises and the three answers; the adapter maps them to `DecisionPrompt` (state as `DecisionInput.fields(LinkedHashMap)`, `YesNoQuestion` for `deklaracja` and `ostrzezenie`, `ChoiceQuestion` for `rodzaj_wpisu`) and back. Scoring never sees Spring AI types.
   - Read yes/no with `getProbability()` and apply the D9 thresholds in our code. Do not use `isYes()`: its threshold is 0.5 and it hides "undetermined".
   - "No answer" in the adapter: a refused question (`isRefused`), an `IllegalStateException` from a typed getter, a missing answer, a timeout, or a failed call after the Spring AI retries. Spring AI says System One "does not support refusals", but OpenRouter documents a `refusal` answer type, so handle both.
   - Retries: `spring.ai.systemone.retry.max-retries` defaults to 2. Decide whether retries count toward the per-Premise timeout (P20).
5. Q-D20a (open for the lead): does the Spring Boot 4.1.2 → 4.2.0 upgrade go to `master` in its own PR before task 02, or inside task 02? Recommendation: its own PR first, green `./gradlew check integrationTest`, so a Boot regression is never mixed with the model work.
6. Q-D20b (open for the lead): is the JDK `HttpClient` fallback of D12 dropped? Recommendation: yes, drop it. The rules-only adapter already covers "model off or failing", and a second model client doubles the tests.
7. `StubAssessmentModelServer` serves `POST /v1/systemone`. The integration spec sets `spring.ai.systemone.base-url` to `http://localhost:<port>`, so the real Spring AI client is exercised against the stub. Spring AI gives no mock guidance (its own test is a live IT), so the stub is our seam.
8. Save the model id from the response metadata as the pinned model version (D18), if the provider reports it; otherwise save the configured value. Spring AI documents the response id (`getMetadata().getId()`, from `x-typesafe-request-id`) and token usage, but no cost field. Unchecked: whether OpenRouter sends that header and the dated model id through this client.

### Current state (verified in the code on 2026-11-22)

- The search keeps a 200-character window around the first identifier match. It detects addresses only inside that window and drops the rest of the content. The Mention identity is a hash of the source name, URL and that window.
- The search accepts a Mention whose author equals the nickname or login (provenance), but scoring never reads the author.
- Scoring matches identifiers with a bare substring check. Any URL under the profile address scores `OWN_PROFILE` 80. A `MASS_COMMENT` Mention on a second host earns `INDEPENDENT_SOURCES` +15. Unclassified candidates get only a 0-point note (D9-007).
- The per-Mention-per-address assessment is private to the algorithm and is discarded. An applied rule is only a rule code and a contribution.
- `ALGORITHM_VERSION = "confidence-1"`. No AI code or dependency exists. The backend is on Spring Boot 4.1.2.
- `ConfidenceScoring.score(ScoringInput)` is a pure function: no database, no clock, no external call (D9-001). The orchestrator wires search, preparation, exclusion and scoring, and builds the Report Limitations.

### Modules

Backend (tasks 01 and 02):

- **Search** (changed, 01): detects addresses in the whole content of a page. For each detected address it stores the text window and the position of the address in it (P2). The Mention identity is computed in a new way (P3). The existing short `context` stays for the `mentions` field (P3).
- **Premise facts** (new, pure, 01): one function from a Mention, an address and the Case identifiers to the code facts of D3. It is used by scoring, and in 02 to build the neutral state for the model. Identifier matching uses word boundaries (P4) and a distance limit (P5).
- **Confidence Level algorithm** (changed, 01 and 02): works per Premise. For each Premise it decides status, kind, points and dismissal reason, then sums per Attribution. It returns the Premises with each Attribution, and each applied rule carries the ids of its Premises (P10). It stays a pure function: the model answers come in as data in the scoring input (P17), so "same Premise assessments → same Confidence Level and same Justification" holds inside one Analysis.
- **Premise assessment port** (new, 02): one interface in the analysis contracts, "assess these Premises", returning per Premise either the three answers with raw probabilities, or "no answer". Two adapters: the model client (Spring AI `DecisionModel`, D20), and a rules-only adapter that answers "no answer" for every Premise, used when the model is off.
- **Orchestrator** (changed, 02): after exclusion and before scoring, it builds the Premises of the scorable candidates and asks the port (P17, P18). It passes the answers to scoring and adds the new Report Limitation on fallback (P19). It records the assessment mode and the versions (P21).
- **Analysis snapshot** (changed, 01 and 02): the Premises, their facts, statuses and model answers are stored inside the Analysis record, not on the shared Mention document. Model answers depend on the Analysis (D7), and facts depend on the Case identifiers.
- **Report and DTOs** (changed, 01 and 02): `premises` on each Attribution (D14). Premise ids on each Scoring breakdown entry (P10). Assessment mode, model version and question-set version on the Analysis. The new Report Limitation code. Then `./scripts/api-sync.sh`.
- **Comparison** (changed, 02): the "model assessment changed, no new Mentions" mark and a comparability limitation for a different model version (P23).
- **Configuration** (changed, 02): the switch, base URL, model and timeout, with the API key from an environment variable (P20). Placed with the other `app.*` settings, like the on-chain check switch.

Frontend (task 03):

- **Report view model** (changed, pure, the frontend seam): maps `premises` to a Premise view per Attribution. It decides the status label and order, the reason label, the kind label and points, the highlighted span of the address, the fact lines, the answer words with percentages, the "reguła"/"model" labels, the technical details, the assessment mode line in the summary, the new Report Limitation sentence, and the Premise names on each Punktacja line. Components still never read API types.
- **Report labels** (changed): Polish texts for statuses, reasons, kinds, answers, facts, mode, the new rule codes and the new Report Limitation (P25).
- **Attribution details** (changed): a new block "Przesłanki (N)" (P25); Punktacja lines show which Premises they come from.

### Rule table per version

| Step | `confidence-2` (rules only) | `confidence-3` (rules + model) |
|---|---|---|
| Link to the person | Code only: author is the person, or URL under the profile with the author being the person or unknown (P7), or a non-wallet, non-full-name identifier within the distance limit as a whole word (P4, P5). No link: 0, dismissed "no link to the person". | Same code link. No link and the model says declaration yes: 0 and "Do weryfikacji" (D6). |
| E-mail of another person | As today (D9-004), on the Premise window: dismissed "e-mail of another person". | Same, code only. |
| Warning | Keyword list on the sentence that holds the address (as today): dismissed "warning". | Model `ostrzezenie` yes and `deklaracja` not yes: dismissed "warning". Both yes and the author is the person: withdrawn address (P14). |
| Declared by another person | Not detectable without the model (see P7 for S2). | `deklaracja` yes and the author is known and is not the person: dismissed "address declared by another person". |
| Kind | Own profile (code), else keyword lists of D9-003 on the window: sale/settlement, mass comment, discussion. | Own profile (code) stays 80. Otherwise the model's `rodzaj_wpisu`. Undetermined answers: at most discussion and "Do weryfikacji" (P12, P13). |
| Base and bonus | Strongest kind among the counted Premises; +15 only for two Premises on different services, both at least discussion, with different address sentences (P8). | Same. |
| Unclassified address | Capped at 59 (P9). | Same. |

Thresholds and points live in code and are tuned on the golden set. Any change of a value, list, threshold or question wording is a new algorithm or question-set version.

### Specific interactions

- **Premise status:** "Zaliczona" when the Premise gives points without doubt. "Odrzucona" when a dismissal rule fired. "Do weryfikacji" when an answer was undetermined (it may still give at most 40 points) or when code found no link but the model saw a declaration (0 points).
- **Who assessed what (D16):** link facts, e-mail, independence, cap and points are always "reguła". Kind, declaration and warning are "model" when the model answered for that Premise, otherwise "reguła" (keyword fallback).
- **Model state (D4, P15, P16):** the masked text window, the assessed address, "author is the investigated person: tak / nie / nieznany", the number of addresses in the Mention, and the service host. Nothing else from the Case.
- **Fallback (D13, P19):** per Premise. A Premise without an answer is assessed by the `confidence-2` rules and labelled "reguła". The Analysis keeps its status. The Report Limitation is added once.
- **Logs:** counts, durations, status codes and request ids only. The text window and the state sent to the model are personal data and are never logged (`rules.md`).

### API contract (proposed shape, final names in tasks 01 and 02)

- `AttributionResponse.premises`: list of Premise entries, each with an id, the Mention fields (source name, URL, author, collection time), the text window, the start and length of the address in the window, status, kind, points, dismissal reason (or none), the "withdrawn by the author" note, the code facts, the three model answers (label or yes/no, raw probability, who assessed it), and the undetermined flags.
- `ScoringRuleContributionResponse.premiseIds`: the Premises that made the rule apply.
- `AnalysisResponse`: `assessmentMode`, `modelVersion`, `questionSetVersion` next to the existing `algorithmVersion`.
- `ReportLimitation`: one new code (P19).
- `AttributionResponse.mentions` and `MentionResponse` keep their shape. Their content changes: `detectedAddresses` now lists every address of the page, so more Rejected Addresses appear (in demo-v1, the malformed `…DivfN0` becomes visible).
- Old Analyses (`confidence-1`) read back with an empty `premises` list and no model fields. The frontend shows them as today.

### Team decisions changed by this spec

| Decision | Change |
|---|---|
| D9-001 | Amended. The scoring input carries Premises (with facts and optional model answers) instead of Mentions alone. Scoring stays pure, and the model is called before it (P17). |
| D9-002 | Amended. +15 needs both Premises to be at least discussion. A cap rule is added. |
| D9-003 | Amended for `confidence-2` (own profile needs the author to be the person or unknown; keywords read the Premise window). Replaced by the model's `rodzaj_wpisu` in `confidence-3`; the keyword lists become the fallback. |
| D9-004 | Superseded. Support is decided per Premise. The author counts as a link. Identifiers match whole words within a distance. Dismissal reasons are a closed list. |
| D9-005 | Amended. The copy rule (same address sentence) stays. The bonus requires both Premises to be at least discussion. Its "reconsider when a source returns full page text" applies now. |
| D9-006 | Amended. New rule codes and sentences. Each rule carries Premise ids. "Same Premise assessments → same text". |
| D9-007 | Superseded. An unclassified address is capped at 59. |
| D9-008 | Superseded by `confidence-2` and `confidence-3`. The demo-v1 table is frozen again (P27). |
| DS1-003 | Already superseded. Its principle "same Mentions → same Confidence Level" is replaced by DJ1-001. |
| DS1-007 G1 | Closed by D2. |
| D26 (comparison) | Amended by P23. |

### Proposals waiting for the lead's review (P1–P28)

Not asked in the grilling, or left open by a decision. They are proposed so the implementation is not blocked. Each can be changed before coding.

| # | Proposal |
|---|---|
| P1 | A Premise exists only for a valid address that reaches scoring (accepted or unclassified). A Rejected Address is not an address, and an Excluded Address keeps only its Mentions as today. Premise id = Mention id + canonical address. |
| P2 | Window = 600 characters on each side of the address (1200 plus the address). It is moved inward at the start or end of the content, so it keeps its length when the content allows. It is stored on the Mention per detected address, with the start and length of the address in the window. The model receives the same window (after masking, P15). |
| P3 | Mention identity = hash of the source name, URL and full content. The full content is not stored. `context` keeps today's 200-character excerpt, so `mentions` and the current quote on screen do not change. |
| P4 | Word boundary for nickname, login and messenger ID: no letter, digit or `_` directly before or after the match. Leading `@` ignored. Case- and diacritic-insensitive, as the keyword lists are. E-mail matched whole. Profile address keeps today's "URL under the profile" rule. |
| P5 | An identifier found in the text counts as a link only within 200 characters of the address (the old window). The distance is a fact either way. Reason: a 1200-character window in a long thread could otherwise link an address to a nickname from another post. Author and profile links do not depend on distance. To be tuned on the golden set (G14). |
| P6 | The fact "author is the investigated person" has three values: tak, nie, nieznany. It is compared with the nickname and login the way the search compares provenance today. A source without an author (synthetic) gives "nieznany". |
| P7 | A URL under the profile is a link, and gives own profile (80), only when the author is the person or unknown. With a known different author: in `confidence-2` there is no link (0, "no link to the person"), the conservative choice for S2. In `confidence-3` code records "profile page, another author" as a link fact and lets the model decide the reason: declaration yes gives "address declared by another person" (S2 as written); otherwise "no link to the person". Alternative for the lead: in `confidence-2`, treat it as discussion 40 with "Do weryfikacji". |
| P8 | The copy rule of D9-005 stays. Only Premises that give points take part in the bonus. "Do weryfikacji" Premises with points count, at their capped kind. |
| P9 | The cap is a new rule `CLASSIFICATION_CAP` whose contribution is negative (the amount cut to reach 59), so Punktacja still sums to the Confidence Level. `CLASSIFICATION_UNAVAILABLE` and its sentence stay. |
| P10 | Each applied rule carries the list of Premise ids that triggered it. 0-point notes (e.g. `SAME_SERVICE`, `NICKNAME_ONLY`) list their Premises too. `NO_PERSON_LINK` lists the dismissed Premises. |
| P11 | Code names: status `COUNTED` / `DISMISSED` / `FOR_REVIEW`; reason `WARNING` / `DECLARED_BY_ANOTHER_PERSON` / `NO_PERSON_LINK` / `CONFLICTING_EMAIL`; kind `OWN_PROFILE` / `OWN_DECLARATION` / `SALE_OR_SETTLEMENT` / `DISCUSSION` / `MASS_COMMENT`; assessed by `RULE` / `MODEL`; mode `WITH_MODEL` / `RULES_ONLY`. |
| P12 | `rodzaj_wpisu`: the top option counts when its probability is at least 0.5, otherwise the kind is undetermined. The 0.7/0.3 thresholds of D9 apply to the two yes/no questions. Both are tuned on the golden set. |
| P13 | Order of the rules for one Premise: no code link first, then e-mail of another person, then dismissals that rest on determined answers (warning, declared by another person), then the withdrawn address, then the kind. Any undetermined answer that is still relevant makes the Premise "Do weryfikacji", and it counts at most as discussion (40). A mass comment stays at 10. |
| P14 | Withdrawn address (D10): the Premise counts like an own declaration (80), status "Zaliczona", with the note "Adres wycofany przez autora" on the Premise and one Justification sentence ("Autor wpisu podał, że ten adres był jego, ale już go nie używa."). Whether the points are reduced is open for the lead. Without the model this case is not detectable (the keyword warning dismisses it). |
| P15 | Before sending, code replaces in the window the Case's e-mail, messenger ID and full name, and every other e-mail address, with neutral placeholders ("[e-mail]", "[komunikator]", "[imię i nazwisko]"). D4 lists only these three, so the nickname and login stay in the text: the lead should confirm. |
| P16 | The state sends the service host, not the full URL (a URL can hold the profile or the nickname). Keys in Polish, as in `jev-stories.md`: `tekst`, `oceniany_adres`, `autor_jest_badana_osoba`, `liczba_adresow_we_wpisie`, `serwis`. |
| P17 | The orchestrator calls the Premise assessment port between exclusion and scoring. The answers go into the scoring input as plain data, so the algorithm keeps "no external call" (D9-001). |
| P18 | Requests run in parallel on virtual threads, one per Premise, with a timeout of 5 s per request and no retry. Premises of Rejected and Excluded Addresses are not sent. |
| P19 | New Report Limitation `MODEL_ASSESSMENT_UNAVAILABLE`: "Model nie ocenił części lub wszystkich przesłanek; te przesłanki oceniono samymi regułami." It is added when at least one Premise fell back while the model was on. Not added when the model is off by configuration: the mode line says "tylko reguły". The Analysis status does not change. |
| P20 | Configuration `app.assessment.model.enabled` (default `false`), `base-url`, `model` (pinned snapshot), `timeout`, and the API key from the environment variable `ASSESSMENT_MODEL_API_KEY` (never in a file). Switched on without a key: one WARN at start-up, and the Analysis runs rules only. |
| P21 | Per Analysis: `algorithmVersion`, `assessmentMode`, `modelVersion` (as echoed by the server, else as configured), `questionSetVersion` (`premise-questions-1`). Per answer: the label or yes/no value, all raw probabilities (every option of the choice), and the request id when the server returns one. |
| P22 | `algorithmVersion` is `confidence-3` whenever the model is on, also on a full fallback, and `confidence-2` when it is off. The comparison then flags `ALGORITHM_VERSION_DIFFERS` between a rules-only and a model Analysis. |
| P23 | Comparison: a `CONFIDENCE_LEVEL_CHANGED` entry gets the flag `modelAssessmentChanged` when the Mention ids of both sides are equal and at least one Analysis used the model. Proposed PL copy: "Zmieniła się ocena modelu, brak nowych wzmianek". New comparability limitation `MODEL_VERSION_DIFFERS`. The frontend has no comparison screen yet, so this is API only. |
| P24 | New Justification sentences, one per new rule or situation (dismissed by warning, declared by another person, "Do weryfikacji", withdrawn address, cap). Plus one sentence when any Premise was assessed by the model: "Rodzaj wpisu i ostrzeżenia oceniono z pomocą modelu; punkty przyznały stałe reguły." The wording is written in tasks 01 and 02 and reviewed by the lead. No numbers, as before. |
| P25 | UI copy. Block "Przesłanki (N)" after "Uzasadnienie" and before "Wzmianki". Counted Premises first, then "Do weryfikacji", then dismissed. Statuses "Zaliczona", "Odrzucona", "Do weryfikacji" with icon and word, not colour alone. Reasons: "Ostrzeżenie przed tym adresem", "Adres podany przez inną osobę", "Brak powiązania z badaną osobą", "E-mail innej osoby". Kinds: "Profil badanej osoby", "Własna deklaracja", "Sprzedaż lub rozliczenie", "Dyskusja", "Wpis masowy". Answers: "Deklaracja: tak (92%)", "niepewne" for undetermined. Labels "Oceniono: reguła" / "Oceniono: model". Summary line "Tryb oceny: z modelem" / "tylko reguły". The address is highlighted with `<mark>`. |
| P26 | Golden set: a versioned JSON fixture in the backend test resources (`golden-set-v1`). Each item holds the Case identifiers, the Mentions (with author), a hand-assigned category, the expected band per algorithm version, and canned model answers. The measurement with the real model is a manual, opt-in run that needs the API key. It is not part of CI, and its result table is kept for the demo. |
| P27 | demo-v1 stays. Its scores under `confidence-2` are expected to stay 75 / 75 / 60, 95 / 95 / 60 and 0 / 0, plus one new Rejected Address (`…DivfN0`). This is to be confirmed when D9-008 is frozen again. Under `confidence-3` the values depend on the model. |
| P28 | Percentages are whole numbers, rounded half up. The yes/no word follows the thresholds, not the rounded number. |

### Golden set (base for P26, from `jev-stories.md`)

Case nickname `kryptomarek` unless stated. "c1" is the expected value of today's code, used only for the comparison. c3 values assume the canned answers given.

| # | From | Content (short) | Category | c1 | c2 | c3 (canned answers) |
|---|---|---|---|---|---|---|
| G1 | S1 | Post by `kryptomarek`, no nickname in the text: "Sprzedam koparkę, płatność na bc1qAAA" | false negative | 0 | 60 | 60 (sale, decl. 0.90, warn. 0.03) |
| G2 | S2 | Comment by `janek99` under the profile: "Mój adres na napiwki: 0xCCC" (Case has the profile address) | false positive | 80 | 0, no link | 0, declared by another person (decl. 0.92) |
| G3 | S3 | Post by `kryptomarek`: "Nie wysyłajcie już na stary adres, mój nowy to bc1qNEW" | false negative | 0 | 0, warning | 80 (decl. 0.94, warn. 0.07) |
| G4 | S4 | Post by `anon12`: "kryptomarek coś wspominał o bc1qDDD chyba" | uncertain | 40 | 40 | 40, Do weryfikacji (decl. 0.48) |
| G5 | S5 | Wallet `bc1qEEE` entered; "Mój portfel: bc1qEEE. Marek Kowalski", author unknown | no link | 0 | 0 | 0, Do weryfikacji (decl. 0.95) |
| G6 | S6 | G1 with the model not answering | fallback | 0 | 60 | 60 + Report Limitation |
| G7 | P1–P3 | Bitcointalk post by `kryptomarek` with bc1qAAA (sale) and bc1qBBB (impostor warning); GitHub profile "Donations: bc1qAAA" | two Premises in one Mention | AAA 80, BBB 0 | AAA 95, BBB 0 warning | AAA 95, BBB 0 warning (warn. 0.95) |
| G8 | D10 | Post by `kryptomarek`: "Nie wysyłajcie już na mój stary adres bc1qOLD, mój nowy to bc1qNEW" | withdrawn address | 0 / 0 | 0 / 0 warning | OLD 80 withdrawn (P14), NEW 80 |
| G9 | weakness 4 | Nickname `max`; text "maximum fee, send to bc1qMMM" | false positive | 40 | 0 | 0 |
| G10 | weakness 2 | Forum "kryptomarek: mój adres bc1qGGG" + giveaway on another service | false positive | 55 | 40 | 40 (forum: discussion) |
| G11 | weakness 3 | ETH address on the own profile, exchange check unavailable | false positive | 80 | 59 | 59 |
| G12 | D9-004 | Case has an e-mail; text with another e-mail, nickname and address | false positive | 0 | 0, e-mail of another person | 0 |
| G13 | D9-005 | The same line "kryptomarek BTC: bc1qCCC" on two services | copy | 40 | 40 | 80 (own declaration), no bonus |
| G14 | P5 | Long post: nickname at the start, another person's address about 700 characters later | false positive | not detected | 0 (distance) | 0 |
| G15 | demo-v1 | The three demo Mentions, nickname only | regression | 75 / 75 / 60 | 75 / 75 / 60 | band 60–100 |

## Testing Decisions

- **A good test here** checks external behaviour through a public port or a pure function: given Mentions, identifiers and (optionally) model answers, the Confidence Level, the Premises and the Scoring breakdown come out. No test looks at private helpers, the keyword lists one by one, or the HTTP client internals.
- **Seam 1, the existing `ConfidenceScoring.score(...)` port (Spock unit specs, as `ConfidenceScoringServiceSpec`).** The golden set drives a data-driven spec, one row per item and version, asserting the Confidence Level, each Premise status and reason, and the Premise ids of each rule. The model answers enter as plain data in the scoring input (P17). At this seam no fake port is needed, and the points arithmetic stays a pure function. A fake of the new port fits one level up, in the existing `AnalysisOrchestratorSpec`: answers, "no answer", and the Report Limitation. Prior art: `ConfidenceScoringServiceSpec`, `AnalysisOrchestratorSpec`.
- **Seam 2, the REST flow integration spec (Testcontainers, as `AnalysisFlowSpec`).** The model endpoint is stubbed by a local HTTP server, like `StubChainServer`, with the base URL injected through `@DynamicPropertySource`. The existing `AnalysisFlowSpec` covers "model off" (the default) and is updated for `confidence-2` and P27. "Model on" and "model does not answer" need the switch on, so they go into a second spec class with its own properties. The Spring test context differs per set of dynamic properties, so they cannot share the existing class. The stub records requests, which lets the spec assert that no full name, e-mail or messenger ID was sent (P15). `OpenApiContractSpec` guards the contract. The Postman and Bruno suites in `.e2e/backend` assert `confidence-1` and must be updated.
- **Seam 3, the frontend report view model (`toReportView`, Vitest, as `report-view-model.test.ts`).** The Premises come inside `report.attributions`, so the signature does not change. Cases: each status and reason label; order of Premises; highlight span at the start, middle and end of the window; answer words at 0.3, 0.5 and 0.7 with percentages; "reguła"/"model" labels; mode line; the new Report Limitation and an unknown code; Punktacja lines with Premise references; an old `confidence-1` report without `premises`.
- **The seams wait for the lead's review.** All three are existing seams, chosen as high as possible. None needs a new test harness except the model stub server.
- **Not tested automatically:** the real model and its quality (manual opt-in golden-set run, P26), the Spring Boot 4.2 attempt (its own branch: `./gradlew check integrationTest`), components and markup (ADR-0010). Verified in the running app.
- **Quality gate:** `./gradlew check integrationTest` in `backend/`; `npm run check` and `npm run build` in `frontend/`; `./scripts/api-sync.sh` after every contract change.

## Out of Scope

- Address Activity and cash-out signals (`addressActivity` stays null, DS1-007 G7).
- New Mention sources.
- Changes to the Excluded Address classification (labels, bytecode check).
- Authentication.
- A screen for the comparison of Analyses. The mark of P23 is API only.
- Reuse of model answers between Analyses (rejected, D7).
- A model that writes text (Justification, summaries) or builds the state (D3, D17).
- Probability-weighted points (thresholds in code, D9).

## Further Notes

- **Delivery split.**
  1. `01` backend, `confidence-2`: golden set; the Premise as a stored concept with code facts; whole-content detection and the 1200-character window; deterministic rule fixes; rule → Premise links; `premises` in the API; `api-sync`.
  2. `02` backend, `confidence-3`: the model behind the port, the three questions, thresholds and rule table, withdrawn address, fallback and Report Limitation, audit fields, off-by-default switch, comparison mark. The ADR exists (DJ1-001). Blocked by 01.
  3. `03` frontend: Premises in the report, with statuses, dismissed Premises with reasons, highlighted address, words with percentages, "reguła"/"model" labels, technical details, assessment mode and the new Report Limitation. Blocked by 01 for the contract; the model fields need 02.
- **Unchecked facts (handoff), to check against a live source before 02 starts** (part of them is now checked, see "Calling Jev through OpenRouter with Spring AI"): Jev's question types (`noul`, `choice`) and its "numbers and labels only" output; the endpoints (TypeSafe `/v1/systemone`, OpenRouter `/api/v1/systemone`); the model ids and pinned snapshot; price and the need for a paid balance; no `seed` or `temperature`, and about 4% run-to-run change of a decision; that `choice` favours the first option; Polish language quality; Spring AI 2.1.0 with the System One module needing Spring Boot 4.2.0; the module's property names. Read the System One OpenAPI contract before the first request (`mega-read`).
- **Open item of the lead (5.7, section 9):** the mentors have not approved sending Mention text to TypeSafe or OpenRouter. No approval for any LLM endpoint is recorded (E7-D4, D22-005). Until they approve, the demo should run with the model off (`confidence-2`), or the lead asks on Discord. A local server of the same protocol (5.6) would remove the problem, but its quality and the thresholds are unchecked.
- **Inconsistency in `jev-stories.md` S4:** warning 0.22 is at or below 0.3, so it is "no", not between the thresholds. Only declaration 0.48 is undetermined. The expected outcome (40, "Do weryfikacji") is the same. The golden set uses the corrected reading.
- **S2 and the order of rules:** D9 puts "no link → 0" before "declared by another person", but S2 expects the second reason. P7 resolves it. The lead should confirm.
- **Privacy versus the window:** D4 says the model never receives the e-mail, full name or messenger ID, but the window itself may contain them. P15 (masking) is needed for D4 to hold.
- **Wider window, more risk:** the 1200-character window helps the model, but it may link an address to a nickname from another post in the same page. P5 keeps the old 200-character reach for the identifier link.
- **Challenge 5.5** refers to "KROK 5" of the attachment, which does not exist in the text we have (it ends at KROK 4).
- **Demo:** the golden-set table c1 / c2 / c3 is the demo material for section 8 (25%). demo-v1 shows a Rejected Address for the first time once whole-content detection lands (it closes the E7.3 gap of D9-008).
- **Glossary:** `BUSINESS-CONTEXT.md` already holds Premise and the new wording of Justification and Scoring breakdown (uncommitted on `master` at the time of writing). "Assessment mode" and "withdrawn address" are not glossary terms yet. The lead decides whether they become terms.
