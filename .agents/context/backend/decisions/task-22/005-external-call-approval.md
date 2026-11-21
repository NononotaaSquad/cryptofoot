# D22-005: External call on by default with a kill switch

- **Status:** accepted (mentor question 4 still open)
- **Date:** 2026-11-21
- **Type:** owner decision under an open external question
- **Source:** task sections 5.7 and 9 (no data to unapproved external services); `BUSINESS-SPEC.md` open question 4; E5-D4, E7-D4
- **Proposed by:** AI agent (Claude) · **Approved by:** Maksym U., grilling 2026-11-21. **Not approved by mentors.**

## Decision
- The bytecode check (D22-003) is on by default: `app.exclusion.onchain-check-enabled: ${EXCLUSION_ONCHAIN_CHECK_ENABLED:true}`.
- When it is switched off, no address leaves the application. Listed addresses are still excluded; unlisted ETH addresses become `unclassified` (honest, no fabricated result); BTC is unaffected.
- Data minimisation either way: the label lists are checked first, so known addresses are never sent; only the bare address is sent (no Case, identifiers, Mentions or results); BTC addresses are never sent.

## Consequences and risks
- Mentors may rule that a public RPC is not an approved service. Then set the variable to `false` and state it as a limitation in the report and presentation.
- What is sent (a public blockchain address) is public data, but it reveals to the RPC operator that someone is interested in that address.

## Verification
`ExclusionClassifierServiceSpec` "with the on-chain check switched off ...".

## Reconsider when
Mentors answer question 4.
