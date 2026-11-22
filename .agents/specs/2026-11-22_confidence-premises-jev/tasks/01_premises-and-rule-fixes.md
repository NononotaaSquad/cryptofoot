# 01 — Premises and the rule fixes (`confidence-2`)

**Spec:** [../spec.md](../spec.md) · **Blocked by:** none · **Status:** ready-for-agent

**What to build:** Every valid cryptocurrency address in every Mention becomes a Premise (PL: Przesłanka): the address, a text window of about 1200 characters around it, the facts that code computed about it, and its assessment (status, kind, points, dismissal reason). Addresses are detected in the whole content of a page, not only in the old 200-character excerpt. Scoring works per Premise and fixes the known rule errors: the author counts as a link, identifiers match whole words only, a page under the profile is "own profile" only when the author is the person or unknown, a mass comment gives no +15, and an unclassified address is capped at 59. Each rule of the Scoring breakdown names the Premises that made it apply. The API gets `premises` on each Attribution and `premiseIds` on each scoring rule; `mentions` stays. The algorithm version becomes `confidence-2`. Before any of this, a golden set of synthetic Mentions is written as a fixture and scored by today's `confidence-1`, so the gain can be shown with numbers. There is no decision model in this task (task 02) and no frontend change (task 03).

## Decisions that apply

- **Premise (D1):** one cryptocurrency address in one Mention, stored with its assessment. A Mention with two addresses gives two Premises that may be assessed differently. Dismissed Premises stay in the report with their reason.
- **Which addresses get a Premise (**P1, proposal the lead may change**):** only a valid address that reaches scoring (accepted or unclassified). A Rejected Address gets none; an Excluded Address keeps only its Mentions, as today. Premise id = Mention id + canonical address (proposal: `"{mentionId}:{address}"`), so task 02 can build the same ids before scoring.
- **Whole content and window (D2, **P2, proposal the lead may change**):** the search detects addresses in the whole content of the page. For each detected address it stores a window of 600 characters on each side of the address (1200 plus the address). At the start or end of the content the window moves inward, so it keeps its length when the content allows. The window is stored on the Mention per detected address, with the start and length of the address in the window (Kotlin `String` indices, that is UTF-16 code units; task 03 relies on this). This closes DS1-007 G1.
- **Mention identity (**P3, proposal the lead may change**):** hash of the source name, the URL and the **full content**. The full content is not stored. `context` keeps today's 200-character excerpt, so `MentionResponse` and the quote on screen do not change. Old Mentions in MongoDB will not deduplicate with new ones (D2, accepted).
- **Facts computed by code (D3)**, deterministic, one pure public function from a Mention, the window of one address and the Case identifiers:
  1. the author is the investigated person: `YES` / `NO` / `UNKNOWN`, compared with the nickname and login the way the search compares provenance today (**P6, proposal the lead may change**);
  2. the URL is under the person's profile address (today's "URL under the profile" rule);
  3. which identifier types occur in the window, matched with word boundaries (**P4, proposal the lead may change**): no letter, digit or `_` directly before or after the match; a leading `@` ignored; case- and diacritic-insensitive like the keyword lists; an e-mail matched whole;
  4. the distance in characters between the nearest identifier and the address in the window (none when no identifier is in the window);
  5. an e-mail of another person is in the window (D9-004);
  6. the number of addresses in the Mention;
  7. the service host and the source name.
- **Who decides (D4):** in this task code decides everything; the keyword lists of D9-003 stay and read the Premise window. The model questions are task 02.
- **`confidence-2` rules for one Premise (spec "Rule table per version"), in this order (**P13, proposal the lead may change**, the order part only):**
  1. **Link to the person:** the author is the person; or the URL is under the profile and the author is the person or unknown (**P7, proposal the lead may change**); or a nickname, login, e-mail, messenger ID or profile address occurs in the window as a whole word within 200 characters of the address (**P4, P5, proposal the lead may change**). The full name never links (D9-004), and the Analyst's wallet never links. No link: 0 points, status `DISMISSED`, reason `NO_PERSON_LINK`.
  2. **E-mail of another person** (as today, on the window): `DISMISSED`, reason `CONFLICTING_EMAIL`.
  3. **Warning** (keyword list on the sentence that holds the address, as today): `DISMISSED`, reason `WARNING`.
  4. **Kind:** own profile (code: under the profile with the author the person or unknown) 80; otherwise the keyword lists on the window: mass comment 10, sale or settlement 60, discussion 40. Status `COUNTED`.
  - "Address declared by another person" is not detectable without the model in `confidence-2` (spec table); G2 gives "no link" here (P7).
