---
name: hackathon-kickoff
description: >
  Use at the start of the hackathon, once the challenge is known: reads the raw challenge
  materials in .agents/context/domain/, grills the team with domain questions, fills product.md, glossary.md and domain-model.md, then
  slices user stories with the hamburger method into a domain-oriented backlog in 3 phases (P1 minimum, P2 ready product, P3 enhancements)
  and creates the GitHub Project with labels, milestones, domain epics and task issues. Also use
  to add tasks to the board later, or when the user says "kickoff", "create the board",
  "plan the tasks" or "create the GitHub project".
---

# Hackathon kickoff

Turn the challenge into shared context and a GitHub Project the team can pull tasks from.
Work in this order and do not skip the checkpoints: the context is the source for every task,
and the board is outward-facing.

## 1. Read the sources

1. List every file in `.agents/context/domain/` (ignore `.gitkeep`) and read all of them:
   Markdown, text, JSON, CSV, PDFs (use `pages` for long ones), images, slides. This folder holds
   the raw challenge materials the team dropped in: brief, judging criteria, sample data,
   partner API docs, meeting notes. They are the source of truth. When two files disagree,
   the newer one or the organizers' document wins; record the conflict in Open decisions.
2. Read `.agents/context/product.md`, `glossary.md` and `domain-model.md`. If they are already
   filled, treat them as decisions the team made: update, do not rewrite.
3. Skim `.agents/context/backend/backend-architecture.md` and
   `frontend/frontend-architecture.md` so tasks fit the stack (MongoDB, `/api/v1`, generated
   client, Spock, Vitest). The template already provides scaffolding, CI and deployment:
   never plan tasks to set those up.

If `domain/` is empty and `product.md` is still a template, stop and ask the team to drop the
challenge brief into `.agents/context/domain/` or paste it. Never invent a challenge.

## 2. Grill the team

Before writing anything, interview the team about the domain, following
[`references/grilling.md`](references/grilling.md): rounds of up to 4 concrete,
scenario-based questions with recommended options, follow-ups on every vague answer, new
noun or contradiction, until its exit criteria hold. Expect 20-40 questions. Do not ask what
the sources already answer; confirm it instead. Record every answer in
`.agents/context/domain/kickoff-qa.md`.

Never fill a gap with a guess: a fact in the context comes from a source file, a team answer,
or an explicit `assumed` default the team accepted.

## 3. Update the context

Fill the files in place, keeping their headings. Short bullets, no prose padding.

- **`product.md`**: every section. Copy judging criteria verbatim with weights and name the
  source file. Write the demo scenario as numbered `Screen -> Action -> Result` steps; tasks
  will reference them by number. Each invariant is one testable sentence.
- **`glossary.md`**: one entry per domain concept, replacing the placeholders. Pick one name,
  list the synonyms found in the sources under `_Avoid_`.
- **`domain-model.md`**: domains (bounded contexts), their aggregates, entities, value
  objects, lifecycle states and transitions, relationships, and which invariant each
  aggregate protects. Use glossary names only.

Whatever the grilling left unanswered goes into product.md `Open decisions` with its
recommended default. Then show a short summary of what changed and get a go-ahead before
planning.

## 4. Plan the backlog

Write `.agents/context/backlog.json` using the shape of
[`backlog.example.json`](backlog.example.json). The script validates and renders it, so every
issue gets the same format.

### Domains

`domains` lists the domains from `domain-model.md` (name, kebab-case slug, one-sentence
description). Add a `Delivery` domain for work the jury sees that is not a feature: seed data,
demo script, README, pitch assets. Each domain becomes an epic issue; its tasks become
sub-issues.

### Slice with the hamburger method

