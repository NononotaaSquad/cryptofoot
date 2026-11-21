# Hackathon Template

Spring Boot Kotlin backend + Next.js frontend monorepo. MongoDB persistence.

Read `.agents/context/product.md` and `.agents/context/domain-model.md` before implementing any feature. At the start of the hackathon, run the `hackathon-kickoff` skill.

## Quick Start

**Backend** (MongoDB + backend in Docker, profile `local`):
```bash
./scripts/run-backend.sh
# or run the app from the IDE / `./gradlew bootRun` in backend/ with only MongoDB:
# docker compose -f backend/docker-compose.yaml up -d mongodb   (MONGODB_URI is required, e.g. mongodb://localhost:27017/db)
```

**Frontend** (from `frontend/`):
```bash
npm install
npm run dev        # http://localhost:3000
npm run check      # typecheck, lint, format check, tests - run after every change
npm run api:gen    # regenerate the API client from openapi.json
```

**API contract** (from the repo root, needs Docker):
```bash
./scripts/api-sync.sh   # openapi.json from the backend + regenerated client; CI fails when they drift
```

**Tests:**
```bash
cd backend && ./gradlew test integrationTest   # Spock unit + integration specs (Docker for Testcontainers)
cd frontend/apps/web && npm run test           # Vitest
```

## Skills

See `.agents/skills/` for specialized guidance:
- `hackathon-kickoff` - Hackathon start: challenge materials in `context/domain/` -> domain grilling -> filled context -> 3-phase domain backlog -> GitHub Project and issues
- `kotlin-springboot` - Spring Boot + Kotlin best practices (MongoDB)
- `api-contract` - Keep backend, `openapi.json` and the generated client in sync; resolve their merge conflicts
- `vercel-react-best-practices` - React/Next.js performance
- `web-design-guidelines` - UI review against Web Interface Guidelines (accessibility, UX)
- `ponytail` - Minimal, pragmatic development
- `git-commit` - Conventional commits
- `create-readme` - Write a project README

## Context

See `.agents/context/`:
- `product.md` - Challenge, demo scenario, invariants, edge cases (filled in at the event)
- `glossary.md` - Domain terms, one canonical name per concept (filled in at the event)
- `domain-model.md` - Domains, aggregates, lifecycles, relationships (filled in at the event)
- `domain/` - Raw challenge materials dropped in by the team (brief, judging criteria, sample data, partner docs) and `kickoff-qa.md` (team answers from the kickoff grilling); source of truth for the files above
- `backlog.json` - Phased task plan the GitHub Project is created from (written by `hackathon-kickoff`)
- `backend/backend-architecture.md` - Backend stack and decisions (ADRs are sections inside): MongoDB persistence, repository pattern, package structure, `@ApiController` with `/api/{version}` prefix, Spock + Testcontainers testing
- `frontend/frontend-architecture.md` - Frontend stack and structure
- `frontend/decisions/` - Frontend ADRs, one file each

## Definition of done

A task (GitHub issue) is done when:
- every acceptance criterion has a passing Spock spec (backend) or Vitest test (frontend);
- names match `.agents/context/glossary.md` in Kotlin, TypeScript, API paths and UI copy;
- `./scripts/api-sync.sh` was re-run and `lib/api/` committed if the contract changed;
- `./gradlew check` and `npm run check` pass;
- the behavior was verified in the running app, and the PR says `Closes #<issue>`.

## Rules

See `.agents/rules/`:
- `rules.md` - Architecture constraints, code quality, security, workflow (backend focus)
- `frontend.md` - Frontend rules: quality gate, API client, data fetching, UI library, accessibility
