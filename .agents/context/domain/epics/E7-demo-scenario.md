# E7. Demo scenario

**Status:** controlled synthetic demo plus separate real-source verification confirmed by the team on 2026-11-21; dependencies and organiser acceptance remain open. **Module:** all.

Business references: [BUSINESS-SPEC.md](../BUSINESS-SPEC.md), [BUSINESS-CONTEXT.md](../BUSINESS-CONTEXT.md).
Challenge reference: [task](../hacknation-2026-task/MF%20-%20Pełna%20Treść%20Zadania%20Konkursowego.md), sections 3, 4, 5, 6 and 9.

## Goal

The team demonstrates a complete, repeatable Analysis and can explain its expected results, limits and protection against incorrect Attributions using anonymised or synthetic data.

## Required outcome

The mandatory demonstration covers input, OSINT search, address detection and verification, Confidence Level calculation, rejection of incorrect assignments, selected blockchain indicators, and sources and final results. These are required stages, not seven independently weighted scoring criteria.

E4 is a dependency of the complete demonstration. A missing E4 stage must be recorded as an unmet requirement, not removed from the scenario.

## User stories and acceptance criteria

### E7.1. Prepare a scenario with known expected results

As a team, we want controlled test data, so that expected outcomes are defined before the demonstration.

- Use a fictional subject and documented anonymised or synthetic source content. Mark the demonstration data and its provenance clearly.
- Cover the networks and address formats actually supported by the application. Do not label an address as valid merely because it resembles one.
- The scenario specifies inputs, source content, expected address validation, expected classifications, applied scoring rules and exact expected scores under a named algorithm version.
- Expected values are derived from the agreed E3 rules. E7 does not invent its own scoring algorithm.
- Blockchain events, labels and timestamps have documented origins. Synthetic responses are clearly distinguished from live blockchain observations.

### E7.2. Demonstrate the full flow

As a jury member, I want to follow the Analysis from input to report, so that I can assess the required stages.

| Step | Action | Visible result | Dependency |
|---|---|---|---|
| 1 | Enter the fictional subject's identifier and start an Analysis | Case and Analysis identified; data mode visible | E1 |
| 2 | Execute source searches | Mentions with content context, provenance and collection times | E2 |
| 3 | Inspect extracted candidates | Correct address detection and validation; invalid candidate rejected | E2/E3 |
| 4 | Inspect an Attribution | Confidence Level, supporting Mentions, applied rules and contributions | E3/E5 |
| 5 | Inspect an excluded candidate | Rejection reason and classification source | E3/E5 |
| 6 | Inspect Address Activity | Selected chronological events and a potential cash-out indicator, with limitations | E4/E5 |
| 7 | Open the report | Facts, calculations and conclusions clearly separated | E5 |

- Repeating the scenario with the same fixture set and algorithm version reproduces the expected outcome.
- Proposed additional short sequence: open an earlier Analysis and compare it with a later one to show E6. A separate check verifies periodic execution without waiting a full production interval during the film; any shortened test interval is clearly labelled.
- A recorded or mocked output is never presented as evidence that a live external integration succeeded.

### E7.3. Verify adverse cases

As a team, we want cases that can defeat naive matching, so that the demonstration is supported by meaningful validation.

| Case | Expected behaviour |
|---|---|
| Address genuinely published as the subject's receiving address | Expected Attribution and score according to E3 |
| Weakly supported address | Lower score explained by the same rules |
| Known exchange or contract address | No direct personal Attribution; exclusion reason retained |
| Address quoted in a warning about another person | No ownership inferred from occurrence alone |
| Same nickname used by an unrelated person | No automatic identity merge |
| Copied Mention on another source | No automatic independent-source bonus |
| Malformed address candidate | Validation failure visible; no accepted Attribution |
| Unavailable source or classification service | Partial/unknown state; no fabricated zero score or absence conclusion |
| Two Analyses with an actual change and a source outage | Actual change and uncertain absence distinguished in E6 |

These cases validate the shared flow; they are not all required scenes in the three-minute film. Missing upstream behaviour is recorded as a known gap rather than changed to an artificially successful expected result.

### E7.4. Prepare repeatable presentation materials

As a team, we want a rehearsal script and submission checklist, so that the demonstrated result can be reproduced and submitted.

- Proposed film timing: 0:00–0:20 input; 0:20–0:55 search and validation; 0:55–1:35 scoring and exclusion; 1:35–2:10 blockchain indicators; 2:10–2:40 report and comparison; 2:40–3:00 limitations and result. Rehearse and shorten to remain within the required three-minute maximum.
- Document prerequisites, fixture version, expected outcomes and how to reset this demonstration Case without deleting unrelated Cases.
- Use the team-confirmed controlled fixture mode for the repeatable demo, clearly labelled. Document actual public-source fetching in a separate integration check. Mentor acceptance of this arrangement for the mandatory demonstration remains to be confirmed.
- Submission check: title and detailed project description including search parameters; PDF presentation of at most 10 slides; video of at most 3 minutes in an openly accessible repository; code repository link; README and component licence documentation.
- The film does not replace the separately required demonstration on participant-prepared data.
- No test data or Analysis results are sent to unapproved external services, including during recording or fixture preparation.

## Dependencies and scope

- Start fixture preparation alongside E1. Integrate E2/E3 as soon as address validation and scoring rules are agreed; agree a minimal E4 scenario before recording.
- Confirmed by the team on 2026-11-21: controlled synthetic responses for repeatability, plus a separately documented check of actual public-source fetching. This team decision does not establish mentor acceptance or that fixture mode alone satisfies the mandatory presentation.
- E7 prepares fixtures, expected results and demonstration materials. The product owners of E1-E6 remain responsible for their underlying behaviour.

## Decisions to confirm

| ID | Question and proposed resolution | Owner | Needed before |
|---|---|---|---|
| E7-D1 | TEAM CONFIRMED: controlled synthetic demo plus separate real-source integration verification. Mentor acceptance of this arrangement for the mandatory presentation remains open. | Team decision recorded; mentor validates acceptance | Before final rehearsal |
| E7-D2 | Which network, blockchain indicator and transaction references will E4 deliver? *Owner answer for demo-v1 ([D23-007](../../backend/decisions/task-23/007-demo-fixture.md)): BTC and ETH; totals and cash-out signals (stablecoin swap, exchange, P2P) from a synthetic fixture with fictional transaction ids; the demo ETH address is hidden while the bytecode check excludes it (D23-009 A1).* | E4 owner and mentor | Fixture completion |
| E7-D3 | Which E3 algorithm version and exact expected scores are frozen for rehearsal? | E3 owner | Expected-result completion |
| E7-D4 | Which source/search/API/AI services are approved for test queries and results? | Mentor / organiser | External calls |
| E7-D5 | Who records the film, prepares the slides and submits the required links? | Team | Final rehearsal |

## Documentation reconciliation

BUSINESS-SPEC.md must reflect the required E4 demonstration stage and the required E6 capabilities when this proposal is adopted. Required stages must not be described as separately weighted points without a published scoring rule.
