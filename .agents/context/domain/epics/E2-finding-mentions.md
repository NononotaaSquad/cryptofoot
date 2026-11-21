# E2. Finding Mentions

**Status:** accepted · **Module:** M2. Information search · [BUSINESS-SPEC.md](../BUSINESS-SPEC.md)

## Goal

Take the tedious walk through sources away from the Analyst. The search collects and marks; it does not judge anything.

## User stories

1. **Start an Analysis with one click**
   As an Analyst, I want to start an Analysis with one click, so that I do not search the sources myself.
2. **Get the Mentions result with full source data**
   As an Analyst, I want to get Mentions from a fixed, short set of sources, where every Mention shows where it comes from, when it was collected, and which cryptocurrency addresses appeared in it, so that I know what was found, where and when, and I do not have to look for the addresses in the content myself.
   Business rules: the set of sources is fixed and short — at most 2–3 public sources plus synthetic data — so that the result is predictable; the result also shows which sources were checked, so that I know what the Analysis did not cover; every Mention always carries the full set: source (so that I can return to it), date and time of collection (so that I know for which moment the result is valid), and the detected addresses. The search collects and marks, it does not judge anything (judging is E3).
3. **Get a clear message when no Mention was found**
   As an Analyst, I want a clear message when no Mention was found, so that I can tell no results apart from an error.

## Out of scope

- General internet search.
- Judging the reliability of Mentions (this is E3).

## Open questions

- Which 2–3 public sources do we choose? (team)
- Which external sources are "approved"? (mentors)
