# OŁCyŚ – business specification

State after the business grilling session of 2026-11-21, updated with the team decisions of 2026-11-21 recorded in epics E5–E7 (see [5. Answered questions](#answered-questions)). Capitalised terms are defined in [CONTEXT.md](CONTEXT.md). This document does not describe technology: no stack, no architecture, no choice of tools.

References like "Z p.N" point to a page of the PDF "MF – Pełna Treść Zadania Konkursowego" (taken from the requirements map in `olcys-flow.html`).

## 1. Target problem

A KAS official who looks for the cryptocurrency addresses of an investigated person must go through many public sources by hand, then join the findings together and judge how reliable they are. In a wider analysis this takes a lot of time and overloads the official.

### The solution from the official's point of view

The Analyst enters the person's data and gets a report in the application: an expandable list of Attributions with address, Confidence Level, and a reference to the Mention that the result comes from. The result is a lead for further verification, not evidence.

Frame decisions:

- User: one Analyst, an official without IT knowledge, no login.
- Judges: a jury made of KAS officials.
- Direction: person to addresses. A cryptocurrency wallet address entered by the Analyst is also searched for (D50-001, D50-002, task owner's decision, team confirmation pending), but new identifiers of the person are not derived from the findings.
- Main rule: little noise. We do not show information that increases the number of false leads to analyse.

## 2. Business modules

Analogy: a detective agency. Boundary rule: **the field agent does not judge, the desk analyst does not search**.

| Module | Role in the analogy | Question it answers | What goes in | What comes out |
|---|---|---|---|---|
| **M1. Input data** | The client's order | Who do we investigate? | Person data entered by the Analyst | A Case with identifiers |
| **M2. Information search** | Field agent | What was found and where? | Identifiers from the Case | Mentions with detected addresses, source and time of collection |
| **M3a. Scoring: whose address** | Desk analyst | Is this the person's address? | Mentions with addresses | Attributions with Confidence Level and Justification; Excluded Addresses |
| **M3b. Scoring: what the address did** | Forensic accountant | What did this address do? | Attributions | Address Activity. **On hold until we talk to the mentors** |
| **M4. Official's view** | The file for the client | What follows from this and how do we know? | Results of M2 and M3 | A report in the application with an expandable list |

### M1. Input data

- The form accepts from one field up to the full set of data.
- **Narrowing identifiers** (at least one is required): nickname, login, e-mail, messenger ID, profile address, cryptocurrency wallet address.
- **Full name** is an additional field. Alone it cannot start an Analysis, because it is too general.
- A **cryptocurrency wallet address** can be entered as a narrowing identifier: one address, Bitcoin or Ethereum, validated and stored in canonical form (D50-001, D50-003, D50-004). It is the Analyst's own input, not a finding from a source: it is never a Mention, never an independent confirmation and does not by itself make an Attribution (D50-006). The Analysis searches the public sources for Mentions of it (D50-002).

### M2. Information search

- It is not a general internet search engine. It works on a fixed, short set of sources: **at most 2–3 public sources plus synthetic data**.
- Every Mention always has the full set: where it comes from, when it was collected, and how to return to it.
- It detects cryptocurrency addresses in the content of Mentions. It does not judge anything.

### M3a. Scoring: whose address

- Gives every address a Confidence Level (0–100%).
- The Justification is references to Mentions, with no text description.
- Filters out Excluded Addresses: exchanges, smart contracts, mixers, fundraisers.

### M3b. Scoring: what the address did

- The scope (volume, signals of exit to traditional currencies) is not accepted. The decision comes after the talk with the mentors.

### M4. Official's view

- Basic flow: the Analyst enters data and gets a report.
- The report is shown **in the application as an expandable list** (mentor feedback, confirmed by the team, E5-D1). Each Attribution is one list item: collapsed, it shows address and Confidence Level; expanded, it shows its Mentions, scoring details and available Address Activity. The exact grouping is still open.
- The report separates source facts, calculation results, and conclusions for verification (Z p.5).
- The Analyst can reopen the report of a saved Analysis in the application. Print, PDF export and passing the report outside the application are out of scope (E5-D2).
- Language without technical terms.

## 3. Value of the modules

| Module | What value it gives | What matters most |
|---|---|---|
| **M1. Input data** | The official starts work in a minute. The required narrowing identifier cuts off random people at the start | A simple form |
| **M2. Information search** | Takes the tedious walk through sources away from the official | Every Mention has a source and a time of collection |
| **M3a. Scoring: whose address** | Joins the findings together and judges them for the official | Confidence Level with a reference to the Mention, then filtering out addresses that must not be attributed |
| **M3b. Scoring: what the address did** | Shows whether real money and withdrawals stand behind the address | Withdrawal signals in time order (on hold) |
| **M4. Official's view** | A short answer instead of information overload | Little noise and understandable language |

## 4. Epics

The full user stories of each epic are in the [epics/](epics/) directory. Format: each user story has a name and a description in the form "As an Analyst, I want …, so that …". The description also states the business rules.

| Epic | Status | Module |
|---|---|---|
| [E1. Creating a Case](epics/E1-creating-a-case.md) | Accepted | M1 |
| [E2. Finding Mentions](epics/E2-finding-mentions.md) | Accepted | M2 |
| [E3. Scoring Attributions](epics/E3-scoring-attributions.md) | Accepted with a change | M3a |
| [E4. Address Activity](epics/E4-address-activity.md) | **On hold** until we talk to the mentors; a required stage of the demo (see Risks) | M3b |
| [E5. Report for the official](epics/E5-report-for-the-official.md) | Accepted; form confirmed: expandable list in the application | M4 |
| [E6. Returning to a Case](epics/E6-returning-to-a-case.md) | Accepted; manual and periodic runs are required, the cadence is open | M4 |
| [E7. Demo scenario](epics/E7-demo-scenario.md) | Accepted, starts in parallel with E1; synthetic demo plus a separate check on real sources | all |

**E1. Creating a Case**
- I enter the known data of a person, from one field up to the full set.
- Without a narrowing identifier I cannot start an Analysis, and I see what is missing.

**E2. Finding Mentions**
- I start an Analysis with one click.
- I get Mentions from a short set of sources and I see which ones were checked.
- For every Mention I see the source and the time of collection.
- I see which addresses appeared in which Mention.

**E3. Scoring Attributions**
- For every address I see a Confidence Level.
- For every Confidence Level I have a reference to the Mention.
- I do not get addresses of exchanges, smart contracts, mixers and fundraisers as Attributions.

**E4. Address Activity** (on hold)
- I see how much money went through the address.
- I see signals of withdrawal to traditional currencies in time order.

**E5. Report for the official**
- I get one result as an expandable list in the application instead of many separate sources.
- I can tell apart source facts, calculation results, and conclusions for verification.
- I can reopen the report of a saved Analysis in the application.

**E6. Returning to a Case**
- I start the Analysis again manually.
- Analyses also run periodically, without my action.
- I compare two Analyses.

**E7. Demo scenario**
- We have a fictional person with synthetic data who goes through the full demo.
- The data also contains an address to reject and an address with a low Confidence Level.
- The demo runs on controlled synthetic data, so it can be repeated. Fetching from real public sources is shown in a separate check.

### Out of scope for now

- Deriving new identifiers of the person from the findings about an entered wallet address (option C of D50-002). Entering a wallet and searching for it is in scope; it is not part of the demo (D50-007).
- General internet search outside the chosen sources.
- A Justification as a text description.
- Login and multiple users.
- Print, PDF export and passing the report outside the application.
- Assigning epics to people (a separate step before implementation).
- All technical decisions.

## 5. Questions

### Answered questions

| # | Question | Answer | Source |
|---|---|---|---|
| 3 | Which form of the result is most convenient for the official? | A report in the application with an expandable list. The earlier table is no longer a requirement. The exact grouping and the content of a list item are still open. | Mentor feedback, confirmed by the team on 2026-11-21 ([E5-D1](epics/E5-report-for-the-official.md#decisions-to-confirm)) |
| — | Can the Analyst pass the report on (print, PDF, sending)? | No, not in the current scope. The report can be reopened in the application. Export comes back only if someone asks for it. | Team ([E5-D2](epics/E5-report-for-the-official.md#decisions-to-confirm)) |
| — | Are scheduled runs and comparison an extra (risk "E6 as an extra")? | No. Manual and periodic runs are both required (Z p.2). | Team, 2026-11-21 ([E6-D1](epics/E6-returning-to-a-case.md#decisions-to-confirm)) |
| — | On which data do we run the demo? | On controlled synthetic data, so the demo can be repeated, plus a separately documented check of fetching from real public sources. The mentors still have to accept this for the required presentation. | Team, 2026-11-21 ([E7-D1](epics/E7-demo-scenario.md#decisions-to-confirm)) |

### Open questions for the mentors

1. Should we show Excluded Addresses to the official, and how: hidden, a collapsed section, or a full list? The team prefers to hide noise; the demo scenario has a required stage "rejecting wrong attributions" (Z p.5, Z p.6). *Proposal in [E5.3](epics/E5-report-for-the-official.md#e53-inspect-exclusions): a separate collapsed section with the number of exclusions, not confirmed.*
2. Should we show Address Activity, in what scope, and for which Attributions: all, above a threshold, or on request? The acceptance of E4 depends on this answer.
4. Which external sources and services are "approved", given the ban on sending data outside (Z p.4 item 7)? (also E5-D4, E7-D4)
5. What value counts as "significant" for signals of withdrawal to traditional currencies?
6. Is a Confidence Level with a reference to the Mention enough as "pointing to sources, premises and scoring rules" (Z p.9), or is a short description of the rule needed? *Proposal in [E5.2](epics/E5-report-for-the-official.md#e52-verify-one-attribution): show the applied scoring rules, their contributions and the algorithm version, without a text description. Not confirmed; it conflicts with the current definition of Justification.*
11. Do the mentors accept the synthetic demo plus a separate check on real sources as the required presentation? (E7-D1)

### Open questions for the team

7. Which 2–3 public sources do we choose? (the demo itself runs on synthetic data, see answered questions)
8. Which Confidence Level bands do we use: our own or the ones from the appendix to the task?
9. What counts as a "change" when comparing two Analyses? *Proposal in [E6.3](epics/E6-returning-to-a-case.md#e63-compare-two-analyses): a new Attribution, an Attribution not seen in the later Analysis, a changed Confidence Level, a changed exclusion. Not confirmed (E6-D2).*
10. When do we add the direction address to person? *Answered by the task owner on 2026-11-21: a wallet address may be entered, counts as a narrowing identifier and is searched for ([D50-001](../backend/decisions/task-50/001-wallet-as-narrowing-identifier.md), [D50-002](../backend/decisions/task-50/002-wallet-in-analysis-search.md)). Team confirmation, agreement of the E2 and E3 owners and mentor acceptance are still open; not part of the demo ([D50-007](../backend/decisions/task-50/007-demo-scope.md)).*
12. How often and at what time do periodic Analyses run, and can the Analyst change it? (E6-D3)

### Risks to decide on knowingly

- **E4 on hold**: analysis of blockchain indicators is one of the required stages of the demo (Z p.5) and an acceptance test (Z p.3). The stages are not scored one by one: the criterion "fit to the challenge and correct scenario" (25%) covers all of them. Without E4 this stage is an unmet requirement and the score in this criterion is lower.