- **Per Attribution (D9, D9-002, D9-005 amended):** the base is the strongest kind among the counted Premises. +15 (`INDEPENDENT_SOURCES`) only for two counted Premises on different services, **both at least discussion**, with different address sentences; the copy rule of D9-005 stays (**P8, proposal the lead may change**). A mass comment never takes part in the bonus.
- **Cap (D9, D9-007 superseded, **P9, proposal the lead may change**):** an unclassified address is capped at 59 by a new rule `CLASSIFICATION_CAP` whose contribution is negative (the amount cut to reach 59), so the Scoring breakdown still sums to the Confidence Level. `CLASSIFICATION_UNAVAILABLE` and its sentence stay.
- **Rules point to Premises (D17, **P10, proposal the lead may change**):** each applied rule carries the ids of the Premises that triggered it. 0-point notes (`SAME_SERVICE`, `COPIED_TEXT`, `NICKNAME_ONLY`, `WARNING_CONTEXT`, `CONFLICTING_EMAIL`) list their Premises too. `NO_PERSON_LINK` lists the dismissed Premises. Rules not triggered by a Premise (`ENTERED_BY_ANALYST`, `CLASSIFICATION_UNAVAILABLE`, `CLASSIFICATION_CAP`) have an empty list.
- **Justification (D17, D9-006 amended, P24):** still built from templates, no numbers, never written by a model. "Same Premise assessments → same text." One new sentence for `CLASSIFICATION_CAP` (wording in Notes, reviewed by the lead).
- **Code names (**P11, proposal the lead may change**):** status `COUNTED` / `DISMISSED` / `FOR_REVIEW`; reason `WARNING` / `DECLARED_BY_ANOTHER_PERSON` / `NO_PERSON_LINK` / `CONFLICTING_EMAIL`; kind `OWN_PROFILE` / `OWN_DECLARATION` / `SALE_OR_SETTLEMENT` / `DISCUSSION` / `MASS_COMMENT`; assessed by `RULE` / `MODEL`. In `confidence-2` only `COUNTED`, `DISMISSED`, the reasons `WARNING` / `NO_PERSON_LINK` / `CONFLICTING_EMAIL`, the kinds without `OWN_DECLARATION`, and `RULE` can occur (see Notes for why the other values are in the enums already).
- **Scoring stays pure (D9-001 amended):** `ConfidenceScoring.score(ScoringInput)` has no database, no clock and no external call. The windows come in on the `FoundMention`s of `ScoringInput`.
- **Snapshot (spec "Modules / Analysis snapshot"):** the Premises with their facts and assessments are stored inside the Analysis record (on each `ScoredAttribution` in the embedded `AnalysisResult`), not on the shared Mention document. Facts depend on the Case identifiers.
- **API (D14, spec "API contract"):** `AttributionResponse.premises` (list) and `ScoringRuleContributionResponse.premiseIds` (list). `mentions` and `MentionResponse` keep their shape; their content changes: `detectedAddresses` now lists every address of the page, so more Rejected Addresses appear (in demo-v1 the malformed `…DivfN0`). Old Analyses (`confidence-1`) read back with empty `premises` and empty `premiseIds`. Contract change, so `./scripts/api-sync.sh` is required.
- **Algorithm version (D9-008 superseded):** `ALGORITHM_VERSION = "confidence-2"`. demo-v1 stays; its expected scores are in the acceptance criteria (**P27, proposal the lead may change**).
- **Golden set (D11, **P26, proposal the lead may change**):** a versioned JSON fixture `golden-set-v1` in the backend test resources, built from `.agents/context/backend/jev-stories.md` and the spec table G1–G15. It is scored first by `confidence-1` (the baseline), then by `confidence-2`. The `confidence-3` part is task 02.
- **Logs (`rules.md`, spec "Logs"):** counts, ids and rule codes only. The text window is personal data and is never logged, also not at `DEBUG`.

## Files

Paths relative to `backend/` unless they start with `../`.

