# D22-002: Label lists: imported Etherscan dataset + own list

- **Status:** accepted
- **Date:** 2026-11-21
- **Type:** owner decision (issue #22 open decision "where does the classification data come from")
- **Source:** brief KROK 3.1 (Address Tags / Name Tags); task section 5.1 (licences of components must be listed); owner requirement: only free / keyless services
- **Proposed by:** AI agent (Claude) · **Approved by:** Maksym U., grilling 2026-11-21

## Problem and context
The brief suggests asking Etherscan or Blockchair for name tags. Etherscan needs an API key for every call, and name tags are not in its public API. Blockchair has no exchange tags for BTC and rate-limits keyless clients (during this task it answered `430 ... IP address is temporary blacklisted`). No keyless tag API is reliable enough to depend on.

## Options considered
1. A small hand-curated list.
2. Import an open dataset (chosen by the owner).
3. A tag API at run time (WalletExplorer for BTC). Rejected: one more flaky external call, and every BTC address would be sent out.

## Decision
- **Dataset:** `backend/src/main/resources/exclusion-labels/etherscan-labels.json` is the verbatim `data/etherscan/combined/combinedAllLabels.json` of github.com/brianleect/etherscan-labels (MIT; licence copied next to it as `etherscan-labels.LICENSE`). Upstream commit `923aba72c7e2d0682f7ae6194b6140bd90668dc9`, file SHA-256 `c07828909e75c39f49b99b9f21514662b052ab1fbd37ccdfa03b089a73bf3353`. ETH only, 29 945 addresses, scraped 2023-06-18. Tags become reasons only through the reviewed table of D22-006, which yields 855 Excluded Address labels (640 EXCHANGE, 129 FUNDRAISER, 86 MIXER; counts pinned in `ExclusionLabelDirectorySpec` and cross-checked with an independent script). The file is never edited by hand, so it can be refreshed by replacing it.
- **Own list:** `own-labels.json` (`network, address, reason, name, reference`), owned by the team. It covers BTC (absent from the dataset), demo fixtures (E7) and corrections: an own entry overrides the dataset entry for the same address.
- **Only confirmed entries** (owner decision 2026-11-21): an own entry is added only when a label service confirms it. Initial content: `1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s` Binance, confirmed by the WalletExplorer label lookup on 2026-11-21. Four widely published but unconfirmed attributions were considered and left out: `34xp4vRoCGJym3xR7yCVPFHoCNxv4Twseo` and `bc1qm34lsc65zpw79lxes69zkqmk6ee3ewf0j77s3h` (Binance cold wallets), `bc1qgdjqv0av3q56jvd82tkdjpy7gdp9ut8tlqmgrpmv24sq90ecnvqqjwvw97` (Bitfinex cold wallet), `357a3So9CbsNfBBgFYACGvxxS6tMaDoa1P` (Ukraine donations; the ETH donation address is in the dataset). They can be added once confirmed, with the confirming source as `reference`.
- Both lists are keyed by the canonical address of ADR 0002 (ETH and bech32 lowercased, Base58 unchanged). Upstream has 173 addresses written twice in different letter case; the first one by sorted key wins.

## Consequences and risks
- Labels date from 2023: newer exchange wallets are missing. New contracts are still caught by bytecode (D22-003); new EOA exchange wallets are not.
- About 3 MB in the jar, parsed once at startup (the count is logged).
- The README lists the dataset and its licence (task section 5.1).

## Verification
`ExclusionLabelDirectorySpec`: the real bundled files (Kraken → EXCHANGE, Tornado Cash router → MIXER, Ukraine donation → FUNDRAISER, own BTC entries); every own entry passes `AddressValidator` in canonical form; case-duplicate handling; an own entry overrides the dataset.

## Reconsider when
Mentors approve a tag service, or the dataset is refreshed (update the commit, hash and date here and in `ExclusionLabelDirectory.ETHERSCAN_SOURCE`).
