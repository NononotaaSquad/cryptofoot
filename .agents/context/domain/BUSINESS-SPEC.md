# OŁCyŚ – business specification

State after the business grilling session of 2026-11-21. Capitalised terms are defined in [CONTEXT.md](CONTEXT.md). This document does not describe technology: no stack, no architecture, no choice of tools.

References like "Z p.N" point to a page of the PDF "MF – Pełna Treść Zadania Konkursowego" (taken from the requirements map in `olcys-flow.html`).

## 1. Target problem

A KAS official who looks for the cryptocurrency addresses of an investigated person must go through many public sources by hand, then join the findings together and judge how reliable they are. In a wider analysis this takes a lot of time and overloads the official.

### The solution from the official's point of view

The Analyst enters the person's data and gets a table of Attributions: address, Confidence Level, and a reference to the Mention that the result comes from. The result is a lead for further verification, not evidence.

Frame decisions:

- User: one Analyst, an official without IT knowledge, no login.
- Judges: a jury made of KAS officials.
- Direction: person → addresses only.
- Main rule: little noise. We do not show information that increases the number of false leads to analyse.

## 2. Business modules

Analogy: a detective agency. Boundary rule: **the field agent does not judge, the desk analyst does not search**.

| Module | Role in the analogy | Question it answers | What goes in | What comes out |
|---|---|---|---|---|
| **M1. Input data** | The client's order | Who do we investigate? | Person data entered by the Analyst | A Case with identifiers |
| **M2. Information search** | Field agent | What was found and where? | Identifiers from the Case | Mentions with detected addresses, source and time of collection |
| **M3a. Scoring: whose address** | Desk analyst | Is this the person's address? | Mentions with addresses | Attributions with Confidence Level and Justification; Excluded Addresses |
| **M3b. Scoring: what the address did** | Forensic accountant | What did this address do? | Attributions | Address Activity. **On hold until we talk to the mentors** |
| **M4. Official's view** | The file for the client | What follows from this and how do we know? | Results of M2 and M3 | A report in the form of a table |

### M1. Input data

- The form accepts from one field up to the full set of data.
- **Narrowing identifiers** (at least one is required): nickname, login, e-mail, messenger ID, profile address.
- **Full name** is an additional field. Alone it cannot start an Analysis, because it is too general.
- A cryptocurrency address is not input data (the direction address → person is postponed).

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
- Results are collected in a **table**: address, Confidence Level, reference to the Mention.
- The report separates source facts, calculation results, and conclusions for verification (Z p.5).
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
| [E4. Address Activity](epics/E4-address-activity.md) | **On hold** until we talk to the mentors | M3b |
| [E5. Report for the official](epics/E5-report-for-the-official.md) | Accepted | M4 |
| [E6. Returning to a Case](epics/E6-returning-to-a-case.md) | Extra, only if there is time | M4 |
| [E7. Demo scenario](epics/E7-demo-scenario.md) | Accepted, starts in parallel with E1 | all |

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
- I get one result as a table instead of many separate sources.
- I can tell apart source facts, calculation results, and conclusions for verification.
- I can pass the report on.

**E6. Returning to a Case** (extra)
- I start the Analysis again manually.
- I compare two Analyses.
- I set up scheduled runs.

**E7. Demo scenario**
- We have a fictional person with synthetic data who goes through the full demo.
- The data also contains an address to reject and an address with a low Confidence Level.

### Out of scope for now

- The direction address → person.
- General internet search outside the chosen sources.
- A Justification as a text description.
- Login and multiple users.
- Assigning epics to people (a separate step before implementation).
- All technical decisions.

## 5. Open questions

### For the mentors

1. Should we show Excluded Addresses to the official, and how: hidden, a collapsed section, or a full list? The team prefers to hide noise; the demo scenario has a scored step "rejecting wrong attributions" (Z p.5, Z p.6).
2. Should we show Address Activity, in what scope, and for which Attributions: all, above a threshold, or on request? The acceptance of E4 depends on this answer.
3. Which form of the result is most convenient for the official: table, graph, text report, a file for the case records? What do they see on the first screen?
4. Which external sources and services are "approved", given the ban on sending data outside (Z p.4 item 7)?
5. What value counts as "significant" for signals of withdrawal to traditional currencies?
6. Is a Confidence Level with a reference to the Mention enough as "pointing to sources, premises and scoring rules" (Z p.9), or is a short description of the rule needed?

### For the team

7. Which 2–3 public sources do we choose?
8. Which Confidence Level bands do we use: our own or the ones from the appendix to the task?
9. What counts as a "change" when comparing two Analyses?
10. When do we add the direction address → person?

### Risks to decide on knowingly

- **E4 on hold**: analysis of blockchain indicators is one of the 7 scored steps of the demo (Z p.5) and an acceptance test (Z p.3). Without E4 the score is lower in the criterion "fit to the challenge" (25%).
- **E6 as an extra**: scheduled analyses and comparison of results are a requirement of the task (Z p.2).
