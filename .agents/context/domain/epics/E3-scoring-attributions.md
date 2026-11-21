# E3. Scoring Attributions

**Status:** accepted with a change (the Justification is a short template text; changed on 2026-11-21, earlier: no text Justification) · **Module:** M3a. Scoring: whose address · [BUSINESS-SPEC.md](../BUSINESS-SPEC.md)

## Goal

Join the findings together and judge them for the Analyst: every address gets a Confidence Level and a reference to the Mention that the result comes from.

## User stories

1. **Get the Attributions result: every address with a Confidence Level and references to its Mentions**
   As an Analyst, I want to get the joined result as Attributions, where every address has a Confidence Level (0–100%) and references to all Mentions in which it appeared, so that I know which addresses to deal with first and can check every source myself.
   Business rules: every address gets a Confidence Level from 0 to 100% — the higher it is, the sooner I deal with the address; next to every Confidence Level there is a reference to the Mention the result comes from, and all Mentions with the same address are shown together, so that I can see whether more than one source confirms it; the Justification is a short plain-language text built from a template of the scoring rules that applied, without numbers, shown together with the references to the Mentions; addresses of exchanges, smart contracts, mixers and fundraisers are filtered out as Excluded Addresses and never come back as Attributions, so that I do not lose time on false leads; the same set of Mentions always gives the same Confidence Level, so that I can rely on the result.

## Out of scope

- A Justification written by an AI model.
- Numbers of the scoring rules (base value, contributions) and the algorithm version in the text shown to the Analyst.

## Open questions

- Exact wording of the Justification template: one sentence per scoring rule. (team)

- Should we show Excluded Addresses, and how? (mentors)
- Is a Confidence Level with a reference enough as "pointing to premises and scoring rules" (Z p.9)? (mentors)
- Which Confidence Level bands do we use? (team)
