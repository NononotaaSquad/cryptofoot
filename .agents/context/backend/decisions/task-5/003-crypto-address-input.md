# D5-003: What "a cryptocurrency address is not input data" means

- **Status:** accepted (by the task owner; not confirmed by the team)
- **Date:** 2026-11-21
- **Type:** decision chosen by the user among offered options; resolves a contradiction between sources
- **Source:** `BUSINESS-SPEC.md` M1 ("A cryptocurrency address is not input data (the direction address to person is postponed)") and "Out of scope"; `epics/E1-creating-a-case.md` Out of scope; the competition brief `MF - Pelna Tresc Zadania Konkursowego.md` section 2 (lists a public cryptocurrency address as a possible identifier); issue #5
- **Proposed by:** AI agent (Claude) offered three options; the user chose
- **Approved by:** the user (task owner), in the working session of 2026-11-21 (answer to the question about the crypto address rule)

## Problem and context
The competition brief allows an address as an identifier; the team spec postpones the direction address to person. Neither source says how the API must treat such a value.

## Options considered
No field and no detection; additionally reject an explicit address field; detect address-like values in every field.

## Decision
The team spec is followed: the API has **no field** for a cryptocurrency address and nothing is detected or rejected on value. Unknown JSON properties (for example `address`) are ignored as for any request, an address-like value in a text field (for example a nickname that looks like an address) is accepted as that text, and a request that has only an address and no narrowing identifier is rejected by the narrowing rule (D5-001).

## Consequences and risks
- An Analyst who types an address in the nickname field gets an Analysis on that text, not on the address; the UI form must not offer an address field (frontend task).
- Silent ignoring of an `address` property could surprise an API client; documented here and in the contract note.
- The competition brief lists the address as an identifier; this deviation from the brief is the team's postponement (BUSINESS-SPEC), to be explained to the mentors if asked.

## Verification
`CaseCreationSpec` ("a cryptocurrency address has no input field ..." and "values in the identifier fields are not scanned for addresses").

## Related
- Files: `dto/Requests.kt`. Issues: #5. Decisions: D5-001.

## Reconsider when
The direction address to person is added (BUSINESS-SPEC question 10).