Split work with Gojko Adzic's
[hamburger method](https://gojko.net/2012/01/23/splitting-user-stories-the-hamburger-method/):
the first bite goes through every layer of the hamburger, and later bites make individual
layers better. Record each hamburger in `hamburgers`; the script checks it and draws it as a
table in the domain epic.

1. **Stories.** One hamburger per user goal in the demo scenario (usually a group of
   adjacent demo steps), e.g. `Patient is seen in a fair order`. Set `domain` to the domain
   that owns the outcome.
2. **Steps (layers).** Write the 3-7 steps the user and the system go through to reach the
   goal, in order, in domain words: `Check in`, `Order the Queue`, `Call the patient`. Steps
   are business steps, not technical layers (no `Database`, `API`, `UI`).
3. **Options per step.** For each step list 2-5 ways to do it, from crude to complete. Vary
   quality: hardcoded/manual vs automated, one vs many, happy path vs validated, plain vs
   polished, rule-of-thumb vs exact. Take options from the sources, the judging criteria and
   the invariants.
4. **Trim.** Mark an option `"discarded": "<reason>"` when it costs more than a hackathon
   allows, needs an unapproved external service or hardware, or another option gives the same
   value for less. Keep discarded options in the file; they answer "why not X?" for the jury.
5. **First bite = P1.** Pick the cheapest option of every step so the whole story works end
   to end. Every step needs a phase 1 option; a story missing a layer is not demoable.
6. **Next bites = P2, P3.** P2 gets the options the demo needs to be convincing and correct
   (invariants, validation, edge cases from product.md). P3 gets options that raise the score
   on a judging criterion. Options of the same step in a later phase replace or extend the
   earlier one.
7. **Options become tasks.** Point each chosen option at the task that delivers it (`task`).
   A P1 task usually delivers the first-bite options of 1-3 adjacent steps as one vertical
   slice. A P2/P3 task usually upgrades one step to one better option. Every task outside the
   `Delivery` domain must deliver at least one option.
8. **Grill the hamburgers.** Before writing tasks, run the questions from "Grilling the
   hamburgers" in [`references/grilling.md`](references/grilling.md): missing steps, whether
   each P1 option is good enough for the demo, which option impresses the judges, which
   discards they agree with. Ask again whenever an acceptance criterion needs a value
   (message, limit, format) that no source or answer gives.

### Task format: clean and precise

A task is read by a teammate or an agent who was not in the planning discussion. It must say
exactly what to build and how to check it, and nothing else. The script rejects tasks that
break the rules marked ✓.

- **Title** ✓: `<Domain>: <actor> <verb> <object>` in glossary words, at most 70 characters,
  no final punctuation. `Visit: patient checks in with name and reason`, not
  `Visit: check-in functionality` or `Create Visit entity`.
- **Goal** ✓: one sentence, `As a <actor>, I can <capability> so that <value>.`
- **demoStep** ✓ (P1, P2): starts with `Step <n>` from product.md or `All steps`.
- **Acceptance criteria** ✓: 1-5 items, each `Given <state>, when <action>, then <observable
  result>`. Use concrete values: example data, statuses, HTTP codes, the exact Polish message.
  Every invariant and edge case the task must enforce is a criterion; there are no separate
  rules or edge-case lists. More than 5 criteria means the task is two tasks.
- **Scope** ✓: the artifacts the task creates or changes, by name: `@Document` and fields,
  endpoint with verb and path, request/response DTOs, page route, generated hook, component.
  `backend` is required for area backend, `frontend` for frontend, both for fullstack. Scope
  says *what*, not *how*: no step-by-step instructions.
- **outOfScope**: things a reader might reasonably assume are included but are not.
- **No vague words** ✓: `properly`, `correctly`, `handle`, `support`, `improve`, `nice`,
  `user-friendly`, `etc`, `TBD` and similar are rejected; write the behavior instead.
- **One task, one outcome**: no "and also" tasks, no copy of product.md, no definition of
  done checklist (it lives once in AGENTS.md).
- `area`: `fullstack` by default. Split into `backend` and `frontend` tasks only so two people
  can work in parallel; both stay named by capability and the frontend one depends on the
  backend one.
- `size` ✓: `S` (≤1h) or `M` (≤3h). Anything bigger must be split.
- `dependsOn` ✓: keys of earlier tasks in the same or an earlier phase only.
- `assignees`: GitHub logins, only if the team assigned them.

The resulting issue is short: one meta line, Goal / Demo / Slice / Blocked by, acceptance
checklist, scope, out of scope. See `P1-01` in [`backlog.example.json`](backlog.example.json).

### Phases

| Phase | Purpose | Contents | Typical size |
|---|---|---|---|
| **1 Minimum** | Something demoable end to end as early as possible | The first bite of every hamburger: the cheapest option of each step. Seeded or hardcoded data is fine. Plain shared UI components. No edge cases beyond what breaks the path. | 4-8 tasks |
| **2 Ready product** | What the jury sees | Bites that make every demo step complete and correct: every invariant enforced and tested; validation with Polish messages; empty, loading and error states; edge cases from product.md; plus Delivery: demo seed data, rehearsal, README. | 6-12 tasks |
| **3 Enhancements** | Extra score if time remains | Further independent, cuttable bites mapped to judging criteria (UX polish, extra insight, accessibility, performance). Ordered by score gain per hour. | 4-10 tasks |

P1 and P2 tasks must set `demoStep` to the demo step they serve. A P3 task should name the
judging criterion it raises in its `goal`.

### Coverage check

Before showing the plan, verify and fix:
- every demo step belongs to a hamburger, and every hamburger step has a P1 option;
- every demo step is covered by P1 (happy path) and finished by P2;
- every invariant appears in some acceptance criterion by the end of P2;
- every judging criterion is served by at least one task;
- nothing in product.md `Out of scope` became a task;
- P1 alone can be demoed.

## 5. Validate and confirm

```bash
node .agents/skills/hackathon-kickoff/scripts/create-board.mjs --dry-run
```

Fix every validation error. Then show the team the hamburger tables the dry run prints
(step × phase, with discarded options) followed by one compact table per phase
(`key | title | area | size | depends on`). The hamburgers are the thing to discuss: moving an
option between phases or discarding it is the cheapest way to cut scope. Apply their changes,
re-run the dry run, and ask for an explicit go-ahead: the next step creates issues and a
project visible to the whole organization.

## 6. Create the board

```bash
gh auth status   # token scopes must include 'project'
# missing? ask the user to run:  ! gh auth refresh -s project
node .agents/skills/hackathon-kickoff/scripts/create-board.mjs
```

The script uses the repo from `git remote` (override with `"repo": "owner/name"`) and creates
the project under the repo owner (override with `project.owner`). It creates or reuses:

- a GitHub Project linked to the repo, with single-select fields `Phase`, `Domain`, `Area`,
  `Size` (plus the default `Status`);
- labels `phase:1-minimum`, `phase:2-product`, `phase:3-enhancement`, `domain:<slug>`, `epic`;
- milestones `Phase 1: Minimum`, `Phase 2: Ready product`, `Phase 3: Enhancements`;
- one `Epic: <Domain>` issue per domain, with its hamburger tables linked to the task issues;
- one issue per task with the standard body (including the hamburger slice it delivers),
  labels, milestone, parent epic, `blocked by` links to its dependencies, and project fields
  set.

It writes `project.number`, `project.url`, `domains[].epic` and `tasks[].issue` back into
`backlog.json` after each step. A re-run skips what exists, so after a failure fix the cause
and run it again; to add tasks later, append them to `backlog.json` and re-run. Never delete
issues or the project to start over without the team's confirmation.

## 7. Report

Give the project URL, task counts per phase, the open decisions still blocking anything, and
suggest a board view grouped by `Phase` and a second one grouped by `Domain`. Offer to commit
the updated context and `backlog.json` with the `git-commit` skill
(`docs(context): add challenge context and backlog`).
