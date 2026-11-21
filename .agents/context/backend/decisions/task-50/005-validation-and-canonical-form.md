# D50-005: Validation, errors and canonical form of the wallet

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** own proposal; contract change
- **Source:** issue #50 (reuse `AddressValidator`, store the canonical form, errors on the right field); `backend/decisions/0002-address-candidate-validation.md`; `rules.md` (Polish messages); D5-002, D5-005
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
The wallet must be validated with the existing validator, not a second one, and stored in the form that other modules match on.

## Options considered
Validate in the service and fail with one general error; validate with a constraint on the field (error on the field); store the original text next to the canonical form.

## Decision (proposed)
- `@ValidWalletAddress` on `CreateCaseRequest.walletAddress` calls `AddressValidator` and gives a Polish error on the field `walletAddress` per failure: `MALFORMED` "Adres portfela ma nieprawidłowy format.", `CHECKSUM_MISMATCH` "Adres portfela ma nieprawidłową sumę kontrolną.", `UNSUPPORTED_NETWORK` "Nieobsługiwany format adresu portfela. Obsługiwane są adresy Bitcoin (BTC) i Ethereum (ETH).". A blank value is treated as missing (trimmed at deserialization, D5-002).
- `CaseService` stores only the canonical address (the original text is not kept, as requested). The network is not stored; it is derived from the canonical address by the validator.
- The missing-identifier message names the wallet: "Podaj przynajmniej jeden identyfikator zawężający: pseudonim, login, adres e-mail, identyfikator w komunikatorze, adres profilu lub adres portfela kryptowalutowego. Samo imię i nazwisko nie wystarcza." (replaces the text of D5-005).
- REST contract: new optional `walletAddress` in `CreateCaseRequest` and `PersonIdentifiersResponse` (therefore also in every `AnalysisResponse.identifiers`); `api-sync` run and the client regenerated.

## Consequences and risks
- The validator runs twice for a valid wallet (the constraint, then `CaseService` for the canonical form).
- `WalletAddressValidator` creates its own `AddressValidator` (the class has no dependencies) so it also works without Spring.
- The frontend (#7) must show the field and its errors; the missing-identifier error has no field.
- An uppercase bech32 address is stored lower case; an ETH address is stored lower case (the EIP-55 checksum is checked on input, then dropped).

## Verification
`NarrowingIdentifierSpec`, `CaseCreationSpec` (BTC Base58, BTC bech32, ETH, invalid, checksum mismatch, unsupported, canonical form stored, trimming), `OpenApiContractSpec`, `npm run check`.

## Related
- Files: `util/ValidWalletAddress.kt`, `util/NarrowingIdentifier.kt`, `service/CaseService.kt`, `frontend/apps/web/lib/api/`. Issues: #7, #21, #50. Decisions: D5-002, D5-005 (message text), D50-004.

## Reconsider when
The frontend needs the network shown or the original spelling kept, or per-field errors for the missing-identifier rule.