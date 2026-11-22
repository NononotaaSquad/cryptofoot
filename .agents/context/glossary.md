# Glossary

Agents: use one canonical name per concept identically across Kotlin models/DTOs, TypeScript types, API paths, and UI copy. When a new domain concept appears or conflicts with an entry below, clarify it with the team and update this file immediately. Keep this file free of implementation details and workflow rules.

Status: empty. Add domain terms as soon as they are agreed (the `hackathon-kickoff` skill seeds them from `domain/`). Domain structure lives in `domain-model.md`.

## Language

Full definitions of the remaining terms are in `domain/BUSINESS-CONTEXT.md`.

**Case** (PL: Sprawa):
The object created for one investigated person from their identifiers. It needs at least one narrowing identifier (nickname, login, e-mail, messenger ID, profile address, cryptocurrency wallet address); a full name alone is not enough.
_Avoid_: investigation, project

**Nickname** (PL: Nazwa użytkownika):
The name a person shows publicly on a forum or service. Not the same as a Login, the name used to sign in.
_Avoid_: nick (in UI copy)

**Wallet address** (PL: Adres portfela):
A cryptocurrency address entered by the Analyst as an identifier of the Case. Input data, not a finding: never a Mention, never independent confirmation.
_Avoid_: address candidate

**Premise** (PL: Przesłanka):
One cryptocurrency address in one Mention, together with its assessment: whether it supports the Attribution or was dismissed, and why. A Mention with two addresses gives two Premises. A dismissed Premise stays visible in the report.
_Avoid_: evidence, reading, hit

**Analysis** (PL: Analiza):
One run of search and scoring for a Case, saved in its history. Has a trigger (manual or scheduled) and a status.
_Avoid_: run, scan, pass

**Address Activity** (PL: Aktywność adresu):
What the address of an Attribution did on the blockchain: how much went through it, when, and its cash-out signals in time order. Missing data is never shown as zero activity.
_Avoid_: indicators, metrics

**Cash-out signal** (PL: Sygnał wyjścia do walut tradycyjnych):
An on-chain event that may mean an exit to traditional currencies: a transfer to an exchange, P2P platform, payment gateway or crypto card service, or a swap into a stablecoin of at least 1000 units. A lead, not proof of a withdrawal.
_Avoid_: withdrawal, fiat exit, off-ramp event

**Analysis status** (PL: Status analizy):
The outcome of an Analysis: running, completed, partial or failed (proposed, see `backend/decisions/task-24/`). No results is not a failure.
_Avoid_: state, result
