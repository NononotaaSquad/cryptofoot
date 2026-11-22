# 03 — Premises in the Analysis report

**Spec:** [../spec.md](../spec.md) · **Blocked by:** 01 (contract); model fields need 02 · **Status:** ready-for-agent

**What to build:** The Analyst opens an Attribution on Case details and sees, after "Uzasadnienie" and before "Wzmianki", a new block "Przesłanki (N)". Each Premise (PL: Przesłanka) shows its Mention (source, link, author, collection date), the text window with the assessed address highlighted, its status "Zaliczona", "Odrzucona" or "Do weryfikacji" (icon and word), the reason of a dismissal, the kind of post and the points it gave, the facts that code found, and for each part who assessed it: "reguła" or "model". When the model answered, its three answers are shown as "tak / nie / niepewne" with a percentage, and the raw numbers sit in a collapsed "szczegóły techniczne" section. Each line of Punktacja names the Premises that made it apply. The report summary shows, once per Analysis, the algorithm version, the assessment mode, the model version and the question-set version, and the new Report Limitation when the model did not answer. A report without model answers (model off) and an older report without `premises` render without errors. This covers user stories 1–6, 8–10, 20–26 of the spec.

The task has two parts, so it can be split if 02 is late:

- **Part A, after 01 (rules only, `confidence-2`):** the Premise block with Mention fields, highlighted window, status, dismissal reason, kind, points and fact lines, every element labelled "Oceniono: reguła"; Punktacja lines with their Premises; the new rule codes of 01 (for example `CLASSIFICATION_CAP`, **P9, proposal the lead may change**) in `report-labels.ts`; the "Wersja algorytmu" line in the summary; the older report without `premises`.
- **Part B, after 02 (`confidence-3`):** the three model answers with words and percentages; "Oceniono: model" on kind, declaration and warning; "szczegóły techniczne"; the note "Adres wycofany przez autora"; the summary lines "Tryb oceny", "Wersja modelu", "Wersja zestawu pytań"; the Report Limitation `MODEL_ASSESSMENT_UNAVAILABLE` (**P19, proposal the lead may change**). If 01 already puts a field in the contract (for example the withdrawn note or the status `FOR_REVIEW`), its label and test can be done in part A, with test data only.

## Decisions that apply

- `toReportView(report, mentions, caseIdentifiers)` stays the only public entry and its signature does not change: the Premises come inside `report.attributions[].premises` (spec "Seam 3"). New functions are internal and tested only through `toReportView`. Components render only the view model and never read API types (ADR-0009, S1 of the analysis-report spec).
- API types come only from the generated client after `./scripts/api-sync.sh` (ADR-0007). No hand-written fetch and no hand-written response type. No new hook: the Premises arrive in the existing `useGetReport` answer, and the summary fields in `useGetAnalysis` / the `analysis` of the report.
- One Premise is one cryptocurrency address in one Mention (D1, glossary). A Mention with two addresses gives two Premises under two Attributions; each Attribution shows only its own Premises. Dismissed Premises stay visible (D1).
- Block and copy (**P25, proposal the lead may change**):
  - heading "Przesłanki (N)", N = all Premises of the Attribution, dismissed ones included;
  - order: "Zaliczona" first, then "Do weryfikacji", then "Odrzucona"; inside a group the order of the API (see Notes);
  - statuses `COUNTED` "Zaliczona", `FOR_REVIEW` "Do weryfikacji", `DISMISSED` "Odrzucona" (codes **P11, proposal the lead may change**); each with an icon and the word, never colour alone;
  - reasons `WARNING` "Ostrzeżenie przed tym adresem", `DECLARED_BY_ANOTHER_PERSON` "Adres podany przez inną osobę", `NO_PERSON_LINK` "Brak powiązania z badaną osobą", `CONFLICTING_EMAIL` "E-mail innej osoby";
  - kinds `OWN_PROFILE` "Profil badanej osoby", `OWN_DECLARATION` "Własna deklaracja", `SALE_OR_SETTLEMENT` "Sprzedaż lub rozliczenie", `DISCUSSION` "Dyskusja", `MASS_COMMENT` "Wpis masowy";
  - answers "Deklaracja: tak (92%)", "niepewne" for an undetermined answer; labels "Oceniono: reguła" / "Oceniono: model"; summary line "Tryb oceny: z modelem" / "tylko reguły";
  - the address is highlighted with `<mark>`.
