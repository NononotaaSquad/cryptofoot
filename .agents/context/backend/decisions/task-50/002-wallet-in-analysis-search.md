# D50-002: What the Analysis does with the wallet: search for Mentions of it

- **Status:** accepted (the choice by the task owner; the contract with the E2 and E3 owners is not agreed)
- **Date:** 2026-11-21
- **Type:** decision chosen by the user; requirement of the competition brief; own interpretation of the answer
- **Source:** issue #50 decision 2 (answer: "it must search according to the content of the task"); competition brief section 2 (the tool searches the public sources and finds the cryptocurrency addresses related to an entered identifier); `epics/E2-finding-mentions.md`; issue #30; `analysis/SearchService.kt`
- **Proposed by:** AI agent (Claude) offered A (context only), B (search for Mentions of the address) and C (B plus new identifiers); the user answered in words
- **Approved by:** the user (task owner), in the working session of 2026-11-21 (answers to the five questions of issue #50); not confirmed by the team, by the owners of E2 and E3, or by the mentors. **Interpretation of the answer by the agent:** option B. Option C (deriving new identifiers of the person from the findings) is not included; confirm if C was meant.

## Problem and context
The issue asked for the wallet to be passed to the Analysis flow. Passing it as an identifier already means a search by it: `SearchService.findMatch` treats an identifier value that looks like an address as an address search (exact text in the content). A probe with the synthetic demo-v1 fixtures on the current `master`:

| Identifier value | Mentions found |
|---|---|
| BTC address as stored by the validator | 2 |
| ETH address in the canonical (lower case) form | **0** |
| the same ETH address in its original EIP-55 spelling | 2 |

## Options considered
A. Context only (the wallet is filtered out before the search). B. Search for Mentions of the address. C. B plus use of the found context to obtain further identifiers.

## Decision
Option B. The Case hands the wallet to the Analysis flow as `CaseIdentifier(WALLET_ADDRESS, <canonical address>)` (`CaseIdentifierSource`) and the Analysis stores it among the identifiers used. This task adds **no** search code and no scoring. The wallet is the Analyst's input (D50-006); the Mentions found by the search are source facts.

## Dependencies on other owners (not agreed, not approved by them)
- **E2 (Maksym Z., #30):** the stored wallet is canonical (ETH in lower case, original text not kept), so the search must match addresses by canonical form (compare the detected candidate and the identifier after validation), otherwise ETH wallets find nothing (probe above). The E2 spec (#30) lists the direction address to person as out of scope: its owner must update or accept this.
- **E3 (Maksym U. #22 merged, Marcin #9 not merged):** candidates found next to the wallet go through the classification flow; scoring must not treat the wallet as evidence (D50-006).
- **Frontend (#7):** the form needs the `walletAddress` field.

## Consequences and risks
- Until E2 matches by canonical form, searching by an ETH wallet finds nothing; the result would wrongly look like "no Mentions". This is a known gap, not hidden by a test.
- Addresses found beside an entered wallet may belong to other people; only the evidence rules of E3 may turn them into Attributions.
- The Analysis flow is still not wired in the application (D24-005, D27-009), so no search by a wallet runs there yet.

## Verification
`CaseCreationSpec` ("the identifiers handed to the Analysis flow include the canonical wallet", "a saved Analysis keeps the wallet among the identifiers used"). The probe above was a one-off run, not a committed test.

## Related
- Files: `analysis/AnalysisContracts.kt` (`IdentifierType.WALLET_ADDRESS`), `service/CaseService.kt`. Issues: #7, #9, #22, #30, #50. Decisions: D50-001, D50-006, D24-006.

## Reconsider when
The E2 owner agrees a contract for the search by address, or the task owner meant option C.