# D50-003: One wallet address per Case

- **Status:** accepted (by the task owner; not confirmed by the team)
- **Date:** 2026-11-21
- **Type:** decision chosen by the user among offered options (recommended option)
- **Source:** issue #50 decision 3; the request for "a field"
- **Proposed by:** AI agent (Claude) offered one address or a list; the user chose one address
- **Approved by:** the user (task owner), in the working session of 2026-11-21 (answers to the five questions of issue #50); not confirmed by the team, by the owners of E2 and E3, or by the mentors

## Problem and context
A person may use a Bitcoin and an Ethereum wallet.

## Options considered
One optional field `walletAddress`; a list `walletAddresses` (per element errors, duplicate rule, list limit, several form rows in #7).

## Decision
One optional field `walletAddress` in `CreateCaseRequest`, `PersonIdentifiersResponse` and the Case record. A person with two wallets needs two Cases or chooses one.

## Consequences and risks
- Changing to a list later is a contract change (field type) and a data migration.
- The form (#7) needs one text field.

## Verification
`CaseCreationSpec`, the generated client (`walletAddress?: string | null`).

## Related
- Files: `dto/Requests.kt`, `dto/Responses.kt`, `model/CaseRecord.kt`. Issues: #7, #50. Decisions: D50-005.

## Reconsider when
Analysts regularly need several wallets for one person.