| File | Role |
|---|---|
| `src/main/kotlin/squad/nononotaa/backend/analysis/AnalysisContracts.kt` | Change: `AddressWindow` (raw address, canonical address or null, text, address start, address length); `FoundMention.addressWindows` (default empty); `Premise` and `PremiseFacts` (snapshot types); `ScoredAttribution.premises` (default empty); `AppliedRule.premiseIds` (default empty). New fields go last, with defaults, and the classes keep or get `@JvmOverloads` (see Before coding) |
| `src/main/kotlin/squad/nononotaa/backend/analysis/AddressCandidateDetector.kt` | Change: one pure function that returns the window of every detected address in a text (P2); `detect` stays |
| `src/main/kotlin/squad/nononotaa/backend/analysis/PremiseFacts.kt` | New (name is a proposal). The pure public function of D3: Mention + window + Case identifiers → `PremiseFacts`. Holds the word-boundary match (P4) and the distance (P5). Used by scoring now and by the orchestrator in 02 |
| `src/main/kotlin/squad/nononotaa/backend/analysis/SearchService.kt` | Change: detects addresses in the whole content, stores the windows with the canonical address (it already has `AddressValidator`), computes the identity from the full content (P3); `context` stays the 200-character excerpt |
| `src/main/kotlin/squad/nononotaa/backend/model/Mention.kt` | Change: `addressWindows` (last field, default empty), copied into `toFoundMention()`; `computeIdentity` and `create` take the full content for the hash, separate from `context` |
| `src/main/kotlin/squad/nononotaa/backend/model/Enums.kt` | Change: `PremiseStatus`, `DismissalReason`, `PremiseKind`, `AssessedBy`, `AuthorMatch` (`YES` / `NO` / `UNKNOWN`; names are proposals, P11 does not name the last two) |
| `src/main/kotlin/squad/nononotaa/backend/service/ConfidenceScoringService.kt` | Change: per-Premise rules of `confidence-2`, sum per Attribution, `CLASSIFICATION_CAP` with its sentence, Premise ids on each rule, `ALGORITHM_VERSION = "confidence-2"` |
| `src/main/kotlin/squad/nononotaa/backend/dto/Responses.kt` | Change: `PremiseResponse`, `PremiseFactsResponse`; `AttributionResponse.premises`; `ScoringRuleContributionResponse.premiseIds` (field list in Notes) |
| `src/main/kotlin/squad/nononotaa/backend/service/AnalysisReportService.kt` | Change: maps the Premises and the Premise ids of the stored snapshot to the DTOs |
| `src/main/kotlin/squad/nononotaa/backend/analysis/AnalysisOrchestrator.kt` | Not changed: `ScoringInput` keeps its shape, the windows travel on the Mentions. Task 02 changes it |
| `src/main/kotlin/squad/nononotaa/backend/service/CandidatePreparationService.kt` | Read only: it now gets more `detectedAddresses`, so more Rejected Addresses |
| `src/main/resources/fixtures/demo-v1.json` | Read only: item G15 of the golden set reads it, so the two do not drift |
| `src/test/resources/golden-set/golden-set-v1.json` | New (the folder `src/test/resources` does not exist yet). The fixture of P26 (shape in Notes) |
| `src/test/groovy/squad/nononotaa/backend/service/ConfidenceScoringServiceSpec.groovy` | Change: a data-driven golden-set feature (Seam 1; task 02 adds its `confidence-3` rows here), new features for the rule fixes, the existing features moved to `confidence-2`; the `mention(...)` helper builds windows with the production function |
| `src/test/groovy/squad/nononotaa/backend/analysis/AddressCandidateDetectorSpec.groovy` | Change: windows at the start, middle and end of a text, short text, two addresses |
| `src/test/groovy/squad/nononotaa/backend/analysis/SearchServiceSpec.groovy` | Change: whole-content detection, windows, identity from the full content; `withId(...)` builds `Mention` by position |
| `src/test/groovy/squad/nononotaa/backend/model/MentionSpec.groovy` | Change: identity from the full content |
| `src/integrationTest/groovy/squad/nononotaa/backend/analysis/SearchServiceIntegrationSpec.groovy` | Change: identity assertion (line 74 uses `context`) and stored windows |
| `src/integrationTest/groovy/squad/nononotaa/backend/repository/MentionRepositorySpec.groovy` | Change if `create` changes its parameters; windows survive a save and a read |
| `src/integrationTest/groovy/squad/nononotaa/backend/controller/AnalysisFlowSpec.groovy` | Change: `confidence-2`, the malformed `…DivfN0` in the P2P Mention (the "every detected address is valid" feature), `premises` and `premiseIds` on demo-v1 |
| `src/integrationTest/groovy/squad/nononotaa/backend/controller/AnalysisReportSpec.groovy` | Change: `scoringRules == [[rule, contribution]]` (line 121) gets `premiseIds: []`; the stored `confidence-1` snapshot reads back with `premises == []` |
| `src/integrationTest/groovy/squad/nononotaa/backend/analysis/web2/Web2PipelineSpec.groovy` | Run; it should stay green (GitHub author `Octo-Dev` = login, Bitcointalk profile with author `UNKNOWN`). Change only if the lead decides otherwise on the open questions in Notes |
| `src/integrationTest/groovy/squad/nononotaa/backend/OpenApiContractSpec.groovy` | Not changed; guards the contract after `api-sync` |
| Groovy specs that build `ScoredAttribution` / `AppliedRule` by position: `AnalysisHistorySpec`, `AnalysisComparisonSpec`, `AnalysisReportSpec`, `ExclusionOnchainCheckSpec`, `AnalysisOrchestratorSpec`, `ExcludedAddressPipelineSpec`, `AnalysisComparerSpec` | Must compile and pass unchanged once the new fields have defaults and `@JvmOverloads` |
| `../.e2e/backend/bruno/04-analysis/wait-for-the-analysis-to-finish.bru`, `../.e2e/backend/bruno/05-rerun-and-comparison/wait-for-the-rerun-to-finish.bru`, `../.e2e/backend/bruno/06-analysis-edge-cases/wait-for-the-wallet-analysis-to-finish.bru` | Change: `confidence-2` |
| `../.e2e/backend/bruno/04-analysis/get-the-report.bru` | Change: the "classification unavailable" branches expect 59 and `CLASSIFICATION_CAP` (ETH 75 → 59, contract 60 → 59); one check that every Attribution has Premises and that every `premiseIds` entry is one of them |
| `../.e2e/backend/bruno/04-analysis/list-the-mentions.bru` | Change: the P2P ad now also lists `1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfN0` as invalid; "every detected address is validated" becomes "valid or with a validation failure" |
| `../.e2e/backend/bruno/06-analysis-edge-cases/the-wallet-alone-gives-no-confidence-level.bru` | Check: rule lists unchanged; `NO_PERSON_LINK` lists the dismissed Premises |
| `../.e2e/backend/postman/cryptofoot-backend.postman_collection.json` | The same changes as Bruno, same order and assertions (decision e2e-1) |
| `../frontend/apps/web/lib/api/` | Regenerated by `./scripts/api-sync.sh` (`openapi.json`, `generated/`), committed in the same PR. No frontend code change |
| `../.agents/context/backend/backend-architecture.md` | Change: section "Confidence Level algorithm" for `confidence-2` and Premises (DOCUMENT rule) |
| `../.agents/context/domain-model.md` | Change: section "Attribution scoring, algorithm `confidence-1`" |
| `../.agents/context/backend/diagrams/08-confidence-scoring.puml` | Change: the flow per Premise |

