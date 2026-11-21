# D50-004: Networks: Bitcoin and Ethereum, detected by the validator

- **Status:** accepted (by the task owner; the scope is fixed by ADR 0002)
- **Date:** 2026-11-21
- **Type:** confirmation of an existing decision
- **Source:** issue #50 decision 4; `backend/decisions/0002-address-candidate-validation.md` (`Network { BTC, ETH }`); `service/AddressValidator.kt`
- **Proposed by:** AI agent (Claude) confirmed it against the ADR and the code; the user confirmed
- **Approved by:** the user (task owner), in the working session of 2026-11-21 (answers to the five questions of issue #50); not confirmed by the team, by the owners of E2 and E3, or by the mentors

## Problem and context
Which networks may the wallet field accept, and who decides the network.

## Decision
Only `BTC` and `ETH`. The network is detected from the format by `AddressValidator` (Base58Check and bech32 for BTC, `0x` plus 40 hex for ETH; any valid `0x` address is ETH, other EVM chains are not distinguished). No network field and no other networks. Anything else fails with `UNSUPPORTED_NETWORK`.

## Consequences and risks
- A Litecoin, TRON or testnet address is rejected on the wallet field.
- The unused `networkHint` of the validator stays unused.

## Verification
`NarrowingIdentifierSpec` and `CaseCreationSpec` (supported and unsupported addresses).

## Related
- Files: `service/AddressValidator.kt`. Issues: #21, #50. Decisions: D50-005.

## Reconsider when
A new network is added to ADR 0002 (needs a validator branch and the owners' agreement).