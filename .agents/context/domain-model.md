# Domain Model

Agents: read this file with `product.md` before implementing any feature. It describes what exists in the domain, not how it is stored or rendered. Use only names from `glossary.md`. Distilled from the raw materials in `domain/`; when they change, update this file.

Status: empty. Filled by the `hackathon-kickoff` skill at the event.

## Domains

One bullet per domain (bounded context): name and the one responsibility it owns. Every backlog task belongs to exactly one domain.

## Aggregates

Per aggregate: its root, the entities and value objects inside it, and the invariants from `product.md` it protects.

### Case (see decisions `D5-001` to `D5-005` and `D50-001` to `D50-007`)

- Root: one Case for one investigated person, with a stable identifier that Analyses and the schedule refer to (`caseId`).
- Inside: the identifiers entered (nickname, login, e-mail, messenger ID, profile address, wallet address, full name) and the creation time.
- Invariants: at least one narrowing identifier (nickname, login, e-mail, messenger ID, profile address or one cryptocurrency wallet address) is required; a full name is additional data and never suffices; blank values count as missing; a wallet address (Bitcoin or Ethereum, stored in canonical form) is the Analyst's input, never a Mention, independent confirmation or evidence of ownership. Cases are never merged or deduplicated.

### Analysis (proposed, see decisions `D25-001`, `D25-002`; not yet approved by the team)

- Root: one Analysis, one stored record with its own identifier, referencing its Case by `caseId`.
- Inside: trigger, status, start and end time, identifiers actually used, networks and sources covered, algorithm version, Mentions (with source, URL, collection time, context), rejected candidates, Attributions and Excluded Addresses (referencing Mentions by id), failures and limitations.
- Invariants: a later Analysis never overwrites an earlier one; a finished Analysis (completed, partial, failed) is never changed; opening it never reruns it; a failure is stored as failed, never as zero findings.

### Analysis comparison (proposed, see decisions `D26-001` to `D26-003`; E6-D2 not confirmed)

- Two finished Analyses of one Case are compared by network and canonical address. Categories: new Attribution, Attribution not observed later, changed Confidence Level, changed exclusion classification.
- `Not observed` is not `no longer belongs to the person`: it is flagged unverifiable unless the later Analysis was completed and rechecked the supporting sources and the network.
- Differences in identifiers, source coverage, networks, algorithm version or completeness are listed as comparability limitations.

### Schedule (proposed, see decisions `D27-001` to `D27-009`; E6-D3 still open in the epic)

- One schedule per Case: enabled or disabled, a predefined cadence (default daily 06:00), fixed zone Europe/Warsaw, next execution, last attempt.
- Each due term starts an ordinary Analysis with trigger `SCHEDULED`. Attempt outcomes: started, skipped because an Analysis was running, skipped because the term was missed during downtime (not made up), failed to start.
- A shortened demo cadence exists only in demo mode and is labelled.

### Excluded Address classification (see decisions `D22-001` to `D22-007`; mentor question 4 open)

- Each valid candidate ends in exactly one of: accepted, Excluded Address (`ExclusionReason` + classification source + its Mentions), unclassified.
- Order: offline label lists (exchange, mixer, fundraiser) -> ETH bytecode via keyless public services (code = smart contract; an EIP-7702 delegated account is still a private account) -> unclassified when no check could be made. An unlisted BTC address is accepted.
- An Excluded Address never reaches the Confidence Level algorithm; an unclassified candidate is scored but flagged `UNAVAILABLE` (D24-006).

### Attribution scoring, algorithm `confidence-1` (see decisions `D9-001` to `D9-008`)

- Every accepted or unclassified candidate becomes one Attribution with a Confidence Level 0–100, the applied scoring rules with their contributions, and a Justification: a short Polish text built from one template sentence per applied rule, with no numbers. The Mentions it rests on are listed next to it.
- A Mention supports an address only if it names the person by a non-wallet narrowing identifier (the full name and the Analyst's wallet never count), does not quote the address in a warning, and holds no other person's e-mail. No supporting Mention gives 0%.
- Base = strongest supporting source kind: own profile 80, sale or settlement 60, discussion 40, mass comment 10. +15 once when two supporting Mentions differ in origin (host) and in the sentence that holds the address; repeats on one service and copies give nothing.
- Invariant: the same input (candidates, Mentions, identifiers) always gives the same Confidence Level, rules and text; the version is stored with the Analysis.

### Address Activity (see decisions `D23-001` to `D23-009`; mentor questions 2, 4 and 5 open)

- Part of the Analysis: a stage after scoring; the outcome is stored on each Attribution in the snapshot and never fetched again when the report is reopened.
- Only Attributions with a Confidence Level above 0 are checked. Status per Attribution: `AVAILABLE` (totals, period, cash-out signals, source, synthetic flag, time read, coverage limitations), `UNAVAILABLE` (no source answered), `NOT_CHECKED` (0%, or an Analysis stored before E4).
- Sources in order: the synthetic demo fixture (demo addresses only), then keyless self-hostable explorers (mempool.space BTC, Blockscout ETH), which are off by default.
- Cash-out signal: an outgoing transfer to a labelled off-ramp (exchange, P2P, payment gateway, crypto card) at any amount, or a swap into a USD stablecoin of at least 1000 units. A signal is a lead, not proof of a fiat withdrawal. The rules have a version (`activity-1`), stored with each Address Activity together with the threshold in use.
- Invariants: unknown data is never shown as zero activity (a total is stated by the source or summed from a complete history, otherwise empty); unavailable activity makes the Analysis `PARTIAL` with `BLOCKCHAIN_DATA_UNAVAILABLE`, never `FAILED`.

## Lifecycles

States and allowed transitions for each aggregate that has a lifecycle, with who or what triggers each transition.

### Analysis (proposed, see decisions `D24-001`, `D24-002`; not yet approved by the team)

- States: `RUNNING` -> exactly one of `COMPLETED`, `PARTIAL`, `FAILED`. Terminal states never change; a rerun is a new Analysis.
- Trigger (separate from status): `MANUAL`, `SCHEDULED`.
- `COMPLETED`: every stage ran and nothing was unavailable. Zero Mentions from healthy sources is still `COMPLETED` ("no results").
- `PARTIAL`: some sources, classifications or Address Activity (D23-005) were unavailable; the rest of the result is kept. Candidates with unavailable classification are still scored but their Attribution is flagged `classificationStatus = UNAVAILABLE` (D24-006).
- `FAILED`: no usable Attributions can be produced (all sources unavailable, or the search, candidate preparation, exclusion or scoring module failed). Retained in history; never shown as an Analysis with zero findings.
- Orchestration (#24) derives the status; storage of the Analysis (#25) is a separate responsibility.

## Relationships

How aggregates reference each other (by ID across aggregates) and the cardinality.

## Sources

Files in `domain/` this model is based on, and what each contributed.
