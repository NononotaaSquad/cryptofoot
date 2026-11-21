# 0001. REST contract for epics E1–E6 ahead of implementation

**Status:** Accepted · 2026-11-21

## Context

Frontend and backend work on epics E1–E6 in parallel. The frontend needs typed hooks before the backend logic exists.

## Decision

Define the whole contract first: controllers whose bodies are `TODO("<story>: ...")` (HTTP 500 until implemented), DTOs in `dto/`, domain enums in `model/Enums.kt`, and a synced `openapi.json` + Orval client.

| Endpoint | Story |
|---|---|
| `POST /api/1/cases` → 201 `CaseResponse` | E1.1–E1.3 |
| `GET /api/1/cases`, `GET /api/1/cases/{caseId}` | E6.1 |
| `POST /api/1/cases/{caseId}/analyses` → 202 `AnalysisResponse` (409 when one is running) | E2.1, E6.2 |
| `GET /api/1/cases/{caseId}/analyses` | E6.1 |
| `GET /api/1/cases/{caseId}/comparison?earlierAnalysisId&laterAnalysisId` | E6.3 |
| `GET/PUT /api/1/cases/{caseId}/schedule` | E6.4 |
| `GET /api/1/analyses/{analysisId}` (polled while `RUNNING`) | E2.2, E6.1 |
| `GET /api/1/analyses/{analysisId}/mentions` | E2.2, E2.3 |
| `GET /api/1/analyses/{analysisId}/report` | E3.1, E5.1–E5.4 |

- Analysis runs asynchronously; the client polls `GET /analyses/{id}`.
- The report reads the saved Analysis only; it never reruns it (E5.4).
- `Justification` is a list of `MentionResponse`, no text (E3).
- `addressActivity` is nullable: E4 is on hold.
- `network` is a free string until supported networks are decided. *Superseded by [0002](0002-address-candidate-validation.md): enum `Network { BTC, ETH }`.*

## Open (contract will change)

- "At least one narrowing identifier" class-level constraint on `CreateCaseRequest` (TODO in `Requests.kt`).
- Schedule cadence/timezone in `UpdateScheduleRequest` (E6-D3).
- Scoring rule names and Confidence Level bands (E3), exclusion visibility (mentor question 1).
- E4 Address Activity scope; E7 demo Case reset has no endpoint.
