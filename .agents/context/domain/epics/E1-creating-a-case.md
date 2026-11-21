# E1. Creating a Case

**Status:** accepted · **Module:** M1. Input data · [BUSINESS-SPEC.md](../BUSINESS-SPEC.md)

## Goal

The Analyst starts work in a minute, and the input data is precise enough that the Analysis does not cover random people.

## User stories

1. **Start an Analysis with at least one narrowing identifier**
   As an Analyst, I want to start an Analysis by entering at least one narrowing identifier of a person (nickname, login, e-mail, messenger ID, profile address), so that I do not get results about random people.
   Business rules: a full name alone cannot start an Analysis — it is too general and is rejected; entering only a first and last name is not enough. A full name can still be entered as additional data that helps in judging the findings, but it never counts as a narrowing identifier and never suffices to start an Analysis on its own.
2. **Enter all known person data at once**
   As an Analyst, I want to enter all the data I know about a person in one form, from a single field up to the full set, so that the Analysis is as accurate as possible.
   Business rules: the form shows which fields are narrowing identifiers (nickname, login, e-mail, messenger ID, profile address) and which are only additional data (full name), so that it is clear what must be entered; at least one narrowing identifier is required (see 1).
3. **See what is missing when no narrowing identifier was entered**
   As an Analyst, I want to see exactly what is missing when I did not enter any narrowing identifier, so that I can fix it at once.

## Out of scope

- A cryptocurrency address as input data.
