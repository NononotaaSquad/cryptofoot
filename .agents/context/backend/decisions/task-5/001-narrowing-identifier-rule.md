# D5-001: A Case needs at least one narrowing identifier; a full name never suffices

- **Status:** accepted (requirement of the accepted E1 epic and BUSINESS-SPEC; implementation details are in D5-004)
- **Date:** 2026-11-21
- **Type:** requirement from the brief of the team (not an own invention)
- **Source:** `epics/E1-creating-a-case.md` stories 1-3 (business rules), `BUSINESS-SPEC.md` M1 ("Narrowing identifiers (at least one is required)", "Full name ... alone cannot start an Analysis"); issue #5 acceptance criteria
- **Proposed by:** E1 epic (team); implemented by AI agent (Claude) for the assignee of #5
- **Approved by:** accepted epic status in E1 (team); no separate approval of the implementation

## Problem and context
The Analysis must not cover random people. E1 defines narrowing identifiers (nickname, login, e-mail, messenger ID, profile address) and full name as additional data. The glossary term for the profile address is "profile address"; the contract field is `profileUrl`.

## Options considered
A class-level Bean Validation constraint; a check in the service; one constraint per field.

## Decision
A class-level constraint `@NarrowingIdentifier` on `CreateCaseRequest` requires at least one non-blank value among `nickname`, `login`, `email`, `messengerId` and `profileUrl`. `fullName` is accepted and stored but does not count. Missing, empty and white-space-only values do not count. The rejection names the required kinds (D5-005).

## Consequences and risks
- The full name is stored with the Case and passed on to the Analysis input, as E1 says it helps in judging findings.
- Nothing prevents a meaningless value (for example `x`) as a nickname; no business rule asks for more.

## Verification
`NarrowingIdentifierSpec` (single identifier, full name alone, empty and white space), `CaseCreationSpec`.

## Related
- Files: `util/NarrowingIdentifier.kt`, `dto/Requests.kt`. Issues: #5. Decisions: D5-002, D5-005.

## Reconsider when
The team changes the list of narrowing identifiers or lets a full name start an Analysis with another condition.