# D5-002: Normalization of the input: trim, blank means missing, nothing else

- **Status:** proposed
- **Date:** 2026-11-21
- **Type:** own proposal
- **Source:** issue #5 (white-space-only values must not satisfy the requirement); E1 (no further rules)
- **Proposed by:** AI agent (Claude)
- **Approved by:** nobody yet

## Problem and context
White-space-only values must count as missing. A white-space-only e-mail would otherwise also fail the e-mail format check and produce a second, confusing error.

## Options considered
Trim in the service after validation; trim at deserialization so validation and storage see the same value; no trimming.

## Decision (proposed)
Every text field is trimmed when the JSON is read (`TrimToNull`): leading and trailing white space is removed and an empty result becomes absent. The trimmed value is stored. No other change: letter case is kept, no format check other than the existing e-mail format of the contract, no length limit, no format check of the profile address, no uniqueness.

## Consequences and risks
- Whoever creates a `CreateCaseRequest` without JSON (a test or another service) bypasses the trimming; the validator still treats blank values as missing.
- Unbounded length: an oversized value is accepted (no rule from the business); this is a security hardening to decide later.
- A `null` JSON value and a missing field are the same thing.

## Verification
`CaseCreationSpec` (white space only gives exactly one error; trimmed values stored; blank optional fields not stored).

## Related
- Files: `util/TrimToNull.kt`, `dto/Requests.kt`. Issues: #5. Decisions: D5-001.

## Reconsider when
The team defines value formats, limits or case-insensitive matching.