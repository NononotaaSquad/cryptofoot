# Product

Agents: read this file before implementing any feature. Use exact domain names from `.agents/context/glossary.md` and the domain structure from `.agents/context/domain-model.md`. Raw challenge materials (brief, judging criteria, sample data, partner docs, `kickoff-qa.md` with the team's answers) live in `.agents/context/domain/`; when this file is silent, check them before asking. When a rule, step, or edge case is missing here, stop and ask the team instead of guessing.

Status: empty. Fill in during the first hour before writing feature code (the `hackathon-kickoff` skill does this from `domain/`). Replace prompts with short bullets.

## Challenge

Institution, challenge name, and verbatim judging criteria with weights.

## Problem and user

Who the user is, what problem they face today, and what outcome our product gives them (2-4 sentences).

## Demo scenario

The 2-3 minute path shown to the jury as numbered steps (`Screen -> Action -> Result`). Every implemented feature must serve a step here.

## Invariants

Business rules that must always hold, one per bullet. Each becomes a type constraint or an automated test.

## Boundaries and edge cases

Expected behavior for empty states, invalid input, limits, errors, and permissions.

## Out of scope

Capabilities we deliberately skip during the hackathon.

## Open decisions

Question, owner, deadline.

## Backlog

Tasks live in the GitHub Project created by the `hackathon-kickoff` skill from `.agents/context/backlog.json`, sliced with the hamburger method (each user story split into steps, each step into options from crude to complete; `backlog.json` keeps the hamburgers) into three phases: P1 Minimum (shortest demoable path), P2 Ready product (full demo scenario, invariants, edge cases), P3 Enhancements (extra score). Before starting a task, read its issue; when a task changes scope, update `backlog.json` and this file.
