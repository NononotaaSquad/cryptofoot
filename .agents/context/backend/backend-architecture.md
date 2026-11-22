# Backend Architecture

## Technology Stack

- **Framework:** Spring Boot 4.x
- **Database:** MongoDB (Spring Data MongoDB)
- **Validation:** Jakarta Bean Validation (`spring-boot-starter-validation`, Hibernate Validator)
- **Language:** Kotlin
- **Java Version:** 25
- **Build Tool:** Gradle (Kotlin DSL)
- **Code Formatting:** Spotless with ktlint
- **Testing:** Spock 2.5 (Groovy 5) on the JUnit Platform. Unit specs in `src/test` (`./gradlew test`); integration specs in a separate `integrationTest` Gradle test suite (`./gradlew integrationTest`) using `spock-spring`, `BaseSpec` and a Testcontainers MongoDB (Docker required)

## Architecture Decisions

### ADR-001: MongoDB Data Persistence

**Status:** Accepted

**Context:**
Application data must survive restarts while keeping a flexible schema that suits rapid hackathon development.

**Decision:**
Use MongoDB as the only database, accessed through Spring Data MongoDB. No JPA or SQL. The connection comes from `spring.mongodb.uri`, which reads the required `MONGODB_URI` environment variable (no default: the app fails to start without it). `backend/docker-compose.yaml` runs MongoDB and the backend (profile `local`) locally.

**Consequences:**
- **Positive:** Data persists across restarts, schema can evolve without migrations, minimal repository code
- **Negative:** The backend needs a running MongoDB instance; multi-document transactions require a replica set
- **Mitigation:** One `docker compose` command starts MongoDB locally; prefer single-document writes

### ADR-002: Spring Data Repositories

**Status:** Accepted

**Context:**
Need a consistent pattern for repositories that provides CRUD operations and query capabilities.

**Decision:**
Define each repository as an interface extending `MongoRepository<V, K>` in `repository/`, with derived query methods for lookups. Define models as `@Document` data classes in `model/`, keeping them separate from DTOs. `spring.data.mongodb.auto-index-creation` is enabled so `@Indexed` takes effect.

**Consequences:**
- **Positive:** No repository boilerplate, type-safe CRUD, derived queries, declarative indexes
- **Negative:** Index creation runs on startup, which suits a hackathon but should be managed explicitly for large production collections
- **Errors:** A unique index violation raises `DuplicateKeyException`, which `RestExceptionHandler` maps to 409

### ADR-003: Package Structure

**Status:** Accepted

**Context:**
Need clear organization for Kotlin code following Spring Boot best practices.

**Decision:**
Organize by feature/domain under `squad.nononotaa.backend`:
- `config/` - Configuration classes
- `controller/` - REST controllers
- `dto/` - Data transfer objects: all request DTOs in `dto/Requests.kt`, all response DTOs in `dto/Responses.kt`
- `exception/` - Custom exceptions and error handling
- `model/` - Domain models
- `repository/` - Repository implementations
- `service/` - Business logic services
- `util/` - Utility classes

**Consequences:**
- **Positive:** Clear separation of concerns, follows Spring Boot conventions, easy to navigate
- **Negative:** None significant

### ADR-004: Exception Handling

**Status:** Accepted

**Context:**
Need consistent error responses across REST API endpoints.

**Decision:**
Use `@RestControllerAdvice` with `RestExceptionHandler` class to handle exceptions globally:
- `BadRequestException` → 400 BAD_REQUEST
- `NotFoundException` → 404 NOT_FOUND
- `ConflictException` → 409 CONFLICT
- `ValidationException` → 422 UNPROCESSABLE_CONTENT
- `DuplicateKeyException` → 409 with a fixed Polish message (logged at WARN without the duplicated value)
- `@Valid` failures (body and method parameters) and missing/mistyped JSON fields → 400 "Popraw błędy w formularzu." with `errors: [{ field, message }]`; default constraint texts are Polish in `ValidationMessages.properties`, and `spring.web.locale: pl` (fixed) keeps any remaining Hibernate Validator defaults Polish
- Spring MVC errors (unknown route, wrong method, malformed body, invalid API version) → their standard status with a fixed Polish message, via `ResponseEntityExceptionHandler`
- `Exception` → 500 INTERNAL_SERVER_ERROR, logged server-side with a generic message to the client

