# D50-001: A wallet address is a narrowing identifier

- **Status:** accepted (by the task owner; not confirmed by the team)
- **Date:** 2026-11-21
- **Type:** decision chosen by the user among offered options; consistent with the competition brief, contrary to the team's earlier postponement
- **Source:** issue #50 decision 1; competition brief `MF - Pelna Tresc Zadania Konkursowego.md` section 2 (a public cryptocurrency address is among the identifiers); `BUSINESS-SPEC.md` M1 and "Out of scope" (before this change: "A cryptocurrency address is not input data"); `epics/E1-creating-a-case.md`; issue #30 (E2 spec, Out of Scope: "The direction address to person")
- **Proposed by:** AI agent (Claude) offered options A and B and recommended B; the user chose A
- **Approved by:** the user (task owner), in the working session of 2026-11-21 (answers to the five questions of issue #50); not confirmed by the team, by the owners of E2 and E3, or by the mentors
- **Supersedes in part:** [D5-003](../task-5/003-crypto-address-input.md) (the rule that the API has no field for a cryptocurrency address and that an address alone is no identifier) and [D5-001](../task-5/001-narrowing-identifier-rule.md) (only the list of narrowing identifiers, which now includes the wallet address)

## Problem and context
A Case could be created only from nickname, login, e-mail, messenger ID or profile address; a wallet address was explicitly not input data (D5-003). The Analyst needs to enter a wallet address.

## Options considered
A. The wallet is a narrowing identifier: a Case, and an Analysis, can start from the address alone.
B. The wallet is additional context like the full name; another narrowing identifier stays required (the recommended, smallest change).

## Decision
Option A. `walletAddress` counts as a narrowing identifier. A Case with only a wallet address is created; a full name alone is still rejected. An invalid wallet is rejected on its own field and does not satisfy the requirement (D50-005). The rejection message for a missing identifier now names the wallet as well.

## Consequences and risks
- A Case with only a wallet has no information about the person: any Attribution built from Mentions of that wallet cannot show that the person owns it (D50-006). The E1 aim "do not get results about random people" depends on the evidence rules of E3, not on this field.
- The spec of E2 (#30) says the direction address to person is out of scope; it must be updated by its owner (D50-002).
- This is a change of the team's earlier decision (BUSINESS-SPEC question 10); the team and the mentors have not confirmed it.

## Verification
`CaseCreationSpec` (a Case from a wallet alone, with other identifiers, with the full set), `NarrowingIdentifierSpec`.

## Related
- Files: `util/NarrowingIdentifier.kt`, `dto/Requests.kt`, `service/CaseService.kt`. Issues: #5, #7, #30, #50. Decisions: D5-001, D5-003, D50-002, D50-005, D50-006.

## Reconsider when
The team or the mentors reject the wallet as an identifier that can stand alone, or E2/E3 cannot support a Case with only a wallet.