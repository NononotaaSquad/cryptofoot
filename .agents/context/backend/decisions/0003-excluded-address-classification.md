# 0003. Excluded Address classification: own versioned list behind pluggable sources

**Status:** Accepted by the task owner · 2026-11-21. Amended by [0004](0004-blockchair-contract-check.md): the application now also calls Blockchair for the KROK 3.2 contract check. Issue: E3 "classify Excluded Addresses". Mentor question 4 (approved external services) is still open.

## Context

E3 requires that addresses of exchanges, smart contracts, mixers and fundraisers become Excluded Addresses with a stated reason and the source of the classification, never reach the Confidence Level algorithm and never come back as Attributions. An unavailable classification must be told apart from a confirmed exclusion, with no fabricated result (E5.3, E7.3). The brief suggests address tags (Etherscan, Blockchair) and a bytecode check (`eth_getCode`), but section 5.7 and section 9 forbid sending test data or results to unapproved external services, and which services are approved is an open mentor question.

## Decision

**Data source.** Our own versioned list `classification/excluded-addresses-v1.csv` (`network,address,reason,label`) read by `OwnListClassificationSource` ("Own list v1"). It contains only well-known publicly tagged addresses: CEX wallets (Binance, Kraken, Bitfinex), token/DEX contracts (USDT, WETH, Uniswap V2 Router), Tornado Cash and the Ukraine government donation address. No network calls.
- Every entry is validated with `AddressValidator` (checksums included) at startup; an invalid entry or a duplicate fails the application. This already caught one wrongly remembered BTC address, which was dropped rather than guessed.
- Any change to an entry bumps the version (file name + `LIST_VERSION`), so `classificationSource` stored with a saved Analysis stays reproducible.

**Pluggable sources.** `AddressClassificationSource { name, networks, findExcluded(addresses) }`. An external source (tags API, ETH bytecode check) is one more `@Component` once approved, with no change to the service. A source that cannot answer throws `ClassificationUnavailableException`; it never returns an empty map instead.

**Classification rules** (`AddressExclusionService.classify(List<AddressCandidate>) -> AddressClassifications`):

| Situation for one candidate | Result |
|---|---|
| Listed by any source that answered (first source in order wins) | `excluded`: reason + `classificationSource = "<source name>: <label>"` |
| Not listed; every source covering its network answered | `accepted` (NOT_EXCLUDED) |
| Not listed; a source covering its network is unavailable | `unavailable` |
| No source covers its network (or no source at all) | `unavailable` |

- A confirmed exclusion stays confirmed even if another source is down.
- "Not on our list" counts as checked-and-not-excluded (team answer); the result lists `checkedSources` and `unavailableSources` so the report can state what was checked.
- Exception messages are not logged (they may carry addresses).
- Input/output use the ADR 0002 model types (`AddressCandidate`, `CanonicalAddress`, `Network`), not the orchestrator's string-based `Candidate`.

**Unavailable candidates are scored and flagged** as proposed in D24-006 (kept by the task owner): `accepted + unavailable` go to the Confidence Level algorithm, the latter with `ClassificationStatus.UNAVAILABLE`; `excluded` never does.

## Options rejected

- ~~Calling Etherscan/Blockchair tags or a public RPC now~~: reversed by the task owner, see [0004](0004-blockchair-contract-check.md) (Blockchair contract check; tags stay on the own list because Blockchair has none and Etherscan tags are paid).
- Treating every ETH address as `UNAVAILABLE` without a bytecode check: would flag almost every lead and make every Analysis `PARTIAL`.
- Implementing the orchestrator's `ExclusionClassifier` port directly: it uses `network: String`; the adapter belongs to whoever wires the pipeline (D24-003, D24-005).

## Consequences

- Coverage is limited to the listed addresses; an unknown exchange or contract becomes an accepted candidate. This is the main weakness against "rejecting incorrect assignments" (25 % of the score) until an approved external source is added.
- E7 demo fixtures must use a listed address for the "known exchange or contract" case, or add a clearly labelled synthetic entry in a new list version.
- No API contract change: `ExcludedAddressResponse(reason, classificationSource)` and `ClassificationStatus` already exist.
- Not wired yet: an adapter from `ExclusionClassifier` (#24) to `AddressExclusionService`, and mapping `checkedSources`/`unavailableSources` to `ReportLimitation.CLASSIFICATION_UNAVAILABLE`.

## Reconsider when

A tag API key becomes available (add a source, bump nothing else), or the team decides an ETH address without a bytecode check must be `UNAVAILABLE`.
