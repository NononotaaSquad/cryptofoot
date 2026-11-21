# 03 — Powiązania, Adresy wykluczone i Adresy odrzucone

**Spec:** [../spec.md](../spec.md) · **Blocked by:** 02 — Case details and report summary · **Status:** ready-for-agent

**What to build:** Under the report summary of Case details, the Analyst sees the note "Powiązania są tropami do dalszej weryfikacji, a nie dowodami." and the Attributions "Powiązania (N)", sorted by Confidence Level and all collapsed. Each row expands into Punktacja, Uzasadnienie, Wzmianki, Weryfikacja adresu and Aktywność adresu. Below them are the collapsed sections "Adresy wykluczone (N)" (rows expand to their Mentions) and "Adresy odrzucone (N)" (built from the Mentions of the Analysis). This covers user stories 32–53 of the spec.

## Decisions that apply

- `toReportView(report, mentions, caseIdentifiers)` stays the only public entry (S1). This task adds internal, non-exported functions for Attribution, Excluded Address and Rejected Address. They are tested only through `toReportView`.
- Order: Confidence Level descending, then address. Bands: 80–100 "Bardzo wysokie prawdopodobieństwo", 60–79 "Prawdopodobny", 40–59 "Średni", 10–39 "Niski / szum". The bar is one colour, and the percent and band label are always shown as text.
- Punktacja: collapsed by default with "Rozwiń" / "Zwiń". It has one line per `scoringRules` entry with its points, then the bold line "Stopień pewności" with `confidenceLevel`. An unknown rule code is shown with its raw code and points and adds no Justification sentence. *Since Q45 the known codes are the 13 codes of `confidence-1`, labelled in `report-labels.ts`.*
- Justification: one sentence per known rule, no numbers, no technical terms (glossary). Known codes in `provisional-1`: `MENTIONED_WITH_IDENTIFIER` (base 40, the address appears in a source together with an identifier of the person) and `INDEPENDENT_SOURCES` (+15, Mentions from at least two different hosts with different text; a copy of the same text on another service does not count). *Superseded by Q46: Uzasadnienie shows the backend `justification` text as returned; the frontend has no sentences per rule.*
- `classificationStatus: UNAVAILABLE` → the collapsed row says "Nie sprawdzono, czy adres należy do serwisu (np. giełdy)."
- Input-address flag: the address equals the Case `walletAddress`, compared in the canonical form the backend stores → the row says "Adres podany przy uruchomieniu analizy".
- Weryfikacja adresu: format valid, network, result of the exchange and contract check. Built from the Mention's `detectedAddresses` entry for this address and from `classificationStatus`.
- Wzmianki (N): source name, link, collection time, quoted fragment (`context`). Read from `mentions` (Q47; formerly the list `justification`). Excluded Addresses read their Mentions from `mentions` too.
- Address Activity (S2): `toAddressActivityFigures(activity)` in its own module. Volume = `totalReceived + totalSent` in `unit`, and a `null` total means unknown, not zero. Count = `events.length`. Period = earliest to latest `occurredAt`. Events are sorted ascending. A `null` activity means no data and shows "Nie udało się pobrać danych o aktywności tego adresu. Brak danych nie oznacza braku aktywności." `CASH_OUT_SIGNAL` is its own event with the line "Możliwe wyjście do walut tradycyjnych."; `SWAP`, `RECEIVED` and `SENT` get plain labels. `coverageLimitation` is a warning under the block. Only the body of this function changes when the backend supplies the figures.
- No Attributions: "Powiązania (0)" and a grey block with "Nie znaleziono powiązań w sprawdzonych źródłach." and "Brak powiązań nie oznacza, że osoba nie ma adresów kryptowalut." In this case the note above the list is not shown.
- Excluded Addresses: collapsed with "Rozwiń". The sentence "Te adresy pojawiły się w źródłach, ale należą do serwisów, z których korzysta wiele osób. Nie są powiązaniami." comes first. Each row shows address, network, reason label and `classificationSource` as returned, and expands to its Mentions (source name and link, no collection time). Reasons: `EXCHANGE` "Adres giełdy kryptowalut", `SMART_CONTRACT` "Adres programu w sieci (smart kontrakt)", `MIXER` "Adres miksera", `FUNDRAISER` "Adres zbiórki publicznej".
- Rejected Addresses: from `GET /analyses/{analysisId}/mentions`, every `detectedAddresses` entry with `valid: false`, de-duplicated by address and reason, with the `sourceName` of its Mention. Reasons: `MALFORMED` "Błędny format", `CHECKSUM_MISMATCH` "Błędna suma kontrolna", `UNSUPPORTED_NETWORK` "Nieobsługiwana sieć", `NETWORK_MISMATCH` "Niezgodna sieć".
- Components render only the view model and never read API types (S1). Only Case details imports from this feature (ADR-0009). No component tests (ADR-0010).

## Files

Paths relative to `frontend/apps/web/`.

| File | Role |
|---|---|
| `features/analysis-report/report-labels.ts` | Change: maps for bands, Excluded reasons, Rejected reasons, rule codes (Punktacja label; *since Q46 no Justification sentence*), activity event labels |
| `features/analysis-report/report-view-model.ts` | Change: Attribution, Excluded Address and Rejected Address sections inside `toReportView` |
| `features/analysis-report/report-view-model.test.ts` | Change: the S1 cases below |
| `features/analysis-report/address-activity-figures.ts` | New: type `AddressActivityFigures` and `toAddressActivityFigures` (S2) |
| `features/analysis-report/address-activity-figures.test.ts` | New: the S2 cases below |
| `features/analysis-report/attribution-list.tsx` | New: note, "Powiązania (N)", collapsed rows, "Powiązania (0)" empty state |
| `features/analysis-report/attribution-details.tsx` | New: Punktacja, Uzasadnienie, Wzmianki (N), Weryfikacja adresu, Aktywność adresu w sieci {network} |
| `features/analysis-report/excluded-addresses.tsx` | New: collapsed section with expandable rows |
| `features/analysis-report/rejected-addresses.tsx` | New: collapsed section "Adresy odrzucone (N)" |
| `features/analysis-report/report-view.tsx` | Change: fill the slot left by task 02 |