## Before coding

- Run `./scripts/api-sync.sh` from the repo root (Docker) and confirm there is no drift. Commit any drift on its own first.
- Read `jev-stories.md` (S1–S6, worked example P1–P3), the spec table "Golden set", D9-001 to D9-008 in `.agents/context/backend/decisions/task-9/`, and `sync-1/007-known-gaps.md` (G1).
- **Record the baseline before changing any rule.** Write the fixture and the golden-set feature first, run it on today's code, and put the observed `confidence-1` values in the fixture. Build the scoring input the way the app does: pass the fixture content through the real `SearchService` (a fake `MentionSource` and a stubbed `MentionRepository`, as `SearchServiceSpec` does) and `CandidatePreparationService`, take the classification (accepted / unclassified / excluded) from the fixture, then call `score`. The same harness then measures `confidence-2`, and G14 is "not detected" under `confidence-1` for the real reason (the 200-character excerpt).
- The spec's `bc1qAAA`, `0xCCC` and similar are placeholders. The detector and `AddressValidator` reject them. The fixture needs distinct valid addresses (correct checksum) with a placeholder → address map, so the items still read like the spec. Read `AddressValidator.kt` and `AddressCandidateDetector.kt` first; check each address with the validator in the spec.
- Groovy calls the Kotlin constructors by position. A new parameter with a default breaks those calls unless the class has `@JvmOverloads` (as `FoundMention` and `Mention` have). Add the new fields last.
- Old snapshots: `ScoredAttribution.justification = ""` is the prior art for a field that old MongoDB documents do not have. Use the same pattern for `premises` and `premiseIds`, and prove it with the stored `confidence-1` snapshot of `AnalysisReportSpec`.
- Read `.agents/skills/api-contract/SKILL.md` and run `npm run check` in `frontend/` after the sync.

