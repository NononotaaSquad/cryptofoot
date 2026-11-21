# Glossary

Agents: use one canonical name per concept identically across Kotlin models/DTOs, TypeScript types, API paths, and UI copy. When a new domain concept appears or conflicts with an entry below, clarify it with the team and update this file immediately. Keep this file free of implementation details and workflow rules.

Status: empty. Add domain terms as soon as they are agreed (the `hackathon-kickoff` skill seeds them from `domain/`). Domain structure lives in `domain-model.md`.

## Language

Full definitions of the remaining terms are in `domain/BUSINESS-CONTEXT.md`.

**Analysis** (PL: Analiza):
One run of search and scoring for a Case, saved in its history. Has a trigger (manual or scheduled) and a status.
_Avoid_: run, scan, pass

**Analysis status** (PL: Status analizy):
The outcome of an Analysis: running, completed, partial or failed (proposed, see `backend/decisions/task-24/`). No results is not a failure.
_Avoid_: state, result
