# Grilling the team

The sources never describe the domain completely, and every gap you fill with a guess becomes
a wrong task. Interview the team until the domain is unambiguous. Expect 20-40 questions in
total across several rounds; that is normal and cheaper than one wrong P1.

## How to ask

- **Rounds of up to 4 questions** with AskUserQuestion, most blocking first. One round, then
  update `kickoff-qa.md`, then the next round built on the answers.
- **Never ask what the sources answer.** Confirm instead: "`brief.pdf` p.3 says a Visit can be
  cancelled only by the patient. Does a nurse also cancel?"
- **Concrete scenarios beat abstract questions.** Not "How are conflicts handled?" but "Two
  nurses mark the same Visit urgent within a second. What does the second one see?"
- **Offer 2-4 concrete options** drawn from the sources, the recommended one first with
  "(Recommended)" and a one-line reason in its description. The team picks or writes their own.
- **Say why it matters** in the question: what the answer decides (a field, a state, a task,
  a phase).
- **Drill down.** A vague answer ("depends", "usually", "something like"), a new noun, or a
  contradiction with the sources or an earlier answer gets an immediate follow-up. Do not move
  on with a fuzzy answer.
- **Hunt synonyms.** When the sources or the team use two words for what might be one thing
  (`ticket`/`request`/`case`), ask whether they are the same and which name wins.
- **"You decide" / "skip"** means: take the recommended option, record it as an assumption
  (marked `assumed`) in `kickoff-qa.md` and product.md `Open decisions`, and keep going.
- **Stop** when the exit criteria below hold or the team says stop. Unanswered questions go to
  `Open decisions` with the recommended default, an owner and a deadline.

## Record every answer

Append each round to `.agents/context/domain/kickoff-qa.md` (create it on the first round):

```markdown
## Round 2 – Visit lifecycle

- **Q:** Can a CALLED Visit go back to WAITING when the patient does not show up?
  **A:** No, it becomes NO_SHOW after 5 minutes; the doctor calls the next one. (team)
- **Q:** Is "appointment" the same as a Visit?
  **A:** No: an appointment is booked in advance and out of scope. (assumed)
```

The file lives in `domain/`, so later kickoff runs and other agents read it as a source.
Then reflect the answer in product.md, glossary.md or domain-model.md right away.

## Question bank

Work through every area. Skip a question only when the sources already answer it clearly.

### 1. Challenge and judging
- Who judges (business, technical, the partner)? What would make them say "this is the winner"?
- Exact weights of each criterion? Which one is the tie-breaker?
- Mandatory deliverables: live demo, pitch deck, video, repository, documentation? Length of the demo?
- Hard constraints: required technology, provided data or API, offline, language of the UI?
- Is the partner's real process expected, or is a simplified one acceptable?

### 2. Actors and problem
- Who are all the actors? Which one is the hero of the demo?
- What does each actor do today, step by step, and where exactly does it hurt (time, errors, cost)?
- What measurable outcome do we promise (minutes saved, errors avoided)?
- Which actor can see or change what? Is there any login, or are roles picked on a start screen?

### 3. Language
- For every noun in the sources: what is it, in one sentence? Is it the same thing as `<synonym>`?
- Which word does the real user say (Polish UI copy) and which name do we use in code?
- Are there words the partner uses with a meaning different from everyday use?

### 4. Things and identity
- What identifies each thing (generated ID, number shown to users, natural key)? Is that number unique per day, per user, globally?
- Which attributes are required at creation, which come later, which are computed?
- Who creates it, who changes it, can it be deleted or only closed?
- One or many: can an X have several Y? Can a Y exist without an X?
- What happens to Y when its X is closed or removed?

### 5. Lifecycle
- What states does it go through? What is the first and the final state?
- Who or what moves it between states (an actor, time, another event)?
- Can it go back to an earlier state? Can it skip one?
- What happens on a timeout (nobody acts for N minutes)?

### 6. Rules and calculations
- Which rules must never be broken? What exactly happens when someone tries?
- Every calculation: exact formula, units, rounding, currency, time zone, which data is included?
- Ordering and priority rules: what wins on a tie?
- Limits: maximum counts, lengths, amounts, time windows?

### 7. Edge cases (ask as scenarios)
- Duplicate: the same actor does the same thing twice.
- Concurrency: two actors change the same thing at once.
- Empty: nothing exists yet; the list is empty; the first use of the day.
- Too much: 1000 items; a very long text; a value at the limit.
- Invalid: missing data, wrong format, a value outside the allowed range.
- Gone: the referenced thing was closed or removed in the meantime.
- Not allowed: an actor tries something outside their role.

### 8. Data
- Where does the demo data come from: provided dataset, realistic seed, typed live?
- How much data makes the demo convincing? Real names or fake ones?
- Is there personal or sensitive data we must not store or must anonymize?
- Formats of provided files; which fields are reliable and which are dirty?

### 9. Integrations
- Which external systems appear in the brief? Do we call them for real, mock them, or skip them?
  (External services need the team's approval per `.agents/rules/rules.md`.)
- If mocked: what exact responses does the demo need?

### 10. Demo and scope
- Which screen does the demo start on? What is the "wow" moment and at which step?
- What must never fail live? What can be pre-seeded instead of typed?
- What will we deliberately not build? What would the judges forgive missing?

## Grilling the hamburgers

While slicing (SKILL.md step 4), run at least one more round per story:
- "Story X has steps A → B → C. Is a step missing or in the wrong order?"
- "For step B, is the crude option `<P1 option>` acceptable in the demo, or do the judges need `<P2 option>`?"
- "Which option of step C would impress the judges most per hour of work?"
- "We plan to discard `<option>` because `<reason>`. Agree?"
- Any acceptance criterion where you had to invent a value (message text, limit, number format).

## Exit criteria

- Every glossary term has a one-sentence definition and no open synonym question.
- Every aggregate in domain-model.md has its identity, lifecycle and invariants confirmed.
- Every invariant states what happens when it would be broken.
- Every demo step has concrete example data.
- Every judging criterion maps to at least one demo step or P3 option.
- No unresolved contradiction between sources and answers.
- Everything still unknown is in `Open decisions` with a recommended default, an owner and a deadline.
