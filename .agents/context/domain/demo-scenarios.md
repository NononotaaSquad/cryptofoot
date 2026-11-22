# Demo scenarios (synthetic data)

Ten fictional people in `backend/src/main/resources/fixtures/demo-v1.json` (`scenarios`), next to the original demo person Zenon Fikcyjny (`zenek_fx`, D9-008). Each one shows one behaviour of the tool: a correct result (happy path), a protection against a false lead (false positive), a known limit (false negative), or an edge case. Mentors and the jury can click through them in the application.

- **Fictional on purpose.** The task requires anonymised or synthetic test data (section 3, E7.1). None of these people exist, and no real person's name is used.
- **Keyless addresses.** The people's own addresses are derived from public seeds (`olcys/demo-v1/scenario/...`), so nobody holds a key for them and they cannot belong to anyone. The ETH address of S1 (`0xE73A…20c6`) was checked on mainnet: no code, no transactions.
- **Real service addresses.** S2 uses public exchange, mixer, fundraiser and contract addresses from the bundled label lists, because that is what the exclusion checks recognise.
- **Synthetic chain data.** Address Activity comes from `fixtures/demo-v1-activity.json` and is labelled "dane testowe".
- **Where it runs.** Synthetic data is on locally (profile `local`) and, after ADR 0003, on production. The expected values below assume the default configuration with the bytecode check on.
- **Frozen.** `DemoScenariosSpec` freezes every value below under algorithm `confidence-1` and Address Activity rules `activity-1`.

## How to run one

1. Open **Nowa sprawa**, fill in only the fields listed for the scenario, click **Rozpocznij analizę**.
2. In **Szczegóły sprawy**, expand each row of **Powiązania** and look at **Punktacja**, **Uzasadnienie**, **Wzmianki** and **Aktywność adresu**. Then check **Adresy wykluczone** and **Adresy odrzucone**.

Locally every Analysis also lists `synthetic-broken` as an unavailable source (the "source unavailable" scene), so the status is **Częściowa**. On production that source is off (ADR 0003).

## Catalogue

| # | Category | Enter | Expected | What it shows |
|---|---|---|---|---|
| S1 | Happy path | Nick `halka_eth`, E-mail `halina.przykladowa@example.com`, Adres profilu `https://social.example/halka_eth` | ETH `0xe73a…20c6` **95%** (własny profil + niezależne źródło). Aktywność: 18 ETH volume (12.5 received, 11.5 sent), 6 transactions; signals in time order: **zamiana na stablecoin 30 000 USDC**, **bramka płatnicza 12 000 USDC** (Bity.com), **P2P 1,5 ETH** (Remitano), **giełda 15 000 USDC** (Kraken 4) | The full path from Web2 trace to cash-out signals, including the off-ramp kinds taken from the label tags (D23-010) |
| S2 | False positive | Nick `bogdan_platnosci` | **Powiązania (0)**, **Adresy wykluczone (5)**: Binance (BTC) and Kraken 4 as exchanges, Uniswap router as a smart contract, Tornado.Cash as a mixer, a donation address as a fundraiser | A person who publishes only service addresses gets no Attribution (cheat sheet KROK 3) |
| S3 | False positive | Nick `celina_uczciwa`, Adres profilu `https://social.example/celina_uczciwa` | Own address **80%**; the impostor's address quoted in a warning **0%** with the warning sentence; its activity is not checked | An address quoted in a warning about another person is not evidence (E7.3) |
| S4 | False positive | Nick `darek_mazur`, E-mail `dariusz.mazur@example.com` | Own P2P ad **60%** with a **transfer 0,8 BTC to Binance**; the namesake's address (other e-mail) **0%** | Same nickname, different person: no automatic identity merge (E7.3) |
| S5 | Edge case | Nick `ewa_kopiowana` | **60%** with no +15: the same sentence copied onto another service; activity **0 BTC, 0 transactions** | A copy is not an independent source; real zero activity differs from missing data |
| S6 | Edge case | Nick `franek_literowka` | **Powiązania (0)**, **Adresy odrzucone (2)**: a BTC address and an ETH address with a wrong checksum | Validation rejects addresses that only look valid (closes DS1-007 G1 for the demo) |
| S7 | Edge case | Nick `grazka_konkursy` | **10%** (Niski / szum) from a giveaway comment; activity "Nie udało się pobrać danych… Brak danych nie oznacza braku aktywności." | Weak evidence stays low; missing blockchain data is never shown as zero |
| S8 | False negative | Nick `henio_ukryty` | **Powiązania (0)** although the person published two addresses | Known detection limits: an address split by spaces, and one more than 100 characters after the nickname (outside the 200-character context) |
| S9 | False negative | Nick `irena_hodl`, E-mail `irena.hodl@example.com`, Adres profilu `https://social.example/irena_hodl` | **80%**; activity 4 BTC in, 3,9 BTC out, **no cash-out signal** | A deposit to an exchange address that is not on the label lists is not recognised (`LABELLED_COUNTERPARTIES_ONLY`) |
| S10 | Edge case | Nick `kamil_nieobecny` | No Mentions and no Attributions; the source is checked, the Analysis is not failed | "No results" is not an error (E2.3) |

## With the bytecode check switched off

With `EXCLUSION_ONCHAIN_CHECK_ENABLED=false`, two results change:
- **S1:** the ETH address becomes an Attribution flagged "Nie sprawdzono, czy adres należy do serwisu". Its level is still 95%.
- **S2:** the Uniswap router is no longer excluded. It becomes an unclassified 40% Attribution with unavailable activity.

## Not covered by static data

- **Two Analyses with a real change** (E6.3). The fixture does not change between runs.
- **A real source outage.** Locally `synthetic-broken` stands in for it.