- Withdrawn own address (D10, **P14, proposal the lead may change**): status "Zaliczona" plus the note "Adres wycofany przez autora" on the Premise. The note is text with an icon, next to the status, not instead of it.
- Who assessed what (D16, spec "Specific interactions"): link facts, e-mail, independence, cap and points are always "reguła". Kind, declaration and warning are "model" when the model answered for this Premise, otherwise "reguła". The view model reads this from the API field (D14 "who assessed it"); it does not guess it from the mode.
- Model answers (D8, D15, **P28, proposal the lead may change**): three answers `rodzaj_wpisu`, `deklaracja`, `ostrzezenie`. The word ("tak" / "nie" / "niepewne", or the kind label for `rodzaj_wpisu`) follows the value and the undetermined flag sent by the backend, not the rounded percentage. The percentage is a whole number, rounded half up (0.925 → "93%"). The frontend does not keep its own copy of the thresholds 0.7 / 0.3 / 0.5 (D9, **P12, proposal the lead may change**): they belong to the question-set version in the backend.
- "szczegóły techniczne": collapsed by default, one per Premise. It lists the raw probability of each answer (every option of `rodzaj_wpisu`), and the request id when the API sends one (**P21, proposal the lead may change**). Shown only when the Premise has at least one model answer.
- Punktacja (D17, glossary "Scoring breakdown", **P10, proposal the lead may change**): each line still shows its label and points, and now names the Premises from its `premiseIds`. When `premiseIds` is present, the source names in brackets come from those Premises only, not from all Mentions of the Attribution. When it is absent (older Analysis), the line is built as today.
- Summary, once per Analysis (D18, **P21, P22, proposal the lead may change**): "Wersja algorytmu: {algorithmVersion}" (the field exists today and is not shown yet), "Tryb oceny: z modelem" / "Tryb oceny: tylko reguły" (`WITH_MODEL` / `RULES_ONLY`, P11), and with the model also "Wersja modelu: {modelVersion}" and "Wersja zestawu pytań: {questionSetVersion}". A line whose field is absent or `null` is not shown.
- New Report Limitation (**P19, proposal the lead may change**): `MODEL_ASSESSMENT_UNAVAILABLE` gives "Model nie ocenił części lub wszystkich przesłanek; te przesłanki oceniono samymi regułami." It goes into the existing `reportLimitations` list; an unknown code keeps the existing fallback sentence.
- Older Analysis (spec "API contract"): `premises` absent or empty → no "Przesłanki" block, Punktacja as today, no mode line. Nothing else in the report changes. The `mentions` field and the "Wzmianki" block stay as they are (D14).
- The Justification stays the backend text, passed through unchanged (D17, Q46 of the analysis-report spec). The frontend adds no sentence of its own.
- Texts live in `report-labels.ts` as code → Polish maps with the existing `isKnownCode` fallback: an unknown status, reason, kind or rule code is shown as its raw code, never hidden.

## Files

Paths relative to `frontend/apps/web/`.

| File | Role |
|---|---|
| `lib/api/openapi.json`, `lib/api/generated/**` | Not edited by hand. Regenerated by `./scripts/api-sync.sh` after 01 and again after 02. Expected new model files (names are set by 01 and 02, check them after `npm run api:gen`): a Premise response type with its status, reason and kind enums, the answer type, `premiseIds` on `ScoringRuleContributionResponse`, `assessmentMode`, `modelVersion`, `questionSetVersion` on `AnalysisResponse`, the new code in `ReportResponseLimitationsItem` |
| `features/analysis-report/report-labels.ts` | Change: maps for Premise statuses, reasons, kinds, answer words, "Oceniono: …", fact lines, assessment mode, the withdrawn note, the new rule codes of 01 and 02, the `MODEL_ASSESSMENT_UNAVAILABLE` sentence, a percent formatter (P28). Adding the new Report Limitation is forced by the type `Record<ReportResponseLimitationsItem, string>` once the client is regenerated |
| `features/analysis-report/premise-view-model.ts` | New, pure. Types `PremiseView` and its parts, and the mapping of one Attribution's `premises` (order, highlight split, facts, answers, technical details). Imported only by `report-view-model.ts`; not tested on its own (see Notes) |
| `features/analysis-report/report-view-model.ts` | Change: `AttributionView` gets `premisesHeading` and `premises: PremiseView[]`; `ScoringLine` gets the Premise references; `ReportSummaryView` gets the version and mode lines |
| `features/analysis-report/report-view-model.test.ts` | Change: the cases of the acceptance criteria, with factories for a Premise and a model answer typed with the generated types |
| `features/analysis-report/premise-list.tsx` | New. Renders "Przesłanki (N)" and each Premise from `PremiseView`; uses `Badge`, `Collapsible` and `MentionLink` |
| `features/analysis-report/attribution-details.tsx` | Change: renders `PremiseList` between "Uzasadnienie" and "Wzmianki"; Punktacja lines show their Premise references |
| `features/analysis-report/report-summary.tsx` | Change: the lines "Wersja algorytmu", "Tryb oceny", "Wersja modelu", "Wersja zestawu pytań" in the existing `<dl>` |

