# {{PROJECT_NAME}}

{{PROJECT_DESCRIPTION}}

## Tech Stack

**Backend:**
- Spring Boot 4.x
- MongoDB (Spring Data MongoDB)
- Kotlin
- Java 25
- Gradle (Kotlin DSL)
- Bean Validation (Hibernate Validator)
- Spotless (ktlint, greclipse)
- Spock (Groovy) unit + integration tests, Testcontainers MongoDB

**Frontend:**
- Next.js 16 (App Router)
- React 19
- TypeScript 5
- Tailwind CSS 4
- shadcn/ui
- Orval (generated API client) + TanStack Query
- zod
- Motion
- Vitest
- Turborepo

**Architecture:**
- MongoDB data persistence
- RESTful API
- Monorepo with shared packages

## Quick Start

After creating a repo from this template, initialize it once:

```bash
./scripts/init.sh "<project name>" "<description>"
```

It fills the README placeholders, sets `APP_NAME` in `backend/docker-compose.yaml` and enables the shared git hooks.

Git does not clone hook config, so each clone enables the hooks itself: `npm install` in `frontend/` sets them up with [husky](https://typicode.github.io/husky/). Backend-only developers run `git config core.hooksPath .githooks` once.

The pre-commit hook (`.githooks/pre-commit`) formats staged backend files with Spotless and staged frontend files with Prettier, then re-stages them. Partially staged files (`git add -p`) are formatted in the working tree but not re-staged, so your unstaged hunks stay out of the commit.

`backend/docker-compose.yaml` builds the backend image and runs it with the `local` profile together with MongoDB (database `db`).

### Using Run Scripts (Recommended)

```bash
./scripts/run-backend.sh   # MongoDB + backend in Docker
./scripts/run-frontend.sh # Run frontend only
./scripts/run-all.sh      # Docker stack + frontend
```

### Manual Start

**Backend:**
```bash
docker compose -f backend/docker-compose.yaml up --build
```

To run the backend from your IDE instead, start only MongoDB (`docker compose -f backend/docker-compose.yaml up -d mongodb`) and run `./gradlew bootRun` in `backend/`. `MONGODB_URI` is required (no default), e.g. `MONGODB_URI=mongodb://localhost:27017/db ./gradlew bootRun`.

Backend runs on `http://localhost:{{BACKEND_PORT}}`. Health: `/actuator/health` (with `/liveness` and `/readiness` probes).

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:{{FRONTEND_PORT}}`. Run `npm run check` (from `frontend/`) before every commit. Details: `frontend/README.md`.

> **Warning:** the browser calls the backend at `NEXT_PUBLIC_API_URL` (dev-only default in `frontend/apps/web/lib/env.ts`). `NEXT_PUBLIC_*` values are inlined at `next build`, so build the frontend Docker image with the real public backend URL (`--build-arg NEXT_PUBLIC_API_URL=...`). Changing the container's environment afterwards has no effect.

## Testing

**Backend:**
```bash
cd backend
./gradlew test             # Spock unit specs (src/test/groovy)
./gradlew integrationTest  # Spock integration specs with Testcontainers MongoDB (needs Docker)
./gradlew check            # Spotless + both suites
```

**API contract** (from the repo root, needs Docker):
```bash
./scripts/api-sync.sh   # rewrite frontend/apps/web/lib/api/openapi.json from the backend + regenerate the client
```

CI fails when `openapi.json` no longer matches the backend or the generated client no longer matches `openapi.json`.

**Frontend** (in `frontend/apps/web`):
```bash
npm run test                      # Vitest unit tests
```

## Deployment (GCP Cloud Run)

Every push to the default branch runs `.github/workflows/ci.yml`. When tests pass, it deploys both apps to Cloud Run in a shared GCP project as `<repo>-backend` and `<repo>-frontend`, under the GitHub environment `production`. A repo created from this template deploys on its first commit, so it gets its own prod environment without any manual steps. The template repo itself never deploys.

**One-time setup per GitHub org** (an admin with GCP owner rights runs it once):

```bash
./scripts/gcp-bootstrap.sh <gcp-project-id> <gcp-region> <github-org>
```

The script enables the required APIs and creates the Artifact Registry repo `hackathon`, a deployer service account and a runtime service account. It sets up keyless Workload Identity Federation that trusts every repo in the org. It then stores these org-level Actions variables: `GCP_PROJECT_ID`, `GCP_PROJECT_NUMBER`, `GCP_REGION`, `GCP_ARTIFACT_REGISTRY`, `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_SERVICE_ACCOUNT`, `GCP_RUNTIME_SERVICE_ACCOUNT`. It also stores the org secret `MONGODB_URI`. If the GCP variables are missing, deploy jobs are skipped.

**MongoDB Atlas:** all repos share one Atlas cluster and each gets its own database named after the repo (`MONGODB_DATABASE`, `prod` profile). Atlas Network Access must allow `0.0.0.0/0`, because Cloud Run has no fixed egress IP. To use a separate cluster for one repo, set a repository secret `MONGODB_URI`; it overrides the org secret. The pipeline copies the URI into Secret Manager as `<repo>-mongodb-uri`.

**GitHub token:** set the Actions secret `SOURCES_GITHUB_TOKEN` (org or repository; a token with no scopes is enough, it only reads public profiles). The pipeline copies it into Secret Manager as `<repo>-github-token` and Cloud Run mounts it as `SOURCES_GITHUB_TOKEN`. Without the secret the deploy switches the GitHub source off; production never calls GitHub anonymously. Locally no token is needed.

**URLs:** `https://<repo>-backend-<project-number>.<region>.run.app` and `https://<repo>-frontend-<project-number>.<region>.run.app`. The frontend image is built with `NEXT_PUBLIC_API_URL` set to the backend URL. The backend allows CORS from any origin on `/api/**`.

## Project Structure

```
{{PROJECT_NAME}}/
├── backend/                    # Spring Boot Kotlin application
│   ├── src/
│   │   ├── main/kotlin/        # Kotlin source code
│   │   │   └── {{PACKAGE_PATH}}/
│   │   │       ├── config/     # Configuration
│   │   │       ├── controller/ # REST controllers
│   │   │       ├── dto/        # Data transfer objects
│   │   │       ├── exception/  # Error handling
│   │   │       ├── model/      # Domain models
│   │   │       ├── repository/ # MongoDB repositories
│   │   │       ├── service/    # Business logic
│   │   │       └── util/       # Utilities
│   │   └── resources/          # Configuration files
│   ├── build.gradle.kts        # Gradle build configuration
│   ├── Dockerfile              # Backend image (layered jar)
│   ├── docker-compose.yaml     # MongoDB + backend (local profile)
│   └── gradlew                 # Gradle wrapper
├── frontend/                   # Next.js monorepo
│   ├── apps/
│   │   └── web/               # Next.js web application
│   ├── packages/
│   │   ├── ui/                # Shared UI components (shadcn/ui)
│   │   ├── eslint-config/     # Shared ESLint configuration
│   │   └── typescript-config/ # Shared TypeScript configuration
│   ├── Dockerfile             # Frontend image (Next.js standalone)
│   ├── turbo.json             # Turborepo configuration
│   └── package.json           # Root package.json
├── .githooks/
│   └── pre-commit             # Shared git hook: formats staged backend + frontend files
├── scripts/
│   ├── init.sh                # One-time project setup after cloning the template
│   ├── api-sync.sh            # Sync openapi.json + generated client with the backend
│   ├── gcp-bootstrap.sh       # One-time GCP + GitHub org setup for deployments
│   ├── run-backend.sh        # Run MongoDB + backend in Docker
│   ├── run-frontend.sh      # Run frontend script
│   └── run-all.sh           # Run both services
├── .agents/
│   ├── skills/                 # Agent skills for AI assistance
│   ├── context/               # Architecture documentation and ADRs
│   └── rules/                 # Agent constraints
└── README.md
```

## Prerequisites

- **Backend:** Docker (also used by integration tests via Testcontainers); Java 25 to build or run tests outside Docker
- **Frontend:** Node.js >= 22.18, npm >= 11.13.0

## Code prepared before the hackathon

This template existed before HackNation 2026 started (21 Nov 2026, 11:00). It contains:
- Spring Boot Kotlin + MongoDB backend skeleton (error handling, API versioning, Docker setup)
- Next.js frontend monorepo with shared shadcn UI package, API client generator setup (Orval config and fetch client; the client itself is generated from the backend spec), app shell
- Agent documentation, rules and skills in `.agents/`, plus CI

The git history shows what was written during the event.

## License

MIT

Third-party data: `backend/src/main/resources/exclusion-labels/etherscan-labels.json` is the Etherscan label dataset from [brianleect/etherscan-labels](https://github.com/brianleect/etherscan-labels) (MIT, licence in `etherscan-labels.LICENSE` next to it), used to recognise Excluded Addresses (decision D22-002).

