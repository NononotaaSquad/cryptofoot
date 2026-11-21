# D5-005: API contract and errors of Case creation

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** own proposal within the accepted contract
- **Source:** `backend/decisions/0001-epic-api-contract.md` (`POST /api/1/cases` returns 201 `CaseResponse`); `rules.md` (Polish messages, DTOs, error format); E1 story 3
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
The endpoint and DTOs already exist in the contract; the validation rule and the error text were open (`TODO` in `Requests.kt`).

## Options considered
Error attached to one field; one error per missing kind; one class-level error naming the kinds.

## Decision (proposed)
The existing endpoint is implemented without changing the schema (the OpenAPI snapshot and the generated client are unchanged). Success: 201 with the Case (`id`, `identifiers`, `createdAt`, `latestAnalysis` null). Rejection: 400 with the usual body: `message` "Popraw bledy w formularzu." and one error with `field` null and the message "Podaj przynajmniej jeden identyfikator zawezajacy: pseudonim, login, adres e-mail, identyfikator w komunikatorze lub adres profilu. Samo imie i nazwisko nie wystarcza." (with Polish diacritics in the code). An invalid e-mail format is a separate error on `email`. The field name stays `profileUrl` (existing contract), shown to the user as "adres profilu". Logs contain the Case id and the kinds of identifiers present, never their values.

## Consequences and risks
- The frontend gets one general error; it must show it near the identifier fields (the error has no field).
- `listCases`, `getCase` and `startAnalysis` stay `TODO` stubs (E6).

## Verification
`CaseCreationSpec` (status, body, message), `OpenApiContractSpec`, `npm run check`.

## Related
- Files: `controller/CaseController.kt`, `dto/Requests.kt`, `util/NarrowingIdentifier.kt`. Issues: #5. Decisions: D5-001, D5-004.

## Reconsider when
The frontend owner needs per-field errors or other texts.