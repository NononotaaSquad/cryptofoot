# Agent Rules

## Architecture Constraints

- **MONGODB ONLY:** MongoDB is the only database. Never add JPA, SQL, or other database dependencies.
- **SPRING DATA REPOSITORIES:** Persist data through interfaces extending `MongoRepository`, with models as `@Document` data classes in `model/`. Use `MongoTemplate` only for queries a repository cannot express.
- **CONNECTION:** Configure MongoDB through the required `MONGODB_URI` environment variable; do not add a default value for it in `application.yaml`. Never hardcode credentials in a connection string.

## Code Quality

- **FOLLOW PATTERNS:** Use existing patterns from the codebase (package structure, dependency injection, error handling).
- **MINIMAL CHANGES:** Apply ponytail principle - smallest change that solves the problem. No over-engineering.
- **MINIMAL COMMENTS:** Code is the source of truth; make it self-explanatory through naming and structure. Write a comment only when the code cannot say why (a non-obvious constraint, a workaround, a disabled lint rule, a type assertion), and keep it to one short line. No comments that restate the code, no multi-line explanations, no KDoc, Javadoc, GroovyDoc or JSDoc blocks. Keep existing comments.
- **TYPE SAFETY:** Use Kotlin null-safety and TypeScript strict mode. No `!!`, `@Suppress`, `eslint-disable` or `as` without a justification comment.
- **LOGGING:** Use `logger()` extension function from `util/Extensions.kt` in companion objects:
  ```kotlin
  companion object {
      private val logger = logger()
  }
  ```
- **LOG WHAT MATTERS:** Every backend class with behavior (controllers, services, handlers, config) must log enough to trace a request and debug a failure from logs alone:
  - `INFO` — state changes and business events: entity created/updated/deleted (with its ID), important actions finished, startup/config summary.
  - `DEBUG` — method entry with key parameters, repository/`MongoTemplate` query results (counts, IDs), branch decisions.
  - `WARN` — handled problems the client caused or recoverable issues: validation failures, not found, conflicts, retries, fallbacks.
  - `ERROR` — unexpected failures; always pass the exception (`logger.error("...", e)`) so the stack trace is kept.
  - Use parameterized messages (`logger.info("Created user id={}", id)`), never string concatenation.
  - Never log secrets, passwords, tokens, full request bodies with personal data, or connection strings.
  - Do not log the same exception twice: `RestExceptionHandler` already logs unexpected (5xx) errors, so don't catch-log-rethrow them in services. Log the `WARN` for a 4xx case where you throw the custom exception (the handler does not log those).
  - Do not log inside tight loops.
- **DTOs:** Put every request DTO in `dto/Requests.kt` and every response DTO in `dto/Responses.kt` as Kotlin `data class`es; do not create one file per DTO. Name them `<Action><Resource>Request` and `<Resource>Response` (e.g. `CreateUserRequest`, `UserResponse`). Controllers accept and return DTOs only, never `@Document` models. Validate request DTOs with Jakarta Bean Validation annotations using the `@field:` target (e.g. `@field:NotBlank val name: String`) and `@Valid` on the controller parameter.
- **FRONTEND:** Follow `.agents/rules/frontend.md` for the API client, data fetching, tests and UI.
- **ERROR HANDLING:** Use custom exceptions `BadRequestException` (400), `NotFoundException` (404), `ConflictException` (409), and `ValidationException` (422). `RestExceptionHandler` automatically converts these to HTTP error responses.
- **ERROR MESSAGES IN POLISH:** The `message` of a custom exception becomes `ErrorResponse.message`, and the frontend shows it to the user unchanged for every 4xx. Write it for the end user:
  - Polish, a full sentence ending with a period, no technical details (no class names, IDs, field paths, stack traces).
  - Only this client-facing text is Polish. Code, identifiers, log messages and comments stay in English.
  - Do not put details into 5xx responses: `RestExceptionHandler` sends `status.reasonPhrase` there and the frontend shows its own text.
  ```kotlin
  throw NotFoundException("Nie znaleziono zgłoszenia o podanym identyfikatorze.") // good
  throw NotFoundException("Ticket $id not found")                               // bad: English, leaks the ID
  ```
- **VALIDATION MESSAGES IN POLISH:** A failed `@Valid` request becomes 400 with `message` "Popraw błędy w formularzu." and `errors: [{ field, message }]`, one per invalid field (`field` is a path like `items[0].name`, or null for a class-level constraint). Missing or mistyped JSON fields are reported the same way. Default constraint texts are Polish sentences in `src/main/resources/ValidationMessages.properties`; give a constraint its own `message = "..."` (Polish, full sentence) when the default does not fit, e.g. `@field:Size(max = 80, message = "Nazwa może mieć najwyżej 80 znaków.")`. Spring's own 4xx errors (unknown route, wrong method, malformed body) get fixed Polish texts in `RestExceptionHandler`.
- **API CONTRACT:** Controllers, DTOs and `ErrorResponse` are the contract the frontend is generated from. After changing any of them, follow the `api-contract` skill: run `./scripts/api-sync.sh` and commit `frontend/apps/web/lib/api/` in the same PR. `OpenApiContractSpec` fails CI otherwise.

## Security

- **NO SECRETS:** Never hardcode API keys, passwords, or credentials. Use environment variables if needed.
- **NO DESTRUCTIVE ACTIONS:** Do not delete files, databases, or run destructive commands without explicit user confirmation.
- **NO UNSAFE DEPENDENCIES:** Do not add dependencies with security vulnerabilities or unmaintained packages.

## Workflow

- **READ FIRST:** Always read existing code and context before making changes.
- **TEST-ONLY CONTROLLERS:** Put controllers used only by specs under `squad.nononotaa.testsupport` (outside the component-scanned base package) and `@Import` them where needed; inside the base package they would leak into every test context and fail `OpenApiContractSpec`.
- **TEST:** Write tests for new functionality as Spock specifications in Groovy (named `*Spec.groovy`); do not add Kotlin/JUnit tests. Unit specs go in `src/test/groovy`: extend `Specification`, no Spring context, mock collaborators with `Mock()`/`Stub()`. Integration specs go in `src/integrationTest/groovy` and extend `BaseSpec` (full app on a random port, MongoDB from a shared Testcontainers `mongo:8` container wired via `MONGODB_URI`, injected `RestTestClient client`; requires Docker); call endpoints over real HTTP through `client`, not MockMvc; run them with `./gradlew integrationTest` (also part of `./gradlew check`). Groovy is formatted by Spotless (greclipse, 4 spaces). Ensure existing tests pass. Frontend tests: see `.agents/rules/frontend.md`.
- **FORMAT:** Backend: run `./gradlew spotlessApply` before committing. Frontend: run `npm run check` (see `.agents/rules/frontend.md`).
- **DOCUMENT:** Update relevant documentation when changing architecture or adding features.

## Prohibited Actions

- Adding JPA starters or non-MongoDB database dependencies
- Hardcoding secrets or credentials
- Deleting data or files without confirmation
- Ignoring existing code patterns
- Bypassing type safety without justification
- Making unnecessary abstractions or over-engineering