UI components used (read, not changed): `packages/ui/src/components/badge.tsx` (`variant`, `render`, `className`; icon with `data-icon="inline-start"` as in `report-summary.tsx`), `collapsible.tsx` (`Collapsible`, `CollapsibleTrigger`, `CollapsibleContent` on Base UI, as in `Scoring` of `attribution-details.tsx`). Icons from `lucide-react` 1.66.0, checked in `frontend/node_modules`: `CircleCheck`, `CircleX`, `CircleQuestionMark` (alias `CircleHelp`), `TriangleAlert`, `Info`.

Not changed by this task: `report-view.tsx`, `attribution-list.tsx`, `excluded-addresses.tsx`, `rejected-addresses.tsx`, `collapsible-section.tsx`, `address-activity-figures.ts`, `packages/ui`.

## Before coding

- Read 01 and 02 (`tasks/01_premises-and-rule-fixes.md`, `tasks/02_decision-model.md`) for the final field and code names. This task uses the names of P11 and of the spec "API contract"; where 01 or 02 chose other names, theirs win.
- Run `./scripts/api-sync.sh` from the repo root and read the new types in `lib/api/openapi.json` and `lib/api/generated/model`. Every field is optional in the generated types and many can be `null`; the view model must handle each without `!` and without `as`.
- Read `features/analysis-report/report-view-model.ts`, `report-view-model.test.ts` and `report-labels.ts` again: the factories (`attribution()`, `report()`), `isKnownCode`, `scoringRule().withSources`, `mentionView` and `formatDateTime` (zone fixed to `Europe/Warsaw`) are reused, not copied.
- Check with the backend code of 01 how the address start and length in the window are counted: UTF-16 code units (JavaScript `string` indices) or Unicode code points (Kotlin `String` indices are UTF-16, so they should match). A text with an emoji before the address is the test for it.
- Read the Base UI Collapsible docs for the installed `@base-ui/react` version (trigger semantics, `aria-expanded`) and MDN for `<mark>` and how screen readers announce it.
- Fetch the Web Interface Guidelines named in `.agents/skills/web-design-guidelines/SKILL.md` and check the new markup against them before the browser check.
- The Pencil design has no frame for Premises (see Notes). Follow the look of the existing blocks of `attribution-details.tsx`.

## Acceptance criteria

Part A (after 01):

- [ ] Tests: each status code gives its word and icon key; an unknown status code gives the raw code.
- [ ] Tests: each of the four reasons gives its label; a "Zaliczona" Premise has no reason; an unknown reason code gives the raw code.
- [ ] Tests: each of the five kinds gives its label and the Premise's points as text ("80", "0").
- [ ] Tests: Premises are ordered "Zaliczona", "Do weryfikacji", "Odrzucona", and inside a group they keep the API order. The heading counts all of them ("Przesłanki (3)").
- [ ] Tests: the highlight splits the window into before / address / after for an address at the start, in the middle and at the end of the window; a start or length outside the window, or absent, gives the whole window as plain text with no highlight and no error; an emoji before the address does not move the highlight.
- [ ] Tests: the fact lines (D3): author is the investigated person "tak / nie / nieznany" (P6), page under the person's profile, the identifier types found (types, not values), the distance in characters, e-mail of another person, number of addresses in the Mention, service; each with "Oceniono: reguła". An absent fact gives no line.
- [ ] Tests: the Mention part of a Premise has source name, link (non-http(s) stays plain text, as `mentionView` does today), author ("—" or no line when absent, see Notes) and collection time in pl-PL.
- [ ] Tests: a Punktacja line with `premiseIds` names those Premises and only their source names; a `premiseIds` entry that matches no Premise is ignored; a line without `premiseIds` is built as today; the new rule codes of 01 have labels, and a negative contribution (the cap, P9) is shown with its minus sign.
- [ ] Tests: an older report with `premises` absent, and one with `premises: []`, give no Premise block, Punktacja as today and no mode line.
- [ ] Tests: the summary shows "Wersja algorytmu: confidence-2" when `algorithmVersion` is set and no line when it is `null`.

Part B (after 02):

- [ ] Tests: answers at probability 0.3, 0.5 and 0.7 with the backend value "nie", undetermined and "tak" give "nie (30%)", "niepewne (50%)", "tak (70%)"; 0.925 gives "93%"; the word follows the backend value, not the percentage.
- [ ] Tests: `rodzaj_wpisu` shows the kind label of the chosen option with its percentage, or "niepewne"; "szczegóły techniczne" list every option with its raw probability.
- [ ] Tests: kind, declaration and warning are "Oceniono: model" when the model answered for the Premise and "Oceniono: reguła" when it did not; a Premise without model answers has no answer lines and no "szczegóły techniczne".
- [ ] Tests: a withdrawn address gives status "Zaliczona" and the note "Adres wycofany przez autora".
- [ ] Tests: the mode line for `WITH_MODEL` and `RULES_ONLY`; model and question-set version lines only when present.
- [ ] Tests: `MODEL_ASSESSMENT_UNAVAILABLE` gives its sentence; an unknown limitation code keeps the fallback sentence.

