# 0004. Blockchair contract check (KROK 3.2) as an external classification source

**Status:** Accepted by the task owner · 2026-11-21. Amends [0003](0003-excluded-address-classification.md) ("no external calls"). Issue #22. Mentor question 4 (approved services) is still open.

## Context

KROK 3 of the brief asks the application to reject false positives by (1) querying a public API for address tags (Etherscan, Blockchair) and (2) checking whether an address has executable code (`bytecode != 0x`). ADR 0003 covered only an own list. The task owner decided the application must perform the external checks itself and chose Blockchair.

What the providers actually offer (docs read 2026-11-21):
- **Etherscan** `module=nametag&action=getaddresstag`: "a PRO+ endpoint ... available exclusively to the Pro Plus plan" (paid).
- **Blockchair** (API v2.0.80 docs): `GET /ethereum/dashboards/address/{address}` returns `data.{address}.address.type` = `account` | `contract`, no key needed (free: 1440 requests/day, hard limit 30/min; `402`/`429` on limit, `430`/`434`/`503` when blocked; key via `?key=`). **No endpoint returns owner/exchange tags or labels.**

## Decision

- `BlockchairContractSource` ("Blockchair contract check", ETH only) implements `AddressClassificationSource`. One GET per ETH candidate; `contract` → Excluded Address `SMART_CONTRACT`, source `"Blockchair contract check: contract code present"`; `account` → not excluded.
- Any HTTP error, rate limit, block, timeout, unparsable body or missing/unknown `type` → `ClassificationUnavailableException`. The service then marks unlisted ETH candidates `unavailable` (ADR 0003 rules); it never guesses `account`.
- **KROK 3.1 (tags) stays with the own list** (ADR 0003): Blockchair has no tags and Etherscan's tag API is paid. A key-gated Etherscan tag source can be added as one more `AddressClassificationSource` if a Pro Plus key appears.
- Sources are ordered: own list (`@Order(1)`, more specific reason, e.g. EXCHANGE for a hot-wallet contract) before Blockchair (`@Order(2)`).
- Configuration (`application.yaml`, prefix `classification.blockchair`): `enabled` (default **true**, env `CLASSIFICATION_BLOCKCHAIR_ENABLED`), `base-url` (env `CLASSIFICATION_BLOCKCHAIR_BASE_URL`), `api-key` (env `BLOCKCHAIR_API_KEY`, optional), `timeout` 10 s. When disabled the bean is not created and only the own list runs.
- Only canonical ETH addresses leave the application (no identifiers, Mentions or results). Tests use a local stub HTTP server and never reach the internet.

## Consequences and risks

- **Observed on 2026-11-21:** from the team's network, Blockchair answered `430` "Your IP address is temporary blacklisted ... Please apply for an API key". Without a key, ETH candidates not on the own list are `unavailable` and the Analysis is `PARTIAL` (correct, but weak for the demo). Mitigation: obtain a Blockchair key, or add a plain Ethereum JSON-RPC `eth_getCode` source (keyless public node or KAS's own node; also satisfies section 9 "no public-cloud dependency").
- Sending addresses to Blockchair must still be accepted by mentors (brief section 5.7); switch off with `CLASSIFICATION_BLOCKCHAIR_ENABLED=false`.
- One request per address with a 30/min free limit; large Analyses will hit the limit and become `PARTIAL`. Unavailability is per source, so a failure mid-way marks all unlisted ETH candidates `unavailable`.
- BTC has no contract concept; BTC exclusion relies on the own list only.

## Verification

`BlockchairContractSourceSpec` (stub server): contract vs account, only ETH sent, key passed, 429/430/500/bad JSON/missing type/unreachable → unavailable, and end-to-end with the own list an unknown contract becomes an Excluded Address.

## Reconsider when

A Blockchair key or an Etherscan Pro Plus key is available, mentors reject Blockchair, or KAS provides its own Ethereum node.
