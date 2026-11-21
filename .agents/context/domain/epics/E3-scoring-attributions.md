# E3. Scoring Attributions

**Status:** accepted with a change (no text Justification) · **Module:** M3a. Scoring: whose address · [BUSINESS-SPEC.md](../BUSINESS-SPEC.md)

## Goal

Join the findings together and judge them for the Analyst: every address gets a Confidence Level and a reference to the Mention that the result comes from.

## User stories

1. As an Analyst, I want to see a Confidence Level (0–100%) for every address, so that I know which addresses to deal with first.
2. As an Analyst, I want a reference to the Mention next to every Confidence Level, so that I can check the source myself.
3. As an Analyst, I want to see all Mentions in which the same address appeared, so that I know whether more than one source confirms it.
4. As an Analyst, I do not want to get addresses of exchanges, smart contracts, mixers and fundraisers as Attributions, so that I do not lose time on false leads.
5. As an Analyst, I want the same set of Mentions to always give the same Confidence Level, so that I can rely on the result.

## Out of scope

- A Justification as a text description.

## Open questions

- Should we show Excluded Addresses, and how? (mentors)
- Is a Confidence Level with a reference enough as "pointing to premises and scoring rules" (Z p.9)? (mentors)
- Which Confidence Level bands do we use? (team)
