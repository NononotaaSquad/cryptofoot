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

`squad.nononotaa.backend.analysis.AnalysisOrchestrator` runs one Analysis through the ports `MentionSearch`, `CandidatePreparation`, `ExclusionClassifier` and `ConfidenceScoring`, and hands the outcome to `AnalysisRecorder` (owned by #25). It derives the status (running/completed/partial/failed) and contains no search, validation, classification or scoring logic. It is a plain class: no production adapters or endpoint exist yet. Contracts and decisions: `.agents/context/backend/decisions/task-24/` (all still proposed, not approved by the module owners).

### Analysis history (task #25, proposed)

`AnalysisRecorderService` implements the `AnalysisRecorder` port of the orchestration: `begin` inserts a `RUNNING` `AnalysisRecord` (collection `analyses`, one document per Analysis), `finish` stores the result once; a finished record is never written again. `AnalysisHistoryService` reads it: `GET /api/1/cases/{caseId}/analyses` (newest first) and `GET /api/1/analyses/{analysisId}`; reads never rerun an Analysis. The `mentions` and `report` endpoints stay stubs (E2/E5). Decisions: `.agents/context/backend/decisions/task-25/`.

### Analysis comparison and manual rerun (task #26, proposed)

`AnalysisComparisonService` serves `GET /api/1/cases/{caseId}/comparison` from two saved Analyses; `AnalysisComparer` holds the rules (categories, comparability limitations, `absenceVerifiable`) and reads only stored data. `AnalysisRerunService` creates a new `MANUAL` Analysis with the Case's current identifiers (via the proposed `CaseIdentifierSource` port) and refuses while one is running; it is a plain class, not wired, and `startAnalysis` stays a stub until Cases (E1) and module adapters exist. Decisions: `.agents/context/backend/decisions/task-26/`.

### Periodic execution (task #27, partly accepted by the task owner)

`ScheduleService` serves `GET/PUT /api/1/cases/{caseId}/schedule` (collection `schedules`, one record per Case): enable or disable, a predefined cadence (`SchedulePreset`, default `DAILY` 06:00), fixed zone `Europe/Warsaw`, next execution and the last attempt. `ScheduledRunner` (`@Scheduled`, `app.scheduler.tick`) starts due terms through the shared `AnalysisRerunService` with trigger `SCHEDULED`; an overlapping term is skipped and recorded, terms missed during downtime (older than `app.scheduler.grace`) are not made up. `DEMO_EVERY_MINUTE` needs `app.scheduler.demo-enabled=true`. The runner idles until an `AnalysisRerunService` bean exists. Decisions: `.agents/context/backend/decisions/task-27/`.

### Demo scheduler check (task #28, Bohdan's part)

`application-local.yaml` (profile `local`) shortens the scheduler check to 5 seconds and allows the labelled `DEMO_EVERY_MINUTE` cadence. The repeatable verification, its blockers and the proposed (not implemented) reset boundary are in `.agents/context/backend/demo-scheduler-check.md`; decisions in `.agents/context/backend/decisions/task-28/`.

### Case creation (task #5)

`POST /api/1/cases` creates a `CaseRecord` (collection `cases`, id generated by MongoDB, never changes). `CreateCaseRequest` is validated by the class-level `@NarrowingIdentifier` (at least one of nickname, login, e-mail, messenger ID, profile address or wallet address; a full name alone is rejected with a Polish message naming the kinds) and its text fields are trimmed at deserialization (`TrimToNull`). The optional `walletAddress` (one BTC or ETH address) is validated by `@ValidWalletAddress` with the existing `AddressValidator`, errors are on that field, and only the canonical form is stored; it is the Analyst's own input, never a Mention or evidence (task #50). `CaseService` also implements `CaseIdentifierSource` for the Analysis flow and passes the wallet as `IdentifierType.WALLET_ADDRESS`. Decisions: `.agents/context/backend/decisions/task-5/` and `task-50/`.

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
