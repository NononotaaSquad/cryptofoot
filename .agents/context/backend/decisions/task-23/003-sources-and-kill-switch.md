# D23-003: Synthetic fixture first, then keyless self-hostable explorers, live off by default

- **Status:** accepted (owner); mentor question 4 ("approved external services") still open
- **Date:** 2026-11-21
- **Type:** owner decision under an open mentor question
- **Source:** task section 2 ("Eksploratory łańcucha bloków (API Web3)"), 5.7 (no test data or Analysis results to unapproved services), 9 (KAS environment without public cloud dependency); E7-D1 (synthetic demo plus a separate real-source check); D22-005 (kill switch precedent)
- **Proposed by:** AI agent (Claude)
- **Approved by:** Maksym U., grilling 2026-11-21. Not approved by mentors.

## Problem and context
Address Activity needs chain data. The demo-v1 addresses are real mainnet addresses (`1A1zP1…` is the Bitcoin genesis address), so live data would show their real history under a fictional person and the demo would not be repeatable. A scored Attribution address is an Analysis result: sending it to a third party may break section 5.7 until the mentors approve the service.

## Options considered
1. Synthetic fixture for the demo addresses, plus live keyless explorers behind a kill switch.
2. Synthetic fixture only; live as a follow-up.
3. Live only.
4. For live: mempool.space (BTC) + Blockscout (ETH), or Blockchair (both networks, already used as the bytecode fallback).
5. Live on by default (like D22-005), or off.

## Decision
Options 1, mempool.space + Blockscout, **off by default**.

- Sources are tried in order; the first that covers the address and answers decides: `SyntheticActivitySource` (only the addresses in `fixtures/demo-v1-activity.json`, active with `sources.synthetic.enabled`, the same switch as the synthetic Mention source), then `MempoolActivitySource` (BTC) and `BlockscoutActivitySource` (ETH) when `app.address-activity.live-enabled` is true.
- `ADDRESS_ACTIVITY_LIVE_ENABLED` (default `false`) switches the live explorers on. *Production default changed to on by [D23-012](012-live-explorers-in-production.md) (2026-11-22); local runs and tests stay off.* `app.address-activity.mempool-url`, `blockscout-url` and `timeout` are configurable, so KAS can point them at its own instances: both explorers are open source and self-hostable, which keeps the target deployment free of public cloud services (section 9).
- Data minimisation: only the bare address is sent (no Case, identifiers, Mentions or scores); fixture addresses and 0% Attributions are never sent; counterparties are labelled from the offline lists inside the application.
- Requests: mempool `GET /api/address/{a}` and `/api/address/{a}/txs`; Blockscout `GET /api/v2/addresses/{a}/counters`, `/transactions`, `/token-transfers?type=ERC-20`. Each stops at the first failed call.

## Consequences and risks
- With the default configuration every Attribution outside the fixture is `UNAVAILABLE`, so a real Analysis is `PARTIAL` (D23-005). This is the honest state until the mentors approve a service.
- Blockchair was not chosen: closed source, a keyless limit of about 1440 requests a day, and no BTC counterparties without extra calls.
- Synthetic data is always labelled (`synthetic = true`, source `synthetic`, no transaction links), and is never presented as a live observation (E7.2).
- The public mempool.space and Blockscout instances may rate-limit or change their API; failures give `UNAVAILABLE`, never zero.

## Verification
`AddressActivityServiceSpec` (order of sources, failures); `AddressActivityLiveSpec` (real wiring against a local stub: configuration binding, both parsers, unavailable explorer, fixture addresses and 0% Attributions never sent); `AnalysisFlowSpec` (`liveEnabled` is false by default).

## Related
- Files: `config/AddressActivityConfig.kt`, `service/AddressActivitySources.kt`, `analysis/synthetic/SyntheticActivitySource.kt`, `util/ExternalHttp.kt` (HTTP helper shared with the bytecode check), `resources/application.yaml`. Decisions: D22-005, E7-D1.

## Reconsider when
Mentors answer question 4. If they approve the explorers, set `ADDRESS_ACTIVITY_LIVE_ENABLED=true` (or a self-hosted URL); if not, keep it off and state the limitation in the presentation.