## Acceptance criteria

- [ ] The fixture `golden-set-v1` exists with items G1–G15 of the spec (G6 is G1 with a "model does not answer" marker for 02), each with a category, the Case identifiers, Mentions with author, valid addresses, the classification, and the observed `confidence-1` result.
- [ ] The `confidence-1` baseline was recorded on the unchanged code and reported to the lead as a table **before** any rule changed. Where an observed value differs from the spec's c1 column, the table says so; the observed value is kept.
- [ ] Golden-set feature, `confidence-2` (Seam 1, one row per item), asserting the Confidence Level, each Premise status and reason, and the Premise ids of each rule: G1 60; G2 0 `NO_PERSON_LINK`; G3 0 `WARNING`; G4 40; G5 0; G6 60; G7 AAA 95, BBB 0 `WARNING` (two Premises from one Mention, assessed differently); G8 OLD 0 and NEW 0, both `WARNING`; G9 0 `NO_PERSON_LINK`; G10 40, no `INDEPENDENT_SOURCES`; G11 59 with `CLASSIFICATION_CAP`; G12 0 `CONFLICTING_EMAIL`; G13 40 with `COPIED_TEXT`; G14 0; G15 75 / 75 / 60.
- [ ] Author: a post by the person without the nickname in the text gives a link (G1). An author that is present while the Case has no nickname and no login gives `UNKNOWN`, so a Bitcointalk profile stays own profile 80 (`Web2PipelineSpec`).
- [ ] Word boundaries: nickname `max` is not found in "maximum" (G9); `@zenek_fx`, `zenek_fx:` and `ZENEK_FX` are found; `zenek_fx2` and `xzenek_fx` are not.
- [ ] Distance: an identifier inside the window but more than 200 characters from the address gives no link, and the fact still records the distance (see Notes on G14).
- [ ] Own profile: a page under the profile with a known other author gives 0, `NO_PERSON_LINK` (G2); with the author unknown or the person it gives 80.
- [ ] Bonus: a discussion plus a mass comment on another service gives 40, no +15 (G10); two discussions on different services with different sentences still give 55.
- [ ] Cap: an unclassified address at 80 gives 59 with `CLASSIFICATION_UNAVAILABLE` 0 and `CLASSIFICATION_CAP` −21, and the contributions sum to 59; an unclassified address at 40 has no `CLASSIFICATION_CAP`.
- [ ] Whole content: an address 700 characters after the identifier in a long page is detected and has a Premise (DS1-007 G1); the window keeps its length near the start and the end of the content, and `text.substring(start, start + length)` is the address.
- [ ] Each Premise has its facts (D3: author, under profile, identifier types, distance, other e-mail, number of addresses, host and source name) and `assessedBy` `RULE`. No Premise is made for a Rejected or an Excluded Address.
- [ ] The existing features of `ConfidenceScoringServiceSpec` pass under `confidence-2` (only the version and the new Premise ids change), and "same input → same result, whatever the order of Mentions" holds for the Premises too.
- [ ] `GET /api/1/analyses/{id}/report`: each Attribution has `premises`, each scoring rule has `premiseIds`, and every id in `premiseIds` is one of the Attribution's Premises. `mentions` keeps its shape. A stored `confidence-1` Analysis reads back with `premises: []` and `premiseIds: []` (`AnalysisReportSpec`).
- [ ] demo-v1 over HTTP (`AnalysisFlowSpec`): `algorithmVersion` `confidence-2`; nickname Case 75 / 75 / 60; nickname + profile address 95 / 95 / 60; wallet-only Case 0 / 0; the P2P Mention lists the malformed `…DivfN0` as a Rejected Address (P27).
- [ ] No log line holds a text window (check the scoring and search logs).
- [ ] `./scripts/api-sync.sh` was re-run and `frontend/apps/web/lib/api/` is committed; `OpenApiContractSpec` passes; `npm run check` passes in `frontend/`.
- [ ] `./gradlew spotlessApply` and `./gradlew check integrationTest` pass in `backend/`.
- [ ] Postman and Bruno pass against a running backend (`./scripts/e2e-backend.sh all`), and both collections still have the same requests and assertions.
- [ ] Verified in the running app (`./scripts/run-backend.sh`): a demo-v1 Analysis report shows `premises`, `premiseIds` and `confidence-2` (curl or Bruno).