Both parts:

- [ ] Fixtures are typed with the generated model types and built by small factories in the test file. No test looks at components, hooks or markup (ADR-0010).
- [ ] No meaning by colour alone: status, reason, "Do weryfikacji", the withdrawn note and the "reguła" / "model" labels are words; icons are `aria-hidden`; the highlight uses `<mark>` (not a coloured `span`), so it is not colour alone either.
- [ ] Every "szczegóły techniczne" opens and closes with Tab plus Enter/Space, is collapsed on entry and has a visible focus ring; the Premise block adds no other focus stop except the source links.
- [ ] A long window (about 1200 characters) wraps and does not widen the page; a long address in it breaks (`break-all` as in `MentionLink`).
- [ ] `npm run check` and `npm run build` pass from `frontend/`.
- [ ] Verified in the browser against `./scripts/run-backend.sh` with demo data and a clean console: an Attribution with its Premises and the highlight, Punktacja with Premise references, the summary lines, and (after 02, model off by default) "Tryb oceny: tylko reguły" with no answer lines; an older Analysis saved before 01 still opens without errors, if one is in the database. Model-on states and the new Report Limitation are checked only by the unit tests unless the lead switches the model on; report them as read, not run.

## Notes

- **What this task expects from 01:** `premises` on each `AttributionResponse` with an id, the Mention fields (source name, URL, author, collection time), the text window, the start and length of the address in the window, status, kind, points, dismissal reason or none, the code facts of D3; `premiseIds` on each Scoring breakdown entry; the new rule codes and their names; old Analyses read back with an empty or absent `premises`.
- **What this task expects from 02:** per Premise the three answers (value or label, raw probability of each option, undetermined flag, who assessed it, request id), the withdrawn note; on the Analysis `assessmentMode`, `modelVersion`, `questionSetVersion`; the code `MODEL_ASSESSMENT_UNAVAILABLE`.
- **Design:** the Pencil file is encrypted and has no frame for Premises. The layout here is a proposal without a design frame; the lead (or the frontend partner) may change it in Pencil before or after coding.
- **Proposals of this task, to be reviewed by the lead (not in the spec):**
  - Each Premise gets a number in the order of the block ("Przesłanka 1", "Przesłanka 2"), and a Punktacja line names them by number and source, for example "Adres na profilu osoby (przesłanka 1: GitHub)". The ids themselves are never shown.
  - Premise layout: a list item with the first line "Przesłanka {n} · {status badge} · {kind} · {points} pkt", then source, link, author and date, then the window as a quote with `<mark>`, then the facts and answers as a `<dl>`, then "szczegóły techniczne". The whole window is shown, wrapped, with `whitespace-pre-wrap`.
  - Inside a status group, the API order is kept (no new sort key), so the order is the same as in the backend.
  - The window is shown unmasked: masking (P15) is only for what is sent to the model; the Analyst sees the text as found.
  - An absent author gives no "Autor" line, not "—".
  - The Premise mapping lives in `premise-view-model.ts`, imported only by `report-view-model.ts`, so `report-view-model.ts` does not grow by some 200 lines. It is still tested only through `toReportView` (one seam, spec "Seam 3"). If the lead prefers one file, it stays inside `report-view-model.ts`.
  - The percent formatter goes into `report-labels.ts`, next to `formatAmount`.
- **Open questions for the lead:**
  - "Odrzucona" (a dismissed Premise) is close to "Adresy odrzucone" (Rejected Address, an invalid address) in the same report. Keep both words, or use for example "Pominięta" for a Premise?
  - Should the block "Przesłanki (N)" be open, or collapsed like Punktacja? With many Premises of about 1200 characters each, an open block is long. This task keeps it open (P25 puts it next to "Uzasadnienie", which is open).
  - P28 says "the yes/no word follows the thresholds". This task takes the word from the backend value, so the thresholds live only in the backend. Confirm that the API sends the value and the undetermined flag (D14 says so), and that the frontend must not compute them.
  - Should the collapsed Attribution row show a count, for example "3 przesłanki, 1 do weryfikacji"? Not in the spec; this task does not add it.
  - "Assessment mode" and "withdrawn address" are not glossary terms yet (spec Further Notes). The code here uses `assessmentMode` and `withdrawn…` as 01 and 02 name them.
  - Should the "Wersja algorytmu" line be shown to the Analyst at all, or only in a technical part of the summary? D18 says "shown once per Analysis"; this task puts it in the summary `<dl>`.
