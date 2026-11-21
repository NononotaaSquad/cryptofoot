# D22-004: EIP-7702 delegated accounts are private accounts

- **Status:** accepted
- **Date:** 2026-11-21
- **Type:** owner decision; **deviation from the literal rule of the brief**
- **Source:** brief KROK 3.2 ("bytecode != 0x → not a private account"); EIP-7702 (live on Ethereum mainnet since the Pectra upgrade, May 2025)
- **Proposed by:** AI agent (Claude) · **Approved by:** Maksym U., grilling 2026-11-21

## Problem and context
Since EIP-7702, a normal user account (an EOA whose private key the person holds) can delegate to contract code. `eth_getCode` then returns the 23-byte delegation designator `0xef0100 || <20-byte address>`, which is not `0x`. Applied literally, the brief's rule would exclude real personal wallets as smart contracts and hide exactly the leads the tool looks for. Observed on 2026-11-21: the well-known personal address `0xd8da6bf26964af9d7eed9e03e53415d37aa96045` returns `0xef01005a7fc11397e9a8ad41bf10bf13f22b0a63f96f6d`.

## Decision
Code that is exactly the delegation designator (`^0xef0100[0-9a-f]{40}$`, case-insensitive) is a private account (`ContractCodeCheck.ACCOUNT`). Anything else that is not `0x`, including a designator followed by more bytes, is a contract.

## Consequences and risks
- The brief's rule still applies to every real contract. The deviation is explainable to the jury: a delegated account still has its own private key.
- Blockchair may report a delegated account as `contract` without `contract_code_hex`, which would classify it `SMART_CONTRACT`. This is unverified and affects only the fallback.

## Verification
`ContractCodeSourcesSpec` (designator in lower and upper case → account; designator plus an extra byte → contract). Live case in `ExclusionClassifierWiringSpec` (the delegated account above is accepted).