## Notes

- **Slots left for task 02 (so 02 does not have to reshape anything):**
  - The Premise id is known before scoring (P1), and the facts function is public and pure, so the orchestrator in 02 can build the Premises and the model state without copying scoring code.
  - The `confidence-2` assessment of one Premise is one function inside `ConfidenceScoringService`, so 02 can call it per Premise as the fallback.
  - `Premise.assessedBy` exists (always `RULE` here). 02 sets it to `MODEL` when the model answered, and adds the answers (a nullable field, added by 02), the withdrawn note and the undetermined flags. 01 adds no model field.
  - The enums already hold `FOR_REVIEW`, `DECLARED_BY_ANOTHER_PERSON`, `OWN_DECLARATION` and `MODEL`, so 02 and 03 work against a contract that does not change its enums.
  - `ScoringInput` keeps its shape; 02 adds the answers to it.
  - The fixture has an empty `cannedAnswers` place per Premise and an empty `confidence-3` expectation per item; 02 fills them.
- **Slots left for task 03:** `PremiseResponse` carries everything of Part A of 03: id, Mention fields, window, start and length, status, kind, points, reason, `assessedBy`, facts. 03 adds the label for `CLASSIFICATION_CAP`; until then the frontend shows its raw code (existing behaviour).
- **Proposed DTO fields** (final names are this task's choice, the spec "API contract" gives the shape):
  - `PremiseResponse`: `id`, `mentionId`, `sourceName`, `url`, `author`, `collectedAt`, `text` (the window), `addressStart`, `addressLength`, `status`, `kind`, `points`, `dismissalReason` (nullable), `assessedBy`, `facts`.
  - `PremiseFactsResponse`: `authorIsPerson` (`YES` / `NO` / `UNKNOWN`), `underProfile`, `identifierTypes` (types only, never values), `identifierDistance` (nullable), `conflictingEmail`, `addressCount`, `host`.
  - `ScoringRuleContributionResponse.premiseIds`.
- **Proposed fixture shape:** `version`, then `items[]` with `id` (G1…), `from` (S1, P1–P3, …), `category`, `identifiers[]`, `mentions[]` (`key`, `sourceName`, `url`, `author`, `content`), `addresses` (placeholder → valid address), `classification` (address → `ACCEPTED` / `UNCLASSIFIED` / `EXCLUDED`), `expected` per algorithm version (`confidence-1` recorded, `confidence-2` asserted, `confidence-3` empty), `cannedAnswers` (empty).
- **Proposals of this task, to be reviewed by the lead (not in the spec):**
  - **An identifier in the URL is not a text link.** Identifiers are searched only in the window; the URL counts only through the profile rule (P7). Without this, G2 would be 40 (the nickname is in the profile URL), not 0 as the spec expects.
  - **The author link is weak.** It is a nickname or login compare, so `NICKNAME_ONLY` still applies when all counted Premises link only by nickname, login or author. This keeps `Web2PipelineSpec` (GitHub) as it is.
  - **`UNKNOWN` when the Case has no nickname and no login**, even when the source gives an author (P6 names only "no author"). Otherwise a Bitcointalk profile found by its profile address would drop from 80 to 0.
  - **The page named after the nickname** (D9-003) stays own profile only with a second identifier of the person in the window and the author not `NO`. The 200-character limit does not apply to that second identifier, because it is about the page, not the address.
  - **The keyword lists read the URL and the window**, as today they read the URL and `context`. Only `context` is replaced.
  - **Premise points** are the base of its kind (0 when dismissed). They do not add up: the Attribution takes the strongest kind, as today.
  - **The window stores the canonical address** (from `AddressValidator`, null for an invalid one), so scoring matches a candidate to its window without a case rule. An address that occurs twice in one Mention gives one window, around its first occurrence.
  - **Number of addresses in the Mention** = distinct detected addresses, Rejected ones included (the text shows them all).
  - **A Mention without a window for the address** (for example an old stored Mention) gives no Premise and no support. There is no fallback to `context` in production code.
  - **`CLASSIFICATION_CAP` sentence** (no numbers): "Z tego powodu stopień pewności ograniczono, tak aby nie wskazywał wysokiego prawdopodobieństwa." It follows the `CLASSIFICATION_UNAVAILABLE` sentence in the Justification.
  - **After the switch, `confidence-1` is not kept as runnable code** (ponytail). Its golden-set result stays as recorded data in the fixture for the demo table.
  - **One `assessedBy` per Premise** for the text reading (kind, warning, and later declaration), because D16 and the per-Premise fallback make them always equal. Link facts, e-mail, bonus, cap and points are always `RULE` and need no field.
  - File and type names marked "proposal" in the table, and the golden-set feature inside `ConfidenceScoringServiceSpec` (task 02 expects it there).
- **Open questions for the lead:**
  - **G14 does not test P5.** At about 700 characters the nickname is outside the 600-character half-window, so G14 tests the window, not the 200-character limit. Add an item G14b (nickname about 400 characters before the address, inside the window)? This task adds it to the fixture as a proposal.
  - **G7 needs a profile address.** The spec's c1 value "AAA 80" is only possible when the Case has the GitHub profile address (`https://github.com/kryptomarek`); the fixture includes it. Confirm.
  - **P27 holds only with the classification available.** When the on-chain check is unavailable (the E2E branches for it), demo-v1 ETH goes from 75 to 59 and the contract from 60 to 59 because of the cap. Confirm that this is the expected `confidence-2` result.
  - **P7 alternative:** keep "profile page, another author" as 0 `NO_PERSON_LINK` in `confidence-2`, or make it discussion 40 with "Do weryfikacji"? This task follows P7 (0).
  - **The 1200-character window may hold another person's e-mail or a contest word from a nearby post** on a thread page, which dismisses a Premise or turns it into a mass comment more often than the 200-character excerpt did. Accept this for `confidence-2`, or let the e-mail and keyword checks read a shorter part (for example the sentence that holds the address)?
  - `AuthorMatch` and the "assessment mode" are not glossary terms yet (spec Further Notes).
- **Lead decisions (2026-11-22), they override the text above:**
  - **L1 — `…DivfN0` stays undetectable.** The detector regex only accepts Base58, and `0` is not Base58, so whole-content detection cannot make it a Rejected Address. demo-v1 and the detector stay unchanged; the P2P Mention keeps 2 detected addresses (3 is the count over the whole demo-v1). Drop the "`…DivfN0` as a Rejected Address" expectation from P27, `AnalysisFlowSpec`, Bruno and Postman. `SyntheticSourceIntegrationSpec` (not in the table above) is updated for the identity and whole-content change.
  - **L2 — a warning on a linked Premise is for review, not dismissed.** A Premise linked to the person whose address sentence matches the warning list gets status `FOR_REVIEW`, 0 points, reason `WARNING`. `WARNING_CONTEXT` lists these Premises, with the sentence "Wzmianki, w których adres występuje w ostrzeżeniu lub z prośbą, by go nie używać, wymagają weryfikacji, więc nie zostały uznane za potwierdzenie." `NO_PERSON_LINK` appears only when no Premise is counted, and lists only the Premises with reason `NO_PERSON_LINK`; it is not emitted when every Premise was put aside for a warning or a conflicting e-mail. A Premise without a link stays `DISMISSED` `NO_PERSON_LINK` (the link rule is first). A conflicting e-mail stays `DISMISSED` `CONFLICTING_EMAIL`. So G3, G7 BBB and G8 OLD/NEW are 0 with status `FOR_REVIEW` (reason `WARNING`).
- **Definition of done:** see `AGENTS.md`. The PR says `Closes #<issue>`; the issue number for this task is not known yet.