## Before coding

- Read the Base UI docs (installed `@base-ui/react` 1.9.0, checked in `frontend/node_modules`) for Collapsible and Accordion. `packages/ui` already has `collapsible.tsx` and `accordion.tsx` (base-nova). Choose one for the rows and record the choice and why: independent rows or one open at a time, and the trigger semantics (`aria-expanded`, Enter/Space). Do not use Radix.
- Read the shadcn base-nova docs for the same components to check the class names and slots.
- Check how the backend stores a wallet address (case of ETH hex, trimming) before you write the input-address comparison.
- Run `./scripts/api-sync.sh` to confirm there is no drift.

## Acceptance criteria

- [ ] S1: band edges 39/40, 59/60, 79/80 and 100 give the right labels.
- [ ] S1: Attributions are sorted by Confidence Level descending, and ties are sorted by address.
- [ ] S1: Punktacja lines and the "Stopień pewności" line match `scoringRules` and `confidenceLevel`. An unknown rule code gives a line with its raw code and points.
- [ ] S1: each known rule gives its Justification sentence, an unknown one gives none, and no sentence contains a digit. *Superseded by Q46: the view model passes the `justification` text through unchanged.*
- [ ] S1: `UNAVAILABLE` gives the warning and `NOT_EXCLUDED` does not. The input-address flag is on for an address equal to `walletAddress` (also when it differs only in letter case, if the canonical form says so) and off otherwise.
- [ ] S1: the "Weryfikacja adresu" lines for `NOT_EXCLUDED` and `UNAVAILABLE`.
- [ ] S1: Excluded reason labels for all four codes, each with its Mentions (source name, link).
- [ ] S1: Rejected Addresses contain only `valid: false` entries, de-duplicated by address and reason, with the label for each of the four reasons and the Mention's source name.
- [ ] S1: no Attributions gives the "Powiązania (0)" state.
- [ ] S2: `null` activity; volume with both totals and with one `null` (unknown); transaction count; period from unsorted events; single event; no events; sort order.
- [ ] All rows are collapsed on entry. Every row, Punktacja and section opens and closes with Tab plus Enter/Space and shows a visible focus ring.
- [ ] No meaning is carried by colour alone: the band label and percent are text, and warnings have text and an icon.
- [ ] `npm run check` and `npm run build` pass from `frontend/`.
- [ ] Verified in the browser against the running backend with demo data (`./scripts/run-backend.sh`) with a clean console: Attributions at 40% / 55% "Średni" (*with `confidence-1` the demo-v1 levels are those of D9-008, e.g. 95% and 60%*), Punktacja, Wzmianki with working links, the Address Activity no-data message (G7: `addressActivity` is always `null`), and Excluded Addresses expanded. The Rejected section is empty because demo-v1 has no Rejected Address (G1). It is checked only through the unit test. Report it as read, not run.

## Notes

- **Proposals, to be reviewed by the lead.** *The Justification sentences and Punktacja labels below are for `provisional-1` and are superseded by Q45–Q46.* Justification: `MENTIONED_WITH_IDENTIFIER` "Adres pojawił się w źródle razem z identyfikatorem tej osoby."; `INDEPENDENT_SOURCES` "Adres pojawił się także w innych, niezależnych źródłach o różnej treści.". Punktacja labels: "Adres przy identyfikatorze osoby" and "Potwierdzenie w niezależnych źródłach". Event labels: `RECEIVED` "Otrzymano", `SENT` "Wysłano", `SWAP` "Zamiana", `CASH_OUT_SIGNAL` "Sygnał wyjścia". Weryfikacja adresu: "Poprawny format adresu", "Sieć: {network}", "Adres nie należy do znanego serwisu (giełdy, smart kontraktu, miksera, zbiórki)." for `NOT_EXCLUDED`, and the UNAVAILABLE sentence otherwise.
- Open: views.md shows the Punktacja label with the data that triggered it ("… (GitHub)"). Should the line carry the source names of the Mentions? The rule code alone does not say which Mention triggered it.
- Open: the band for 0–9% is not defined in the challenge text. `provisional-1` never gives it, but the mapping needs a value (no label, or "Niski / szum").
- Open: when a Rejected Address appears in several Mentions, is it shown with one source name (which one) or with all of them?
- Open: story 53 says empty sections "say so or not take space". Which one applies to Excluded (0) and Rejected (0)?
- Open: what does Weryfikacja adresu show when no `detectedAddresses` entry matches the Attribution's address?
- Open: views.md Implementation Decisions shows the Excluded Addresses as a table with rows that do not expand. The newer "Report view against the real contract" section (rows expand to Mentions) wins. Confirm that the table layout stays.
- Open: number and date format for the activity figures (decimals of volume, date-only or date and time for the period). Not decided.

## Resolved by the lead (2026-11-21)

The open questions of this task were answered in Q38–Q44; see the table "Decisions after the task review" in [../spec.md](../spec.md). Those answers win over the open questions and proposals above.

## Changed after merging confidence-1, #58 (2026-11-21)

Q45–Q48 in [../spec.md](../spec.md) win over this task: Punktacja labels cover the 13 `confidence-1` codes, Uzasadnienie is the backend `justification` text, and the Mentions are read from `mentions`.