**Consequences:**
- **Positive:** Consistent error responses, centralized error handling, clean controller code
- **Negative:** None significant
- **Location:** `squad.nononotaa.backend.exception.RestExceptionHandler`

### Analysis orchestration (task #24, proposed)

`squad.nononotaa.backend.analysis.AnalysisOrchestrator` runs one Analysis through the ports `MentionSearch`, `CandidatePreparation`, `ExclusionClassifier` and `ConfidenceScoring`, and hands the outcome to `AnalysisRecorder` (owned by #25). It derives the status (running/completed/partial/failed) and contains no search, validation, classification or scoring logic. It is a plain class, wired as a bean by `config/AnalysisConfig` (backend-sync-1). Contracts and decisions: `.agents/context/backend/decisions/task-24/` (all still proposed, not approved by the module owners).

### Analysis history (task #25, proposed)

`AnalysisRecorderService` implements the `AnalysisRecorder` port of the orchestration: `begin` inserts a `RUNNING` `AnalysisRecord` (collection `analyses`, one document per Analysis), `finish` stores the result once; a finished record is never written again. `AnalysisHistoryService` reads it: `GET /api/1/cases/{caseId}/analyses` (newest first) and `GET /api/1/analyses/{analysisId}`; reads never rerun an Analysis. The `mentions` and `report` endpoints are served by `AnalysisReportService` (backend-sync-1). Decisions: `.agents/context/backend/decisions/task-25/`.

### Analysis comparison and manual rerun (task #26, proposed)

`AnalysisComparisonService` serves `GET /api/1/cases/{caseId}/comparison` from two saved Analyses; `AnalysisComparer` holds the rules (categories, comparability limitations, `absenceVerifiable`) and reads only stored data. `AnalysisRerunService` creates a new `MANUAL` Analysis with the Case's current identifiers (via the proposed `CaseIdentifierSource` port) and refuses while one is running; it is wired by `AnalysisConfig` and serves `startAnalysis` (backend-sync-1). Decisions: `.agents/context/backend/decisions/task-26/`.

### Periodic execution (task #27, partly accepted by the task owner)

`ScheduleService` serves `GET/PUT /api/1/cases/{caseId}/schedule` (collection `schedules`, one record per Case): enable or disable, a predefined cadence (`SchedulePreset`, default `DAILY` 06:00), fixed zone `Europe/Warsaw`, next execution and the last attempt. `ScheduledRunner` (`@Scheduled`, `app.scheduler.tick`) starts due terms through the shared `AnalysisRerunService` with trigger `SCHEDULED`; an overlapping term is skipped and recorded, terms missed during downtime (older than `app.scheduler.grace`) are not made up. `DEMO_EVERY_MINUTE` needs `app.scheduler.demo-enabled=true`. The runner uses the `AnalysisRerunService` bean wired by `AnalysisConfig`; `app.scheduler.enabled=false` removes it. Decisions: `.agents/context/backend/decisions/task-27/`. **Production runtime (#73):** the runner is an in-process `@Scheduled` task, so the Cloud Run backend is deployed with `--min-instances 1 --no-cpu-throttling` (an instance that scales to zero or is CPU-throttled outside requests would not run terms without a browser); `--max-instances 3` stays because the claim of a term is an optimistic lock (`@Version` of the schedule record), so two instances never start the same term twice. The frontend (`features/case-details/schedule-*`) only reads and writes the Schedule through the generated client; terms and time zone come from the contract.

### Wired Analysis flow (backend-sync-1)

`config/AnalysisConfig` builds the `AnalysisOrchestrator` from the port beans (`SearchService`, `CandidatePreparationService`, `ExclusionClassifier`, `ConfidenceScoringService`, `AnalysisRecorderService`) and the shared `AnalysisRerunService` (virtual-thread executor). `POST /api/1/cases/{caseId}/analyses` starts a `MANUAL` Analysis (202, 404, 409) and `ScheduledRunner` starts `SCHEDULED` ones through the same flow; `app.scheduler.enabled=false` removes the runner (set in `BaseSpec`). The scoring port receives `ScoringInput` (see the Confidence Level algorithm below). The search matches the canonical wallet and ignores the full name as a relevance key. `GET /cases`, `GET /cases/{caseId}` (with `latestAnalysis`), `GET /analyses/{id}/mentions` and `/report` (`AnalysisReportService`, 409 while running) read the stored snapshot only. `PUT .../schedule` needs an existing Case. Decisions: `.agents/context/backend/decisions/sync-1/`; one PlantUML diagram per flow in `.agents/context/backend/diagrams/`.

### Confidence Level algorithm (task #9)

`ConfidenceScoringService` (`@Service`) implements the `ConfidenceScoring` port: `score(ScoringInput(accepted, unclassified, mentions, identifiers))`, a pure function with algorithm version `confidence-1`. A Mention supports an address only if it names the person by a non-wallet narrowing identifier, does not quote the address in a warning and has no conflicting e-mail; with no support the Attribution gets 0%. The strongest supporting source kind sets the base (own profile 80, sale or settlement 60, discussion 40, mass comment 10, inferred from URL, snippet and Case identifiers), +15 once for independent sources (different host and different address sentence). Every applied rule (`ScoringRule`, including 0-point explanations) is stored with its contribution, and its Polish sentence builds the Justification text stored with the Attribution; the report returns `justification` (text) and `mentions`. Decisions: `.agents/context/backend/decisions/task-9/`.

### Address Activity (task #23)

`AnalysisOrchestrator` runs a fifth stage `ADDRESS_ACTIVITY` after scoring through the port `AddressActivityCollector` (`AddressActivityService`, wired by `config/AddressActivityConfig`). Only Attributions with a Confidence Level above 0 are checked; the outcome (`AVAILABLE` with data, or `UNAVAILABLE`) is stored on `ScoredAttribution.addressActivity` in the Analysis snapshot, and the report returns `addressActivityStatus` (`NOT_CHECKED` when absent) and `addressActivity`. Sources in order: `SyntheticActivitySource` (`fixtures/demo-v1-activity.json`, the demo addresses only, with `sources.synthetic.enabled`), then `MempoolActivitySource` (BTC) and `BlockscoutActivitySource` (ETH), keyless and self-hostable, only with `ADDRESS_ACTIVITY_LIVE_ENABLED=true` (default false, but on by default in the prod profile, D23-012; URLs in `app.address-activity.*`). `AddressActivityEvaluator` (rules version `activity-1`, stored with each activity together with the threshold) derives totals (unknown stays null, never 0), cash-out signals (outgoing transfer to an `EXCHANGE`-labelled counterparty at any amount, its kind P2P, crypto card or payment gateway taken from the dataset tags by `EtherscanTagMapping.offRampKindFor`; swap into USDT/USDC/DAI, matched by contract, from `app.address-activity.stablecoin-swap-threshold`, default 1000) and coverage limitations. An unavailable outcome makes the Analysis `PARTIAL` with `BLOCKCHAIN_DATA_UNAVAILABLE`; a crash of the stage never fails the Analysis. HTTP helpers shared with the bytecode check are in `util/ExternalHttp.kt`. Decisions: `.agents/context/backend/decisions/task-23/`; diagram `15-address-activity.puml`.

### Demo scheduler check (task #28, Bohdan's part)

`application-local.yaml` (profile `local`) shortens the scheduler check to 5 seconds and allows the labelled `DEMO_EVERY_MINUTE` cadence. The repeatable verification, its blockers and the proposed (not implemented) reset boundary are in `.agents/context/backend/demo-scheduler-check.md`; decisions in `.agents/context/backend/decisions/task-28/`.

### Case creation (task #5)

`POST /api/1/cases` creates a `CaseRecord` (collection `cases`, id generated by MongoDB, never changes). `CreateCaseRequest` is validated by the class-level `@NarrowingIdentifier` (at least one of nickname, login, e-mail, messenger ID, profile address or wallet address; a full name alone is rejected with a Polish message naming the kinds) and its text fields are trimmed at deserialization (`TrimToNull`). The optional `walletAddress` (one BTC or ETH address) is validated by `@ValidWalletAddress` with the existing `AddressValidator`, errors are on that field, and only the canonical form is stored; it is the Analyst's own input, never a Mention or evidence (task #50). `CaseService` also implements `CaseIdentifierSource` for the Analysis flow and passes the wallet as `IdentifierType.WALLET_ADDRESS`. Decisions: `.agents/context/backend/decisions/task-5/` and `task-50/`.

### E2E suites (backend-e2e-1)

`.e2e/backend/` holds black-box E2E tests of the running backend as a Postman collection and an identical Bruno collection (same requests, order and assertions; variable `baseUrl`). One ordered run covers health, Case validation and creation, a manual Analysis of the synthetic demo person (polled until finished) with its report and Mentions, rerun, history and comparison, the wallet-as-input and no-results cases, the schedule, API errors and CORS. `./scripts/e2e-backend.sh [postman|bruno|all]` runs them with `npx`. Decisions: `.agents/context/backend/decisions/e2e-1/`.

### Web2 sources (task #61, proposed)

`analysis/web2` holds two `MentionSource` adapters (`github`, `bitcointalk`; `synthetic=false`) on the JDK `HttpClient` (no redirects, bounded body, per-source timeout), configured by `config/Web2SourcesConfig` (`sources.<name>.*`). Both are **on by default** (`application.yaml`; the `local` profile and the integration specs switch them off so the demo, the E2E suites and CI stay offline); Reddit was rejected by the team. Environment: `SOURCES_GITHUB_ENABLED`, `SOURCES_GITHUB_TOKEN`, `SOURCES_BITCOINTALK_ENABLED` (limits: `max-logins`, `max-posts`, `max-resources`, `max-bytes`, `timeout`, `request-delay`). Outcomes: results, valid zero (`CHECKED`), `UNAVAILABLE` (401/403/429/5xx/timeout), `NOT_CHECKED` (no supported identifier). Decisions and open points: `.agents/context/backend/decisions/task-61/`.

**GitHub token (prod).** The GitHub source calls only public endpoints (`GET /users/{login}`, `GET /repos/{login}/{login}/readme`), so the token needs **no scopes** (a fine-grained token with *Public repositories (read-only)* and no extra permissions, or a classic token with no scopes; it only lifts the anonymous 60 requests/hour limit to 5000). Local: the token is optional; an enabled source without one sends anonymous requests with no `Authorization` header (the local profile keeps the source off; switch on with `SOURCES_GITHUB_ENABLED=true`). Production (`application-prod.yaml`, `sources.github.require-token=true`): an enabled source without a token **fails the startup** with a message naming `SOURCES_GITHUB_TOKEN` (never a value), and a rejected token (401/403/429) is `UNAVAILABLE`, never retried anonymously. Wiring: GitHub Actions secret `SOURCES_GITHUB_TOKEN` (org or repository) -> the deploy job copies it to Secret Manager secret `<repo>-github-token` -> Cloud Run mounts it as env var `SOURCES_GITHUB_TOKEN` (`--set-secrets`). With the Actions secret missing the deploy warns and sets `SOURCES_GITHUB_ENABLED=false` explicitly. The deployer service account already holds `roles/secretmanager.admin` and the runtime one `roles/secretmanager.secretAccessor` (project level, `scripts/gcp-bootstrap.sh`), so no extra IAM is needed. Rotation: create a new token, update the Actions secret, re-run the deploy (a new secret version is added and `latest` is mounted on the new revision), then revoke the old token. The token never reaches the frontend, the image or build arguments.

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── kotlin/squad/nononotaa/backend/
│   │   │   ├── BackendApplication.kt
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   │   ├── Requests.kt
│   │   │   │   └── Responses.kt
│   │   │   ├── exception/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── util/
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-local.yaml
│   │       ├── application-prod.yaml
│   │       ├── static/
│   │       └── templates/
│   ├── test/groovy/            # Spock unit specs (no Spring context, mocks)
│   └── integrationTest/groovy/ # Spock integration specs (extend BaseSpec)
├── build.gradle.kts
└── gradlew
```

## Key Components

### MongoDB Repositories
Model and repository for a collection:
```kotlin
@Document("items")
data class Item(
    @Id val id: String? = null,
    @Indexed(unique = true) val name: String,
)

interface ItemRepository : MongoRepository<Item, String> {
    fun findByName(name: String): Item?
}
```
Leave `id` null on insert and MongoDB generates it.

### Error Handling
- **BadRequestException** - Custom exception for malformed requests (400)
- **NotFoundException** - Custom exception for 404 errors
- **ConflictException** - Custom exception for conflicts such as duplicates (409)
- **ValidationException** - Custom exception for validation failures (422)
- **ErrorResponse** - Consistent error response data class with HTTP status and message
- **RestExceptionHandler** - Global `@RestControllerAdvice` extending `ResponseEntityExceptionHandler` that converts custom and Spring MVC exceptions to HTTP error responses with the matching status

### Logger Extension
Extension function `T.logger()` in `util/Extensions.kt` provides idiomatic Kotlin logger creation:
```kotlin
companion object {
    private val logger = logger()
}
```

### ApiController
Use `@ApiController` from `util/ApiController.kt` on every REST controller instead of `@RestController` and `@RequestMapping`. It combines both and aliases `path` and `version` (default `"1"`).

Example:
```kotlin
@ApiController("/test")
class TestController {
    @GetMapping
    fun hello() = "Hello"
}
```
This serves `GET /api/1/test`. Give only the resource path: `config/WebConfig.kt` prepends `/api/{version}` (`WebConfig.API_PATH_PREFIX`) to every `@ApiController` via `PathMatchConfigurer.addPathPrefix`, and `spring.mvc.apiversion.use.path-segment: 1` in `application.yaml` reads the version from that second path segment. Never write `/api/{version}` in `@ApiController` yourself. Call endpoints with the bare version number (`/api/1/...`), the form springdoc publishes in `/v3/api-docs` and the generated client uses.

To add a new version of an endpoint, set `version` explicitly, e.g. `@ApiController("/test", version = "2")`.

### Configuration
- Health: `spring-boot-starter-actuator` exposes only `/actuator/health` plus `/actuator/health/liveness` and `/readiness`; Docker Compose uses it as the backend healthcheck
- `application.yaml` - Base configuration with profile-based application naming, the required `MONGODB_URI` and path-segment API versioning
- Profile-specific configs: `local`, `prod` (`prod` sets the database name from `MONGODB_DATABASE`)
- CORS: `WebConfig` allows any origin on `/api/**` (public API, no cookies; covers both Cloud Run frontend URLs). Switch to an explicit origin list before adding cookie-based auth

## Development Guidelines

- Use primary constructors for dependency injection
- Prefer `val` over `var` for immutability
- Use `data class` for DTOs
- Use companion objects for loggers
- Run `./gradlew spotlessApply` before committing
- NO JPA, NO databases other than MongoDB
- Use repositories extending `MongoRepository`

## Running the Application

### Using Run Scripts (Recommended)

```bash
./scripts/run-backend.sh   # MongoDB + backend in Docker
./scripts/run-frontend.sh # Run frontend only
./scripts/run-all.sh      # Docker stack + frontend
```

### Manual Start

```bash
docker compose -f backend/docker-compose.yaml up --build
```

To run from the IDE, start only MongoDB (`docker compose -f backend/docker-compose.yaml up -d mongodb`) and run `./gradlew bootRun` in `backend/`. `MONGODB_URI` is required (no default), e.g. `MONGODB_URI=mongodb://localhost:27017/db ./gradlew bootRun`.

Backend runs on `http://localhost:8080`
